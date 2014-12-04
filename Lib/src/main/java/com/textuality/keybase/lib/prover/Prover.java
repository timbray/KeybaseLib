/*
 * Copyright (C) 2014 Tim Bray <tbray@textuality.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.textuality.keybase.lib.prover;

import android.util.Base64;

import com.textuality.keybase.lib.JWalk;
import com.textuality.keybase.lib.KeybaseException;
import com.textuality.keybase.lib.Proof;
import com.textuality.keybase.lib.Search;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * Supports Keybase proof verification.  This is self-contained with no dependencies, except on
 *  the caller.  Keybase proof checking requires checking OpenPGP signatures and fetching DNS
 *  TXT records, but libraries to do these things can be complex and heavyweight.  Therefore
 *  this function requires that the caller provide signature-checking and DNS-fetching functions.
 *
 * A note on signature checking.  Keybase proofs are ASCII-armored OpenPGP compressed messages
 *  with a signature.  That means, in terms of OpenPGP packet types, the message contains a
 *  Compressed Data packet (tag=8).  That in turn contains a one-pass signature packet (tag=4),
 *  a Literal Data packet (tag=11), and a signature packet (tag=2).  You need to do the
 *  equivalent of pgp --decrypt, validating that the signature is correct and the signing key
 *  is the one the proof concerns.
 *
 * How to use:
 * 1. call fetchProofData(), which will exhibit network latency. If it returns false the proof
 *    verification failed; an explanation can be found in the log.
 * 2. call checkFingerprint(), passing it the fingerprint of the key you’re checking up on; if
 *    if it returns false the verification failed.
 * 3. fetch the PGP message with getPgpMessage(), check that it’s signed with the right fingerprint
 *    (see above).
 * 4. Call dnsTxtCheckRequired() and if it returns non-null, the return value is a domain name;
 *    retrieve TXT records from that domain and pass them to checkDnsTxt(); if it returns false
 *    the proof verification failed; an explanation can be found in the log.
 * 5. call rawMessageCheckRequired() and if it returns true, feed the raw (de-armored) bytes
 *    of the message to checkRawMessageBytes(). if it returns false the proof verification failed;
 *    an explanation can be found in the log. This may exhibit crypto latency.
 * 6. Pass the message to validate(), which should have no real latency.  If it returns false the
 *    proof verification failed; an explanation can be found in the log.
 */
public abstract class Prover {

    String mPgpMessage;
    String mPayload;
    String mShortenedMessageHash;
    String mFingerprintUsedInProof = null;
    final Proof mProof;
    final List<String> mLog = new ArrayList<String>();

    public static Prover findProverFor(Proof proof) {
        switch (proof.getType()) {
            case Proof.PROOF_TYPE_TWITTER: return new Twitter(proof);
            case Proof.PROOF_TYPE_GITHUB: return new GitHub(proof);
            case Proof.PROOF_TYPE_DNS: return new DNS(proof);
            case Proof.PROOF_TYPE_WEB_SITE: return new Website(proof);
            case Proof.PROOF_TYPE_HACKERNEWS: return new HackerNews(proof);
            case Proof.PROOF_TYPE_COINBASE: return new Coinbase(proof);
            case Proof.PROOF_TYPE_REDDIT: return new Reddit(proof);
            default: return null;
        }
    }

    public Prover(Proof proof) {
        mProof = proof;
    }

    abstract public boolean fetchProofData();

    public String getPgpMessage() {
        return mPgpMessage;
    }

    public boolean checkFingerprint(String fingerprint) {
        return fingerprint.equalsIgnoreCase(mFingerprintUsedInProof);
    }

    public boolean validate(String decryptedMessage) {
        return mPayload.equals(decryptedMessage);
    }

    public List<String> getLog() {
        return mLog;
    }

    JSONObject readSig(String sigId) throws JSONException, KeybaseException {

        // fetch the sig
        JSONObject sigJSON = Search.getFromKeybase("_/api/1.0/sig/get.json?sig_id=", sigId);
        mLog.add("Successfully retrieved sig from Keybase");

        sigJSON = JWalk.getArray(sigJSON, "sigs").getJSONObject(0);
        mPayload = JWalk.getString(sigJSON, "payload_json");
        mPgpMessage = JWalk.getString(sigJSON, "sig");
        mFingerprintUsedInProof = JWalk.getString(sigJSON, "fingerprint");

        mLog.add("Extracted payload & message from sig");

        return sigJSON;
    }

    public boolean rawMessageCheckRequired() {
        return false;
    }

    public boolean checkRawMessageBytes(InputStream in) {
        try {
            MessageDigest digester = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int byteCount;
            while ((byteCount = in.read(buffer)) > 0) {
                digester.update(buffer, 0, byteCount);
            }
            String digest = Base64.encodeToString(digester.digest(), Base64.URL_SAFE);
            if (!digest.startsWith(mShortenedMessageHash)) {
                mLog.add("Proof post doesn’t contain correct encoded message.");
                return false;
            }
            return true;
        } catch (NoSuchAlgorithmException e) {
            mLog.add("SHA-256 not available");
        } catch (IOException e) {
            mLog.add("Error checking raw message: " + e.getLocalizedMessage());
        }
        return false;
    }

    public String dnsTxtCheckRequired() {
        return null;
    }
    public boolean checkDnsTxt(List<List<byte[]>> records) {
        return false;
    }

    /* A proof narrative needs the following strings:
     *  a Url for the actual proof document (may be null, e.g. for DNS)
     *  a Url for the person’s presence at the service  e.g. https://twitter.com/timbray
     *  a name for the person's presence, e.g. twitter.com/timbray
     */
    public String getProofUrl() throws KeybaseException {
        return mProof.getHumanUrl();
    }
    public String getPresenceUrl() throws KeybaseException {
        return mProof.getServiceUrl();
    }
    public String getPresenceLabel() throws  KeybaseException {
        String answer = mProof.getServiceUrl();
        try {
            URL u = new URL(answer);
            answer = u.getHost() + u.getPath();
        } catch (MalformedURLException e) {
        }
        return answer;
    }
}

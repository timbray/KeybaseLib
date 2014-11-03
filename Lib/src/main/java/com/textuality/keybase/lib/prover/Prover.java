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

import com.textuality.keybase.lib.JWalk;
import com.textuality.keybase.lib.KeybaseException;
import com.textuality.keybase.lib.Proof;
import com.textuality.keybase.lib.Search;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Supports proof verification.  How to use:
 * 1. call fetchProofData(), which will exhibit network latency. If it returns false,
 *    an explanation can be found in the log.
 * 2. fetch the PGP message with getPgpMessage, check that it’s signed with the right fingerprint
 * 3. call rawMessageCheckRequired() and if it returns true, feed the raw (de-armored) bytes
 *    of the message to checkRawMessageBytes(). If it returns null that’s OK.  Otherwise it
 *    returns a message suitable for public display as to what went wrong. This may
 *    exhibit crypto latency.
 * 4. Pass the message to validate(), which should have no real latency
 */
public abstract class Prover {

    String mPgpMessage;
    String mPayload;
    final Proof mProof;
    final List<String> mLog = new ArrayList<String>();

    public static Prover findProverFor(Proof proof) {
        switch (proof.getType()) {
            case Proof.PROOF_TYPE_TWITTER: return new Twitter(proof);
            case Proof.PROOF_TYPE_GITHUB: return new GitHub(proof);
            case Proof.PROOF_TYPE_DNS: return null;
            case Proof.PROOF_TYPE_WEB_SITE: return new Website(proof);
            case Proof.PROOF_TYPE_HACKERNEWS: return null;
            case Proof.PROOF_TYPE_COINBASE: return null;
            case Proof.PROOF_TYPE_REDDIT: return null;
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

        mLog.add("Extracted payload & message from sig");

        return sigJSON;
    }

    public boolean rawMessageCheckRequired() {
        return false;
    }

    public String checkRawMessageBytes(InputStream in) {
        return null;
    }
}

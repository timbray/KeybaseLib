package com.textuality.keybase.lib.prover;

import com.textuality.keybase.lib.JWalk;
import com.textuality.keybase.lib.KeybaseException;
import com.textuality.keybase.lib.Proof;
import com.textuality.keybase.lib.Search;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Supports proof verification.  How to use:
 * 1. call fetchProofData(), which will exhibit network latency. If it returns false,
 *    an explanation can be found in the log.
 * 2. decrypt the PGP message, check that it’s signed with the right fingerprint
 * 3. Pass the message to validate(), which may exhibit crypto latency
 */
public abstract class Prover {

    String mPgpMessage;
    String mPayload;
    final Proof mProof;
    final List<String> mLog = new ArrayList<String>();

    public static Prover findProverFor(Proof proof) {
        switch (proof.getType()) {
            case Proof.PROOF_TYPE_TWITTER: return null;
            case Proof.PROOF_TYPE_GITHUB: return new GitHub(proof);
            case Proof.PROOF_TYPE_DNS: return null;
            case Proof.PROOF_TYPE_WEB_SITE: return null;
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
}

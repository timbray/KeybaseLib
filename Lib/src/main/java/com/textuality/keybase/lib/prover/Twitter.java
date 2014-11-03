package com.textuality.keybase.lib.prover;

import android.util.Base64;
import android.util.Log;

import com.textuality.keybase.lib.JWalk;
import com.textuality.keybase.lib.KeybaseException;
import com.textuality.keybase.lib.Proof;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Twitter extends Prover {

    private String mShortenedMessageHash = null;

    public Twitter(Proof proof) {
        super(proof);
    }

    @Override
    public boolean fetchProofData() {

        try {
            JSONObject sigJSON = readSig(mProof.getSigId());

            // the magic string is the base64 of the SHA of the raw message
            mShortenedMessageHash = JWalk.getString(sigJSON, "sig_id_short");

            // find the tweet's url and fetch it
            String tweetUrl = mProof.getProofUrl();
            Fetch fetch = new Fetch(tweetUrl);
            String problem = fetch.problem();
            if (problem != null) {
                mLog.add(problem);
                return false;
            }

            // now let's dig through the tweet
            String tweet = fetch.getBody();

            // make sure we’re looking only through the header
            int index1 = tweet.indexOf("</head>");
            if (index1 == -1) {
                mLog.add("</head> not found in proof tweet");
                mLog.add("Proof tweet is malformed.");
                return false;
            }
            tweet = tweet.substring(0, index1);

            index1 = tweet.indexOf("<title>");
            int index2 = tweet.indexOf("</title>");
            if (index1 == -1 || index2 == -1 || index1 >= index2) {
                mLog.add("Bogus head locations: " + index1 + "/" + index2);
                mLog.add("Unable to find proof tweet header.");
                return false;
            }

            // ensure the magic string appears in the tweet’s <title>
            tweet = tweet.substring(index1, index2);
            if (tweet.contains(mShortenedMessageHash)) {
                return true;
            } else {
                mLog.add("Encoded message not found in proof tweet.");
                return false;
            }


        } catch (KeybaseException e) {
            mLog.add("Keybase API problem: " + e.getLocalizedMessage());
            return false;
        } catch (JSONException e) {
            mLog.add("Broken JSON message: " + e.getLocalizedMessage());
            return false;
        }
    }

    @Override
    public boolean rawMessageCheckRequired() {
        return true;
    }

    @Override
    public String checkRawMessageBytes(InputStream in) {
        try {
            MessageDigest digester = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int byteCount;
            while ((byteCount = in.read(buffer)) > 0) {
                digester.update(buffer, 0, byteCount);
            }
            String digest = Base64.encodeToString(digester.digest(), Base64.URL_SAFE);
            if (!digest.startsWith(mShortenedMessageHash)) {
                return "Proof tweet doesn’t contain correct encoded message.";
            }
        } catch (NoSuchAlgorithmException e) {
            return "SHA-256h has not available";
        } catch (IOException e) {
            return "Error checking raw message: " + e.getLocalizedMessage();
        }
        return null;
    }
}

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
        } catch (JSONException e) {
            mLog.add("Broken JSON message: " + e.getLocalizedMessage());
        }
        return false;
    }

    @Override
    public boolean rawMessageCheckRequired() {
        return true;
    }

}

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

import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;

public class Twitter extends Prover {

    public Twitter(Proof proof) {
        super(proof);
    }

    @Override
    public boolean fetchProofData() {

        String tweetUrl = null;
        try {
            JSONObject sigJSON = readSig(mProof.getSigId());

            // the magic string is the base64 of the SHA of the raw message
            mShortenedMessageHash = JWalk.getString(sigJSON, "sig_id_short");

            // find the tweet's url and fetch it
            tweetUrl = mProof.getProofUrl();
            Fetch fetch = new Fetch(tweetUrl);
            String problem = fetch.problem();
            if (problem != null) {
                mLog.add(problem);
                return false;
            }

            // Paranoid Interlude:
            // 1. It has to be a tweet https://twitter.com/<nametag>/status/\d+
            // 2. the magic string has to appear in the <title>; we’re worried that
            //    someone could @-reply and fake us out with the magic string down in someone
            //    else’s tweet

            URL suspectUrl = new URL(tweetUrl);
            String scheme = suspectUrl.getProtocol();
            String host = suspectUrl.getHost();
            String path = suspectUrl.getPath();
            String nametag = mProof.getNametag();
            if (!(scheme.equals("https") &&
                    host.equals("twitter.com") &&
                    path.startsWith("/" + nametag + "/status/") &&
                    endsWithDigits(path))) {
                mLog.add("Unacceptable Twitter proof Url: " + tweetUrl);
            }

            // dig through the tweet to find the magic string in the <title>
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
        } catch (MalformedURLException e) {
            mLog.add("Unparsable tweet URL: " + tweetUrl);
        }
        return false;
    }

    private boolean endsWithDigits(String path) {
        int i = path.length() - 1;
        while (i >= 0 && Character.isDigit(path.charAt(i))) {
            i--;
        }
        return ('/' == path.charAt(i));
    }

    @Override
    public boolean rawMessageCheckRequired() {
        return true;
    }

}

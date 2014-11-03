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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HackerNews extends Prover {

    public HackerNews(Proof proof) {
        super(proof);
    }

    @Override
    public boolean fetchProofData() {

        try {
            JSONObject sigJSON = readSig(mProof.getSigId());

            // the magic string is the base64 of the SHA of the raw message
            mShortenedMessageHash = JWalk.getString(sigJSON, "sig_id_short");

            // The api form is off at firebasio, so we’ll use the proof URL
            String hnUrl = mProof.getProofUrl();

            Fetch fetch = new Fetch(hnUrl);
            String problem = fetch.problem();
            if (problem != null) {
                mLog.add(problem);
                return false;
            }

            // paranoia. source has to have the right host and have malgorithms in the path
            String nametag = mProof.getmNametag();
            URL url = new URL(hnUrl);
            String scheme = url.getProtocol();
            String host = url.getHost();
            if ((!(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) ||
                    (!host.equalsIgnoreCase("news.ycombinator.com")) ||
                    (!hnUrl.contains("id=" + nametag))) {
                mLog.add("Proof either doesn’t come from news.ycombinator.com or isn’t specific to " + nametag);
                return false;
            }

            if (!fetch.getBody().contains(mShortenedMessageHash)) {
                mLog.add("Hacker News post doesn’t contain signed PGP message");
                return false;
            }

            return true;

        } catch (KeybaseException e) {
            mLog.add("Keybase API problem: " + e.getLocalizedMessage());
        } catch (JSONException e) {
            mLog.add("Broken JSON message: " + e.getLocalizedMessage());
        } catch (MalformedURLException e) {
            mLog.add("Malformed URL for proof post");
        }
        return false;
    }

    @Override
    public boolean rawMessageCheckRequired() {
        return true;
    }

}

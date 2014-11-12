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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;

public class Reddit extends Prover {

    private String mApiUrl = null;

    @Override
    public boolean fetchProofData() {

        try {
            JSONObject sigJSON = readSig(mProof.getSigId());

            // the magic string is the base64 of the SHA of the raw message
            mShortenedMessageHash = JWalk.getString(sigJSON, "sig_id_short");
            mApiUrl = JWalk.getString(sigJSON, "api_url");
            String nametag = mProof.getNametag();

            // fetch the JSON proof
            Fetch fetch = new Fetch(mApiUrl);
            String problem = fetch.problem();
            if (problem != null) {
                mLog.add(problem);
                return false;
            }

            // it’s a bunch of complicated JSON
            JSONArray redditJSON = new JSONArray(fetch.getBody());

            // Paranoid Interlude:
            // Let’s make sure the apiUrl comes from reddit and specifically /r/KeybaseProofs
            //
            String host = (new URL(fetch.getActualUrl())).getHost();
            if (!(host.equals("reddit.com") || host.equals("www.reddit.com"))) {
                mLog.add("Bad host for reddit proof: " + host);
                return false;
            }
            if (!((new URL(mApiUrl)).getPath().startsWith("/r/KeybaseProofs"))) {
                mLog.add("Reddit proof not from /r/KeybaseProofs");
                return false;
            }

            // navigate down through the horrible JSON
            JSONObject payload = redditJSON.getJSONObject(0).
                    getJSONObject("data").
                    getJSONArray("children").
                    getJSONObject(0).
                    getJSONObject("data");

            String author = payload.getString("author");
            if (!(author.equals(nametag))) {
                mLog.add("Author in Reddit proof is not " + nametag);
                return false;
            }

            // verifying that the message appears is hard because something is screwing with
            //  white space.  So we’ll settle for the shortened form
            String title = payload.getString("title");
            if (!title.contains(mShortenedMessageHash)) {
                mLog.add("Reddit proof doesn’t contain signed PGP message");
                return false;
            }

            return true;

        } catch (KeybaseException e) {
            mLog.add("Keybase API problem: " + e.getLocalizedMessage());
        } catch (JSONException e) {
            mLog.add("Broken JSON message: " + e.getLocalizedMessage());
        } catch (MalformedURLException e) {
            mLog.add("Malformed URL for Reddit proof: " + e.getLocalizedMessage());
        }
        return false;
    }

    @Override
    public boolean rawMessageCheckRequired() {
        return true;
    }

    public Reddit(Proof proof) {
        super(proof);
    }

    @Override
    public String getProofUrl() throws KeybaseException {
        return mApiUrl;
    }

    @Override
    public String getPresenceLabel() throws KeybaseException {
        return "/r/KeybaseProofs";
    }

    @Override
    public String getPresenceUrl() throws KeybaseException {
        return "https://reddit.com/r/KeybaseProofs";
    }
}

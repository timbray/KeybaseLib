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
import java.net.Proxy;
import java.net.URL;

public class HackerNews extends Prover {

    @Override
    public boolean fetchProofData(Proxy proxy) {

        try {
            JSONObject sigJSON = readSig(mProof.getSigId(), proxy);

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

            // Paranoid Interlude:
            // Let’s make sure source has right host and there's an id=<nametag> in the path
            //
            String nametag = mProof.getNametag();
            URL url = new URL(hnUrl);
            String scheme = url.getProtocol();
            String host = url.getHost();
            if (!((scheme.equals("http") || scheme.equals("https")) &&
                    host.equals("news.ycombinator.com") &&
                    hnUrl.contains("id=" + nametag))) {
                mLog.add("Proof either doesn’t come from news.ycombinator.com or isn’t specific to " + nametag);
                return false;
            }

            if (!(fetch.getBody().contains(mShortenedMessageHash))) {
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
    public String getPresenceLabel() throws KeybaseException {
        String answer = mProof.getServiceUrl();
        try {
            URL u = new URL(answer);
            answer = u.getHost() + u.getPath() + '?' + u.getQuery();
        } catch (MalformedURLException e) {
            answer = super.getPresenceLabel();
        }
        return answer;
    }

    @Override
    public boolean rawMessageCheckRequired() {
        return true;
    }

    public HackerNews(Proof proof) {
        super(proof);
    }
}

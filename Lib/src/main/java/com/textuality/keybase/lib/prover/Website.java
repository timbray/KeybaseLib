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

import android.util.Log;

import com.textuality.keybase.lib.JWalk;
import com.textuality.keybase.lib.KeybaseException;
import com.textuality.keybase.lib.Proof;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;

public class Website extends Prover {

    public Website(Proof proof) {
        super(proof);
    }

    @Override
    public boolean fetchProofData() {

        try {
            JSONObject sigJSON = readSig(mProof.getSigId());

            // find the .well-known URL
            String wellKnownUrl = JWalk.getString(sigJSON, "api_url");

            // fetch the proof
            Fetch fetch = new Fetch(wellKnownUrl);
            String problem = fetch.problem();
            if (problem != null) {
                mLog.add(problem);
                return false;
            }

            // sanity-check per Keybase guidance
            String actualUrl = fetch.getActualUrl();

            // paranoia. URL has to be of the form https?//<nametag>/.well-known/keybase.txt
            String nametag = mProof.getmNametag();
            URL url = new URL(actualUrl);
            String scheme = url.getProtocol();
            String host = url.getHost();
            if ((!(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) ||
                    (!host.equalsIgnoreCase(nametag))) {
                mLog.add("Proof either doesn’t come from " + nametag + " or isn’t at an HTTP URL");
                return false;
            }

            // verify that message appears in gist
            if (!fetch.getBody().contains(mPgpMessage)) {
                mLog.add("Domain name claiming post doesn’t contain signed PGP message");
                return false;
            }

            return true;

        } catch (KeybaseException e) {
            mLog.add("Keybase API problem: " + e.getLocalizedMessage());
            return false;
        } catch (JSONException e) {
            mLog.add("Broken JSON message: " + e.getLocalizedMessage());
            return false;
        } catch (MalformedURLException e) {
            mLog.add("Malformed proof URL");
            return false;
        }
    }
}

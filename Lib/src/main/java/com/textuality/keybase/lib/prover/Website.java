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

public class Website extends Prover {

    public Website(Proof proof) {
        super(proof);
    }

    @Override
    public boolean fetchProofData(Proxy proxy) {

        try {
            JSONObject sigJSON = readSig(mProof.getSigId(), proxy);

            // find the .well-known URL
            String wellKnownUrl = JWalk.getString(sigJSON, "api_url");

            // fetch the proof
            Fetch fetch = new Fetch(wellKnownUrl);
            String problem = fetch.problem();
            if (problem != null) {
                mLog.add(problem);
                return false;
            }

            String actualUrl = fetch.getActualUrl();

            // Paranoid Interlude:
            // A bad guy who can’t post to the site might still be able to squeeze in a redirect
            //  to somewhere else, so let’s ensure that the data is really coming from the site
            //
            String nametag = mProof.getNametag();
            URL url = new URL(actualUrl);
            String scheme = url.getProtocol();
            String host = url.getHost();
            if (!(scheme.equals("http") || scheme.equals("https")) &&
                    host.equals(nametag)) {
                mLog.add("Proof either doesn’t come from " + nametag + " or isn’t at an HTTP URL");
                return false;
            }

            // verify that message appears in gist
            if (!fetch.getBody().contains(mPgpMessage)) {
                mLog.add("Website claiming post doesn’t contain signed PGP message");
                return false;
            }

            return true;

        } catch (KeybaseException e) {
            mLog.add("Keybase API problem: " + e.getLocalizedMessage());
        } catch (JSONException e) {
            mLog.add("Broken JSON message: " + e.getLocalizedMessage());
        } catch (MalformedURLException e) {
            mLog.add("Malformed proof URL");
        }
        return false;
    }

    @Override
    public String getPresenceLabel() throws KeybaseException {
        return mProof.getNametag();
    }
}

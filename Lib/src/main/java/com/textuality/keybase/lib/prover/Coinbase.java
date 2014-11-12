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

import com.textuality.keybase.lib.KeybaseException;
import com.textuality.keybase.lib.Proof;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;

public class Coinbase extends Prover {

    @Override
    public boolean fetchProofData() {

        try {
            JSONObject sigJSON = readSig(mProof.getSigId());

            String proofUrl = mProof.getProofUrl();

            // fetch the post
            Fetch fetch = new Fetch(proofUrl);
            String problem = fetch.problem();
            if (problem != null) {
                mLog.add(problem);
                return false;
            }

            // Paranoid Interlude:
            // Let’s make sure the URL is of the form https://coinbase.com/<nametag>/public-key
            //  and the actual host after redirects is coinbase
            //
            URL url = new URL(proofUrl);
            if (!(url.getProtocol().equals("https") &&
                    url.getHost().equals("coinbase.com") &&
                    url.getPath().equals("/" + mProof.getHandle() + "/public-key"))) {
                mLog.add("Bogus Coinbase proof URL: " + proofUrl);
                return false;
            }
            url = new URL(fetch.getActualUrl());
            if (!url.getHost().equals("coinbase.com")) {
                mLog.add("Coinbase proof doesn’t come from coinbase.com: " + fetch.getActualUrl());
                return false;
            }

            // verify that message appears in body, which coinbase messes up with \r’s
            String body = fetch.getBody().replace("\r", "");
            if (!body.contains(mPgpMessage)) {
                mLog.add("Coinbase proof doesn’t contain signed PGP message");
                return false;
            }

            return true;

        } catch (KeybaseException e) {
            mLog.add("Keybase API problem: " + e.getLocalizedMessage());
        } catch (JSONException e) {
            mLog.add("Broken JSON message: " + e.getLocalizedMessage());
        } catch (MalformedURLException e) {
            mLog.add("Malformed Coinbase proof URL");
        }
        return false;
    }

    public Coinbase(Proof proof) {
        super(proof);
    }
}

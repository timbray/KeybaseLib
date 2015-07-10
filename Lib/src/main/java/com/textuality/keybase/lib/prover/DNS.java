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

import java.net.Proxy;
import java.util.List;

public class DNS extends Prover {

    private String mDomain = null;

    @Override
    public boolean fetchProofData(Proxy proxy) {

        try {
            JSONObject sigJSON = readSig(mProof.getSigId(), proxy);

            // the magic string is the base64 of the SHA of the raw message
            mShortenedMessageHash = JWalk.getString(sigJSON, "sig_id_short");

            // no fetching!

            return true;

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

    @Override
    public String dnsTxtCheckRequired() {
        return mProof.getNametag();
    }

    @Override
    // Each TXT record is a bunch of byte[] extents
    public boolean checkDnsTxt(List<List<byte[]>> records) {
        for (List<byte[]> record : records) {
            for (byte[] extent : record) {
                if ((new String(extent)).contains(mShortenedMessageHash)) {
                    return true;
                }
            }
        }
        mLog.add("Processed " + records.size() + " TXT records");
        mLog.add("No DNS TXT record for " + mDomain + " contains signed PGP message");
        return false;
    }

    @Override
    public String getProofUrl() throws KeybaseException {
        return null;
    }

    public DNS(Proof proof) {
        super(proof);
    }
}

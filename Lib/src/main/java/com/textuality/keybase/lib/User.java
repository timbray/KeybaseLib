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
package com.textuality.keybase.lib;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.Proxy;
import java.util.Iterator;

public class User {

    private final JSONObject mJson;

    public static User findByUsername(String username, Proxy proxy) throws KeybaseException {
        JSONObject json = Search.getFromKeybase("_/api/1.0/user/lookup.json?username=", username, proxy);
        try {
            json = JWalk.getObject(json, "them");
        } catch (JSONException e) {
            throw KeybaseException.keybaseScrewup(e);
        }
        return new User(json);
    }
    public static String keyForUsername(String username, Proxy proxy) throws KeybaseException {
        return findByUsername(username, proxy).getKey();
    }
    public static User findByFingerprint(String fingerprint, Proxy proxy) throws KeybaseException {
        JSONObject json = Search.getFromKeybase("_/api/1.0/user/lookup.json?key_fingerprint=", fingerprint, proxy);
        try {
            JSONArray them = JWalk.getArray(json, "them");
            if (them.length() != 1) {
                throw KeybaseException.queryScrewup("Key retrieval produced " + them.length() +
                        " results");
            }
            return new User(them.getJSONObject(0));
        } catch (JSONException e) {
            throw KeybaseException.keybaseScrewup(e);
        }
    }
    private User(JSONObject json) {
        mJson = json;
    }
    public String getKey() throws KeybaseException {
        try {
            return JWalk.getString(mJson, "public_keys", "primary", "bundle");
        } catch (JSONException e) {
            throw KeybaseException.keybaseScrewup(e);
        }
    }
    public Iterable<Proof> getProofs() throws KeybaseException {
        try {
            return new ProofIterator(JWalk.getArray(mJson, "proofs_summary", "all"));
        } catch (JSONException e) {
            throw KeybaseException.keybaseScrewup(e);
        }
    }

    class ProofIterator implements Iterable<Proof>, Iterator<Proof> {

        private final JSONArray mArray;
        private int mLastIndex = -1;

        public ProofIterator(JSONArray json) {
            mArray = json;
        }

        @Override
        public boolean hasNext() {
            return (mLastIndex < (mArray.length() - 1));
        }

        @Override
        public Proof next() {
            try {
                return new Proof(mArray.getJSONObject(++mLastIndex));
            } catch (JSONException e) {
                throw new RuntimeException(KeybaseException.keybaseScrewup(e));
            } catch (KeybaseException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void remove() {
            throw new RuntimeException("ProofIterator doesn’t support remove");
        }

        @Override
        public Iterator<Proof> iterator() {
            return this;
        }
    }
}

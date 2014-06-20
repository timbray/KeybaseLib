package com.textuality.keybase.lib;

import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class User {

    private final JSONObject mJson;

    public static User findByUsername(String username) throws KeybaseException {
        JSONObject json = Search.getFromKeybase("_/api/1.0/user/lookup.json?username=", username);
        try {
            json = JWalk.getObject(json, "them");
        } catch (JSONException e) {
            throw KeybaseException.keybaseScrewup(e);
        }
        return new User(json);
    }
    public static String keyForUsername(String username) throws KeybaseException {
        return findByUsername(username).getKey();
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

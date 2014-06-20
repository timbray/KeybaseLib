package com.textuality.keybase.lib;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class Search {
    
    private static final String TAG = "KEYBASE-LIB";

    public static Iterable<Match> search(String query) throws KeybaseException {
        JSONObject result = getFromKeybase("_/api/1.0/user/autocomplete.json?q=", query);
        try {
            return new MatchIterator(JWalk.getArray(result, "completions"));
        } catch (JSONException e) {
            throw KeybaseException.keybaseScrewup(e);
        }
    }

    public static JSONObject getFromKeybase(String path, String query) throws KeybaseException {
        try {
            String url = "https://keybase.io/" + path + URLEncoder.encode(query, "utf8");
            System.out.println(" URL: " + url);

            URL realUrl = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) realUrl.openConnection();
            conn.setConnectTimeout(5000); // TODO: Reasonable values for keybase
            conn.setReadTimeout(25000);
            conn.connect();
            int response = conn.getResponseCode();
            if (response >= 200 && response < 300) {
                String text = snarf(conn.getInputStream());
                try {
                    JSONObject json = new JSONObject(text);
                    if (JWalk.getInt(json, "status", "code") != 0) {
                        throw KeybaseException.queryScrewup("Keybase.io query failed: " + path + "?" + query);
                    }
                    return json;
                } catch (JSONException e) {
                    throw KeybaseException.keybaseScrewup(e);
                }
            } else {
                String message = snarf(conn.getErrorStream());
                throw KeybaseException.networkScrewup("Keybase.io query error (status=" + response + "): " + message);
            }
        } catch (Exception e) {
            throw KeybaseException.networkScrewup(e);
        }
    }

    private static String snarf(InputStream in) 
            throws IOException {
        byte[] buf = new byte[1024];
        int count = 0;
        ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
        while ((count = in.read(buf)) != -1) 
            out.write(buf, 0, count);
        return out.toString();
    }

    static class MatchIterator implements Iterable<Match>, Iterator<Match> {

        private final JSONArray mMatches;
        private int mLastIndex;
        private Match mNextMatch;

        public MatchIterator(JSONArray matches) throws KeybaseException {
            Log.d(TAG, "match count=" + matches.length());
            mMatches = matches;
            mLastIndex = -1;
            mNextMatch = null;
            hasNext();
        }
        
        // caches mNextMatch but not the index
        private int findNext() {
            try {
                for (int index = mLastIndex + 1; index < mMatches.length(); index++) {
                    mNextMatch = new Match(mMatches.getJSONObject(index));
                    if (mNextMatch.hasKey()) {
                        return index;
                    }
                }
            } catch (JSONException e) {
                throw new RuntimeException(KeybaseException.keybaseScrewup(e));
            } catch (KeybaseException e) {
                throw new RuntimeException(e);
            }
           return -1;
        }

        @Override
        public boolean hasNext() {
            return (findNext() != -1);
        }

        @Override
        public Match next() {
            mLastIndex = findNext();
            if (mLastIndex == -1) {
                throw new NoSuchElementException();
            } else {
                return mNextMatch;
            }
        }

        @Override
        public void remove() {
            throw new RuntimeException("UserIterator.remove() not supported");            
        }

        @Override
        public Iterator<Match> iterator() {
            return this;
        }
    }
}

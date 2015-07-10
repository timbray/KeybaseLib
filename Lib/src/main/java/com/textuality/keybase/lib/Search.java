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

import android.util.Log;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Proxy;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

public class Search {
    
    private static final String TAG = "KEYBASE-LIB";

    public static Iterable<Match> search(String query, Proxy proxy) throws KeybaseException {
        JSONObject result = getFromKeybase("_/api/1.0/user/autocomplete.json?q=", query, proxy);
        try {
            return new MatchIterator(JWalk.getArray(result, "completions"));
        } catch (JSONException e) {
            throw KeybaseException.keybaseScrewup(e);
        }
    }

    public static JSONObject getFromKeybase(String path, String query, Proxy proxy) throws KeybaseException {
        try {
            String url = "https://keybase.io/" + path + URLEncoder.encode(query, "utf8");

            URL realUrl = new URL(url);

            OkHttpClient client = new OkHttpClient();
            client.setProxy(proxy);

            if (proxy != null) {
                client.setConnectTimeout(30000, TimeUnit.MILLISECONDS);
                client.setReadTimeout(40000, TimeUnit.MILLISECONDS);
            } else {
                client.setConnectTimeout(5000, TimeUnit.MILLISECONDS); // TODO: Reasonable values for keybase
                client.setReadTimeout(25000, TimeUnit.MILLISECONDS);
            }

            Response resp = client.newCall(new Request.Builder().url(realUrl).build()).execute();

            int response = resp.code();

            String text = resp.body().string();

            if (response >= 200 && response < 300) {
                try {
                    JSONObject json = new JSONObject(text);
                    if (JWalk.getInt(json, "status", "code") != 0) {
                        throw KeybaseException.queryScrewup("Keybase.io query failed: " + path + "?" + query +
                                " using proxy: " + proxy);
                    }
                    return json;
                } catch (JSONException e) {
                    throw KeybaseException.keybaseScrewup(e);
                }
            } else {
                throw KeybaseException.networkScrewup("Keybase.io query error (status=" + response + "): " + text);
            }
        } catch (Exception e) {
            throw KeybaseException.networkScrewup(e);
        }
    }

    public static String snarf(InputStream in)
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

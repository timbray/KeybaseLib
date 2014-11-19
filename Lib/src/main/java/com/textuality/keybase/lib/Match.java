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

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

public class Match {
    private final JSONObject mComponents;

    public Match(JSONObject json) throws KeybaseException {
        try {
            mComponents = JWalk.getObject(json, "components");
        } catch (JSONException e) {
            throw KeybaseException.keybaseScrewup(e);
        }
    }
    public boolean hasKey() throws KeybaseException {
        try {
            return (JWalk.optObject(mComponents, "key_fingerprint") != null);
        } catch (JSONException e) {
            throw KeybaseException.keybaseScrewup(e);
        }
    }
    public String getKeyID() throws KeybaseException {
        String fingerprint = getFingerprint();
        if (fingerprint.length() > 16) {
            fingerprint = fingerprint.substring(fingerprint.length() - 16);
        }

        return fingerprint.replace(" ", "");
    }
    public String getUsername() throws KeybaseException{
        try {
            return JWalk.getString(mComponents, "username", "val");
        } catch (JSONException e) {
            throw KeybaseException.keybaseScrewup(e);
        }
    }
    public String getFullName() throws KeybaseException {
        try {
            return JWalk.getString(mComponents, "full_name", "val");
        } catch (JSONException e) {
            return null;
        }
    }
    public String getFingerprint() throws KeybaseException {
        try {
            return JWalk.getString(mComponents, "key_fingerprint", "val");
        } catch (JSONException e) {
            throw KeybaseException.keybaseScrewup(e);
        }
    }
    public int getAlgorithmId() throws KeybaseException {
        try {
            return JWalk.getInt(mComponents, "key_fingerprint", "algo");
        } catch (JSONException e) {
            throw KeybaseException.keybaseScrewup(e);
        }
    }
    public int getBitStrength() throws KeybaseException {
        try {
            return JWalk.getInt(mComponents, "key_fingerprint", "nbits");
        } catch (JSONException e) {
            throw KeybaseException.keybaseScrewup(e);
        }
    }
    public List<String> getProofLabels() {
        ArrayList<String> labels = new ArrayList<String>();
        try {
            labels.add("Twitter: @" + JWalk.getString(mComponents, "twitter", "val"));
        } catch (JSONException e) {
            // s'OK
        }
        try {
            labels.add("GitHub: " + JWalk.getString(mComponents, "github", "val"));
        } catch (JSONException e) {
            // s'OK
        }
        try {
            labels.add("Reddit: " + JWalk.getString(mComponents, "reddit", "val"));
        } catch (JSONException e) {
            // s'OK
        }
        try {
            labels.add("Hacker News: " + JWalk.getString(mComponents, "hackernews", "val"));
        } catch (JSONException e) {
            // s'OK
        }
        try {
            labels.add("Coinbase: " + JWalk.getString(mComponents, "coinbase", "val"));
        } catch (JSONException e) {
            // s'OK
        }
        try {
            JSONArray sites = JWalk.getArray(mComponents, "websites");
            Hashtable<String, Integer> uniqueNames = new Hashtable<String, Integer>();
            int i;
            for (i = 0; i < sites.length(); i++) {
                uniqueNames.put(JWalk.getString(sites.getJSONObject(i), "val"), 1);
            }
            Set<String> names = uniqueNames.keySet();
            StringBuilder label = new StringBuilder("Web: ");
            i = 0;
            for (String name : names) {
                label.append(name);
                if (i < names.size() - 1) {
                    label.append(", ");
                }
                i++;
            }
            labels.add(label.toString());
        } catch (JSONException e) {
            // s'OK
        }
        return labels;
    }
}

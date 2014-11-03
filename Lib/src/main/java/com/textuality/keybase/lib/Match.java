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
import java.util.List;

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
            labels.add("twitter.com/" + JWalk.getString(mComponents, "twitter", "val"));
        } catch (JSONException e) {
            // s'OK
        }
        try {
            labels.add("github.com/" + JWalk.getString(mComponents, "github", "val"));
        } catch (JSONException e) {
            // s'OK
        }
        try {
            JSONArray sites = JWalk.getArray(mComponents, "websites");
            labels.add(JWalk.getString(sites.getJSONObject(0), "val"));
        } catch (JSONException e) {
            // s'OK
        }
        return labels;
    }
}

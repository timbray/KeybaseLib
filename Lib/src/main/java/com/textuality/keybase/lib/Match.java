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

        return fingerprint.replace(" ", "").toUpperCase();
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
            throw KeybaseException.keybaseScrewup(e);
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

package com.textuality.keybase.lib;

import org.json.JSONException;
import org.json.JSONObject;

public class Match {
    private final JSONObject mJson;

    public Match(JSONObject json) {
        mJson = json;
    }
    public boolean hasKey() throws KeybaseException {
        try {
            return (JWalk.optObject(mJson, "components", "key_fingerprint") != null);
        } catch (JSONException e) {
            throw KeybaseException.keybaseScrewup(e);
        }
    }
    public String getKeyID() throws KeybaseException {
        String fingerprint = fingerprint();
        if (fingerprint.length() > 16) {
            fingerprint = fingerprint.substring(fingerprint.length() - 16);
        }
        return fingerprint;
    }
    public String getUsername() throws KeybaseException{
        try {
            return JWalk.getString(mJson, "components", "username", "val");
        } catch (JSONException e) {
            throw KeybaseException.keybaseScrewup(e);
        }
    }
    private String fingerprint() throws KeybaseException {
        try {
            String fingerprint = JWalk.getString(mJson, "components", "key_fingerprint", "val");
            return fingerprint.replace(" ", "").toUpperCase();
        } catch (JSONException e) {
            throw KeybaseException.keybaseScrewup(e);
        }

    }
}

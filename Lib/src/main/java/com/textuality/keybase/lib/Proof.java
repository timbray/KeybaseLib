package com.textuality.keybase.lib;

import org.json.JSONException;
import org.json.JSONObject;

public class Proof {
    
    private final JSONObject mJson;
    
    public Proof(JSONObject json) {
        mJson = json;
    }
    private String getField(String name) throws KeybaseException {
        try {
            return JWalk.getString(mJson, name);
        } catch (JSONException e) {
            throw KeybaseException.keybaseScrewup(e);
        }
    }
    public String getType() throws KeybaseException {
        return getField("proof_type");
    }
    public String getUrl() throws KeybaseException {
        return getField("proof_url");
    }
    
}

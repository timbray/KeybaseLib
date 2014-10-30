package com.textuality.keybase.lib;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Hashtable;

public class Proof {
    
    private final JSONObject mJson;
    private final String mNametag;
    private final int mProofType;

    private final static Hashtable<String, Integer> sProofTypes;
    public final static int PROOF_TYPE_TWITTER = 0;
    public final static int PROOF_TYPE_GITHUB = 1;
    public final static int PROOF_TYPE_DNS = 2;
    public final static int PROOF_TYPE_WEB_SITE = 3;
    public final static int PROOF_TYPE_HACKERNEWS = 4;
    public final static int PROOF_TYPE_COINBASE = 5;
    public final static int PROOF_TYPE_REDDIT = 6;

    static {
        sProofTypes = new Hashtable<String, Integer>();
        sProofTypes.put("twitter", PROOF_TYPE_TWITTER);
        sProofTypes.put("github", PROOF_TYPE_GITHUB);
        sProofTypes.put("dns", PROOF_TYPE_DNS);
        sProofTypes.put("generic_web_site", PROOF_TYPE_WEB_SITE);
        sProofTypes.put("hackernews", PROOF_TYPE_HACKERNEWS);
        sProofTypes.put("coinbase", PROOF_TYPE_COINBASE);
        sProofTypes.put("reddit", PROOF_TYPE_REDDIT);
    }

    public Proof(JSONObject json) throws KeybaseException {
        mJson = json;
        mNametag = getField("nametag");
        mProofType = sProofTypes.get(getField("proof_type"));
    }

    private String getField(String name) throws KeybaseException {
        try {
            return JWalk.getString(mJson, name);
        } catch (JSONException e) {
            throw KeybaseException.keybaseScrewup(e);
        }
    }

    public String getHandle() {
        String handle = mNametag;
        if ("twitter".equals(mProofType)) {
            handle = "@" + mNametag;
        } else if ("github".equals(mProofType)) {
            handle = "github.com/" + mNametag;
        }
        return handle;
    }

    public String getId() throws KeybaseException {
        return getField("proof_id");
    }

    public int getType() {
        return mProofType;
    }

    public String getServiceUrl() throws KeybaseException {
        return getField("service_url");
    }

    public String toString() {
        return mJson.toString();
    }
    
}

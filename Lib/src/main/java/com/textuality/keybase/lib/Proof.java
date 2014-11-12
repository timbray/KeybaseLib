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

import org.json.JSONException;
import org.json.JSONObject;

public class Proof {

    private final JSONObject mJson;
    private final String mNametag;
    private final int mProofType;

    public final static int PROOF_TYPE_UNKNOWN = -1;
    public final static int PROOF_TYPE_COINBASE = 0;
    public final static int PROOF_TYPE_DNS = 1;
    public final static int PROOF_TYPE_GITHUB = 2;
    public final static int PROOF_TYPE_HACKERNEWS = 3;
    public final static int PROOF_TYPE_REDDIT = 4;
    public final static int PROOF_TYPE_TWITTER = 5;
    public final static int PROOF_TYPE_WEB_SITE = 6;

    public Proof(JSONObject json) throws KeybaseException {
        // pre-compute these ones because they’re used in multiple popular methods
        mJson = json;
        mNametag = getField("nametag");
        mProofType = findType(getField("proof_type"));
    }

    private String getField(String name) throws KeybaseException {
        try {
            return JWalk.getString(mJson, name);
        } catch (JSONException e) {
            throw KeybaseException.keybaseScrewup(e);
        }
    }

    public String getNametag() {
        return mNametag;
    }

    public String getHandle() {
        String handle = mNametag;
        switch (mProofType) {
            case PROOF_TYPE_TWITTER:
                handle = "@" + mNametag;
                break;
            case PROOF_TYPE_GITHUB:
                handle = "github.com/" + mNametag;
                break;
            case PROOF_TYPE_COINBASE:
                handle = mNametag.substring("coinbase/".length());
                break;
        }
        return handle;
    }

    public String getId() throws KeybaseException {
        return getField("proof_id");
    }

    public String getSigId() throws KeybaseException {
        return getField("sig_id");
    }

    public int getType() {
        return mProofType;
    }

    public String getPrettyName() {
        switch (mProofType) {
            case PROOF_TYPE_COINBASE: return "Coinbase";
            case PROOF_TYPE_DNS: return "DNS";
            case PROOF_TYPE_GITHUB: return "GitHub";
            case PROOF_TYPE_HACKERNEWS: return "Hacker News";
            case PROOF_TYPE_REDDIT: return "Reddit";
            case PROOF_TYPE_TWITTER: return "Twitter";
            case PROOF_TYPE_WEB_SITE: return "Web site";
            default: return "Unknown";
        }
    }

    public String getHumanUrl() throws KeybaseException {
        return getField("human_url");
    }
    public String getServiceUrl() throws KeybaseException {
        return getField("service_url");
    }

    public String getProofUrl() throws KeybaseException {
        return getField("proof_url");
    }

    public String toString() {
        return mJson.toString();
    }

    private int findType(String pType) {
        switch (pType.charAt(0)) {
            case 'c': return PROOF_TYPE_COINBASE;
            case 'd': return PROOF_TYPE_DNS;
            case 'g': {
                switch (pType.charAt(1)) {
                    case 'e': return PROOF_TYPE_WEB_SITE;
                    case 'i': return PROOF_TYPE_GITHUB;
                    default: return PROOF_TYPE_UNKNOWN;
                }
            }
            case 'h': return PROOF_TYPE_HACKERNEWS;
            case 'r': return PROOF_TYPE_REDDIT;
            case 't': return PROOF_TYPE_TWITTER;
            default: return PROOF_TYPE_UNKNOWN;
        }
    }
}

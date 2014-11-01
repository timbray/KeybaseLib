package com.textuality.keybase.lib.prover;

import com.textuality.keybase.lib.JWalk;
import com.textuality.keybase.lib.KeybaseException;
import com.textuality.keybase.lib.Proof;
import com.textuality.keybase.lib.Search;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;

public class GitHub extends Prover {

    public GitHub(Proof proof) {
        super(proof);
    }
    private static final String[] sApiBases = {
            "https://gist.githubusercontent.com/", "https://gist.github.com/"
    };

    @Override
    public boolean fetchProofData() {

        try {
            JSONObject sigJSON = readSig(mProof.getSigId());

            // find the URL for the markdown form of the gist
            String markdownURL = JWalk.getString(sigJSON, "api_url");
            String nametag = mProof.getmNametag();

            // fetch the gist
            Fetch fetch = new Fetch(markdownURL);
            String problem = fetch.problem();
            if (problem != null) {
                mLog.add(problem);
                return false;
            }

            // sanity-check per Keybase guidance
            String actualUrl = fetch.getActualUrl();
            String apiNametag = null;
            for (String base : sApiBases) {
                if (actualUrl.startsWith(base)) {
                    apiNametag = actualUrl.substring(base.length());
                    apiNametag = apiNametag.substring(0, apiNametag.indexOf('/'));
                }
            }
            if ((apiNametag == null) || !apiNametag.equalsIgnoreCase(nametag)) {
                mLog.add("Bogus GitHub API URL: " + markdownURL);
                return false;
            }

            // verify that message appears in gist
            if (!fetch.getBody().contains(mPgpMessage)) {
                mLog.add("GitHub gist doesn’t contain signed PGP message");
                return false;
            }

            return true;

        } catch (KeybaseException e) {
            mLog.add("Keybase API problem: " + e.getLocalizedMessage());
            return false;
        } catch (JSONException e) {
            mLog.add("Broken JSON message: " + e.getLocalizedMessage());
            return false;
        }
    }
}

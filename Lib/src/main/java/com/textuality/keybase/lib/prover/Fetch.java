package com.textuality.keybase.lib.prover;

import com.textuality.keybase.lib.Search;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

/**
 * Created by twbray on 14-11-01.
 */
public class Fetch {

    private String mProblem = null;
    private String mActualUrl = null;
    private String mBody = null;

    public Fetch(String urlString) {

        try {
            HttpURLConnection conn = null;
            int status = 0;
            while (true) {
                mActualUrl = urlString;
                URL url = new URL(urlString);
                conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5000); // TODO: Reasonable values
                conn.setReadTimeout(25000);
                conn.connect();
                status = conn.getResponseCode();
                if (status == 301) {
                    Map<String, List<String>> headers = conn.getHeaderFields();
                    urlString = headers.get("Location").get(0);
                } else {
                    break;
                }
            }
            if (status >= 200 && status < 300) {
                mBody = Search.snarf(conn.getInputStream());
            } else {
                mProblem = "Fetch failed, status " + status + ": " + Search.snarf(conn.getErrorStream());
            }

        } catch (MalformedURLException e) {
            mProblem = "Bad URL: " + urlString;
            return;
        } catch (IOException e) {
            mProblem = "Network error: " + e.getLocalizedMessage();
            return;
        }

    }

    public String problem() {
        return mProblem;
    }
    public String getActualUrl() {
        return mActualUrl;
    }
    public String getBody() {
        return mBody;
    }

}

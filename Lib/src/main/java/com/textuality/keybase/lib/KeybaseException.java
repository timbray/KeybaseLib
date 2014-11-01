package com.textuality.keybase.lib;

import org.json.JSONException;

public class KeybaseException extends Exception {

    private static final long serialVersionUID = 2451035852671678652L;

    private KeybaseException(Throwable e, String message) {
        super(message, e);
    }
    private KeybaseException(String message) {
        super(message);
    }

    public static KeybaseException keybaseScrewup(JSONException e) {
        return new KeybaseException(e, "JSON error in Keybase query");
    }
    public static KeybaseException networkScrewup(String message) {
        return new KeybaseException(message);
    }
    public static KeybaseException networkScrewup(Exception e) {
        return new KeybaseException(e, "Network error attempting Keybase query");
    }
    public static KeybaseException queryScrewup(String message) {
        return new KeybaseException(message);
    }

}

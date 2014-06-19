package com.textuality.keybase.lib;

import org.json.JSONException;

public class KeybaseException extends Throwable {

    private static final long serialVersionUID = 2451035852671678652L;

    private KeybaseException(Throwable e, String message) {
        super(message, e);
    }
    private KeybaseException(String message) {
        super(message);
    }

    static KeybaseException keybaseScrewup(JSONException e) {
        return new KeybaseException(e, "JSON error in Keybase query");
    }
    static KeybaseException networkScrewup(String message) {
        return new KeybaseException(message);
    }
    static KeybaseException networkScrewup(Exception e) {
        return new KeybaseException(e, "Network error attempting Keybase query");
    }
    static KeybaseException queryScrewup(String message) {
        return new KeybaseException(message);
    }

}

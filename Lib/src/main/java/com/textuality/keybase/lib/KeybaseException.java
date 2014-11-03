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

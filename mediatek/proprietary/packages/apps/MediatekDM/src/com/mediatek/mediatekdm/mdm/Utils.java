package com.mediatek.mediatekdm.mdm;

public class Utils {
    public static String join(String seperator, String... segments) {
        if (segments.length == 0) {
            return null;
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append(segments[0]);
            for (int i = 1; i < segments.length; ++i) {
                sb.append(seperator).append(segments[i]);
            }
            return sb.toString();
        }
    }
}

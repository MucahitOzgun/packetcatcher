package com.company.sql.util;

public class StringUtil {

    public static String cut(String str, int maxLength) {
        if (str != null && str.length() > maxLength) {
            str = str.substring(0, maxLength);
        }
        return str;
    }

    public static String removeZeroBytes(String str) {
        StringBuffer buffer = new StringBuffer();
        for (char c : str.toCharArray()) {
            if (c != 0) {
                buffer.append(c);
            }
        }
        return buffer.toString();
    }
}

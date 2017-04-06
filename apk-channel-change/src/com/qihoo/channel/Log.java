
package com.qihoo.channel;

public class Log {

    public static final boolean DEBUG = true;

    public static void d(String tag, String log) {
        if (!DEBUG) {
            return;
        }

        i(tag, log);
    }

    public static void i(String tag, String log) {
        if (!DEBUG) {
            return;
        }

        System.out.println(log);
    }

    public static void printBytes(String tag, byte[] bytes) {
        if (!DEBUG) {
            return;
        }

        if (bytes != null) {
            int len = bytes.length;
            Log.d(tag, "bytes.length=" + len);
            if (len > 0) {
                StringBuilder log = new StringBuilder("bytes=");
                for (int i = 0; i < bytes.length; i++) {
                    log.append(bytes[i]).append(' ');
                }
                Log.d(tag, log.toString());
            }
        } else {
            Log.d(tag, "bytes=null");
        }
    }

}

package com.tallogre.hanbaobao.Utilities;

import android.os.Looper;

public class DebugUtil {
    public static void throwIfMainThread() {
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) try {
            throw new Exception("Processing on the main thread is not allowed");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

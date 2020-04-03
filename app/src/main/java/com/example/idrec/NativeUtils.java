package com.example.idrec;

import android.graphics.Bitmap;

public class NativeUtils {

    static {
        System.loadLibrary("native-lib");
    }

    public static native Bitmap getIdNumber(Bitmap src, Bitmap.Config config);
}

package com.hide.reflection;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;

import dalvik.system.DexFile;

public class HideReflection {
    static {
        System.loadLibrary("HideReflection");
    }

    private static Method REFLECT;

    public static void init(Context application) {
        try {
            reflection(application);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void reflection(Context application) throws Exception {
        File dir = new File(application.getFilesDir(), "hot");
        if (!dir.exists()) dir.mkdir();
        File dex = new File(dir, "reflection.dex");
        if (!dex.exists()) {
            InputStream inputStream = application.getAssets().open("reflection.dex");
            dex.createNewFile();
            FileOutputStream fos = new FileOutputStream(dex);
            byte[] buffer = new byte[1024];
            int length = -1;
            while ((length = inputStream.read(buffer)) != -1) {
                fos.write(buffer, 0, length);
            }
            fos.close();
            inputStream.close();
        }
        DexFile dexFile = new DexFile(dex);
        Class aClass = dexFile.loadClass("com.cmq.dex.HideReflection", null);
        REFLECT = aClass.getDeclaredMethod("reflect", Class.class, String.class, Class[].class);
    }

    public static Method reflect(Class clazz, String name, Class... param) throws Exception {
        if (Build.VERSION.SDK_INT < 28) return clazz.getDeclaredMethod(name, param);
        else if (Build.VERSION.SDK_INT < 30) {
            Method getDeclaredMethod = Class.class.getDeclaredMethod("getDeclaredMethod", String.class, Class[].class);
            return (Method) getDeclaredMethod.invoke(clazz, name, param);
        }
        return (Method) REFLECT.invoke(null, clazz, name, param);
    }

    public static native int nativeInit(int targetSdkVersion);
}

package com.cmq.dex;

import android.app.Activity;

import java.lang.reflect.Method;

public class LoaderReflect {

    public static Method main(){
        try {
            return Activity.class.getDeclaredMethod("dispatchEnterAnimationComplete", null);

        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        return null;
    }
}

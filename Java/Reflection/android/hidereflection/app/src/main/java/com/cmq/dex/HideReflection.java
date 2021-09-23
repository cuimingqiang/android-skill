package com.cmq.dex;


import java.lang.reflect.Method;

public class HideReflection {
    public static Method reflect(Class clazz,String name,Class... param)throws Exception{
        Method getDeclaredMethod = Class.class.getDeclaredMethod("getDeclaredMethod",String.class,Class[].class);
        return (Method) getDeclaredMethod.invoke(clazz,name,param);
    }
}

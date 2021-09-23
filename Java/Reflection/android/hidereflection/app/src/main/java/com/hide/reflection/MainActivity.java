package com.hide.reflection;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import androidx.appcompat.app.AppCompatActivity;
import dalvik.system.BaseDexClassLoader;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            // copy();
           // metaReflectPassHide();
            nativePassHide();
            Method declaredMethod = Activity.class.getDeclaredMethod("dispatchEnterAnimationComplete", null);
            Log.i("----",declaredMethod.getName());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void nativePassHide(){
        HideReflection.nativeInit(Build.VERSION.SDK_INT);
    }

    private void metaReflectPassHide() throws Exception{
        HideReflection.init(getApplication());
        Method forName = Class.class.getDeclaredMethod("forName", String.class);
        Class<?> vmRuntimeClass = (Class<?>) forName.invoke(null, "dalvik.system.VMRuntime");
        Method getRuntime = (Method) HideReflection.reflect(vmRuntimeClass,"getRuntime",null);
        Method setHiddenApiExemptions = (Method) HideReflection.reflect(vmRuntimeClass, "setHiddenApiExemptions", new Class[]{String[].class});
        Object sVmRuntime = getRuntime.invoke(null);
        setHiddenApiExemptions.invoke(sVmRuntime, new Object[]{new String[]{"L"}});
    }

    //通过类加载器的方式绕过限制
    private void classLoaderPassHide() throws Exception {
        File dir = new File(getFilesDir(), "hot");
        if (!dir.exists()) dir.mkdir();
        File dex = new File(dir, "loader.dex");
        if (!dex.exists()) {
            dex.createNewFile();
            FileOutputStream fos = new FileOutputStream(dex);
            byte[] buffer = new byte[1024];
            int length = -1;
            InputStream inputStream = getAssets().open("loader.dex");
            while ((length = inputStream.read(buffer)) != -1) {
                fos.write(buffer, 0, length);
            }
            fos.close();
            inputStream.close();
        }
        Field parent = ClassLoader.class.getDeclaredField("parent");
        parent.setAccessible(true);
        Object o = parent.get(getApplication().getClassLoader());
        Constructor<BaseDexClassLoader> constructor = BaseDexClassLoader.class.getConstructor(String.class, File.class, String.class, ClassLoader.class, boolean.class);
        BaseDexClassLoader classLoader = constructor.newInstance(dex.getPath(), null, null, o, true);
        Class<?> aClass = classLoader.loadClass("com.cmq.dex.LoaderReflect");
        Method main = aClass.getDeclaredMethod("main", null);
        main.invoke(null, null);
    }
}
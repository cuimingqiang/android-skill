#### 继承关系

* ClassLoader

  * BootClassLoader

  * BaseDexClassLoader

    * DexClassLoader
    * PathClassLoader

    DexClassLoader与PathClassLoader的不同在于是否传入了优化目录optimizedDirectory。

#### 双亲委托

* 构造时需传入ClassLoader(装饰器模式)
* 继承关系(多态)

#### 加载流程

通过类加载器直接调用loadClass(String name)

```java
public Class<?> loadClass(String name) throws ClassNotFoundException {
    return loadClass(name, false);
}
```

或直接调用loadClass(name, false)

```java
protected Class<?> loadClass(String name, boolean resolve)
    throws ClassNotFoundException
{
        //1.在缓存中查找是否加载过该类。
        Class<?> c = findLoadedClass(name);
        if (c == null) {
            try {//2.
                if (parent != null) {//2.1委托parent去加载
                    c = parent.loadClass(name, false);
                } else {//2.2不做任何事情。
                    c = findBootstrapClassOrNull(name);
                }
            } catch (ClassNotFoundException e) {
                // ClassNotFoundException thrown if class not found
                // from the non-null parent class loader
            }
            if (c == null) {
                //3.如果未加载到，则自己加载(继承委托) 。
                c = findClass(name);
            }
        }
        return c;
}
```

主要做了以下三个工作：

1. 在缓存中查找是否加载过该类。

2. 如果父类加载器parent不为空，有parent去加载(装饰器委托)，2.2中不做任何事情。

3. 如果未加载到，则自己加载(继承委托) 。

* 缓存查找过程

  * ```java
    protected final Class<?> findLoadedClass(String name) {
        ClassLoader loader;
        if (this == BootClassLoader.getInstance())
            loader = null;
        else
            loader = this;
        return VMClassLoader.findLoadedClass(loader, name);
    }
    ```

  * ```java
    class VMClassLoader {
    	native static Class findLoadedClass(ClassLoader cl, String name);
    }
    ```

  * ```c
    //java_lang_VMClassLoader.cc
    static jclass VMClassLoader_findLoadedClass(JNIEnv* env, jclass, jobject javaLoader,
                                                jstring javaName) {
      ScopedFastNativeObjectAccess soa(env);
      ObjPtr<mirror::ClassLoader> loader = soa.Decode<mirror::ClassLoader>(javaLoader);
      ScopedUtfChars name(env, javaName);
      if (name.c_str() == nullptr) {
        return nullptr;
      }
      ClassLinker* cl = Runtime::Current()->GetClassLinker();
      // Compute hash once.
      std::string descriptor(DotToDescriptor(name.c_str()));
      const size_t descriptor_hash = ComputeModifiedUtf8Hash(descriptor.c_str());
      //1.如果已经加载过直接返回
      ObjPtr<mirror::Class> c = VMClassLoader::LookupClass(cl,
                                                           soa.Self(),
                                                           descriptor.c_str(),
                                                           descriptor_hash,
                                                           loader);
      if (c != nullptr && c->IsResolved()) {
        return soa.AddLocalReference<jclass>(c);
      }
      //...
      if (loader != nullptr) {
        // Try the common case.
        StackHandleScope<1> hs(soa.Self());
        //2.从父加载器parent中查找是否已经该类
        c = VMClassLoader::FindClassInPathClassLoader(cl,
                                                      soa,
                                                      soa.Self(),
                                                      descriptor.c_str(),
                                                      descriptor_hash,
                                                      hs.NewHandle(loader));
        if (c != nullptr) {
          return soa.AddLocalReference<jclass>(c);
        }
      }
      return nullptr;
    }
    ```

  缓存查找主要做了如下工作

  1. 查找该类的类加载器是否已经加载过该类。
  2. 查找该类的类加载器的父加载器parent是否加载过该类。

* 通过继承委托加载类

  ClassLoader的findClass未实现

  ```java
  protected Class<?> findClass(String name) throws ClassNotFoundException {
      throw new ClassNotFoundException(name);
  }
  ```

  * BootClassLoader的实现参见[Class.forName(String name)](#forName)

    ```java
    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        return Class.classForName(name, false, null);
    }
    ```

  * BaseDexClassLoader的实现如下

    ```java
    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        List<Throwable> suppressedExceptions = new ArrayList<Throwable>();
      	// DexPathList 
        Class c = pathList.findClass(name, suppressedExceptions);
        if (c == null) {
            //抛出异常
            throw cnfe;
        }
        return c;
    }
    ```

    * DexPathList以数组成员管理所有dex文件和so库

      * ```java
        private Element[] dexElements;//dex文件或包含dex的资源路径
        ```

      * ```java
        private final NativeLibraryElement[] nativeLibraryPathElements;//so库
        ```

      ```java
      public Class<?> findClass(String name, List<Throwable> suppressed) {
          for (Element element : dexElements) {
              Class<?> clazz = element.findClass(name, definingContext, suppressed);
              if (clazz != null) {
                  return clazz;
              }
          }
          return null;
      }
      ```

      ```java
      Element//Element of the dex/resource path.
      public Class<?> findClass(String name, ClassLoader definingContext,
              List<Throwable> suppressed) {
          return dexFile != null ? dexFile.loadClassBinaryName(name, definingContext, suppressed)
                  : null;
      }
      ```

      ```java
      DexFile
      public Class loadClassBinaryName(String name, ClassLoader loader, List<Throwable> suppressed) {
          return defineClass(name, loader, mCookie, this, suppressed);
      }
      
      private static Class defineClass(String name, ClassLoader loader, Object cookie,DexFile dexFile, List<Throwable> suppressed) {
              Class result = null;
              try {
                  result = defineClassNative(name, loader, cookie, dexFile);
              } catch (NoClassDefFoundError e) {
                  //...
              }
              return result;
      }
      //dalvik_system_DexFile.cc
      private static native Class defineClassNative(String name, ClassLoader loader, Object cookie,DexFile dexFile)
      ```

    通过以上代码流程可以得知，BaseDexClassLoader的findClass流程：BaseDexClassLoader.findClass-->DexPathList.findClass-->Element[] findClass-->DexFile.loadClassBinaryName-->defineClass-->defineClassNative，最终到native层去加载类。

#### <span id="forName">Class.forName(String name) </span>加载类过程

```java
public static Class<?> forName(String className)
            throws ClassNotFoundException {
    Class<?> caller = Reflection.getCallerClass();
    return forName(className, true, ClassLoader.getClassLoader(caller));
}
```

```java
public static Class<?> forName(String name, boolean initialize,
                               ClassLoader loader)throws ClassNotFoundException{
    if (loader == null) {
        loader = BootClassLoader.getInstance();
    }
    Class<?> result;
    try {
        result = classForName(name, initialize, loader);
    } catch (ClassNotFoundException e) {
        //抛出异常
        throw e;
    }
    return result;
}

static native Class<?> classForName(String className, boolean shouldInitialize,
        ClassLoader classLoader) throws ClassNotFoundException;
```

```c
//java_lang_Class.cc
static jclass Class_classForName(JNIEnv* env, jclass, jstring javaName, jboolean initialize,
                                 jobject javaLoader) {
  ScopedFastNativeObjectAccess soa(env);
  ScopedUtfChars name(env, javaName);
  if (name.c_str() == nullptr) {
    return nullptr;
  }
  // We need to validate and convert the name (from x.y.z to x/y/z).  This
  // is especially handy for array types, since we want to avoid
  // auto-generating bogus array classes.
  //1.检查类名字符串是否合法
  if (!IsValidBinaryClassName(name.c_str())) {
    soa.Self()->ThrowNewExceptionF("Ljava/lang/ClassNotFoundException;",
                                   "Invalid name: %s", name.c_str());
    return nullptr;
  }
  std::string descriptor(DotToDescriptor(name.c_str()));
  StackHandleScope<2> hs(soa.Self());
  Handle<mirror::ClassLoader> class_loader(
      hs.NewHandle(soa.Decode<mirror::ClassLoader>(javaLoader)));
  ClassLinker* class_linker = Runtime::Current()->GetClassLinker();
  //2.通过ClassLinker根据传入的ClassLoader去查找该类
  Handle<mirror::Class> c(
      hs.NewHandle(class_linker->FindClass(soa.Self(), descriptor.c_str(), class_loader))); 
  if (c == nullptr) {
    //传递异常到java层
    return nullptr;
  }
  if (initialize) {
    //3.初始化类，比如静态字段、构造器、接口默认方法以及父类。
    class_linker->EnsureInitialized(soa.Self(), c, true, true);
  }
  return soa.AddLocalReference<jclass>(c.Get());
}
```

Class.forName主要三件事

1. 检查类名字符串是否合法。
2. 通过ClassLinker根据传入的ClassLoader去FindClass。
3. 初始化类，比如静态字段、构造器、接口默认方法以及父类。

ClassLinker->FindClass的过程

```c++
//class_linker.cc
mirror::Class* ClassLinker::FindClass(Thread* self,
                                      const char* descriptor,
                                      Handle<mirror::ClassLoader> class_loader) {
 	...
  self->PoisonObjectPointers();  // For DefineClass, CreateArrayClass, etc...
  //1.原始类型
  if (descriptor[1] == '\0') {
    return FindPrimitiveClass(descriptor[0]);
  }
  const size_t hash = ComputeModifiedUtf8Hash(descriptor);
  //2.缓存中查找 Find the class in the loaded classes table.
  ObjPtr<mirror::Class> klass = LookupClass(self, descriptor, hash, class_loader.Get());
  if (klass != nullptr) {
    return EnsureResolved(self, descriptor, klass);
  }
  //3.原始类型数组
  if (descriptor[0] != '[' && class_loader == nullptr) {
    // Non-array class and the boot class loader, search the boot class path.
    ClassPathEntry pair = FindInClassPath(descriptor, hash, boot_class_path_);
    if (pair.second != nullptr) {
      return DefineClass(self,descriptor,hash,ScopedNullHandle<mirror::ClassLoader>(),*pair.first,*pair.second);
    } else { ... }
  }
  ObjPtr<mirror::Class> result_ptr;//目标类
  bool descriptor_equals;
  //4.数组类型
  if (descriptor[0] == '[') {
    result_ptr = CreateArrayClass(self, descriptor, hash, class_loader);
    descriptor_equals = true;
  } else {
    ScopedObjectAccessUnchecked soa(self);
    //5.在BaseDexClassLoader的继承类中反射dexPathList查找，直到BootClassLoader，如果parent链有继承ClassLoader的则查找失败，
    bool known_hierarchy =
        FindClassInBaseDexClassLoader(soa, self, descriptor, hash, class_loader, &result_ptr);
    if (result_ptr != nullptr) {
      descriptor_equals = true;
    } else {
			...//处理类名
      //6.回调java层ClassLoader loadClass查找
      ScopedLocalRef<jobject> class_loader_object(
          soa.Env(), soa.AddLocalReference<jobject>(class_loader.Get()));
      ScopedLocalRef<jobject> result(soa.Env(), nullptr);
      {
        ScopedThreadStateChange tsc(self, kNative);
        ScopedLocalRef<jobject> class_name_object(
            soa.Env(), soa.Env()->NewStringUTF(class_name_string.c_str()));
        result.reset(soa.Env()->CallObjectMethod(class_loader_object.get(),
                 WellKnownClasses::java_lang_ClassLoader_loadClass,
                                                 class_name_object.get()));
      }
      result_ptr = soa.Decode<mirror::Class>(result.get());
      // Check the name of the returned class.
      descriptor_equals = (result_ptr != nullptr) && result_ptr->DescriptorEquals(descriptor);
    }
  }
	...
  // Try to insert the class to the class table, checking for mismatch.
	...
  //7.如果加载的类的类名不与descriptor相同，则报错
  if (UNLIKELY(!descriptor_equals)) {
    ...
    ThrowNoClassDefFoundError(
        "Initiating class loader of type %s returned class %s instead of %s.",...);
    return nullptr;
  }
  // success, return mirror::Class*
  return result_ptr.Ptr();
}
```

查找类主要做了以下工作

1. 是否为原始类型(int、short、long、char、double、float，byte等)。
2. 缓存中查找。
3. 是否为原始类型数组。
4. 是否为数组类型。
5. 通过继承自BaseDexClassLoader递归查找，结束条件parent为以下情况：
   1. BootClassLoader。
   2. ClassLoader(非PathClassLoader、DexClassLoader)，直接查找失败。
6. 通过java层ClassLoader的loadClass去加载。
7. 验证Class.forName(String name)返回的类的类名是否与name相同。

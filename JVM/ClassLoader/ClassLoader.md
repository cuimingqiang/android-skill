### 	ClassLoader知识点目录

* ##### [继承关系](#Inherit)

* ##### [双亲委派](#Parent)

* ##### [类查找流程](#FindClass)

  * [通过ClassLoader查找](#classLoader)
    * [缓存查找过程](#findCache)
    * [通过继承委派查找](#findExtend)
  * [通过Class.forName(String name)查找](#forName)
  * [加载so库(System.load)](#system)
    * [通过ClassLoader获取so绝对路径](#clsopath)
    * [加载so](#loadso)
  
* ##### [类加载](#LoadClass)

#### <span id="Inherit">继承关系</span>

* ClassLoader

  * BootClassLoader

  * BaseDexClassLoader

    * DexClassLoader
    * PathClassLoader

    DexClassLoader与PathClassLoader的不同在于是否传入了优化目录optimizedDirectory。

#### <span id="Parent">双亲委派</span>

* 构造时需传入ClassLoader(装饰器模式)
* 继承委派(多态)

#### <span id="FindClass">类查找流程</span>

* ##### <span id="loadClass">通过ClassLoader查找</span>

通过loadClass(String name)

```java
public Class<?> loadClass(String name) throws ClassNotFoundException {
    return loadClass(name, false);
}
```

或直接调用loadClass(String name, boolean resolve)

```java
protected Class<?> loadClass(String name, boolean resolve)
    throws ClassNotFoundException{
        //1.在缓存中查找是否加载过该类。
        Class<?> c = findLoadedClass(name);
        if (c == null) {
            try {
                //2委托parent去加载
                if (parent != null) {
                    c = parent.loadClass(name, false);
                } else {。
                    c = findBootstrapClassOrNull(name);//不做任何事情
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

2. 如果父类加载器parent不为空，有parent去加载(装饰器委派)。

3. 如果未加载到，则自己(默认由BaseDexClassLoader)加载(继承委派) 。

* <span id="findCache">缓存查找过程</span>

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
        //2.从父加载器parent中查找是否已经该类，在查找到的过程中会初始化类信息
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

* <span id="findExtend">通过继承委派查找</span>

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
        //dex文件或包含dex的资源路径
        private Element[] dexElements;
        ```
        
      * ```java
        //so库路径(包括app和系统)
        private final NativeLibraryElement[] nativeLibraryPathElements;
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
      //Element   Element of the dex/resource path.
      public Class<?> findClass(String name, ClassLoader definingContext,
              List<Throwable> suppressed) {
          return dexFile != null ? dexFile.loadClassBinaryName(name, definingContext, suppressed)
                  : null;
      }
      ```
      
      ```java
      //DexFile
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
    
    通过以上代码流程可以得知，BaseDexClassLoader的findClass流程：BaseDexClassLoader.findClass-->DexPathList.findClass-->Element[] findClass-->DexFile.loadClassBinaryName-->defineClass-->defineClassNative，最终到native层去加载类，详细信息参加[类加载](#LoadClass)。

* ##### <span id="forName"> Class.forName(String name) </span>查找类过程

```java
public static Class<?> forName(String className) throws ClassNotFoundException {
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

1. 是否为原始类型(int、short、long、char、double、float，byte，boolean等)。
2. 缓存中查找。
3. 是否为原始类型数组。
4. 是否为数组类型。
5. 通过继承自BaseDexClassLoader递归查找，结束条件parent为以下情况：
   1. BootClassLoader。
   2. ClassLoader(非PathClassLoader、DexClassLoader)，直接查找失败。
6. 回调java层通过ClassLoader的loadClass去查找。
7. 验证Class.forName(String name)返回的类的类名是否与name相同。

* ##### <span id="system">System.loadLibrary(String name)</span>

```java
public static void loadLibrary(String libname) {
    Runtime.getRuntime().loadLibrary0(VMStack.getCallingClassLoader(), libname);
}
```

```java
//Runtime
void loadLibrary0(Class<?> fromClass, String libname) {
    ClassLoader classLoader = ClassLoader.getClassLoader(fromClass);
    loadLibrary0(classLoader, fromClass, libname);
}

synchronized void loadLibrary0(ClassLoader loader, String libname) {
    String libraryName = libname;
    if (loader != null) {
      //1.交由ClassLoader根据so库名称查找绝对路径包括库名称
      String filename = loader.findLibrary(libraryName);
      String error = doLoad(filename, loader);
      return;
    }
  	//通过so库获取完整库名 如 log-->liblog.so
    String filename = System.mapLibraryName(libraryName);
    List<String> candidates = new ArrayList<String>();
    String lastError = null;
    //2.遍历lib path目录拼接库名得到绝对路径去加载
    for (String directory : getLibPaths()) {
      String candidate = directory + filename;
      candidates.add(candidate);
      if (IoUtils.canOpenReadOnly(candidate)) {
        //3.根据库绝对路径加载
        String error = doLoad(candidate, loader);
        if (error == null) {
          return; // We successfully loaded the library. Job done.
        }
      }
    }
}
private String[] getLibPaths() {
  	mLibPaths = initLibPaths();
    return mLibPaths;
}
//获取系统so库目录，/system/lib:/system_ext/lib等
private static String[] initLibPaths() {
    String javaLibraryPath = System.getProperty("java.library.path");
    String[] paths = javaLibraryPath.split(":");
    for (int i = 0; i < paths.length; ++i) {
      if (!paths[i].endsWith("/")) {
        paths[i] += "/";
      }
    }
    return paths;
}
```

主要做以下事情：

1. 如果ClassLoader不为空(加载app或系统的so库)，通过ClassLoader获取库绝对路径。
2. 如果ClassLoader为空(加载系统的so库)，遍历系统lib目录尝试获取库绝对路径。
3. 根据绝对路径加载so库。

* <span id="clsopath">通过ClassLoader获取so绝对路径</span>

```java
//BaseDexClassLoader
//nativeLibraryPathElements  集合中包含系统路径，
//nativeLibraryDirectories   不包含系统路径
//-------------------------------初始化部分代码-------------------------------------------
//nativeLibraryDirectories = splitPaths(librarySearchPath, false);
//systemNativeLibraryDirectories = splitPaths(System.getProperty("java.library.path"),true);
//List<File> allNativeLibraryDirectories = new ArrayList<>(nativeLibraryDirectories);
//allNativeLibraryDirectories.addAll(systemNativeLibraryDirectories);
//nativeLibraryPathElements = makePathElements(allNativeLibraryDirectories);
//--------------------------------------------------------------------------------------
public String findLibrary(String name) {
    return pathList.findLibrary(name);
}
//DexPathList
public String findLibrary(String libraryName) {
    String fileName = System.mapLibraryName(libraryName);//获取so文件名
    for (NativeLibraryElement element : nativeLibraryPathElements) {
      String path = element.findNativeLibrary(fileName);
      if (path != null) {
        return path;
      }
    }
    return null;
}
//NativeLibraryElement
public String findNativeLibrary(String name) {
    maybeInit();
   //如果so不在zip文件中，直接返回绝对路径
    if (zipDir == null) {
      String entryPath = new File(path, name).getPath();
      if (IoUtils.canOpenReadOnly(entryPath)) {
        return entryPath;
      }
    } else if (urlHandler != null) {
      //如果在zip文件中，返回指向zip中so库的特殊路径
      String entryName = zipDir + '/' + name;
      if (urlHandler.isEntryStored(entryName)) {
        return path.getPath() + zipSeparator + entryName;
      }
    }
    return null;
}
```

返回库绝对路径有两种方式：

1. 目录拼接库名称。
2. zip文件拼接指向库的特殊路径。

* <span id="loadso">加载so库</span>

```java
//Runtime
private String doLoad(String name, ClassLoader loader) {
    String librarySearchPath = null;
    //1.获取BaseDexClassLoader的所以so库路径
    if (loader != null && loader instanceof BaseDexClassLoader) {
        BaseDexClassLoader dexClassLoader = (BaseDexClassLoader) loader;
        //由上面DexPathList.findLibrary注释可知，该路径不包含系统路径
        librarySearchPath = dexClassLoader.getLdLibraryPath();
    }
    synchronized (this) {
        return nativeLoad(name, loader, librarySearchPath);
    }
}
private static native String nativeLoad(String filename, ClassLoader loader,
                                            String librarySearchPath);
```

```c
//libcore/ojluni/src/main/native/Runtime.c
JNIEXPORT jstring JNICALL
Runtime_nativeLoad(JNIEnv* env, jclass ignored, jstring javaFilename,
                   jobject javaLoader, jstring javaLibrarySearchPath)
{
    return JVM_NativeLoad(env, javaFilename, javaLoader, javaLibrarySearchPath);
}
//libcore/ojluni/src/main/native/jvm.h
//art/runtime/openjdkjvm/OpenjdkJvm.cc
JNIEXPORT jstring 
JVM_NativeLoad(JNIEnv* env,jstring javaFilename,jobject javaLoader,jstring javaLibrarySearchPath) {
  ScopedUtfChars filename(env, javaFilename);
  if (filename.c_str() == NULL) {
    return NULL;
  }
  std::string error_msg;
  {
    art::JavaVMExt* vm = art::Runtime::Current()->GetJavaVM();
    bool success = vm->LoadNativeLibrary(env,filename.c_str(),
                                         avaLoader,avaLibrarySearchPath,&error_msg);
    if (success) {
      return nullptr;
    }
  }
  // Don't let a pending exception from JNI_OnLoad cause a CheckJNI issue with NewStringUTF.
  env->ExceptionClear();
  return env->NewStringUTF(error_msg.c_str());
}
//art/runtime/java_vm_ext.cc
bool JavaVMExt::LoadNativeLibrary(JNIEnv* env,
                                  const std::string& path,
                                  jobject class_loader,
                                  jstring library_path,
                                  std::string* error_msg) {
  SharedLibrary* library;
  Thread* self = Thread::Current();
  //2.如果已经加载过so，返回加载成功
  {
    MutexLock mu(self, *Locks::jni_libraries_lock_);
    library = libraries_->Get(path);
  }
  if (library != nullptr) {
    if (library->GetClassLoaderAllocator() != class_loader_allocator) { return false; }
    if (!library->CheckOnLoadResult()) {return false;}
    return true;
  }
  Locks::mutator_lock_->AssertNotHeld(self);
  const char* path_str = path.empty() ? nullptr : path.c_str();
  bool needs_native_bridge = false;
  //3.dlopen打开so库
  //system/core/libnativeloader/native_loader.cpp
  void* handle = android::OpenNativeLibrary(env,runtime_->GetTargetSdkVersion(),
                 path_str,class_loader,library_path,&needs_native_bridge,error_msg);
  if (handle == nullptr) {
    return false;
  }
  bool created_library = false;
  {
    //4.创建ShareLibrary并添加到map<so绝对路径,so实例>中
    std::unique_ptr<SharedLibrary> new_library(
        new SharedLibrary(env,self,path,handle,needs_native_bridge,
                          class_loader,class_loader_allocator));
    MutexLock mu(self, *Locks::jni_libraries_lock_);
    library = libraries_->Get(path);
    if (library == nullptr) {  // We won race to get libraries_lock.
      library = new_library.release();
      libraries_->Put(path, library);
      created_library = true;
    }
  }
  if (!created_library) {
    return library->CheckOnLoadResult();
  }
  bool was_successful = false;
  //5.查找so库是否有JNI_OnLoad函数，如果有则调用
  void* sym = library->FindSymbol("JNI_OnLoad", nullptr);
  if (sym == nullptr) {
    was_successful = true;
  } else {
    //调用JNI_OnLoad
  }
  library->SetResult(was_successful);
  return was_successful;
}
```

主要做了以下工作：

1. 获取BaseDexClassLoader所有so库的路径，作为搜索路径，如果so引用了其他so库，可以在该路径搜索。
2. 如果已经加载过so，返回加载成功。
3. dlopen打开so库。
4. 创建ShareLibrary并添加到map<so绝对路径,so实例>中。
5. 查找so库是否有JNI_OnLoad函数，如果有则调用。
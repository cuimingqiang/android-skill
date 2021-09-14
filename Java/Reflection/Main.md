### 反射

* Class
* Constructor
* Method
* Field
* Android9开始禁止@hide标记API反射

#### Class

```c
//1.art/runtime/mirror/class.cc
//2.art/runtime/native/java_lang_Class.cc
//3.java.lang.Class
//4.art/runtime/mirror/class-inl.h class.h的部分实现。
//1与3互为镜像，1是native层类的实现，3是1在Java层的引用，2是3通过jni的方式实现功能的桥梁。
```

###### isAssignableFrom

```java
public boolean isAssignableFrom(Class<?> cls) {
    if (this == cls) {
        return true;  // Can always assign to things of the same type.
    } else if (this == Object.class) {
        return !cls.isPrimitive();  // Can assign any reference to java.lang.Object.
    } else if (isArray()) {
        return cls.isArray() && componentType.isAssignableFrom(cls.componentType);
    } else if (isInterface()) {
        // Search iftable which has a flattened and uniqued list of interfaces.
        Object[] iftable = cls.ifTable;
        if (iftable != null) {
            for (int i = 0; i < iftable.length; i += 2) {
                if (iftable[i] == this) {
                    return true;
                }
            }
        }
        return false;
    } else {
        if (!cls.isInterface()) {
            for (cls = cls.superClass; cls != null; cls = cls.superClass) {
                if (cls == this) {
                    return true;
                }
            }
        }
        return false;
    }
}
```

`判断该类是否由目标类派生。`

> 1. 该类就是目标类。
>
> 2. 该类本身是Object，目标类不是原始类型(基本类型)。<font color="red" >Java的数据类型分为原始类型和引用类型，所有引用类型都是继承自Object。</font>
>
> 3. 该类是数组。
>
>    ```java
>        /**
>         * For array classes, the component class object for instanceof/checkcast (for String[][][],
>         * this will be String[][]). null for non-array classes.
>         */
>        private transient Class<?> componentType;
>    ```
>
> 4. 该类是接口，遍历目标类的ifTable。每次i+=2，在字节码方法分派实现中，有探索ifTable的实现，偶数存放接口类，奇数存放结构vtable。
>
> 5. 目标类不是接口，遍历目标类的继承链。

###### 方法信息

```c
//art/runtime/mirror/class.h
// Pointer to an ArtMethod length-prefixed array. All the methods where this class is the place where they are logically defined. This includes all private, static, final and virtual methods as well as inherited default methods and miranda methods.
  //
  // The slice methods_ [0, virtual_methods_offset_) are the direct (static, private, init) methods declared by this class.
  //
  // The slice methods_ [virtual_methods_offset_, copied_methods_offset_) are the virtual methods declared by this class.
  //
  // The slice methods_ [copied_methods_offset_, |methods_|) are the methods that are copied from interfaces such as miranda or default methods. These are copied for resolution purposes as this class is where they are (logically) declared as far as the virtual dispatch is concerned.
  //
  // Note that this field is used by the native debugger as the unique identifier for the type.
  uint64_t methods_;

  // The offset of the first virtual method that is copied from an interface. This includes miranda, default, and default-conflict methods. Having a hard limit of ((2 << 16) - 1) for methods defined on a single class is well established in Java so we will use only uint16_t's here.
  uint16_t copied_methods_offset_;

  // The offset of the first declared virtual methods in the methods_ array.
  uint16_t virtual_methods_offset_;
```

`miranda method(米兰达方法)，指接口方法包括间接实现或未实现的。`

> methods_，指所有自身方法的总数，不包含从父类继承的方法。
>
> 1. [0,virtual_methods_offset_]是静态函数、私有函数、构造函数的总数。
> 2. [virtual_methods_offset_,virtual_method_offset_]是虚方法的总数。
> 3. [virtual_methods_offset_,method_]是接口方法的总数。

#### Constructor

```java
public class ClassDemo {
    public ClassDemo(String filed) {
    }
    ClassDemo() {
    }
    protected ClassDemo(double i) {
    }
    private ClassDemo(int s) {
    }
}
```

###### getConstructors

![Alt text](constructor.png)

> 只能获取本身public的构造函数。

```java
public Constructor<?>[] getConstructors() throws SecurityException {
    return getDeclaredConstructorsInternal(true);
}
```

###### getDeclaredConstructors

![Alt text](declared-constructor.png)

> 获取自身所有构造函数。

```java
public Constructor<?>[] getDeclaredConstructors() throws SecurityException {
    return getDeclaredConstructorsInternal(false);
}
```

###### 实现

```java
private native Constructor<?>[] getDeclaredConstructorsInternal(boolean publicOnly);
```

```c
//art/runtime/native/java_lang_Class.cc
static jobjectArray Class_getDeclaredConstructorsInternal(
    JNIEnv* env, jobject javaThis, jboolean publicOnly) {
  ScopedFastNativeObjectAccess soa(env);
  StackHandleScope<2> hs(soa.Self());
  Handle<mirror::Class> h_klass = hs.NewHandle(DecodeClass(soa, javaThis));
  size_t constructor_count = 0;
  // Two pass approach for speed.
  //1 获取包含静态、私有、构造函数的数组。
  //2.1计满足条件的构造函数的长度
  for (auto& m : h_klass->GetDirectMethods(kRuntimePointerSize)) {
    constructor_count += MethodMatchesConstructor(&m, publicOnly != JNI_FALSE) ? 1u : 0u;
  }
  //2.2构建数组
  auto h_constructors = hs.NewHandle(mirror::ObjectArray<mirror::Constructor>::Alloc(
      soa.Self(), mirror::Constructor::ArrayClass(), constructor_count));
  if (UNLIKELY(h_constructors == nullptr)) {
    soa.Self()->AssertPendingException();
    return nullptr;
  }
  constructor_count = 0;
  //3.满足条件的构造函数添加到数组
  for (auto& m : h_klass->GetDirectMethods(kRuntimePointerSize)) {
    if (MethodMatchesConstructor(&m, publicOnly != JNI_FALSE)) {
      DCHECK_EQ(Runtime::Current()->GetClassLinker()->GetImagePointerSize(), kRuntimePointerSize);
      DCHECK(!Runtime::Current()->IsActiveTransaction());
      auto* constructor = mirror::Constructor::CreateFromArtMethod<kRuntimePointerSize, false>(
          soa.Self(), &m);
      if (UNLIKELY(constructor == nullptr)) {
        soa.Self()->AssertPendingOOMException();
        return nullptr;
      }
      h_constructors->SetWithoutChecks<false>(constructor_count++, constructor);
    }
  }
  return soa.AddLocalReference<jobjectArray>(h_constructors.Get());
}

static ALWAYS_INLINE inline bool MethodMatchesConstructor(ArtMethod* m, bool public_only)
    REQUIRES_SHARED(Locks::mutator_lock_) {
  DCHECK(m != nullptr);
  return (!public_only || m->IsPublic()) && !m->IsStatic() && m->IsConstructor();
}
```

```c++
//art/runtime/mirror/class-inl.h
inline IterationRange<StrideIterator<ArtMethod>> Class::GetDirectMethods(PointerSize pointer_size) {
  CheckPointerSize(pointer_size);
  return GetDirectMethodsSliceUnchecked(pointer_size).AsRange();
}
//获取包含静态、私有、构造函数的数组
inline ArraySlice<ArtMethod> Class::GetDirectMethodsSliceUnchecked(PointerSize pointer_size) {
  return ArraySlice<ArtMethod>(GetMethodsPtr(),//函数数组指针地址，
                               GetDirectMethodsStartOffset(),//0
                               GetVirtualMethodsStartOffset(),//虚方法开始索引
                               ArtMethod::Size(pointer_size),//指针大小
                               ArtMethod::Alignment(pointer_size));//对齐
}
inline uint32_t Class::GetDirectMethodsStartOffset() {
  return 0;
}
inline uint32_t Class::GetVirtualMethodsStartOffset() {
  // Object::GetFieldShort returns an int16_t value, but
  // Class::virtual_method_offset_ is an uint16_t value; cast the
  // latter to int16_t before returning it as an uint32_t value, so
  // that uint16_t values between 2^15 and 2^16-1 are correctly
  // handled.
  return static_cast<uint16_t>(
      GetFieldShort(OFFSET_OF_OBJECT_MEMBER(Class, virtual_methods_offset_)));
}
```

`对齐指如果一个指针大小是8字节，而指针指向的内容不是8字节的倍数，将补齐至8字节的倍数，这样有利于指针偏移。`

> 1. GetDirectMethods(kRuntimePointerSize)。获取包含静态、私有、构造函数的数组。kRuntimePointerSize(art/runtime/base/enums.h)是虚拟机指针大小。
> 2. 先统计满足条件的构造函数的长度，构建数组。
> 3. 将满足条件的构造函数添加到数组。

#### Method

```java
public class Base {
    private void isPrivate(){}
    protected void isProtected(){}
    public void isPublic(){}
    void isPackage(){}
}
public class Child extends Base{
    public void isChildPublic(){}
    protected void isChildProtected(){}
    private void isChildPrivate(){}
    void isChildPackage(){}
}
```

###### getMethods

```java
 public static void main(String[] args) {
        Method[] methods = Child.class.getMethods();
        for (Method method:methods){
            //public标志
            int modifiers = method.getModifiers() & 0x0001;
            System.out.println(String.format("--method->%s--access->%d",method.getName(),modifiers));
        }
    }
/*
--method->isChildPublic--access->1
--method->isPublic--access->1
--method->wait--access->1
--method->wait--access->1
--method->wait--access->1
--method->equals--access->1
--method->toString--access->1
--method->hashCode--access->1
--method->getClass--access->1
--method->notify--access->1
--method->notifyAll--access->1
*/
```

> getMethods只能获取所有public方法包括父类的。

```java
public Method[] getMethods() throws SecurityException {
  List<Method> methods = new ArrayList<Method>();
  getPublicMethodsInternal(methods);
  /*
   * Remove duplicate methods defined by superclasses and
   * interfaces, preferring to keep methods declared by derived
   * types.
   */
  //4.去除重写方法。
  CollectionUtils.removeDuplicates(methods, Method.ORDER_BY_SIGNATURE);
  return methods.toArray(new Method[methods.size()]);
}
private void getPublicMethodsInternal(List<Method> result) {
  //1.获取自身所有public方法。
  Collections.addAll(result, getDeclaredMethodsUnchecked(true));
  if (!isInterface()) {
    // Search superclasses, for interfaces don't search java.lang.Object.
    //2.遍历父类，获取父类所有public方法。
    for (Class<?> c = superClass; c != null; c = c.superClass) {
      Collections.addAll(result, c.getDeclaredMethodsUnchecked(true));
    }
  }
  // Search iftable which has a flattened and uniqued list of interfaces.
  //3.遍历接口，获取接口中所有public方法。
  Object[] iftable = ifTable;
  if (iftable != null) {
    for (int i = 0; i < iftable.length; i += 2) {
      Class<?> ifc = (Class<?>) iftable[i];
      Collections.addAll(result, ifc.getDeclaredMethodsUnchecked(true));
    }
  }
}
public native Method[] getDeclaredMethodsUnchecked(boolean publicOnly);
```

> 1. 获取本身所有public方法。
> 2. 遍历父类，获取父类所有public的方法。
> 3. 遍历接口表，获取接口所有public的方法。接口的方法默认是public。
> 4. 去除因重写而重复方法。

###### getDeclaredMethods

```java
public static void main(String[] args) {
    Method[] methods = Child.class.getDeclaredMethods();
    for (Method method:methods){
        int modifiers = method.getModifiers();
        System.out.println(String.format("--method->%s--access->%d",method.getName(),modifiers));
    }
}
/*
--method->isChildPublic--access->1
--method->isChildProtected--access->4
--method->isChildPrivate--access->2
--method->isChildPackage--access->0
*/
```

> getDeclaredMethods只能获取自身所有方法。不能获取父类的方法或字段。
>
> 如果获取父类非public方法或字段，子类遍历父类获取。

```java
public Method[] getDeclaredMethods() throws SecurityException {
    Method[] result = getDeclaredMethodsUnchecked(false);
    for (Method m : result) {
        // Throw NoClassDefFoundError if types cannot be resolved.
        m.getReturnType();
        m.getParameterTypes();
    }
    return result;
}
public native Method[] getDeclaredMethodsUnchecked(boolean publicOnly);
```

###### 实现

```c
//art/runtime/native/java_lang_Class.cc
static jobjectArray Class_getDeclaredMethodsUnchecked(JNIEnv* env, jobject javaThis,
                                                      jboolean publicOnly) {
  ScopedFastNativeObjectAccess soa(env);
  StackHandleScope<2> hs(soa.Self());
  Handle<mirror::Class> klass = hs.NewHandle(DecodeClass(soa, javaThis));
  size_t num_methods = 0;
  //1 获取自身所有方法。
  //2.1 统计满足条件的自身方法。
  for (auto& m : klass->GetDeclaredMethods(kRuntimePointerSize)) {
    auto modifiers = m.GetAccessFlags();
    // Add non-constructor declared methods.
    if ((publicOnly == JNI_FALSE || (modifiers & kAccPublic) != 0) &&
        (modifiers & kAccConstructor) == 0) {
      ++num_methods;
    }
  }
  //2.2构建数组
  auto ret = hs.NewHandle(mirror::ObjectArray<mirror::Method>::Alloc(
      soa.Self(), mirror::Method::ArrayClass(), num_methods));
  if (ret == nullptr) {
    soa.Self()->AssertPendingOOMException();
    return nullptr;
  }
  num_methods = 0;
  //3 将满足条件的函数添加到数组
  for (auto& m : klass->GetDeclaredMethods(kRuntimePointerSize)) {
    auto modifiers = m.GetAccessFlags();
    if ((publicOnly == JNI_FALSE || (modifiers & kAccPublic) != 0) &&
        (modifiers & kAccConstructor) == 0) {
      DCHECK_EQ(Runtime::Current()->GetClassLinker()->GetImagePointerSize(), kRuntimePointerSize);
      DCHECK(!Runtime::Current()->IsActiveTransaction());
      //将ArtMethod转换成java层Method
      auto* method =
          mirror::Method::CreateFromArtMethod<kRuntimePointerSize, false>(soa.Self(), &m);
      if (method == nullptr) {
        soa.Self()->AssertPendingException();
        return nullptr;
      }
      ret->SetWithoutChecks<false>(num_methods++, method);
    }
  }
  return soa.AddLocalReference<jobjectArray>(ret.Get());
}
```

```c++
//art/runtime/mirror/class-inl.h
inline IterationRange<StrideIterator<ArtMethod>> Class::GetDeclaredMethods(
      PointerSize pointer_size) {
  return GetDeclaredMethodsSliceUnchecked(pointer_size).AsRange();
}
//获取自身所有所有方法，包括静态、私有、构造函数、虚方法
inline ArraySlice<ArtMethod> Class::GetDeclaredMethodsSliceUnchecked(PointerSize pointer_size) {
  return ArraySlice<ArtMethod>(GetMethodsPtr(),
                               GetDirectMethodsStartOffset(),
                               GetCopiedMethodsStartOffset(),
                               ArtMethod::Size(pointer_size),
                               ArtMethod::Alignment(pointer_size));
}
```

> 1. 获取自身所有方法，包括静态、私有、构造、重写方法，但不包括父类和接口方法。
> 2. 统计自身所有满足条件的函数个数，构建数组
> 3. 将满足条件的函数由ArtMethod转换Java层Method结构，并添加到数组。

###### 方法结构

```java
public class MethodDemo {
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.PARAMETER, ElementType.METHOD})
    @interface Param1 {
        String value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.PARAMETER, ElementType.METHOD})
    @interface Param2 {
        int value();
    }

    @Param1(value = "method")
    @Param2(value = 1)
    public <T extends String> T getT(@Param1(value = "p1") @Param2(value = 1) String param1, @Param1(value = "p2") int... param2) throws Exception {
        return null;
    }
}
```

![Alt text](method-struct.png)

* getGenericParameterTypes

  > 获取参数集合

* getAnnotations与getDeclaredAnnotations

  > 前者实际调用后者。获取作用在方法上的注解。

* getExceptionTypes

  > 获取声明的异常列表

* getParameterAnnotations

  > 获取作用在参数上的注解，一维是参数，二维是参数上的注解集合。

* getReturnType

  > 获取返回值类型

* isVarArgs

  > 是否为可变参数

#### Field

```java
public static class FieldParent{
    String pkgField;
    private String privateField;
    protected String protectedField;
    public String publicField;

}
public static class FieldChild extends FieldParent{
    String pkgField;
    private String privateField;
    protected String protectedField;
    public String publicField;
}
```

![Alt text](field.png)

###### getFields

> 获取自身和父类的public字段

```java
public Field[] getFields() throws SecurityException {
    List<Field> fields = new ArrayList<Field>();
    getPublicFieldsRecursive(fields);
    return fields.toArray(new Field[fields.size()]);
}
private void getPublicFieldsRecursive(List<Field> result) {
    // search superclasses
    for (Class<?> c = this; c != null; c = c.superClass) {
      Collections.addAll(result, c.getPublicDeclaredFields());
    }

    // search iftable which has a flattened and uniqued list of interfaces
    Object[] iftable = ifTable;
    if (iftable != null) {
      for (int i = 0; i < iftable.length; i += 2) {
        Collections.addAll(result, ((Class<?>) iftable[i]).getPublicDeclaredFields());
      }
    }
}
private native Field[] getPublicDeclaredFields();
```

```c
//art/runtime/native/java_lang_Class.cc
static jobjectArray Class_getPublicDeclaredFields(JNIEnv* env, jobject javaThis) {
  ScopedFastNativeObjectAccess soa(env);
  return soa.AddLocalReference<jobjectArray>(
      GetDeclaredFields(soa.Self(), DecodeClass(soa, javaThis), true, true));
}
```

###### getDeclaredFields

> 获取自身所有字段。

```java
public native Field[] getDeclaredFields();
```

```c
//art/runtime/native/java_lang_Class.cc
static jobjectArray Class_getDeclaredFields(JNIEnv* env, jobject javaThis) {
  ScopedFastNativeObjectAccess soa(env);
  return soa.AddLocalReference<jobjectArray>(
      GetDeclaredFields(soa.Self(), DecodeClass(soa, javaThis), false, true));
}
```

###### 实现

```c
//art/runtime/native/java_lang_Class.cc
static mirror::ObjectArray<mirror::Field>* GetDeclaredFields(
    Thread* self, ObjPtr<mirror::Class> klass, bool public_only, bool force_resolve)
      REQUIRES_SHARED(Locks::mutator_lock_) {
  StackHandleScope<1> hs(self);
  //1.1 获取成员字段
  IterationRange<StrideIterator<ArtField>> ifields = klass->GetIFields();
  //1.2 获取静态字段
  IterationRange<StrideIterator<ArtField>> sfields = klass->GetSFields();
  size_t array_size = klass->NumInstanceFields() + klass->NumStaticFields();
  //1.3 统计满足条件的字段格式
  if (public_only) {
    // Lets go subtract all the non public fields.
    for (ArtField& field : ifields) {
      if (!field.IsPublic()) {
        --array_size;
      }
    }
    for (ArtField& field : sfields) {
      if (!field.IsPublic()) {
        --array_size;
      }
    }
  }
  size_t array_idx = 0;
  //1.4 创建数组
  auto object_array = hs.NewHandle(mirror::ObjectArray<mirror::Field>::Alloc(
      self, mirror::Field::ArrayClass(), array_size));
  if (object_array == nullptr) {
    return nullptr;
  }
  //2.1 将符合条件的成员字段由ArtField转成java层字段，并添加到数组
  for (ArtField& field : ifields) {
    if (!public_only || field.IsPublic()) {
      auto* reflect_field = mirror::Field::CreateFromArtField<kRuntimePointerSize>(self,
                                                                                   &field,
                                                                              force_resolve);
      if (reflect_field == nullptr) {
        if (kIsDebugBuild) {
          self->AssertPendingException();
        }
        // Maybe null due to OOME or type resolving exception.
        return nullptr;
      }
      object_array->SetWithoutChecks<false>(array_idx++, reflect_field);
    }
  }
  //2.2 将符合条件的静态字段由ArtField转成java层字段，并添加到数组
  for (ArtField& field : sfields) {
    if (!public_only || field.IsPublic()) {
      auto* reflect_field = mirror::Field::CreateFromArtField<kRuntimePointerSize>(self,&field,force_resolve);
      if (reflect_field == nullptr) {
        if (kIsDebugBuild) {
          self->AssertPendingException();
        }
        return nullptr;
      }
      object_array->SetWithoutChecks<false>(array_idx++, reflect_field);
    }
  }
  DCHECK_EQ(array_idx, array_size);
  return object_array.Get();
}
```

> 1. 获取成员、静态字段，统计满足条件的个数，创建数组。
> 2. 将满足条件的成员、静态字段转成Java层字段，添加到数组。

#### 字段结构

> 请参考泛型中使用泛型的字段获取泛型的真实类型。


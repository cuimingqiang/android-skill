### 注解

* 元注解
* 参数类型

#### 元注解

###### Retention

`注解的保留期。`

* SOURCE

> 保留在源码阶段，当编译时，注解会被丢弃。

* CLASS

> 保留在字节码中，但JVM不会加载。

* RUNTIME

> 会被JVM加载，可以通过反射获取。

###### Target

`注解作用域。`

<table>
  <head>
    <tr>
    <th>ElementType</th>
    <th>作用域</th>
    </tr>
    <tr>
    <td rowspan="4">TYPE</td>
    <td>Class</td>
    </tr>
    <tr>
     <td>Interface</td>
    </tr>
    <tr>
    <td>Annotation</td>
    </tr>
    <tr>
    <td>Enum</td>
    </tr>
    <tr>
    <td rowspan="2">FIELD</td>
    <td>field</td>
    </tr>
    <tr>
    <td>Enum常量</td>
    </tr>
    <tr>
    <td>METHOD</td>
    <td>方法</td>
    </tr>
    <tr>
    <td>PARAMETER</td>
    <td>方法参数</td>
    </tr>
    <tr>
    <td>CONSTRUCTOR</td>
    <td>构造函数</td>
    </tr>
    <tr>
    <td>LOCAL_VARIABLE</td>
    <td>局部变量</td>
    </tr>
    <tr>
    <td>ANNOTATION_TYPE</td>
    <td>注解类型</td>
    </tr>
    <tr>
    <td>PACKAGE</td>
    <td>包</td>
    </tr>
    <tr>
    <td>TYPE_PARAMETER(1.8)</td>
    <td>类型参数</td>
    </tr>
    <tr>
    <td>TYPE_USE(1.8)</td>
    <td>类型使用</td>
    </tr>
  </head>
</table>

> 作用域可以是一个或多个类型。

###### Repeatable

`表示该注解是否可以多次使用。`

```java
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Repeatable(Person.class)
@interface Role {
    String value();
}
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@interface Person {
    Role[] value();
}
@Role(value = "super")
@Role(value = "user")
public static class One {
  public void print() {
    Person annotation = One.class.getAnnotation(Person.class);
    for(Role role : annotation.value()){
      Log.i("---print", role.value());
    }
  }
}
//---print: super
//---print: user
```

###### Inherited

`表示该注解可以被其他注解继承该注解。`

```java
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Inherited
@interface Param {
  String value();
}

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Inherited
@interface Parent {
  String value();
  int integer();
}

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Parent(value = "parent",integer = 1)
@interface Child {
  String value();
  Class clazz();
  Param param();
}

@Child(value = "32323", clazz = Object.class, param = @Param("222"))
public static class Two extends One {
    @Override
    public void print() {
        Child child = Two.class.getAnnotation(Child.class);
        Parent parent = Child.class.getAnnotation(Parent.class);
        Log.i("--print",child.value()+"|"+parent.value()+"|"+parent.integer());
    }
}
//--print: 32323|parent|1
```

#### 参数类型

`声明注解中可以使用的参数类型`

* 八种基本类型及其数组
* String类型及其数组
* Class类型及其数组
* 注解类型及其数组

### 设计模式

#### 创建型

##### 单例模式

* 存在的问题

  > 1. 反序列化问题，readResolve可以解决。
  > 2. 反射。
  > 3. 并发问题。

###### 饿汉式

###### 懒汉式

###### 双重锁

###### 静态内部类

> 由类加载器解决反序列化、并发问题。

###### 枚举

###### 容器

##### 建造者模式

* Dialog
* Retrofit
* Notification
* OkHttp
* WorkManager配置
* Glide，RequestOption

##### 原型模式

* 实现Cloneable接口
* Intent
* Animation

##### 工厂模式

###### 简单工厂

* 加密算法

###### 工厂方法

* LayoutInflater

##### 抽象工厂模式

#### 结构型

##### 过滤器模式

* AutoCompleteTextView。
* TextView。

##### 享元模式

* String
* Message
* MotionEvent
* Parcel

##### 适配器模式

###### 类适配器

###### 对象适配器

* RecyclerView,ListView,GridView等的适配器。
* Gson。

##### 桥接模式

* Window和WindowManager。
* View与ViewRootImp。
* AMS。

##### 组合模式

* 文件夹。
* View。

##### 外观模式

* Context封装了AMS的接口。

##### 装饰器模式

* Context与ContextImp。

##### 代理模式

###### 动态代理

* Retrofit
* Proxy

###### 静态代理

* Binder。
* ActivityManagerProxy。

#### 行为型

##### 责任链模式

* 点击事件分发机制
* OkHttp请求与响应处理
* 有序广播

##### 命令模式

* Thread
* Handler

##### 解释器模式

* PackageParser

##### 迭代器模式

* 集合
* 游标

##### 中介者模式

* Keyguard

##### 备忘录模式

* activity中保存和恢复机制
* Canvas

##### 观察者模式

* 列表视图的适配器也实现了观察者
* 数据库
* 内容提供者
* 点击事件
* 广播
* RxJava
* EventBus、Otto
* Lifecycle

##### 状态模式

* MediaPlayer
* 蓝牙状态
* 网络状态
* Activity/Fragment的状态

##### 空对象模式

##### 策略模式

* 动画插值器
* Glide的缓存策略

##### 模板模式

* AQS
* AsyncTask
* Activity/Fragment声明周期

##### 访问者模式

* ASM
* Javassit


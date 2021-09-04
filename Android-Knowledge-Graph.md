### 数据结构与算法

* 数据结构
  * 线性表
    * 数组
    * 链表
      * 单向链表
      * 双向链表
      * 跳表
    * 栈
    * 队列
  * 树
    * 二叉树
      * 平衡二叉树
      * 红黑树
  * 图
* 算法
  * 算法思想
    * 分治算法
    * 贪婪算法
    * 回溯算法
    * 动态规划
    * 分支界限
  * 排序算法
    * 交换排序
      * 冒泡排序
      * 快速排序
    * 插入排序
      * 简单插入排序
      * 希尔排序
    * 选择排序
      * 简单选择排序
      * 堆排序
    * 归并排序
      * 二路归并排序
      * 多路归并排序
    * 计数排序
    * 基数排序
    * 桶排序
  * 查找算法
  * KMP字符串匹配

### 汇编

* ARM指令
* thumb指令

### linux

* Syscall
* IPC
* 线程
  * 线程
  * TLS
  * 锁
* I/O复用
  * select
  * poll
  * epoll
* ELF
* 链接库
  * 静态链接库(.a)
  * 动态链接库(.so)
* Hook

### 网络协议

* 七层模型
* TCP/IP
* Socket
  * BIO
  * NIO
  * AIO
* Http

### Java 基础

* 泛型
* 注解
* 反射
  * getDeclard
  * get
  * Android高版本hide限制
* MethodHandle
* 并发
  * 线程
  * 线程池
    * ExecutorService
    * ForkJoin
  * 锁
    * volatile
    * synchronized
    * CMS
    * AQS
      * CLH
    * LatchDownCount
    * LockSupport
* 集合
  * Array
  * List
  * Map

### Kotlin

* 协程

### JVM

* JMM
* 运行时数据区
* GC
  * 标记算法
  * 回收算法
* 字节码
  * class
  * 执行引擎
* ClassLoader
  * 类加载
    * 加载
    * 链接
      * 验证
      * 准备
      * 解析
    * 使用
    * 卸载
  * 双亲委派
  * BaseDexClassLoader
    * PathDexClassLoader
    * DexClassLoader
    * DexPathList
    * DexFile
  * [Hook](JVM/ClassLoader/Hook.md)
    * Android Hook点
    * 重排Element数组
    * 重写类加载器
* Android JVM
  * Dex
  * Dalvik VM
  * Art VM

### Framework

* Android启动流程
  * init
  * zygote
  * systemServer

* Handler
  * Looper
  * MessageQueue
  * Message
  * 创建方式
  * dispatchMessage
  * HandlerThread
* Binder
  * 驱动
  * ServiceManager
  * 服务端
  * 客户端
* AMS
  * Activity
  * Service
    * IntentService
  * Broadcast
  * ContentProvider
* WMS
  * App(客户端)
  * WMS(管理端)
  * SurfaceFlinger(服务端)
* PKMS
* AsyncTask
* LayoutInflater

### UI

* View
* ViewGroup
* 绘制流程
* 事件传递
* TextView
* RecyclerView

### NDK

* C/C++
* CMake

* JNI
  * JavaVM
  * JNIEnv
* FFMpeg
* OpenGL
* OpenSL
* OpenCV

### Gradle

* APT
* 编译时Hook

### 热修复

* 方法修复(Native方法替换)
* dex修复

### 组件化

* 组件间通信
  * Intent隐式启动
  * 接口依赖
    * Hilt注入
    * AutoService
  * 路由
* 资源

### 插件化

* 插件加载
  * dex加载
  * 资源加载
* 启动方式
  * Hook AMS
  * Hook ClassLoader
* 资源依赖
  * 独立资源
  * 依赖宿主

### APM

* 启动优化
* 内存优化
* 绘制优化
* 监控
  * ANR
  * 卡顿检测
* 线程优化
* 网络优化
* 电量优化
* APK瘦身

### Jetpack

* Lifecycle
* ViewModel
* Room
* Pageing
* WorkManager
* Databinding
* Viewbinding
* Starter
* Hilt

### 开源库

* OkHttp
* Retrofit
* 图片加载库
  * Glide
  * Fresco
  * Picasso
* RxJava
* 插件化
  * RePlugin
  * VirtualApk
* 热修复
  * Tinker
  * sophix
* Android-skin-support
* ARoute
* epic
* AOP
  * aspectJ
* 日志
  * xCrash
  * Logan
  * Timber
* APM
  * matrix
* 长连接
  * mars

### Flutter


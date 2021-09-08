#### Android JVM知识目录

* #### Dex文件结构

  * dex
  * odex
  * oat

* #### 虚拟机

  * Dalvik 优化
    * dex通过dex_opt优化成odex
    * 采用JIT
  * ART优化
    * 5.0-7.0通过dex2oat优化成oat文件
    * 7.0之后(三者并存)
      * 解释器
      * JIT
      * OAT

#### <span id="dex">Dex文件结构</span>

#### <span id="jvm">虚拟机</span>

Android虚拟机是基于寄存器，所以速度要快于基于栈的Java虚拟机。从早期的Dalvik虚拟机发展到现在的ART虚拟机(Android 5.0及以后默认虚拟机)。

##### ART优化

从5.0到7.0版本在安装时，PMKS会通过dex2oat静态方式编译dex文件生成oat，所以很耗时。

从7.0版本之后采用混合模式，即采用解释器+JIT+OAT的方式，系统会再空闲的时候将dex编译成oat。

[详细请参考](https://www.jianshu.com/p/bcc4a9209ef5)










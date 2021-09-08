### 字节码知识点目录

* #### [类文件结构](#struct)

* #### [字节码指令](#instruction)

* #### [方法分派](#dispatch)

* #### [MethodHandle](#methodHandle)

#### <span id="struct">类文件结构</spna>

* 魔数
* 版本号
* 常量池
* 访问标志
* 类索引、父类索引、接口索引集合
* 字段表集合
* 方法表集合
* 属性表集合

#### <span id="instruction">字节码指令</span>

* 方法调用
  * invokeStatic
  * invokeSpecial
  * invokeVirtual
  * invokeInterface
  * invokeDynamic

#### <span id="dispatch">方法分派</span>

* 宗量
* 类型
* 实现
  * 类
  * 接口

#### <span id="methodHandle">MethodHandle</span>

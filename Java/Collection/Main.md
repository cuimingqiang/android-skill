### 集合

* Arrays

* Collection
  * List
  * Set
* Map

#### Arrays

> 数组操作工具。

#### Collection

##### 继承关系

* Collection

  * List

    * AbstractList
      * AbstractSequentialList
        * LinkedList
      * ArrayList
      * Vector
        * Stack
    * CopyOnWriteArrayList

  * Queue

    * Deque

      > LinkedList实现了Deque。

  * Set

    * AbstractSet
      * EnumSet
      * HashSet
        * LinkedHashSet
      * TreeSet
      * ConcurrentSkipListSet
      * CopyOnWriteArraySet
    * ArraySet
    * ArraySet(androidx)

##### List比较

|    List    | 插入 | 删除 | 查找 | 扩容 |  内存  |
| :--------: | :--: | :--: | :--: | :--: | :----: |
| LinkedList | 较快 | 较快 | 较慢 |  否  | 不连续 |
| ArrayList  | 较慢 | 较慢 | 较快 |  是  |  连续  |

#### Map

##### 继承关系

* ArrayMap

* ArrayMap(Androidx )

* HashTable(Dictionary)

  > 线程安全。
  >
  > 只能存放非空key和value。

* AbstractMap

  * HashMap
    * LinkedHashMap
  * WeakHashMap
  * EnumMap
  * IdentityHashMap
  * TreeMap
  * ConcurrentHashMap
  * ConcurrentSkipListMap

##### HashMap

|              |           1.7           |                             1.8                              |
| :----------: | :---------------------: | :----------------------------------------------------------: |
|   数据结构   |        数组+链表        |                       数组+链表+红黑树                       |
|   默认长度   |           16            |                              16                              |
| 默认负载因子 |          0.75           |                             0.75                             |
|   元素索引   | hash & (tab.length – 1) |                   hash & (tab.length – 1)                    |
|  索引相同时  |        链表存放         | 当桶(数组)>=64。链表长度大于8时转换成红黑树；当链表长度小于6时，退化成数组。 |

> 扩容时只能是2的幂。

##### ConcurrentHashMap

|              |           1.7           |           1.8           |
| :----------: | :---------------------: | :---------------------: |
|   数据结构   |        数组+链表        |    数组+链表+红黑树     |
|   默认长度   |           16            |           16            |
| 默认负载因子 |          0.75           |          0.75           |
|   元素索引   | hash & (tab.length – 1) | hash & (tab.length – 1) |
|  索引相同时  |        同HashMap        |        同HashMap        |
|      锁      |         分段锁          |    CAS+synchronized     |




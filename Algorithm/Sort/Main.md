### 排序算法

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

#### 交换排序

##### 冒泡排序

```kotlin
//冒泡排序
fun bubble(array: Array<Int>) {
    var count = 0
    for (end in array.size downTo 0 step 1) {
        for (j in 1 until end) {
            if (array[j - 1] > array[j]) {
                val temp = array[j - 1]
                array[j - 1] = array[j]
                array[j] = temp

            }
            count++
        }
    }
    println("count = $count")
}
```

```kotlin
//冒泡排序优化 如果顺序已排好，提前中断
fun bubbleBreakOnOrder(array: Array<Int>) {
    var count = 0
    for (end in array.size downTo 0) {
        var sorted = true
        for (j in 1 until end) {
            if (array[j - 1] > array[j]) {
                val temp = array[j]
                array[j] = array[j - 1]
                array[j - 1] = temp
                sorted = false
            }
            count++
        }
        if (sorted) break
    }
    println("count = $count")
}
```

```kotlin
//冒泡排序优化 记下最后一次发生交换数据的位置，该位置之后都是排序好的
fun bubbleEndToSwap(array: Array<Int>) {
    var end = array.size
    var count = 0
    for (i in array.size downTo 0) {
        var sortEnd = 1
        for (j in 1 until end) {
            if (array[j - 1] > array[j]) {
                val temp = array[j]
                array[j] = array[j - 1]
                array[j - 1] = temp
                sortEnd = j
            }
            count++
        }
        end = sortEnd
    }
    println("count = $count")
}
```

##### 快速排序

```kotlin
fun quick(array: Array<Int>, start: Int = 0, end: Int = array.size) {
    if (start >= end) return
    var s = start
    var e = end - 1
    val base = array[s]
    while (s < e) {
        while (e > s) {
            if (array[e] < base) break
            e--
        }
        if (e > s) array[s] = array[e]
        while (s < e) {
            if (array[s] > base) break
            s++
        }
        if (s < e) array[e] = array[s]
    }
    array[s] = base
    quick(array, start, s - 1)
    quick(array, e + 1, end)
}
```

#### 插入排序

##### 简单插入排序

```kotlin
fun insertSimple(array: Array<Int>) {
    for (i in 1 until array.size) {
        for (j in i downTo 1) {
            if (array[j - 1] > array[j]) {
                val temp = array[j]
                array[j] = array[j - 1]
                array[j - 1] = temp
            } else break
        }
    }
}
```

##### 希尔排序

```kotlin
fun insertHill(array: Array<Int>) {
    var group = array.size / 2
    while (group > 0) {
        for (i in group until array.size) {
            var pair = i
            while ((pair - group) >= 0 && array[pair] < array[pair - group]) {
                val temp = array[pair - group]
                array[pair - group] = array[pair]
                array[pair] = temp
                pair -= group
            }
        }
        group /= 2
    }
}
```

#### 选择排序

##### 简单选择排序

```kotlin
fun selectSimple(array: Array<Int>) {
    for (i in 0 until array.size - 1) {
        var min = array[i]
        var position = i
        for (j in i + 1 until array.size) {
            if (array[j] < min) {
                min = array[j]
                position = j
            }
        }
        if (position != i) {
            array[position] = array[i]
            array[i] = min
        }
    }
}
```

##### 堆排序

```kotlin
class Node(val value: Int) {
    var left: Node? = null
    var right: Node? = null
}
fun addNode(root: Node, child: Node) {
    if (child.value < root.value) {
        if (root.left == null) { root.left = child
        } else {addNode(root.left!!, child)}
    } else {
        if (root.right == null) root.right = child
        else {addNode(root.right!!, child)}
    }
}
fun selectHeap(array: Array<Int>) {
    var head: Node? = null
    array.forEach {
        if (head == null) head = Node(it)
        else addNode(head!!, Node(it))
    }
}
```

#### 归并排序

##### 二路归并排序

```kotlin
fun merger(array: Array<Int>, start: Int, middle: Int, end: Int) {
    if (start >= end) return
    val temp = arrayOfNulls<Int>(end - start + 1)
    var tempIndex = 0
    var low = start
    var high = middle + 1
    while (low <= middle && high <= end) {
        if (array[low] <= array[high]) {
            temp[tempIndex++] = array[low++]
        } else temp[tempIndex++] = array[high++]
    }
    while (low <= middle) {temp[tempIndex++] = array[low++] }
    while (high <= end) {temp[tempIndex++] = array[high++] }
    for (i in start until end + 1) {
        array[i] = temp[i - start]!!
    }
}
fun mergerTwo(array: Array<Int>, start: Int = 0, end: Int = array.size - 1) {
    if (start >= end) return
    val mid = (start + end) / 2
    mergerTwo(array, start, mid)
    mergerTwo(array, mid + 1, end)
    merger(array, start, mid, end)
}
```

多路归并排序

#### 计数排序

```kotlin
//计数排序
fun countSort(array: Array<Int>) {
    //1 找出最大最小数
    var max = Int.MIN_VALUE
    var min = Int.MAX_VALUE
    array.forEach {
        if (max < it) max = it
        if (min > it) min = it
    }
    //2.统计数组元素出现的次数
    val temp = Array<Int>(max - min) { 0 }
    array.forEach {temp[it - min].inc() }
    //3.计算每个元素的位置
    for (i in 1 until temp.size){ temp[i] += temp[i-1] }
    //4.按位置输出到新数组
    val newArray = Array<Int>(array.size){0}
    array.forEach { newArray[temp[it - min] --] = it }
    //5.复制到原数组
    for (i in array.indices){ array[i] = newArray[i] }
}
```

#### 基数排序

```kotlin
//基数排序
fun base(array: Array<Int>) {
    val baseArray = Array(10) { Array(array.size) { 0 } }
    val countArray = Array(10) { 0 }
    var max = Int.MIN_VALUE
    array.forEach { if (max < it) max = it }
    var div = 1
    while (div < max) {
        array.forEach {
            val base = it / div % 10
            baseArray[base][countArray[base]++] = it
        }
        var count = 0
        for (i in baseArray.indices){
            for ( j in 0 until countArray[i]){
                array[count++] = baseArray[i][j]
            }
            countArray[i] = 0
        }
        div *= 10
    }
}
```

#### 桶排序

```kotlin
fun bucket(array: Array<Int>) {
    val baseArray = Array(10) { Array(array.size) { 0 } }
    val countArray = Array(10) { 0 }
    //1、分桶规则
    var max = Int.MIN_VALUE
    var min = Int.MAX_VALUE
    array.forEach { if (max < it) max = it; if (min > it) min = it }
    val avg = (max - min) / 10
    //2、分桶
    array.forEach {
        val index = (it - min) / avg
        baseArray[index][countArray[index]++] = it
    }
    var index = 0
    for (i in baseArray.indices) {
        //3.桶内排序
        for (j in 1 until countArray[i]) {
           for (k in j downTo 1){
               if (baseArray[i][k-1] > baseArray[i][k]){
                   val temp = baseArray[i][k-1]
                   baseArray[i][k-1] = baseArray[i][k]
                   baseArray[i][k] = temp
               }
           }
        }
        //4、回写原数组
        for (j in 0 until countArray[i]) {
            array[index++] = baseArray[i][j]
        }
    }
}
```

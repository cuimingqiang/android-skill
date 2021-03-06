### 算法思想

* 分治算法
* 贪婪算法
* 回溯算法
* 动态规划
* 分支界限

#### 分治算法

##### 问题特征

* 该问题的规模缩小到一定的程度就可以容易地解决。

* 该问题可以分解为若干个规模较小的相同问题，即该问题具有最优子结构性质。

* 利用该问题分解出的子问题的解可以合并为该问题的解。

  > 如果不满足该条，可以考虑：
  >
  > 1. 动态规划。
  > 2. 贪心算法。

* 子问题是相互独立的，即子问题之间没有公共的子问题。

  > 如果不满足该条，分治过程中会存在大量重复子问题被多次计算，从而拖慢效率。可以考虑：
  >
  > 1. 动态规划。

##### 解题步骤

* 分解，将要解决的问题划分成若干规模较小的同类问题。
* 求解，当子问题划分得足够小时，用较简单的方法解决。
* 合并，按原问题的要求，将子问题的解逐层合并构成原问题的解。

##### 应用

###### 二分查找

#### 贪婪算法

`对问题求解时，总是做出在当前看来是最好的选择。也就是说，不从整体最优上加以考虑，算法得到的是在某种意义上的局部最优解。`

##### 问题特征

* 贪心选择性质

  >  所谓贪心选择性质是指所求问题的整体最优解可以通过一系列局部最优的选择，换句话说，当考虑做何种选择的时候 我们只考虑对当前问题最佳的选择而不考虑子问题的结果。

* 最优子结构信息

  > 当 一个问题的最优解包含其子问题的最优解时，称此问题具有最优子结构性质。

##### 存在的问题

> 1. 不能保证求得的最后解是最佳的。
> 2. 不能用来求最大或最小解问题。
> 3. 只能求满足某些约束条件的可行解的范围。

##### 应用

###### Dijkstra

###### Prim

###### Kruskal

#### 回溯算法

##### 一般解题思路

* 针对所给问题，定义问题的解空间，它至少包含问题的一个（最优）解。
* 确定易于搜索的解空间结构,使得能用回溯法方便地搜索整个解空间。
* 以深度优先的方式搜索解空间，并且在搜索过程中用剪枝函数避免无效搜索。

##### 应用

###### 图的深度有限搜索

###### 二叉树的后续遍历

##### 经典问题

###### 八皇后问题

#### 动态规划

##### 问题特征

`动态规划算法通常用于求解具有某种最优性质的问题。`

* 最优化原理

  > 如果问题的最优解所包含的子问题的解也是最优的，就称该问题具有最优子结构，即满足最优化原理。

* 无后效性

  > 某阶段状态一旦确定，就不受这个状态以后决策的影响。即某状态以后的过程不会影响以前的状态，只与当前状态有关。

* 有重叠子问题

  > 即子问题之间是不独立的，一个子问题在下一阶段决策中可能被多次使用到。该性质并不是动态规划适用的必要条件，但是如果没有这条性质，动态规划算法同其他算法相比就不具备优势。

##### 一般解决思路

* 分析最优解的性质，并刻画其结构特征。
* 递归的定义最优解。
* 以自底向上或自顶向下的记忆化方式（备忘录法）计算出最优值。
* 根据计算最优值时得到的信息，构造问题的最优解。

##### 经典问题

###### 斐波那契数列

###### 约瑟夫问题

#### 分支界限

`分支限界法常以广度优先或以最小耗费（最大效益）优先的方式搜索问题的解空间树。`

> 分支限界法与回溯法的不同：
>
> 1. 求解目标：回溯法的求解目标是找出解空间树中满足约束条件的所有解，而分支限界法的求解目标则是找出满足约束条件的一个解，或是在满足约束条件的解中找出在某种意义下的最优解。
> 2. 搜索方式的不同：回溯法以深度优先的方式搜索解空间树，而分支限界法则以广度优先或以最小耗费优先的方式搜索解空间树。

##### 两种实现

* 队列式(FIFO)分支限界法

  > 按照队列先进先出（FIFO）原则选取下一个节点为扩展节点。

* 优先队列式分支限界法

  > 按照优先队列中规定的优先级选取优先级最高的节点成为当前扩展节点。


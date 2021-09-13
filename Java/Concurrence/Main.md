### 并发

* 线程
* 线程池
  * ExecutorService
  * ForkJoin
* 锁
  * wait/notify
  * volatile
  * synchronized
  * CMS
  * AQS
    * CLH
  * LatchDownCount
* LockSupport
  

#### 线程

###### 创建方式



> 重写Thread的run方法。



> 实现Runnable接口，作为参数传递给Thread。

###### 线程状态

###### 线程中断

###### sleep

###### join

#### 线程池



#### 锁

###### 并发特性

* 原子性
* 有序性
* 可见性

###### wait/notify/notifyAll

`wait、notify、notifyAll是Object的方法，所以所有java对象默认继承该方法。`

> wait()阻塞当前线程。
>
> wait(long millTimes)阻塞当前线程，并设置一个超时时间。
>
> notify()唤醒一个wait()阻塞的线程。
>
> notifyAll()唤醒所有被wait()阻塞的线程。

###### volatile

`是一种最简单的同步机制，保证了原子性和可见性。`

> 
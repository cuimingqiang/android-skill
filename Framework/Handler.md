### Handler

* [Message](#message)
* [MessageQueue](#mq)
  * [初始化](#mq-init)
  * [消息屏障](#mq-barrier)
  * [消息插入](#mq-insert)
  * [消息获取](#mq-get)
* [Looper](#looper)
* [Handler](#handler)
  * [创建方式](#handler-create)
  * [消息分发](#handler-dispatch)
  * [发送消息](#handler-send)
  * [移除消息](#handler-remove)
  * [应用](#handler-apply)
* [HandlerThread](#handlerThread)

#### <span id="message">Message</span>

* what

  ```java
  public int what;
  ```

  > 用户自定义字段，可以用来标记命令。

* arg1

  ```java
  public int arg1;
  ```

  > 最简单的参数。

* arg2

  ```java
  public int arg2;
  ```

  > 最简单的参数。

* data

  ```java
  Bundle data;
  ```

  > 可以传输复杂的参数

* obj

  ```java
  public Object obj;
  ```

  > 也可以传输对象参数

* replyTo

  ```java
  public Messenger replyTo;
  ```

  > 主要用于跨进程通信。

* target

  ```java
  Handler target;
  ```

  > 消息所属Handler，通常不为空，如果为空，意味着这条消息为屏障消息。

* callback

  ```java
  Runnable callback;
  ```

  > 添加一个Runnable任务。

* flags

  ```java
  int flags;
  public void setAsynchronous(boolean async) {
      if (async) {
        flags |= FLAG_ASYNCHRONOUS;
      } else {
        flags &= ~FLAG_ASYNCHRONOUS;
      }
  }
  ```

  > 消息标志，可以设置异步消息，通常用于开启同步屏障后，发送MotionEvent、绘制、动画等异步消息。

* sPool

  ```java
  private static Message sPool;
  private static int sPoolSize = 0;
  private static final int MAX_POOL_SIZE = 50;
  ```

  > 享元模式，消息缓存池，上线50。

##### 缓存池

###### 分配

* ```java
   public static Message obtain() {
       synchronized (sPoolSync) {
         if (sPool != null) {
           Message m = sPool;
           sPool = m.next;
           m.next = null;
           m.flags = 0; // clear in-use flag
           sPoolSize--;
           return m;
         }
       }
       return new Message();
   }
  ```

* ```java
  public static Message obtain(Message orig)
  ```

* ```java
  public static Message obtain(Handler h) 
  ```

* ```java
  public static Message obtain(Handler h, Runnable callback) 
  ```

* ```java
  public static Message obtain(Handler h, int what) 
  ```

* ```java
  public static Message obtain(Handler h, int what, Object obj)
  ```

* ```java
  public static Message obtain(Handler h, int what, int arg1, int arg2)
  ```

* ```java
  public static Message obtain(Handler h, int what, int arg1, int arg2, Object obj) 
  ```

> 所有有参obtain都是调用无参obtain，然后赋值参数。

###### 回收

```java
void recycleUnchecked() {
    // Mark the message as in use while it remains in the recycled object pool.
    // Clear out all other details.
    flags = FLAG_IN_USE;
    what = 0;
    arg1 = 0;
    arg2 = 0;
    obj = null;
    replyTo = null;
    sendingUid = UID_NONE;
    workSourceUid = UID_NONE;
    when = 0;
    target = null;
    callback = null;
    data = null;

    synchronized (sPoolSync) {
        if (sPoolSize < MAX_POOL_SIZE) {
            next = sPool;
            sPool = this;
            sPoolSize++;
        }
    }
}
```

#### <span id="mq">MessageQueue</span>

* mIdleHandlers

  ```java
  //MessageQueue
  private final ArrayList<IdleHandler> mIdleHandlers = new ArrayList<IdleHandler>();
  public void addIdleHandler(@NonNull IdleHandler handler) {
      if (handler == null) {
        throw new NullPointerException("Can't add a null IdleHandler");
      }
      synchronized (this) {
        mIdleHandlers.add(handler);
      }
  }
  public void removeIdleHandler(@NonNull IdleHandler handler) {
      synchronized (this) {
        mIdleHandlers.remove(handler);
      }
  }
  /**
  * Callback interface for discovering when a thread is going to block
  * waiting for more messages.
  */
  public static interface IdleHandler {
    	/**
   		* Called when the message queue has run out of messages and will now
    	* wait for more.  Return true to keep your idle handler active, false
    	* to have it removed.  This may be called if there are still messages
    	* pending in the queue, but they are all scheduled to be dispatched
   		* after the current time.
    	*/
   	  boolean queueIdle();
  }
  ```

  > 添加线程空闲任务，比如需要在主线程初始化或者执行的任务，但又不是很紧急，可以在主线程空闲时执行即可。

* mPtr

  ```java
  private long mPtr; // used by native code
  MessageQueue(boolean quitAllowed) {
      mQuitAllowed = quitAllowed;
      mPtr = nativeInit();
  }
  ```

  ```java
  //android10/frameworks/base/core/jni/android_os_MessageQueue.cpp
  static jlong android_os_MessageQueue_nativeInit(JNIEnv* env, jclass clazz) {
      NativeMessageQueue* nativeMessageQueue = new NativeMessageQueue();
      if (!nativeMessageQueue) {
          jniThrowRuntimeException(env, "Unable to allocate native queue");
          return 0;
      }
  
      nativeMessageQueue->incStrong(env);
      return reinterpret_cast<jlong>(nativeMessageQueue);
  }
  ```

  > 保存Native的MessageQueue的指针。

* mMessages

  ```java
  Message mMessages;
  ```

  > 消息链表队头。

##### <span id="mq-init">初始化</span>

```java
private Looper(boolean quitAllowed) {
    mQueue = new MessageQueue(quitAllowed);
    mThread = Thread.currentThread();
}
```

```java
MessageQueue(boolean quitAllowed) {
    mQuitAllowed = quitAllowed;
    mPtr = nativeInit();
}
private native static long nativeInit();
```

```c++
//android10/frameworks/base/core/jni/android_os_MessageQueue.cpp
static jlong android_os_MessageQueue_nativeInit(JNIEnv* env, jclass clazz) {
    NativeMessageQueue* nativeMessageQueue = new NativeMessageQueue();
    if (!nativeMessageQueue) {
        jniThrowRuntimeException(env, "Unable to allocate native queue");
        return 0;
    }
    nativeMessageQueue->incStrong(env);
    return reinterpret_cast<jlong>(nativeMessageQueue);
}

NativeMessageQueue::NativeMessageQueue() :
        mPollEnv(NULL), mPollObj(NULL), mExceptionObj(NULL) {
    mLooper = Looper::getForThread();
    if (mLooper == NULL) {
        mLooper = new Looper(false);
      	//通过TLS设置线程变量
        Looper::setForThread(mLooper);
    }
}
```

```c++
Looper::Looper(bool allowNonCallbacks)
    : mAllowNonCallbacks(allowNonCallbacks),
      mSendingMessage(false),
      mPolling(false),
      mEpollRebuildRequired(false),
      mNextRequestSeq(0),
      mResponseIndex(0),
      mNextMessageUptime(LLONG_MAX) {
    mWakeEventFd.reset(eventfd(0, EFD_NONBLOCK | EFD_CLOEXEC));
    AutoMutex _l(mLock);
    rebuildEpollLocked();
}
void Looper::rebuildEpollLocked() {
    // Close old epoll instance if we have one.
    if (mEpollFd >= 0) {
        mEpollFd.reset();
    }

    // Allocate the new epoll instance and register the wake pipe.
  	//创建epoll
    mEpollFd.reset(epoll_create1(EPOLL_CLOEXEC));

    struct epoll_event eventItem;
    memset(& eventItem, 0, sizeof(epoll_event)); // zero out unused members of data field union
    eventItem.events = EPOLLIN;
    eventItem.data.fd = mWakeEventFd.get();
  	//通过epoll机制发送事件
    int result = epoll_ctl(mEpollFd.get(), EPOLL_CTL_ADD, mWakeEventFd.get(), &eventItem);
    for (size_t i = 0; i < mRequests.size(); i++) {
        const Request& request = mRequests.valueAt(i);
        struct epoll_event eventItem;
        request.initEventItem(&eventItem);
        int epollResult = epoll_ctl(mEpollFd.get(), EPOLL_CTL_ADD, request.fd, &eventItem);
    }
}
```

> Java层Looper初始化时，构建Java层MessageQueue，因此伴随着创建了native层NativeMessageQueue，同时也创建了native层Looper。
>
> 通过epoll机制管理消息队列阻塞、唤醒、发送、获取。

##### <span id="mq-barrier">消息屏障</span>

`屏障消息的target为null。当Looper遇到屏障消息后，只能执行异步消息，除非移除同步屏障后，才会执行同步消息。`

```java
//发起同步屏障
public int postSyncBarrier() {
    return postSyncBarrier(SystemClock.uptimeMillis());
}
private int postSyncBarrier(long when) {
    // Enqueue a new sync barrier token.
    // We don't need to wake the queue because the purpose of a barrier is to stall it.
    synchronized (this) {
      final int token = mNextBarrierToken++;
      //1.从消息缓存池中分配一个消息，并初始化
      final Message msg = Message.obtain();
      msg.markInUse();
      msg.when = when;
      msg.arg1 = token;
			//2.将屏障消息按照when插入指定位置
      Message prev = null;
      Message p = mMessages;
      if (when != 0) {
        while (p != null && p.when <= when) {
          prev = p;
          p = p.next;
        }
      }
      if (prev != null) { // invariant: p == prev.next
        msg.next = p;
        prev.next = msg;
      } else {
        msg.next = p;
        mMessages = msg;
      }
      return token;
    }
}
//移除同步屏障
public void removeSyncBarrier(int token) {
  	// Remove a sync barrier token from the queue.
    // If the queue is no longer stalled by a barrier then wake it.
    synchronized (this) {
      Message prev = null;
      Message p = mMessages;
      //3.遍历消息列表，删除屏障消息
      while (p != null && (p.target != null || p.arg1 != token)) {
        prev = p;
        p = p.next;
      }
      if (p == null) {
        throw new IllegalStateException("The specified message queue synchronization "
                   + " barrier token has not been posted or has already been removed.");
      }
      //4.回复执行普通消息
      final boolean needWake;
      if (prev != null) {
        prev.next = p.next;
        needWake = false;
      } else {
        mMessages = p.next;
        needWake = mMessages == null || mMessages.target != null;
      }
      //5.回收消息
      p.recycleUnchecked();
      // If the loop is quitting then it is already awake.
      // We can assume mPtr != 0 when mQuitting is false.
      if (needWake && !mQuitting) {
        nativeWake(mPtr);
      }
    }
}
```

1. 从消息缓存池中分配一个消息，并初始化。
2. 将屏障消息按照when插入指定位置。
3. 遍历消息列表，删除屏障消息。
4. 回复执行普通消息。
5. 回收消息。

##### <span id="mq-insert">消息插入</span>

```java
boolean enqueueMessage(Message msg, long when) {
    //...
    synchronized (this) {
       //...
        msg.markInUse();
        msg.when = when;
        Message p = mMessages;
        boolean needWake;
      	//1.如果消息队列没有消息、插入消息立即执行、插入消息比消息队列头的消息先执行，将该消息插入消息队列头，如果当前当前队列被阻塞，则唤醒。
        if (p == null || when == 0 || when < p.when) {
            // New head, wake up the event queue if blocked.
            msg.next = p;
            mMessages = msg;
            needWake = mBlocked;
        } else {
         		 //2.将要插入的消息按照when排序插入到指定位置，并标记是否需要唤醒阻塞队列
            // Inserted within the middle of the queue.  Usually we don't have to wake
            // up the event queue unless there is a barrier at the head of the queue
            // and the message is the earliest asynchronous message in the queue.
            needWake = mBlocked && p.target == null && msg.isAsynchronous();
            Message prev;
            for (;;) {
                prev = p;
                p = p.next;
                if (p == null || when < p.when) {
                    break;
                }
                if (needWake && p.isAsynchronous()) {
                    needWake = false;
                }
            }
            msg.next = p; // invariant: p == prev.next
            prev.next = msg;
        }
        // We can assume mPtr != 0 because mQuitting is false.
      	//3.如果需要，唤醒当前当前队列
        if (needWake) {
            nativeWake(mPtr);
        }
    }
    return true;
}
```

1. 如果消息队列没有消息、插入消息立即执行、插入消息比消息队列头的消息先执行，将该消息插入消息队列头，如果当前当前队列被阻塞，则唤醒。

2. 将要插入的消息按照when排序插入到指定位置，并标记是否需要唤醒阻塞队列。

3. 如果需要，唤醒当前当前队列。

   ```c++
   //android10/frameworks/base/core/jni/android_os_MessageQueue.cpp
   static void android_os_MessageQueue_nativeWake(JNIEnv* env, jclass clazz, jlong ptr) {
       NativeMessageQueue* nativeMessageQueue = reinterpret_cast<NativeMessageQueue*>(ptr);
       nativeMessageQueue->wake();
   }
   void NativeMessageQueue::wake() {
       mLooper->wake();
   }
   ```

##### <span id="mq-get">消息获取</span>

> native层的事件先于java消息执行。

###### Java层消息处理

```java
Message next() {
    // Return here if the message loop has already quit and been disposed.
    // This can happen if the application tries to restart a looper after quit
    // which is not supported.
    final long ptr = mPtr;
    if (ptr == 0) {
        return null;
    }
    int pendingIdleHandlerCount = -1; // -1 only during first iteration
    int nextPollTimeoutMillis = 0;
    for (;;) {
        if (nextPollTimeoutMillis != 0) {
            Binder.flushPendingCommands();
        }
				//1.阻塞java层消息循环直到nextPollTimeoutMillis超时，但会执行native消息。
        nativePollOnce(ptr, nextPollTimeoutMillis);
        synchronized (this) {
            // Try to retrieve the next message.  Return if found.
            final long now = SystemClock.uptimeMillis();
            Message prevMsg = null;
            Message msg = mMessages;
          	//2.如果有消息屏障，遍历消息队列，处理异步消息
            if (msg != null && msg.target == null) {
                // Stalled by a barrier.  Find the next asynchronous message in the queue.
                do {
                    prevMsg = msg;
                    msg = msg.next;
                } while (msg != null && !msg.isAsynchronous());
            }
          	//3.消息处理
          	//3.1有消息要处理
            if (msg != null) {
              	//3.1.1消息未到处理时间
                if (now < msg.when) {
                    // Next message is not ready.  Set a timeout to wake up when it is 
                  	//ready.
                    nextPollTimeoutMillis = (int) Math.min(msg.when - now, 			
                                                           Integer.MAX_VALUE);
                } else {
                  	//3.1.2消息已到处理时间
                    // Got a message.
                    mBlocked = false;
                    if (prevMsg != null) {
                        prevMsg.next = msg.next;
                    } else {
                        mMessages = msg.next;
                    }
                    msg.next = null;
                    msg.markInUse();
                    return msg;
                }
            } else {
                // No more messages.
              	//3.2没有消息要处理
                nextPollTimeoutMillis = -1;
            }
						//...
            // If first time idle, then get the number of idlers to run.
            // Idle handles only run if the queue is empty or if the first message
            // in the queue (possibly a barrier) is due to be handled in the future.
          	//4.如果消息队列为空，处理IdleHandler
            if (pendingIdleHandlerCount < 0
                    && (mMessages == null || now < mMessages.when)) {
                pendingIdleHandlerCount = mIdleHandlers.size();
            }
            if (pendingIdleHandlerCount <= 0) {
                // No idle handlers to run.  Loop and wait some more.
                mBlocked = true;
                continue;
            }
            if (mPendingIdleHandlers == null) {
                mPendingIdleHandlers = new IdleHandler[Math.max(pendingIdleHandlerCount, 
                                                                4)];
            }
            mPendingIdleHandlers = mIdleHandlers.toArray(mPendingIdleHandlers);
        }

        // Run the idle handlers.
        // We only ever reach this code block during the first iteration.
        for (int i = 0; i < pendingIdleHandlerCount; i++) {
            final IdleHandler idler = mPendingIdleHandlers[i];
            mPendingIdleHandlers[i] = null; // release the reference to the handler
            boolean keep = false;
            try {
                keep = idler.queueIdle();
            } catch (Throwable t) {
                Log.wtf(TAG, "IdleHandler threw exception", t);
            }
            if (!keep) {
                synchronized (this) {
                    mIdleHandlers.remove(idler);
                }
            }
        }
        // Reset the idle handler count to 0 so we do not run them again.
        pendingIdleHandlerCount = 0;
        // While calling an idle handler, a new message could have been delivered
        // so go back and look again for a pending message without waiting.
        nextPollTimeoutMillis = 0;
    }
}
```

1. 阻塞java层消息循环直到nextPollTimeoutMillis超时，但会执行native消息。nextPollTimeoutMillis = -1阻塞当前队列。
2. 如果有消息屏障，遍历消息队列，处理异步消息。
3. 消息处理：
   1. 有消息要处理。
      1. 消息未到处理时间，睡眠剩余时间后唤醒再执行。
      2. 消息已到处理时间，返回该消息给Looper处理。
   2. 没有消息要处理，阻塞Java层消息队列，处理native消息，然后阻塞。
4. 如果消息队列为空，处理IdleHandler任务。

###### Native层消息处理

```c++
//android10/frameworks/base/core/jni/android_os_MessageQueue.cpp
static void android_os_MessageQueue_nativePollOnce(JNIEnv* env, jobject obj,
        jlong ptr, jint timeoutMillis) {
    NativeMessageQueue* nativeMessageQueue = reinterpret_cast<NativeMessageQueue*>(ptr);
    nativeMessageQueue->pollOnce(env, obj, timeoutMillis);
}
void NativeMessageQueue::pollOnce(JNIEnv* env, jobject pollObj, int timeoutMillis) {
    mPollEnv = env;
    mPollObj = pollObj;
    mLooper->pollOnce(timeoutMillis);
    mPollObj = NULL;
    mPollEnv = NULL;
    if (mExceptionObj) {
        env->Throw(mExceptionObj);
        env->DeleteLocalRef(mExceptionObj);
        mExceptionObj = NULL;
    }
}
```

```c++
//android10/system/core/libutils/include/utils/Looper.h
inline int pollOnce(int timeoutMillis) {
    return pollOnce(timeoutMillis, nullptr, nullptr, nullptr);
}
//android10/system/core/libutils/include/utils/Looper.cpp
int Looper::pollOnce(int timeoutMillis, int* outFd, int* outEvents, void** outData) {
    int result = 0;
    for (;;) {
      	//...无关代码
        if (result != 0) {
            //...
            return result;
        }
        result = pollInner(timeoutMillis);
    }
}
int Looper::pollInner(int timeoutMillis) {
    // Adjust the timeout based on when the next message is due.
  	//1.根据下一个消息的执行时间调整timeoutMillis
    if (timeoutMillis != 0 && mNextMessageUptime != LLONG_MAX) {
        nsecs_t now = systemTime(SYSTEM_TIME_MONOTONIC);
        int messageTimeoutMillis = toMillisecondTimeoutDelay(now, mNextMessageUptime);
        if (messageTimeoutMillis >= 0
                && (timeoutMillis < 0 || messageTimeoutMillis < timeoutMillis)) {
            timeoutMillis = messageTimeoutMillis;
        }
    }

    // Poll.
    int result = POLL_WAKE;
    mResponses.clear();
    mResponseIndex = 0;
    // We are about to idle.
    mPolling = true;
    struct epoll_event eventItems[EPOLL_MAX_EVENTS];
  	//2.通过epoll机制阻塞当前队列，等待事件唤醒
    int eventCount = epoll_wait(mEpollFd.get(), eventItems, EPOLL_MAX_EVENTS, timeoutMillis);
    // No longer idling.
    mPolling = false;
    // Acquire lock.
    mLock.lock();
    //...边界处理
    // Handle all events.
  	//3.处理epoll里取出的Callback事件并添加到mResponses
    for (int i = 0; i < eventCount; i++) {
        int fd = eventItems[i].data.fd;
        uint32_t epollEvents = eventItems[i].events;
        if (fd == mWakeEventFd.get()) {
            if (epollEvents & EPOLLIN) {
                awoken();
            } 
        } else {
            ssize_t requestIndex = mRequests.indexOfKey(fd);
            if (requestIndex >= 0) {
                int events = 0;
                if (epollEvents & EPOLLIN) events |= EVENT_INPUT;
                if (epollEvents & EPOLLOUT) events |= EVENT_OUTPUT;
                if (epollEvents & EPOLLERR) events |= EVENT_ERROR;
                if (epollEvents & EPOLLHUP) events |= EVENT_HANGUP;
                pushResponse(events, mRequests.valueAt(requestIndex));
            }
        }
    }
Done: ;
		//4.处理事件
    // Invoke pending message callbacks.
  	//4.1处理Message类型事件
    mNextMessageUptime = LLONG_MAX;
    while (mMessageEnvelopes.size() != 0) {
        nsecs_t now = systemTime(SYSTEM_TIME_MONOTONIC);
        const MessageEnvelope& messageEnvelope = mMessageEnvelopes.itemAt(0);
        if (messageEnvelope.uptime <= now) {
            // Remove the envelope from the list.
            // We keep a strong reference to the handler until the call to handleMessage
            // finishes.  Then we drop it so that the handler can be deleted *before*
            // we reacquire our lock.
            { // obtain handler
                sp<MessageHandler> handler = messageEnvelope.handler;
                Message message = messageEnvelope.message;
                mMessageEnvelopes.removeAt(0);
                mSendingMessage = true;
                mLock.unlock();

                handler->handleMessage(message);
            } // release handler

            mLock.lock();
            mSendingMessage = false;
            result = POLL_CALLBACK;
        } else {
            // The last message left at the head of the queue determines the next wakeup time.
            mNextMessageUptime = messageEnvelope.uptime;
            break;
        }
    }

    // Release lock.
    mLock.unlock();
		//4.2处理Callback类型事件
    // Invoke all response callbacks.
    for (size_t i = 0; i < mResponses.size(); i++) {
        Response& response = mResponses.editItemAt(i);
        if (response.request.ident == POLL_CALLBACK) {
            int fd = response.request.fd;
            int events = response.events;
            void* data = response.request.data;

            // Invoke the callback.  Note that the file descriptor may be closed by
            // the callback (and potentially even reused) before the function returns so
            // we need to be a little careful when removing the file descriptor afterwards.
            int callbackResult = response.request.callback->handleEvent(fd, events, data);
            if (callbackResult == 0) {
                removeFd(fd, response.request.seq);
            }

            // Clear the callback reference in the response structure promptly because we
            // will not clear the response vector itself until the next poll.
            response.request.callback.clear();
            result = POLL_CALLBACK;
        }
    }
    return result;
}
```

1. 根据下一个消息的执行时间调整timeoutMillis。
2. 通过epoll机制阻塞当前队列，等待事件唤醒，超时时间timeoutMillis。
3. 处理epoll里取出的Callback事件并添加到mResponses。
4. 处理事件：
   1. 处理mMessageEnvelopes中的Message类型事件。
   2. 处理mResponses中的Callback类型事件。

#### <span id="looper">Looper</span>

* sThreadLocal

  ```java
  static final ThreadLocal<Looper> sThreadLocal = new ThreadLocal<Looper>();
  ```

  `通过线程变量保证每个线程只有一个Looper。`

* sMainLooper

  ```java
  //Looper
  private static Looper sMainLooper; 
  ```

  ```java
  //android10/frameworks/base/core/java/android/app/ActivityThread.java
  public static void main(String[] args) {
      //...
      Looper.prepareMainLooper();
  		//...
      Looper.loop();
  }
  ```

  ```java
  //Looper
  public static void prepareMainLooper() {
      prepare(false);
      synchronized (Looper.class) {
          if (sMainLooper != null) {
              throw new IllegalStateException("The main Looper has already been prepared.");
          }
          sMainLooper = myLooper();
      }
  }
  private static void prepare(boolean quitAllowed) {
    	//如果线程变量的值不为空，说明已经创建Looper对象，则报错
      if (sThreadLocal.get() != null) {
        throw new RuntimeException("Only one Looper may be created per thread");
      }
      sThreadLocal.set(new Looper(quitAllowed));
  }
  public static @Nullable Looper myLooper() {
   	  return sThreadLocal.get();
  }
  ```

  

* mLogging

  ```java
  //Looper
  private Printer mLogging;
  public void setMessageLogging(@Nullable Printer printer) {
   	 mLogging = printer;
  }
  ```

  > 可用通过设置Printer获取Looper打印的日志，进而分析Message的执行情况。

* mQueue

  ```java
  final MessageQueue mQueue;
  ```

  > 每个Looper对象都有一个消息队列。

* mThread

  ```java
  final Thread mThread;
  ```

  > 保存当前Looper所属线程。

* sObserver

  `Android API 30`

  ```Java
  private static Observer sObserver;
  ```

  > 回调Messgae处理的每个过程。可以替代分析mLogging的方式分析Message的执行情况。

##### loop

```java
public static void loop() {
    final Looper me = myLooper();
    //...
    me.mInLoop = true;
    final MessageQueue queue = me.mQueue;
    // Make sure the identity of this thread is that of the local process,
    // and keep track of what that identity token actually is.
    for (;;) {
        //1.从消息队列中获取消息，如果没有消息，会阻塞当前线程，直到消息到来。
        Message msg = queue.next(); // might block
        if (msg == null) {
            // No message indicates that the message queue is quitting.
            return;
        }
        // This must be in a local variable, in case a UI event sets the logger
      	//2打印日志，可以通过获取日志来分析主线程卡顿
        final Printer logging = me.mLogging;
        if (logging != null) {
          	//2.1消息执行开始
            logging.println(">>>>> Dispatching to " + msg.target + " " +
                    msg.callback + ": " + msg.what);
        }
        // Make sure the observer won't change while processing a transaction.
        final Observer observer = sObserver;
				//3.可以设置打印Message执行性能。
        final long traceTag = me.mTraceTag;
   			//...
      	//3.1开启是否开启trace
        if (traceTag != 0 && Trace.isTagEnabled(traceTag)) {
            Trace.traceBegin(traceTag, msg.target.getTraceName(msg));
        }
      	//4.通过设置Observer观察Message的执行情况
        Object token = null;
        if (observer != null) {
          	//4.1消息分发开始
            token = observer.messageDispatchStarting();
        }
        long origWorkSource = ThreadLocalWorkSource.setUid(msg.workSourceUid);
        try {
          	//4.2消息分发给目标Handler
            msg.target.dispatchMessage(msg);
          	//4.3消息分发完成
            if (observer != null) {
                observer.messageDispatched(token, msg);
            }
        } catch (Exception exception) {
          	//4.4消息执行抛出异常	
            if (observer != null) {
                observer.dispatchingThrewException(token, msg, exception);
            }
            throw exception;
        } finally {
            ThreadLocalWorkSource.restore(origWorkSource);
          	//3.2结束trace
            if (traceTag != 0) {
                Trace.traceEnd(traceTag);
            }
        }
        //...
      	//2.2消息执行完成
        if (logging != null) {
            logging.println("<<<<< Finished to " + msg.target + " " + msg.callback);
        }
				//...
        msg.recycleUnchecked();
    }
}
```

1. 从消息队列中获取消息，如果没有消息，会阻塞当前线程，直到消息到来。
2. 打印日志，可以通过获取日志来分析主线程卡顿
   1. 消息执行开始
   2. 消息执行完成
3. 可以设置打印Message执行性能。
   1. 开启是否开启trace
   2. 结束trace
4. 通过设置Observer观察Message的执行情况。
   1. 消息分发开始
   2. 消息分发给目标Handler
   3. 消息分发完成
   4. 消息执行抛出异常	

#### <span id="handler">Handler</span>

`命令模式。线程间通信的方式。`

##### <span id="handler-create">创建方式</span>

> 这两种方式可以同时存在，也可以单独存在。

###### 重写方法

`继承Handler或者匿名内部类重写handleMessage方法。`

```java
Handler handler = new Handler(Looper.getMainLooper()) {
    @Override
    public void handleMessage(@NonNull Message msg) {
        super.handleMessage(msg);
    }
};
```

> 当不再使用Handler对象时，比如Activity/Fragment销毁时，应该移除所以消息，防止内存泄漏。

###### Callback

```java
Handler handler = new Handler(new Handler.Callback() {
    @Override
    public boolean handleMessage(@NonNull Message msg) {
        return false;
    }
});
```

> 通过回调的方式处理消息，如果返回true，表示该消息处理完成；返回false，继续交给handleMessage方法处理。

##### <span id="handler-dispatch">消息分发</span>

```java
public void dispatchMessage(@NonNull Message msg) {
    if (msg.callback != null) {
      handleCallback(msg);
    } else {
      if (mCallback != null) {
        if (mCallback.handleMessage(msg)) {
          return;
        }
      }
      handleMessage(msg);
    }
}
private static void handleCallback(Message message) {
  	message.callback.run();
}
```

1. 如果Message的callback不为空，运行callback。
2. 如果Handler的mCallback不为空，交由mCallback处理，如果返回true直接返回，否则交由handleMessage处理。
3. 交由handleMessage处理。

##### <span id="handler-send">发送消息</span>

###### send

* ```java
  public final boolean sendMessage(@NonNull Message msg) 
  ```

* ```java
  public final boolean sendMessageAtFrontOfQueue(@NonNull Message msg)
  ```

* ```java
  public boolean sendMessageAtTime(@NonNull Message msg, long uptimeMillis) 
  ```

* ```java
  public final boolean sendMessageDelayed(@NonNull Message msg, long delayMillis)
  ```

* ```java
  public final boolean sendEmptyMessage(int what)
  ```

* ```java
  public final boolean sendEmptyMessageAtTime(int what, long uptimeMillis) 
  ```

  > uptimeMillis从开机算起，不包括CPU睡眠时间的绝对时间。

* ```java
  public final boolean sendEmptyMessageDelayed(int what, long delayMillis)
  ```

  > 会自动计算SystemClock.uptimeMillis() 加上delayMillis。

> send*方式发送的消息在处理消息时，只走[消息分发](#dispatch)的2、3步骤。

###### post

* ```java
  public final boolean post(@NonNull Runnable r)
  ```

* ```java
  public final boolean postAtFrontOfQueue(@NonNull Runnable r) 
  ```

* ```java
  public final boolean postAtTime(@NonNull Runnable r, long uptimeMillis) 
  ```

* ```java
  public final boolean postAtTime(@NonNull Runnable r, @Nullable Object token, long uptimeMillis) 
  ```

* ```java
  public final boolean postDelayed(@NonNull Runnable r, long delayMillis)
  ```

* ```java
  public final boolean postDelayed(@NonNull Runnable r, @Nullable Object token, long delayMillis)
  ```

> post*方式发送的消息通过设置Message的callback = Runnable，在处理消息时，只走[消息分发](#dispatch)的1步骤。

###### 消息延迟

* 指定时间点执行

  > uptimeMillis
  >
  > 在指定的时间点执行，计算方式是从开机到现在的时间，不包括CPU深度睡眠的时间。
  >
  > SystemClock.uptimeMillis() 获取从开机到现在的时间，不包括深度睡眠时间。

* 定时时间执行

  > delayMillis
  >
  > 本质上是获取SystemClock.uptimeMillis() +delayMillis，在这个时间点上执行。

```java
private boolean enqueueMessage(@NonNull MessageQueue queue, @NonNull Message msg,
        long uptimeMillis) {
    msg.target = this;
    msg.workSourceUid = ThreadLocalWorkSource.getUid();

    if (mAsynchronous) {
        msg.setAsynchronous(true);
    }
    return queue.enqueueMessage(msg, uptimeMillis);
}
```

> 上面两种延迟消息最终都是调用该方法，继而调用MessageQueue的[插入消息](#mq-insert)。

###### runWithScissors

```java
public final boolean runWithScissors(@NonNull Runnable r, long timeout) {
    //...
    BlockingRunnable br = new BlockingRunnable(r);
    return br.postAndWait(this, timeout);
}
private static final class BlockingRunnable implements Runnable {
    private final Runnable mTask;
    private boolean mDone;

    public BlockingRunnable(Runnable task) {
      mTask = task;
    }

    @Override
    public void run() {
      try {
        mTask.run();
      } finally {
        synchronized (this) {
          mDone = true;
          notifyAll();
        }
      }
    }

    public boolean postAndWait(Handler handler, long timeout) {
      if (!handler.post(this)) {
        return false;
      }

      synchronized (this) {
        if (timeout > 0) {
          final long expirationTime = SystemClock.uptimeMillis() + timeout;
          while (!mDone) {
            long delay = expirationTime - SystemClock.uptimeMillis();
            if (delay <= 0) {
              return false; // timeout
            }
            try {
              wait(delay);
            } catch (InterruptedException ex) {
            }
          }
        } else {
          while (!mDone) {
            try {
              wait();
            } catch (InterruptedException ex) {
            }
          }
        }
      }
      return true;
    }
}
```

> 会阻塞当前线程直到runnable执行完成。

```java
//android10/frameworks/base/services/core/java/com/android/server/wm/WindowManagerService.java
public static WindowManagerService main(final Context context, final InputManagerService im,
        final boolean showBootMsgs, final boolean onlyCore, WindowManagerPolicy policy,
        ActivityTaskManagerService atm, Supplier<SurfaceControl.Transaction> transactionFactory,
        Supplier<Surface> surfaceFactory,
        Function<SurfaceSession, SurfaceControl.Builder> surfaceControlFactory) {
  	//在DisplayThread线程初始化，在当前线程返回。
    DisplayThread.getHandler().runWithScissors(() ->
            sInstance = new WindowManagerService(context, im, showBootMsgs, onlyCore, policy,
                    atm, transactionFactory, surfaceFactory, surfaceControlFactory), 0);
    return sInstance;
}
```

#####  <span id="handler-remove">移除消息</span>

###### Runnable

* ```java
  public final void removeCallbacks(@NonNull Runnable r) {
      mQueue.removeMessages(this, r, null);
  }
  ```

* ```java
  public final void removeCallbacks(@NonNull Runnable r, @Nullable Object token) {
      mQueue.removeMessages(this, r, token);
  }
  ```

```java
//MessageQueue
void removeMessages(Handler h, Runnable r, Object object) {
    if (h == null || r == null) {
        return;
    }
    synchronized (this) {
        Message p = mMessages;
        // Remove all messages at front.
        while (p != null && p.target == h && p.callback == r
               && (object == null || p.obj == object)) {
            Message n = p.next;
            mMessages = n;
            p.recycleUnchecked();
            p = n;
        }
        // Remove all messages after front.
        while (p != null) {
            Message n = p.next;
            if (n != null) {
                if (n.target == h && n.callback == r
                    && (object == null || n.obj == object)) {
                    Message nn = n.next;
                    n.recycleUnchecked();
                    p.next = nn;
                    continue;
                }
            }
            p = n;
        }
    }
}
```

> 遍历消息列表，移除h(Handler)中所有r(Runnable)消息。

###### what

* ```java
  public final void removeMessages(int what) {
      mQueue.removeMessages(this, what, null);
  }
  ```

* ```java
  public final void removeMessages(int what, @Nullable Object object) {
      mQueue.removeMessages(this, what, object);
  }
  ```

```java
//MessageQueue
void removeMessages(Handler h, int what, Object object) {
    if (h == null) {
      return;
    }
    synchronized (this) {
      Message p = mMessages;

      // Remove all messages at front.
      while (p != null && p.target == h && p.what == what
             && (object == null || p.obj == object)) {
        Message n = p.next;
        mMessages = n;
        p.recycleUnchecked();
        p = n;
      }

      // Remove all messages after front.
      while (p != null) {
        Message n = p.next;
        if (n != null) {
          if (n.target == h && n.what == what
              && (object == null || n.obj == object)) {
            Message nn = n.next;
            n.recycleUnchecked();
            p.next = nn;
            continue;
          }
        }
        p = n;
      }
    }
}
```

> 遍历消息列表，移除h(Handler)中所有what消息。

###### token

* ```java
  public final void removeCallbacksAndMessages(@Nullable Object token) {
      mQueue.removeCallbacksAndMessages(this, token);
  }
  ```

```java
//MessageQueue
void removeCallbacksAndMessages(Handler h, Object object) {
    if (h == null) {
        return;
    }
    synchronized (this) {
        Message p = mMessages;

        // Remove all messages at front.
        while (p != null && p.target == h
                && (object == null || p.obj == object)) {
            Message n = p.next;
            mMessages = n;
            p.recycleUnchecked();
            p = n;
        }

        // Remove all messages after front.
        while (p != null) {
            Message n = p.next;
            if (n != null) {
                if (n.target == h && (object == null || n.obj == object)) {
                    Message nn = n.next;
                    n.recycleUnchecked();
                    p.next = nn;
                    continue;
                }
            }
            p = n;
        }
    }
}
```

> 遍历消息列表，移除h(Handler)中所有token消息。
>
> 如果token为null，则移除消息队列中所有消息。

##### <span id="handler-apply">应用</span>

###### VirtualAPK

```java

public class PluginManager {
    private static volatile PluginManager sInstance = null;
		//...
    public static PluginManager getInstance(Context base) {
        if (sInstance == null) {
            synchronized (PluginManager.class) {
                if (sInstance == null) {
                    sInstance = createInstance(base);
                }
            }
        }

        return sInstance;
    }
    
    private static PluginManager createInstance(Context context) {
       //...
        return new PluginManager(context);
    }

    protected PluginManager(Context context) {
        //...
        hookCurrentProcess();
    }

    protected void hookCurrentProcess() {
        hookInstrumentationAndHandler();
   			//...
    }

    protected void hookInstrumentationAndHandler() {
        try {
            ActivityThread activityThread = ActivityThread.currentActivityThread();
            Instrumentation baseInstrumentation = activityThread.getInstrumentation();
            final VAInstrumentation instrumentation = createInstrumentation(baseInstrumentation);        
            Reflector.with(activityThread).field("mInstrumentation").set(instrumentation);
            Handler mainHandler = Reflector.with(activityThread).method("getHandler").call();
            Reflector.with(mainHandler).field("mCallback").set(instrumentation);
            this.mInstrumentation = instrumentation;
        } catch (Exception e) {
            
        }
    }

}
public class ActivityThread{
  
  final Handler getHandler() {
        return mH;
    }
}
public class VAInstrumentation extends Instrumentation implements Handler.Callback {
		//...
    @Override
    public boolean handleMessage(Message msg) {
        if (msg.what == LAUNCH_ACTIVITY) {
            // ActivityClientRecord r
            Object r = msg.obj;
            try {
                Reflector reflector = Reflector.with(r);
                Intent intent = reflector.field("intent").get();
                intent.setExtrasClassLoader(mPluginManager.getHostContext().getClassLoader());
                ActivityInfo activityInfo = reflector.field("activityInfo").get();

                if (PluginUtil.isIntentFromPlugin(intent)) {
                    int theme = PluginUtil.getTheme(mPluginManager.getHostContext(), intent);
                    if (theme != 0) {
                        activityInfo.theme = theme;
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, e);
            }
        }
        return false;
    }
}
```

> 由[消息分发](#handler-dispatch)可知，如果设置了Callback，其优先处理。通过反射设置ActivityThread中mH(继承Handler的H)的Callback拦截Activity的创建流程，来将插件中Activity的主题替换坑位Activity的主题。

###### LeakCannay

```kotlin
class ServiceWatcher(private val reachabilityWatcher: ReachabilityWatcher) : InstallableWatcher {

  private val servicesToBeDestroyed = WeakHashMap<IBinder, WeakReference<Service>>()

  private val activityThreadClass by lazy { Class.forName("android.app.ActivityThread") }

  private val activityThreadInstance by lazy {
    activityThreadClass.getDeclaredMethod("currentActivityThread").invoke(null)!!
  }

  private val activityThreadServices by lazy {
    val mServicesField =
      activityThreadClass.getDeclaredField("mServices").apply { isAccessible = true }

    @Suppress("UNCHECKED_CAST")
    mServicesField[activityThreadInstance] as Map<IBinder, Service>
  }

  private var uninstallActivityThreadHandlerCallback: (() -> Unit)? = null
  //...

  override fun install() {
    checkMainThread()

    try {
      swapActivityThreadHandlerCallback { mCallback ->
        //...
        Handler.Callback { msg ->
          if (msg.obj !is IBinder) {
            return@Callback false
          }
          if (msg.what == STOP_SERVICE) {
            //处理Service销毁事件
          }
          mCallback?.handleMessage(msg) ?: false
        }
      }
      //...
    } catch (ignored: Throwable) {}
  }
  //...
  private fun swapActivityThreadHandlerCallback(swap: (Handler.Callback?) -> Handler.Callback?) {
    val mHField =
      activityThreadClass.getDeclaredField("mH").apply { isAccessible = true }
    val mH = mHField[activityThreadInstance] as Handler
    val mCallbackField =
      Handler::class.java.getDeclaredField("mCallback").apply { isAccessible = true }
    val mCallback = mCallbackField[mH] as Handler.Callback?
    mCallbackField[mH] = swap(mCallback)
  }
  //...
}
```

> 拦截mH的mCallback，处理STOP_SERVICE事件，但又不会替换原先的mCallback。
>
> 不论是bindService还是stopService的方式，销毁Service都会发送STOP_SERVICE事件。

###### Tinker

```java
public final class TinkerApplicationInlineFence extends Handler {
    private final ApplicationLike mAppLike;

    public TinkerApplicationInlineFence(ApplicationLike appLike) {
        mAppLike = appLike;
    }

    @Override
    public void handleMessage(Message msg) {
        handleMessage_$noinline$(msg);
    }

    private void handleMessage_$noinline$(Message msg) {
        try {
            dummyThrowExceptionMethod();
        } finally {
            handleMessageImpl(msg);
        }
    }

    @Override
    public void dispatchMessage(Message msg) {
        // Any requests come from dispatchMessage are unexpected. Ignore them should be ok.
    }

    private void handleMessageImpl(Message msg) {
        switch (msg.what) {
            case ACTION_ON_BASE_CONTEXT_ATTACHED: {
                mAppLike.onBaseContextAttached((Context) msg.obj);
                break;
            }
            case ACTION_ON_CREATE: {
                mAppLike.onCreate();
                break;
            }
            case ACTION_ON_CONFIGURATION_CHANGED: {
                mAppLike.onConfigurationChanged((Configuration) msg.obj);
                break;
            }
            case ACTION_ON_TRIM_MEMORY: {
                mAppLike.onTrimMemory((Integer) msg.obj);
                break;
            }
            case ACTION_ON_LOW_MEMORY: {
                mAppLike.onLowMemory();
                break;
            }
            case ACTION_ON_TERMINATE: {
                mAppLike.onTerminate();
                break;
            }
            case ACTION_GET_CLASSLOADER: {
                msg.obj = mAppLike.getClassLoader((ClassLoader) msg.obj);
                break;
            }
            case ACTION_GET_BASE_CONTEXT: {
                msg.obj = mAppLike.getBaseContext((Context) msg.obj);
                break;
            }
            case ACTION_GET_ASSETS: {
                msg.obj = mAppLike.getAssets((AssetManager) msg.obj);
                break;
            }
            case ACTION_GET_RESOURCES : {
                msg.obj = mAppLike.getResources((Resources) msg.obj);
                break;
            }
            case ACTION_GET_SYSTEM_SERVICE : {
                final Object[] params = (Object[]) msg.obj;
                msg.obj = mAppLike.getSystemService((String) params[0], params[1]);
                break;
            }
            case ACTION_MZ_NIGHTMODE_USE_OF: {
                msg.obj = mAppLike.mzNightModeUseOf();
                break;
            }
            default: {
                throw new IllegalStateException("Should not be here.");
            }
        }
    }

    private static void dummyThrowExceptionMethod() {
        if (TinkerApplicationInlineFence.class.isPrimitive()) {
            throw new RuntimeException();
        }
    }
}
```

```java
public final class TinkerInlineFenceAction {
    public static final int ACTION_ON_BASE_CONTEXT_ATTACHED = 1;
    public static final int ACTION_ON_CREATE = 2;
    public static final int ACTION_ON_CONFIGURATION_CHANGED = 3;
    public static final int ACTION_ON_TRIM_MEMORY = 4;
    public static final int ACTION_ON_LOW_MEMORY = 5;
    public static final int ACTION_ON_TERMINATE = 6;
    public static final int ACTION_GET_CLASSLOADER = 7;
    public static final int ACTION_GET_BASE_CONTEXT = 8;
    public static final int ACTION_GET_ASSETS = 9;
    public static final int ACTION_GET_RESOURCES = 10;
    public static final int ACTION_GET_SYSTEM_SERVICE = 11;
    public static final int ACTION_MZ_NIGHTMODE_USE_OF = 12;

    static void callOnBaseContextAttached(Handler inlineFence, Context context) {
        Message msg = null;
        try {
            msg = Message.obtain(inlineFence, ACTION_ON_BASE_CONTEXT_ATTACHED, context);
            inlineFence.handleMessage(msg);
        } finally {
            msg.recycle();
        }
    }

    static void callOnCreate(Handler inlineFence) {
        Message msg = null;
        try {
            msg = Message.obtain(inlineFence, ACTION_ON_CREATE);
            inlineFence.handleMessage(msg);
        } finally {
            msg.recycle();
        }
    }

    static void callOnConfigurationChanged(Handler inlineFence, Configuration newConfig) {
        Message msg = null;
        try {
            msg = Message.obtain(inlineFence, ACTION_ON_CONFIGURATION_CHANGED, newConfig);
            inlineFence.handleMessage(msg);
        } finally {
            msg.recycle();
        }
    }

    static void callOnTrimMemory(Handler inlineFence, int level) {
        Message msg = null;
        try {
            msg = Message.obtain(inlineFence, ACTION_ON_TRIM_MEMORY, level);
            inlineFence.handleMessage(msg);
        } finally {
            msg.recycle();
        }
    }

    static void callOnLowMemory(Handler inlineFence) {
        Message msg = null;
        try {
            msg = Message.obtain(inlineFence, ACTION_ON_LOW_MEMORY);
            inlineFence.handleMessage(msg);
        } finally {
            msg.recycle();
        }
    }

    static void callOnTerminate(Handler inlineFence) {
        Message msg = null;
        try {
            msg = Message.obtain(inlineFence, ACTION_ON_TERMINATE);
            inlineFence.handleMessage(msg);
        } finally {
            msg.recycle();
        }
    }

    static ClassLoader callGetClassLoader(Handler inlineFence, ClassLoader cl) {
        Message msg = null;
        try {
            msg = Message.obtain(inlineFence, ACTION_GET_CLASSLOADER, cl);
            inlineFence.handleMessage(msg);
            return (ClassLoader) msg.obj;
        } finally {
            msg.recycle();
        }
    }

    static Context callGetBaseContext(Handler inlineFence, Context base) {
        Message msg = null;
        try {
            msg = Message.obtain(inlineFence, ACTION_GET_BASE_CONTEXT, base);
            inlineFence.handleMessage(msg);
            return (Context) msg.obj;
        } finally {
            msg.recycle();
        }
    }

    static AssetManager callGetAssets(Handler inlineFence, AssetManager assets) {
        Message msg = null;
        try {
            msg = Message.obtain(inlineFence, ACTION_GET_ASSETS, assets);
            inlineFence.handleMessage(msg);
            return (AssetManager) msg.obj;
        } finally {
            msg.recycle();
        }
    }

    static Resources callGetResources(Handler inlineFence, Resources res) {
        Message msg = null;
        try {
            msg = Message.obtain(inlineFence, ACTION_GET_RESOURCES, res);
            inlineFence.handleMessage(msg);
            return (Resources) msg.obj;
        } finally {
            msg.recycle();
        }
    }

    static Object callGetSystemService(Handler inlineFence, String name, Object service) {
        Message msg = null;
        try {
            msg = Message.obtain(inlineFence, ACTION_GET_SYSTEM_SERVICE, new Object[]{name, service});
            inlineFence.handleMessage(msg);
            return msg.obj;
        } finally {
            msg.recycle();
        }
    }

    static int callMZNightModeUseOf(Handler inlineFence) {
        Message msg = null;
        try {
            msg = Message.obtain(inlineFence, ACTION_MZ_NIGHTMODE_USE_OF);
            inlineFence.handleMessage(msg);
            return (int) msg.obj;
        } finally {
            msg.recycle();
        }
    }
}
```

```java
public abstract class TinkerApplication extends Application {
   	//...
    private Handler mInlineFence = null;
   	//...

    private Handler createInlineFence(Application app,int tinkerFlags,String delegateClassName,
                           boolean tinkerLoadVerifyFlag,long applicationStartElapsedTime,
                                      long applicationStartMillisTime,Intent resultIntent) {
        try {
            //...
            final Class<?> inlineFenceClass = Class.forName(
                    "com.tencent.tinker.entry.TinkerApplicationInlineFence", false, mCurrentClassLoader);
            final Class<?> appLikeClass = Class.forName(
                    "com.tencent.tinker.entry.ApplicationLike", false, mCurrentClassLoader);
            final Constructor<?> inlineFenceCtor = inlineFenceClass.getConstructor(appLikeClass);
            inlineFenceCtor.setAccessible(true);
            return (Handler) inlineFenceCtor.newInstance(appLike);
        } catch (Throwable thr) {
            throw new TinkerRuntimeException("createInlineFence failed", thr);
        }
    }

    protected void onBaseContextAttached(Context base, long applicationStartElapsedTime, long applicationStartMillisTime) {
        try {
            loadTinker();
            mCurrentClassLoader = base.getClassLoader();
            mInlineFence = createInlineFence(this, tinkerFlags, delegateClassName,
                    tinkerLoadVerifyFlag, applicationStartElapsedTime, applicationStartMillisTime,
                    tinkerResultIntent);
            TinkerInlineFenceAction.callOnBaseContextAttached(mInlineFence, base);
            //... 
            }
        } catch (TinkerRuntimeException e) {
        }
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        //...
        onBaseContextAttached(base, applicationStartElapsedTime, applicationStartMillisTime);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (mInlineFence == null) {
            return;
        }
        TinkerInlineFenceAction.callOnCreate(mInlineFence);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        if (mInlineFence == null) {
            return;
        }
        TinkerInlineFenceAction.callOnTerminate(mInlineFence);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mInlineFence == null) {
            return;
        }
        TinkerInlineFenceAction.callOnLowMemory(mInlineFence);
    }

    @TargetApi(14)
    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (mInlineFence == null) {
            return;
        }
        TinkerInlineFenceAction.callOnTrimMemory(mInlineFence, level);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mInlineFence == null) {
            return;
        }
        TinkerInlineFenceAction.callOnConfigurationChanged(mInlineFence, newConfig);
    }

    @Override
    public Resources getResources() {
        final Resources resources = super.getResources();
        if (mInlineFence == null) {
            return resources;
        }
        return TinkerInlineFenceAction.callGetResources(mInlineFence, resources);
    }

    @Override
    public ClassLoader getClassLoader() {
        final ClassLoader classLoader = super.getClassLoader();
        if (mInlineFence == null) {
            return classLoader;
        }
        return TinkerInlineFenceAction.callGetClassLoader(mInlineFence, classLoader);
    }

    @Override
    public AssetManager getAssets() {
        final AssetManager assets = super.getAssets();
        if (mInlineFence == null) {
            return assets;
        }
        return TinkerInlineFenceAction.callGetAssets(mInlineFence, assets);
    }

    @Override
    public Object getSystemService(String name) {
        final Object service = super.getSystemService(name);
        if (mInlineFence == null) {
            return service;
        }
        return TinkerInlineFenceAction.callGetSystemService(mInlineFence, name, service);
    }

    @Override
    public Context getBaseContext() {
        final Context base = super.getBaseContext();
        if (mInlineFence == null) {
            return base;
        }
        return TinkerInlineFenceAction.callGetBaseContext(mInlineFence, base);
    }

    @Keep
    public int mzNightModeUseOf() {
        if (mInlineFence == null) {
            // Return 1 for default according to MeiZu's announcement.
            return 1;
        }
        return TinkerInlineFenceAction.callMZNightModeUseOf(mInlineFence);
    }

    public void setUseSafeMode(boolean useSafeMode) {
        this.useSafeMode = useSafeMode;
    }

    public boolean isTinkerLoadVerifyFlag() {
        return tinkerLoadVerifyFlag;
    }

    public int getTinkerFlags() {
        return tinkerFlags;
    }

    public boolean isUseDelegateLastClassLoader() {
        return useDelegateLastClassLoader;
    }
}
```

> 因为一个app的Application除非重新安装，否则无法被替换。Tinker通过继承Handler连接TinkerApplication和自定义ApplicationLike的通信，避免两者之间除了系统类之外有其他依赖，通过反射尽量减少TinkerApplication对其他类的强依赖，这样可以被修复的类就会更多。

###### Glide

```java
//Glide
Glide.with(this);
public static RequestManager with(@NonNull FragmentActivity activity) {
  	return getRetriever(activity).get(activity);
}
//RequestManagerRetriever
@NonNull
public RequestManager get(@NonNull FragmentActivity activity) {
    if (Util.isOnBackgroundThread()) {
      return get(activity.getApplicationContext());
    } else {
      //...
      return supportFragmentGet(activity, fm, /*parentHint=*/ null, isActivityVisible(activity));
    }
}

@NonNull
private RequestManager supportFragmentGet(
    @NonNull Context context,
    @NonNull FragmentManager fm,
    @Nullable Fragment parentHint,
    boolean isParentVisible) {
    SupportRequestManagerFragment current = getSupportRequestManagerFragment(fm, parentHint);
    RequestManager requestManager = current.getRequestManager();
    //...
    return requestManager;
}
final Map<FragmentManager, SupportRequestManagerFragment> pendingSupportRequestManagerFragments =
      new HashMap<>();
@NonNull
private SupportRequestManagerFragment getSupportRequestManagerFragment(
    @NonNull final FragmentManager fm, @Nullable Fragment parentHint) {
  	//1.通过tag从FragmentManager中获取Fragment。
    SupportRequestManagerFragment current =
      (SupportRequestManagerFragment) fm.findFragmentByTag(FRAGMENT_TAG);
    if (current == null) {
      //2.如果从fm获取失败，从HashMap中获取。
      current = pendingSupportRequestManagerFragments.get(fm);
      if (current == null) {
        //3.如果从HashMap中获取失败，创建一个新的，并添加到map和提交到fm中。
        current = new SupportRequestManagerFragment();
        current.setParentFragmentHint(parentHint);
        pendingSupportRequestManagerFragments.put(fm, current);
        fm.beginTransaction().add(current, FRAGMENT_TAG).commitAllowingStateLoss();
        //4.发送一个ID_REMOVE_SUPPORT_FRAGMENT_MANAGER消息，移除pendingSupportRequestManagerFragments中的fragment。
        handler.obtainMessage(ID_REMOVE_SUPPORT_FRAGMENT_MANAGER, fm).sendToTarget();
      }
    }
    return current;
}
```

1. 通过tag从FragmentManager中获取Fragment。
2. 如果从fm获取失败，从HashMap中获取。
3. 如果从HashMap中获取失败，创建一个新的，并添加到map和提交到fm中。fm提交fragment也是发送消息到主线程，在主线程执行。
4. 发送一个ID_REMOVE_SUPPORT_FRAGMENT_MANAGER消息，移除pendingSupportRequestManagerFragments中的fragment。

> 通过Handler发送消息到消息队列，由于消息队列的消息有先后顺序，所以fm提交fragment之后才会执行ID_REMOVE_SUPPORT_FRAGMENT_MANAGER消息。

###### Fragment

```java
final class BackStackRecord extends FragmentTransaction implements
        FragmentManager.BackStackEntry, FragmentManagerImpl.OpGenerator {
 @Override
  public int commit() {
    	return commitInternal(false);
  }
  int commitInternal(boolean allowStateLoss) {
      //...
      mManager.enqueueAction(this, allowStateLoss);
      return mIndex;
  }  
  
}

final class FragmentManagerImpl extends FragmentManager implements LayoutInflater.Factory2 {
  public void enqueueAction(OpGenerator action, boolean allowStateLoss) {
    //...
    synchronized (this) {
      //...
      mPendingActions.add(action);
      scheduleCommit();
    }
  }
  void scheduleCommit() {
    synchronized (this) {
      boolean postponeReady =
        mPostponedTransactions != null && !mPostponedTransactions.isEmpty();
      boolean pendingReady = mPendingActions != null && mPendingActions.size() == 1;
      if (postponeReady || pendingReady) {
        mHost.getHandler().removeCallbacks(mExecCommit);
        mHost.getHandler().post(mExecCommit);
        updateOnBackPressedCallbackEnabled();
      }
    }
  }
}
```

> scheduleCommit通过发送mExecCommit(Runnable)到Handler执行fragment添加。

#### <span id="handlerThread">HandlerThread</span>

```java
public class HandlerThread extends Thread {
    @Override
    public void run() {
        mTid = Process.myTid();
        Looper.prepare();
        synchronized (this) {
            mLooper = Looper.myLooper();
            notifyAll();
        }
        Process.setThreadPriority(mPriority);
        onLooperPrepared();
        Looper.loop();
        mTid = -1;
    }
}
HandlerThread handlerThread = new HandlerThread("handler-thread");
Looper looper = handlerThread.getLooper();
Log.i("--handlerThread","start before:"+ (looper==null?"null":looper.toString()));
handlerThread.start();
looper = handlerThread.getLooper();
Log.i("--handlerThread","start after:"+ (looper==null?"null":looper.toString()));
```

```java
--handlerThread: start before:null
--handlerThread: start after:Looper (handler-thread, tid 616) {33180fc}  
```

> HandlerThread主要封装了Looper的操作，需要start线程后才能获取Looper。

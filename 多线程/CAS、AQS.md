# CAS 与 AQS

---

## 一、CAS（Compare And Swap）

### 1.1 是什么

CAS 是一条 **CPU 原子指令**（x86 下对应 `cmpxchg`），含义：

> 比较内存中的值是否等于预期值，如果相等则更新为新值，否则不做任何操作。

整个过程由硬件保证原子性，不需要加锁。

```java
// 伪代码语义
boolean CAS(内存地址, 预期值, 新值) {
    if (内存当前值 == 预期值) {
        内存当前值 = 新值;
        return true;
    }
    return false;
}
```

### 1.2 Java 中的 CAS —— Unsafe + 原子类

Java 通过 `sun.misc.Unsafe` 暴露 CAS 操作，`java.util.concurrent.atomic` 包下的原子类都基于此实现。

```java
// AtomicInteger 自增的底层实现
public final int getAndIncrement() {
    return unsafe.getAndAddInt(this, valueOffset, 1);
}

// Unsafe.getAndAddInt 内部就是自旋 CAS
public final int getAndAddInt(Object o, long offset, int delta) {
    int v;
    do {
        v = getIntVolatile(o, offset);          // 读当前值
    } while (!compareAndSwapInt(o, offset, v, v + delta)); // CAS 失败则重试
    return v;
}
```

### 1.3 常用原子类

| 类 | 说明 |
|----|------|
| AtomicInteger / AtomicLong | 整数原子操作 |
| AtomicReference\<V\> | 对象引用原子操作 |
| AtomicStampedReference\<V\> | 带版本号，解决 ABA 问题 |
| LongAdder | 高并发计数，分段累加，比 AtomicLong 吞吐更高 |

### 1.4 CAS 的三个问题

#### ABA 问题
值从 A → B → A，CAS 看到的还是 A，认为没有变化，但实际上被改过。

**解决**：用 `AtomicStampedReference`，每次修改同时更新版本号（stamp）。

```java
AtomicStampedReference<Integer> ref = new AtomicStampedReference<>(100, 0);
int[] stampHolder = new int[1];
Integer value = ref.get(stampHolder);         // 同时获取值和版本号
ref.compareAndSet(value, 200, stampHolder[0], stampHolder[0] + 1); // CAS 时校验版本号
```

#### 自旋开销问题
CAS 失败会不断循环重试（自旋），在竞争激烈时大量消耗 CPU。

**解决**：
- 控制自旋次数，超过阈值升级为阻塞（synchronized 的锁升级就是这个思路）
- 使用 `LongAdder` 分段减少竞争

#### 只能保证单个变量的原子性
无法同时原子地修改多个变量。

**解决**：把多个变量封装成对象，用 `AtomicReference` 操作整个对象。

---

## 二、AQS（AbstractQueuedSynchronizer）

### 2.1 是什么

AQS 是 JUC 中大多数同步工具的**基础框架**，`ReentrantLock`、`CountDownLatch`、`Semaphore`、`ReentrantReadWriteLock` 都基于它实现。

核心思想：**用一个 int 型 state 变量表示同步状态，用 CLH 变体队列管理等待线程。**

### 2.2 核心数据结构

```
AQS
 ├── volatile int state          // 同步状态（0=未加锁，>0=已加锁/重入次数）
 ├── Node head                   // 等待队列头节点
 └── Node tail                   // 等待队列尾节点

Node（双向链表节点）
 ├── Thread thread               // 等待的线程
 ├── int waitStatus              // 节点状态（CANCELLED/SIGNAL/CONDITION/PROPAGATE）
 ├── Node prev / next            // 前驱/后继
 └── Node nextWaiter             // Condition 队列中的下一个节点
```

### 2.3 加锁流程（以 ReentrantLock 非公平锁为例）

```
lock()
 │
 ├─ CAS(state: 0 → 1) 成功  ──→ 设置当前线程为独占线程，加锁成功
 │
 └─ CAS 失败（锁被占用）
       │
       ├─ 是否是当前线程持有（重入）？──→ state++，加锁成功
       │
       └─ 其他线程持有
             │
             └─ 构建 Node 加入 CLH 队列尾部
                   │
                   └─ 自旋尝试获取锁（前驱是 head 才有资格）
                         │
                         ├─ 获取成功 ──→ 将自己设为 head，出队
                         └─ 获取失败 ──→ park 挂起线程，等待 unpark 唤醒
```

### 2.4 解锁流程

```
unlock()
 │
 └─ state--（重入次数-1）
       │
       └─ state == 0（完全释放）
             │
             └─ 唤醒队列中 head 的后继节点（unpark）
                   │
                   └─ 后继节点线程被唤醒，重新竞争锁
```

### 2.5 公平锁 vs 非公平锁

| | 非公平锁（默认） | 公平锁 |
|--|--|--|
| 新线程来了 | 直接 CAS 抢锁，不排队 | 先检查队列是否有等待者，有则排队 |
| 吞吐量 | 更高（减少线程切换） | 较低 |
| 饥饿问题 | 可能（队列里的线程可能一直抢不到） | 不会 |

```java
new ReentrantLock();       // 非公平锁
new ReentrantLock(true);   // 公平锁
```

### 2.6 Condition（条件变量）

每个 `ReentrantLock` 可以创建多个 `Condition`，对应 synchronized 的 `wait/notify`，但更灵活（可以有多个等待队列）。

```java
ReentrantLock lock = new ReentrantLock();
Condition notFull  = lock.newCondition();
Condition notEmpty = lock.newCondition();

// 生产者
lock.lock();
try {
    while (queue.isFull()) notFull.await();   // 挂起，释放锁
    queue.add(item);
    notEmpty.signal();                         // 唤醒消费者
} finally {
    lock.unlock();
}
```

Condition.await() 会将线程移入 **Condition 等待队列**，释放锁；
signal() 将节点从 Condition 队列转移到 **AQS 同步队列**，等待重新获取锁。

### 2.7 基于 AQS 的工具类

| 工具类 | state 含义 | 用途 |
|--------|-----------|------|
| ReentrantLock | 0=未锁，>0=重入次数 | 可重入互斥锁 |
| CountDownLatch | 倒计数，到0时唤醒所有等待线程 | 等待多个线程完成 |
| Semaphore | 许可证数量 | 限流，控制并发数 |
| ReentrantReadWriteLock | 高16位=读锁数，低16位=写锁数 | 读多写少场景 |

---

## 三、CAS vs synchronized vs AQS

| | CAS（乐观锁） | synchronized | AQS（ReentrantLock）|
|--|--|--|--|
| 实现层次 | CPU 指令 | JVM 内置 | Java 层（基于 CAS）|
| 竞争激烈时 | 自旋浪费 CPU | 阻塞挂起 | 阻塞挂起（park）|
| 可重入 | 不支持 | 支持 | 支持 |
| 公平性 | 不支持 | 不支持 | 支持（可选）|
| 适用场景 | 低竞争、简单变量 | 通用 | 需要高级特性（公平/多条件/可中断）|

---

## 四、面试高频问题

**Q：CAS 是什么？有哪些问题？**

CAS 是 CPU 级别的原子比较并交换指令，Java 通过 Unsafe 暴露给原子类使用。
三个问题：① ABA 问题（用 AtomicStampedReference 解决）；② 自旋开销（竞争激烈时 CPU 消耗大）；③ 只能操作单个变量。

**Q：AQS 的核心原理是什么？**

AQS 用一个 volatile int state 表示同步状态，用 CLH 双向队列管理等待线程。
加锁：CAS 修改 state，失败则入队并 park 挂起；解锁：修改 state，unpark 唤醒队头后继节点。
子类只需实现 `tryAcquire / tryRelease` 定义 state 的语义，AQS 负责队列管理。

**Q：ReentrantLock 和 synchronized 的区别？**

| 对比点 | synchronized | ReentrantLock |
|--------|-------------|---------------|
| 实现 | JVM 关键字 | Java 类（基于 AQS）|
| 公平锁 | 不支持 | 支持 |
| 可中断 | 不支持 | 支持（lockInterruptibly）|
| 多条件 | 只有一个 wait/notify | 多个 Condition |
| 自动释放 | 是（代码块结束） | 否（必须手动 unlock）|

**Q：CountDownLatch 和 CyclicBarrier 的区别？**

- CountDownLatch：一个或多个线程**等待**其他线程完成，计数归零后等待线程继续执行，不可重用
- CyclicBarrier：一组线程互相**等待**，全部到达屏障点后一起继续，可重复使用（cyclic）

**Q：ReentrantReadWriteLock 适合什么场景？state 如何设计？**

适合**读多写少**场景，允许多个线程同时读，但写操作独占。
state 高 16 位记录读锁持有数，低 16 位记录写锁重入次数，通过位运算分别读写两个计数。

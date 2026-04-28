# ThreadLocal

## 1. 是什么

ThreadLocal 是 Java 提供的**线程局部变量**机制，让每个线程拥有变量的独立副本，线程之间互不干扰。

典型场景：Web 请求中传递当前登录用户，无需通过方法参数层层传递。

```java
// UserContext.java —— 典型用法
private static ThreadLocal<String> currentUser = new ThreadLocal<>();

public static void set(String user) { currentUser.set(user); }
public static String get()          { return currentUser.get(); }
public static void remove()         { currentUser.remove(); }
```

```java
// ThreadLocalTest.java —— 验证线程隔离
Thread t1 = new Thread(() -> {
    UserContext.set("张三");
    Thread.sleep(1000);
    System.out.println("线程A 读到：" + UserContext.get()); // 张三，不受 t2 影响
    UserContext.remove();
});

Thread t2 = new Thread(() -> {
    UserContext.set("李四");
    System.out.println("线程B 读到：" + UserContext.get()); // 李四
    UserContext.remove();
});
```

---

## 2. 底层数据结构

```
Thread
 └── ThreadLocal.ThreadLocalMap  threadLocals
          └── Entry[]  table
                └── Entry extends WeakReference<ThreadLocal<?>>
                          key   → ThreadLocal 实例（弱引用）
                          value → 存入的实际值（强引用）
```

- 每个 **Thread** 对象内部持有一个 `ThreadLocalMap`
- Map 的 **key** 是 ThreadLocal 实例本身（弱引用）
- Map 的 **value** 是 `set()` 进去的值（强引用）
- `get() / set() / remove()` 本质上都是操作当前线程的 `ThreadLocalMap`

---

## 3. 内存泄漏原因

### 3.1 弱引用 key 被回收后，value 依然存活

```
GC 后：
  Entry.key  → null（ThreadLocal 实例被回收）
  Entry.value → BigObject 实例（1MB，仍被强引用，无法回收）
```

### 3.2 线程池场景下问题被放大

线程池的线程**长期存活、反复复用**，其 `ThreadLocalMap` 也长期存在。
如果每次任务都创建新 ThreadLocal 且不 remove，Entry 会不断堆积。

```java
// ThreadLocalLeakTest.java —— 复现泄漏
ExecutorService pool = Executors.newFixedThreadPool(1); // 线程被复用

for (int i = 0; i < 200; i++) {
    pool.execute(() -> {
        ThreadLocal<BigObject> t1 = new ThreadLocal<>();
        t1.set(new BigObject("Task" + taskId));  // 每次 1MB
        leakList.add(t1);
        // 故意不 remove —— ThreadLocalMap 中的 value 无法释放
    });
}
```

上面代码中线程只有 1 个，执行 200 次任务后，其 `ThreadLocalMap` 里积累了 200 个 value（共约 200MB），全部无法 GC。

### 3.3 为什么 key 用弱引用

弱引用是一种"缓解"而非"解决"：
- 当外部没有强引用指向 ThreadLocal 时，key 在下次 GC 时会被置为 null
- 但 value 仍是强引用，依然泄漏
- ThreadLocalMap 在 `get() / set()` 时会顺带清理 key == null 的 Entry（启发式清理），但**不能依赖这个机制**

---

## 4. 正确使用姿势

### 4.1 必须在 finally 中 remove

```java
try {
    UserContext.set(currentUser);
    // 业务逻辑
} finally {
    UserContext.remove(); // 无论是否异常都要清理
}
```

### 4.2 线程池中尤其重要

线程池线程复用，不 remove 会导致下一个任务读到上一个任务的残留数据（数据污染），同时造成内存泄漏。

### 4.3 推荐使用 InheritableThreadLocal（父子线程传值）

```java
// 子线程可以继承父线程的 ThreadLocal 值
private static InheritableThreadLocal<String> ctx = new InheritableThreadLocal<>();
```

注意：线程池中子线程是复用的，InheritableThreadLocal 只在线程创建时继承一次，不适合线程池场景（可用 TransmittableThreadLocal 替代）。

---

## 5. 应用场景

| 场景 | 说明 |
|------|------|
| 用户上下文传递 | Web 请求进来后存入当前用户，业务层直接 get，无需传参 |
| 数据库连接/Session | 同一线程内复用同一个连接，保证事务一致性 |
| 日期格式化 | SimpleDateFormat 非线程安全，每个线程独立一份 |
| 链路追踪 traceId | 全链路日志中传递 traceId |

---

## 6. 面试高频问题

**Q：ThreadLocal 是怎么实现线程隔离的？**

每个 Thread 内部有一个 ThreadLocalMap，key 是 ThreadLocal 实例，value 是存入的值。不同线程有各自独立的 Map，所以互不影响。

**Q：ThreadLocal 为什么会内存泄漏？**

ThreadLocalMap 的 Entry 中，key（ThreadLocal）是弱引用，GC 后变为 null；但 value 是强引用，不会被回收。在线程池场景下线程长期存活，导致 value 一直堆积无法释放。

**Q：弱引用 key 的设计是为了解决内存泄漏吗？**

不是完全解决，只是缓解。key 变 null 后，ThreadLocalMap 在下次 get/set 时会做启发式清理，但无法保证及时清理。根本解决方案是手动调用 `remove()`。

**Q：线程池中使用 ThreadLocal 有什么风险？**

1. 内存泄漏：线程不销毁，ThreadLocalMap 一直存在，value 堆积
2. 数据污染：线程复用时，上一个任务的数据没清理，被下一个任务读到

**Q：父子线程如何共享 ThreadLocal？**

使用 `InheritableThreadLocal`，子线程创建时会拷贝父线程的值。但线程池场景不适用（线程是复用的，不是每次新建），需要使用阿里的 `TransmittableThreadLocal`。

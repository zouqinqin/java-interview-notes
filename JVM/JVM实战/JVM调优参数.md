# JVM 调优参数

## 一、参数类型速览

| 类型 | 格式 | 说明 |
|------|------|------|
| 标准参数 | `-Xms`、`-Xmx` | 所有 JVM 都支持，稳定 |
| 非标准参数 | `-XX:+选项` / `-XX:key=value` | HotSpot 专用，可能随版本变化 |
| 布尔开关 | `-XX:+GCDetails`（开）/ `-XX:-GCDetails`（关） | `+` 开启，`-` 关闭 |

---

## 二、堆内存参数（最高频）

```bash
-Xms512m              # 堆初始大小（建议与 -Xmx 相同，避免动态扩容）
-Xmx2g                # 堆最大大小
-Xmn512m              # 新生代大小（Young Gen）
-XX:NewRatio=2        # 老年代:新生代 = 2:1（默认）
-XX:SurvivorRatio=8   # Eden:Survivor = 8:1:1（默认）
```

> 💡 **面试常问**：为什么 `-Xms` 和 `-Xmx` 要设置成一样？
> → 避免堆动态扩容/缩容带来的 Full GC，减少性能抖动。

---

## 三、栈 & 元空间参数

```bash
-Xss256k              # 每个线程的栈大小（默认 512k~1m）
                      # 线程多时可适当调小，节省内存

-XX:MetaspaceSize=256m      # 元空间初始大小（触发 GC 的阈值）
-XX:MaxMetaspaceSize=512m   # 元空间最大大小（不设则无限制，可能 OOM）
```

> 💡 **面试常问**：元空间和永久代的区别？
> → 永久代在堆内（JDK7及以前），元空间在本地内存（JDK8+），不受堆大小限制，但仍可能 OOM。

---

## 四、垃圾回收器选择

```bash
# JDK8 常用
-XX:+UseSerialGC          # Serial + Serial Old（单线程，适合小内存）
-XX:+UseParallelGC        # Parallel Scavenge + Parallel Old（吞吐量优先，JDK8默认）
-XX:+UseConcMarkSweepGC   # ParNew + CMS（低延迟，已废弃）
-XX:+UseG1GC              # G1（JDK9+ 默认，均衡吞吐和延迟）

# JDK11+
-XX:+UseZGC               # ZGC（超低延迟，停顿 < 10ms）
-XX:+UseShenandoahGC      # Shenandoah（RedHat，低延迟）
```

---

## 五、GC 调优参数

```bash
# 停顿时间目标（G1 专用）
-XX:MaxGCPauseMillis=200      # 期望最大 GC 停顿时间（默认 200ms）
-XX:G1HeapRegionSize=4m       # G1 Region 大小（1~32m，2的幂次）

# 晋升老年代阈值
-XX:MaxTenuringThreshold=15   # 对象经历多少次 Minor GC 后晋升老年代（默认15）
-XX:PretenureSizeThreshold=1m # 大于此值的对象直接进老年代

# 并发线程数
-XX:ParallelGCThreads=4       # STW 阶段并行 GC 线程数
-XX:ConcGCThreads=2           # 并发 GC 线程数（不 STW）
```

---

## 六、OOM 诊断参数（生产必备）

```bash
-XX:+HeapDumpOnOutOfMemoryError          # OOM 时自动 dump 堆快照
-XX:HeapDumpPath=./heapdump.hprof        # dump 文件路径

-XX:+PrintGCDetails                      # 打印 GC 详情
-XX:+PrintGCDateStamps                   # 打印 GC 时间戳
-Xloggc:./gc.log                         # GC 日志输出到文件

-XX:+PrintGCApplicationStoppedTime      # 打印 STW 停顿时间
```

---

## 七、JIT 编译参数

```bash
-XX:+TieredCompilation          # 分层编译（JDK8+ 默认开启）
-XX:CompileThreshold=10000      # 方法调用多少次后触发 JIT 编译（默认10000）
-XX:+PrintCompilation           # 打印 JIT 编译信息（调试用）
-Xint                           # 关闭 JIT，纯解释执行（性能极差，仅测试用）
-Xcomp                          # 强制全部 JIT 编译（启动慢）
```

---

## 八、面试高频考点

### 1. 如何设置合理的堆大小？
- 一般设置为物理内存的 **1/4 ~ 1/2**
- `-Xms` = `-Xmx`，避免动态扩容
- 新生代占堆的 **1/3 ~ 1/4**，对象朝生夕死的场景可适当调大

### 2. 频繁 Full GC 怎么排查？
```
原因                    解决方案
─────────────────────────────────────────────
老年代空间不足          → 增大 -Xmx，或排查内存泄漏
元空间不足              → 设置 -XX:MaxMetaspaceSize
大对象直接进老年代      → 调整 -XX:PretenureSizeThreshold
System.gc() 被调用      → -XX:+DisableExplicitGC 禁用
```

### 3. Minor GC 和 Full GC 的区别？

| | Minor GC | Full GC |
|--|---------|---------|
| 回收区域 | 新生代（Eden + Survivor） | 整个堆 + 元空间 |
| 触发条件 | Eden 区满 | 老年代满、元空间满、System.gc() |
| 停顿时间 | 短（毫秒级） | 长（可能秒级） |
| 频率 | 频繁 | 尽量避免 |

### 4. G1 为什么比 CMS 好？

| 对比点 | CMS | G1 |
|--------|-----|----|
| 内存碎片 | 有（标记-清除） | 无（整理） |
| 停顿可预测 | 不可控 | 可设置目标（MaxGCPauseMillis） |
| 大堆表现 | 差 | 好（Region 化管理） |
| 并发失败 | Concurrent Mode Failure | 可降级处理 |

### 5. 线上 OOM 如何快速定位？
```
1. 查看 GC 日志 → 确认是堆 OOM 还是元空间 OOM
2. jmap -heap <pid> → 查看堆使用情况
3. jmap -dump:format=b,file=heap.hprof <pid> → 手动 dump
4. MAT 打开 hprof → Dominator Tree 按 Retained Heap 排序
5. 找到最大对象 → 追溯引用链 → 定位代码
```

---

## 九、常用调优模板

### 场景一：高吞吐量（批处理、计算密集）
```bash
-Xms4g -Xmx4g -Xmn2g
-XX:+UseParallelGC
-XX:ParallelGCThreads=8
```

### 场景二：低延迟（Web 服务、API 接口）
```bash
-Xms4g -Xmx4g
-XX:+UseG1GC
-XX:MaxGCPauseMillis=100
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/logs/heapdump.hprof
-Xloggc:/logs/gc.log
-XX:+PrintGCDetails
-XX:+PrintGCDateStamps
```

### 场景三：容器环境（Docker/K8s）
```bash
-XX:+UseContainerSupport          # 自动识别容器内存限制（JDK8u191+）
-XX:MaxRAMPercentage=75.0         # 使用容器内存的 75%
-XX:+UseG1GC
-XX:+HeapDumpOnOutOfMemoryError
```

---

## 十、参数速查表

| 参数 | 作用 | 推荐值 |
|------|------|--------|
| `-Xms` / `-Xmx` | 堆初始/最大 | 设为相同值 |
| `-Xmn` | 新生代大小 | 堆的 1/3 |
| `-Xss` | 线程栈大小 | 256k~512k |
| `-XX:MetaspaceSize` | 元空间初始 | 256m |
| `-XX:MaxMetaspaceSize` | 元空间上限 | 512m |
| `-XX:MaxGCPauseMillis` | G1 停顿目标 | 100~200ms |
| `-XX:+HeapDumpOnOutOfMemoryError` | OOM 自动 dump | 生产必开 |
| `-XX:+UseG1GC` | 使用 G1 | JDK8 推荐 |

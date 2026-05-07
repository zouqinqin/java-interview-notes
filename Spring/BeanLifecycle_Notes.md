# Bean 生命周期学习笔记

## 完整顺序

```
1. 构造方法
2. 依赖注入（@Autowired 字段赋值）
3. Aware 接口回调（setBeanName / setBeanFactory / setApplicationContext）
4. @PostConstruct
5. InitializingBean.afterPropertiesSet
6. initMethod（@Bean 配置的）
--- Bean 可以正常使用了 ---
7. @PreDestroy
8. DisposableBean.destroy
9. destroyMethod（@Bean 配置的）
```

---

## 关键结论

### 1. 构造方法里不能用注入的字段

```java
@Autowired
private ConnectionPool pool;

public DataService() {
    System.out.println(pool); // 输出 null！
}
```

`@Autowired` 是字段注入，Spring 先调用构造方法创建对象，再通过反射给字段赋值。
构造方法执行时，`pool` 还没被赋值，是 `null`。

**结论：** 依赖注入的字段，只能在 `@PostConstruct` 之后才能安全使用。

---

### 2. @PostConstruct 存在的意义

```java
@PostConstruct
public void postConstruct() {
    // 这里 pool 已经被注入了，可以安全使用
    System.out.println(pool.isReady());
}
```

执行时机：**构造方法完成 + 依赖注入完成 之后**。

需要依赖注入字段做初始化的逻辑，放这里。

---

### 3. 三种初始化方式对比

| 方式 | 适用场景 | 是否侵入 Spring |
|------|----------|----------------|
| `@PostConstruct` | 业务组件，简单方便 | 是（需要 import javax.annotation） |
| `InitializingBean.afterPropertiesSet` | 需要强依赖 Spring 的场景 | 是（implements Spring 接口） |
| `@Bean(initMethod="xxx")` | 框架/中间件，保持 POJO | 否 |

**记忆口诀：注解 → 接口 → XML配置，执行顺序从前到后。**

框架/中间件选 `initMethod` 的核心原因：让类保持 POJO，不依赖 Spring，可以在任何环境使用。

---

### 4. 销毁方法的触发时机

- 显式调用 `ctx.close()` → 触发
- Web 应用（Tomcat）正常关闭 → Tomcat 帮你调，会触发
- `kill -9` 强制杀进程 / 断电 → JVM 来不及执行 shutdown hook，**不会触发**

---

### 5. 面试核心

光背顺序不够，要知道每一步 Bean 的状态。

考察核心：**知道什么时候可以安全使用 Bean**，避免 Bean 还没初始化完成就使用，导致 NPE 等问题。

**进阶考点：** 循环依赖（A 依赖 B，B 依赖 A）——Spring 用三级缓存解决，在实例化之后、属性注入之前，提前暴露"半成品" Bean。

---

## 参考代码

`src/spring/ioc/Step4_BeanLifecycle.java`

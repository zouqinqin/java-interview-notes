# Spring Bean 生命周期

## 生命周期全景图

```
┌─────────────────────────────────────────────────────────────────┐
│                    ApplicationContext.refresh()                  │
└──────────────────────────┬──────────────────────────────────────┘
                           │
          ┌────────────────▼────────────────┐
          │   1. 加载 BeanDefinition         │
          │   @Component扫描 / @Bean / XML   │
          │   → BeanDefinitionRegistry       │
          └────────────────┬────────────────┘
                           │
          ┌────────────────▼────────────────┐
          │  2. BeanFactoryPostProcessor     │
          │   修改 BeanDefinition（此时Bean  │
          │   还没创建，只是元数据）          │
          │   e.g. PropertySourcesPlaceholder│
          └────────────────┬────────────────┘
                           │
          ┌────────────────▼────────────────┐
          │  3. 实例化 Instantiation         │◄── doCreateBean()
          │   反射调用构造方法               │
          │   Constructor.newInstance()      │
          └────────────────┬────────────────┘
                           │
          ┌────────────────▼────────────────┐
          │  4. 依赖注入 Populate Properties │◄── populateBean()
          │   @Autowired / @Value / @Resource│
          │   字段注入 / setter注入          │
          └────────────────┬────────────────┘
                           │
          ┌────────────────▼────────────────┐
          │  5. Aware 接口回调               │◄── initializeBean()
          │   BeanNameAware                  │    第一步
          │   BeanFactoryAware               │
          │   ApplicationContextAware        │
          └────────────────┬────────────────┘
                           │
          ┌────────────────▼────────────────┐
          │  6. BeanPostProcessor            │
          │   postProcessBeforeInitialization│◄── AOP代理就在这里生成！
          └────────────────┬────────────────┘
                           │
          ┌────────────────▼────────────────┐
          │  7. 初始化 Initialization        │
          │   @PostConstruct                 │
          │   InitializingBean.              │
          │     afterPropertiesSet()         │
          │   @Bean(initMethod="xxx")        │
          └────────────────┬────────────────┘
                           │
          ┌────────────────▼────────────────┐
          │  8. BeanPostProcessor            │
          │   postProcessAfterInitialization │
          └────────────────┬────────────────┘
                           │
          ┌────────────────▼────────────────┐
          │      ✅ Bean 就绪，放入单例池    │
          │      singletonObjects (Map)      │
          └────────────────┬────────────────┘
                           │
                    ... 使用中 ...
                           │
          ┌────────────────▼────────────────┐
          │  9. 销毁 Destruction             │
          │   @PreDestroy                    │
          │   DisposableBean.destroy()       │
          │   @Bean(destroyMethod="xxx")     │
          └─────────────────────────────────┘
```

---

## 源码核心路径分析

### 入口：`AbstractApplicationContext.refresh()`

```java
// Spring 容器启动的总指挥，12个步骤
public void refresh() {
    invokeBeanFactoryPostProcessors(beanFactory); // 步骤2: 执行BFPP
    registerBeanPostProcessors(beanFactory);       // 注册BPP（先注册，后触发）
    finishBeanFactoryInitialization(beanFactory);  // 步骤3-8: 实例化所有单例Bean
}
```

### 核心：`AbstractAutowireCapableBeanFactory.doCreateBean()`

```java
protected Object doCreateBean(String beanName, RootBeanDefinition mbd, ...) {
    // 步骤3：反射实例化
    BeanWrapper instanceWrapper = createBeanInstance(beanName, mbd, args);

    // 步骤4：依赖注入
    populateBean(beanName, mbd, instanceWrapper);

    // 步骤5-8：初始化（Aware + BPP + init方法）
    Object exposedObject = initializeBean(beanName, exposedObject, mbd);

    return exposedObject; // 注意：返回的可能是代理对象，不是原始对象
}
```

### 重难点1：`initializeBean()` 内部执行顺序

```java
protected Object initializeBean(String beanName, Object bean, ...) {
    // 5. Aware 回调
    invokeAwareMethods(beanName, bean);

    // 6. BPP Before（@PostConstruct 在这里执行！）
    //    CommonAnnotationBeanPostProcessor 处理 @PostConstruct
    Object wrappedBean = applyBeanPostProcessorsBeforeInitialization(wrappedBean, beanName);

    // 7. afterPropertiesSet + initMethod
    invokeInitMethods(beanName, wrappedBean, mbd);

    // 8. BPP After（AOP 代理在这里生成！）
    wrappedBean = applyBeanPostProcessorsAfterInitialization(wrappedBean, beanName);

    return wrappedBean;
}
```

> **关键**：`@PostConstruct` 不是 Spring 的，是 JSR-250 规范。Spring 通过 `CommonAnnotationBeanPostProcessor`（它是一个 BPP）在 Before 阶段扫描并调用它。

### 重难点2：AOP 代理的生成时机

```java
// AbstractAutoProxyCreator（AOP核心BPP）
public Object postProcessAfterInitialization(Object bean, String beanName) {
    return wrapIfNecessary(bean, beanName, cacheKey);
}

protected Object wrapIfNecessary(Object bean, ...) {
    Object[] specificInterceptors = getAdvicesAndAdvisorsForBean(...);
    if (specificInterceptors != DO_NOT_PROXY) {
        // 有接口用 JDK 动态代理，无接口用 CGLIB
        Object proxy = createProxy(bean.getClass(), beanName, specificInterceptors, ...);
        return proxy; // 返回代理对象，原始 Bean 被包在里面
    }
    return bean;
}
```

### 重难点3：三级缓存解决循环依赖

```java
// DefaultSingletonBeanRegistry 中的三个 Map
Map<String, Object> singletonObjects;             // 一级：完整Bean
Map<String, Object> earlySingletonObjects;        // 二级：半成品Bean（已实例化未初始化）
Map<String, ObjectFactory<?>> singletonFactories; // 三级：Bean工厂（用于生成早期引用）

// A 依赖 B，B 依赖 A 的解决过程：
// 1. 创建A：实例化 → 放入三级缓存（singletonFactories）
// 2. 注入A的依赖，发现需要B → 开始创建B
// 3. 创建B：实例化 → 放入三级缓存
// 4. 注入B的依赖，发现需要A → 从三级缓存拿到A的早期引用 → 放入二级缓存
// 5. B 初始化完成 → 放入一级缓存
// 6. A 继续初始化，拿到完整的B → A 完成 → 放入一级缓存
```

> **为什么需要三级而不是两级？**
> 第三级存的是 `ObjectFactory`，不是 Bean 本身。这是为了支持 AOP：循环依赖中如果涉及代理，需要通过工厂提前生成代理对象，保证 A 和 B 拿到的是同一个代理引用。

---

## 三种注入方式对比

| 注入方式 | 写法 | @Autowired 时机 | 可否 mock 测试 |
|---------|------|----------------|--------------|
| 构造器注入 | `@Autowired` 在构造方法 | 构造时即注入 | 最佳 |
| Setter 注入 | `@Autowired` 在 setter | 构造后注入 | 可以 |
| 字段注入 | `@Autowired` 在字段 | 构造后注入 | 需反射 |

> **构造器注入无法解决循环依赖**：因为实例化时就需要依赖，无法先放入三级缓存，Spring 直接抛 `BeanCurrentlyInCreationException`。

---

## 初始化方法执行顺序

同一个 Bean 上如果同时有三种初始化方式，执行顺序固定：

```
@PostConstruct  →  afterPropertiesSet()  →  initMethod
```

销毁顺序：

```
@PreDestroy  →  destroy()  →  destroyMethod
```

---

## 10个面试题

### 基础

**Q1. Bean 生命周期有哪几个阶段？`@PostConstruct`、`afterPropertiesSet()`、`initMethod` 执行顺序？**

实例化 → 依赖注入 → Aware回调 → BPP Before → @PostConstruct → afterPropertiesSet → initMethod → BPP After → Bean就绪 → @PreDestroy → destroy → destroyMethod。初始化顺序：@PostConstruct 最先，initMethod 最后。

---

**Q2. `@PostConstruct` 和构造方法的区别？什么时候必须用 `@PostConstruct`？**

构造方法执行时 `@Autowired` 字段还未注入（为 null）。需要依赖注入完成后才能执行的初始化逻辑（如用注入的连接池做预热），必须放在 `@PostConstruct` 中。

---

**Q3. `BeanPostProcessor` 和 `BeanFactoryPostProcessor` 的区别？**

- `BeanFactoryPostProcessor`（BFPP）：在 Bean **实例化之前**执行，操作的是 BeanDefinition（元数据），常见实现：`PropertySourcesPlaceholderConfigurer`。
- `BeanPostProcessor`（BPP）：在 Bean **实例化之后**执行，操作的是 Bean 对象本身，常见实现：AOP代理生成、@PostConstruct处理。

---

### 进阶

**Q4. Spring AOP 代理在 Bean 生命周期的哪个阶段生成？**

在 `BeanPostProcessor.postProcessAfterInitialization()` 阶段，由 `AbstractAutoProxyCreator` 判断 Bean 是否匹配切面，如果匹配则包一层代理返回。

---

**Q5. 从 Spring 容器 `getBean()` 拿到的一定是原始对象吗？**

不一定。如果 Bean 被 AOP 切面匹配，返回的是代理对象（JDK动态代理或CGLIB子类），原始 Bean 被封装在代理内部。

---

**Q6. Spring 如何解决循环依赖？为什么需要三级缓存而不是两级？**

三级缓存机制：
- 三级存 `ObjectFactory`（Bean工厂）
- 二级存早期引用（半成品 Bean）
- 一级存完整 Bean

需要三级而不是两级，是因为要支持 AOP：通过 `ObjectFactory` 提前生成代理对象，保证循环依赖中各方拿到的是同一个代理引用，而不是原始对象。

---

**Q7. 构造器注入的 Bean 能解决循环依赖吗？为什么？**

不能。三级缓存的前提是先调用无参构造实例化，然后放入缓存。构造器注入时依赖还未创建，Spring 无法实例化，直接抛 `BeanCurrentlyInCreationException`。

---

### 高阶

**Q8. `@Lazy` 注解如何打破循环依赖？原理是什么？**

`@Lazy` 注入的不是真实 Bean，而是一个 CGLIB 代理占位符。真正调用方法时才触发实际 Bean 的创建，打破了"我必须先有你"的死锁依赖链。

---

**Q9. `ApplicationContextAware` 和直接 `@Autowired ApplicationContext` 有什么区别？**

功能等价。`@Autowired ApplicationContext` 更简洁，推荐使用。`ApplicationContextAware` 是老式写法，主要用于不能使用注解的场景（如工具类静态方法、框架底层代码）。

---

**Q10. 如果一个 Bean 的 `@PostConstruct` 方法抛异常，会发生什么？**

Bean 创建失败，Spring 容器启动时抛出异常，该 Bean 不会进入单例池。如果其他 Bean 依赖它，整个容器无法启动。生产中要避免在 `@PostConstruct` 里做可能失败的远程调用。

---

## 实践代码

参考：`src/spring/ioc/` 下的四个 Step 文件

| 文件 | 内容 |
|------|------|
| `Step1_NoDI.java` | 没有 IOC 时的痛点：重复创建、紧耦合 |
| `Step2_ManualDI.java` | 手动依赖注入：构造器注入解决问题 |
| `Step3_SpringIOC.java` | Spring IOC 容器接管，三种注入方式对比 |
| `Step4_BeanLifecycle.java` | Bean 生命周期各阶段回调的执行顺序演示 |

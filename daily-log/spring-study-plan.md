# Spring 源码学习路线（面试导向版）

> 目标：1个月内掌握 Spring 核心原理，冲击用友/金蝶等企业级岗位
> 使用方式：每次学习前把本文件发给 AI，告知当前进度，AI 会接着上次继续带你学习
> 源码版本：Spring 5.3.x（对应 Spring Boot 2.x）
> 本地源码路径：D:\project\spring-framework

---

## 当前进度

<!-- 每次学完一个小节，在括号里填写完成日期，例如：(✅ 2024-01-15) -->

- [x] 环境搭建：clone 源码、预编译、IDEA 导入 (✅ 已完成)
- [x] 第一个断点：doCreateBean 调用栈跑通 (✅ 已完成)
- [ ] 阶段一：IoC 容器核心
- [ ] 阶段二：AOP 原理
- [ ] 阶段三：事务原理
- [ ] 阶段四：Spring MVC
- [ ] 阶段五：Spring Boot 自动装配
- [ ] 阶段六：面试题整理与模拟

---

## 阶段一：IoC 容器核心（预计 2 天）

### Day 1 — Bean 生命周期主干

**目标**：能手画生命周期图，能说出每个阶段对应的源码方法

**调试路径**：
1. 断点打在 `AbstractAutowireCapableBeanFactory.doCreateBean()`
2. 跟进 `createBeanInstance()` — 看构造器推断
3. 跟进 `populateBean()` — 看属性注入入口
4. 跟进 `initializeBean()` — 看 Aware、BPP before、init、BPP after 四个子步骤

**核心方法清单**：
```
AbstractAutowireCapableBeanFactory
  ├── doCreateBean()              // 生命周期主干
  ├── createBeanInstance()        // 实例化
  ├── populateBean()              // 属性注入
  └── initializeBean()           // 初始化四步
        ├── invokeAwareMethods()
        ├── applyBPPBeforeInitialization()
        ├── invokeInitMethods()
        └── applyBPPAfterInitialization()
```

**完成标准**：能在纸上默写调用链，不看代码能说清楚每步做了什么

**学习笔记**：
<!-- 在这里记录你的理解和疑问 -->

---

### Day 2 — 三级缓存与循环依赖

**目标**：彻底搞懂三级缓存，能解释为什么需要三级而不是两级

**调试路径**：
1. 写一个 A 依赖 B、B 依赖 A 的循环依赖测试用例
2. 断点打在 `DefaultSingletonBeanRegistry.getSingleton()`
3. 观察 singletonObjects / earlySingletonObjects / singletonFactories 三个 Map 的变化

**核心方法清单**：
```
DefaultSingletonBeanRegistry
  ├── getSingleton()              // 三级缓存查找逻辑
  ├── addSingletonFactory()       // 注册三级缓存（doCreateBean 中调用）
  └── addSingleton()             // 放入一级缓存（Bean 创建完成后）

AbstractAutowireCapableBeanFactory
  └── getEarlyBeanReference()    // 三级缓存 lambda 触发，AOP 提前代理
```

**面试必背结论**：
- 一级缓存：完整 Bean（singletonObjects）
- 二级缓存：提前暴露的代理对象（earlySingletonObjects）
- 三级缓存：ObjectFactory lambda（singletonFactories）
- 构造器注入无法解决循环依赖，原因：实例化阶段三级缓存还未注册

**学习笔记**：
<!-- 在这里记录你的理解和疑问 -->

---

## 阶段二：AOP 原理（预计 1.5 天）

### Day 3 — 动态代理创建过程

**目标**：搞清楚 AOP 代理在生命周期哪个阶段创建，JDK vs CGLIB 如何选择

**调试路径**：
1. 写一个带 `@Transactional` 的 Service，断点打在 `AbstractAutoProxyCreator.postProcessAfterInitialization()`
2. 跟进 `wrapIfNecessary()` 看代理决策逻辑
3. 跟进 `createProxy()` 看 JDK/CGLIB 选择逻辑

**核心方法清单**：
```
AbstractAutoProxyCreator
  ├── postProcessAfterInitialization()  // BPP after 阶段触发
  ├── wrapIfNecessary()                 // 判断是否需要代理
  └── createProxy()                    // 创建代理对象

ProxyFactory / ProxyCreatorSupport
  └── getProxy()                       // 最终生成代理

JdkDynamicAopProxy.invoke()            // JDK 代理拦截入口
CglibAopProxy.intercept()             // CGLIB 代理拦截入口
```

**学习笔记**：
<!-- 在这里记录你的理解和疑问 -->

---

### Day 3 下午 — AOP 失效场景分析

**目标**：能说出 5 种以上 AOP/事务失效场景及原因

**失效场景清单**（结合 IMS 项目举例）：
- [ ] 同类内部自调用（最常见）
- [ ] 方法非 public
- [ ] 异常被 catch 吞掉
- [ ] 异常类型不匹配（默认只回滚 RuntimeException）
- [ ] 多线程中事务失效
- [ ] @Transactional 加在接口方法上（CGLIB 模式下失效）

**学习笔记**：
<!-- 在这里记录你的理解和疑问 -->

---

## 阶段三：事务原理（预计 1 天）

### Day 4 — 事务传播与 TransactionInterceptor

**目标**：搞清楚 7 种传播行为，能结合项目场景举例

**调试路径**：
1. 断点打在 `TransactionInterceptor.invoke()`
2. 跟进 `invokeWithinTransaction()`
3. 跟进 `createTransactionIfNecessary()` 看传播行为判断逻辑

**核心方法清单**：
```
TransactionInterceptor
  └── invoke()                         // 事务拦截入口（AOP advice）

TransactionAspectSupport
  ├── invokeWithinTransaction()        // 事务执行主干
  ├── createTransactionIfNecessary()   // 根据传播行为决定是否开启事务
  └── completeTransactionAfterThrowing() // 异常回滚判断
```

**7种传播行为速记**：
```
REQUIRED        默认，有则加入，无则新建
REQUIRES_NEW    始终新建，挂起当前事务（IMS 记录日志场景）
NESTED          嵌套事务，savepoint 回滚
SUPPORTS        有则加入，无则非事务执行
NOT_SUPPORTED   挂起当前事务，非事务执行
MANDATORY       必须在事务中，否则抛异常
NEVER           不能在事务中，否则抛异常
```

**学习笔记**：
<!-- 在这里记录你的理解和疑问 -->

---

## 阶段四：Spring MVC（预计 1 天）

### Day 5 — DispatcherServlet 请求处理流程

**目标**：能画出一次 HTTP 请求从进入到返回的完整链路

**调试路径**：
1. 断点打在 `DispatcherServlet.doDispatch()`
2. 跟进 `getHandler()` — HandlerMapping 查找
3. 跟进 `getHandlerAdapter()` — 适配器选择
4. 跟进 `handle()` — 执行 Controller 方法
5. 跟进 `processDispatchResult()` — 视图渲染/响应返回

**核心方法清单**：
```
DispatcherServlet
  └── doDispatch()
        ├── getHandler()              // 找到对应 Controller 方法
        ├── getHandlerAdapter()       // 适配器（处理参数绑定/返回值）
        ├── handle()                  // 实际执行
        └── processDispatchResult()  // 处理结果
```

**学习笔记**：
<!-- 在这里记录你的理解和疑问 -->

---

## 阶段五：Spring Boot 自动装配（预计 0.5 天）

### Day 6 上午 — @EnableAutoConfiguration 原理

**目标**：能解释 Spring Boot 启动时如何自动加载配置类

**核心链路**：
```
@SpringBootApplication
  └── @EnableAutoConfiguration
        └── @Import(AutoConfigurationImportSelector)
              └── selectImports()
                    └── 读取 META-INF/spring.factories
                          └── 过滤 @ConditionalOnXxx
                                └── 注册符合条件的配置类
```

**学习笔记**：
<!-- 在这里记录你的理解和疑问 -->

---

## 阶段六：面试题整理（持续更新）

### IoC / Bean 生命周期
- [ ] Bean 生命周期完整阶段（能手画）
- [ ] BeanFactory vs ApplicationContext 区别
- [ ] 三级缓存解决循环依赖原理
- [ ] BeanFactoryPostProcessor vs BeanPostProcessor 区别
- [ ] FactoryBean vs BeanFactory 区别
- [ ] @PostConstruct / afterPropertiesSet / init-method 执行顺序

### AOP
- [ ] JDK 动态代理 vs CGLIB 区别及选择逻辑
- [ ] AOP 代理在生命周期哪个阶段创建
- [ ] @Transactional 自调用失效原因及解决方案
- [ ] AOP 失效的所有场景

### 事务
- [ ] 7种传播行为及适用场景
- [ ] 事务失效的8种场景
- [ ] @Transactional 源码入口（TransactionInterceptor）

### Spring MVC
- [ ] DispatcherServlet 请求处理完整流程
- [ ] HandlerMapping vs HandlerAdapter 区别

### Spring Boot
- [ ] 自动装配原理（spring.factories / SPI 机制）
- [ ] @Conditional 系列注解原理

---

## 项目结合点（面试加分项）

> 结合 IMS 账期对账系统的实际场景，让回答更有说服力

- 循环依赖：IMS 中 Service 层互相注入的场景
- 事务传播：对账任务主事务 + 日志记录 REQUIRES_NEW 独立事务
- AOP 失效：同类内部调用核销方法导致事务失效的踩坑经历
- BeanPostProcessor：自定义动态数据源切换的 AOP 实现

---

*最后更新：环境搭建完成，doCreateBean 断点调试跑通*

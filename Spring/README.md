# Spring

## 核心知识点

- [x] IOC容器启动流程
- [x] Bean生命周期
- [x] DI依赖注入的方式
- [x] AOP原理（动态代理，JDK vs CGLIB）
- [x] Spring事务（传播机制/失效场景）
- [x] SpringMVC请求处理流程（DispatcherServlet)
- [x] Spring循环依赖怎么解决（三级缓存）
- [x] ApplicationContext vs BeanFactory区别

### 3. DI依赖注入的方式

Spring 中常见的依赖注入方式有三种：**构造器注入**、**Setter 注入**、**字段注入**。底层都由 `AutowiredAnnotationBeanPostProcessor` 等 BeanPostProcessor 处理。

#### 3.1 构造器注入（推荐）
通过构造方法传入依赖，Spring 在实例化 Bean 时完成注入。

```java
@Component
public class UserService {
    private final UserDao userDao;

    // Spring 4.3+ 单构造器可省略 @Autowired
    public UserService(UserDao userDao) {
        this.userDao = userDao;
    }
}
```

特点：
- 依赖可以声明为 `final`，保证不可变。
- 强依赖关系，缺少依赖时启动即失败，避免 NPE。
- 天然规避循环依赖（无法通过三级缓存解决，会直接报错暴露问题）。
- 单元测试友好，无需反射即可注入 mock。

#### 3.2 Setter 注入
通过 setter 方法注入，适合**可选依赖**或需要**重新配置**的场景。

```java
@Component
public class UserService {
    private UserDao userDao;

    @Autowired(required = false)
    public void setUserDao(UserDao userDao) {
        this.userDao = userDao;
    }
}
```

特点：
- 依赖可选，可以为 null。
- 可以解决 Setter 之间的循环依赖（三级缓存机制）。
- 对象在 setter 调用之前处于"半初始化"状态。

#### 3.3 字段注入（不推荐）
直接在字段上使用 `@Autowired` / `@Resource`，由反射赋值。

```java
@Component
public class UserService {
    @Autowired
    private UserDao userDao;
}
```

特点：
- 写法最简洁，但隐藏了依赖关系。
- 无法用 `final` 修饰，对象可变。
- 脱离 Spring 容器无法实例化，测试时必须借助反射或容器。
- 容易产生过多依赖而不自知（违反单一职责）。

#### 3.4 @Autowired vs @Resource vs @Inject

| 注解 | 来源 | 默认匹配方式 | 备注 |
| ---- | ---- | ------------ | ---- |
| `@Autowired` | Spring | byType | 可配合 `@Qualifier` 按名匹配；`required=false` 允许缺失 |
| `@Resource` | JSR-250（JDK） | byName，找不到再 byType | name/type 都可显式指定 |
| `@Inject` | JSR-330 | byType | 需引入 `javax.inject` 依赖 |

#### 3.5 注入处理流程（源码视角）
1. `AbstractAutowireCapableBeanFactory#createBeanInstance`：根据构造器解析进行**构造器注入**。
2. `populateBean`：处理属性填充，调用 `InstantiationAwareBeanPostProcessor#postProcessProperties`。
3. `AutowiredAnnotationBeanPostProcessor` 扫描 `@Autowired` 字段/方法，构建 `InjectionMetadata`，通过 `DefaultListableBeanFactory#resolveDependency` 解析依赖并反射赋值。
4. 完成 Setter 注入和字段注入。

---

### 4. AOP原理（动态代理，JDK vs CGLIB）

AOP（Aspect Oriented Programming，面向切面编程）通过**动态代理**在不修改原代码的前提下，对方法调用进行增强。Spring AOP 的核心实现就是**运行期生成代理对象**，将切面逻辑织入目标方法的调用链。

#### 4.1 核心概念
- **Aspect（切面）**：横切关注点的模块化封装，如日志、事务、权限。
- **JoinPoint（连接点）**：可以被拦截的程序执行点，Spring AOP 中仅支持**方法执行**。
- **Pointcut（切点）**：匹配连接点的表达式，如 `execution(* com.xxx.service..*.*(..))`。
- **Advice（通知）**：切面在切点处执行的动作，类型有 `@Before` / `@After` / `@AfterReturning` / `@AfterThrowing` / `@Around`。
- **Weaving（织入）**：将切面应用到目标对象的过程，Spring AOP 在**运行时**织入。
- **Target（目标对象）**：被代理的原始对象。
- **Proxy（代理对象）**：织入切面后生成的对象，调用方实际拿到的是它。

#### 4.2 JDK 动态代理
基于**接口**的代理，由 `java.lang.reflect.Proxy` 在运行时生成一个实现目标接口的代理类。

```java
public class JdkProxyDemo {
    public static void main(String[] args) {
        UserService target = new UserServiceImpl();
        UserService proxy = (UserService) Proxy.newProxyInstance(
            target.getClass().getClassLoader(),
            target.getClass().getInterfaces(),
            (p, method, methodArgs) -> {
                System.out.println("before " + method.getName());
                Object result = method.invoke(target, methodArgs);
                System.out.println("after  " + method.getName());
                return result;
            }
        );
        proxy.save();
    }
}
```

要点：
- 代理类继承自 `Proxy`，并实现目标接口。
- 拦截逻辑写在 `InvocationHandler#invoke` 中，每次方法调用都走 `invoke`。
- **必须有接口**，没有接口就无法使用 JDK 代理。
- JDK 8+ 经过优化后，调用性能与 CGLIB 差距很小。

#### 4.3 CGLIB 动态代理
基于**继承**的代理，使用 ASM 字节码框架在运行时生成目标类的**子类**，重写其非 final 方法。

```java
public class CglibProxyDemo {
    public static void main(String[] args) {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(UserServiceImpl.class);
        enhancer.setCallback((MethodInterceptor) (obj, method, args1, proxy) -> {
            System.out.println("before " + method.getName());
            Object result = proxy.invokeSuper(obj, args1);
            System.out.println("after  " + method.getName());
            return result;
        });
        UserServiceImpl proxy = (UserServiceImpl) enhancer.create();
        proxy.save();
    }
}
```

要点：
- 不依赖接口，直接继承目标类。
- **不能代理 final 类、final 方法、private 方法**。
- 通过 `FastClass` 机制建立方法索引，避免反射调用，相比早期 JDK 代理性能更高。
- Spring 内置 CGLIB（包路径在 `org.springframework.cglib.*`）。

#### 4.4 Spring AOP 的选择策略

| 场景 | 默认使用 |
| ---- | -------- |
| 目标类实现了接口 | JDK 动态代理 |
| 目标类未实现接口 | CGLIB |
| 配置 `proxyTargetClass=true` | 强制 CGLIB |
| Spring Boot 2.x+ | 默认 `proxyTargetClass=true`，统一使用 CGLIB |

源码入口：`DefaultAopProxyFactory#createAopProxy`，根据 `AdvisedSupport` 的配置决定返回 `JdkDynamicAopProxy` 还是 `ObjenesisCglibAopProxy`。

#### 4.5 JDK vs CGLIB 对比

| 维度 | JDK 动态代理 | CGLIB 动态代理 |
| ---- | ------------ | -------------- |
| 实现方式 | 基于接口，反射 | 基于继承，字节码生成 |
| 是否需要接口 | 必须 | 不需要 |
| 能否代理 final 方法 | 接口无 final，不涉及 | 不能 |
| 能否代理 private 方法 | 接口方法均 public | 不能 |
| 启动性能 | 生成代理类快 | 生成子类较慢（需字节码） |
| 调用性能 | JDK 8+ 接近 CGLIB | 通过 FastClass 调用更快 |
| 代理对象创建 | `Proxy.newProxyInstance` | `Enhancer.create` |

#### 4.6 Spring AOP 失效场景（高频面试点）
1. **同类内部方法调用**：`this.method()` 不经过代理对象，AOP 不生效。解决：通过 `AopContext.currentProxy()` 或自注入获取代理。
2. **方法被 final / static / private 修饰**：CGLIB 无法重写，AOP 失效。
3. **非 Spring 管理的对象**：手动 `new` 出来的对象不会被代理。
4. **切点表达式不匹配**：包路径、方法签名拼写错误。

#### 4.7 与 AspectJ 的区别
- Spring AOP：**运行时**代理，只支持方法级别连接点，性能略低但无需额外编译器。
- AspectJ：**编译期/类加载期**织入字节码，功能更全（字段、构造器都能切），但需要 ajc 编译器或 LTW 配置。

---

### 5. Spring事务（传播机制/失效场景）

Spring 事务管理本质上是 **AOP + ThreadLocal + 数据库事务** 的组合：通过代理拦截 `@Transactional` 方法，在方法前后调用 `PlatformTransactionManager` 完成事务的开启、提交、回滚，并将 `Connection` 绑定到当前线程的 `TransactionSynchronizationManager`。

#### 5.1 核心组件
- **`PlatformTransactionManager`**：事务管理器顶层接口，常用实现：
  - `DataSourceTransactionManager`：JDBC / MyBatis
  - `JpaTransactionManager`：JPA / Hibernate
  - `JtaTransactionManager`：分布式事务（XA）
- **`TransactionDefinition`**：定义事务属性（传播行为、隔离级别、超时、只读）。
- **`TransactionStatus`**：表示当前事务的运行状态（是否新建、是否完成、是否回滚）。
- **`TransactionInterceptor`**：AOP 拦截器，是 `@Transactional` 的核心实现，内部调用 `TransactionAspectSupport#invokeWithinTransaction`。

#### 5.2 @Transactional 常用属性

| 属性 | 含义 | 默认值 |
| ---- | ---- | ------ |
| `propagation` | 事务传播行为 | `REQUIRED` |
| `isolation` | 隔离级别 | `DEFAULT`（跟随数据库） |
| `timeout` | 超时时间（秒） | `-1` |
| `readOnly` | 只读事务 | `false` |
| `rollbackFor` | 指定回滚的异常 | `RuntimeException` 和 `Error` |
| `noRollbackFor` | 指定不回滚的异常 | 空 |

> **默认只回滚 RuntimeException / Error**，受检异常（如 `IOException`）默认不回滚，必须显式 `rollbackFor`。

#### 5.3 七种事务传播机制（Propagation）

| 传播行为 | 行为描述 | 是否新事务 |
| -------- | -------- | ---------- |
| `REQUIRED`（默认） | 当前有事务就加入，没有就新建 | 看上下文 |
| `REQUIRES_NEW` | 无论是否存在，都**挂起当前事务**，新建一个 | 是 |
| `NESTED` | 当前有事务则创建**嵌套事务**（Savepoint），否则等同 REQUIRED | 子事务 |
| `SUPPORTS` | 有事务则加入，无事务则以非事务方式执行 | 否 |
| `NOT_SUPPORTED` | 挂起当前事务，以非事务方式执行 | 否 |
| `MANDATORY` | 必须存在事务，否则抛 `IllegalTransactionStateException` | 否 |
| `NEVER` | 必须不存在事务，否则抛异常 | 否 |

**REQUIRED vs REQUIRES_NEW vs NESTED 区别**：

```java
@Service
public class OrderService {
    @Transactional // REQUIRED
    public void createOrder() {
        orderDao.insert(...);
        logService.log();   // 看下面 log() 的传播行为
        throw new RuntimeException("boom");
    }
}
```

- `log()` 用 `REQUIRED`：与 `createOrder` **共享事务**，外层回滚 → log 一起回滚。
- `log()` 用 `REQUIRES_NEW`：log **独立提交**，外层回滚不影响 log 已提交的数据。
- `log()` 用 `NESTED`：log 作为 Savepoint，外层回滚连同子事务一起回滚；子事务自己回滚不影响外层。

#### 5.4 隔离级别（Isolation）

| 隔离级别 | 脏读 | 不可重复读 | 幻读 |
| -------- | ---- | ---------- | ---- |
| `READ_UNCOMMITTED` | 可能 | 可能 | 可能 |
| `READ_COMMITTED` | 否 | 可能 | 可能 |
| `REPEATABLE_READ`（MySQL 默认） | 否 | 否 | 可能（InnoDB 通过间隙锁基本规避） |
| `SERIALIZABLE` | 否 | 否 | 否 |

`@Transactional(isolation = Isolation.DEFAULT)` 表示采用底层数据库默认隔离级别。

#### 5.5 失效场景（高频面试点）

事务失效的根因几乎都是 **AOP 代理没被走到** 或 **异常被吞掉**。

1. **方法非 public**
   - Spring AOP 只对 public 方法生效，`protected` / `private` 上加 `@Transactional` 不会启动事务。

2. **同类内部方法调用（this 调用）**
   ```java
   public void a() {
       this.b(); // ❌ 走的是原始对象的 b()，不经过代理
   }
   @Transactional
   public void b() { ... }
   ```
   **解决**：
   - 把 `b()` 抽到另一个 Bean。
   - 自注入：`@Autowired private OrderService self; self.b();`
   - 使用 `AopContext.currentProxy()`（需开启 `exposeProxy=true`）。

3. **异常被 catch 吞掉**
   ```java
   @Transactional
   public void save() {
       try {
           dao.insert(...);
       } catch (Exception e) {
           log.error(e); // ❌ 没抛出，事务不会回滚
       }
   }
   ```

4. **抛出的是受检异常但未配置 rollbackFor**
   默认只回滚 `RuntimeException` / `Error`，`SQLException`、`IOException` 等不会回滚。需要：
   ```java
   @Transactional(rollbackFor = Exception.class)
   ```

5. **数据库引擎不支持事务**
   MySQL 的 MyISAM 引擎不支持事务，需要使用 InnoDB。

6. **多线程调用**
   事务通过 `ThreadLocal` 绑定 `Connection`，子线程拿不到外层事务，事务上下文丢失。

7. **未被 Spring 管理的对象**
   `new` 出来的对象不会被代理，`@Transactional` 不生效。

8. **`@Transactional` 标在接口上 + CGLIB 代理**
   Spring 5 之前，CGLIB 代理可能无法识别接口上的注解。推荐标在**实现类的具体方法上**。

9. **传播行为使用不当**
   外层用 `NOT_SUPPORTED` 或 `NEVER`，内层方法即便加了 `@Transactional` 也不会有事务。

10. **同方法内既要新事务又要回滚旧事务**
    `REQUIRED` 下任何子方法抛异常都会把整个事务标记为 `rollback-only`，外层即使 catch 后再提交也会报：
    ```
    Transaction rolled back because it has been marked as rollback-only
    ```
    **解决**：将子方法改为 `REQUIRES_NEW`，让其独立事务。

#### 5.6 编程式事务（兜底方案）

注解失效或需要更细粒度控制时，使用 `TransactionTemplate`：

```java
@Autowired
private TransactionTemplate transactionTemplate;

public void save() {
    transactionTemplate.execute(status -> {
        try {
            dao.insert(...);
            return null;
        } catch (Exception e) {
            status.setRollbackOnly();
            throw e;
        }
    });
}
```

#### 5.7 源码入口
- `@EnableTransactionManagement` → 导入 `TransactionManagementConfigurationSelector`。
- 注册 `BeanFactoryTransactionAttributeSourceAdvisor`（Advisor）+ `TransactionInterceptor`（Advice）。
- 方法调用时 → `TransactionInterceptor#invoke` → `TransactionAspectSupport#invokeWithinTransaction`：
  1. 解析 `TransactionAttribute`（传播行为、隔离级别等）。
  2. `createTransactionIfNecessary` → 通过 `PlatformTransactionManager#getTransaction` 开启/加入事务。
  3. 执行目标方法。
  4. 异常 → `completeTransactionAfterThrowing`（按 `rollbackFor` 决定回滚或提交）。
  5. 正常 → `commitTransactionAfterReturning`。

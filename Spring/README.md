# Spring

## 核心知识点

- [x] IOC容器启动流程
- [x] Bean生命周期
- [x] DI依赖注入的方式
- [x] AOP原理（动态代理，JDK vs CGLIB）
- [ ] Spring事务（传播机制/失效场景）
- [ ] SpringMVC请求处理流程（DispatcherServlet)
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

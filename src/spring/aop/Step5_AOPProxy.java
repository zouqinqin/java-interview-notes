package spring.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.stereotype.Component;

/**
 * 第五关：AOP 代理是什么时候生成的？
 *
 * 挑战：
 *   1. 跑一下，看 PayService 的实际类型是什么？
 *   2. 和没有切面时相比，类名有什么不同？
 *   3. pay() 方法被调用前，切面会打印什么？
 */
public class Step5_AOPProxy {

    @Configuration
    @ComponentScan("spring.aop")
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    static class AppConfig {}

    @Aspect
    @Component
    static class LogAspect {

        @Before("execution(* spring.aop.PayService.pay(..))")
        public void beforePay() {
            System.out.println("[切面] pay() 被调用了");
        }
    }

    public static void main(String[] args) {
        AnnotationConfigApplicationContext ctx =
                new AnnotationConfigApplicationContext(AppConfig.class);

        PayService ipayService = ctx.getBean(PayService.class);

        System.out.println("实际类型: " + ipayService.getClass().getName());
        System.out.println("父类: " + ipayService.getClass().getSuperclass().getName());
        System.out.println("---");

        ipayService.pay(1L, 99.9);
        System.out.println("---");
        ipayService.queryBalance(1L);

        ipayService.payAndCheck(1L, 99.9);

        ctx.close();
    }

    @Aspect
    @Component
    static class SecurityAspect {
        @Before("execution(* spring.aop.PayService.pay(..))")
        public void check() {
            System.out.println("[Security] 权限检查");
        }
    }

    @Aspect
    @Component
    static class TimingAspect {
        @Around("execution(* spring.aop.PayService.pay(..))")
        public Object timing(ProceedingJoinPoint pjp) throws Throwable {
            System.out.println("[Timing] 开始计时");
            Object result = pjp.proceed();
            System.out.println("[Timing] 结束计时");
            return result;
        }
    }


}

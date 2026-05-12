package spring.ioc;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * 第四关：Bean 的一生
 *
 * 一个 Bean 从出生到死亡，Spring 会在不同阶段回调不同的方法。
 * 跑一下，观察控制台输出的顺序。
 *
 * 挑战：
 *   1. 记录下各个生命周期方法的执行顺序
 *   2. 这里有一个坑，会导致 NullPointerException，你能找到吗？
 *   3. 思考：@PostConstruct 和构造方法有什么区别？为什么需要它？
 */
public class Step4_BeanLifecycle {

    @Configuration
    @ComponentScan("spring.ioc")
    static class AppConfig {
        @Bean(initMethod = "initMethod", destroyMethod = "destroyMethod")
        public ConnectionPool connectionPool() {
            return new ConnectionPool();
        }
    }

    // 模拟连接池
    static class ConnectionPool {
        private boolean ready = false;

        public ConnectionPool() {
            System.out.println("[1] ConnectionPool 构造方法");
        }

        public void initMethod() {
            System.out.println("[6] initMethod() — 连接池初始化完成，ready=true");
            this.ready = true;
        }

        public void destroyMethod() {
            System.out.println("[destroy] destroyMethod() — 连接池关闭");
        }

        public boolean isReady() {
            return ready;
        }
    }

    @Component
    static class DataService implements BeanNameAware, BeanFactoryAware,
            ApplicationContextAware, InitializingBean, DisposableBean {

        @Autowired
        private ConnectionPool pool;

        private String beanName;

        public DataService() {
            System.out.println("[2] DataService 构造方法");
            // 试着在这里用 pool，看看会发生什么？
            System.out.println("[2] pool 此时是: " + pool);
        }

        @Override
        public void setBeanName(String name) {
            this.beanName = name;
            System.out.println("[3] BeanNameAware.setBeanName: " + name);
        }

        @Override
        public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
            System.out.println("[4a] BeanFactoryAware.setBeanFactory");
        }

        @Override
        public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
            System.out.println("[4b] ApplicationContextAware.setApplicationContext");
        }

        @PostConstruct
        public void postConstruct() {
            System.out.println("[5] @PostConstruct — pool.isReady()=" + pool.isReady());
        }

        @Override
        public void afterPropertiesSet() {
            System.out.println("[7] InitializingBean.afterPropertiesSet");
        }

        @PreDestroy
        public void preDestroy() {
            System.out.println("[pre-destroy] @PreDestroy");
        }

        @Override
        public void destroy() {
            System.out.println("[destroy] DisposableBean.destroy");
        }
    }

    public static void main(String[] args) {
        System.out.println("=== Spring 容器启动 ===");
        AnnotationConfigApplicationContext ctx =
                new AnnotationConfigApplicationContext(AppConfig.class);

        System.out.println("=== 容器就绪，开始使用 Bean ===");
        DataService service = ctx.getBean(DataService.class);

        System.out.println("=== 容器关闭 ===");
        ctx.close();
    }
}

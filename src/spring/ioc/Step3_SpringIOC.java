package spring.ioc;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

/**
 * 第三关：Spring IOC 登场
 *
 * 现在把 Step2 的支付系统交给 Spring 管理。
 * 注意观察：main() 里还有几个 new？
 * Spring 怎么知道该创建哪些对象、按什么顺序、怎么组装？
 *
 * 挑战：
 *   1. 跑一下，看 DB 连接还是只创建一次吗？
 *   2. 找找代码里还有没有 "new UserRepository" / "new OrderRepository" 这样的代码？
 *   3. @Autowired 是怎么工作的？Spring 怎么知道往哪里注入？
 *   4. 这里有一个坑，会导致一个依赖注入不生效——你能找到吗？
 */
public class Step3_SpringIOC {


    @Configuration
    @ComponentScan("spring.ioc")
    @EnableAspectJAutoProxy
    static class AppConfig {

        @Bean
        public DatabaseConn databaseConn() {
            return new DatabaseConn("jdbc:mysql://localhost:3306/payment");
        }
    }

    static class DatabaseConn {
        private String url;

        public DatabaseConn(String url) {
            this.url = url;
            System.out.println("DB连接创建: " + url);
        }

        public String query(String sql) {
            return "result_from_db";
        }
    }

    @Repository
    static class UserRepo {
        private DatabaseConn db;

        public UserRepo(DatabaseConn db) {
            this.db = db;
        }

        public String findUser(long userId) {
            return db.query("SELECT * FROM user WHERE id = " + userId);
        }
    }

    @Repository
    static class OrderRepo {
        private DatabaseConn db;

        public OrderRepo(DatabaseConn db) {
            this.db = db;
        }

        public String findOrder(long orderId) {
            return db.query("SELECT * FROM orders WHERE id = " + orderId);
        }
    }

    @Service
    static class NotifyService {
        public void sendSms(long userId, String msg) {
            System.out.println("发送短信给用户 " + userId + ": " + msg);
        }
    }

    @Service
    static class PayService {
        private UserRepo userRepo;
        private OrderRepo orderRepo;
        private NotifyService notifyService;

        @Autowired
        public PayService(UserRepo userRepo, OrderRepo orderRepo) {
            this.userRepo = userRepo;
            this.orderRepo = orderRepo;
        }
        @Autowired
        public void setNotifyService(NotifyService notifyService) {
            this.notifyService = notifyService;
        }

        public void pay(long userId, long orderId, double amount) {
            String user = userRepo.findUser(userId);
            String order = orderRepo.findOrder(orderId);
            System.out.println("处理支付: user=" + user + ", order=" + order + ", amount=" + amount);
            notifyService.sendSms(userId, "支付成功: " + amount + "元");
        }
    }

    public static void main(String[] args) {
        AnnotationConfigApplicationContext ctx =
                new AnnotationConfigApplicationContext(AppConfig.class);

        PayService payService = ctx.getBean(PayService.class);
        payService.pay(1L, 100L, 99.9);
        System.out.println("实际类型: " + payService.getClass().getName());

        ctx.close();
    }
}

package spring.ioc;

/**
 * 第二关：手动依赖注入
 *
 * 上一关的三个问题，我们用"构造器注入"来修：
 *   - DB 连接只创建一次，共享给所有 Repository
 *   - 依赖从外部传入，不在内部 new
 *   - 现在可以传入 mock 对象来写单元测试了
 *
 * 挑战：跑一下，看看 DB 连接创建几次了？
 * 然后再想想：如果系统再大一点，多几十个 Service，这个 main() 会变成什么样子？
 */
public class Step2_ManualDI {

    static class DatabaseConnection {
        private String url;

        public DatabaseConnection(String url) {
            this.url = url;
            System.out.println("DB连接创建: " + url);
        }

        public String query(String sql) {
            return "result_from_db";
        }
    }

    static class UserRepository {
        private DatabaseConnection db;

        public UserRepository(DatabaseConnection db) {
            this.db = db;
        }

        public String findUser(long userId) {
            return db.query("SELECT * FROM user WHERE id = " + userId);
        }
    }

    static class OrderRepository {
        private DatabaseConnection db;

        public OrderRepository(DatabaseConnection db) {
            this.db = db;
        }

        public String findOrder(long orderId) {
            return db.query("SELECT * FROM orders WHERE id = " + orderId);
        }
    }

    static class PaymentService {
        private UserRepository userRepo;
        private OrderRepository orderRepo;

        public PaymentService(UserRepository userRepo, OrderRepository orderRepo) {
            this.userRepo = userRepo;
            this.orderRepo = orderRepo;
        }

        public void pay(long userId, long orderId, double amount) {
            String user = userRepo.findUser(userId);
            String order = orderRepo.findOrder(orderId);
            System.out.println("处理支付: user=" + user + ", order=" + order + ", amount=" + amount);
        }
    }

    // 新需求：加一个通知服务，支付完成后发短信
    static class NotificationService {
        private DatabaseConnection db;

        public NotificationService(DatabaseConnection db) {
            this.db = db;
        }

        public void sendSms(long userId, String msg) {
            System.out.println("发送短信给用户 " + userId + ": " + msg);
        }
    }

    static class PaymentFacade {
        private PaymentService paymentService;
        private NotificationService notificationService;

        public PaymentFacade(PaymentService paymentService, NotificationService notificationService) {
            this.paymentService = paymentService;
            this.notificationService = notificationService;
        }

        public void pay(long userId, long orderId, double amount) {
            paymentService.pay(userId, orderId, amount);
            notificationService.sendSms(userId, "支付成功: " + amount + "元");
        }
    }

    public static void main(String[] args) {
        // 所有"组装"工作都集中在这里
        DatabaseConnection db = new DatabaseConnection("jdbc:mysql://localhost:3306/payment");

        UserRepository userRepo = new UserRepository(db);
        OrderRepository orderRepo = new OrderRepository(db);
        NotificationService notificationService = new NotificationService(db);
        PaymentService paymentService = new PaymentService(userRepo, orderRepo);
        PaymentFacade facade = new PaymentFacade(paymentService, notificationService);

        facade.pay(1L, 100L, 99.9);

        // 思考题：
        // 1. DB连接现在创建了几次？比 Step1 好了吗？
        // 2. 现在能 mock 了吗？怎么 mock UserRepository？
        // 3. 如果项目有 50 个 Service，100 个 Repository，这个 main() 会是什么样？
        // 4. 如果 DatabaseConnection 构造参数改了，要改几处地方？
    }
}

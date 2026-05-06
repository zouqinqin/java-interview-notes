package spring.ioc;

/**
 * 第一关：没有 IOC 的世界
 *
 * 场景：一个支付系统，不用任何框架，纯手工管理依赖。
 *
 * 挑战：代码能跑，但藏着几个"地雷"——先读代码，再跑，然后思考：
 *   1. 看输出，"DB连接创建" 出现了几次？这合理吗？
 *   2. 如果要换数据库（比如从 MySQL 换 Oracle），要改几处？
 *   3. 如果要给 PaymentService 写单元测试，怎么 mock 掉数据库？
 */
public class Step1_NoDI {

    static class DatabaseConnection {
        private String url;

        public DatabaseConnection() {
            this.url = "jdbc:mysql://localhost:3306/payment";
            System.out.println("DB连接创建: " + url);
        }

        public String query(String sql) {
            return "result_from_db";
        }
    }

    static class UserRepository {
        private DatabaseConnection db = new DatabaseConnection();

        public String findUser(long userId) {
            return db.query("SELECT * FROM user WHERE id = " + userId);
        }
    }

    static class OrderRepository {
        private DatabaseConnection db = new DatabaseConnection();

        public String findOrder(long orderId) {
            return db.query("SELECT * FROM orders WHERE id = " + orderId);
        }
    }

    static class PaymentService {
        private UserRepository userRepo = new UserRepository();
        private OrderRepository orderRepo = new OrderRepository();

        public void pay(long userId, long orderId, double amount) {
            String user = userRepo.findUser(userId);
            String order = orderRepo.findOrder(orderId);
            System.out.println("处理支付: user=" + user + ", order=" + order + ", amount=" + amount);
        }
    }

    public static void main(String[] args) {
        PaymentService paymentService = new PaymentService();
        paymentService.pay(1L, 100L, 99.9);
    }
}

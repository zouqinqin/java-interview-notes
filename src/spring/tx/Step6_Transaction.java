package spring.tx;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;

/**
 * 第六关：Spring 事务传播机制
 *
 * 场景：用户支付 → 扣款 + 记录流水，两步必须在同一事务里。
 * 使用内嵌 H2 数据库，不需要安装任何数据库。
 *
 * 挑战：
 *   1. 跑一下，观察正常流程输出
 *   2. 把 payWithRecord() 里的 throw 那行取消注释，再跑
 *      看看 account 表和 record 表的数据各是什么？
 *   3. 把 @Transactional 去掉后重复第2步，结果变了吗？
 */
public class Step6_Transaction {

    @Configuration
    @ComponentScan("spring.tx")
    @EnableTransactionManagement
    static class AppConfig {

        @Bean
        public DataSource dataSource() {
            return new EmbeddedDatabaseBuilder()
                    .setType(EmbeddedDatabaseType.H2)
                    .addScript("classpath:spring/tx/schema.sql")
                    .build();
        }

        @Bean
        public JdbcTemplate jdbcTemplate(DataSource dataSource) {
            return new JdbcTemplate(dataSource);
        }

        @Bean
        public PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }
    }

    @Service
    static class AccountService {

        private final JdbcTemplate jdbc;

        public AccountService(JdbcTemplate jdbc) {
            this.jdbc = jdbc;
        }

        @Transactional
        public void payWithRecord(long userId, double amount) {
            // 第一步：扣款
            jdbc.update("UPDATE account SET balance = balance - ? WHERE user_id = ?", amount, userId);
            System.out.println("扣款完成: " + amount);

            // 第二步：记录流水
            jdbc.update("INSERT INTO record(user_id, amount) VALUES(?, ?)", userId, amount);
            System.out.println("流水记录完成");

            // 取消下面这行注释，模拟异常
            // throw new RuntimeException("系统异常！");
        }

        public void printData() {
            Double balance = jdbc.queryForObject("SELECT balance FROM account WHERE user_id = 1", Double.class);
            Integer records = jdbc.queryForObject("SELECT COUNT(*) FROM record", Integer.class);
            System.out.println("账户余额: " + balance + "，流水条数: " + records);
        }
    }

    public static void main(String[] args) {
        AnnotationConfigApplicationContext ctx =
                new AnnotationConfigApplicationContext(AppConfig.class);

        AccountService service = ctx.getBean(AccountService.class);

        System.out.println("=== 支付前 ===");
        service.printData();

        System.out.println("\n=== 执行支付 ===");
        try {
            service.payWithRecord(1L, 100.0);
        } catch (Exception e) {
            System.out.println("捕获异常: " + e.getMessage());
        }

        System.out.println("\n=== 支付后 ===");
        service.printData();

        ctx.close();
    }
}

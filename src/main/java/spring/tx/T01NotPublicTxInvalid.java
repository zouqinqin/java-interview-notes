package spring.tx;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 验证事务失效场景
 * 1. 方法非 public
 *    - Spring AOP 只对 public 方法生效，protected 上加 @Transactional 不会启动事务。
 *
 * 验证步骤：
 *   1. 访问 /txInvalid/public  → 抛异常，查 record 表条数不变（事务回滚）
 *   2. 访问 /txInvalid/protected → 抛异常，查 record 表条数+1（事务失效，未回滚）
 *   3. 访问 /txInvalid/count 查看当前 record 表条数
 */
@RestController
@RequestMapping("/txInvalid")
public class T01NotPublicTxInvalid {

    private final OrderService orderService;
    private final JdbcTemplate jdbcTemplate;

    public T01NotPublicTxInvalid(OrderService orderService, JdbcTemplate jdbcTemplate) {
        this.orderService = orderService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @RequestMapping(value = "/getOrderInfo")
    public Order getOrderInfo(@RequestParam String orderId) {
        return orderService.getOrderInfo(orderId);
    }

    /**
     * 测试 public 方法事务：插入后抛异常，事务生效 → 数据回滚
     */
    @RequestMapping("/public")
    public String testPublic() {
        try {
            orderService.publicTransactionalMethod();
        } catch (Exception e) {
            return "public 方法抛异常被捕获，事务应已回滚。当前 record 条数: " + recordCount();
        }
        return "未抛异常";
    }

    /**
     * 测试 protected 方法事务：插入后抛异常，事务失效 → 数据未回滚
     */
    @RequestMapping("/protected")
    public String testProtected() {
        try {
            orderService.protectedTransactionalMethod();
        } catch (Exception e) {
            return "protected 方法抛异常被捕获，事务应已失效。当前 record 条数: " + recordCount();
        }
        return "未抛异常";
    }

    /**
     * 查看当前 record 表条数
     */
    @RequestMapping("/count")
    public String count() {
        return "当前 record 条数: " + recordCount();
    }

    private int recordCount() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM record", Integer.class);
        return count == null ? 0 : count;
    }
}

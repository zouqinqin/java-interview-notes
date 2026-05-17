package spring.tx;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 事务失效场景 6：多线程调用
 *
 * 原理：
 *   Spring 事务管理器把 Connection 绑定在 TransactionSynchronizationManager 的 ThreadLocal 中，
 *   即"事务 = 线程 + Connection"。
 *   子线程的 ThreadLocal 是独立的，拿不到外层事务的 Connection，
 *   于是子线程内部的 JDBC 操作会从连接池另取一条非事务连接（auto-commit=true）独立提交。
 *
 * 验证：
 *   访问 /thread
 *     before = 当前 record 条数
 *     外层 @Transactional 方法启动子线程做 INSERT → 子线程独立提交
 *     外层抛 RuntimeException → 外层事务回滚（但外层根本没插数据）
 *     after = before + 1  →  说明子线程的写入未被回滚，事务上下文未传播
 */
@RestController
public class T06Thread {

    private final OrderService orderService;
    private final JdbcTemplate jdbcTemplate;

    public T06Thread(OrderService orderService, JdbcTemplate jdbcTemplate) {
        this.orderService = orderService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @RequestMapping("/thread")
    public String thread() {
        int before = recordCount();
        try {
            orderService.threadMethod();
        } catch (Exception e) {
            int after = recordCount();
            return String.format(
                    "多线程调用 → before=%d, after=%d, 差值=%d (期望 +1：子线程 INSERT 未被外层事务回滚)",
                    before, after, after - before);
        }
        return "未抛异常";
    }

    private int recordCount() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM record", Integer.class);
        return count == null ? 0 : count;
    }
}

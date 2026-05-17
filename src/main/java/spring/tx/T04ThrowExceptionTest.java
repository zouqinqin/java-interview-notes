package spring.tx;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class T04ThrowExceptionTest {
    private final OrderService orderService;
    private final JdbcTemplate jdbcTemplate;

    public T04ThrowExceptionTest(OrderService orderService,JdbcTemplate jdbcTemplate){
        this.orderService=orderService;
        this.jdbcTemplate =jdbcTemplate;
    }

    @RequestMapping("/checkedNoRollbackFor")
    public String checkedNoRollbackFor() {
        int before = recordCount();
        try {
            orderService.checkedExceptionNoRollbackFor();
        } catch (Exception e) {
            int after = recordCount();
            return String.format("受检异常 默认不回滚 → before=%d, after=%d, 差值=%d (期望 +1)",
                    before, after, after - before);
        }
        return "未抛异常";
    }

    @RequestMapping("/checkedWithRollbackFor")
    public String checkedWithRollbackFor() {
        int before = recordCount();
        try {
            orderService.checkedExceptionWithRollbackFor();
        } catch (Exception e) {
            int after = recordCount();
            return String.format("受检异常 + rollbackFor → before=%d, after=%d, 差值=%d (期望 0)",
                    before, after, after - before);
        }
        return "未抛异常";
    }

    private int recordCount() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM record", Integer.class);
        return count == null ? 0 : count;
    }
}

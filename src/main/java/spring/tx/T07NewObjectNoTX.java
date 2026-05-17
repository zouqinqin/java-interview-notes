package spring.tx;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class T07NewObjectNoTX {


    private final JdbcTemplate jdbcTemplate;

    public T07NewObjectNoTX(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @RequestMapping("/newObjectNoTX")
    public String newObjectNoTX() {
        OrderService orderService = new OrderServiceImpl(jdbcTemplate);
        int before = recordCount();
        try{
            orderService.newObjectNoTX();
        }catch (Exception e) {
            int after = recordCount();
            return String.format(
                    "new Object调用 → before=%d, after=%d, 差值=%d (期望 +1：子线程 INSERT 未被外层事务回滚)",
                    before, after, after - before);
        }
        return "未抛异常";
    }
    private int recordCount() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM record", Integer.class);
        return count == null ? 0 : count;
    }
}

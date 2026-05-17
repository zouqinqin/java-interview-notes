package spring.tx;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class T03ExceptionNoThrown {

    private final OrderService orderService;
    private final JdbcTemplate jdbcTemplate;

    public T03ExceptionNoThrown(OrderService orderService,JdbcTemplate jdbcTemplate){
        this.orderService=orderService;
        this.jdbcTemplate =jdbcTemplate;
    }

    @RequestMapping("/noThrowMethod")
    public String insertOrder(){
        try {
            orderService.noThrowMethod();
        }catch (Exception e){
            System.out.println("insertOrder 调用this.insertOrder2方法，事务失效不会回滚。当前 record 条数: " + recordCount());
        }
        return "未抛异常";
    }

    private int recordCount() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM record", Integer.class);
        return count == null ? 0 : count;
    }

}

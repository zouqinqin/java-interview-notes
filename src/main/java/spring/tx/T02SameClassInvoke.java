package spring.tx;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 事务实效场景2
 * **同类内部方法调用（this 调用）**
 *    ```java
 *    public void a() {
 *        this.b(); // ❌ 走的是原始对象的 b()，不经过代理
 *    }
 *    @Transactional
 *    public void b() { ... }
 */
@RestController
public class T02SameClassInvoke {

    private final OrderService orderService;
    private final JdbcTemplate jdbcTemplate;

    public T02SameClassInvoke(OrderService orderService,JdbcTemplate jdbcTemplate){
        this.orderService=orderService;
        this.jdbcTemplate =jdbcTemplate;
    }
    @RequestMapping("/insertOrder")
    public String insertOrder(){
        try {
            orderService.insertOrder();
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

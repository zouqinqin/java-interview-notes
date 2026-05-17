package spring.tx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;

@Service
public class OrderServiceImpl implements OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderServiceImpl.class);

    private final JdbcTemplate jdbcTemplate;
    private final InsertHelper insertHelper;

    @Lazy
    @Autowired
    private OrderService self;

    public OrderServiceImpl(JdbcTemplate jdbcTemplate,InsertHelper insertHelper) {
        this.jdbcTemplate = jdbcTemplate;
        this.insertHelper = insertHelper;
    }

    @Override
    @Transactional
    public Order getOrderInfo(String orderId) {
        Order order = new Order();
        order.setOrderId(orderId);
        return order;
    }

    /**
     * public 方法加 @Transactional：事务生效
     * 插入一条 record 后抛异常 → 数据回滚 → record 表无新数据
     */
    @Override
    @Transactional
    public void publicTransactionalMethod() {
        jdbcTemplate.update("INSERT INTO record(user_id, amount) VALUES(?, ?)", 1L, 99.0);
        System.out.println("public 方法：插入 record 成功，即将抛异常...");
        throw new RuntimeException("模拟异常");
    }

    /**
     * protected 方法加 @Transactional：事务失效
     * 插入一条 record 后抛异常 → 数据不回滚 → record 表有新数据
     */
    @Transactional
    protected void protectedInsert() {
        jdbcTemplate.update("INSERT INTO record(user_id, amount) VALUES(?, ?)", 1L, 88.0);
        System.out.println("protected 方法：插入 record 成功，即将抛异常...");
        throw new RuntimeException("模拟异常");
    }

    /**
     * 接口方法，内部调用 protectedInsert
     * this 调用绕过 AOP 代理，@Transactional 不生效
     */
    @Override
    public void protectedTransactionalMethod() {
        this.protectedInsert();
    }

    @Override
    public void insertOrder() {
//        this.insertOrder2();
        ((OrderService)AopContext.currentProxy()).insertOrder2();
    }
    @Override
    @Transactional
    public void insertOrder2(){
        jdbcTemplate.update("INSERT INTO record(user_id, amount) VALUES(?, ?)", 1L, 77);
        System.out.println("insertOrder方法：插入 insertOrder 成功，即将异常..");
        throw new RuntimeException("模拟异常");
    }

    @Override
    @Transactional
    public void noThrowMethod() {
        try{
            jdbcTemplate.update("INSERT INTO record(user_id, amount) VALUES(?, ?)", 1L, 77);
            System.out.println("insertOrder方法：插入 insertOrder 成功，即将异常..");
            int a =1/0;
        }catch (Exception e){
            logger.error(e.getMessage());
        }
    }

    @Override
    @Transactional   // 没有 rollbackFor
    public void checkedExceptionNoRollbackFor() throws Exception {
        jdbcTemplate.update("INSERT INTO record(user_id, amount) VALUES(?, ?)", 1L, 66.0);
        System.out.println("插入成功，即将抛受检异常（无 rollbackFor）...");
        throw new Exception("受检异常 - 默认不回滚");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void checkedExceptionWithRollbackFor() throws Exception {
        jdbcTemplate.update("INSERT INTO record(user_id, amount) VALUES(?, ?)", 1L, 55.0);
        System.out.println("插入成功，即将抛受检异常（已配 rollbackFor）...");
        throw new Exception("受检异常 + rollbackFor=Exception → 回滚");
    }
}

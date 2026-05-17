package spring.tx;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InsertHelper {

    @Autowired
    private  JdbcTemplate jdbcTemplate;

    @Transactional
    public void doInsert() {
        jdbcTemplate.update("INSERT INTO record(user_id, amount) VALUES(?, ?)", 1L, 77);
        System.out.println("insertOrder方法：插入 insertOrder 成功，即将异常..");
        throw new RuntimeException("模拟异常");
    }
}

package spring.tx;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * 故意不加 @Transactional：注解只在接口上。
 */
@Service
public class T08TxOnInterfaceServiceImpl implements T08TxOnInterfaceService {

    private final JdbcTemplate jdbcTemplate;

    public T08TxOnInterfaceServiceImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void doInsertAndThrow() {
        jdbcTemplate.update("INSERT INTO record(user_id, amount) VALUES(?, ?)", 1L, 22.0);
        System.out.println("T08：插入成功，即将抛 RuntimeException...");
        throw new RuntimeException("接口上注解 + CGLIB 验证");
    }
}

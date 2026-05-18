package spring.tx;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class T10RollbackOnlyServiceImpl implements T10RollbackOnlyService {

    private final JdbcTemplate jdbcTemplate;

    @Lazy
    @Autowired
    private T10RollbackOnlyService self;

    public T10RollbackOnlyServiceImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 经典坑：外层先写一条，然后调内层；内层抛异常被外层吞掉，外层方法正常返回。
     * Spring 在提交阶段发现 tx 被标记 rollback-only → 抛 UnexpectedRollbackException。
     * 结果：外层那条 INSERT 也一起被回滚。
     */
    @Override
    @Transactional
    public void outerCatchInnerThrow_required() {
        jdbcTemplate.update("INSERT INTO record(user_id, amount) VALUES(?, ?)", 1L, 21.0);
        System.out.println("outer：已插入一条（21.0），开始调内层...");
        try {
            self.innerRequired();
        } catch (Exception e) {
            System.out.println("outer：catch 到内层异常 [" + e.getMessage()
                    + "]，事务已被标记 rollback-only，但外层方法继续正常返回。");
        }
        System.out.println("outer：方法即将正常 return，由 Spring 触发提交...");
    }

    /**
     * 修复版：内层 REQUIRES_NEW，独立物理事务。
     * 内层异常只回滚内层的事务，外层的事务和那条 21.0 不受影响。
     */
    @Override
    @Transactional
    public void outerCatchInnerThrow_requiresNew() {
        jdbcTemplate.update("INSERT INTO record(user_id, amount) VALUES(?, ?)", 1L, 31.0);
        System.out.println("outer：已插入一条（31.0），开始调内层(REQUIRES_NEW)...");
        try {
            self.innerRequiresNew();
        } catch (Exception e) {
            System.out.println("outer：catch 到内层异常 [" + e.getMessage()
                    + "]，但内层是独立事务，外层 rollback-only 标记没被设置。");
        }
        System.out.println("outer：方法即将正常 return，外层事务可以正常提交。");
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void innerRequired() {
        jdbcTemplate.update("INSERT INTO record(user_id, amount) VALUES(?, ?)", 1L, 22.0);
        System.out.println("inner REQUIRED：已插入一条（22.0），即将抛异常...");
        throw new RuntimeException("inner REQUIRED 抛异常");
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void innerRequiresNew() {
        jdbcTemplate.update("INSERT INTO record(user_id, amount) VALUES(?, ?)", 1L, 32.0);
        System.out.println("inner REQUIRES_NEW：已插入一条（32.0），即将抛异常...");
        throw new RuntimeException("inner REQUIRES_NEW 抛异常");
    }
}

package spring.tx;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class T09PropagationServiceImpl implements T09PropagationService {

    private final JdbcTemplate jdbcTemplate;

    /** 自注入：用代理调内层，确保 @Transactional 生效 */
    @Lazy
    @Autowired
    private T09PropagationService self;

    public T09PropagationServiceImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /* ----- 外层方法 ----- */

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void outerNotSupported_innerSupports() {
        printTxState("outer NOT_SUPPORTED");
        self.innerSupports();
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void outerNotSupported_innerRequired() {
        printTxState("outer NOT_SUPPORTED");
        self.innerRequired();
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public void outerNever_innerSupports() {
        printTxState("outer NEVER");
        self.innerSupports();
    }

    /* ----- 内层方法 ----- */

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public void innerSupports() {
        printTxState("inner SUPPORTS");
        jdbcTemplate.update("INSERT INTO record(user_id, amount) VALUES(?, ?)", 1L, 11.0);
        throw new RuntimeException("inner SUPPORTS 抛异常");
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void innerRequired() {
        printTxState("inner REQUIRED");
        jdbcTemplate.update("INSERT INTO record(user_id, amount) VALUES(?, ?)", 1L, 12.0);
        throw new RuntimeException("inner REQUIRED 抛异常");
    }

    private void printTxState(String tag) {
        boolean active = TransactionSynchronizationManager.isActualTransactionActive();
        String name = TransactionSynchronizationManager.getCurrentTransactionName();
        System.out.printf("[%s] active=%s, currentTxName=%s%n", tag, active, name);
    }
}

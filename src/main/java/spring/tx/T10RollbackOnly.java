package spring.tx;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 验证场景 10：rollback-only
 *
 * 两个端点对照：
 *
 *   /rollbackOnly/required
 *     外层 REQUIRED + 内层 REQUIRED，外层 catch 内层异常
 *     → 外层提交时抛 UnexpectedRollbackException
 *     → 期望差值=0（外层那条 INSERT 也被一起回滚）
 *     → 期望异常类型=UnexpectedRollbackException
 *
 *   /rollbackOnly/requiresNew
 *     外层 REQUIRED + 内层 REQUIRES_NEW，外层 catch 内层异常
 *     → 内层独立事务自己回滚，外层不受影响
 *     → 期望差值=1（外层那条 INSERT 提交成功，内层那条被回滚）
 *     → 期望无异常
 */
@RestController
@RequestMapping("/rollbackOnly")
public class T10RollbackOnly {

    private final T10RollbackOnlyService service;
    private final JdbcTemplate jdbcTemplate;

    public T10RollbackOnly(T10RollbackOnlyService service, JdbcTemplate jdbcTemplate) {
        this.service = service;
        this.jdbcTemplate = jdbcTemplate;
    }

    @RequestMapping("/required")
    public String required() {
        int before = recordCount();
        String outcome;
        try {
            service.outerCatchInnerThrow_required();
            outcome = "外层方法正常返回，未抛异常（理论上不可能走到这）";
        } catch (Exception e) {
            outcome = "外层抛出异常: " + e.getClass().getSimpleName() + " - " + e.getMessage();
        }
        int after = recordCount();
        int delta = after - before;
        return String.format("[REQUIRED + REQUIRED]%n%s%nbefore=%d, after=%d, 差值=%d (期望 0：rollback-only 把外层 INSERT 一起干掉)",
                outcome, before, after, delta);
    }

    @RequestMapping("/requiresNew")
    public String requiresNew() {
        int before = recordCount();
        String outcome;
        try {
            service.outerCatchInnerThrow_requiresNew();
            outcome = "外层方法正常返回，外层 tx 提交成功";
        } catch (Exception e) {
            outcome = "外层抛出异常: " + e.getClass().getSimpleName() + " - " + e.getMessage();
        }
        int after = recordCount();
        int delta = after - before;
        return String.format("[REQUIRED + REQUIRES_NEW]%n%s%nbefore=%d, after=%d, 差值=%d (期望 1：外层 INSERT 提交，内层独立 tx 回滚)",
                outcome, before, after, delta);
    }

    private int recordCount() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM record", Integer.class);
        return count == null ? 0 : count;
    }
}

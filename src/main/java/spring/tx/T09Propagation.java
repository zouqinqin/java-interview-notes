package spring.tx;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 验证场景 9：传播行为使用不当
 *
 * 三个端点对照实验：
 *
 *   /prop/notSupported_supports
 *     外层 NOT_SUPPORTED + 内层 SUPPORTS
 *     → 外层无 tx，内层 SUPPORTS 跟随 → 内层也无 tx → INSERT 不回滚
 *     → 差值=+1，命题【成立】
 *
 *   /prop/notSupported_required
 *     外层 NOT_SUPPORTED + 内层 REQUIRED
 *     → 外层无 tx，内层 REQUIRED 自起新 tx → 抛异常回滚
 *     → 差值=0，命题【不成立】（内层照样有事务）
 *
 *   /prop/never_supports
 *     外层 NEVER + 内层 SUPPORTS
 *     → 外层无 tx，内层 SUPPORTS 跟随 → 内层也无 tx → INSERT 不回滚
 *     → 差值=+1，命题【成立】
 */
@RestController
@RequestMapping("/prop")
public class T09Propagation {

    private final T09PropagationService service;
    private final JdbcTemplate jdbcTemplate;

    public T09Propagation(T09PropagationService service, JdbcTemplate jdbcTemplate) {
        this.service = service;
        this.jdbcTemplate = jdbcTemplate;
    }

    @RequestMapping("/notSupported_supports")
    public String notSupportedSupports() {
        return run("NOT_SUPPORTED + SUPPORTS", service::outerNotSupported_innerSupports, 1);
    }

    @RequestMapping("/notSupported_required")
    public String notSupportedRequired() {
        return run("NOT_SUPPORTED + REQUIRED", service::outerNotSupported_innerRequired, 0);
    }

    @RequestMapping("/never_supports")
    public String neverSupports() {
        return run("NEVER + SUPPORTS", service::outerNever_innerSupports, 1);
    }

    private String run(String tag, Runnable action, int expectedDelta) {
        int before = recordCount();
        try {
            action.run();
        } catch (Exception e) {
            int after = recordCount();
            int delta = after - before;
            String verdict = (delta == expectedDelta) ? "符合预期" : "不符合预期";
            return String.format("[%s] before=%d, after=%d, 差值=%d (期望 %d, %s)%n异常: %s",
                    tag, before, after, delta, expectedDelta, verdict, e.getMessage());
        }
        return "未抛异常";
    }

    private int recordCount() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM record", Integer.class);
        return count == null ? 0 : count;
    }
}

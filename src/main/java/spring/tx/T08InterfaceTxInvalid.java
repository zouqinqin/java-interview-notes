package spring.tx;

import org.springframework.aop.support.AopUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 验证场景 8：@Transactional 标在接口上 + CGLIB 代理
 *
 * 访问 /interfaceTx ，输出包含三部分：
 *   1. proxyClass：代理类名（含 $$EnhancerBySpringCGLIB$$ → CGLIB；含 $Proxy → JDK 动态代理）
 *   2. isCglib   ：AopUtils 判定结果，二次确认
 *   3. 差值      ：record 表 +1 → 事务失效 ; 差值 0 → 事务生效
 *
 * Spring Boot 默认 spring.aop.proxy-target-class=true（CGLIB），无需额外配置。
 * 在 Spring 5.3.39 上预期结果：差值=0（事务依然生效，源码原因见类尾说明）。
 */
@RestController
public class T08InterfaceTxInvalid {

    private final T08TxOnInterfaceService service;
    private final JdbcTemplate jdbcTemplate;

    public T08InterfaceTxInvalid(T08TxOnInterfaceService service, JdbcTemplate jdbcTemplate) {
        this.service = service;
        this.jdbcTemplate = jdbcTemplate;
    }

    @RequestMapping("/interfaceTx")
    public String interfaceTx() {
        String proxyClass = service.getClass().getName();
        boolean isCglib = AopUtils.isCglibProxy(service);
        boolean isJdk = AopUtils.isJdkDynamicProxy(service);

        int before = recordCount();
        try {
            service.doInsertAndThrow();
        } catch (Exception e) {
            int after = recordCount();
            int delta = after - before;
            String verdict = (delta == 0)
                    ? "事务【生效】(Spring 5+ 兜底，接口注解仍被识别)"
                    : "事务【失效】(差值 +1，接口注解未被识别)";
            return String.format(
                    "proxyClass = %s%nisCglibProxy = %s , isJdkDynamicProxy = %s%nbefore=%d, after=%d, 差值=%d%n%s",
                    proxyClass, isCglib, isJdk, before, after, delta, verdict);
        }
        return "未抛异常";
    }

    private int recordCount() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM record", Integer.class);
        return count == null ? 0 : count;
    }
}

package spring.tx;

import org.springframework.transaction.annotation.Transactional;

/**
 * 演示 8：@Transactional 标在【接口】方法上
 *
 * 反面教材：注解写在接口而非实现类。
 * - Spring 4.x + CGLIB 代理 时，CGLIB 是基于子类继承生成代理，
 *   而 Java 注解默认不从接口继承到方法上 → 代理类找不到 @Transactional → 事务失效。
 * - Spring 5+ 的 AnnotationTransactionAttributeSource 做了兜底，
 *   找不到方法注解时会沿接口方法继续找，CGLIB 也能识别到 → 失效问题已基本消除。
 *
 * 最佳实践：永远把 @Transactional 写在【实现类的具体方法】上。
 */
public interface T08TxOnInterfaceService {

    @Transactional
    void doInsertAndThrow();
}

package spring.tx;

/**
 * 演示 10：同方法内既要新事务又要回滚旧事务
 *
 * REQUIRED 传播下，内层方法和外层方法共享同一个物理事务。
 * 内层抛异常 → TransactionInterceptor 会把整个共享事务标记为 rollback-only。
 * 外层即使 catch 了异常想"吞掉"它继续提交，
 * AbstractPlatformTransactionManager#processCommit 会发现 rollback-only 标记 →
 * 强制回滚 + 抛 UnexpectedRollbackException：
 *   "Transaction rolled back because it has been marked as rollback-only"
 *
 * 修复：让内层使用 REQUIRES_NEW（独立物理事务），内层自己回滚，不污染外层。
 */
public interface T10RollbackOnlyService {

    /** 外层 REQUIRED + 内层 REQUIRED → 内层抛 → 外层 catch → 提交时 UnexpectedRollbackException */
    void outerCatchInnerThrow_required();

    /** 外层 REQUIRED + 内层 REQUIRES_NEW → 内层独立事务，回滚不污染外层 */
    void outerCatchInnerThrow_requiresNew();

    /** 内层：REQUIRED（默认），与外层共享同一个事务 */
    void innerRequired();

    /** 内层：REQUIRES_NEW，独立物理事务 */
    void innerRequiresNew();
}

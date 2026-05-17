package spring.tx;

/**
 * 演示 9：传播行为使用不当
 *
 * 命题：外层 NOT_SUPPORTED / NEVER，内层即便加了 @Transactional 也不会有事务。
 * 真相（必须看内层传播行为）：
 *   - 内层 SUPPORTS  ：跟随外层 → 外层无事务 → 内层无事务 → 命题成立
 *   - 内层 REQUIRED  ：外层无事务时自起新事务 → 内层有事务 → 命题不成立
 *   - 内层 MANDATORY ：要求外层必须有事务 → 直接抛 IllegalTransactionStateException
 */
public interface T09PropagationService {

    /** 外层 NOT_SUPPORTED → self.innerSupports() */
    void outerNotSupported_innerSupports();

    /** 外层 NOT_SUPPORTED → self.innerRequired() */
    void outerNotSupported_innerRequired();

    /** 外层 NEVER → self.innerSupports() */
    void outerNever_innerSupports();

    /** 内层方法：SUPPORTS（跟随外层） */
    void innerSupports();

    /** 内层方法：REQUIRED（外层无事务时自起新事务） */
    void innerRequired();
}

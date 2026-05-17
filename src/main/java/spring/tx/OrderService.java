package spring.tx;

public interface OrderService {
    Order getOrderInfo(String orderId);

    // public 方法加 @Transactional，异常后数据应回滚
    void publicTransactionalMethod();

    // protected 方法加 @Transactional，异常后数据不会回滚（事务失效）
     void protectedTransactionalMethod();

    void insertOrder();

    void insertOrder2();

    void noThrowMethod();

    // 受检异常 + 默认 @Transactional → 不回滚
    void checkedExceptionNoRollbackFor() throws Exception;

    // 受检异常 + rollbackFor=Exception.class → 回滚
    void checkedExceptionWithRollbackFor() throws Exception;
}

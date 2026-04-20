namespace java com.demo.thrift

struct CollectionTask {
    1: required i64 taskId,
    2: required string customerId,
    3: required double overdueAmount,
    4: required i32 overdueDays,
    5: required string priority
}

exception BizException {
    1: i32 code,
    2: string message
}

service ReconciliationService {
    CollectionTask getTask(1: i64 taskId) throws (1: BizException e),
    list<CollectionTask> getTasksByCustomer(1: string customerId) throws (1: BizException e),
    bool createTask(1: CollectionTask task) throws (1: BizException e)
}
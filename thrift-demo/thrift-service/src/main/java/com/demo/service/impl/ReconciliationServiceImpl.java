package com.demo.service.impl;

import com.demo.thrift.BizException;
import com.demo.thrift.CollectionTask;
import com.demo.thrift.ReconciliationService;
import org.apache.thrift.TException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ReconciliationServiceImpl implements ReconciliationService.Iface {

    // 模拟数据库，用 Map 存数据
    private static final Map<Long, CollectionTask> DB = new ConcurrentHashMap<>();

    static {
        // 初始化几条测试数据
        CollectionTask t1 = new CollectionTask();
        t1.setTaskId(1L);
        t1.setCustomerId("C001");
        t1.setOverdueAmount(5000.00);
        t1.setOverdueDays(30);
        t1.setPriority("HIGH");
        DB.put(1L, t1);

        CollectionTask t2 = new CollectionTask();
        t2.setTaskId(2L);
        t2.setCustomerId("C001");
        t2.setOverdueAmount(1200.00);
        t2.setOverdueDays(10);
        t2.setPriority("LOW");
        DB.put(2L, t2);

        CollectionTask t3 = new CollectionTask();
        t3.setTaskId(3L);
        t3.setCustomerId("C002");
        t3.setOverdueAmount(8800.00);
        t3.setOverdueDays(60);
        t3.setPriority("HIGH");
        DB.put(3L, t3);
    }

    @Override
    public CollectionTask getTask(long taskId) throws TException {
        CollectionTask task = DB.get(taskId);
        if (task == null) {
            throw new BizException(404, "任务不存在: " + taskId);
        }
        return task;
    }

    @Override
    public List<CollectionTask> getTasksByCustomer(String customerId) throws TException {
        if (customerId == null || customerId.isEmpty()) {
            throw new BizException(400, "customerId 不能为空");
        }
        List<CollectionTask> result = new ArrayList<>();
        for (CollectionTask task : DB.values()) {
            if (customerId.equals(task.getCustomerId())) {
                result.add(task);
            }
        }
        return result;
    }

    @Override
    public boolean createTask(CollectionTask task) throws TException {
        if (task == null) {
            throw new BizException(400, "task 不能为空");
        }
        if (DB.containsKey(task.getTaskId())) {
            throw new BizException(409, "任务已存在: " + task.getTaskId());
        }
        DB.put(task.getTaskId(), task);
        System.out.println("创建催收任务成功: " + task.getTaskId());
        return true;
    }
}
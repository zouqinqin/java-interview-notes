package com.demo.web.controller;

import com.demo.thrift.BizException;
import com.demo.thrift.CollectionTask;
import com.demo.web.client.ThriftClientConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/collection")
public class ReconciliationController {

    @Autowired
    private ThriftClientConfig thriftClient;

    // 查询单个任务
    // GET http://localhost:8080/api/collection/task/1
    @GetMapping("/task/{taskId}")
    public CollectionTask getTask(@PathVariable long taskId) throws Exception {
        try {
            return thriftClient.getClient().getTask(taskId);
        } catch (BizException e) {
            throw new RuntimeException("业务异常: " + e.getMessage());
        }
    }

    // 按客户查询任务列表
    // GET http://localhost:8080/api/collection/customer/C001
    @GetMapping("/customer/{customerId}")
    public List<CollectionTask> getByCustomer(@PathVariable String customerId) throws Exception {
        try {
            return thriftClient.getClient().getTasksByCustomer(customerId);
        } catch (BizException e) {
            throw new RuntimeException("业务异常: " + e.getMessage());
        }
    }

    // 创建催收任务
    // POST http://localhost:8080/api/collection/task
    @PostMapping("/task")
    public boolean createTask(@RequestBody CollectionTask task) throws Exception {
        try {
            return thriftClient.getClient().createTask(task);
        } catch (BizException e) {
            throw new RuntimeException("业务异常: " + e.getMessage());
        }
    }
}
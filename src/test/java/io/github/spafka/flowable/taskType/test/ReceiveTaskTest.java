package io.github.spafka.flowable.taskType.test;


import io.github.spafka.flowable.service.FlowBase;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.DeploymentBuilder;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.Test;

/**
 * Java接收任务--测试
 */
public class ReceiveTaskTest extends FlowBase {
    String id = null;

    /**
     * 部署
     */
    @Test
    public void deploy() {
        DeploymentBuilder deploymentBuilder = repositoryService
                .createDeployment()
                .category("receiveprocess")
                .name("receiveprocess")
                .addClasspathResource("process/Java接收任务.bpmn20.xml");
        Deployment deploy = deploymentBuilder.deploy();

        System.out.println("流程ID: " + deploy.getId());
    }

    /**
     * 启动流程实例
     */
    @Test
    public void start() {
        String processDefinitionKey = "receiveprocess";
        Map<String, Object> variables = new HashMap<String, Object>();
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(processDefinitionKey, variables);
        String id = processInstance.getId();

        this.id = id;
    }

    /**
     * 触发流程穿过接收任务继续执行
     */
    @Test
    public void trigger() {

        deploy();
        start();
        List<Execution> list = runtimeService.createExecutionQuery()
                .processInstanceId(id)
                .list();

        String executionId = list.get(1).getId();
        runtimeService.trigger(executionId);

        taskService.complete( listall().get(0).getId());
    }

    /**
     * 完成任务
     */
    @Test
    public void complete() {
        String taskId = "177502";
        taskService.complete(taskId);
    }
}

package io.github.spafka.flowable.taskType.test;

import io.github.spafka.flowable.service.FlowBase;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.DeploymentBuilder;
import org.junit.jupiter.api.Test;


import java.util.HashMap;
import java.util.Map;


/**
 * 用户任务--测试
 */
public class ServiceTaskTest extends FlowBase {

    /**
     * 部署
     */
    @Test
    public void deploy() {
        DeploymentBuilder deploymentBuilder = repositoryService
                .createDeployment()
                .category("servicetaks")
                .name("servicetaks")
                .addClasspathResource("process/服务任务.bpmn20.xml");
        Deployment deploy = deploymentBuilder.deploy();

        System.out.println("流程ID: " + deploy.getId());
    }

    /**
     * 启动流程实例
     */
    @Test
    public void start() {
        deploy();
        String processDefinitionKey = "servicetaks";
        Map<String, Object> variables = new HashMap<String, Object>();
        variables.put("van", "mj");
        runtimeService.startProcessInstanceByKey(processDefinitionKey, variables);
    }

    /**
     * 完成任务
     */
    @Test
    public void complete() {
        String taskId = "390025";
        taskService.complete(taskId);
    }
}

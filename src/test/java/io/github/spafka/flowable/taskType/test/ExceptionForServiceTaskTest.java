package io.github.spafka.flowable.taskType.test;


import io.github.spafka.flowable.service.FlowBase;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.DeploymentBuilder;


import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * 用户任务--测试
 */
public class ExceptionForServiceTaskTest extends FlowBase {

    /**
     * 部署
     */
    @Test
    public void deploy() {
        DeploymentBuilder deploymentBuilder = repositoryService
                .createDeployment()
                .category("servicetaskexception")
                .name("servicetaskexception")
                .addClasspathResource("process/服务任务异常处理.bpmn20.xml");
        Deployment deploy = deploymentBuilder.deploy();

        System.out.println("流程ID: " + deploy.getId());
    }

    /**
     * 启动流程实例
     */
    @Test
    public void start() {
        String processDefinitionKey = "servicetaskexception";
        Map<String, Object> variables = new HashMap<String, Object>();
        variables.put("localError", true);
        runtimeService.startProcessInstanceByKey(processDefinitionKey, variables);
    }

    /**
     * 完成任务
     */
    @Test
    public void complete() {
        String taskId = "67517";
        taskService.complete(taskId);
    }
}

package io.github.spafka.flowable.taskType.test;


import io.github.spafka.flowable.service.FlowBase;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.DeploymentBuilder;


import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * 业务规则任务--测试
 */
public class BusinessRuleTaskTest extends FlowBase {

    /**
     * 部署
     */
    @Test
    public void deploy() {
        DeploymentBuilder deploymentBuilder = repositoryService
                .createDeployment()
                .category("businessruleprocess")
                .name("businessruleprocess")
                .addClasspathResource("process/业务规则任务.bpmn20.xml");
        Deployment deploy = deploymentBuilder.deploy();

        System.out.println("流程ID: " + deploy.getId());
    }

    /**
     * 启动流程实例
     */
    @Test
    public void start() {
        String processDefinitionKey = "businessruleprocess";
        Map<String, Object> variables = new HashMap<String, Object>();
        runtimeService.startProcessInstanceByKey(processDefinitionKey, variables);
    }

    /**
     * 完成任务
     */
    @Test
    public void complete() {
        String taskId = "252512";
        taskService.complete(taskId);
    }
}

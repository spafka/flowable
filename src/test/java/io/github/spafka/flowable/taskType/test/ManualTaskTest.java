package io.github.spafka.flowable.taskType.test;


import io.github.spafka.flowable.service.FlowBase;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.DeploymentBuilder;


import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * 手动任务--测试
 */
public class ManualTaskTest extends FlowBase {

    /**
     * 部署
     */
    @Test
    public void deploy() {
        DeploymentBuilder deploymentBuilder = repositoryService
                .createDeployment()
                .category("manualtaskprocess")
                .name("manualtaskprocess")
                .addClasspathResource("process/手动任务.bpmn20.xml");
        Deployment deploy = deploymentBuilder.deploy();

        System.out.println("流程ID: " + deploy.getId());
    }

    /**
     * 启动流程实例
     */
    @Test
    public void start() {
        String processDefinitionKey = "manualtaskprocess";
        Map<String, Object> variables = new HashMap<String, Object>();
        runtimeService.startProcessInstanceByKey(processDefinitionKey, variables);
    }

    /**
     * 完成任务
     */
    @Test
    public void complete() {
        String taskId = "167506";
        taskService.complete(taskId);
    }
}

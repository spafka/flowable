package io.github.spafka.flowable.taskType.test;


import io.github.spafka.flowable.service.FlowBase;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.DeploymentBuilder;


import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * 任务监听器--测试
 */
public class TaskListenerTest extends FlowBase {

    /**
     * 部署
     */
    @Test
    public void deploy() {
        DeploymentBuilder deploymentBuilder = repositoryService
                .createDeployment()
                .category("tasklistenerprocess")
                .name("tasklistenerprocess")
                .addClasspathResource("process/任务监听器.bpmn20.xml");
        Deployment deploy = deploymentBuilder.deploy();

        System.out.println("流程ID: " + deploy.getId());
    }

    /**
     * 启动流程实例
     */
    @Test
    public void start() {
        String processDefinitionKey = "tasklistenerprocess";


        Map<String, Object> vars = new HashMap<String, Object>();
        vars.put("assgine", "wangwu");
        runtimeService.startProcessInstanceByKey(processDefinitionKey, vars);
    }

    /**
     * 完成任务
     */
    @Test
    public void complete() {
        String taskId = "110002";
        taskService.complete(taskId);
    }
}

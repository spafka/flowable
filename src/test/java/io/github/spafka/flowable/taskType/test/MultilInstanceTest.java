package io.github.spafka.flowable.taskType.test;


import io.github.spafka.flowable.service.FlowBase;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.DeploymentBuilder;


import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * 多实例测试--测试
 */
public class MultilInstanceTest extends FlowBase {

    /**
     * 部署
     */
    @Test
    public void deploy() {
        DeploymentBuilder deploymentBuilder = repositoryService
                .createDeployment()
                .category("mutiInstanceprocess")
                .name("mutiInstanceprocess")
                .addClasspathResource("process/多实例.bpmn20.xml");
        Deployment deploy = deploymentBuilder.deploy();

        System.out.println("流程ID: " + deploy.getId());
    }

    /**
     * 启动流程实例
     */
    @Test
    public void start() {
        String processDefinitionKey = "mutiInstanceprocess";


        Map<String, Object> vars = new HashMap<String, Object>();
        vars.put("assigneeList", Arrays.asList("zhangsan", "lisi", "wangwu"));
        runtimeService.startProcessInstanceByKey(processDefinitionKey, vars);
    }

    /**
     * 完成任务
     */
    @Test
    public void complete() {
        String taskId = "122525";
        Map<String, Object> vars = new HashMap<String, Object>();
        vars.put("isComplete", "true");
        taskService.complete(taskId, vars);
    }
}

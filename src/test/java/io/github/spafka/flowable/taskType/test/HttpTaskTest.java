package io.github.spafka.flowable.taskType.test;


import io.github.spafka.flowable.service.FlowBase;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.DeploymentBuilder;


import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Http任务--测试
 */
public class HttpTaskTest extends FlowBase {

    /**
     * 部署
     */
    @Test
    public void deploy() {
        DeploymentBuilder deploymentBuilder = repositoryService
                .createDeployment()
                .category("httptaskprocess")
                .name("httptaskprocess")
                .addClasspathResource("process/Http任务.bpmn20.xml");
        Deployment deploy = deploymentBuilder.deploy();

        System.out.println("流程ID: " + deploy.getId());
    }

    /**
     * 启动流程实例
     */
    @Test
    public void start() {
        String processDefinitionKey = "httptaskprocess";


        Map<String, Object> vars = new HashMap<String, Object>();
        vars.put("requestTimeout", 6000);

        runtimeService.startProcessInstanceByKey(processDefinitionKey, vars);
    }

    /**
     * 完成任务
     */
    @Test
    public void complete() {

        deploy();
        start();
        taskService.complete( listall().get(0).getId());
        assert  listall().isEmpty();
    }
}

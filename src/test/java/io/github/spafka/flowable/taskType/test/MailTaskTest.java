package io.github.spafka.flowable.taskType.test;


import io.github.spafka.flowable.service.FlowBase;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.DeploymentBuilder;


import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * 邮件任务--测试
 */
public class MailTaskTest extends FlowBase {

    /**
     * 部署
     */
    @Test
    public void deploy() {
        DeploymentBuilder deploymentBuilder = repositoryService
                .createDeployment()
                .category("emailtaskprocess")
                .name("emailtaskprocess")
                .addClasspathResource("process/邮件任务.bpmn20.xml");
        Deployment deploy = deploymentBuilder.deploy();

        System.out.println("流程ID: " + deploy.getId());
    }

    /**
     * 启动流程实例
     */
    @Test
    public void start() {
        String processDefinitionKey = "emailtaskprocess";

        String recipient = "465243573@qq.com";
        String recipientName = "JamesYee";
        String subject = "flowable邮件服务测试";

        Map<String, Object> vars = new HashMap<String, Object>();
        vars.put("recipient", recipient);
        vars.put("recipientName", recipientName);
        vars.put("subject", subject);

        vars.put("gender", "male");
        vars.put("html", "<html><body>Hello ${gender == 'male' ? 'Mr' : 'Ms' }. <b>JamesYee</b><body></html>");

        runtimeService.startProcessInstanceByKey(processDefinitionKey, vars);
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

package io.github.spafka.flowable.taskType.test;


import io.github.spafka.flowable.service.FlowBase;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.DeploymentBuilder;
import org.junit.jupiter.api.Test;


import javax.mail.Flags;
import java.util.HashMap;
import java.util.Map;


/**
 * 补偿处理器--测试
 */
public class CompensationHandlerTest extends FlowBase {

    /**
     * 部署
     */
    @Test
    public void deploy() {
        DeploymentBuilder deploymentBuilder = repositoryService
                .createDeployment()
                .category("compensationHandlerprocess")
                .name("compensationHandlerprocess")
                .addClasspathResource("process/补偿处理器.bpmn20.xml");
        Deployment deploy = deploymentBuilder.deploy();

        System.out.println("流程ID: " + deploy.getId());
    }

    /**
     * 启动流程实例
     */
    @Test
    public void start() {
        String processDefinitionKey = "compensationHandlerprocess";


        Map<String, Object> vars = new HashMap<String, Object>();
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

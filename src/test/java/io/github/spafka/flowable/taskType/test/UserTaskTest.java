package io.github.spafka.flowable.taskType.test;


import io.github.spafka.flowable.service.FlowBase;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.DeploymentBuilder;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户任务--测试
 */
public class UserTaskTest extends FlowBase {

    /**
     * 部署
     */
    @Test
    public void deploy() {
        DeploymentBuilder deploymentBuilder = repositoryService
                .createDeployment()
                .category("usertaskprocess")
                .name("usertaskprocess")
                .addClasspathResource("process/用户任务.bpmn20.xml");
        Deployment deploy = deploymentBuilder.deploy();

        System.out.println("流程ID: " + deploy.getId());
    }

    /**
     * 启动流程实例
     */
    @Test
    public void start() {
        String processDefinitionKey = "usertaskprocess";
        Map<String, Object> variables = new HashMap<String, Object>();
        variables.put("emp", "test");
        runtimeService.startProcessInstanceByKey(processDefinitionKey, variables);
    }

    /**
     * 完成任务
     */
    @Test
    public void complete() {
        //查询个人任务
        String user = "test.org";
        List<Task> tasks = new ArrayList<Task>();
        // 查询个人任务
        List<Task> assigneeTasks = taskService.createTaskQuery().taskAssignee(user).list();
        tasks.addAll(assigneeTasks);

        // 查询组任务
        List<Task> groupTasks = taskService.createTaskQuery().taskCandidateUser(user).list();
        tasks.addAll(groupTasks);
        for (Task task : tasks) {
            // 完成任务
            System.out.println("分配人: " + task.getAssignee());
            System.out.println("id: " + task.getId());
            taskService.complete(task.getId());
        }
    }
}

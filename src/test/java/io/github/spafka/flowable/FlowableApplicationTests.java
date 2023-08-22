package io.github.spafka.flowable;

import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootTest
class FlowableApplicationTests {
    private static final String key = "holidayRequest";

    @Autowired
    DataSource dataSource;

    @Autowired
    ProcessEngine processEngine;
    @Autowired
    RepositoryService repositoryService;
    @Autowired
    TaskService taskService;
    @Autowired
    RuntimeService runtimeService;


    @Test
    public void deploy() {
        Deployment deployment = repositoryService.createDeployment()// 创建Deployment对象
                .addClasspathResource("bpmn/holiday-request.bpmn20.xml") // 添加流程部署文件
                .name("suposHoliday")
                .key("supos")
                .deploy(); // 执行部署操作
        System.out.println("deployment.getId() = " + deployment.getId());
        System.out.println("deployment.getName() = " + deployment.getName());

        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().deploymentId(deployment.getId()).singleResult();

        System.out.println("processDefinition = " + processDefinition);
    }

    @Test
    public void list() {
        List<ProcessDefinition> list = repositoryService
                .createProcessDefinitionQuery()
                .list();

        System.out.println("list = " + list);
    }

    @Test
    public void preStart() {

        ProcessDefinition processDefinition = repositoryService
                .createProcessDefinitionQuery()
                .processDefinitionName(key)
                .latestVersion()
                .singleResult();

        Map<String, Object> variables = new HashMap<>();
        variables.put("employee", "张三");// 谁申请请假
        variables.put("nrOfHolidays", 3); // 请几天假
        variables.put("description", "工作累了，想出去玩玩"); // 请假的原因
        // 启动流程实例，第一个参数是流程定义的id
        ProcessInstance processInstance = runtimeService
                .startProcessInstanceByKey(key, variables);// 启动流程实例
        // 输出相关的流程实例信息
        System.out.println("流程定义的ID：" + processInstance.getProcessDefinitionId());
        System.out.println("流程实例的ID：" + processInstance.getId());
        System.out.println("当前活动的ID：" + processInstance.getActivityId());


        System.out.println("processDefinition = " + processDefinition);

    }


    @Test
    public void queryMy() {

        List<Task> list = taskService.createTaskQuery()
                .taskCandidateOrAssigned("1")
                .list();

        System.out.println("list = " + list);

    }


}

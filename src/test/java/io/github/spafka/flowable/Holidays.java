package io.github.spafka.flowable;

import io.github.spafka.flowable.core.FlowService;
import liquibase.pro.packaged.F;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.constants.BpmnXMLConstants;
import org.flowable.bpmn.model.UserTask;
import org.flowable.common.engine.impl.util.IoUtil;
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
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.io.*;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@SpringBootTest
class Holidays {
    private static final String key = "holiday";

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
    @Autowired
    FlowService flowService;


    @Test
    public void deploy() {
        Deployment deployment = repositoryService.createDeployment()// 创建Deployment对象
                .addClasspathResource("bpmn/holiday.bpmn20.xml") // 添加流程部署文件
                .name("holiday")
                .key("holiday")
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
    public void submit() {

        ProcessDefinition processDefinition = repositoryService
                .createProcessDefinitionQuery()
                .processDefinitionName(key)
                .latestVersion()
                .singleResult();


        Map<String, Object> variables = new HashMap<>();

        variables.put("days", 3); // 请几天假
        variables.put(BpmnXMLConstants.ATTRIBUTE_EVENT_START_INITIATOR, "whf");

        // 启动流程实例，第一个参数是流程定义的id
        ProcessInstance processInstance = runtimeService
                .startProcessInstanceByKey(key, variables);// 启动流程实例
        // 输出相关的流程实例信息
        System.out.println("流程定义的ID：" + processInstance.getProcessDefinitionId());
        System.out.println("流程实例的ID：" + processInstance.getId());
        System.out.println("当前活动的ID：" + processInstance.getActivityId());


        System.out.println("processDefinition = " + processDefinition);

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getProcessInstanceId()).singleResult();
        if (Objects.nonNull(task)) {
            String userIdStr = (String) variables.get(BpmnXMLConstants.ATTRIBUTE_EVENT_START_INITIATOR);
            if (StringUtils.equals(task.getAssignee(), userIdStr)) {
                taskService.complete(task.getId(), variables);
            }
        }

    }


    @Test
    public void complete() {

        List<Task> list = taskService.createTaskQuery()
                .taskAssignee("lisi")
                .list();

        for (Task task : list) {
            taskService.complete(task.getId(), taskService.getVariables(task.getId()));
        }

        List<Task> wangwu = taskService.createTaskQuery()
                .taskAssignee("wangwu")
                .list();

        for (Task task : wangwu) {
            taskService.complete(task.getId(), taskService.getVariables(task.getId()));
        }

        System.out.println("list = " + list);

    }

    @Test
    public void queryall() {
        List<Task> all = taskService.createTaskQuery()
                .list();

        System.out.println(all);
    }

    @Test
    public void findCanBack() throws IOException {
        List<Task> all = taskService.createTaskQuery()
                .list();

        for (Task task : all) {
            List<UserTask> returnTaskList = flowService.findReturnTaskList(task.getId());
            System.out.println();
            String processInstanceId = task.getProcessInstanceId();
            InputStream diagram = flowService.diagram(processInstanceId);
            IOUtils.copy(diagram, Files.newOutputStream(new File(task.getName() + ".png").toPath()));
        }

        System.out.println(all);
    }


}

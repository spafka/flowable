package io.github.spafka.flowable.multiinstance;


import io.github.spafka.flowable.core.FlowService;
import io.github.spafka.flowable.service.FlowBase;
import io.github.spafka.flowable.service.ReturnService;
import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.constants.BpmnXMLConstants;
import org.flowable.engine.*;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.util.*;

@SpringBootTest

public class multiInstanceBackTests extends FlowBase {

    private static final String key = "multiInstanceBack";

    @Autowired
    DataSource dataSource;
    @Resource
    protected HistoryService historyService;
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

    String processName = "多实例驳回T2-T1";

    static int i = 0;

    String processId = null;
    @Autowired
    ReturnService returnService;

    @Test
    public void deploy() {
        Deployment deployment = repositoryService.createDeployment()
                .addClasspathResource(String.format("process/%s.bpmn20.xml", processName))
                .name(processName)
                .key(key)
                .deploy(); // 执行部署操作
        System.out.println("deployment.getId() = " + deployment.getId());
        System.out.println("deployment.getName() = " + deployment.getName());

        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().deploymentId(deployment.getId()).singleResult();

        System.out.println("processDefinition = " + processDefinition);
    }

    @Test
    public void submit() {


        ProcessDefinition processDefinition = repositoryService
                .createProcessDefinitionQuery()
                .processDefinitionName(processName)
                .latestVersion()
                .singleResult();


        Map<String, Object> variables = new HashMap<>();
        variables.put("days", 3);
        variables.put("initiator", "whf");

        variables.put("status", "approve");
        variables.put(BpmnXMLConstants.ATTRIBUTE_EVENT_START_INITIATOR, "whf");


        ProcessInstance processInstance = runtimeService
                .startProcessInstanceByKey(processDefinition.getKey(), variables);

        processId = processInstance.getId();


    }


    @Test
    public void trace() {

        listCanRetuen("T6");
        System.out.println();
    }


    @Test
    public void okshould() {


        assert listall().isEmpty();

    }

    @Test
    public void okshould_case1() {
        deploy();
        submit();
        show(processId);


        Task task = taskService.createTaskQuery().processInstanceId(processId).singleResult();
        Map<String, Object> variables = new HashMap<>();
        variables.put("days", 3);
        variables.put("initiator", "whf");

        // 设置多人会签的数据
        variables.put("persons", Arrays.asList("zs", "ls", "ww", "zl"));
        taskService.complete(task.getId(), variables);

        List<Task> listall = listall(processId);

        complete("zs");
        show(processId);
        complete("ls");
        show(processId);
        complete("ww");
        show(processId);
        complete("whf");
        show(processId);
        complete("whf");

        System.out.println();
    }

}

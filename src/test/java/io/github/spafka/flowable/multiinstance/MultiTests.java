package io.github.spafka.flowable.multiinstance;

import io.github.spafka.flowable.core.FlowService;
import io.github.spafka.flowable.service.FlowBase;
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

/**
 * @link {{src/main/resources/returntest/复杂并行网关.bpmn20.xml}}
 */
@SpringBootTest

public class MultiTests extends FlowBase {

    private static final String key = "myProcess";

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

    String processName = "会签2";

    static int i = 0;

    @Test
    public void deploy() {
        Deployment deployment = repositoryService.createDeployment()
                .addClasspathResource("process/会签2.bpmn20.xml")
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
        variables.put("_FLOWABLE_SKIP_EXPRESSION_ENABLED", true);
        // 设置多人会签的数据
        variables.put("persons", Arrays.asList("zs", "ls", "ww"));


        variables.put("status", "approve");
        variables.put(BpmnXMLConstants.ATTRIBUTE_EVENT_START_INITIATOR, "whf");


        ProcessInstance processInstance = runtimeService
                .startProcessInstanceByKey(processDefinition.getKey(), variables);

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getProcessInstanceId()).singleResult();
        if (Objects.nonNull(task)) {
            String userIdStr = (String) variables.get(BpmnXMLConstants.ATTRIBUTE_EVENT_START_INITIATOR);
            if (StringUtils.equals(task.getAssignee(), userIdStr)) {
                taskService.complete(task.getId(), variables);
            }
        }


    }

    public void completeOK(String user) {

        List<Task> list = taskService.createTaskQuery()
                .taskAssignee(user)
                .list();
        Task task1 = list.get(0);
        Map<String, Object> map = new HashMap<>();

        map.put("circulationConditions", "Y");
        map.put("flag", true);
        taskService.complete(task1.getId(), map);


    }
    public void completeNOK(String user) {

        List<Task> list = taskService.createTaskQuery()
                .taskAssignee(user)
                .list();
        Task task1 = list.get(0);
        Map<String, Object> map = new HashMap<>();

        map.put("circulationConditions", "N");

        taskService.complete(task1.getId(), map);


    }



    @Test
    public void startTasks() {

        deploy();
        submit();
        listall();

        List<Task> list = taskService.createTaskQuery()
                .list();
        Task task1 = list.get(0);
        Map<String, Object> map = new HashMap<>();

        map.put("circulationConditions", "Y");
        map.put("flag", false);
        taskService.complete(task1.getId(),map);
        show();

        task1 = list.get(1);

        map.put("circulationConditions", "N");
        map.put("flag", false);
        taskService.complete(task1.getId(),map);

        list = taskService.createTaskQuery()
                .list();

        System.out.println();



    }


}

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

    String processName = "会签";

    static int i = 0;

    @Test
    public void deploy() {
        Deployment deployment = repositoryService.createDeployment()
                .addClasspathResource("会签.bpmn20.xml")
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

        // 设置多人会签的数据
        variables.put("persons", Arrays.asList("张三", "李四", "王五", "赵六"));


        variables.put("status", "approve");
        variables.put(BpmnXMLConstants.ATTRIBUTE_EVENT_START_INITIATOR, "whf");


        ProcessInstance processInstance = runtimeService
                .startProcessInstanceByKey(processDefinition.getKey(), variables);


    }


    @Test
    public void startTasks() {

        deploy();
        submit();
        listall();

        List<Task> listall = listall();
        listall.stream().skip(2).forEach(x -> {
            Map<String, Object> map = new HashMap<>();

            map.put("flag", false);

            taskService.complete(x.getId(), map);
            System.out.println("complete ....");
        });

        Task task = listall.get(0);
        Map<String, Object> map = new HashMap<>();
        map.put("flag", true);
        taskService.complete(task.getId(), map);
        System.out.println("complete ....");



    }


}

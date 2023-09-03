package io.github.spafka.flowable.returnTests;

import io.github.spafka.flowable.FlowBase;
import io.github.spafka.flowable.core.FlowService;
import io.github.spafka.flowable.core.TopologyNode;
import io.github.spafka.flowable.service.Graphs;
import io.github.spafka.tuple.Tuple2;
import io.github.spafka.util.JoinUtils;
import lombok.var;
import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.constants.BpmnXMLConstants;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.engine.*;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @link {{src/main/resources/returntest/复杂并行网关.bpmn20.xml}}
 */
@SpringBootTest
@RunWith(value = SpringRunner.class)
public class PallergateTests extends FlowBase {

    private static final String key = "pg01";

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

    String processName = "复杂并行网关";

    static int i = 0;

    @Test
    public void deploy() {
        Deployment deployment = repositoryService.createDeployment()
                .addClasspathResource("returntest/复杂并行网关.bpmn20.xml")
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

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getProcessInstanceId()).singleResult();
        if (Objects.nonNull(task)) {
            String userIdStr = (String) variables.get(BpmnXMLConstants.ATTRIBUTE_EVENT_START_INITIATOR);
            if (StringUtils.equals(task.getAssignee(), userIdStr)) {
                taskService.complete(task.getId(), variables);
            }
        }

    }


    @Test
    public void trace() {

        System.out.println();
    }


    @Test
    public void okshould() {
        deploy();
        submit();
        complete("whf", "T2");
        complete("whf", "T4");
        complete("whf", "T5");
        complete("whf", "T6");


        complete("whf", "T3");
        complete("whf", "T3-1");
        complete("whf", "T3-2");
        complete("whf", "T7");
        complete("whf", "T8");
        assert listall().isEmpty();

    }
    @Test
    public void okshould_case1() {
        deploy();
        submit();
        complete("whf", "T2");
        complete("whf", "T4");
        complete("whf", "T5");
        complete("whf", "T6");


        complete("whf", "T3");
        complete("whf", "T3-1");
        complete("whf", "T3-2");
        complete("whf", "T7");
        return2Node("T8","T7");
        complete("whf", "T7");
        complete("whf", "T8");
        assert listall().isEmpty();

    }

    @Test
    public void okshould_case2() {
        deploy();
        submit();
        complete("whf", "T2");
        complete("whf", "T4");
        complete("whf", "T5");
        complete("whf", "T6");


        complete("whf", "T3");
        complete("whf", "T3-1");
        complete("whf", "T3-2");
        complete("whf", "T7");
        return2Node("T8","T5");
        complete("whf", "T5");
        complete("whf", "T7");
        complete("whf", "T8");
        assert listall().isEmpty();

    }

    @Test
    public void okshould_case3() {
        deploy();
        submit();
        complete("whf", "T2");
        complete("whf", "T4");
        complete("whf", "T5");
        complete("whf", "T6");


        complete("whf", "T3");
        complete("whf", "T3-1");
        complete("whf", "T3-2");
        complete("whf", "T7");
        return2Node("T8","T4");
        complete("whf", "T4");
        complete("whf", "T5");
        complete("whf", "T6");
        complete("whf", "T7");
        complete("whf", "T8");
        assert listall().isEmpty();

    }
}

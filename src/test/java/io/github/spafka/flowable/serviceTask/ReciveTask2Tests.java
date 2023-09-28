package io.github.spafka.flowable.serviceTask;

import io.github.spafka.flowable.core.FlowService;
import io.github.spafka.flowable.service.FlowBase;
import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.constants.BpmnXMLConstants;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


/**
 * @see io.github.spafka.flowable.task.ServiceTaskDelegateExpression
 */
@SpringBootTest
public class ReciveTask2Tests extends FlowBase {

    private static final String key = "serviceTask";

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

    String processName = "服务任务";

    String processId = "";

    public void deploy() {
        Deployment deployment = repositoryService.createDeployment()
                .addClasspathResource("process/服务任务.bpmn20.xml")
                .name(processName)
                .key(key)
                .deploy(); // 执行部署操作
        System.out.println("deployment.getId() = " + deployment.getId());
        System.out.println("deployment.getName() = " + deployment.getName());

        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().deploymentId(deployment.getId()).singleResult();
        System.out.println("processDefinition = " + processDefinition);
    }


    public void submit() {


        ProcessDefinition processDefinition = repositoryService
                .createProcessDefinitionQuery()
                .processDefinitionName(processName)
                .latestVersion()
                .singleResult();


        Map<String, Object> variables = new HashMap<>();
        variables.put("days", 3);
        variables.put("a", 99);
        variables.put("b", 98);
        variables.put(BpmnXMLConstants.ATTRIBUTE_EVENT_START_INITIATOR, "whf");

        variables.put("status", "approve");
        variables.put(BpmnXMLConstants.ATTRIBUTE_EVENT_START_INITIATOR.toUpperCase(), "whf");


        ProcessInstance processInstance = runtimeService
                .startProcessInstanceByKey(processDefinition.getKey(), variables);
        processId = processInstance.getId();

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

        deploy();
        submit();
        listall(processId);

        show(processId);
        complete("whf"); //T1
        List<Execution> list = runtimeService.createExecutionQuery()
                .processInstanceId(processId)
                .activityId("recv")
                .list();
        String executionId = list.get(0).getId();
        show(processId);
        runtimeService.trigger(executionId);
        show(processId);

        System.out.println();
    }


    @Test
    @DisplayName("完整走完流程")
    public void okshould() {
        deploy();
        submit();


        // ScriptTaskActivityBehavior
    }

}

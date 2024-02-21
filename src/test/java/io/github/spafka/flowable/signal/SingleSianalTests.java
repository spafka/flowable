package io.github.spafka.flowable.signal;

import io.github.spafka.flowable.core.FlowService;
import io.github.spafka.flowable.service.FlowBase;
import org.flowable.bpmn.constants.BpmnXMLConstants;
import org.flowable.engine.*;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * @link {{src/main/resources/returntest/复杂并行网关.bpmn20.xml}}
 */
@SpringBootTest

public class SingleSianalTests extends FlowBase {

    private static final String key = "K4187597187930914";

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

    String processName = "回归测试";

    static int i = 0;

    @Test
    public void deploy() {
        Deployment deployment = repositoryService.createDeployment()
                .addClasspathResource("signal/K4187597187930914.bpmn20.xml")
                .name(processName)
                .key(key)
                .disableSchemaValidation()
                .disableBpmnValidation()
                .deploy(); // 执行部署操作
        System.out.println("deployment.getId() = " + deployment.getId());
        System.out.println("deployment.getName() = " + deployment.getName());

        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().deploymentId(deployment.getId()).singleResult();

        System.out.println("processDefinition = " + processDefinition);
    }

    @Test
    public void submit() {

        deploy();


        ProcessDefinition processDefinition = repositoryService
                .createProcessDefinitionQuery()
                .processDefinitionName(processName)
                .latestVersion()
                .singleResult();


        Map<String, Object> variables = new HashMap<>();

        variables.put(BpmnXMLConstants.ATTRIBUTE_EVENT_START_INITIATOR, "whf");
        variables.put("persons", Arrays.asList("zs", "ls", "ww"));


        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionId(processDefinition.getId())

                .variables(variables)
                .predefineProcessInstanceId(System.currentTimeMillis() + "")
                .start();

        complete(null);

        LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(15));

        complete(null);

        TaskQuery taskQuery = taskService.createTaskQuery();

        List<Task> list = taskQuery
                .list();
        Task task1 = list.get(1);
        Map<String, Object> variables1 = taskService.getVariables(task1.getId());
        variables1.put("audit", true);
        taskService.complete(task1.getId(), variables1);

        System.out.println(processInstance.getProcessInstanceId());

        show(processInstance.getId());

        LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(20));

        System.out.println();


    }


}

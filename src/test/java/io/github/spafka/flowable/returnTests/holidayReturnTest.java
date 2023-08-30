package io.github.spafka.flowable.returnTests;

import io.github.spafka.flowable.FlowBase;
import io.github.spafka.flowable.core.FlowService;
import io.github.spafka.flowable.service.FlowNodeDto;
import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.constants.BpmnXMLConstants;
import org.flowable.engine.*;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@SpringBootTest
public class holidayReturnTest extends FlowBase {

    private static final String key = "LeaveApplication";

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

    String processName = "请假申请";

    static int i = 0;

    @Test
    public void deploy() {
        Deployment deployment = repositoryService.createDeployment()
                .addClasspathResource("returntest/请假申请.bpmn20.xml")
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

        list();

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
    public void debug() {
        List<Task> all = taskService.createTaskQuery()
                .processDefinitionName(processName)
                .list();

        System.out.println(all);
    }

    @Test
    public void back() {

        diagram(repositoryService, processName);

    }


    public void _1(String processInstanceId) {

        List<HistoricActivityInstance> historicActivityInstances = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId)
                .orderByHistoricActivityInstanceStartTime()
                .asc()
                .list();

        // 确定要回退到的目标节点
        HistoricActivityInstance targetNode = null;
        for (HistoricActivityInstance historicActivityInstance : historicActivityInstances) {
            if (historicActivityInstance.getActivityId().equals(targetNode)) {
                targetNode = historicActivityInstance;
                break;
            }
        }
    }

    public List<FlowNodeDto> listCanRetuen(Task task) {
        if (task == null) {
            List<Task> all = taskService.createTaskQuery()
                    .processDefinitionName(processName)
                    .list();
            task = all.get(0);
        }
        List<FlowNodeDto> backNodes = flowService.getBackNodes(task.getId());

        backNodes.forEach(x -> {
            System.out.printf("can back %s %s %s", x.getId(), x.getName(), "\n");
        });

        return backNodes;
    }

    public void return2Node(String to) {
        Task task = null;
        List<Task> all = taskService.createTaskQuery()
                .processDefinitionName(processName)
                .list();
        task = all.get(0);
        flowService.backTask(task.getId(), to);

    }


    @Test
    public void trace() {
        System.out.println();
    }
}

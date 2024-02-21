package io.github.spafka.flowable.service;

import io.github.spafka.flowable.core.FlowService;
import io.github.spafka.flowable.core.JoinUtils;
import io.github.spafka.flowable.service.BpmnService;
import io.github.spafka.flowable.service.FlowNodeDto;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.constants.BpmnXMLConstants;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.SubProcess;
import org.flowable.engine.*;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskInfo;
import org.flowable.task.api.TaskQuery;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SpringBootTest
public class FlowBase {

    static int i = 0;

    public String processName = "s";

    @Autowired
    public RuntimeService runtimeService;
    @Autowired
    public TaskService taskService;
    @Autowired
    public HistoryService historyService;
    @Autowired
    public FlowService flowService;
    @Autowired
    public BpmnService bpmnService;
    @Autowired
    public DataSource dataSource;
    @Autowired
    public ProcessEngine processEngine;
    @Autowired
    public RepositoryService repositoryService;

    public String processInstanceId;

    public ProcessDefinition processDefinition;


    public void diagram(RepositoryService repositoryService, String processName) {


        List<ProcessDefinition> all = repositoryService.createProcessDefinitionQuery()
                .processDefinitionName(processName)
                .list();
        BpmnModel bpmnModel = repositoryService.getBpmnModel(all.get(0).getId());

        Process process = bpmnModel.getProcesses().get(0);

        Collection<FlowElement> flowElements = process.getFlowElements().stream().flatMap(x -> {
            if (x instanceof SubProcess) {
                Collection<FlowElement> sbs = ((SubProcess) x).getFlowElements();
                return Stream.concat(Stream.of(x), sbs.stream());
            } else {
                return Stream.of(x);
            }
        }).collect(Collectors.toList());

    }

    public void debug() {
        List<org.flowable.task.api.Task> all = taskService.createTaskQuery()
                .list();
        Task task = all.get(0);
        String processInstanceId = task.getProcessInstanceId();
        show();

        all.forEach(x -> System.out.println(String.format("Task[id=%s,name=%s,assignee=%s]", x.getId(), x.getName(), x.getAssignee())));
    }

    public List<Task> listall(String... processId) {
        return taskService.createTaskQuery()
                .processInstanceId(processId.length > 0 ? processId[0] : null)
                .list();

    }

    public void show(String... processId) {

        String processInstanceId;
        processInstanceId = ArrayUtils.isEmpty(processId) ? this.processInstanceId : processId[0];
        InputStream diagram = flowService.diagram(processInstanceId);
        try {
            OutputStream output = Files.newOutputStream(new File(processName + i++ + ".png").toPath());
            IOUtils.copy(diagram, output);
            output.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void complete(String user) {

        TaskQuery taskQuery = taskService.createTaskQuery();
        if (user != null) {
            taskQuery.taskAssignee(user);
        }
        List<Task> list = taskQuery
                .list();
        Task task1 = list.get(0);
        taskService.complete(task1.getId(), taskService.getVariables(task1.getId()));


    }

    public void complete(String user, String taskName) {

        List<Task> list = taskService.createTaskQuery()
                .taskAssignee(user)
                .list();
        list.stream().filter(x -> x.getName().equals(taskName))
                .findFirst().ifPresentOrElse(x -> {
                    taskService.complete(x.getId(), taskService.getVariables(x.getId()));
                }, () -> {
                    throw new RuntimeException("待办任务不存在 " + taskName);
                });


    }

    public List<FlowNodeDto> listCanRetuen(String taskName) {
        Task task;
        List<Task> all = taskService.createTaskQuery().list();

        task = all.stream().filter(x -> x.getName().equals(taskName) || Objects.isNull(taskName) || x.getTaskDefinitionKey().equals(taskName)).findFirst().get();
        List<HistoricTaskInstance> list = historyService.createHistoricTaskInstanceQuery().processInstanceId(task.getProcessInstanceId()).list();
        List<FlowNodeDto> backNodes = flowService.getBackNodes(task.getId());

        return JoinUtils.sortJoin(backNodes,
                list,
                FlowNodeDto::getId,
                TaskInfo::getTaskDefinitionKey,
                (a, b) -> a);
    }

    public void return2Node(String from, String... to) {
        Task task;
        List<Task> all = taskService.createTaskQuery()
                .list();
        task = all.stream().filter(x -> x.getName().equals(from)).findFirst().get();
        flowService.backTask(task.getId(), to);


    }


    public void deploy(String key, String processName, String xml) {
        Deployment deployment = repositoryService.createDeployment()
                .addClasspathResource(xml)
                .name(processName)
                .key(key)
                .deploy(); // 执行部署操作
        System.out.println("deployment.getId() = " + deployment.getId());
        System.out.println("deployment.getName() = " + deployment.getName());

        processDefinition = repositoryService.createProcessDefinitionQuery().deploymentId(deployment.getId()).singleResult();

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
        variables.put(BpmnXMLConstants.ATTRIBUTE_EVENT_START_INITIATOR, "whf");

        variables.put("INITIATOR", "whf");

        ProcessInstance processInstance = runtimeService
                .startProcessInstanceByKey(processDefinition.getKey(), variables);

        processInstanceId = processInstance.getProcessInstanceId();
        Task task = taskService.createTaskQuery().processInstanceId(processInstanceId).singleResult();
        if (Objects.nonNull(task)) {
            String userIdStr = (String) variables.get(BpmnXMLConstants.ATTRIBUTE_EVENT_START_INITIATOR);
            if (StringUtils.equals(task.getAssignee(), userIdStr)) {
                taskService.complete(task.getId(), variables);
            }
        }

    }
}

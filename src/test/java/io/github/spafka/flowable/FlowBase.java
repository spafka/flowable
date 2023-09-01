package io.github.spafka.flowable;

import io.github.spafka.flowable.core.FlowService;
import io.github.spafka.flowable.service.FlowNodeDto;
import org.apache.commons.io.IOUtils;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.*;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FlowBase {

    int i = 0;

    String processName = "s";

    @Autowired
    RuntimeService runtimeService;
    @Autowired
    TaskService taskService;
    @Autowired
    HistoryService historyService;
    @Autowired
    FlowService flowService;


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

    public void show() {

        String processInstanceId = null;
        List<Task> all = taskService.createTaskQuery()
                .list();
        processInstanceId = all.get(0).getProcessInstanceId();
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

        List<Task> list = taskService.createTaskQuery()
                .taskAssignee(user)
                .list();
        Task task1 = list.get(0);
        taskService.complete(task1.getId(), taskService.getVariables(task1.getId()));


    }

    public void complete(String user, String taskName) {

        List<Task> list = taskService.createTaskQuery()
                .taskAssignee(user)
                .list();
        list.stream().filter(x -> x.getName().equals(taskName)).forEach(x -> {
            taskService.complete(x.getId(), taskService.getVariables(x.getId()));

        });


    }

    public List<FlowNodeDto> listCanRetuen(String taskName) {
        Task task;

        List<Task> all = taskService.createTaskQuery()

                .list();
        task = all.stream().filter(x -> x.getName().equals(taskName) || Objects.isNull(taskName)).findFirst().get();

        List<FlowNodeDto> backNodes = flowService.getBackNodes(task.getId());

        backNodes.forEach(x -> {
            System.out.printf("can back %s %s %s", x.getId(), x.getName(), "\n");
        });

        return backNodes;
    }

    public void return2Node(String to) {
        Task task = null;
        List<Task> all = taskService.createTaskQuery()
                .list();
        task = all.get(0);
        flowService.backTask(task.getId(), to);

    }
}

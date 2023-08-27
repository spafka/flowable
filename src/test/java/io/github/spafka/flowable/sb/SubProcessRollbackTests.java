package io.github.spafka.flowable.sb;

import io.github.spafka.flowable.TopologyNode;
import io.github.spafka.flowable.core.FlowService;
import io.github.spafka.flowable.core.FlowableUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.constants.BpmnXMLConstants;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.*;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SpringBootTest
public class SubProcessRollbackTests {
    private static final String key = "subProcess";

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

    String processName = "嵌套子流程";

    static int i = 0;


    @Test //error
    public void _1() {
        deploy();
        submit();

        System.out.println();

    }

    @Test //error
    public void _2() {
        deploy();
        submit();
        complete("ls");
        complete("ww");
        complete("ls");
        complete("ww");

        show(null);
        taskReturn(null, "initiator1");
        complete("whf");
        complete("ls");
        complete("ww");
        complete("ww");
        complete("ls");

        show(null);

        debug();
        complete("whf");

        complete("ls");
        complete("ww");

        show(null);

        complete("zl");
        System.out.println();

    }


    @Test
    public void deploy() {
        Deployment deployment = repositoryService.createDeployment()// 创建Deployment对象
                .addClasspathResource("bpmn/subProcessRuo.bpmn20.xml") // 添加流程部署文件
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

        variables.put("days", 3); // 请几天假
        variables.put(BpmnXMLConstants.ATTRIBUTE_EVENT_START_INITIATOR, "whf");

        // 启动流程实例，第一个参数是流程定义的id
        ProcessInstance processInstance = runtimeService
                .startProcessInstanceByKey(processDefinition.getKey(), variables);// 启动流程实例
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


    public void complete(String user) {

        List<Task> list = taskService.createTaskQuery()
                .taskAssignee(user)
                .processDefinitionName(processName)
                .list();
        Task task1 = list.get(0);
        taskService.complete(task1.getId(), taskService.getVariables(task1.getId()));


        System.out.println("list = " + list);

    }


    @Test
    public void list() {
        List<Task> all = taskService.createTaskQuery()
                .processDefinitionName(processName)
                .list();
        Task task = all.get(0);
        String processInstanceId = task.getProcessInstanceId();
        show(processInstanceId);


        List<HistoricActivityInstance> historicActivityInstances = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(all.get(0).getProcessInstanceId())
                .orderByHistoricActivityInstanceStartTime()
                .asc()
                .list();

        all.forEach(x -> {
            System.out.printf("Task[id= %s , name= %s user=%s", x.getId(), x.getName(), x.getAssignee());
        });


        System.out.println(all);
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
        List<ProcessDefinition> all = repositoryService.createProcessDefinitionQuery()
                .processDefinitionName(processName)
                .list();
        BpmnModel bpmnModel = repositoryService.getBpmnModel(all.get(0).getId());

        Process process = bpmnModel.getProcesses().get(0);

        Collection<FlowElement> flowElements = process.getFlowElements().stream().flatMap(x -> {
            if (x instanceof SubProcess) {
                Collection<FlowElement> flowElements1 = ((SubProcess) x).getFlowElements();
                return Stream.concat(Stream.of(x), flowElements1.stream());
            }else {
                return Stream.of(x);
            }
        }).collect(Collectors.toList());


        Map<String, FlowElement> idMap = flowElements.stream().collect(Collectors.toMap(BaseElement::getId, x -> x));


        FlowElement start = flowElements.stream().filter(x -> x instanceof StartEvent).findFirst().get();
        TopologyNode<FlowElement> root = new TopologyNode<>(start);

        Map<String, TopologyNode<FlowElement>> cache = new HashMap<>();
        cache.put(start.getId(), root);

        flowElements.forEach(x -> {
            if (x instanceof SequenceFlow) {
                TopologyNode<FlowElement> source = cache.computeIfAbsent(((SequenceFlow) x).getSourceRef(), s -> new TopologyNode<>(idMap.get(s)));
                TopologyNode<FlowElement> target = cache.computeIfAbsent(((SequenceFlow) x).getTargetRef(), s -> new TopologyNode<>(idMap.get(s)));
                TopologyNode thiS = cache.computeIfAbsent(x.getId(), s -> new TopologyNode<>(idMap.get(s)));
                thiS.pre.add(source);
                source.next.add(target);
                target.pre.add(thiS);
                thiS.next.add(target);
            } else {

            }
        });
//
//        List<HistoricActivityInstance> historicActivityInstances = historyService.createHistoricActivityInstanceQuery()
//                .processInstanceId(all.get(0).getProcessInstanceId())
//                .orderByHistoricActivityInstanceStartTime()
//                .asc()
//                .list();
//
//        historicActivityInstances.forEach(x -> {
//            cache.get(x.getActivityId()).ts = x.getEndTime() == null ? 0 : x.getEndTime().getTime();
//        });

        System.out.println();
    }


    public void show(String processInstanceId) {
        if (processInstanceId != null) {
            InputStream diagram = flowService.diagram(processInstanceId);
            try {
                OutputStream output = Files.newOutputStream(new File(processName + i++ + ".png").toPath());
                IOUtils.copy(diagram, output);
                output.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            List<Task> all = taskService.createTaskQuery()
                    .processDefinitionName(processName)
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


    }

    public void showDiagram(String processInstanceId) {
        InputStream diagram = flowService.diagram(processInstanceId);
        try {
            OutputStream output = Files.newOutputStream(new File(processName + (i++) + ".png").toPath());
            IOUtils.copy(diagram, output);
            output.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


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

    public void taskReturn(Task task, String toId) {
        if (task == null) {
            List<Task> all = taskService.createTaskQuery()
                    .processDefinitionName(processName)
                    .list();
            task = all.get(0);
        }
        // 当前任务 task
        if (Objects.isNull(task)) {
            throw new RuntimeException("获取任务信息异常！");
        }
        if (task.isSuspended()) {
            throw new RuntimeException("任务处于挂起状态");
        }
        // 获取流程定义信息
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionId(task.getProcessDefinitionId()).singleResult();
        // 获取所有节点信息
        Process process = repositoryService.getBpmnModel(processDefinition.getId()).getProcesses().get(0);
        // 获取全部节点列表，包含子节点
        Collection<FlowElement> allElements = FlowableUtils.getAllElements(process.getFlowElements(), null);
        // 获取当前任务节点元素
        FlowElement source = null;
        // 获取跳转的节点元素
        FlowElement target = null;

        if (allElements != null) {
            for (FlowElement flowElement : allElements) {
                // 当前任务节点元素
                if (flowElement.getId().equals(task.getTaskDefinitionKey())) {
                    source = flowElement;
                }
                // 跳转的节点元素
                if (flowElement.getId().equals(toId)) {
                    target = flowElement;
                }
            }
        }

        // 从当前节点向前扫描
        // 如果存在路线上不存在目标节点，说明目标节点是在网关上或非同一路线上，不可跳转
        // 否则目标节点相对于当前节点，属于串行
//        Boolean isSequential = FlowableUtils.iteratorCheckSequentialReferTarget(source, bo.getTargetKey(), null, null);
//        if (!isSequential) {
//            throw new RuntimeException("当前节点相对于目标节点，不属于串行关系，无法回退");
//        }


        // 获取所有正常进行的任务节点 Key，这些任务不能直接使用，需要找出其中需要撤回的任务
        List<Task> runTaskList = taskService.createTaskQuery().processInstanceId(task.getProcessInstanceId()).list();
        List<String> runTaskKeyList = new ArrayList<>();
        runTaskList.forEach(item -> runTaskKeyList.add(item.getTaskDefinitionKey()));
        // 需退回任务列表
        List<String> currentIds = new ArrayList<>();
        // 通过父级网关的出口连线，结合 runTaskList 比对，获取需要撤回的任务
        List<UserTask> currentUserTaskList = FlowableUtils.iteratorFindChildUserTasks(target, runTaskKeyList, null, null);
        currentUserTaskList.forEach(item -> currentIds.add(item.getId()));

        // 循环获取那些需要被撤回的节点的ID，用来设置驳回原因
        List<String> currentTaskIds = new ArrayList<>();
        currentIds.forEach(currentId -> runTaskList.forEach(runTask -> {
            if (currentId.equals(runTask.getTaskDefinitionKey())) {
                currentTaskIds.add(runTask.getId());
            }
        }));
        try {
            // 1 对 1 或 多 对 1 情况，currentIds 当前要跳转的节点列表(1或多)，targetKey 跳转到的节点(1)
            runtimeService.createChangeActivityStateBuilder()
                    .processInstanceId(task.getProcessInstanceId())
                    .moveActivityIdsToSingleActivityId(currentIds, toId).changeState();
        } catch (FlowableObjectNotFoundException e) {
            throw new RuntimeException("未找到流程实例，流程可能已发生变化");
        } catch (FlowableException e) {
            throw new RuntimeException("无法取消或开始活动");
        }

    }


}

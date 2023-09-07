package io.github.spafka.flowable.service;

import io.github.spafka.flowable.JumpTypeEnum;
import io.github.spafka.flowable.core.TopologyNode;
import io.github.spafka.flowable.service.impl.returns.SaveExecutionCmd;
import io.github.spafka.util.JoinUtils;
import io.vavr.Tuple3;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.flowable.bpmn.model.*;
import org.flowable.common.engine.impl.cfg.IdGenerator;
import org.flowable.engine.*;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityImpl;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityManager;
import org.flowable.engine.runtime.Execution;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskInfo;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Primary
@Slf4j
public class MainReturnService implements ReturnService {

    @Autowired
    private BpmnService bpmnService;
    @Autowired
    private TaskService taskService;
    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private ProcessEngineConfiguration processEngineConfiguration;
    @Autowired
    private ManagementService managementService;
    @Autowired
    JdbcTemplate jdbcTemplate;
    @Autowired
    HistoryService historyService;

    @Override
    public List<FlowNodeDto> getCanRejectedFlowNode(BpmnModel bpmnModel, String instanceId, String processInstanceId) {
        Task task = taskService.createTaskQuery().taskId(instanceId).singleResult();
        Tuple3<JumpTypeEnum, List<LinkedList<TopologyNode<FlowElement>>>, Set<FlowElement>> tuple3 = Graphs.backTrack(bpmnModel, task.getTaskDefinitionKey(), null);

        LinkedList<TopologyNode<FlowElement>> one = tuple3._2.get(0);


        one.removeFirst();
        TopologyNode last = one.getFirst();

        Deque<TopologyNode> deque = new LinkedList<>();
        Set<TopologyNode> processed = new LinkedHashSet<>();


        deque.add(last);
        processed.add(last);
        while (!deque.isEmpty()) {

            TopologyNode<FlowElement> poll = deque.pollFirst();

            TopologyNode<FlowElement>.SkipList<TopologyNode<FlowElement>> pre = poll.pre;

            pre.forEach(x -> {
                boolean contains = processed.contains(x);
                if (!contains) {
                    processed.add(x);
                    deque.addLast(x);
                }
            });

        }

        List<FlowNodeDto> backNodes = processed
                .stream()
                .filter(x -> x.node instanceof UserTask)
                .map(x -> new FlowNodeDto(x.node.getId(), x.node.getName()))
                .collect(Collectors.toList());


        List<HistoricTaskInstance> list = historyService.createHistoricTaskInstanceQuery().processInstanceId(task.getProcessInstanceId()).list();

        List<FlowNodeDto> flowNodeDtos = JoinUtils.sortInnerJoin(backNodes,
                list,
                Comparator.comparing(FlowNodeDto::getId),
                Comparator.comparing(TaskInfo::getTaskDefinitionKey)
                , (a, b) -> a.getId().compareTo(b.getTaskDefinitionKey()),
                (a, b) -> a);

        return flowNodeDtos;
    }

    @Override
    public boolean returnToTarget(String taskId, String targetId) {


        BpmnModel model = bpmnService.getBpmnModelByFlowableTaskId(taskId);
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();

        List<Execution> executions = runtimeService.createExecutionQuery().parentId(task.getProcessInstanceId()).list();

        var tuple = Graphs.backTrack(model, task.getTaskDefinitionKey(), targetId);
        List<TopologyNode> collect = tuple._2().stream().flatMap(Collection::stream).distinct().collect(Collectors.toList());


        if (tuple._1 == JumpTypeEnum.serial) {
            log.info("驳回方式驳{}", tuple._1.name());

            runtimeService.createChangeActivityStateBuilder()
                    .processInstanceId(task.getProcessInstanceId())
                    .moveActivityIdTo(task.getTaskDefinitionKey(), targetId)
                    .changeState();
        }
//        if (tuple._1 == JumpTypeEnum.subToParentProcess) {
//            log.info("驳回方式驳{}", tuple._1.name());
//
//            Execution executionTask = runtimeService.createExecutionQuery().executionId(task.getExecutionId()).singleResult();
//            String parentId = executionTask.getParentId();
//            List<Execution> executions2 = runtimeService.createExecutionQuery().parentId(parentId).list();
//            List<String> executionIds = new ArrayList<>();
//
//            executions2.forEach(execution -> executionIds.add(execution.getId()));
//            runtimeService.createChangeActivityStateBuilder()
//                    .moveExecutionsToSingleActivityId(executionIds, targetId)
//                    .changeState();
//
//        }


        if (tuple._1 == JumpTypeEnum.paral || tuple._1 == JumpTypeEnum.subToParentProcess) {

            log.info("驳回方式驳{}", tuple._1.name());
            TopologyNode<FlowElement> topologyNode = collect.get(collect.size() - 1);

            LinkedList<LinkedList<FlowElement>> pathToEnd = new LinkedList<>();

            Graphs.currentToEndAllPath(topologyNode, new LinkedList<>(), pathToEnd);


            List<FlowElement> toEnd = pathToEnd.stream().flatMap(Collection::stream)

                    .distinct().collect(Collectors.toList());
            List<String> executionIds = JoinUtils.innerJoin(executions, toEnd, (a, b) -> a.getId(), Execution::getActivityId, BaseElement::getId);
            log.info("当前executions {} {}", executionIds, executions);
            runtimeService.createChangeActivityStateBuilder()
                    .processInstanceId(task.getProcessInstanceId())
                    .moveExecutionsToSingleActivityId(executionIds, targetId)
                    .changeState();

            Set<FlowElement> sequenceFlows = tuple._3;
            sequenceFlows.forEach(x -> {
                if (x instanceof SequenceFlow) {
                    //todo need remove more test
                    // insertExecution(((SequenceFlow) x).getSourceRef(), task.getProcessInstanceId(), task.getProcessDefinitionId(), task.getTenantId());
                }
                if (x instanceof Gateway) {
                    if (executionIds.contains(x.getId())) {
                        return;
                    }

                    if (runtimeService.createNativeExecutionQuery().sql(String.format("select * from act_ru_execution where PROC_INST_ID_='%s' and ACT_ID_='%s';", task.getProcessInstanceId(), x.getId())).list().isEmpty()) {
                        insertExecution(x.getId(), task.getProcessInstanceId(), task.getProcessDefinitionId(), task.getTenantId());
                    }
                }
            });
        }

        return false;

    }

    protected void insertExecution(String gatewayId, String processInstanceId, String processDefinitionId, String tenantId) {


        ExecutionEntityManager executionEntityManager = ((ProcessEngineConfigurationImpl) processEngineConfiguration).getExecutionEntityManager();
        ExecutionEntity executionEntity = executionEntityManager.create();
        IdGenerator idGenerator = processEngineConfiguration.getIdGenerator();
        executionEntity.setId(idGenerator.getNextId());
        executionEntity.setRevision(0);
        executionEntity.setProcessInstanceId(processInstanceId);

        executionEntity.setParentId(processInstanceId);
        executionEntity.setProcessDefinitionId(processDefinitionId);

        executionEntity.setRootProcessInstanceId(processInstanceId);
        ((ExecutionEntityImpl) executionEntity).setActivityId(gatewayId);
        executionEntity.setActive(false);

        executionEntity.setSuspensionState(1);
        executionEntity.setTenantId(tenantId);

        executionEntity.setStartTime(new Date());
        ((ExecutionEntityImpl) executionEntity).setCountEnabled(true);

        managementService.executeCommand(new SaveExecutionCmd(executionEntity));
    }
}

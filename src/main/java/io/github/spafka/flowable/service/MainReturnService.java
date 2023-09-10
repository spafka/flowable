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
import org.flowable.engine.repository.ProcessDefinition;
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
    @Autowired
    RepositoryService repositoryService;

    @Override
    public List<FlowNodeDto> getCanRejectedFlowNode(BpmnModel bpmnModel, String instanceId, String processInstanceId) {
        Task task = taskService.createTaskQuery().taskId(instanceId).singleResult();
        var tuple3 = Graphs.backTrack(bpmnModel, task.getTaskDefinitionKey(), null);

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
    public boolean returnToTarget(String processInstanceId, String currentId, String targetId) {


        List<Task> list = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .list();
        String processDefinitionId = list.get(0).getProcessDefinitionId();

        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionId(processDefinitionId).singleResult();

        BpmnModel model = repositoryService.getBpmnModel(processDefinition.getId());


        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstanceId).list().stream().filter(x -> x.getActivityId() != null).collect(Collectors.toList());


        var tuple = Graphs.backTrack(model, currentId, targetId);
        List<TopologyNode<FlowElement>> paths = tuple._2().stream().flatMap(Collection::stream).distinct().collect(Collectors.toList());

        Map<String, TopologyNode<FlowElement>> indexMap = tuple._4;

        if (tuple._1 == JumpTypeEnum.serial) {


            log.info("驳回方式驳{}", tuple._1.name());
            LinkedList<LinkedList<FlowElement>> pathToEnd = new LinkedList<>();
            TopologyNode<FlowElement> targetNode = indexMap.get(targetId);

            Graphs.currentToEndAllPath(targetNode, null, new LinkedList<>(), pathToEnd);


            List<TopologyNode> joinGateWay = pathToEnd.stream().flatMap(x -> x.stream().flatMap(y -> indexMap.get(y.getId()).gateways.stream())).distinct()
                    .collect(Collectors.toList()).stream().map(x -> x.join).filter(Objects::nonNull).collect(Collectors.toList());

            List<TopologyNode> g1 = targetNode.gateways.stream().map(x -> x.join).filter(Objects::nonNull).collect(Collectors.toList());
            joinGateWay.removeAll(g1);
            List<FlowElement> toEnd = null;
            if (!joinGateWay.isEmpty()) {
                toEnd = joinGateWay.stream().flatMap(x -> {
                            LinkedList<LinkedList<FlowElement>> pathToEnd2 = new LinkedList<>();
                            Graphs.currentToEndAllPath(targetNode, x.node.getId(), new LinkedList<>(), pathToEnd2);
                            return pathToEnd2.stream().flatMap(Collection::stream);
                        })
                        .distinct().collect(Collectors.toList());
            } else {

                            LinkedList<LinkedList<FlowElement>> pathToEnd2 = new LinkedList<>();
                            Graphs.currentToEndAllPath(targetNode,currentId, new LinkedList<>(), pathToEnd2);

                            toEnd=pathToEnd2.stream().flatMap(x->x.stream()).distinct().collect(Collectors.toList());

            }

            List<String> executionIds = JoinUtils.innerJoin(executions, toEnd, (a, b) -> a.getId(), Execution::getActivityId, BaseElement::getId);
            log.info("当前executions {} {}", executionIds, executions);

            runtimeService.createChangeActivityStateBuilder()
                    .processInstanceId(processInstanceId)
                    .moveExecutionsToSingleActivityId(executionIds, targetId)
                    .changeState();
            if (pathToEnd.stream().anyMatch(flowElements -> flowElements.stream().anyMatch(flowElement -> flowElement instanceof SubProcess))) {
                List<Task> currentTasks = taskService.createTaskQuery().processInstanceId(processInstanceId).list();

                Map<String, List<Task>> listMap = currentTasks.stream().collect(Collectors.groupingBy(x -> x.getTaskDefinitionKey()));

                listMap.forEach((k, v) -> {
                    if (v.size() > 1) {
                        List<Task> toRemoveTask = v.stream().skip(1).collect(Collectors.toList());
                        toRemoveTask.forEach(x -> {
                            jdbcTemplate.update("delete from ACT_RU_TASK where id_ = ? ", x.getId());
                            jdbcTemplate.update("delete from ACT_RU_EXECUTION where id_ = ? ", x.getExecutionId());
                        });
                    }
                });
            }

            return true;

        }


        if (tuple._1 == JumpTypeEnum.paral) {

            log.info("驳回方式驳{}", tuple._1.name());
            TopologyNode<FlowElement> topologyNode = paths.get(paths.size() - 1);

            LinkedList<LinkedList<FlowElement>> pathToEnd = new LinkedList<>();

            Graphs.currentToEndAllPath(topologyNode, null, new LinkedList<>(), pathToEnd);


            List<FlowElement> toEnd = pathToEnd.stream().flatMap(Collection::stream)

                    .distinct().collect(Collectors.toList());
            List<String> executionIds = JoinUtils.innerJoin(executions, toEnd, (a, b) -> a.getId(), Execution::getActivityId, BaseElement::getId);
            log.info("当前executions {} {}", executionIds, executions);
            runtimeService.createChangeActivityStateBuilder()
                    .processInstanceId(processInstanceId)
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

                    if (runtimeService.createNativeExecutionQuery().sql(String.format("select * from act_ru_execution where PROC_INST_ID_='%s' and ACT_ID_='%s';", processDefinition, x.getId())).list().isEmpty()) {
                        insertExecution(x.getId(), processInstanceId, processDefinition.getId(), processDefinition.getTenantId());
                    }
                }
            });
            return true;
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

        managementService.executeCommand(new SaveExecutionCmd(executionEntity, idGenerator));
    }
}

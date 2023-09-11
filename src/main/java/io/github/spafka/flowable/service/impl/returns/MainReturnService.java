package io.github.spafka.flowable.service.impl.returns;

import io.github.spafka.flowable.JumpTypeEnum;
import io.github.spafka.flowable.core.TopologyNode;
import io.github.spafka.flowable.service.FlowNodeDto;
import io.github.spafka.flowable.service.Graphs;
import io.github.spafka.flowable.service.ReturnService;
import io.github.spafka.flowable.service.impl.returns.SaveExecutionCmd;
import io.github.spafka.tuple.Tuple2;
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
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
@Primary
@Slf4j
public class MainReturnService implements ReturnService {

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
        var tuple = Graphs.backTrack(bpmnModel, task.getTaskDefinitionKey(), null);

        LinkedList<TopologyNode<FlowElement>> one = tuple._2.get(0);


        one.removeFirst();
        TopologyNode<FlowElement> last = one.getFirst();

        Deque<TopologyNode<FlowElement>> deque = new LinkedList<>();
        Set<TopologyNode<FlowElement>> processed = new LinkedHashSet<>();


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

        list = list.stream().filter(new Predicate<HistoricTaskInstance>() {

            Set<String> s = new HashSet<>();

            @Override
            public boolean test(HistoricTaskInstance historicTaskInstance) {
                return s.add(historicTaskInstance.getTaskDefinitionKey());
            }
        }).collect(Collectors.toList());

        return JoinUtils.sortInnerJoin(backNodes,
                list,
                Comparator.comparing(FlowNodeDto::getId),
                Comparator.comparing(TaskInfo::getTaskDefinitionKey)
                , (a, b) -> a.getId().compareTo(b.getTaskDefinitionKey()),
                (a, b) -> a);
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

        var topologyNode = indexMap.get(targetId);
        if (tuple._1 == JumpTypeEnum.simple_serial) {
            log.info("简单串行驳回 {} 2 {}", currentId, targetId);
            runtimeService.createChangeActivityStateBuilder()
                    .processInstanceId(processInstanceId)
                    .moveActivityIdTo(currentId, targetId)
                    .changeState();
        } else if (tuple._1 == JumpTypeEnum.serial) {
            log.info("串行驳回 {} 2 {}", currentId, targetId);
            LinkedList<LinkedList<FlowElement>> pathToEnd = new LinkedList<>();
            TopologyNode<FlowElement> targetNode = indexMap.get(targetId);

            Graphs.currentToEndAllPath(targetNode, null, new LinkedList<>(), pathToEnd);

            LinkedList<LinkedList<FlowElement>> pathToEnd2 = new LinkedList<>();
            Graphs.currentToEndAllPath(targetNode, null, new LinkedList<>(), pathToEnd2);
            List<FlowElement> toEnd = pathToEnd2.stream().flatMap(Collection::stream).distinct().collect(Collectors.toList());

            List<String> executionIds = JoinUtils.innerJoin(executions, toEnd, (a, b) -> a.getId(), Execution::getActivityId, BaseElement::getId);
            log.info("当前executions {} {}", executionIds, executions);
            runtimeService.createChangeActivityStateBuilder()
                    .processInstanceId(processInstanceId)
                    .moveExecutionsToSingleActivityId(executionIds, targetId)
                    .changeState();
            if (paths.stream().anyMatch(flowElement -> flowElement.node instanceof SubProcess)) {
                List<Task> currentTasks = taskService.createTaskQuery().processInstanceId(processInstanceId).list();
                Map<String, List<Task>> listMap = currentTasks.stream().collect(Collectors.groupingBy(TaskInfo::getTaskDefinitionKey));

                listMap.forEach((k, v) -> {
                    if (v.size() > 1) {
                        List<Task> toRemoveTask = v.stream().skip(1).collect(Collectors.toList());
                        toRemoveTask.forEach(x -> {
                            jdbcTemplate.update("delete from act_ru_task where id_ = ? ", x.getId());
                            jdbcTemplate.update("delete from act_ru_execution where id_ = ? ", x.getExecutionId());
                        });
                    }
                });
            }
            return true;

        } else if (tuple._1 == JumpTypeEnum.paral_to_child) {
            log.info("并行驳回到join到fork分支 {} 2 {}", currentId, targetId);

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
        } else if (tuple._1 == JumpTypeEnum.paral_to_father) {
            log.info("并行驳回至父网关 {} 2 {}", currentId, targetId);


            TopologyNode<FlowElement>.SkipList<TopologyNode<FlowElement>> parentGates = indexMap.get(targetId).gateways;
            TopologyNode<FlowElement>.SkipList<TopologyNode<FlowElement>> currentGates = indexMap.get(currentId).gateways;
            List<TopologyNode<FlowElement>> endGates = JoinUtils.leftOnly(new ArrayList<>(currentGates), new ArrayList<>(parentGates), (a, b) -> a.node.getId().equals(b.node.getId())).stream().map(x -> x.join).filter(Objects::nonNull).collect(Collectors.toList());


            LinkedList<LinkedList<FlowElement>> pathToEnd = new LinkedList<>();

            endGates.forEach(x -> Graphs.currentToEndAllPath(topologyNode, x.node.getId(), new LinkedList<>(), pathToEnd));


            List<FlowElement> toEnd = pathToEnd.stream().flatMap(Collection::stream)
                    .distinct().collect(Collectors.toList());
            List<String> executionIds = JoinUtils.innerJoin(executions, toEnd, (a, b) -> a.getId(), Execution::getActivityId, BaseElement::getId);
            log.info("当前executions {} {}", executionIds, executions);
            runtimeService.createChangeActivityStateBuilder()
                    .processInstanceId(processInstanceId)
                    .moveExecutionsToSingleActivityId(executionIds, targetId)
                    .changeState();
            // 子流程bug
            if (indexMap.get(currentId).node.getParentContainer() != indexMap.get(targetId).node.getParentContainer()) {
                List<Task> currentTasks = taskService.createTaskQuery().processInstanceId(processInstanceId).list();
                Map<String, List<Task>> listMap = currentTasks.stream().collect(Collectors.groupingBy(TaskInfo::getTaskDefinitionKey));

                listMap.forEach((k, v) -> {
                    if (v.size() > 1) {
                        List<Task> toRemoveTask = v.stream().skip(1).collect(Collectors.toList());
                        toRemoveTask.forEach(x -> {
                            jdbcTemplate.update("delete from act_ru_task where id_ = ? ", x.getId());
                            jdbcTemplate.update("delete from act_ru_execution where id_ = ? ", x.getExecutionId());
                        });
                    }
                });
            }

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

        managementService.executeCommand(new SaveExecutionCmd(executionEntity));
    }
}
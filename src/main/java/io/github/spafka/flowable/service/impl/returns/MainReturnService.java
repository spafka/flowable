package io.github.spafka.flowable.service.impl.returns;

import io.github.spafka.flowable.core.FlowableUtils;
import io.github.spafka.flowable.service.JumpTypeEnum;
import io.github.spafka.flowable.core.TopologyNode;
import io.github.spafka.flowable.service.FlowNodeDto;
import io.github.spafka.flowable.service.Graphs;
import io.github.spafka.flowable.service.ReturnService;
import io.github.spafka.tuple.Tuple2;
import io.github.spafka.util.JoinUtils;
import io.vavr.collection.Stream;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.flowable.bpmn.converter.BpmnXMLConverter;
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

import java.io.ByteArrayInputStream;
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
    public Tuple2<List<FlowNodeDto>, Boolean> getCanRejectedFlowNode(BpmnModel bpmnModel, String instanceId, String processInstanceId) {
        Task task = taskService.createTaskQuery().taskId(instanceId).singleResult();
        var tuple = Graphs.backTrack(bpmnModel, task.getTaskDefinitionKey(), null);
        List<FlowNodeDto> backNodes = null;

        Map<String, TopologyNode<BaseElement>> indexMap = tuple._4;

        if (tuple._2.size() == 1) {
            LinkedList<TopologyNode<BaseElement>> topologyNodes = tuple._2.get(0);
            topologyNodes.removeFirst();
            backNodes = topologyNodes
                    .stream()
                    .filter(x -> x.node instanceof UserTask)
                    .map(x -> new FlowNodeDto(x.node.getId(), ((UserTask) (x.node)).getName()))
                    .collect(Collectors.toList());
        } else {
            LinkedList<TopologyNode<BaseElement>> one = tuple._2.get(0);


            one.removeFirst();
            TopologyNode<BaseElement> last = one.getFirst();

            Deque<TopologyNode<BaseElement>> deque = new LinkedList<>();
            Set<TopologyNode<BaseElement>> processed = new LinkedHashSet<>();


            deque.add(last);
            processed.add(last);
            while (!deque.isEmpty()) {
                TopologyNode<BaseElement> poll = deque.pollFirst();
                TopologyNode<BaseElement>.SkipList<TopologyNode<BaseElement>> pre = poll.pre;
                pre.forEach(x -> {
                    boolean contains = processed.contains(x);
                    if (!contains) {
                        processed.add(x);
                        deque.addLast(x);
                    }
                });
            }
            backNodes = processed
                    .stream()
                    .filter(x -> x.node instanceof UserTask)
                    .map(x -> new FlowNodeDto(x.node.getId(), ((UserTask) (x.node)).getName()))
                    .collect(Collectors.toList());
        }


        List<HistoricTaskInstance> list = historyService.createHistoricTaskInstanceQuery().processInstanceId(task.getProcessInstanceId()).list().stream().filter(x -> x.getEndTime() != null).collect(Collectors.toList());

        list = list.stream().filter(new Predicate<HistoricTaskInstance>() {

            Set<String> s = new HashSet<>();

            @Override
            public boolean test(HistoricTaskInstance historicTaskInstance) {
                return s.add(historicTaskInstance.getTaskDefinitionKey());
            }
        }).collect(Collectors.toList());

        List<FlowNodeDto> collect = JoinUtils.sortInnerJoin(backNodes,
                        list,
                        Comparator.comparing(FlowNodeDto::getId),
                        Comparator.comparing(TaskInfo::getTaskDefinitionKey)
                        , (a, b) -> a.getId().compareTo(b.getTaskDefinitionKey()),
                        (a, b) -> a)
                .stream().filter(x -> !x.getId().equals(task.getTaskDefinitionKey())).collect(Collectors.toList());

        boolean ok = FlowableUtils.iteratorCheckSequentialReferTarget((FlowElement) indexMap.get(task.getTaskDefinitionKey()).node, collect.get(collect.size() - 1).getId(), null, null);

        return Tuple2.of(collect, ok);
    }

    @Override
    public boolean returnToTarget(Task task, String... targetId) {
        String currentId = task.getTaskDefinitionKey();

        String processInstanceId = task.getProcessInstanceId();
        List<Task> list = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .list();
        String processDefinitionId = list.get(0).getProcessDefinitionId();

        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionId(processDefinitionId).singleResult();
        // 获取所有节点信息，暂不考虑子流程情况
        byte[] bytesS = jdbcTemplate.queryForObject("SELECT BYTES_ FROM ACT_GE_BYTEARRAY WHERE NAME_ = ? and DEPLOYMENT_ID_=? ", new Object[]{processDefinition.getResourceName(), processDefinition.getDeploymentId()}, (resultSet, i) -> resultSet.getBytes("BYTES_"));
        repositoryService.getBpmnModel(processDefinition.getId());

        BpmnModel model = new BpmnXMLConverter().convertToBpmnModel(() -> new ByteArrayInputStream(bytesS), false, false);


        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstanceId).list().stream().filter(x -> x.getActivityId() != null).collect(Collectors.toList());


        var tuple = Graphs.backTrack(model, currentId, targetId[0]);

        Map<String, TopologyNode<BaseElement>> indexMap = tuple._4;

        var topologyNode = indexMap.get(targetId[0]);

        {
            if (tuple._1 == JumpTypeEnum.simple_serial || tuple._1 == JumpTypeEnum.paral_to_child) {
                List<HistoricTaskInstance> taskInstances = historyService.createHistoricTaskInstanceQuery()
                        .processInstanceId(processInstanceId).orderByHistoricTaskInstanceStartTime().asc().list();

                LinkedList<TopologyNode<BaseElement>> topologyNodes = tuple._2.get(0);
                TopologyNode<BaseElement> head = topologyNodes.getLast();
                TopologyNode<BaseElement> tail = topologyNodes.getFirst();

                List<HistoricTaskInstance> javaList = Stream.ofAll(taskInstances.stream()).dropUntil(x -> x.getTaskDefinitionKey().equals(head.node.getId())).dropRightUntil(x -> x.getTaskDefinitionKey().equals(tail.node.getId())).toJavaList();

                List<TopologyNode<BaseElement>> betweenNodes = tuple._2.stream().flatMap(Collection::stream).distinct().collect(Collectors.toList());

                List<HistoricTaskInstance> toDeleteHistory = JoinUtils.sortInnerJoin(javaList, betweenNodes, Comparator.comparing(TaskInfo::getTaskDefinitionKey), Comparator.comparing(x -> x.node.getId()), (a, b) -> a.getTaskDefinitionKey().compareTo(b.node.getId()), (a, b) -> a);
                log.info("{} => {} , to delete task his {}", task.getTaskDefinitionKey(), targetId, toDeleteHistory.stream().map(TaskInfo::getTaskDefinitionKey).collect(Collectors.toList()));

                toDeleteHistory.forEach(x -> historyService.deleteHistoricTaskInstance(x.getId()));
            } else {
                List<HistoricTaskInstance> taskInstances = historyService.createHistoricTaskInstanceQuery()
                        .processInstanceId(processInstanceId).orderByHistoricTaskInstanceStartTime().asc().list();

                var more = indexMap.get(currentId).gateways.stream().flatMap(x -> x.join.stream()).filter(Objects::nonNull).collect(Collectors.toList());
                var less = indexMap.get(targetId).gateways.stream().flatMap(x -> x.join.stream()).filter(Objects::nonNull).collect(Collectors.toList());

                List<TopologyNode<BaseElement>> endGates = JoinUtils.leftOnly(more, less, (a, b) -> a == b);

                LinkedList<LinkedList<FlowElement>> pathToEnd = new LinkedList<>();

                endGates.forEach(x -> Graphs.currentToEndAllPath(topologyNode, x.node.getId(), new LinkedList<>(), pathToEnd));

                List<HistoricTaskInstance> javaList = taskInstances;
                var betweenNodes = pathToEnd.stream().flatMap(Collection::stream).distinct().collect(Collectors.toList());

                List<HistoricTaskInstance> toDeleteHistory = JoinUtils.sortInnerJoin(javaList, betweenNodes, Comparator.comparing(TaskInfo::getTaskDefinitionKey), Comparator.comparing(BaseElement::getId), (a, b) -> a.getTaskDefinitionKey().compareTo(b.getId()), (a, b) -> a);
                log.info("{} => {} , to delete task his {}", task.getTaskDefinitionKey(), targetId, toDeleteHistory.stream().map(TaskInfo::getTaskDefinitionKey).collect(Collectors.toList()));

                toDeleteHistory.forEach(x -> historyService.deleteHistoricTaskInstance(x.getId()));

            }

        }

        if (tuple._1 == JumpTypeEnum.simple_serial) {
            log.info("简单串行驳回 {} 2 {}", currentId, targetId);
            runtimeService.createChangeActivityStateBuilder()
                    .processInstanceId(processInstanceId)
                    .moveActivityIdTo(currentId, targetId[0])
                    .changeState();
        } else if (tuple._1 == JumpTypeEnum.paral_to_child) {
            log.info("并行驳回到join到fork分支 {} 2 {}", currentId, targetId);

            LinkedList<LinkedList<FlowElement>> pathToEnd = new LinkedList<>();

            Arrays.stream(targetId).forEach(x -> {
                Graphs.currentToEndAllPath(indexMap.get(x), null, new LinkedList<>(), pathToEnd);
            });


            List<FlowElement> toEnd = pathToEnd.stream().flatMap(Collection::stream)
                    .distinct().collect(Collectors.toList());
            List<String> executionIds = JoinUtils.innerJoin(executions, toEnd, (a, b) -> a.getId(), Execution::getActivityId, BaseElement::getId);
            log.info("当前executions {} {}", executionIds, executions);

            runtimeService.createChangeActivityStateBuilder()
                    .processInstanceId(processInstanceId)
                    .moveSingleActivityIdToActivityIds(task.getTaskDefinitionKey(), Arrays.stream(targetId).sorted().collect(Collectors.toList()))
                    .changeState();

            pathToEnd.stream().flatMap(Collection::stream).distinct().forEach(x -> {
                if (x instanceof Gateway) {
                    if (executionIds.contains(x.getId())) {
                        return;
                    }
                    if (runtimeService.createNativeExecutionQuery().sql(String.format("select * from act_ru_execution where PROC_INST_ID_='%s' and ACT_ID_='%s'", processInstanceId, x.getId())).list().isEmpty()) {
                      //  insertExecution(x.getId(), processInstanceId, processDefinition.getId(), processDefinition.getTenantId());
                    }
                }
            });
            return true;
        } else if (tuple._1 == JumpTypeEnum.paral_to_father) {
            log.info("并行驳回至父网关 {} 2 {}", currentId, targetId);


            TopologyNode<BaseElement>.SkipList<TopologyNode<BaseElement>> parentGates = indexMap.get(targetId).gateways;
            TopologyNode<BaseElement>.SkipList<TopologyNode<BaseElement>> currentGates = indexMap.get(currentId).gateways;
            List<TopologyNode> endGates = JoinUtils.leftOnly(new ArrayList<>(currentGates), new ArrayList<>(parentGates), (a, b) -> a.node.getId().equals(b.node.getId())).stream().flatMap(x -> x.join.stream()).filter(Objects::nonNull).collect(Collectors.toList());


            LinkedList<LinkedList<FlowElement>> pathToEnd = new LinkedList<>();

            endGates.forEach(x -> Graphs.currentToEndAllPath(topologyNode, x.node.getId(), new LinkedList<>(), pathToEnd));


            List<FlowElement> toEnd = pathToEnd.stream().flatMap(Collection::stream)
                    .distinct().collect(Collectors.toList());
            List<String> executionIds = JoinUtils.innerJoin(executions, toEnd, (a, b) -> a.getId(), Execution::getActivityId, BaseElement::getId);
            log.info("当前executions {} {}", executionIds, executions);
            runtimeService.createChangeActivityStateBuilder()
                    .processInstanceId(processInstanceId)
                    .moveExecutionsToSingleActivityId(executionIds, targetId[0])
                    .changeState();
            // 子流程bug
            if (((FlowElement) indexMap.get(currentId).node).getParentContainer() != ((FlowElement) indexMap.get(targetId).node).getParentContainer()) {
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

    public void insertExecution(String gatewayId, String processInstanceId, String processDefinitionId, String tenantId) {


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

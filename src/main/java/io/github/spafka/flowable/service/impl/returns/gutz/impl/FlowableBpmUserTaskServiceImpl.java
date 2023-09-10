package io.github.spafka.flowable.service.impl.returns.gutz.impl;

import io.github.spafka.flowable.service.impl.returns.SaveExecutionCmd;
import io.github.spafka.flowable.service.impl.returns.gutz.BpmProcessService;
import io.github.spafka.flowable.service.impl.returns.gutz.BpmUserTaskService;
import io.github.spafka.flowable.service.impl.returns.gutz.dto.ParallelGatwayDTO;
import io.github.spafka.flowable.service.impl.returns.gutz.dto.UserTaskModelDTO;
import io.github.spafka.flowable.service.impl.returns.gutz.entity.BpmTaskEntity;
import io.github.spafka.flowable.service.impl.returns.gutz.entity.BpmTaskModelEntity;
import io.github.spafka.flowable.service.impl.returns.gutz.enums.GatewayJumpTypeEnum;
import io.github.spafka.flowable.service.impl.returns.gutz.form.BpmJumpForm;
import io.github.spafka.flowable.service.impl.returns.gutz.query.BpmBackTaskModelQuery;
import io.github.spafka.flowable.service.impl.returns.gutz.query.BpmTaskModelQuery;
import io.github.spafka.flowable.service.impl.returns.gutz.query.BpmTaskQuery;
import io.github.spafka.flowable.service.impl.returns.gutz.util.UserTaskAttrUtil;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.*;
import org.flowable.common.engine.impl.cfg.IdGenerator;
import org.flowable.engine.*;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityImpl;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityManager;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskInfo;
import org.flowable.task.api.TaskInfoQuery;
import org.flowable.task.api.TaskQuery;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Flowable 6
 *
 * @author guzt
 * @author spafka
 */
@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
@AllArgsConstructor
@Service
public class FlowableBpmUserTaskServiceImpl implements BpmUserTaskService {

    private static final Logger logger = LoggerFactory.getLogger(FlowableBpmUserTaskServiceImpl.class);

    private RuntimeService runtimeService;

    private BpmProcessService bpmProcessService;

    private HistoryService historyService;

    private RepositoryService repositoryService;

    private TaskService taskService;

    private ProcessEngineConfiguration processEngineConfiguration;

    private ManagementService managementService;


    private void setItemToBpmTaskEntityList(List<BpmTaskEntity> result, TaskInfo taskInfo, Map<String, Map<String, UserTask>> allProcessUserTaskMap) {
        Map<String, UserTask> allUserTaskMap = allProcessUserTaskMap.get(taskInfo.getProcessDefinitionId());
        if (allUserTaskMap == null) {
            allUserTaskMap = getAllUserTaskMap(taskInfo.getProcessDefinitionId());
            allProcessUserTaskMap.put(taskInfo.getProcessDefinitionId(), allUserTaskMap);
        }

        BpmTaskEntity entity = new BpmTaskEntity();
        getTaskInfoToBpmTaskEntity(entity, taskInfo, allUserTaskMap);
        result.add(entity);
    }


    /**
     * 适用于 HistoricTaskInstanceQuery  TaskQuery
     *
     * @param taskQuery TaskInfoQuery
     * @param query     BpmTaskQuery
     */
    @SuppressWarnings("rawtypes")
    private void initTaskInfoQuery(TaskInfoQuery taskQuery, BpmTaskQuery query) {
        if (StringUtils.isNotBlank(query.getTenantId())) {
            taskQuery.taskTenantId(query.getTenantId());
        }
        if (StringUtils.isNotBlank(query.getProcessInstanceId())) {
            taskQuery.processInstanceId(query.getProcessInstanceId());
        }
        if (StringUtils.isNotBlank(query.getCategory())) {
            taskQuery.taskCategory(query.getCategory());
        }
        if (StringUtils.isNotBlank(query.getTaskDefKey())) {
            taskQuery.taskDefinitionKey(query.getTaskDefKey());
        }
    }

    private List<BpmTaskEntity> convertBpmHisTaskEntity(List<HistoricTaskInstance> taskList) {
        List<BpmTaskEntity> result = new ArrayList<>(8);
        if (!CollectionUtils.isEmpty(taskList)) {
            Map<String, Map<String, UserTask>> allProcessUserTaskMap = new HashMap<>(8);
            for (HistoricTaskInstance item : taskList) {
                setItemToBpmTaskEntityList(result, item, allProcessUserTaskMap);
            }
        }

        return result;
    }

    private void getTaskInfoToBpmTaskEntity(BpmTaskEntity entity, TaskInfo item, Map<String, UserTask> allUserTaskMap) {
        entity.setId(item.getId());
        entity.setExecutionId(item.getExecutionId());
        entity.setTaskName(item.getName());
        entity.setProcInstId(item.getProcessInstanceId());
        entity.setProcDefId(item.getProcessDefinitionId());
        entity.setAssignee(item.getAssignee());
        entity.setStartTime(item.getCreateTime());
        if (item instanceof HistoricTaskInstance) {
            entity.setEndTime(((HistoricTaskInstance) item).getEndTime());
        }
        entity.setTenantId(item.getTenantId());
        entity.setFormKey(item.getFormKey());
        entity.setCategory(item.getCategory());
        entity.setTaskDefKey(item.getTaskDefinitionKey());
        entity.setDueDate(item.getDueDate());
        // 是否多实例信息设置
        UserTask userTaskModel = allUserTaskMap.get(item.getTaskDefinitionKey());
        entity.setSkipExpression(userTaskModel.getSkipExpression());
        entity.setHasMultiInstance(userTaskModel.hasMultiInstanceLoopCharacteristics());
        if (userTaskModel.hasMultiInstanceLoopCharacteristics()) {
            entity.setSequential(userTaskModel.getLoopCharacteristics().isSequential());
        } else {
            entity.setSequential(false);
        }
        UserTaskAttrUtil.setAttr(entity, userTaskModel);
    }

    private Map<String, UserTask> getAllUserTaskMap(String processDefinitionId) {
        ProcessDefinition processDefinition = repositoryService
                .createProcessDefinitionQuery()
                .processDefinitionId(processDefinitionId).singleResult();
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinition.getId());
        Process process = bpmnModel.getProcesses().get(0);
        return process.findFlowElementsOfType(UserTask.class)
                .stream().collect(Collectors.toMap(UserTask::getId, a -> a, (k1, k2) -> k1));
    }


    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    @Override
    public void jump(BpmJumpForm form) {
        Task task = taskService.createTaskQuery().taskId(form.getTaskId()).singleResult();

        BpmTaskModelQuery query = new BpmTaskModelQuery();
        query.setDefineId(task.getProcessDefinitionId());
        UserTaskModelDTO userTaskModelsDTO = bpmProcessService.getUserTaskModelDto(query);

        List<BpmTaskModelEntity> taskModelEntities = userTaskModelsDTO.getAllUserTaskModels();
        Map<String, BpmTaskModelEntity> taskModelEntitiesMap = taskModelEntities.stream().collect(
                Collectors.toMap(BpmTaskModelEntity::getTaskDefKey, a -> a, (k1, k2) -> k1));

        // 要跳转的节点 B
        List<BpmTaskModelEntity> targetNodes = new ArrayList<>(4);
        // 当前节点 A
        BpmTaskModelEntity currentNode = taskModelEntitiesMap.get(task.getTaskDefinitionKey());
        form.getTargetTaskDefineKes().forEach(item -> targetNodes.add(taskModelEntitiesMap.get(item)));
        if (currentNode == null || CollectionUtils.isEmpty(targetNodes)) {
            Objects.requireNonNull(currentNode);
        }
        // 设置本次全局变量信息
        if (!CollectionUtils.isEmpty(form.getVariables())) {
            runtimeService.setVariables(task.getProcessInstanceId(), form.getVariables());
        }
        // （1）如果B有多个节点
        //        必须为同一个并行网关内的任务节点（网关开始、合并节点必须一致）
        //        必须不是同一条流程线上的任务节点
        checkjumpTargetNodes(targetNodes);

        // （2）如果A和B为同一条顺序流程线上（其中包含了A/B都是非并行网关上的节点 或都为并行网关中同一条流程线上的节点），则可以直接跳转
        if (targetNodes.size() == 1 &&
                currentNode.getParallelGatewayForkRef().equals(targetNodes.get(0).getParallelGatewayForkRef())) {
            runtimeService.createChangeActivityStateBuilder().processInstanceId(task.getProcessInstanceId())
                    .moveActivityIdTo(task.getTaskDefinitionKey(), form.getTargetTaskDefineKes().get(0))
                    .changeState();
            return;
        }

        //（3）如果A非并行分支上的任务节点
        //    则根据以上判定，B一定是为并行网关上节点，需要创建其B所在并行网关内其他任务节点已完成日志
        if (!currentNode.getInParallelGateway()) {
            String forkParallelGatwayId = targetNodes.get(0).getForkParallelGatewayId();
            ParallelGatwayDTO forkGatewayB = userTaskModelsDTO.getAllForkGatewayMap().get(forkParallelGatwayId);
            // B为并行网关上节点，需要创建其B所在并行网关内其他任务节点已完成日志
            dealParallelGatewayFinishLog(forkGatewayB, task, targetNodes.size());
            // 跳转
            runtimeService.createChangeActivityStateBuilder().processInstanceId(
                            task.getProcessInstanceId())
                    .moveSingleActivityIdToActivityIds(task.getTaskDefinitionKey(), form.getTargetTaskDefineKes())
                    .changeState();
        } else {
            //（4）如果A是并行分支上的任务节点
            //    4.1 从外向里面跳转（父并网关 》子并网关）
            //    B是为并行网关上节点，需要创建其B所在并行网关内其他任务节点已完成日志
            //   4.2 从里向外面跳转 （子并网关 》父并网关 【或】 非并行网关上的节点 【或】 其他非父子关系的并行网关节点）
            //    需要清除本任务节点并行网关上（包括父网关）所有的其他未完成的用户任务
            //    B如果是为并行网关上节点，需要创建其B所在并行网关内其他任务节点已完成日志
            gatewayJump(userTaskModelsDTO, targetNodes, currentNode, task, form);

        }


    }

    private void gatewayJump(
            UserTaskModelDTO userTaskModelsDTO, List<BpmTaskModelEntity> targetNodes, BpmTaskModelEntity currentNode, Task task, BpmJumpForm form) {
        // A是并行分支上的任务节点，获得跳转类别
        GatewayJumpTypeEnum gatewayJumpFlag = getGatewayJumpFlag(
                currentNode, targetNodes.get(0), userTaskModelsDTO.getAllForkGatewayMap());

        if (gatewayJumpFlag.equals(GatewayJumpTypeEnum.TO_IN_CHILD)) {
            String forkParallelGatwayId = targetNodes.get(0).getForkParallelGatewayId();
            ParallelGatwayDTO forkGatewayB = userTaskModelsDTO.getAllForkGatewayMap().get(forkParallelGatwayId);
            // B为并行网关上节点，需要创建其B所在并行网关内其他任务节点已完成日志
            dealParallelGatewayFinishLog(forkGatewayB, task, targetNodes.size(), currentNode.getForkParallelGatewayId());
            runtimeService.createChangeActivityStateBuilder().processInstanceId(task.getProcessInstanceId())
                    .moveSingleActivityIdToActivityIds(task.getTaskDefinitionKey(), form.getTargetTaskDefineKes())
                    .changeState();
        } else if (gatewayJumpFlag.equals(GatewayJumpTypeEnum.TO_OUT_NO_PARALLEL)) {
            // B非并行网关上的节点,则只可能有一个, 因此只能全部终结现有的任务，跳转到B点
            TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(task.getProcessInstanceId());
            List<Task> runningTasks = taskQuery.list();
            runtimeService.createChangeActivityStateBuilder().processInstanceId(task.getProcessInstanceId())
                    .moveActivityIdsToSingleActivityId(
                            runningTasks.stream().map(Task::getTaskDefinitionKey).collect(Collectors.toList()),
                            form.getTargetTaskDefineKes().get(0))
                    .changeState();
        } else if (gatewayJumpFlag.equals(GatewayJumpTypeEnum.TO_OUT_PARENT)) {
            //获取本 targetTaskDefineKey 所在的并行网关中，下游所有用户任务，查找直到targetTaskDefineKey的合并网关位置
            Set<String> taskKeys = new HashSet<>(4);
            targetNodes.forEach(item -> taskKeys.addAll(getMyNextFlowForkGatewayTaskKeys(item, userTaskModelsDTO.getProcess())));
            // 跳转
            moveActivityIdToOtherParallelGateway(taskKeys, task, form.getTargetTaskDefineKes());
        } else if (gatewayJumpFlag.equals(GatewayJumpTypeEnum.TO_OUT_OTHER_PARALLEL)) {
            // 获取本并行网关上的全部任务
            Set<String> taskKeys = getMyOtherForkGatewayTaskKeys(
                    currentNode, userTaskModelsDTO.getAllForkGatewayMap());

            String forkParallelGatwayId = targetNodes.get(0).getForkParallelGatewayId();
            ParallelGatwayDTO forkGatewayB = userTaskModelsDTO.getAllForkGatewayMap().get(forkParallelGatwayId);
            // B为并行网关上节点，需要创建其B所在并行网关内其他任务节点已完成日志
            dealParallelGatewayFinishLog(forkGatewayB, task, targetNodes.size());
            // 跳转
            moveActivityIdToOtherParallelGateway(taskKeys, task, form.getTargetTaskDefineKes());
        } else {
            throw new RuntimeException("暂不支持这样的流程跳转");
        }
    }

    /**
     * 跳转其他并行网关上的用户任务.
     *
     * @param otherUserTaskKeys   本并行网关上的其他在运行的全部任务
     * @param currentTask         当前并行网关上用户任务
     * @param targetTaskDefineKes 跳转的目标节点
     */
    private void moveActivityIdToOtherParallelGateway(
            Set<String> otherUserTaskKeys,
            Task currentTask,
            List<String> targetTaskDefineKes) {
        if (targetTaskDefineKes.size() == 1) {
            otherUserTaskKeys.add(currentTask.getTaskDefinitionKey());
            runtimeService.createChangeActivityStateBuilder().processInstanceId(currentTask.getProcessInstanceId())
                    .moveActivityIdsToSingleActivityId(new ArrayList<>(otherUserTaskKeys), targetTaskDefineKes.get(0))
                    .changeState();
        } else {
            // 删除本并行网关上的其他在运行的全部任务
            deleteMyRelateTask(otherUserTaskKeys, currentTask);
            runtimeService.createChangeActivityStateBuilder().processInstanceId(currentTask.getProcessInstanceId())
                    .moveSingleActivityIdToActivityIds(currentTask.getTaskDefinitionKey(), targetTaskDefineKes)
                    .changeState();
        }
    }


    /**
     * 并行网关跳转时，本节点之外的并行网关上的节点任务需要删除
     *
     * @param deleteTaskKeys 本并行网关上的其他任务节点key
     * @param currentTask    当前要准备跳转的任务
     */
    private void deleteMyRelateTask(Set<String> deleteTaskKeys, Task currentTask) {
        deleteTaskKeys.remove(currentTask.getTaskDefinitionKey());
        TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(currentTask.getProcessInstanceId());
        taskQuery.or();
        deleteTaskKeys.forEach(taskQuery::taskDefinitionKey);
        taskQuery.endOr();
        List<Task> runningTasks = taskQuery.list();
        if (runningTasks != null) {
            runningTasks.forEach(item -> deleteExecutionByTaskId(item.getId()));
        }
    }

    /**
     * 获取本 targetTaskDefineKey 所在的并行网关中，下游所有用户任务，查找直到targetTaskDefineKey的合并网关位置
     *
     * @param targetTask 要查找的节点id
     * @param process    所在Process
     * @return Set<String>
     */
    private Set<String> getMyNextFlowForkGatewayTaskKeys(BpmTaskModelEntity targetTask, Process process) {
        Set<String> taskKeys = new HashSet<>(4);
        FlowElement flowElement = process.getFlowElementMap().get(targetTask.getTaskDefKey());
        loopOutcomingFlows(((FlowNode) flowElement).getOutgoingFlows(),
                taskKeys, process.getFlowElementMap(), targetTask.getJoinParallelGatewayId());
        return taskKeys;
    }

    /**
     * 递归获取某个节点后面的所有相关节点
     *
     * @param outcomingFlows 相邻来源节点引用集合
     * @param taskKeys       最后输出的 List结果集
     * @param flowElementMap Process # getFlowElementMap()
     */
    private void loopOutcomingFlows(
            List<SequenceFlow> outcomingFlows,
            Set<String> taskKeys,
            Map<String, FlowElement> flowElementMap,
            String limitFlowId) {
        if (CollectionUtils.isEmpty(outcomingFlows)) {
            return;
        }

        for (SequenceFlow item : outcomingFlows) {
            if (StringUtils.isNotBlank(limitFlowId) && item.getId().equals(limitFlowId)) {
                break;
            }
            FlowElement flowElement = flowElementMap.get(item.getTargetRef());
            if (flowElement instanceof FlowNode) {
                if (flowElement instanceof UserTask) {
                    UserTask task = (UserTask) flowElement;
                    taskKeys.add(task.getId());
                }

                loopOutcomingFlows(
                        ((FlowNode) flowElement).getOutgoingFlows(),
                        taskKeys,
                        flowElementMap,
                        limitFlowId);
            }
        }
    }

    /**
     * 本节点所在的全部相关网(包括父子并行网关)内的任务定义的key，除了本节点之外
     *
     * @param currentNode       ignore
     * @param allForkGatewayMap ignore
     * @return Set<String>
     */
    private Set<String> getMyOtherForkGatewayTaskKeys(BpmTaskModelEntity currentNode, Map<String, ParallelGatwayDTO> allForkGatewayMap) {
        Set<String> taskKeys = new HashSet<>(4);
        if (!currentNode.getInParallelGateway()) {
            return taskKeys;
        }
        // 所有子网关内的用户keys
        currentNode.getChildForkParallelGatewayIds().forEach(item -> taskKeys.addAll(allForkGatewayMap.get(item).getUserTaskModels().keySet()));
        // 所有并行网关内的用户keys
        ParallelGatwayDTO parallelGatwayDTO = allForkGatewayMap.get(currentNode.getForkParallelGatewayId());
        ParallelGatwayDTO parentParallelGatwayDTO = parallelGatwayDTO.getParentParallelGatwayDTO();
        while (parentParallelGatwayDTO != null) {
            taskKeys.addAll(parentParallelGatwayDTO.getUserTaskModels().keySet());
            parentParallelGatwayDTO = parentParallelGatwayDTO.getParentParallelGatwayDTO();
        }

        return taskKeys;
    }

    /**
     * 如果A是并行分支上的任务节点，获得跳转类别
     *
     * @param currentNode       当前节点
     * @param targetNode        其中一个目标节点
     * @param allForkGatewayMap 所有的（不分父子网关）并行网关
     * @return 一共4中情况
     * 1 从外向里面跳转（父并网关 》子并网关）
     * 2 从里向外面跳转 B为非并行行网关上的节点
     * 3 从里向外面跳转 B为父并行网关上的节点
     * 4 从里向外面跳转 B为其他独立并行网关上的节点
     */
    private GatewayJumpTypeEnum getGatewayJumpFlag(
            BpmTaskModelEntity currentNode,
            BpmTaskModelEntity targetNode,
            Map<String, ParallelGatwayDTO> allForkGatewayMap) {

        if (!targetNode.getInParallelGateway()) {
            return GatewayJumpTypeEnum.TO_OUT_NO_PARALLEL;
        } else {
            if (currentNode.getChildForkParallelGatewayIds().contains(targetNode.getForkParallelGatewayId())) {
                return GatewayJumpTypeEnum.TO_IN_CHILD;
            } else if (targetNode.getChildForkParallelGatewayIds().contains(currentNode.getForkParallelGatewayId())) {
                return GatewayJumpTypeEnum.TO_OUT_PARENT;
            } else if (targetNode.getForkParallelGatewayId().equals(currentNode.getForkParallelGatewayId())) {
                logger.info("相同网关中的节点不可进行跳转操作，currentNode={} targetNode={}", currentNode.getTaskDefKey(), targetNode.getTaskDefKey());
                return GatewayJumpTypeEnum.NO_SUPPORT_JUMP;
            } else {
                // B为其他并行网关上的节点
                ParallelGatwayDTO currentTopPgw = allForkGatewayMap.get(currentNode.getForkParallelGatewayId()).getParentParallelGatwayDTO();
                while (currentTopPgw.getParentParallelGatwayDTO() != null) {
                    currentTopPgw = currentTopPgw.getParentParallelGatwayDTO();
                }
                ParallelGatwayDTO targetTopPgw = allForkGatewayMap.get(targetNode.getForkParallelGatewayId()).getParentParallelGatwayDTO();
                while (targetTopPgw.getParentParallelGatwayDTO() != null) {
                    targetTopPgw = targetTopPgw.getParentParallelGatwayDTO();
                }

                if (currentTopPgw.getForkId().equals(targetTopPgw.getForkId())) {
                    logger.info("相同顶级父并行网关下的节点不可进行跳转操作，currentNode={} targetNode={}", currentNode.getTaskDefKey(), targetNode.getTaskDefKey());
                    return GatewayJumpTypeEnum.NO_SUPPORT_JUMP;
                }

                return GatewayJumpTypeEnum.TO_OUT_OTHER_PARALLEL;
            }
        }
    }

    /**
     * B为并行网关上节点，需要创建其B所在并行网关内其他任务节点已完成日志
     *
     * @param forkGatewayB    B所在的并行网关
     * @param task            当然任务
     * @param targetNodesSize B的数量
     */
    private void dealParallelGatewayFinishLog(
            ParallelGatwayDTO forkGatewayB, Task task, int targetNodesSize) {
        dealParallelGatewayFinishLog(forkGatewayB, task, targetNodesSize, null);
    }

    /**
     * B为并行网关上节点，需要创建其B所在并行网关内其他任务节点已完成日志
     *
     * @param forkGatewayB             B所在的并行网关
     * @param task                     当然任务
     * @param targetNodesSize          B的数量
     * @param untilParentForkGatewayId b的最上层父并行网关
     */
    private void dealParallelGatewayFinishLog(
            ParallelGatwayDTO forkGatewayB, Task task, int targetNodesSize, String untilParentForkGatewayId) {
        int reduceForkSize = forkGatewayB.getForkSize() - targetNodesSize;
        if (reduceForkSize < 0) {
            throw new RuntimeException("目标节点数量不能大于并行网关的总分支数量");
        }
        if (reduceForkSize > 0) {
            for (int i = 0; i < reduceForkSize; i++) {
                logger.debug("插入目标节点的合并网关 {} 一条分支线上的完成记录", forkGatewayB.getJoinId());
                insertExecutionTest(forkGatewayB.getJoinId(), task.getProcessInstanceId(), task.getProcessDefinitionId(), task.getTenantId());
            }
        }

        // 如果该网关是子网关则，还需要处理父网关信息
        ParallelGatwayDTO parentParallelGatwayDTO = forkGatewayB.getParentParallelGatwayDTO();
        while (parentParallelGatwayDTO != null) {
            if (StringUtils.isNotBlank(untilParentForkGatewayId)
                    && parentParallelGatwayDTO.getForkId().equals(untilParentForkGatewayId)) {
                break;
            }

            for (int i = 0; i < parentParallelGatwayDTO.getForkSize() - 1; i++) {
                logger.debug("插入目标节点的父合并网关 {} 一条分支线上的完成记录 ", parentParallelGatwayDTO.getJoinId());
                insertExecutionTest(parentParallelGatwayDTO.getJoinId(), task.getProcessInstanceId(), task.getProcessDefinitionId(), task.getTenantId());
            }
            parentParallelGatwayDTO = parentParallelGatwayDTO.getParentParallelGatwayDTO();
        }
    }

    /**
     * （1）如果B有多个节点
     * 必须为同一个并行网关内的任务节点（网关开始、合并节点必须一致）
     * 必须不是同一条流程线上的任务节点
     *
     * @param targetNodes 要跳转到的目标节点集合
     */
    private void checkjumpTargetNodes(List<BpmTaskModelEntity> targetNodes) {
        int limitSize = 2;
        if (targetNodes.size() < limitSize) {
            return;
        }
        //判断节点
        String forkParallelGatwayId = "";
        String joinParallelGatwayId = "";
        String parallelGatewayForkRef = "";
        for (BpmTaskModelEntity item : targetNodes) {
            if (!item.getInParallelGateway()) {
                throw new RuntimeException("目标节点非并行网关中的节点");
            }
            if (StringUtils.isBlank(forkParallelGatwayId) || StringUtils.isBlank(joinParallelGatwayId)) {
                forkParallelGatwayId = item.getForkParallelGatewayId();
                joinParallelGatwayId = item.getJoinParallelGatewayId();
                continue;
            }
            if (!forkParallelGatwayId.equals(item.getForkParallelGatewayId()) ||
                    !joinParallelGatwayId.equals(item.getJoinParallelGatewayId())) {
                throw new RuntimeException("目标节点不是同一个并行网关");
            }
            if (StringUtils.isBlank(parallelGatewayForkRef)) {
                parallelGatewayForkRef = item.getParallelGatewayForkRef();
                continue;
            }
            if (parallelGatewayForkRef.equals(item.getParallelGatewayForkRef())) {
                throw new RuntimeException("目标节点不能在同一条业务线上");
            }
        }
    }

    @Override
    public List<BpmTaskModelEntity> listAllBackTaskModel(BpmBackTaskModelQuery query, BpmnModel bpmnModel) {
        List<BpmTaskModelEntity> resultTasks = new ArrayList<>(16);
        Task task = taskService.createTaskQuery().taskId(query.getTaskId()).singleResult();
        Map<String, BpmTaskModelEntity> allTaskMap = initLimitBackTaskModel(query, task);

        Map<String, FlowElement> flowElementMap = getAllFlowElement(task.getProcessDefinitionId());
        FlowElement targetFlowElement = flowElementMap.get(task.getTaskDefinitionKey());
        // 目标节点的上游节点集合
        List<UserTask> tasks = new ArrayList<>(16);
        Map<String, UserTask> taskMap = new HashMap<>(16);
        loopIncomingFlows(((FlowNode) targetFlowElement).getIncomingFlows(),
                tasks, taskMap, flowElementMap, query, false,null);
        tasks.forEach(item -> {
            if (allTaskMap.containsKey(item.getId())) {
                resultTasks.add(allTaskMap.get(item.getId()));
            }
        });
        return resultTasks;
    }

    private Map<String, FlowElement> getAllFlowElement(String processDefinitionId) {
        ProcessDefinition processDefinition = repositoryService
                .createProcessDefinitionQuery()
                .processDefinitionId(processDefinitionId)
                .singleResult();
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinition.getId());
        Process process = bpmnModel.getProcesses().get(0);
        return process.getFlowElementMap();
    }

    private Map<String, BpmTaskModelEntity> initLimitBackTaskModel(BpmBackTaskModelQuery query, Task task) {
        // 历史用户任务
        Set<String> taskKeysSet = historyService.createHistoricTaskInstanceQuery()
                .finished()
                .processInstanceId(task.getProcessInstanceId())
                .list().stream().map(HistoricTaskInstance::getTaskDefinitionKey).collect(Collectors.toSet());
        BpmTaskModelQuery taskModelQuery = new BpmTaskModelQuery();
        taskModelQuery.setDefineId(task.getProcessDefinitionId());
        if (StringUtils.isNotBlank(query.getCategory())) {
            taskModelQuery.setCategory(query.getCategory());
        }
        if (StringUtils.isNotBlank(query.getNodeType())) {
            taskModelQuery.setNodeType(query.getNodeType());
        }
        List<BpmTaskModelEntity> allTasks = bpmProcessService.listUserTaskModels(taskModelQuery);
        if (CollectionUtils.isEmpty(allTasks)) {
            return new HashMap<>(2);
        }

        return allTasks.stream().filter(item -> taskKeysSet.contains(item.getTaskDefKey()))
                .collect(Collectors.toMap(BpmTaskModelEntity::getTaskDefKey, a -> a, (k1, k2) -> k1));
    }

    /**
     * 递归获取某个节点前面的所有相关节点
     *
     * @param incomingFlows  相邻来源节点引用集合
     * @param resultTasks    最后输出的 List结果集
     * @param resultTaskMap  最后输出的 Map结果集
     * @param flowElementMap Process # getFlowElementMap()
     * @param query          任务查询条件
     * @param isOneBack      是否一条线上只找一个符合条件的任务
     */
    private void loopIncomingFlows(
            List<SequenceFlow> incomingFlows,
            List<UserTask> resultTasks,
            Map<String, UserTask> resultTaskMap,
            Map<String, FlowElement> flowElementMap,
            BpmBackTaskModelQuery query,
            boolean isOneBack, Set<SequenceFlow> visited) {
        if (CollectionUtils.isEmpty(incomingFlows)) {
            return;
        }
        if (visited == null) {
            visited = new HashSet<>();
        }

        for (SequenceFlow item : incomingFlows) {

            if (visited.contains(item)) {
                continue;
            }
            visited.add(item);
            FlowElement flowElement = flowElementMap.get(item.getSourceRef());
            if (flowElement instanceof FlowNode) {
                if (flowElement instanceof UserTask) {
                    UserTask task = (UserTask) flowElement;
                    boolean check = !StringUtils.isNotBlank(query.getCategory()) || query.getCategory().equals(task.getCategory());
                    if (StringUtils.isNotBlank(query.getNodeType()) && !query.getNodeType().equals(UserTaskAttrUtil.getAttr(task, UserTaskAttrUtil.NODE_TYPE_KEY))) {
                        check = false;
                    }
                    if (check && !resultTaskMap.containsKey(task.getId())) {
                        resultTasks.add(task);
                        resultTaskMap.put(task.getId(), task);

                        if (isOneBack) {
                            continue;
                        }
                    }
                }
                loopIncomingFlows(((FlowNode) flowElement).getIncomingFlows(),
                        resultTasks, resultTaskMap, flowElementMap, query, isOneBack,visited);
            }
        }
    }

    protected void insertExecutionTest(String gatewayId, String processInstanceId, String processDefinitionId, String tenantId) {
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
        //executionEntityManager.insert(executionEntity);
    }

    /**
     * 场景一、由于多实例（串行）减签操作有异常数据产生，所以该方法用于删除部分异常数据，flowable BUG
     * 场景二、并行网关中的任务跳转到其他并行网关中，需要先删除一些本网关其他任务，然后跳转
     *
     * @param taskId 并行网关上的任务id 或 多实例任务id
     */
    protected void deleteExecutionByTaskId(String taskId) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            return;
        }
        String executionId = task.getExecutionId();
        logger.info("删除executionId=" + executionId);
        runtimeService.createNativeExecutionQuery().sql(
                "DELETE FROM act_hi_identitylink  WHERE TASK_ID_ = '" + taskId + "'").list();
        runtimeService.createNativeExecutionQuery().sql(
                "DELETE FROM act_ru_identitylink  WHERE TASK_ID_ = '" + taskId + "'").list();

        deleteExecutionById(executionId);

    }

    /**
     * 场景一、由于多实例（串行）减签操作有异常数据产生，所以该方法用于删除部分异常数据，flowable BUG
     * 场景二、并行网关中的任务跳转到其他并行网关中，需要先删除一些本网关其他任务，然后跳转
     *
     * @param executionId 并行网关上的任务对应的executionId
     */
    protected void deleteExecutionById(String executionId) {
        logger.info("删除executionId=" + executionId);
        runtimeService.createNativeExecutionQuery().sql(
                "DELETE FROM act_hi_actinst  WHERE EXECUTION_ID_ = '" + executionId + "'").list();
        runtimeService.createNativeExecutionQuery().sql(
                "DELETE FROM act_hi_taskinst  WHERE EXECUTION_ID_ = '" + executionId + "'").list();
        runtimeService.createNativeExecutionQuery().sql(
                "DELETE FROM act_hi_varinst  WHERE EXECUTION_ID_ = '" + executionId + "'").list();
        runtimeService.createNativeExecutionQuery().sql(
                "DELETE FROM act_ru_suspended_job  WHERE EXECUTION_ID_ = '" + executionId + "'").list();
        runtimeService.createNativeExecutionQuery().sql(
                "DELETE FROM act_ru_deadletter_job  WHERE EXECUTION_ID_ = '" + executionId + "'").list();
        runtimeService.createNativeExecutionQuery().sql(
                "DELETE FROM act_ru_timer_job  WHERE EXECUTION_ID_ = '" + executionId + "'").list();
        runtimeService.createNativeExecutionQuery().sql(
                "DELETE FROM act_ru_actinst  WHERE EXECUTION_ID_ = '" + executionId + "'").list();
        runtimeService.createNativeExecutionQuery().sql(
                "DELETE FROM act_ru_task  WHERE EXECUTION_ID_ = '" + executionId + "'").list();
        runtimeService.createNativeExecutionQuery().sql(
                "DELETE FROM act_ru_variable  WHERE EXECUTION_ID_ = '" + executionId + "'").list();
        runtimeService.createNativeExecutionQuery().sql(
                "DELETE FROM act_ru_execution  WHERE ID_ = '" + executionId + "'").list();

    }
}

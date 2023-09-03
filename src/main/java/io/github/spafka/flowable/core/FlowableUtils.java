package io.github.spafka.flowable.core;

import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.*;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.impl.bpmn.behavior.ParallelMultiInstanceBehavior;
import org.flowable.engine.impl.bpmn.behavior.SequentialMultiInstanceBehavior;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.util.ProcessDefinitionUtil;
import org.flowable.task.api.history.HistoricTaskInstance;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Tony
 * @date 2021-04-03 23:57
 */
@Slf4j
public class FlowableUtils {

    /**
     * 根据节点，获取入口连线
     *
     * @param source
     * @return
     */
    public static List<SequenceFlow> getElementIncomingFlows(FlowElement source) {
        List<SequenceFlow> sequenceFlows = null;
        if (source instanceof FlowNode) {
            sequenceFlows = ((FlowNode) source).getIncomingFlows();
        } else if (source instanceof Gateway) {
            sequenceFlows = ((Gateway) source).getIncomingFlows();
        } else if (source instanceof SubProcess) {
            sequenceFlows = ((SubProcess) source).getIncomingFlows();
        } else if (source instanceof StartEvent) {
            sequenceFlows = ((StartEvent) source).getIncomingFlows();
        } else if (source instanceof EndEvent) {
            sequenceFlows = ((EndEvent) source).getIncomingFlows();
        }
        return sequenceFlows;
    }

    /**
     * 根据节点，获取出口连线
     *
     * @param source
     * @return
     */
    public static List<SequenceFlow> getElementOutgoingFlows(FlowElement source) {
        List<SequenceFlow> sequenceFlows = null;
        if (source instanceof FlowNode) {
            sequenceFlows = ((FlowNode) source).getOutgoingFlows();
        } else if (source instanceof Gateway) {
            sequenceFlows = ((Gateway) source).getOutgoingFlows();
        } else if (source instanceof SubProcess) {
            sequenceFlows = ((SubProcess) source).getOutgoingFlows();
        } else if (source instanceof StartEvent) {
            sequenceFlows = ((StartEvent) source).getOutgoingFlows();
        } else if (source instanceof EndEvent) {
            sequenceFlows = ((EndEvent) source).getOutgoingFlows();
        }
        return sequenceFlows;
    }

    /**
     * 获取全部节点列表，包含子流程节点
     *
     * @param flowElements
     * @param allElements
     * @return
     */
    public static Collection<FlowElement> getAllElements(Collection<FlowElement> flowElements, Collection<FlowElement> allElements) {
        allElements = allElements == null ? new ArrayList<>() : allElements;

        for (FlowElement flowElement : flowElements) {
            allElements.add(flowElement);
            if (flowElement instanceof SubProcess) {
                // 继续深入子流程，进一步获取子流程
                allElements = FlowableUtils.getAllElements(((SubProcess) flowElement).getFlowElements(), allElements);
            }
        }
        return allElements;
    }

    /**
     * 迭代获取父级任务节点列表，向前找
     *
     * @param source          起始节点
     * @param hasSequenceFlow 已经经过的连线的 ID，用于判断线路是否重复
     * @param userTaskList    已找到的用户任务节点
     * @return
     */
    public static List<UserTask> iteratorFindParentUserTasks(FlowElement source, Set<String> hasSequenceFlow, List<UserTask> userTaskList) {
        userTaskList = userTaskList == null ? new ArrayList<>() : userTaskList;
        hasSequenceFlow = hasSequenceFlow == null ? new HashSet<>() : hasSequenceFlow;

        // 如果该节点为开始节点，且存在上级子节点，则顺着上级子节点继续迭代
        if (source instanceof StartEvent && source.getSubProcess() != null) {
            userTaskList = iteratorFindParentUserTasks(source.getSubProcess(), hasSequenceFlow, userTaskList);
        }

        // 根据类型，获取入口连线
        List<SequenceFlow> sequenceFlows = getElementIncomingFlows(source);

        if (sequenceFlows != null) {
            // 循环找到目标元素
            for (SequenceFlow sequenceFlow : sequenceFlows) {
                // 如果发现连线重复，说明循环了，跳过这个循环
                if (hasSequenceFlow.contains(sequenceFlow.getId())) {
                    continue;
                }
                // 添加已经走过的连线
                hasSequenceFlow.add(sequenceFlow.getId());
                // 类型为用户节点，则新增父级节点
                if (sequenceFlow.getSourceFlowElement() instanceof UserTask) {
                    userTaskList.add((UserTask) sequenceFlow.getSourceFlowElement());
                    continue;
                }
                // 类型为子流程，则添加子流程开始节点出口处相连的节点
                if (sequenceFlow.getSourceFlowElement() instanceof SubProcess) {
                    // 获取子流程用户任务节点
                    List<UserTask> childUserTaskList = findChildProcessUserTasks((StartEvent) ((SubProcess) sequenceFlow.getSourceFlowElement()).getFlowElements().toArray()[0], null, null);
                    // 如果找到节点，则说明该线路找到节点，不继续向下找，反之继续
                    if (childUserTaskList != null && childUserTaskList.size() > 0) {
                        userTaskList.addAll(childUserTaskList);
                        continue;
                    }
                }
                // 继续迭代
                userTaskList = iteratorFindParentUserTasks(sequenceFlow.getSourceFlowElement(), hasSequenceFlow, userTaskList);
            }
        }
        return userTaskList;
    }

    /**
     * 根据正在运行的任务节点，迭代获取子级任务节点列表，向后找
     *
     * @param source          起始节点(退回节点)
     * @param runTaskKeyList  正在运行的任务 Key，用于校验任务节点是否是正在运行的节点
     * @param hasSequenceFlow 已经经过的连线的 ID，用于判断线路是否重复
     * @param userTaskList    需要撤回的用户任务列表
     * @return
     */
    public static List<UserTask> iteratorFindChildUserTasks(FlowElement source, List<String> runTaskKeyList, Set<String> hasSequenceFlow, List<UserTask> userTaskList) {
        hasSequenceFlow = hasSequenceFlow == null ? new HashSet<>() : hasSequenceFlow;
        userTaskList = userTaskList == null ? new ArrayList<>() : userTaskList;

        // 如果该节点为开始节点，且存在上级子节点，则顺着上级子节点继续迭代
        if (source instanceof EndEvent && source.getSubProcess() != null) {
            userTaskList = iteratorFindChildUserTasks(source.getSubProcess(), runTaskKeyList, hasSequenceFlow, userTaskList);
        }

        // 根据类型，获取出口连线
        List<SequenceFlow> sequenceFlows = getElementOutgoingFlows(source);

        if (sequenceFlows != null) {
            // 循环找到目标元素
            for (SequenceFlow sequenceFlow : sequenceFlows) {
                // 如果发现连线重复，说明循环了，跳过这个循环
                if (hasSequenceFlow.contains(sequenceFlow.getId())) {
                    continue;
                }
                // 添加已经走过的连线
                hasSequenceFlow.add(sequenceFlow.getId());
                // 如果为用户任务类型，且任务节点的 Key 正在运行的任务中存在，添加
                if (sequenceFlow.getTargetFlowElement() instanceof UserTask && runTaskKeyList.contains((sequenceFlow.getTargetFlowElement()).getId())) {
                    userTaskList.add((UserTask) sequenceFlow.getTargetFlowElement());
                    continue;
                }
                // 如果节点为子流程节点情况，则从节点中的第一个节点开始获取
                if (sequenceFlow.getTargetFlowElement() instanceof SubProcess) {
                    List<UserTask> childUserTaskList = iteratorFindChildUserTasks((FlowElement) (((SubProcess) sequenceFlow.getTargetFlowElement()).getFlowElements().toArray()[0]), runTaskKeyList, hasSequenceFlow, null);
                    // 如果找到节点，则说明该线路找到节点，不继续向下找，反之继续
                    if (childUserTaskList != null && childUserTaskList.size() > 0) {
                        userTaskList.addAll(childUserTaskList);
                        continue;
                    }
                }
                // 继续迭代
                userTaskList = iteratorFindChildUserTasks(sequenceFlow.getTargetFlowElement(), runTaskKeyList, hasSequenceFlow, userTaskList);
            }
        }
        return userTaskList;
    }

    /**
     * 迭代获取子流程用户任务节点
     *
     * @param source          起始节点
     * @param hasSequenceFlow 已经经过的连线的 ID，用于判断线路是否重复
     * @param userTaskList    需要撤回的用户任务列表
     * @return
     */
    public static List<UserTask> findChildProcessUserTasks(FlowElement source, Set<String> hasSequenceFlow, List<UserTask> userTaskList) {
        hasSequenceFlow = hasSequenceFlow == null ? new HashSet<>() : hasSequenceFlow;
        userTaskList = userTaskList == null ? new ArrayList<>() : userTaskList;

        // 根据类型，获取出口连线
        List<SequenceFlow> sequenceFlows = getElementOutgoingFlows(source);

        if (sequenceFlows != null) {
            // 循环找到目标元素
            for (SequenceFlow sequenceFlow : sequenceFlows) {
                // 如果发现连线重复，说明循环了，跳过这个循环
                if (hasSequenceFlow.contains(sequenceFlow.getId())) {
                    continue;
                }
                // 添加已经走过的连线
                hasSequenceFlow.add(sequenceFlow.getId());
                // 如果为用户任务类型，且任务节点的 Key 正在运行的任务中存在，添加
                if (sequenceFlow.getTargetFlowElement() instanceof UserTask) {
                    userTaskList.add((UserTask) sequenceFlow.getTargetFlowElement());
                    continue;
                }
                // 如果节点为子流程节点情况，则从节点中的第一个节点开始获取
                if (sequenceFlow.getTargetFlowElement() instanceof SubProcess) {
                    List<UserTask> childUserTaskList = findChildProcessUserTasks((FlowElement) (((SubProcess) sequenceFlow.getTargetFlowElement()).getFlowElements().toArray()[0]), hasSequenceFlow, null);
                    // 如果找到节点，则说明该线路找到节点，不继续向下找，反之继续
                    if (childUserTaskList != null && childUserTaskList.size() > 0) {
                        userTaskList.addAll(childUserTaskList);
                        continue;
                    }
                }
                // 继续迭代
                userTaskList = findChildProcessUserTasks(sequenceFlow.getTargetFlowElement(), hasSequenceFlow, userTaskList);
            }
        }
        return userTaskList;
    }

    /**
     * 从后向前寻路，获取所有脏线路上的点
     *
     * @param source          起始节点
     * @param passRoads       已经经过的点集合
     * @param hasSequenceFlow 已经经过的连线的 ID，用于判断线路是否重复
     * @param targets         目标脏线路终点
     * @param dirtyRoads      确定为脏数据的点，因为不需要重复，因此使用 set 存储
     * @return
     */
    public static Set<String> iteratorFindDirtyRoads(FlowElement source, List<String> passRoads, Set<String> hasSequenceFlow, List<String> targets, Set<String> dirtyRoads) {
        passRoads = passRoads == null ? new ArrayList<>() : passRoads;
        dirtyRoads = dirtyRoads == null ? new HashSet<>() : dirtyRoads;
        hasSequenceFlow = hasSequenceFlow == null ? new HashSet<>() : hasSequenceFlow;

        // 如果该节点为开始节点，且存在上级子节点，则顺着上级子节点继续迭代
        if (source instanceof StartEvent && source.getSubProcess() != null) {
            dirtyRoads = iteratorFindDirtyRoads(source.getSubProcess(), passRoads, hasSequenceFlow, targets, dirtyRoads);
        }

        // 根据类型，获取入口连线
        List<SequenceFlow> sequenceFlows = getElementIncomingFlows(source);

        if (sequenceFlows != null) {
            // 循环找到目标元素
            for (SequenceFlow sequenceFlow : sequenceFlows) {
                // 如果发现连线重复，说明循环了，跳过这个循环
                if (hasSequenceFlow.contains(sequenceFlow.getId())) {
                    continue;
                }
                // 添加已经走过的连线
                hasSequenceFlow.add(sequenceFlow.getId());
                // 新增经过的路线
                passRoads.add(sequenceFlow.getSourceFlowElement().getId());
                // 如果此点为目标点，确定经过的路线为脏线路，添加点到脏线路中，然后找下个连线
                if (targets.contains(sequenceFlow.getSourceFlowElement().getId())) {
                    dirtyRoads.addAll(passRoads);
                    continue;
                }
                // 如果该节点为开始节点，且存在上级子节点，则顺着上级子节点继续迭代
                if (sequenceFlow.getSourceFlowElement() instanceof SubProcess) {
                    dirtyRoads = findChildProcessAllDirtyRoad((StartEvent) ((SubProcess) sequenceFlow.getSourceFlowElement()).getFlowElements().toArray()[0], null, dirtyRoads);
                    // 是否存在子流程上，true 是，false 否
                    Boolean isInChildProcess = dirtyTargetInChildProcess((StartEvent) ((SubProcess) sequenceFlow.getSourceFlowElement()).getFlowElements().toArray()[0], null, targets, null);
                    if (isInChildProcess) {
                        // 已在子流程上找到，该路线结束
                        continue;
                    }
                }
                // 继续迭代
                dirtyRoads = iteratorFindDirtyRoads(sequenceFlow.getSourceFlowElement(), passRoads, hasSequenceFlow, targets, dirtyRoads);
            }
        }
        return dirtyRoads;
    }

    /**
     * 迭代获取子流程脏路线
     * 说明，假如回退的点就是子流程，那么也肯定会回退到子流程最初的用户任务节点，因此子流程中的节点全是脏路线
     *
     * @param source          起始节点
     * @param hasSequenceFlow 已经经过的连线的 ID，用于判断线路是否重复
     * @param dirtyRoads      确定为脏数据的点，因为不需要重复，因此使用 set 存储
     * @return
     */
    public static Set<String> findChildProcessAllDirtyRoad(FlowElement source, Set<String> hasSequenceFlow, Set<String> dirtyRoads) {
        hasSequenceFlow = hasSequenceFlow == null ? new HashSet<>() : hasSequenceFlow;
        dirtyRoads = dirtyRoads == null ? new HashSet<>() : dirtyRoads;

        // 根据类型，获取出口连线
        List<SequenceFlow> sequenceFlows = getElementOutgoingFlows(source);

        if (sequenceFlows != null) {
            // 循环找到目标元素
            for (SequenceFlow sequenceFlow : sequenceFlows) {
                // 如果发现连线重复，说明循环了，跳过这个循环
                if (hasSequenceFlow.contains(sequenceFlow.getId())) {
                    continue;
                }
                // 添加已经走过的连线
                hasSequenceFlow.add(sequenceFlow.getId());
                // 添加脏路线
                dirtyRoads.add(sequenceFlow.getTargetFlowElement().getId());
                // 如果节点为子流程节点情况，则从节点中的第一个节点开始获取
                if (sequenceFlow.getTargetFlowElement() instanceof SubProcess) {
                    dirtyRoads = findChildProcessAllDirtyRoad((FlowElement) (((SubProcess) sequenceFlow.getTargetFlowElement()).getFlowElements().toArray()[0]), hasSequenceFlow, dirtyRoads);
                }
                // 继续迭代
                dirtyRoads = findChildProcessAllDirtyRoad(sequenceFlow.getTargetFlowElement(), hasSequenceFlow, dirtyRoads);
            }
        }
        return dirtyRoads;
    }

    /**
     * 判断脏路线结束节点是否在子流程上
     *
     * @param source          起始节点
     * @param hasSequenceFlow 已经经过的连线的 ID，用于判断线路是否重复
     * @param targets         判断脏路线节点是否存在子流程上，只要存在一个，说明脏路线只到子流程为止
     * @param inChildProcess  是否存在子流程上，true 是，false 否
     * @return
     */
    public static Boolean dirtyTargetInChildProcess(FlowElement source, Set<String> hasSequenceFlow, List<String> targets, Boolean inChildProcess) {
        hasSequenceFlow = hasSequenceFlow == null ? new HashSet<>() : hasSequenceFlow;
        inChildProcess = inChildProcess != null && inChildProcess;

        // 根据类型，获取出口连线
        List<SequenceFlow> sequenceFlows = getElementOutgoingFlows(source);

        if (sequenceFlows != null && !inChildProcess) {
            // 循环找到目标元素
            for (SequenceFlow sequenceFlow : sequenceFlows) {
                // 如果发现连线重复，说明循环了，跳过这个循环
                if (hasSequenceFlow.contains(sequenceFlow.getId())) {
                    continue;
                }
                // 添加已经走过的连线
                hasSequenceFlow.add(sequenceFlow.getId());
                // 如果发现目标点在子流程上存在，说明只到子流程为止
                if (targets.contains(sequenceFlow.getTargetFlowElement().getId())) {
                    inChildProcess = true;
                    break;
                }
                // 如果节点为子流程节点情况，则从节点中的第一个节点开始获取
                if (sequenceFlow.getTargetFlowElement() instanceof SubProcess) {
                    inChildProcess = dirtyTargetInChildProcess((FlowElement) (((SubProcess) sequenceFlow.getTargetFlowElement()).getFlowElements().toArray()[0]), hasSequenceFlow, targets, inChildProcess);
                }
                // 继续迭代
                inChildProcess = dirtyTargetInChildProcess(sequenceFlow.getTargetFlowElement(), hasSequenceFlow, targets, inChildProcess);
            }
        }
        return inChildProcess;
    }

    /**
     * 迭代从后向前扫描，判断目标节点相对于当前节点是否是串行
     * 不存在直接回退到子流程中的情况，但存在从子流程出去到父流程情况
     *
     * @param source          起始节点
     * @param isSequential    是否串行
     * @param hasSequenceFlow 已经经过的连线的 ID，用于判断线路是否重复
     * @param targetKsy       目标节点
     * @return
     */
    public static Boolean iteratorCheckSequentialReferTarget(FlowElement source, String targetKsy, Set<String> hasSequenceFlow, Boolean isSequential) {
        isSequential = isSequential == null || isSequential;
        hasSequenceFlow = hasSequenceFlow == null ? new HashSet<>() : hasSequenceFlow;

        // 如果该节点为开始节点，且存在上级子节点，则顺着上级子节点继续迭代
        if (source instanceof StartEvent && source.getSubProcess() != null) {
            isSequential = iteratorCheckSequentialReferTarget(source.getSubProcess(), targetKsy, hasSequenceFlow, isSequential);
        }

        // 根据类型，获取入口连线
        List<SequenceFlow> sequenceFlows = getElementIncomingFlows(source);

        if (sequenceFlows != null) {
            // 循环找到目标元素
            for (SequenceFlow sequenceFlow : sequenceFlows) {
                // 如果发现连线重复，说明循环了，跳过这个循环
                if (hasSequenceFlow.contains(sequenceFlow.getId())) {
                    continue;
                }
                // 添加已经走过的连线
                hasSequenceFlow.add(sequenceFlow.getId());
                // 如果目标节点已被判断为并行，后面都不需要执行，直接返回
                if (!isSequential) {
                    break;
                }
                // 这条线路存在目标节点，这条线路完成，进入下个线路
                if (targetKsy.equals(sequenceFlow.getSourceFlowElement().getId())) {
                    continue;
                }
                if (sequenceFlow.getSourceFlowElement() instanceof StartEvent) {
                    isSequential = false;
                    break;
                }
                // 否则就继续迭代
                isSequential = iteratorCheckSequentialReferTarget(sequenceFlow.getSourceFlowElement(), targetKsy, hasSequenceFlow, isSequential);
            }
        }
        return isSequential;
    }

    /**
     * 从后向前寻路，获取到达节点的所有路线
     * 不存在直接回退到子流程，但是存在回退到父级流程的情况
     *
     * @param source    起始节点
     * @param passRoads 已经经过的点集合
     * @param roads     路线
     * @return
     */
    public static List<List<UserTask>> findRoad(FlowElement source, List<UserTask> passRoads, Set<String> hasSequenceFlow, List<List<UserTask>> roads) {
        passRoads = passRoads == null ? new ArrayList<>() : passRoads;
        roads = roads == null ? new ArrayList<>() : roads;
        hasSequenceFlow = hasSequenceFlow == null ? new HashSet<>() : hasSequenceFlow;

        // 如果该节点为开始节点，且存在上级子节点，则顺着上级子节点继续迭代
        if (source instanceof StartEvent && source.getSubProcess() != null) {
            roads = findRoad(source.getSubProcess(), passRoads, hasSequenceFlow, roads);
        }

        // 根据类型，获取入口连线
        List<SequenceFlow> sequenceFlows = getElementIncomingFlows(source);

        if (sequenceFlows != null && !sequenceFlows.isEmpty()) {
            for (SequenceFlow sequenceFlow : sequenceFlows) {
                // 如果发现连线重复，说明循环了，跳过这个循环
                if (hasSequenceFlow.contains(sequenceFlow.getId())) {
                    continue;
                }
                // 添加已经走过的连线
                hasSequenceFlow.add(sequenceFlow.getId());
                // 添加经过路线
                if (sequenceFlow.getSourceFlowElement() instanceof UserTask) {
                    passRoads.add((UserTask) sequenceFlow.getSourceFlowElement());
                }
                // 继续迭代
                roads = findRoad(sequenceFlow.getSourceFlowElement(), passRoads, hasSequenceFlow, roads);
            }
        } else {
            // 添加路线
            roads.add(passRoads);
        }
        return roads;
    }

    /**
     * 历史节点数据清洗，清洗掉又回滚导致的脏数据
     *
     * @param allElements              全部节点信息
     * @param historicTaskInstanceList 历史任务实例信息，数据采用开始时间升序
     * @return
     */
    public static List<String> historicTaskInstanceClean(Collection<FlowElement> allElements, List<HistoricTaskInstance> historicTaskInstanceList) {
        // 会签节点收集
        List<String> multiTask = new ArrayList<>();
        allElements.forEach(flowElement -> {
            if (flowElement instanceof UserTask) {
                // 如果该节点的行为为会签行为，说明该节点为会签节点
                if (((UserTask) flowElement).getBehavior() instanceof ParallelMultiInstanceBehavior || ((UserTask) flowElement).getBehavior() instanceof SequentialMultiInstanceBehavior) {
                    multiTask.add(flowElement.getId());
                }
            }
        });
        // 循环放入栈，栈 LIFO：后进先出
        Stack<HistoricTaskInstance> stack = new Stack<>();
        historicTaskInstanceList.forEach(stack::push);
        // 清洗后的历史任务实例
        List<String> lastHistoricTaskInstanceList = new ArrayList<>();
        // 网关存在可能只走了部分分支情况，且还存在跳转废弃数据以及其他分支数据的干扰，因此需要对历史节点数据进行清洗
        // 临时用户任务 key
        StringBuilder userTaskKey = null;
        // 临时被删掉的任务 key，存在并行情况
        List<String> deleteKeyList = new ArrayList<>();
        // 临时脏数据线路
        List<Set<String>> dirtyDataLineList = new ArrayList<>();
        // 由某个点跳到会签点,此时出现多个会签实例对应 1 个跳转情况，需要把这些连续脏数据都找到
        // 会签特殊处理下标
        int multiIndex = -1;
        // 会签特殊处理 key
        StringBuilder multiKey = null;
        // 会签特殊处理操作标识
        boolean multiOpera = false;
        while (!stack.empty()) {
            // 从这里开始 userTaskKey 都还是上个栈的 key
            // 是否是脏数据线路上的点
            final boolean[] isDirtyData = {false};
            for (Set<String> oldDirtyDataLine : dirtyDataLineList) {
                if (oldDirtyDataLine.contains(stack.peek().getTaskDefinitionKey())) {
                    isDirtyData[0] = true;
                }
            }
            // 删除原因不为空，说明从这条数据开始回跳或者回退的
            // MI_END：会签完成后，其他未签到节点的删除原因，不在处理范围内
            if (stack.peek().getDeleteReason() != null && !"MI_END".equals(stack.peek().getDeleteReason())) {
                // 可以理解为脏线路起点
                String dirtyPoint = "";
                if (stack.peek().getDeleteReason().contains("Change activity to ")) {
                    dirtyPoint = stack.peek().getDeleteReason().replace("Change activity to ", "");
                }
                // 会签回退删除原因有点不同
                if (stack.peek().getDeleteReason().contains("Change parent activity to ")) {
                    dirtyPoint = stack.peek().getDeleteReason().replace("Change parent activity to ", "");
                }
                FlowElement dirtyTask = null;
                // 获取变更节点的对应的入口处连线
                // 如果是网关并行回退情况，会变成两条脏数据路线，效果一样
                for (FlowElement flowElement : allElements) {
                    if (flowElement.getId().equals(stack.peek().getTaskDefinitionKey())) {
                        dirtyTask = flowElement;
                    }
                }
                // 获取脏数据线路
                Set<String> dirtyDataLine = FlowableUtils.iteratorFindDirtyRoads(dirtyTask, null, null, Arrays.asList(dirtyPoint.split(",")), null);
                // 自己本身也是脏线路上的点，加进去
                dirtyDataLine.add(stack.peek().getTaskDefinitionKey());
                log.info(stack.peek().getTaskDefinitionKey() + "点脏路线集合：" + dirtyDataLine);
                // 是全新的需要添加的脏线路
                boolean isNewDirtyData = true;
                for (int i = 0; i < dirtyDataLineList.size(); i++) {
                    // 如果发现他的上个节点在脏线路内，说明这个点可能是并行的节点，或者连续驳回
                    // 这时，都以之前的脏线路节点为标准，只需合并脏线路即可，也就是路线补全
                    if (dirtyDataLineList.get(i).contains(userTaskKey.toString())) {
                        isNewDirtyData = false;
                        dirtyDataLineList.get(i).addAll(dirtyDataLine);
                    }
                }
                // 已确定时全新的脏线路
                if (isNewDirtyData) {
                    // deleteKey 单一路线驳回到并行，这种同时生成多个新实例记录情况，这时 deleteKey 其实是由多个值组成
                    // 按照逻辑，回退后立刻生成的实例记录就是回退的记录
                    // 至于驳回所生成的 Key，直接从删除原因中获取，因为存在驳回到并行的情况
                    deleteKeyList.add(dirtyPoint + ",");
                    dirtyDataLineList.add(dirtyDataLine);
                }
                // 添加后，现在这个点变成脏线路上的点了
                isDirtyData[0] = true;
            }
            // 如果不是脏线路上的点，说明是有效数据，添加历史实例 Key
            if (!isDirtyData[0]) {
                lastHistoricTaskInstanceList.add(stack.peek().getTaskDefinitionKey());
            }
            // 校验脏线路是否结束
            for (int i = 0; i < deleteKeyList.size(); i++) {
                // 如果发现脏数据属于会签，记录下下标与对应 Key，以备后续比对，会签脏数据范畴开始
                if (multiKey == null && multiTask.contains(stack.peek().getTaskDefinitionKey())
                        && deleteKeyList.get(i).contains(stack.peek().getTaskDefinitionKey())) {
                    multiIndex = i;
                    multiKey = new StringBuilder(stack.peek().getTaskDefinitionKey());
                }
                // 会签脏数据处理，节点退回会签清空
                // 如果在会签脏数据范畴中发现 Key改变，说明会签脏数据在上个节点就结束了，可以把会签脏数据删掉
                if (multiKey != null && !multiKey.toString().equals(stack.peek().getTaskDefinitionKey())) {
                    deleteKeyList.set(multiIndex, deleteKeyList.get(multiIndex).replace(stack.peek().getTaskDefinitionKey() + ",", ""));
                    multiKey = null;
                    // 结束进行下校验删除
                    multiOpera = true;
                }
                // 其他脏数据处理
                // 发现该路线最后一条脏数据，说明这条脏数据线路处理完了，删除脏数据信息
                // 脏数据产生的新实例中是否包含这条数据
                if (multiKey == null && deleteKeyList.get(i).contains(stack.peek().getTaskDefinitionKey())) {
                    // 删除匹配到的部分
                    deleteKeyList.set(i, deleteKeyList.get(i).replace(stack.peek().getTaskDefinitionKey() + ",", ""));
                }
                // 如果每组中的元素都以匹配过，说明脏数据结束
                if ("".equals(deleteKeyList.get(i))) {
                    // 同时删除脏数据
                    deleteKeyList.remove(i);
                    dirtyDataLineList.remove(i);
                    break;
                }
            }
            // 会签数据处理需要在循环外处理，否则可能导致溢出
            // 会签的数据肯定是之前放进去的所以理论上不会溢出，但还是校验下
            if (multiOpera && deleteKeyList.size() > multiIndex && "".equals(deleteKeyList.get(multiIndex))) {
                // 同时删除脏数据
                deleteKeyList.remove(multiIndex);
                dirtyDataLineList.remove(multiIndex);
                multiIndex = -1;
                multiOpera = false;
            }
            // pop() 方法与 peek() 方法不同，在返回值的同时，会把值从栈中移除
            // 保存新的 userTaskKey 在下个循环中使用
            userTaskKey = new StringBuilder(stack.pop().getTaskDefinitionKey());
        }
        log.info("清洗后的历史节点数据：" + lastHistoricTaskInstanceList);
        return lastHistoricTaskInstanceList;
    }

    public static <T> Map<String, List<T>> groupListContentBy(List<T> source, Function<T, String> classifier) {
        return source.stream().collect(Collectors.groupingBy(classifier));
    }

    public static Map<String, FlowNode> getCanReachTo(FlowNode toFlowNode) {
        return getCanReachTo(toFlowNode, null);
    }

    public static Map<String, FlowNode> getCanReachTo(FlowNode toFlowNode, Map<String, FlowNode> canReachToNodes) {
        if (canReachToNodes == null) {
            canReachToNodes = new HashMap<>(16);
        }
        List<SequenceFlow> flows = toFlowNode.getIncomingFlows();
        if (flows != null && flows.size() > 0) {
            for (SequenceFlow sequenceFlow : flows) {
                FlowElement sourceFlowElement = sequenceFlow.getSourceFlowElement();
                if (sourceFlowElement instanceof FlowNode) {
                    canReachToNodes.put(sourceFlowElement.getId(), (FlowNode) sourceFlowElement);
                    if (sourceFlowElement instanceof SubProcess) {
                        for (Map.Entry<String, FlowElement> entry :
                                ((SubProcess) sourceFlowElement).getFlowElementMap().entrySet()) {
                            if (entry.getValue() instanceof FlowNode) {
                                FlowNode flowNodeV = (FlowNode) entry.getValue();
                                canReachToNodes.put(entry.getKey(), flowNodeV);
                            }
                        }
                    }
                    getCanReachTo((FlowNode) sourceFlowElement, canReachToNodes);
                }
            }
        }
        if (toFlowNode.getSubProcess() != null) {
            getCanReachTo(toFlowNode.getSubProcess(), canReachToNodes);
        }
        return canReachToNodes;
    }

    public static Map<String, FlowNode> getCanReachFrom(FlowNode fromFlowNode) {
        return getCanReachFrom(fromFlowNode, null);
    }

    public static Map<String, FlowNode> getCanReachFrom(FlowNode fromFlowNode,
                                                        Map<String, FlowNode> canReachFromNodes) {
        if (canReachFromNodes == null) {
            canReachFromNodes = new HashMap<>(16);
        }
        List<SequenceFlow> flows = fromFlowNode.getOutgoingFlows();
        if (flows != null && flows.size() > 0) {
            for (SequenceFlow sequenceFlow : flows) {
                FlowElement targetFlowElement = sequenceFlow.getTargetFlowElement();
                if (targetFlowElement instanceof FlowNode) {
                    canReachFromNodes.put(targetFlowElement.getId(), (FlowNode) targetFlowElement);
                    if (targetFlowElement instanceof SubProcess) {
                        for (Map.Entry<String, FlowElement> entry :
                                ((SubProcess) targetFlowElement).getFlowElementMap().entrySet()) {
                            if (entry.getValue() instanceof FlowNode) {
                                FlowNode flowNodeV = (FlowNode) entry.getValue();
                                canReachFromNodes.put(entry.getKey(), flowNodeV);
                            }
                        }
                    }
                    getCanReachFrom((FlowNode) targetFlowElement, canReachFromNodes);
                }
            }
        }
        if (fromFlowNode.getSubProcess() != null) {
            getCanReachFrom(fromFlowNode.getSubProcess(), canReachFromNodes);
        }
        return canReachFromNodes;
    }

    public static Map<String, Set<String>> getSpecialGatewayElements(FlowElementsContainer container) {
        return getSpecialGatewayElements(container, null);
    }

    public static Map<String, Set<String>> getSpecialGatewayElements(FlowElementsContainer container, Map<String,
            Set<String>> specialGatewayElements) {
        if (specialGatewayElements == null) {
            specialGatewayElements = new HashMap<>(16);
        }
        Collection<FlowElement> flowelements = container.getFlowElements();
        for (FlowElement flowElement : flowelements) {
            boolean isBeginSpecialGateway =
                    (flowElement instanceof ParallelGateway || flowElement instanceof InclusiveGateway || flowElement instanceof ComplexGateway) && ((Gateway) flowElement).getIncomingFlows().size() > 1;
            if (isBeginSpecialGateway) {
                String gatewayId = flowElement.getId();
                Set<String> gatewayIdContainFlowelements = specialGatewayElements.computeIfAbsent(gatewayId,
                        k -> new HashSet<>());
                findElementsBetweenSpecialGateway(flowElement,
                        gatewayId, gatewayIdContainFlowelements);
            } else if (flowElement instanceof SubProcess) {
                getSpecialGatewayElements((SubProcess) flowElement, specialGatewayElements);
            }
        }

        // 外层到里层排序
        Map<String, Set<String>> specialGatewayNodesSort = new LinkedHashMap<>();
        specialGatewayElements.entrySet().stream().sorted((o1, o2) -> o2.getValue().size() - o1.getValue().size()).forEach(entry -> specialGatewayNodesSort.put(entry.getKey(), entry.getValue()));

        return specialGatewayNodesSort;
    }

    public static void findElementsBetweenSpecialGateway(FlowElement specialGatewayBegin, String specialGatewayEndId,
                                                         Set<String> elements) {
        elements.add(specialGatewayBegin.getId());
        List<SequenceFlow> sequenceFlows = ((FlowNode) specialGatewayBegin).getOutgoingFlows();
        if (sequenceFlows != null && !sequenceFlows.isEmpty()) {
            for (SequenceFlow sequenceFlow : sequenceFlows) {
                FlowElement targetFlowElement = sequenceFlow.getTargetFlowElement();
                String targetFlowElementId = targetFlowElement.getId();
                elements.add(specialGatewayEndId);
                if (targetFlowElementId.equals(specialGatewayEndId)) {
                    continue;
                } else {
                    findElementsBetweenSpecialGateway(targetFlowElement, specialGatewayEndId, elements);
                }
            }
        }
    }

    /**
     * Verifies if the element with the given source identifier can reach the element with the target identifier through
     * following sequence flow.
     */
    public static boolean isReachable(Process process , String sourceElementId, String targetElementId) {
        // Fetch source and target elements

        FlowElement sourceFlowElement = process.getFlowElement(sourceElementId, true);
        FlowNode sourceElement = null;
        if (sourceFlowElement instanceof FlowNode) {
            sourceElement = (FlowNode) sourceFlowElement;
        } else if (sourceFlowElement instanceof SequenceFlow) {
            sourceElement = (FlowNode) ((SequenceFlow) sourceFlowElement).getTargetFlowElement();
        }
        FlowElement targetFlowElement = process.getFlowElement(targetElementId, true);
        FlowNode targetElement = null;
        if (targetFlowElement instanceof FlowNode) {
            targetElement = (FlowNode) targetFlowElement;
        } else if (targetFlowElement instanceof SequenceFlow) {
            targetElement = (FlowNode) ((SequenceFlow) targetFlowElement).getTargetFlowElement();
        }
        if (sourceElement == null) {
            throw new FlowableException("Invalid sourceElementId '" + sourceElementId + "': no element found for " +
                    "this" + " id n process definition '" + process.getId() + "'");
        }
        if (targetElement == null) {
            throw new FlowableException("Invalid targetElementId '" + targetElementId + "': no element found for " +
                    "this" + " id n process definition '" + process.getId() + "'");
        }
        Set<String> visitedElements = new HashSet<>();
        return isReachable(process, sourceElement, targetElement, visitedElements);
    }

    public static boolean isReachable(Process process, FlowNode sourceElement, FlowNode targetElement) {
        return isReachable(process, sourceElement, targetElement, Sets.newHashSet());
    }

    public static boolean isReachable(Process process, FlowNode sourceElement, FlowNode targetElement,
                                      Set<String> visitedElements) {
        // Special case: start events in an event subprocess might exist as an execution and are most likely be able to
        // reach the target
        // when the target is in the event subprocess, but should be ignored as they are not 'real' runtime executions
        // (but rather waiting for a
        // trigger)
        if (sourceElement instanceof StartEvent && isInEventSubprocess(sourceElement)) {
            return false;
        }
        // No outgoing seq flow: could be the end of eg . the process or an embedded subprocess
        if (sourceElement.getOutgoingFlows().size() == 0) {
            visitedElements.add(sourceElement.getId());
            FlowElementsContainer parentElement = process.findParent(sourceElement);
            if (parentElement instanceof SubProcess) {
                sourceElement = (SubProcess) parentElement;
                // by zjm begin
                // 子流程的结束节点，若目标节点在该子流程中，说明无法到达，返回false
                if (((SubProcess) sourceElement).getFlowElement(targetElement.getId()) != null) {
                    return false;
                }
                // by zjm end
            } else {
                return false;
            }
        }
        if (sourceElement.getId().equals(targetElement.getId())) {
            return true;
        }
        // To avoid infinite looping, we must capture every node we visit
        // and check before going further in the graph if we have already
        // visited the node.
        visitedElements.add(sourceElement.getId());
        // by zjm begin
        // 当前节点能够到达子流程，且目标节点在子流程中，说明可以到达，返回true
        if (sourceElement instanceof SubProcess && ((SubProcess) sourceElement).getFlowElement(targetElement.getId()) != null) {
            return true;
        }
        // by zjm end
        List<SequenceFlow> sequenceFlows = sourceElement.getOutgoingFlows();
        if (sequenceFlows != null && sequenceFlows.size() > 0) {
            for (SequenceFlow sequenceFlow : sequenceFlows) {
                String targetRef = sequenceFlow.getTargetRef();
                FlowNode sequenceFlowTarget = (FlowNode) process.getFlowElement(targetRef, true);
                if (sequenceFlowTarget != null && !visitedElements.contains(sequenceFlowTarget.getId())) {
                    boolean reachable = isReachable(process, sequenceFlowTarget, targetElement, visitedElements);
                    if (reachable) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    protected static boolean isInEventSubprocess(FlowNode flowNode) {
        FlowElementsContainer flowElementsContainer = flowNode.getParentContainer();
        while (flowElementsContainer != null) {
            if (flowElementsContainer instanceof EventSubProcess) {
                return true;
            }
            if (flowElementsContainer instanceof FlowElement) {
                flowElementsContainer = ((FlowElement) flowElementsContainer).getParentContainer();
            } else {
                flowElementsContainer = null;
            }
        }
        return false;
    }

    public static List<String> getParentProcessIds(FlowNode flowNode) {
        List<String> result = new ArrayList<>();
        FlowElementsContainer flowElementsContainer = flowNode.getParentContainer();
        while (flowElementsContainer != null) {
            if (flowElementsContainer instanceof SubProcess) {
                SubProcess flowElement = (SubProcess) flowElementsContainer;
                result.add(flowElement.getId());
                flowElementsContainer = flowElement.getParentContainer();
            } else if (flowElementsContainer instanceof Process) {
                Process flowElement = (Process) flowElementsContainer;
                result.add(flowElement.getId());
                flowElementsContainer = null;
            }
        }
        // 第一层Process为第0个
        Collections.reverse(result);
        return result;
    }

    /**
     * 查询不同层级
     *
     * @param sourceList
     * @param targetList
     * @return 返回不同的层级，如果其中一个层级较深，则返回层级小的+1，从第0层开始，请注意判断是否会出现下标越界异常；返回 -1 表示在同一层
     */
    public static Integer getDiffLevel(List<String> sourceList, List<String> targetList) {
        if (sourceList == null || sourceList.isEmpty() || targetList == null || targetList.isEmpty()) {
            throw new FlowableException("sourceList and targetList cannot be empty");
        }
        if (sourceList.size() == 1 && targetList.size() == 1) {
            // 都在第0层且不相等
            if (!sourceList.get(0).equals(targetList.get(0))) {
                return 0;
            } else {// 都在第0层且相等
                return -1;
            }
        }

        int minSize = sourceList.size() < targetList.size() ? sourceList.size() : targetList.size();
        Integer targetLevel = null;
        for (int i = 0; i < minSize; i++) {
            if (!sourceList.get(i).equals(targetList.get(i))) {
                targetLevel = i;
                break;
            }
        }
        if (targetLevel == null) {
            if (sourceList.size() == targetList.size()) {
                targetLevel = -1;
            } else {
                targetLevel = minSize;
            }
        }
        return targetLevel;
    }

    public static Set<String> getParentExecutionIdsByActivityId(List<ExecutionEntity> executions, String activityId) {
        List<ExecutionEntity> activityIdExecutions =
                executions.stream().filter(e -> activityId.equals(e.getActivityId())).collect(Collectors.toList());
        if (activityIdExecutions.isEmpty()) {
            throw new FlowableException("Active execution could not be found with activity id " + activityId);
        }
        // check for a multi instance root execution
        ExecutionEntity miExecution = null;
        boolean isInsideMultiInstance = false;
        for (ExecutionEntity possibleMiExecution : activityIdExecutions) {
            if (possibleMiExecution.isMultiInstanceRoot()) {
                miExecution = possibleMiExecution;
                isInsideMultiInstance = true;
                break;
            }
            if (isExecutionInsideMultiInstance(possibleMiExecution)) {
                isInsideMultiInstance = true;
            }
        }
        Set<String> parentExecutionIds = new HashSet<>();
        if (isInsideMultiInstance) {
            Stream<ExecutionEntity> executionEntitiesStream = activityIdExecutions.stream();
            if (miExecution != null) {
                executionEntitiesStream = executionEntitiesStream.filter(ExecutionEntity::isMultiInstanceRoot);
            }
            executionEntitiesStream.forEach(childExecution -> {
                parentExecutionIds.add(childExecution.getParentId());
            });
        } else {
            ExecutionEntity execution = activityIdExecutions.iterator().next();
            parentExecutionIds.add(execution.getParentId());
        }
        return parentExecutionIds;
    }

    public static boolean isExecutionInsideMultiInstance(ExecutionEntity execution) {
        return getFlowElementMultiInstanceParentId(execution.getCurrentFlowElement()).isPresent();
    }

    public static Optional<String> getFlowElementMultiInstanceParentId(FlowElement flowElement) {
        FlowElementsContainer parentContainer = flowElement.getParentContainer();
        while (parentContainer instanceof Activity) {
            if (isFlowElementMultiInstance((Activity) parentContainer)) {
                return Optional.of(((Activity) parentContainer).getId());
            }
            parentContainer = ((Activity) parentContainer).getParentContainer();
        }
        return Optional.empty();
    }

    public static boolean isFlowElementMultiInstance(FlowElement flowElement) {
        if (flowElement instanceof Activity) {
            return ((Activity) flowElement).getLoopCharacteristics() != null;
        }
        return false;
    }

    public static String getParentExecutionIdFromParentIds(ExecutionEntity execution, Set<String> parentExecutionIds) {
        ExecutionEntity taskParentExecution = execution.getParent();
        String realParentExecutionId = null;
        while (taskParentExecution != null) {
            if (parentExecutionIds.contains(taskParentExecution.getId())) {
                realParentExecutionId = taskParentExecution.getId();
                break;
            }
            taskParentExecution = taskParentExecution.getParent();
        }
        if (realParentExecutionId == null || realParentExecutionId.length() == 0) {
            throw new FlowableException("Parent execution could not be found with executionId id " + execution.getId());
        }
        return realParentExecutionId;
    }

    public static String[] getSourceAndTargetRealActivityId(FlowNode sourceFlowElement, FlowNode targetFlowElement) {
        // 实际应操作的当前节点ID
        String sourceRealActivityId = sourceFlowElement.getId();
        // 实际应操作的目标节点ID
        String targetRealActivityId = targetFlowElement.getId();
        List<String> sourceParentProcesss = FlowableUtils.getParentProcessIds(sourceFlowElement);
        List<String> targetParentProcesss = FlowableUtils.getParentProcessIds(targetFlowElement);
        int diffParentLevel = getDiffLevel(sourceParentProcesss, targetParentProcesss);
        if (diffParentLevel != -1) {
            sourceRealActivityId = sourceParentProcesss.size() == diffParentLevel ? sourceRealActivityId :
                    sourceParentProcesss.get(diffParentLevel);
            targetRealActivityId = targetParentProcesss.size() == diffParentLevel ? targetRealActivityId :
                    targetParentProcesss.get(diffParentLevel);
        }
        return new String[]{sourceRealActivityId, targetRealActivityId};
    }

    public static String getAttributeValue(BaseElement element, String namespace, String name) {
        return element.getAttributeValue(namespace, name);
    }

    public static String getFlowableAttributeValue(BaseElement element, String name) {
        return element.getAttributeValue("http://flowable.org/bpmn", name);
    }

    public static List<ExtensionElement> getExtensionElements(BaseElement element, String name) {
        return element.getExtensionElements().get(name);
    }


    public static FlowElement getFlowElement(RepositoryService repositoryService, String processDefinitionId,
                                             String flowElementId, boolean searchRecursive) {
        Process process = repositoryService.getBpmnModel(processDefinitionId).getMainProcess();
        FlowElement flowElement = process.getFlowElement(flowElementId, searchRecursive);
        return flowElement;
    }

    public static FlowElement getFlowElement(RepositoryService repositoryService, String processDefinitionId,
                                             String flowElementId) {
        return getFlowElement(repositoryService, processDefinitionId, flowElementId, true);
    }

    public static String determineParallelGatewayType(BpmnModel bpmnModel, String gatewayId) {
        // 获取并行网关的元素
        FlowElement gateway = bpmnModel.getFlowElement(gatewayId);

        // 检查并行网关的入口和出口
        boolean isEntry = false;
        boolean isExit = false;

        // 查找并行网关的所有流入连接
        for (SequenceFlow incomingFlow : ((ParallelGateway) gateway).getIncomingFlows()) {
            FlowElement sourceElement = incomingFlow.getSourceFlowElement();
            if (sourceElement instanceof Activity) {
                isEntry = true;
                break;
            }
        }

        // 查找并行网关的所有流出连接
        for (SequenceFlow outgoingFlow : ((ParallelGateway) gateway).getOutgoingFlows()) {
            FlowElement targetElement = outgoingFlow.getTargetFlowElement();
            if (targetElement instanceof Activity) {
                isExit = true;
                break;
            }
        }

        // 根据入口和出口判断结果返回相应的值
        if (isEntry && !isExit) {
            return "Parallel gateway is an entry.";
        } else if (!isEntry && isExit) {
            return "Parallel gateway is an exit.";
        } else {
            return "Unable to determine the type of parallel gateway.";
        }
    }

}

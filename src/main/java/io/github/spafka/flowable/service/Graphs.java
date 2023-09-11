package io.github.spafka.flowable.service;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import io.github.spafka.flowable.JumpTypeEnum;
import io.github.spafka.flowable.core.FlowableUtils;
import io.github.spafka.flowable.core.TopologyNode;
import io.vavr.Tuple;
import io.vavr.Tuple3;
import io.vavr.Tuple4;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j()
public class Graphs {


    public static Tuple3<Map<String, TopologyNode<FlowElement>>, String, String> buildGraph(BpmnModel bpmnModel, String lastNode, String beforeNode) {
        /**
         * 从bpmn解析出procss对象
         */
        Process process = bpmnModel.getMainProcess();

        /**
         * 子流程扁平化处理
         */
        List<FlowElement> flowElements = process.getFlowElements().stream().flatMap(flowElement -> {
            if (flowElement instanceof SubProcess) {
                Collection<FlowElement> sbs = ((SubProcess) flowElement).getFlowElements();
                return Stream.concat(Stream.of(flowElement), sbs.stream());
            } else {
                return Stream.of(flowElement);
            }
        }).collect(Collectors.toList());

        /**
         * id->FlowElement
         */
        Map<String, FlowElement> idMap = flowElements.stream().collect(Collectors.toMap(BaseElement::getId, x -> x));

        /**
         * 起始节点
         */
        FlowElement start = flowElements.stream().filter(x -> x instanceof StartEvent && x.getParentContainer() instanceof Process).findFirst().get();

        if (StringUtils.isBlank(beforeNode)) {
            beforeNode = start.getId();
        }
        /**
         * 终止节点中的一个？
         */
        if (StringUtils.isBlank(lastNode)) {
            FlowElement flowElement = process.getFlowElements().stream().filter(x -> x instanceof EndEvent && x.getParentContainer() instanceof Process).findFirst().get();
            lastNode = flowElement.getId();
        }

        assert idMap.containsKey(lastNode);
        assert idMap.containsKey(beforeNode);

        Map<String, TopologyNode<FlowElement>> indexMap = Maps.newLinkedHashMap();

        /**
         * 构建拓扑图
         */
        flowElements.forEach(x -> {
            if (x instanceof SequenceFlow) {
                TopologyNode<FlowElement> pre = indexMap.computeIfAbsent(((SequenceFlow) x).getSourceRef(), s -> new TopologyNode<>(idMap.get(s)));
                TopologyNode<FlowElement> next = indexMap.computeIfAbsent(((SequenceFlow) x).getTargetRef(), s -> new TopologyNode<>(idMap.get(s)));
                pre.addNext(next);
                next.addSource(pre);
            }
        });

        /**e
         * 子流程和父流程连接起来
         */
        indexMap.forEach((k, v) -> {
            if (v.node instanceof StartEvent) {
                StartEvent node = (StartEvent) v.node;
                FlowElementsContainer parentContainer = node.getParentContainer();

                // 子流程起始节点
                if (parentContainer instanceof SubProcess) {
                    String id = ((SubProcess) parentContainer).getId();
                    indexMap.get(id).addNext(v);
                    v.addSource(indexMap.get(id));
                }
            }
        });

        indexMap.forEach((k, v) -> {
            if (v.node instanceof SubProcess) {
                SubProcess node = (SubProcess) v.node;

                TopologyNode<FlowElement> subNode = indexMap.get(node.getId());

                // todo 子流程的起始节点只能是startEvent?
                List<FlowElement> endEvents = node.getFlowElements().stream().filter(x -> (x instanceof EndEvent)).collect(Collectors.toList());

                List<TopologyNode<FlowElement>> collect = subNode.next.stream().filter(x -> !(x.node instanceof StartEvent)).collect(Collectors.toList());
                endEvents.forEach(x -> {
                    collect.forEach(y -> {
                        indexMap.get(x.getId()).addNext(y);
                        y.addSource(indexMap.get(x.getId()));

                        y.pre.remove(subNode);
                    });

                });
                subNode.next.removeAll(collect);
            }
        });


        indexMap.forEach((k, v) -> buildParallelGatewayForkJoin(v, indexMap, process));

        return Tuple.of(indexMap, lastNode, beforeNode);
    }

    public static Tuple4<JumpTypeEnum, List<LinkedList<TopologyNode<FlowElement>>>, Set<FlowElement>, Map<String, TopologyNode<FlowElement>>> backTrack(BpmnModel bpmnModel, String lastNode, String beforeNode) {

        Tuple3<Map<String, TopologyNode<FlowElement>>, String, String> tuple3 = buildGraph(bpmnModel, lastNode, beforeNode);

        Map<String, TopologyNode<FlowElement>> indexMap = tuple3._1;
        lastNode = tuple3._2;
        beforeNode = tuple3._3;

        List<FlowElement> flowElements = bpmnModel.getMainProcess().getFlowElements().stream().flatMap(flowElement -> {
            if (flowElement instanceof SubProcess) {
                Collection<FlowElement> sbs = ((SubProcess) flowElement).getFlowElements();
                return Stream.concat(Stream.of(flowElement), sbs.stream());
            } else {
                return Stream.of(flowElement);
            }
        }).collect(Collectors.toList());
        FlowElement endNode = flowElements.stream().filter(x -> x instanceof EndEvent).findFirst().get();


        TopologyNode<FlowElement> end = indexMap.get(endNode.getId());
        LinkedList<TopologyNode<FlowElement>> path = new LinkedList<>();
        path.addFirst(end);
        path.removeLast();

        List<LinkedList<TopologyNode<FlowElement>>> paths = new ArrayList<>();
        findPathFromBehind(indexMap, beforeNode, lastNode, paths, new LinkedList<>(), Sets.newHashSet());
        JumpTypeEnum jumpTypeEnum = judgeJumpType(paths, indexMap.get(beforeNode), indexMap.get(lastNode));

        Set<FlowElement> toAddExecution = new HashSet<>();

        if (jumpTypeEnum == JumpTypeEnum.paral) {

            List<TopologyNode<FlowElement>> joinGateways = paths.stream().flatMap(x -> x.stream().filter(y -> isParallelGateway(y))).distinct().collect(Collectors.toList());
            List<TopologyNode<FlowElement>> innerGateway = new LinkedList<>();

            joinGateways.stream()
                    .filter(x -> {
                        if (x.join != null) {
                            TopologyNode<FlowElement> join = x.join;
                            if (joinGateways.contains(join)) {
                                innerGateway.add(x);
                                innerGateway.add(x.join);
                            }
                        }

                        return false;
                    }).count();
            joinGateways.removeAll(innerGateway);
            joinGateways.forEach(gates -> {
                List<SequenceFlow> incomingFlows = ((Gateway) gates.node).getIncomingFlows();
                for (SequenceFlow seq : incomingFlows) {
                    boolean present = paths.stream().anyMatch(x -> x.stream().anyMatch(y -> y.node.getId().equals(seq.getSourceRef())));
                    if (!present) {
                        toAddExecution.add(seq);
                    }
                }
                toAddExecution.add(gates.node);
            });
        }
        return Tuple.of(jumpTypeEnum, paths, toAddExecution, indexMap);
    }


    public static void buildParallelGatewayForkJoin(TopologyNode<FlowElement> parallable, Map<String, TopologyNode<FlowElement>> indexMap, Process process) {


        if (isParallelGateway(parallable)) {

            Gateway parallelGateway = (Gateway) parallable.node;
            List<FlowElement> endEvents = parallelGateway.getParentContainer().getFlowElements().stream().filter(x -> x instanceof EndEvent).collect(Collectors.toList());

            for (FlowElement endEvent : endEvents) {

                if (parallelGateway.getOutgoingFlows().size() > 1) {

                    log.info("判断 {} 网关", parallelGateway.getId());

                    List<LinkedList<TopologyNode<FlowElement>>> paths = new ArrayList<>();
                    findPathFromBehind(indexMap, parallable.node.getId(), endEvent.getId(), paths, new LinkedList<>(), Sets.newHashSet());
                    List<TopologyNode<FlowElement>> pgs = paths.stream().flatMap(x -> x.stream().filter(y -> isParallelGateway(y))).distinct().collect(Collectors.toList());

                    for (TopologyNode<FlowElement> pg : pgs) {
                        if (pg == parallable) {
                            continue;
                        }
                        log.info("判断 {} 是否是 {}'s join", pg.node.getId(), parallable.node.getId());
                        paths = new ArrayList<>();
                        findPathFromBehind(indexMap, parallable.node.getId(), pg.node.getId(), paths, new LinkedList<>(), Sets.newHashSet());

                        List<SequenceFlow> outgoingFlows = parallelGateway.getOutgoingFlows();
                        Boolean reduce = outgoingFlows.stream().map(x -> FlowableUtils.isReachable(process, x.getId(), pg.node.getId())).reduce(true, (a, b) -> a && b);

                        if (reduce) {
                            log.info("判断 {} 是 {}'s join", pg.node.getId(), parallable.node.getId());

                            paths.forEach(x -> x.stream()
                                    .filter(y -> y != parallable)
                                    .filter(y -> isParallelGateway(y))
                                    .forEach(y -> {
                                        y.addFork(parallable);
                                    }));
                            paths.forEach(x -> x.stream().filter(y -> !isParallelGateway(y))
                                    .forEach(y -> {
                                        y.addGate(parallable);
                                    }));
                            parallable.join = pg;
                        }
                    }


                }

            }

        }

    }

    private static boolean isParallelGateway(TopologyNode<FlowElement> y) {
        return y.node instanceof ParallelGateway || (y.node instanceof InclusiveGateway && ((InclusiveGateway) y.node).getOutgoingFlows().stream().noneMatch(z -> z.getSkipExpression() == null && z.getConditionExpression() == null));
    }


    public static void findPathFromBehind(Map<String, TopologyNode<FlowElement>> nodeMap, String startId, String endId, List<LinkedList<TopologyNode<FlowElement>>> res, LinkedList<TopologyNode<FlowElement>> path, Set<String> visited) {
        log.trace("{} {} {} {}", startId, endId, path, visited);

        if (path.isEmpty()) {
            path.addFirst(nodeMap.get(endId));
        }
        if (path.getLast().node.getId().equals(startId)) {
            res.add(new LinkedList<>(path));
            return;
        }
        if (visited.contains(startId)) {
            return;
        }
        TopologyNode<FlowElement> currentNode = nodeMap.get(endId);
        TopologyNode<FlowElement>.SkipList<TopologyNode<FlowElement>> pre = currentNode.pre;
        if (visited.contains(currentNode.node.getId())) {
            return;
        }
        visited.add(currentNode.node.getId());

        for (TopologyNode<FlowElement> preNode : pre) {
            path.addLast(preNode);
            findPathFromBehind(nodeMap, startId, preNode.node.getId(), res, path, visited);
            path.removeLast();
        }


    }

    public static JumpTypeEnum judgeJumpType(List<LinkedList<TopologyNode<FlowElement>>> paths, TopologyNode<FlowElement> before, TopologyNode<FlowElement> later) {

        log.info("{} gate:[{}] {}:gate[{}]", before.node.getId(), before.gateways, later.node.getId(), later.gateways);
        if (paths.isEmpty()) {
            throw new RuntimeException("路径不可达");
        }

        /**
         *  B-----user-------A
         *
         *  B----gate-------A-----gate
         *       |-----user-------|
         */

        if (before.gateways.isEmpty() && later.gateways.isEmpty()) {
            return JumpTypeEnum.serial;
        }

        /**
         *  gate--------B-----user-----A---gate
         *     |                           |
         *     |--------user---------------
         *     or
         *  gate--------B-----user----gate----A--gate --gate
         *     |                          |      |      |
         *     |                          ----user      |
         *     |--------user----------------------------
         *
         */
        if (!later.gateways.isEmpty() && later.gateways.containsAll(before.gateways) && before.gateways.containsAll(later.gateways)) {

            return JumpTypeEnum.serial;
        }

        /**    -----user-----
         *    |              \
         * gate------B-------gate--A
         *    \-----user-----/
         *
         *
         */
        if (!later.gateways.isEmpty() && !later.gateways.containsAll(before.gateways)) {
            return JumpTypeEnum.paral;
        }

        return JumpTypeEnum.paral;
    }

    /**
     * 查找当前节点到
     *
     * @param head
     * @param path
     * @param res
     */
    public static void currentToEndAllPath(TopologyNode<FlowElement> head, String tail, LinkedList<FlowElement> path, LinkedList<LinkedList<FlowElement>> res) {

        if (tail == null) {
            if (head.node instanceof EndEvent && head.node.getParentContainer() instanceof Process) {
                res.add(new LinkedList<>(path));
            }
        } else {
            if (head.node.getId().equals(tail)) {
                res.add(new LinkedList<>(path));
            }
        }
        TopologyNode<FlowElement>.SkipList<TopologyNode<FlowElement>> next = head.next;
        for (TopologyNode<FlowElement> f : next) {
            path.addLast(f.node);
            currentToEndAllPath(f, tail, path, res);
            path.removeLast();
        }
    }
}

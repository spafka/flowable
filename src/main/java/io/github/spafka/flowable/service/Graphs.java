package io.github.spafka.flowable.service;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import io.github.spafka.flowable.core.FlowableUtils;
import io.github.spafka.flowable.core.TopologyNode;
import io.vavr.Tuple;
import io.vavr.Tuple3;
import io.vavr.Tuple4;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.*;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j()
public class Graphs {


    public static Tuple3<Map<String, TopologyNode<BaseElement>>, String, String> buildGraph(BpmnModel bpmnModel, String lastNode, String beforeNode) {
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
        Collection<Signal> signals = bpmnModel.getSignals();

        Map<String, Signal> sigMap = Maps.newLinkedHashMap();

        if (!CollectionUtils.isEmpty(signals)) {
            signals.forEach(x -> {
                sigMap.put(x.getId(), x);
            });
        }


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

        Map<String, TopologyNode<BaseElement>> indexMap = Maps.newLinkedHashMap();

        sigMap.forEach((k, v) -> {
            indexMap.put(k, new TopologyNode<>(v));
        });

        /**
         * 构建拓扑图
         */
        flowElements.forEach(x -> {
            if (x instanceof SequenceFlow) {

                SequenceFlow x1 = (SequenceFlow) x;
                TopologyNode<BaseElement> pre = indexMap.computeIfAbsent(x1.getSourceRef(), s -> new TopologyNode<>(idMap.get(s)));
                TopologyNode<BaseElement> next = indexMap.computeIfAbsent(x1.getTargetRef(), s -> new TopologyNode<>(idMap.get(s)));
                pre.addNext(next);
                next.addSource(pre);
            }
        });

        /**
         * 子流程和父流程连接起来
         */
        indexMap.forEach((k, v) -> {
            if (v.node instanceof StartEvent) {
                StartEvent node = (StartEvent) v.node;
                FlowElementsContainer parentContainer = node.getParentContainer();

                // 子流程起始节点
                if (parentContainer instanceof SubProcess) {
                    String id = ((SubProcess) parentContainer).getId();
                    TopologyNode<BaseElement> node1 = indexMap.get(id);

                    if (node1 != null) {
                        node1.addNext(v);
                        v.addSource(node1);
                    } else {

//                        List<EventDefinition> eventDefinitions = node.getEventDefinitions();
//                        eventDefinitions.forEach(x -> {
//                            if (x instanceof SignalEventDefinition) {
//                                String signalRef = ((SignalEventDefinition) x).getSignalRef();
//                                var node2 = indexMap.get(signalRef);
//
//                                node2.addNext(v);
//                                v.addSource(node2);
//                            }
//                        });

                    }
                }
            }
        });

        indexMap.forEach((k, v) -> {
            if (v.node instanceof SubProcess) {
                SubProcess node = (SubProcess) v.node;

                TopologyNode<BaseElement> subNode = indexMap.get(node.getId());

                // todo 子流程的起始节点只能是startEvent?
                List<FlowElement> endEvents = node.getFlowElements().stream().filter(x -> (x instanceof EndEvent)).collect(Collectors.toList());

                List<TopologyNode<BaseElement>> collect = subNode.next.stream().filter(x -> !(x.node instanceof StartEvent)).collect(Collectors.toList());
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


        // 处理边界事件，边界事件 作为next 加入到当前事件
        indexMap.forEach((k, v) -> {
            if (v.node instanceof BoundaryEvent) {
                Activity attachedToRef = ((BoundaryEvent) v.node).getAttachedToRef();
                TopologyNode<BaseElement> node = indexMap.get(attachedToRef.getId());
                node.addNext(v);
                v.addSource(node);
            }
        });
        indexMap.forEach((k, v) -> {
            if (v.node instanceof StartEvent) {
                StartEvent node = (StartEvent) v.node;
                List<EventDefinition> eventDefinitions = node.getEventDefinitions();
                eventDefinitions.forEach(x -> {
                    if (x instanceof SignalEventDefinition) {
                        String signalRef = ((SignalEventDefinition) x).getSignalRef();
                        TopologyNode<BaseElement> sig = indexMap.get(signalRef);
                        v.addSource(sig);
                        sig.addNext(v);
                    }
                });
            }
            if (v.node instanceof ThrowEvent) {
                ThrowEvent node = (ThrowEvent) v.node;
                List<EventDefinition> eventDefinitions = node.getEventDefinitions();
                eventDefinitions.forEach(x -> {
                    if (x instanceof SignalEventDefinition) {
                        String signalRef = ((SignalEventDefinition) x).getSignalRef();
                        TopologyNode<BaseElement> sig = indexMap.get(signalRef);
                        v.addNext(sig);
                        sig.addSource(v);
                    }
                });
            }
        });

        indexMap.forEach((k, v) -> buildParallelGatewayForkJoin(v, indexMap, process));
        List<LinkedList<TopologyNode<BaseElement>>> res = new ArrayList<>();

        findPathFromBehind(indexMap, beforeNode, lastNode, res, new LinkedList<>(), Sets.newHashSet());
        if (res.isEmpty()) {
            TopologyNode<BaseElement> eventSubProcess = indexMap.get(lastNode);
            beforeNode = ((FlowElement) eventSubProcess.node).getParentContainer().getFlowElements().stream().filter(x -> x instanceof StartEvent).findFirst().get().getId();
        }

        return Tuple.of(indexMap, lastNode, beforeNode);
    }

    public static Tuple4<JumpTypeEnum, List<LinkedList<TopologyNode<BaseElement>>>, Set<FlowElement>, Map<String, TopologyNode<BaseElement>>> backTrack(BpmnModel bpmnModel, String lastNode, String beforeNode) {

        Tuple3<Map<String, TopologyNode<BaseElement>>, String, String> tuple3 = buildGraph(bpmnModel, lastNode, beforeNode);

        Map<String, TopologyNode<BaseElement>> indexMap = tuple3._1;
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


        TopologyNode<BaseElement> end = indexMap.get(endNode.getId());
        LinkedList<TopologyNode<BaseElement>> path = new LinkedList<>();
        path.addFirst(end);
        path.removeLast();

        List<LinkedList<TopologyNode<BaseElement>>> paths = new ArrayList<>();
        findPathFromBehind(indexMap, beforeNode, lastNode, paths, new LinkedList<>(), Sets.newHashSet());
        JumpTypeEnum jumpTypeEnum = judgeJumpType(paths, indexMap.get(beforeNode), indexMap.get(lastNode));

        Set<FlowElement> toAddExecution = new HashSet<>();

        if (jumpTypeEnum == JumpTypeEnum.paral_to_child) {

            List<TopologyNode<BaseElement>> joinGateways = paths.stream().flatMap(x -> x.stream().filter(y -> isParallelGateway(y))).distinct().collect(Collectors.toList());
            List<TopologyNode<BaseElement>> innerGateway = new LinkedList<>();

            joinGateways.stream()
                    .filter(x -> {
                        if (x.join != null && !x.join.isEmpty()) {

                            var joins = x.join;
                            joins.stream().filter(y -> y.node instanceof ParallelGateway || y.node instanceof InclusiveGateway).forEach(join -> {
                                if (joinGateways.contains(join)) {
                                    innerGateway.add(x);
                                    innerGateway.add(join);
                                }
                            });
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
                toAddExecution.add((FlowElement) gates.node);
            });
        }
        return Tuple.of(jumpTypeEnum, paths, toAddExecution, indexMap);
    }


    public static void buildParallelGatewayForkJoin(TopologyNode<BaseElement> parallable, Map<String, TopologyNode<BaseElement>> indexMap, Process process) {


        if (isParallelGateway(parallable)) {

            Gateway parallelGateway = (Gateway) parallable.node;
            List<FlowElement> endEvents = parallelGateway.getParentContainer().getFlowElements().stream().filter(x -> x instanceof EndEvent).collect(Collectors.toList());

            if (parallelGateway.getOutgoingFlows().size() > 1) {
                for (FlowElement endEvent : endEvents) {

                    log.debug("判断 {} 网关", parallelGateway.getId());
                    List<LinkedList<TopologyNode<BaseElement>>> paths = new ArrayList<>();
                    findPathFromBehind(indexMap, parallable.node.getId(), endEvent.getId(), paths, new LinkedList<>(), Sets.newHashSet());
                    List<TopologyNode<BaseElement>> pgs = paths.stream().flatMap(x -> x.stream().filter(Graphs::isParallelGateway)).distinct().collect(Collectors.toList());
                    if (pgs.isEmpty()) {

                        parallable.join.add(indexMap.get(endEvent.getId()));

                        paths.stream().flatMap(Collection::stream).filter(x -> x.node != parallelGateway)
                                .forEach(x -> x.addFork(parallable));
                    } else {
                        for (TopologyNode<BaseElement> pg : pgs) {
                            if (pg == parallable) {
                                continue;
                            }
                            log.debug("判断 {} 是否是 {}'s join", pg.node.getId(), parallable.node.getId());
                            paths = new ArrayList<>();
                            findPathFromBehind(indexMap, parallable.node.getId(), pg.node.getId(), paths, new LinkedList<>(), Sets.newHashSet());

                            List<SequenceFlow> outgoingFlows = parallelGateway.getOutgoingFlows();
                            Boolean ok = outgoingFlows.stream().map(x -> FlowableUtils
                                            .isReachable(process,
                                                    x.getId(), pg.node.getId()))
                                    .reduce(true, (a, b) -> a && b);

                            if (ok && FlowableUtils.iteratorCheckSequentialReferTarget((FlowElement) pg.node, parallable.node.getId(), null, null)) {
                                log.debug("判断 {} 是 {}'s join", pg.node.getId(), parallable.node.getId());

                                paths.forEach(x -> x.stream()
                                        .filter(y -> y != parallable)
                                        .filter(Graphs::isParallelGateway)
                                        .forEach(y -> {
                                            y.addFork(parallable);
                                        }));
                                paths.forEach(x -> x.stream().filter(y -> !isParallelGateway(y))
                                        .forEach(y -> y.addGate(parallable)));
                                parallable.join.add(pg);
                            }
                        }

                    }
                }
            }
        }

    }

    private static boolean isParallelGateway(TopologyNode<BaseElement> y) {
        return y.node instanceof ParallelGateway || (y.node instanceof InclusiveGateway && ((InclusiveGateway) y.node).getOutgoingFlows().stream().noneMatch(z -> z.getSkipExpression() == null && z.getConditionExpression() == null));
    }


    public static void findPathFromBehind(Map<String, TopologyNode<BaseElement>> nodeMap, String startId, String endId, List<LinkedList<TopologyNode<BaseElement>>> res, LinkedList<TopologyNode<BaseElement>> path, Set<String> visited) {
        log.debug("{} {} {} {}", startId, endId, path, visited);

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
        TopologyNode<BaseElement> currentNode = nodeMap.get(endId);
        TopologyNode<BaseElement>.SkipList<TopologyNode<BaseElement>> pre = currentNode.pre;
        if (visited.contains(currentNode.node.getId())) {
            return;
        }
        visited.add(currentNode.node.getId());

        for (TopologyNode<BaseElement> preNode : pre) {
            path.addLast(preNode);
            findPathFromBehind(nodeMap, startId, preNode.node.getId(), res, path, visited);
            path.removeLast();
        }
        visited.remove(currentNode.node.getId());


    }

    public static void findPathFromHead(Map<String, TopologyNode<BaseElement>> nodeMap, String startId, String currentId, List<LinkedList<TopologyNode<BaseElement>>> res, LinkedList<TopologyNode<BaseElement>> path, Set<String> visited) {
        log.debug("{} {} {} {}", startId, currentId, path, visited);

        if (path.isEmpty()) {
            path.addFirst(nodeMap.get(startId));
        }
        if (path.getLast().node.getId().equals(startId)) {
            res.add(new LinkedList<>(path));
            return;
        }
        if (visited.contains(startId)) {
            return;
        }
        TopologyNode<BaseElement> currentNode = nodeMap.get(currentId);
        TopologyNode<BaseElement>.SkipList<TopologyNode<BaseElement>> pre = currentNode.pre;
        if (visited.contains(currentNode.node.getId())) {
            return;
        }
        visited.add(currentNode.node.getId());

        for (TopologyNode<BaseElement> preNode : pre) {
            path.addLast(preNode);
            findPathFromBehind(nodeMap, startId, preNode.node.getId(), res, path, visited);
            path.removeLast();
        }
        visited.remove(currentNode.node.getId());


    }

    public static JumpTypeEnum judgeJumpType(List<LinkedList<TopologyNode<BaseElement>>> paths, TopologyNode<BaseElement> before, TopologyNode<BaseElement> later) {

        log.debug("{} gate:[{}] {}:gate[{}]", before.node.getId(), before.gateways, later.node.getId(), later.gateways);
        if (paths.isEmpty()) {
            throw new RuntimeException("路径不可达");
        }

        /**
         *  B-----user-------A
         *
         */

        if ((before.gateways.isEmpty() && later.gateways.isEmpty())) {
            return JumpTypeEnum.simple_serial;
        } else if ((before.gateways.containsAll(later.gateways) && later.gateways.containsAll(before.gateways))) {
            return JumpTypeEnum.simple_serial;
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
        else if (later.gateways.containsAll(before.gateways)) {

            return JumpTypeEnum.paral_to_father;
        }

        /**    -----user-----
         *    |              \
         * gate------B-------gate--A
         *    \-----user-----/
         *
         *
         */
        else {
            return JumpTypeEnum.paral_to_child;
        }


    }

    /**
     * 查找当前节点到
     *
     * @param head
     * @param path
     * @param res
     */
    public static void currentToEndAllPath(TopologyNode<BaseElement> head, String tail, LinkedList<FlowElement> path, LinkedList<LinkedList<FlowElement>> res) {

        if (path.isEmpty()) {
            path.add(((FlowElement) head.node));
        }
        if (tail == null) {
            // 直到非子流程的终止节点
            if (head.node instanceof EndEvent && ((FlowElement) (head.node)).getParentContainer() instanceof Process) {
                res.add(new LinkedList<>(path));
            }
        } else {
            if (head.node.getId().equals(tail)) {
                res.add(new LinkedList<>(path));
            }
        }
        TopologyNode<BaseElement>.SkipList<TopologyNode<BaseElement>> next = head.next;
        for (TopologyNode<BaseElement> f : next) {
            if (path.stream().anyMatch(x -> x.getId().equals(f.node.getId()))) {
                continue;
            }
            path.addLast((FlowElement) f.node);
            currentToEndAllPath(f, tail, path, res);
            path.removeLast();
        }
    }
}

package io.github.spafka.flowable.service;

import com.google.common.collect.Sets;
import io.github.spafka.flowable.JumpTypeEnum;
import io.github.spafka.flowable.core.FlowableUtils;
import io.github.spafka.flowable.core.TopologyNode;
import io.vavr.Tuple;
import io.vavr.Tuple3;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j()
public class Graphs {

    public static Tuple3<JumpTypeEnum, List<LinkedList<TopologyNode<FlowElement>>>, Set<FlowElement>> backStace(BpmnModel bpmnModel, String lastNode, String beforeNode) {

        Process process = bpmnModel.getMainProcess();

        Collection<FlowElement> flowElements = process.getFlowElements().stream().flatMap(flowElement -> {
            if (flowElement instanceof SubProcess) {
                Collection<FlowElement> sbs = ((SubProcess) flowElement).getFlowElements();
                return Stream.concat(Stream.of(flowElement), sbs.stream());
            } else {
                return Stream.of(flowElement);
            }
        }).collect(Collectors.toList());

        Map<String, FlowElement> idMap = flowElements.stream().collect(Collectors.toMap(BaseElement::getId, x -> x));


        FlowElement start = flowElements.stream().filter(x -> x instanceof StartEvent).findFirst().get();
        TopologyNode<FlowElement> root = new TopologyNode<>(start);

        if (StringUtils.isBlank(beforeNode)) {
            beforeNode = start.getId();
        }

        assert idMap.containsKey(lastNode);
        assert idMap.containsKey(beforeNode);

        Map<String, TopologyNode<FlowElement>> indexMap = new HashMap<>();
        indexMap.put(start.getId(), root);

        flowElements.forEach(x -> {
            if (x instanceof SequenceFlow) {
                TopologyNode<FlowElement> source = indexMap.computeIfAbsent(((SequenceFlow) x).getSourceRef(), s -> new TopologyNode<>(idMap.get(s)));
                TopologyNode<FlowElement> target = indexMap.computeIfAbsent(((SequenceFlow) x).getTargetRef(), s -> new TopologyNode<>(idMap.get(s)));
                source.addNext(target);
                target.addSource(source);
            }
        });

        FlowElement endNode = flowElements.stream().filter(x -> x instanceof EndEvent).findFirst().get();

        indexMap.forEach((k, v) -> isInParallelGateway(v, root, indexMap, endNode, process));

        TopologyNode<FlowElement> end = indexMap.get(endNode.getId());
        LinkedList<TopologyNode<FlowElement>> path = new LinkedList<>();
        path.addFirst(end);
        path.removeLast();

        List<LinkedList<TopologyNode<FlowElement>>> paths = new ArrayList<>();
        findPath(indexMap, indexMap.get(beforeNode).node.getId(), indexMap.get(lastNode).node.getId(), paths, new LinkedList<>(), Sets.newHashSet());
        JumpTypeEnum jumpTypeEnum = judgeJumpType(paths, indexMap.get(beforeNode), indexMap.get(lastNode));

        Set<FlowElement> toAddExecution = new HashSet<>();

        if (jumpTypeEnum == JumpTypeEnum.paral) {

            List<TopologyNode<FlowElement>> joinGateways = paths.stream().flatMap(x -> x.stream().filter(y -> y.node instanceof ParallelGateway || y.node instanceof InclusiveGateway)).distinct().collect(Collectors.toList());

            var currentTaskNode = paths.get(0).getFirst();
            List<TopologyNode<FlowElement>> innergateway = new LinkedList<>();

                    joinGateways.stream()
                    .filter(x -> {
                        if (x.join != null) {
                            TopologyNode join = x.join;
                            if (joinGateways.contains(join)) {
                                innergateway.add(x);
                                innergateway.add(x.join);
                            }
                        }

                        return false;
                    })
                    .collect(Collectors.toList());
            joinGateways.removeAll(innergateway);

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
            System.out.println();
        }
        return Tuple.of(jumpTypeEnum, paths, toAddExecution);
    }


    public static void isInParallelGateway(TopologyNode<FlowElement> parallable, TopologyNode<FlowElement> start, Map<String, TopologyNode<FlowElement>> indexMap, FlowElement endNode, Process process) {
        if (!(parallable.node instanceof ParallelGateway) && !(parallable.node instanceof InclusiveGateway)) {
            return;
        } else {
            Gateway parallelGateway = (Gateway) parallable.node;
            if (parallelGateway.getOutgoingFlows().size() > 1) {

                log.info("判断 {} 网关", parallelGateway.getId());

                List<LinkedList<TopologyNode<FlowElement>>> paths = new ArrayList<>();


                findPath(indexMap, parallable.node.getId(), endNode.getId(), paths, new LinkedList<>(), Sets.newHashSet());

                List<TopologyNode<FlowElement>> pgs = paths.stream().flatMap(x -> x.stream().filter(y -> y.node instanceof ParallelGateway || y.node instanceof InclusiveGateway)).distinct().collect(Collectors.toList());

                for (TopologyNode<FlowElement> pg : pgs) {
                    if (pg == parallable) {
                        continue;
                    }
                    log.info("判断 {} 是否是 {}'s join", pg.node.getId(), parallable.node.getId());
                    paths = new ArrayList<>();
                    findPath(indexMap, parallable.node.getId(), pg.node.getId(), paths, new LinkedList<>(), Sets.newHashSet());

                    List<SequenceFlow> outgoingFlows = parallelGateway.getOutgoingFlows();
                    Boolean reduce = outgoingFlows.stream().map(x -> FlowableUtils.isReachable(process, x.getId(), pg.node.getId())).reduce(true, (a, b) -> a && b);

                    if (reduce) {
                        paths.forEach(x -> {
                            x.stream()
                                    .filter(y -> y != parallable)
                                    .filter(y -> (y.node instanceof ParallelGateway || y.node instanceof InclusiveGateway))
                                    .forEach(y -> {
                                        y.forks.add(parallable);
                                    });
                        });
                        paths.forEach(x -> {
                            x.stream().filter(y -> !(y.node instanceof ParallelGateway || y.node instanceof InclusiveGateway))
                                    .forEach(y -> {
                                        y.addGate(parallable);
                                        y.forks.add(parallable);
                                    });
                        });
                        parallable.join = pg;
                    }
                }

            }

        }

    }


    /***
     *
     * /-----\
     * A     B b的fork节点是A ,A的join节点只有b
     * \-----/
     */
    public static boolean isJoinGateway(TopologyNode topologyNode) {
        return !topologyNode.forks.isEmpty();
    }

    public static void findPath(Map<String, TopologyNode<FlowElement>> nodeMap, String startId, String endId, List<LinkedList<TopologyNode<FlowElement>>> res, LinkedList<TopologyNode<FlowElement>> path, Set<String> visited) {
        log.trace("{} {} {} {}", startId, endId, path, visited);
        if (path.isEmpty()) {
            path.addFirst(nodeMap.get(endId));
        }
        if (path.getLast().node.getId().equals(startId)) {
            res.add(new LinkedList<>(path));
        }
        if (visited.contains(startId)) {
            return;
        }
        TopologyNode<FlowElement> node = nodeMap.get(endId);
        TopologyNode<FlowElement>.SkipList<TopologyNode<FlowElement>> pre = node.pre;

        for (TopologyNode<FlowElement> topologyNode : pre) {
            path.addLast(topologyNode);
            visited.add(topologyNode.node.getId());
            findPath(nodeMap, startId, topologyNode.node.getId(), res, path, visited);
            visited.remove(topologyNode.node.getId());
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

        if (before.gateways.isEmpty()) {
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
        if (later.gateways.containsAll(before.gateways)) {
            return JumpTypeEnum.serial;
        }

        /**    -----user-----
         *    |              \
         * gate------B-------gate--A
         *    \-----user-----/
         *
         *
         */
        if (!later.gateways.containsAll(before.gateways)) {
            return JumpTypeEnum.paral;
        }

        return JumpTypeEnum.un_known;
    }

    public static void currentToEnd(TopologyNode node, LinkedList<FlowElement> path, LinkedList<LinkedList<FlowElement>> res) {

        if (node.node instanceof EndEvent) {
            res.add(new LinkedList<>(path));

        }
        TopologyNode<FlowElement>.SkipList<TopologyNode<FlowElement>> next = node.next;
        for (TopologyNode<FlowElement> f : next) {
            path.addLast(f.node);
            currentToEnd(f, path, res);
            path.removeLast();
        }
    }
}

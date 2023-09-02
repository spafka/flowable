package io.github.spafka.flowable.service;

import io.github.spafka.flowable.JumpTypeEnum;
import io.github.spafka.flowable.core.TopologyNode;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Graphs {

    public static JumpTypeEnum backStace(BpmnModel bpmnModel, String lastNode, String beforeNode) {

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

        Map<String, TopologyNode<FlowElement>> cache = new HashMap<>();
        cache.put(start.getId(), root);

        flowElements.forEach(x -> {
            if (x instanceof SequenceFlow) {
                TopologyNode<FlowElement> source = cache.computeIfAbsent(((SequenceFlow) x).getSourceRef(), s -> new TopologyNode<>(idMap.get(s)));
                TopologyNode<FlowElement> target = cache.computeIfAbsent(((SequenceFlow) x).getTargetRef(), s -> new TopologyNode<>(idMap.get(s)));
                source.addNext(target);
                target.addSource(source);
            }
        });

        FlowElement endNode = flowElements.stream().filter(x -> x instanceof EndEvent).findFirst().get();


        TopologyNode<FlowElement> end = cache.get(endNode.getId());
        LinkedList<TopologyNode> path = new LinkedList<>();
        path.addFirst(end);
        path.removeLast();

        List<LinkedList<FlowElement>> paths = new ArrayList<>();
        findPath(cache, cache.get(beforeNode).node.getId(), cache.get(lastNode).node.getId(), paths, new LinkedList<>());
        return judgeJumpType(paths, root, end);


    }


    public static void findPath(Map<String, TopologyNode<FlowElement>> nodeMap, String startId, String endId, List<LinkedList<FlowElement>> res, LinkedList<FlowElement> path) {


        if (!path.isEmpty() && path.getLast().getId().equals(startId)) {
            res.add(new LinkedList<>(path));
        }

        TopologyNode<FlowElement> node = nodeMap.get(endId);

        TopologyNode<FlowElement>.SkipList<TopologyNode<FlowElement>> pre = node.pre;


        for (TopologyNode<FlowElement> topologyNode : pre) {
            path.addLast(topologyNode.node);
            findPath(nodeMap, startId, topologyNode.node.getId(), res, path);
            path.removeLast();
        }


    }

    public static JumpTypeEnum judgeJumpType(List<LinkedList<FlowElement>> paths, TopologyNode<FlowElement> root, TopologyNode<FlowElement> end) {

        if (paths.size() == 1) {

            /**
             *   user---------user----------user
             *
             */
            long count = paths.get(0).stream().map(x -> x instanceof ParallelGateway || x instanceof InclusiveGateway).count();
            if (count == 0L) {
                return JumpTypeEnum.serial;
            } else if (count == 1L) {

                /**
                 *   user--gate------user
                 *         |
                 *         ——user
                 */
                return JumpTypeEnum.inpg_2_seq;
            }
        }
        {

            FlowElement first = paths.get(0).getFirst();
            FlowElement last = paths.get(0).getLast();

            boolean sql = true;
            // 所有路径的所有头和尾都是同一个的话，说明中间有网关或分支，也属于串行
            for (int i = 1; i < paths.size(); i++) {
                if (!(first == paths.get(i).getFirst() && last == paths.get(i).getLast())) {
                    sql = false;
                    break;
                }
            }
            if (sql) {
                return JumpTypeEnum.serial;
            }
        }

        return JumpTypeEnum.un_known;
    }
}

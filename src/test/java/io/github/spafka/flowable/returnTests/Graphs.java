package io.github.spafka.flowable.returnTests;

import io.github.spafka.flowable.core.TopologyNode;
import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Graphs {

    @Test
    public void test() {
        BpmnModel bpmnModel = new BpmnXMLConverter().convertToBpmnModel(() -> {
            try {
                return Files.newInputStream(Paths.get("src/main/resources/returntest/复杂并行网关.bpmn20.xml"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, true, true);
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
                //TopologyNode thiS = cache.computeIfAbsent(x.getId(), s -> new TopologyNode<>(idMap.get(s)));
                //thiS.pre.add(source);
                source.addNext(target);
                target.addSource(source);
                // thiS.next.add(target);
            }
        });

        FlowElement endNode = flowElements.stream().filter(x -> x instanceof EndEvent).findFirst().get();


        TopologyNode<FlowElement> end = cache.get(endNode.getId());
        LinkedList<TopologyNode> path = new LinkedList<>();

        path.addFirst(end);
        path.removeLast();

        LinkedList<TopologyNode> collect = path.stream().filter(x -> !(x.node instanceof SequenceFlow)).collect(Collectors.toCollection(LinkedList::new));


        LinkedList<FlowElement> res = new LinkedList<>();



    }
}

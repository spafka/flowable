package io.github.spafka.flowable;

import org.flowable.bpmn.model.*;
import org.flowable.bpmn.model.Process;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.ProcessDefinition;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FlowBase {

    public void diagram(RepositoryService repositoryService, String processName) {


        List<ProcessDefinition> all = repositoryService.createProcessDefinitionQuery()
                .processDefinitionName(processName)
                .list();
        BpmnModel bpmnModel = repositoryService.getBpmnModel(all.get(0).getId());

        Process process = bpmnModel.getProcesses().get(0);

        Collection<FlowElement> flowElements = process.getFlowElements().stream().flatMap(x -> {
            if (x instanceof SubProcess) {
                Collection<FlowElement> sbs = ((SubProcess) x).getFlowElements();
                return Stream.concat(Stream.of(x), sbs.stream());
            } else {
                return Stream.of(x);
            }
        }).collect(Collectors.toList());
        ;


        Map<String, FlowElement> idMap = flowElements.stream().collect(Collectors.toMap(BaseElement::getId, x -> x));


        FlowElement start = flowElements.stream().filter(x -> x instanceof StartEvent).findFirst().get();
        TopologyNode<FlowElement> root = new TopologyNode<>(start);

        Map<String, TopologyNode<FlowElement>> cache = new HashMap<>();
        cache.put(start.getId(), root);

        flowElements.forEach(x -> {
            if (x instanceof SequenceFlow) {
                TopologyNode<FlowElement> source = cache.computeIfAbsent(((SequenceFlow) x).getSourceRef(), s -> new TopologyNode<>(idMap.get(s)));
                TopologyNode<FlowElement> target = cache.computeIfAbsent(((SequenceFlow) x).getTargetRef(), s -> new TopologyNode<>(idMap.get(s)));
                TopologyNode thiS = cache.computeIfAbsent(x.getId(), s -> new TopologyNode<>(idMap.get(s)));
                thiS.pre.add(source);
                source.next.add(thiS);
                target.pre.add(thiS);
                thiS.next.add(target);
            } else {

            }
        });

        TopologyNode<FlowElement> end = cache.get("Event_06lk902");


        ArrayDeque<TopologyNode<FlowElement>> flowElements1 = new ArrayDeque<>();


        LinkedList<FlowElement> path = new LinkedList<>();
        while (end.pre != null && !end.pre.isEmpty()) {
            path.addFirst(end.node);
            end = end.pre.get(0);
        }
        path.addFirst(end.node);

        List<FlowElement> collect = path.stream().filter(x -> !(x instanceof SequenceFlow)).collect(Collectors.toList());


        System.out.println();
    }
}

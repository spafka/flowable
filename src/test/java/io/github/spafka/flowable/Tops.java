package io.github.spafka.flowable;

import liquibase.pro.packaged.S;
import lombok.var;
import org.apache.commons.io.FileUtils;
import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.*;
import org.flowable.bpmn.model.Process;
import org.flowable.common.engine.api.io.InputStreamProvider;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class Tops {



    @Test
    public void buildGate() throws IOException {
        String xml = FileUtils.readFileToString(new File("src/main/resources/bpmn/paralGateway.bpmn20.xml"));
        BpmnModel bpmnModel = new BpmnXMLConverter().convertToBpmnModel((InputStreamProvider) () -> new ByteArrayInputStream(xml.getBytes()), false, false);

        Process process = bpmnModel.getProcesses().get(0);

        Collection<FlowElement> flowElements = process.getFlowElements();


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
                source.next.add(target);
                target.pre.add(thiS);
                thiS.next.add(target);
            } else {

            }
        });

        System.out.println();
    }


    @Test
    public void subProcess() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<definitions xmlns=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:activiti=\"http://activiti.org/bpmn\" xmlns:bpmndi=\"http://www.omg.org/spec/BPMN/20100524/DI\" xmlns:omgdc=\"http://www.omg.org/spec/DD/20100524/DC\" xmlns:omgdi=\"http://www.omg.org/spec/DD/20100524/DI\" typeLanguage=\"http://www.w3.org/2001/XMLSchema\" expressionLanguage=\"http://www.w3.org/1999/XPath\" targetNamespace=\"http://www.activiti.org/test\">\n" +
                "  <process id=\"purchase\" name=\"采购单据\" isExecutable=\"true\">\n" +
                "    <startEvent id=\"startevent1\" name=\"Start\"></startEvent>\n" +
                "    <userTask id=\"usertask1\" name=\"提交人\" activiti:assignee=\"${initiator}\" activiti:skipExpression=\"${initiator==''}\"></userTask>\n" +
                "    <sequenceFlow id=\"flow1\" sourceRef=\"startevent1\" targetRef=\"usertask1\"></sequenceFlow>\n" +
                "    <userTask id=\"usertask2\" name=\"领导审批1\" activiti:assignee=\"00000001\"></userTask>\n" +
                "    <sequenceFlow id=\"flow2\" sourceRef=\"usertask1\" targetRef=\"usertask2\"></sequenceFlow>\n" +
                "    <userTask id=\"usertask3\" name=\"联系供货方2\" activiti:assignee=\"00000002\"></userTask>\n" +
                "    <sequenceFlow id=\"flow3\" sourceRef=\"usertask2\" targetRef=\"usertask3\"></sequenceFlow>\n" +
                "    <subProcess id=\"subprocess1\" name=\"付费子流程\">\n" +
                "      <startEvent id=\"startevent2\" name=\"Start\"></startEvent>\n" +
                "      <userTask id=\"usertask4\" name=\"财务审批3\" activiti:assignee=\"00000003\"></userTask>\n" +
                "      <sequenceFlow id=\"flow4\" sourceRef=\"startevent2\" targetRef=\"usertask4\"></sequenceFlow>\n" +
                "      <exclusiveGateway id=\"exclusivegateway1\" name=\"Exclusive Gateway\" default=\"flow7\"></exclusiveGateway>\n" +
                "      <sequenceFlow id=\"flow5\" sourceRef=\"usertask4\" targetRef=\"exclusivegateway1\"></sequenceFlow>\n" +
                "      <userTask id=\"usertask5\" name=\"总经理审批5\" activiti:assignee=\"00000005\"></userTask>\n" +
                "      <sequenceFlow id=\"flow6\" name=\"金额&gt;=1万\" sourceRef=\"exclusivegateway1\" targetRef=\"usertask5\">\n" +
                "        <conditionExpression xsi:type=\"tFormalExpression\"><![CDATA[${money>=10000}]]></conditionExpression>\n" +
                "      </sequenceFlow>\n" +
                "      <userTask id=\"usertask6\" name=\"出纳付款4\" activiti:assignee=\"00000004\"></userTask>\n" +
                "      <sequenceFlow id=\"flow7\" name=\"金额&lt;1万\" sourceRef=\"exclusivegateway1\" targetRef=\"usertask6\"></sequenceFlow>\n" +
                "      <endEvent id=\"endevent1\" name=\"End\"></endEvent>\n" +
                "      <sequenceFlow id=\"flow8\" sourceRef=\"usertask6\" targetRef=\"endevent1\"></sequenceFlow>\n" +
                "      <sequenceFlow id=\"flow9\" sourceRef=\"usertask5\" targetRef=\"usertask6\"></sequenceFlow>\n" +
                "    </subProcess>\n" +
                "    <sequenceFlow id=\"flow10\" sourceRef=\"usertask3\" targetRef=\"subprocess1\"></sequenceFlow>\n" +
                "    <userTask id=\"usertask7\" name=\"收货确认6\" activiti:assignee=\"00000006\"></userTask>\n" +
                "    <sequenceFlow id=\"flow11\" sourceRef=\"subprocess1\" targetRef=\"usertask7\"></sequenceFlow>\n" +
                "    <endEvent id=\"endevent2\" name=\"End\"></endEvent>\n" +
                "    <sequenceFlow id=\"flow12\" sourceRef=\"usertask7\" targetRef=\"endevent2\"></sequenceFlow>\n" +
                "  </process>\n" +
                "  <bpmndi:BPMNDiagram id=\"BPMNDiagram_purchase\">\n" +
                "    <bpmndi:BPMNPlane bpmnElement=\"purchase\" id=\"BPMNPlane_purchase\">\n" +
                "      <bpmndi:BPMNShape bpmnElement=\"startevent1\" id=\"BPMNShape_startevent1\">\n" +
                "        <omgdc:Bounds height=\"35.0\" width=\"35.0\" x=\"40.0\" y=\"30.0\"></omgdc:Bounds>\n" +
                "      </bpmndi:BPMNShape>\n" +
                "      <bpmndi:BPMNShape bpmnElement=\"usertask1\" id=\"BPMNShape_usertask1\">\n" +
                "        <omgdc:Bounds height=\"55.0\" width=\"105.0\" x=\"140.0\" y=\"20.0\"></omgdc:Bounds>\n" +
                "      </bpmndi:BPMNShape>\n" +
                "      <bpmndi:BPMNShape bpmnElement=\"usertask2\" id=\"BPMNShape_usertask2\">\n" +
                "        <omgdc:Bounds height=\"55.0\" width=\"105.0\" x=\"290.0\" y=\"20.0\"></omgdc:Bounds>\n" +
                "      </bpmndi:BPMNShape>\n" +
                "      <bpmndi:BPMNShape bpmnElement=\"usertask3\" id=\"BPMNShape_usertask3\">\n" +
                "        <omgdc:Bounds height=\"55.0\" width=\"105.0\" x=\"440.0\" y=\"20.0\"></omgdc:Bounds>\n" +
                "      </bpmndi:BPMNShape>\n" +
                "      <bpmndi:BPMNShape bpmnElement=\"subprocess1\" id=\"BPMNShape_subprocess1\">\n" +
                "        <omgdc:Bounds height=\"245.0\" width=\"505.0\" x=\"40.0\" y=\"130.0\"></omgdc:Bounds>\n" +
                "      </bpmndi:BPMNShape>\n" +
                "      <bpmndi:BPMNShape bpmnElement=\"startevent2\" id=\"BPMNShape_startevent2\">\n" +
                "        <omgdc:Bounds height=\"35.0\" width=\"35.0\" x=\"66.0\" y=\"170.0\"></omgdc:Bounds>\n" +
                "      </bpmndi:BPMNShape>\n" +
                "      <bpmndi:BPMNShape bpmnElement=\"usertask4\" id=\"BPMNShape_usertask4\">\n" +
                "        <omgdc:Bounds height=\"55.0\" width=\"105.0\" x=\"146.0\" y=\"160.0\"></omgdc:Bounds>\n" +
                "      </bpmndi:BPMNShape>\n" +
                "      <bpmndi:BPMNShape bpmnElement=\"exclusivegateway1\" id=\"BPMNShape_exclusivegateway1\">\n" +
                "        <omgdc:Bounds height=\"40.0\" width=\"40.0\" x=\"296.0\" y=\"168.0\"></omgdc:Bounds>\n" +
                "      </bpmndi:BPMNShape>\n" +
                "      <bpmndi:BPMNShape bpmnElement=\"usertask5\" id=\"BPMNShape_usertask5\">\n" +
                "        <omgdc:Bounds height=\"55.0\" width=\"105.0\" x=\"415.0\" y=\"161.0\"></omgdc:Bounds>\n" +
                "      </bpmndi:BPMNShape>\n" +
                "      <bpmndi:BPMNShape bpmnElement=\"usertask6\" id=\"BPMNShape_usertask6\">\n" +
                "        <omgdc:Bounds height=\"55.0\" width=\"105.0\" x=\"264.0\" y=\"249.0\"></omgdc:Bounds>\n" +
                "      </bpmndi:BPMNShape>\n" +
                "      <bpmndi:BPMNShape bpmnElement=\"endevent1\" id=\"BPMNShape_endevent1\">\n" +
                "        <omgdc:Bounds height=\"35.0\" width=\"35.0\" x=\"299.0\" y=\"330.0\"></omgdc:Bounds>\n" +
                "      </bpmndi:BPMNShape>\n" +
                "      <bpmndi:BPMNShape bpmnElement=\"usertask7\" id=\"BPMNShape_usertask7\">\n" +
                "        <omgdc:Bounds height=\"55.0\" width=\"105.0\" x=\"610.0\" y=\"225.0\"></omgdc:Bounds>\n" +
                "      </bpmndi:BPMNShape>\n" +
                "      <bpmndi:BPMNShape bpmnElement=\"endevent2\" id=\"BPMNShape_endevent2\">\n" +
                "        <omgdc:Bounds height=\"35.0\" width=\"35.0\" x=\"645.0\" y=\"330.0\"></omgdc:Bounds>\n" +
                "      </bpmndi:BPMNShape>\n" +
                "      <bpmndi:BPMNEdge bpmnElement=\"flow1\" id=\"BPMNEdge_flow1\">\n" +
                "        <omgdi:waypoint x=\"75.0\" y=\"47.0\"></omgdi:waypoint>\n" +
                "        <omgdi:waypoint x=\"140.0\" y=\"47.0\"></omgdi:waypoint>\n" +
                "      </bpmndi:BPMNEdge>\n" +
                "      <bpmndi:BPMNEdge bpmnElement=\"flow2\" id=\"BPMNEdge_flow2\">\n" +
                "        <omgdi:waypoint x=\"245.0\" y=\"47.0\"></omgdi:waypoint>\n" +
                "        <omgdi:waypoint x=\"290.0\" y=\"47.0\"></omgdi:waypoint>\n" +
                "      </bpmndi:BPMNEdge>\n" +
                "      <bpmndi:BPMNEdge bpmnElement=\"flow3\" id=\"BPMNEdge_flow3\">\n" +
                "        <omgdi:waypoint x=\"395.0\" y=\"47.0\"></omgdi:waypoint>\n" +
                "        <omgdi:waypoint x=\"440.0\" y=\"47.0\"></omgdi:waypoint>\n" +
                "      </bpmndi:BPMNEdge>\n" +
                "      <bpmndi:BPMNEdge bpmnElement=\"flow4\" id=\"BPMNEdge_flow4\">\n" +
                "        <omgdi:waypoint x=\"101.0\" y=\"187.0\"></omgdi:waypoint>\n" +
                "        <omgdi:waypoint x=\"146.0\" y=\"187.0\"></omgdi:waypoint>\n" +
                "      </bpmndi:BPMNEdge>\n" +
                "      <bpmndi:BPMNEdge bpmnElement=\"flow5\" id=\"BPMNEdge_flow5\">\n" +
                "        <omgdi:waypoint x=\"251.0\" y=\"187.0\"></omgdi:waypoint>\n" +
                "        <omgdi:waypoint x=\"296.0\" y=\"188.0\"></omgdi:waypoint>\n" +
                "      </bpmndi:BPMNEdge>\n" +
                "      <bpmndi:BPMNEdge bpmnElement=\"flow6\" id=\"BPMNEdge_flow6\">\n" +
                "        <omgdi:waypoint x=\"336.0\" y=\"188.0\"></omgdi:waypoint>\n" +
                "        <omgdi:waypoint x=\"415.0\" y=\"188.0\"></omgdi:waypoint>\n" +
                "        <bpmndi:BPMNLabel>\n" +
                "          <omgdc:Bounds height=\"14.0\" width=\"100.0\" x=\"340.0\" y=\"157.0\"></omgdc:Bounds>\n" +
                "        </bpmndi:BPMNLabel>\n" +
                "      </bpmndi:BPMNEdge>\n" +
                "      <bpmndi:BPMNEdge bpmnElement=\"flow7\" id=\"BPMNEdge_flow7\">\n" +
                "        <omgdi:waypoint x=\"316.0\" y=\"208.0\"></omgdi:waypoint>\n" +
                "        <omgdi:waypoint x=\"316.0\" y=\"249.0\"></omgdi:waypoint>\n" +
                "        <bpmndi:BPMNLabel>\n" +
                "          <omgdc:Bounds height=\"14.0\" width=\"100.0\" x=\"318.0\" y=\"220.0\"></omgdc:Bounds>\n" +
                "        </bpmndi:BPMNLabel>\n" +
                "      </bpmndi:BPMNEdge>\n" +
                "      <bpmndi:BPMNEdge bpmnElement=\"flow8\" id=\"BPMNEdge_flow8\">\n" +
                "        <omgdi:waypoint x=\"316.0\" y=\"304.0\"></omgdi:waypoint>\n" +
                "        <omgdi:waypoint x=\"316.0\" y=\"330.0\"></omgdi:waypoint>\n" +
                "      </bpmndi:BPMNEdge>\n" +
                "      <bpmndi:BPMNEdge bpmnElement=\"flow9\" id=\"BPMNEdge_flow9\">\n" +
                "        <omgdi:waypoint x=\"467.0\" y=\"216.0\"></omgdi:waypoint>\n" +
                "        <omgdi:waypoint x=\"467.0\" y=\"276.0\"></omgdi:waypoint>\n" +
                "        <omgdi:waypoint x=\"369.0\" y=\"276.0\"></omgdi:waypoint>\n" +
                "      </bpmndi:BPMNEdge>\n" +
                "      <bpmndi:BPMNEdge bpmnElement=\"flow10\" id=\"BPMNEdge_flow10\">\n" +
                "        <omgdi:waypoint x=\"492.0\" y=\"75.0\"></omgdi:waypoint>\n" +
                "        <omgdi:waypoint x=\"492.0\" y=\"111.0\"></omgdi:waypoint>\n" +
                "        <omgdi:waypoint x=\"292.0\" y=\"130.0\"></omgdi:waypoint>\n" +
                "      </bpmndi:BPMNEdge>\n" +
                "      <bpmndi:BPMNEdge bpmnElement=\"flow11\" id=\"BPMNEdge_flow11\">\n" +
                "        <omgdi:waypoint x=\"545.0\" y=\"252.0\"></omgdi:waypoint>\n" +
                "        <omgdi:waypoint x=\"610.0\" y=\"252.0\"></omgdi:waypoint>\n" +
                "      </bpmndi:BPMNEdge>\n" +
                "      <bpmndi:BPMNEdge bpmnElement=\"flow12\" id=\"BPMNEdge_flow12\">\n" +
                "        <omgdi:waypoint x=\"662.0\" y=\"280.0\"></omgdi:waypoint>\n" +
                "        <omgdi:waypoint x=\"662.0\" y=\"330.0\"></omgdi:waypoint>\n" +
                "      </bpmndi:BPMNEdge>\n" +
                "    </bpmndi:BPMNPlane>\n" +
                "  </bpmndi:BPMNDiagram>\n" +
                "</definitions>";

        BpmnModel bpmnModel = new BpmnXMLConverter().convertToBpmnModel((InputStreamProvider) () -> new ByteArrayInputStream(xml.getBytes()), false, false);


        Process process = bpmnModel.getProcesses().get(0);

        Collection<FlowElement> flowElements = process.getFlowElements();


        Map<String, FlowElement> collect1 = flowElements.stream().collect(Collectors.toMap(BaseElement::getId, x -> x));

        List<SequenceFlow> seq = flowElements.stream().filter(x -> x instanceof SequenceFlow).map(x -> (SequenceFlow) x).collect(Collectors.toList());
        flowElements.removeAll(seq);
        List<FlowElement> collect = new ArrayList<>(flowElements);
        FlowElement start = collect.stream().filter(x -> x instanceof StartEvent).findFirst().get();
        TopologyNode<FlowElement> root = new TopologyNode<>(start);

        Map<String, TopologyNode<FlowElement>> cache = new HashMap<>();
        cache.put(start.getId(), root);

        seq.forEach(x -> {
            TopologyNode<FlowElement> source = cache.computeIfAbsent(x.getSourceRef(), s -> new TopologyNode<>(collect1.get(s)));
            TopologyNode<FlowElement> target = cache.computeIfAbsent(x.getTargetRef(), s -> new TopologyNode<>(collect1.get(s)));
            source.next.add(target);
            target.pre.add(source);
        });

        System.out.println();


    }


}
package io.github.spafka.flowable;

import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.bpmn.model.UserTask;
import org.flowable.common.engine.api.io.InputStreamProvider;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class Tops {


    @Test
    public void buildGate() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<definitions xmlns=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "             xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:flowable=\"http://flowable.org/bpmn\"\n" +
                "             xmlns:bpmndi=\"http://www.omg.org/spec/BPMN/20100524/DI\"\n" +
                "             xmlns:omgdc=\"http://www.omg.org/spec/DD/20100524/DC\" xmlns:omgdi=\"http://www.omg.org/spec/DD/20100524/DI\"\n" +
                "             typeLanguage=\"http://www.w3.org/2001/XMLSchema\" expressionLanguage=\"http://www.w3.org/1999/XPath\"\n" +
                "             targetNamespace=\"http://www.flowable.org/processdef\">\n" +
                "    <process id=\"holiday\" name=\"holiday\" isExecutable=\"true\">\n" +
                "        <startEvent id=\"sid-de544e5d-e955-416c-9d66-246e01453a90\"/>\n" +
                "        <userTask id=\"sid-c222eb6f-7681-4e68-966b-ae4692ef2807\" flowable:assignee=\"${initiator}\" name=\"发起人审批\"/>\n" +
                "        <sequenceFlow id=\"sid-36c80ccd-e866-40dc-af64-47a0c9bd107b\" sourceRef=\"sid-de544e5d-e955-416c-9d66-246e01453a90\"\n" +
                "                      targetRef=\"sid-c222eb6f-7681-4e68-966b-ae4692ef2807\"/>\n" +
                "        <exclusiveGateway id=\"sid-e4799473-4265-48a6-a34f-4190a498ff20\"/>\n" +
                "        <sequenceFlow id=\"sid-edc3eade-7b55-4ed1-86a2-ceb3bd011a42\" sourceRef=\"sid-c222eb6f-7681-4e68-966b-ae4692ef2807\"\n" +
                "                      targetRef=\"sid-e4799473-4265-48a6-a34f-4190a498ff20\"/>\n" +
                "        <userTask id=\"sid-000e33db-470a-4c11-a0e7-2e10d22f897a\" name=\"张三审批\" flowable:candidateUsers=\"zhangsan\"\n" +
                "                  flowable:assignee=\"zhangsan\"/>\n" +
                "        <sequenceFlow id=\"sid-90e30751-e137-41be-a3e3-090016db6fd5\" sourceRef=\"sid-e4799473-4265-48a6-a34f-4190a498ff20\"\n" +
                "                      targetRef=\"sid-000e33db-470a-4c11-a0e7-2e10d22f897a\">\n" +
                "            <conditionExpression xsi:type=\"tFormalExpression\"><![CDATA[${days<3}]]></conditionExpression>\n" +
                "        </sequenceFlow>\n" +
                "        <parallelGateway id=\"sid-b77a7430-8e7f-40bb-a37c-9d7162012382\"/>\n" +
                "        <sequenceFlow id=\"sid-eac0cb27-3648-4029-aa71-bc3cf377734d\" sourceRef=\"sid-e4799473-4265-48a6-a34f-4190a498ff20\"\n" +
                "                      targetRef=\"sid-b77a7430-8e7f-40bb-a37c-9d7162012382\">\n" +
                "            <conditionExpression xsi:type=\"tFormalExpression\"/>\n" +
                "        </sequenceFlow>\n" +
                "        <userTask id=\"sid-badfc7b3-e990-4537-b8cc-9480147b0e0e\" name=\"李四审批\" flowable:candidateUsers=\"lisi\"\n" +
                "                  flowable:assignee=\"lisi\"/>\n" +
                "        <sequenceFlow id=\"sid-28d604b0-612a-42df-984a-496bd4066118\" sourceRef=\"sid-b77a7430-8e7f-40bb-a37c-9d7162012382\"\n" +
                "                      targetRef=\"sid-badfc7b3-e990-4537-b8cc-9480147b0e0e\">\n" +
                "            <conditionExpression xsi:type=\"tFormalExpression\"/>\n" +
                "        </sequenceFlow>\n" +
                "        <userTask id=\"sid-aabb87b8-c8e4-4662-bb60-19641f207d57\" name=\"王五审批\" flowable:candidateUsers=\"wangwu\"\n" +
                "                  flowable:assignee=\"wangwu\"/>\n" +
                "        <sequenceFlow id=\"sid-65821bbd-eb4c-4211-9a4c-83a77928c36e\" sourceRef=\"sid-b77a7430-8e7f-40bb-a37c-9d7162012382\"\n" +
                "                      targetRef=\"sid-aabb87b8-c8e4-4662-bb60-19641f207d57\">\n" +
                "            <conditionExpression xsi:type=\"tFormalExpression\"><![CDATA[${days>=3}]]></conditionExpression>\n" +
                "        </sequenceFlow>\n" +
                "        <parallelGateway id=\"sid-4475d7e0-b067-42bb-8e55-b44103e100fb\"/>\n" +
                "        <sequenceFlow id=\"sid-228c58ec-5f9c-4505-b350-10fff5eb92c9\" sourceRef=\"sid-badfc7b3-e990-4537-b8cc-9480147b0e0e\"\n" +
                "                      targetRef=\"sid-4475d7e0-b067-42bb-8e55-b44103e100fb\"/>\n" +
                "        <sequenceFlow id=\"sid-7358b14f-25de-4ded-8a8e-82cf04e2234d\" sourceRef=\"sid-aabb87b8-c8e4-4662-bb60-19641f207d57\"\n" +
                "                      targetRef=\"sid-4475d7e0-b067-42bb-8e55-b44103e100fb\"/>\n" +
                "        <userTask id=\"asd\" name=\"赵六审批\" flowable:assignee=\"zhaoliu\" flowable:candidateUsers=\"zhaoliu\"/>\n" +
                "        <sequenceFlow id=\"sid-5860209a-1eab-4973-8e96-b78066411d8f\" sourceRef=\"sid-4475d7e0-b067-42bb-8e55-b44103e100fb\"\n" +
                "                      targetRef=\"asd\">\n" +
                "            <conditionExpression xsi:type=\"tFormalExpression\"/>\n" +
                "        </sequenceFlow>\n" +
                "        <sequenceFlow id=\"sid-5e6dc23b-9aaa-4468-bb35-c533c854281c\" sourceRef=\"sid-000e33db-470a-4c11-a0e7-2e10d22f897a\"\n" +
                "                      targetRef=\"asd\"/>\n" +
                "        <endEvent id=\"sid-02a894c3-de97-4389-867b-b0e94e7552e2\"/>\n" +
                "        <sequenceFlow id=\"sid-7cea80d2-7281-4265-acec-37d7189f9e77\" sourceRef=\"asd\"\n" +
                "                      targetRef=\"sid-02a894c3-de97-4389-867b-b0e94e7552e2\"/>\n" +
                "    </process>\n" +
                "    <bpmndi:BPMNDiagram id=\"BPMNDiagram_holiday\">\n" +
                "        <bpmndi:BPMNPlane bpmnElement=\"holiday\" id=\"BPMNPlane_holiday\">\n" +
                "            <bpmndi:BPMNShape id=\"shape-9941c8ef-b400-440e-ae78-4aa264cbd05b\"\n" +
                "                              bpmnElement=\"sid-de544e5d-e955-416c-9d66-246e01453a90\">\n" +
                "                <omgdc:Bounds x=\"-223.33368\" y=\"-12.100881\" width=\"30.0\" height=\"30.0\"/>\n" +
                "            </bpmndi:BPMNShape>\n" +
                "            <bpmndi:BPMNShape id=\"shape-bd9b3e88-23be-405e-9a09-e44c4c9a5976\"\n" +
                "                              bpmnElement=\"sid-c222eb6f-7681-4e68-966b-ae4692ef2807\">\n" +
                "                <omgdc:Bounds x=\"-150.5\" y=\"-31.25\" width=\"100.0\" height=\"80.0\"/>\n" +
                "            </bpmndi:BPMNShape>\n" +
                "            <bpmndi:BPMNEdge id=\"edge-f35ef6a6-0b4b-4fcb-8fe4-c04b93b38433\"\n" +
                "                             bpmnElement=\"sid-36c80ccd-e866-40dc-af64-47a0c9bd107b\">\n" +
                "                <omgdi:waypoint x=\"-193.33368\" y=\"10.399119\"/>\n" +
                "                <omgdi:waypoint x=\"-150.5\" y=\"8.75\"/>\n" +
                "            </bpmndi:BPMNEdge>\n" +
                "            <bpmndi:BPMNShape id=\"shape-5ae9c72b-3773-47fe-bd7f-0b25ab42149a\"\n" +
                "                              bpmnElement=\"sid-e4799473-4265-48a6-a34f-4190a498ff20\">\n" +
                "                <omgdc:Bounds x=\"6.0\" y=\"-11.25\" width=\"40.0\" height=\"40.0\"/>\n" +
                "            </bpmndi:BPMNShape>\n" +
                "            <bpmndi:BPMNEdge id=\"edge-33a95efa-4c99-4a97-8c32-f76f297efe3b\"\n" +
                "                             bpmnElement=\"sid-edc3eade-7b55-4ed1-86a2-ceb3bd011a42\">\n" +
                "                <omgdi:waypoint x=\"-50.5\" y=\"8.75\"/>\n" +
                "                <omgdi:waypoint x=\"6.0\" y=\"8.75\"/>\n" +
                "            </bpmndi:BPMNEdge>\n" +
                "            <bpmndi:BPMNShape id=\"shape-d9b7fb01-a7aa-47f8-890c-ce57dd41346f\"\n" +
                "                              bpmnElement=\"sid-000e33db-470a-4c11-a0e7-2e10d22f897a\">\n" +
                "                <omgdc:Bounds x=\"27.5\" y=\"-146.75\" width=\"100.0\" height=\"80.0\"/>\n" +
                "            </bpmndi:BPMNShape>\n" +
                "            <bpmndi:BPMNEdge id=\"edge-c5fac3a4-89ec-4d1c-85a1-ef34e6dbc5ff\"\n" +
                "                             bpmnElement=\"sid-90e30751-e137-41be-a3e3-090016db6fd5\">\n" +
                "                <omgdi:waypoint x=\"26.0\" y=\"-11.25\"/>\n" +
                "                <omgdi:waypoint x=\"27.5\" y=\"-86.75\"/>\n" +
                "            </bpmndi:BPMNEdge>\n" +
                "            <bpmndi:BPMNShape id=\"shape-5efcb707-312a-46ef-a36d-84e3aa47debd\"\n" +
                "                              bpmnElement=\"sid-b77a7430-8e7f-40bb-a37c-9d7162012382\">\n" +
                "                <omgdc:Bounds x=\"6.0\" y=\"41.75\" width=\"40.0\" height=\"40.0\"/>\n" +
                "            </bpmndi:BPMNShape>\n" +
                "            <bpmndi:BPMNEdge id=\"edge-4dac611a-7764-424e-89c8-2742dd8f6094\"\n" +
                "                             bpmnElement=\"sid-eac0cb27-3648-4029-aa71-bc3cf377734d\">\n" +
                "                <omgdi:waypoint x=\"26.0\" y=\"28.75\"/>\n" +
                "                <omgdi:waypoint x=\"26.0\" y=\"41.75\"/>\n" +
                "            </bpmndi:BPMNEdge>\n" +
                "            <bpmndi:BPMNShape id=\"shape-77a49085-5955-4f58-9b8a-55c90b8f7e86\"\n" +
                "                              bpmnElement=\"sid-badfc7b3-e990-4537-b8cc-9480147b0e0e\">\n" +
                "                <omgdc:Bounds x=\"98.5\" y=\"0.75\" width=\"100.0\" height=\"80.0\"/>\n" +
                "            </bpmndi:BPMNShape>\n" +
                "            <bpmndi:BPMNEdge id=\"edge-dfd9c3e1-3fa0-427f-9275-14bd2d6a168d\"\n" +
                "                             bpmnElement=\"sid-28d604b0-612a-42df-984a-496bd4066118\">\n" +
                "                <omgdi:waypoint x=\"46.0\" y=\"61.75\"/>\n" +
                "                <omgdi:waypoint x=\"98.5\" y=\"60.75\"/>\n" +
                "            </bpmndi:BPMNEdge>\n" +
                "            <bpmndi:BPMNShape id=\"shape-aefc6586-e39f-4fb5-9de9-8ebbbee20801\"\n" +
                "                              bpmnElement=\"sid-aabb87b8-c8e4-4662-bb60-19641f207d57\">\n" +
                "                <omgdc:Bounds x=\"27.499992\" y=\"128.93782\" width=\"100.0\" height=\"80.0\"/>\n" +
                "            </bpmndi:BPMNShape>\n" +
                "            <bpmndi:BPMNEdge id=\"edge-958a9b19-1974-4e1f-8703-a5f8c18605a3\"\n" +
                "                             bpmnElement=\"sid-65821bbd-eb4c-4211-9a4c-83a77928c36e\">\n" +
                "                <omgdi:waypoint x=\"26.0\" y=\"81.75\"/>\n" +
                "                <omgdi:waypoint x=\"27.499992\" y=\"168.93782\"/>\n" +
                "            </bpmndi:BPMNEdge>\n" +
                "            <bpmndi:BPMNShape id=\"shape-e5022dc6-9491-47ea-a04f-e6de7bf5e90e\"\n" +
                "                              bpmnElement=\"sid-4475d7e0-b067-42bb-8e55-b44103e100fb\">\n" +
                "                <omgdc:Bounds x=\"252.5\" y=\"41.75\" width=\"40.0\" height=\"40.0\"/>\n" +
                "            </bpmndi:BPMNShape>\n" +
                "            <bpmndi:BPMNEdge id=\"edge-9f675e2e-47cc-4f95-9ad6-31a23a7b1e08\"\n" +
                "                             bpmnElement=\"sid-228c58ec-5f9c-4505-b350-10fff5eb92c9\">\n" +
                "                <omgdi:waypoint x=\"198.5\" y=\"60.75\"/>\n" +
                "                <omgdi:waypoint x=\"252.5\" y=\"61.75\"/>\n" +
                "            </bpmndi:BPMNEdge>\n" +
                "            <bpmndi:BPMNEdge id=\"edge-5ad7fd87-8e17-4802-ad2d-79cd6f4d1689\"\n" +
                "                             bpmnElement=\"sid-7358b14f-25de-4ded-8a8e-82cf04e2234d\">\n" +
                "                <omgdi:waypoint x=\"127.49999\" y=\"148.93782\"/>\n" +
                "                <omgdi:waypoint x=\"272.5\" y=\"81.75\"/>\n" +
                "            </bpmndi:BPMNEdge>\n" +
                "            <bpmndi:BPMNShape id=\"shape-12d417a9-85ce-45b2-96bd-71e5b5f6d74e\" bpmnElement=\"asd\">\n" +
                "                <omgdc:Bounds x=\"291.25128\" y=\"-103.44638\" width=\"100.0\" height=\"80.0\"/>\n" +
                "            </bpmndi:BPMNShape>\n" +
                "            <bpmndi:BPMNEdge id=\"edge-9f475396-6117-472a-b0aa-03e4e3b4e4e5\"\n" +
                "                             bpmnElement=\"sid-5860209a-1eab-4973-8e96-b78066411d8f\">\n" +
                "                <omgdi:waypoint x=\"292.5\" y=\"61.75\"/>\n" +
                "                <omgdi:waypoint x=\"291.25128\" y=\"-63.44638\"/>\n" +
                "            </bpmndi:BPMNEdge>\n" +
                "            <bpmndi:BPMNEdge id=\"edge-10f2c638-9986-4400-998f-f9f137d9df75\"\n" +
                "                             bpmnElement=\"sid-5e6dc23b-9aaa-4468-bb35-c533c854281c\">\n" +
                "                <omgdi:waypoint x=\"127.5\" y=\"-86.75\"/>\n" +
                "                <omgdi:waypoint x=\"291.25128\" y=\"-83.44638\"/>\n" +
                "            </bpmndi:BPMNEdge>\n" +
                "            <bpmndi:BPMNShape id=\"shape-a71823a8-5803-42cd-a27a-e83907ef9efb\"\n" +
                "                              bpmnElement=\"sid-02a894c3-de97-4389-867b-b0e94e7552e2\">\n" +
                "                <omgdc:Bounds x=\"569.13794\" y=\"-53.801155\" width=\"30.0\" height=\"30.0\"/>\n" +
                "            </bpmndi:BPMNShape>\n" +
                "            <bpmndi:BPMNEdge id=\"edge-5a46436a-5b0f-4b2c-9673-7675ae6e8826\"\n" +
                "                             bpmnElement=\"sid-7cea80d2-7281-4265-acec-37d7189f9e77\">\n" +
                "                <omgdi:waypoint x=\"391.25128\" y=\"-43.44638\"/>\n" +
                "                <omgdi:waypoint x=\"563.7425\" y=\"-38.801155\"/>\n" +
                "            </bpmndi:BPMNEdge>\n" +
                "        </bpmndi:BPMNPlane>\n" +
                "    </bpmndi:BPMNDiagram>\n" +
                "</definitions>\n";
        BpmnModel bpmnModel = new BpmnXMLConverter().convertToBpmnModel((InputStreamProvider) () -> new ByteArrayInputStream(xml.getBytes()), false, false);

        Process process = bpmnModel.getProcesses().get(0);

        Collection<FlowElement> flowElements = process.getFlowElements();

        List<FlowElement> seq = flowElements.stream().filter(x -> x instanceof SequenceFlow).collect(Collectors.toList());
        flowElements.removeAll(seq);
        List<FlowElement> collect = flowElements.stream().collect(Collectors.toList());

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

        List<FlowElement> seq = flowElements.stream().filter(x -> x instanceof SequenceFlow).collect(Collectors.toList());
        flowElements.removeAll(seq);
        List<FlowElement> collect = flowElements.stream().collect(Collectors.toList());


    }

    class TopologyNode {
        private String id;
        private String name;
        private List<io.github.spafka.flowable.TopologyNode> nextNodes;
        private List<io.github.spafka.flowable.TopologyNode> preNodes;

        public TopologyNode(String id, String name) {
            this.id = id;
            this.name = name;
            this.nextNodes = new ArrayList<>();
            this.preNodes = new ArrayList<>();
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public List<io.github.spafka.flowable.TopologyNode> getNextNodes() {
            return nextNodes;
        }

        public List<io.github.spafka.flowable.TopologyNode> getPreNodes() {
            return preNodes;
        }

        public void addNextNode(io.github.spafka.flowable.TopologyNode node) {
            nextNodes.add(node);
        }

        public void addPreNode(io.github.spafka.flowable.TopologyNode node) {
            preNodes.add(node);
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
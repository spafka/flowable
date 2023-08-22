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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class Tops {

    public static void main(String[] args) {
        // 创建一个流程定义模型
        BpmnModel bpmnModel = new BpmnModel();

        // 创建一个流程
        Process process = new Process();
        process.setId("process1");
        bpmnModel.addProcess(process);

        // 创建开始事件
        FlowElement startEvent = createFlowElement("startEvent", "Start Event");
        process.addFlowElement(startEvent);

        // 创建用户任务1
        FlowElement userTask1 = createFlowElement("userTask1", "User Task 1");
        process.addFlowElement(userTask1);

        // 创建用户任务2
        FlowElement userTask2 = createFlowElement("userTask2", "User Task 2");
        process.addFlowElement(userTask2);

        // 创建结束事件
        FlowElement endEvent = createFlowElement("endEvent", "End Event");
        process.addFlowElement(endEvent);

        // 创建连接关系
        createSequenceFlow(process, "flow1", startEvent.getId(), userTask1.getId());
        createSequenceFlow(process, "flow2", userTask1.getId(), userTask2.getId());
        createSequenceFlow(process, "flow3", userTask2.getId(), endEvent.getId());

        // 构建拓扑图
        Map<String, String> topologyGraph = buildTopologyGraph(bpmnModel);
        System.out.println(topologyGraph);
    }

    private static FlowElement createFlowElement(String id, String name) {
        UserTask userTask = new UserTask();
        userTask.setId(id);
        userTask.setName(name);
        return userTask;
    }

    private static void createSequenceFlow(Process process, String id, String sourceRef, String targetRef) {
        SequenceFlow sequenceFlow = new SequenceFlow();
        sequenceFlow.setId(id);
        sequenceFlow.setSourceRef(sourceRef);
        sequenceFlow.setTargetRef(targetRef);
        process.addFlowElement(sequenceFlow);
    }

    private static Map<String, String> buildTopologyGraph(BpmnModel bpmnModel) {
        Map<String, String> topologyGraph = new HashMap<>();

        Process process = bpmnModel.getMainProcess();
        for (FlowElement flowElement : process.getFlowElements()) {
            topologyGraph.put(flowElement.getId(), flowElement.getName());
        }

        return topologyGraph;
    }

    @Test
    public void build(){
        BpmnModel bpmnModel = new BpmnXMLConverter().convertToBpmnModel((InputStreamProvider) () -> new ByteArrayInputStream(("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
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
                "</definitions>\n").getBytes()), false, false);

        Process process = bpmnModel.getProcesses().get(0);

        Collection<FlowElement> flowElements = process.getFlowElements();

        System.out.println();
    }
}
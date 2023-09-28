package io.github.spafka.flowable;

import io.github.spafka.flowable.core.FlowableUtils;
import io.github.spafka.flowable.core.TopologyNode;
import io.github.spafka.flowable.service.Graphs;
import io.vavr.Tuple3;
import lombok.var;
import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.*;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class GraphTests {


    public BpmnModel init(String path) {
        return new BpmnXMLConverter().convertToBpmnModel(() -> {
            try {

                return Files.newInputStream(Paths.get(path));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, false, false);
    }

    @Test
    public void testSimple() {

        BpmnModel bpmnModel = init("src/main/resources/returntest/简单并行网关.bpmn20.xml");

        var jumpTypeEnum = Graphs.backTrack(bpmnModel, "T5", "T3");

        System.out.println();
    }

    @Test
    public void testmultigate() {

        BpmnModel bpmnModel = init("src/main/resources/returntest/多路并行网关.bpmn20.xml");

        var jumpTypeEnum = Graphs.backTrack(bpmnModel, "T4", "T2-1");
        //var jumpTypeEnum2 = Graphs.backStace(bpmnModel, "T4", "T3-1");

        System.out.println();
    }

    @Test
    public void test3() {

        BpmnModel bpmnModel = init("src/main/resources/returntest/1开3并行流.bpmn20.xml");

        var tuple = Graphs.backTrack(bpmnModel, "T3", "T2-1");


        System.out.println();
    }

    @Test
    public void test5loopoutcoming() {

        BpmnModel bpmnModel = init("src/main/resources/returntest/请假申请.bpmn20.xml");

        var tuple = Graphs.backTrack(bpmnModel, "T4", "T1");

        System.out.println();

    }

    @Test
    public void testloopoutcoming() {

        BpmnModel bpmnModel = init("src/main/resources/returntest/请假申请.bpmn20.xml");

        var tuple = Graphs.backTrack(bpmnModel, "T4", "T1");

        System.out.println();

    }

    @Test
    public void testcomplexpg() {

        BpmnModel bpmnModel = init("src/main/resources/returntest/复杂并行网关.bpmn20.xml");

        var tuple = Graphs.backTrack(bpmnModel, "T7", "T5");

        var tuple3 = Graphs.backTrack(bpmnModel, "T7", "T4");

        System.out.println();

    }

    @Test
    public void testcomplexpg2() {

        BpmnModel bpmnModel = init("src/main/resources/returntest/复杂并行网关.bpmn20.xml");

        var tuple = Graphs.backTrack(bpmnModel, "T5", "T2");

        System.out.println();

    }


    @Test
    public void testSupos() {
        String xml = "src/main/resources/bpmn/回归测试.bpmn20.xml";
        BpmnModel bpmnModel = init(xml);

        var jumpTypeEnumListSetTuple3 = Graphs.backTrack(bpmnModel, "EndEvent_1621823740971", "");
        System.out.println();

    }

    @Test
    public void testSubProcess() {
        String xml = "src/main/resources/returntest/嵌套子流程2.bpmn20.xml";
        BpmnModel bpmnModel = init(xml);

        var r = Graphs.backTrack(bpmnModel, "T6", "T1");
        System.out.println();

    }

    @Test
    public void testSubProcess2() {
        String xml = "src/main/resources/returntest/嵌套子流程3.bpmn20.xml";
        BpmnModel bpmnModel = init(xml);

        var r = Graphs.backTrack(bpmnModel, "T4", "T11");
        System.out.println();

    }

    @Test
    public void testSubProcess3() {
        String xml = "src/main/resources/returntest/嵌套相容子流程.bpmn20.xml";
        BpmnModel bpmnModel = init(xml);

        var r = Graphs.backTrack(bpmnModel, "T4", "T11");
        System.out.println();

    }

    @Test
    public void testSubProcess43() {
        String xml = "src/main/resources/returntest/嵌套相容子流程.bpmn20.xml";
        BpmnModel bpmnModel = init(xml);

        var r = Graphs.backTrack(bpmnModel, "T8", "T1");
        System.out.println();

    }

    @Test
    public void testSubProcess44() {
        String xml = "supos.bpmn20.xml";
        BpmnModel bpmnModel = init(xml);

        var r = Graphs.backTrack(bpmnModel, "UserTask_1693389822468", "UserTask_1693271307830");
        System.out.println();

    }

    @Test
    public void testSubProcess45() {
        String xml = "src/main/resources/returntest/简单串行测试.bpmn20.xml";
        BpmnModel bpmnModel = init(xml);

        var r = Graphs.backTrack(bpmnModel, null, null);
        System.out.println();

    }

    @Test
    public void testInclusiveGate() {
        String xml = "嵌套相容子流程.bpmn20.xml";
        BpmnModel bpmnModel = init(xml);

        var r = Graphs.backTrack(bpmnModel, null, null);
        System.out.println();

    }

    @Test
    public void testSupos1() {
        String xml = "supos.bpmn20.xml";
        BpmnModel bpmnModel = init(xml);

        var r = Graphs.backTrack(bpmnModel, "UserTask_1621823731368", null);
        System.out.println();

    }

    @Test
    public void testStartTimeEvent() {
        String xml = "src/main/resources/event/start_time_event.bpmn20.xml";
        BpmnModel bpmnModel = init(xml);

        Collection<FlowElement> flowElements = bpmnModel.getMainProcess().getFlowElements();

        var r = Graphs.backTrack(bpmnModel, null, null);
        System.out.println();

    }

    @Test
    public void testSimpleTimeEvent() {
        String xml = "aa.xml";
        BpmnModel bpmnModel = init(xml);

        Collection<FlowElement> flowElements = bpmnModel.getMainProcess().getFlowElements();


        var r = Graphs.backTrack(bpmnModel, "UserTask_1694589756473", "UserTask_1694588579853");

        boolean reachable = FlowableUtils.isReachable(bpmnModel.getMainProcess(), "UserTask_1694589756473", "UserTask_1694588579853");
        boolean reachable1 = FlowableUtils.isReachable(bpmnModel.getMainProcess(), "UserTask_1694588579853", "UserTask_1694589756473");
        System.out.println();

    }


    @Test
    public void testPgNoJoin() {

        String xml =
                "src/main/resources/returntest/1开3并行流3end.bpmn20.xml";
        BpmnModel bpmnModel = init(xml);
        var r = Graphs.backTrack(bpmnModel, null, null);

        System.out.printf("");


    }


    public static void main(String[] args) {
        try {
            try {
                // 加载XML文件
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document doc = builder.parse(new File("src/main/resources/returntest/1开3并行流.bpmn20.xml")); // 替换成实际的XML文件路径

                // 查找所有<process>元素
                NodeList processNodes = doc.getElementsByTagName("process");

                // 遍历每个<process>元素
                for (int i = 0; i < processNodes.getLength(); i++) {
                    Element processElement = (Element) processNodes.item(i);

                    // 获取globalViewUrl属性的值
                    String globalViewUrl = processElement.getAttribute("flowable:globalViewUrl2");
                    System.out.println("globalViewUrl value: " + globalViewUrl);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



}

package io.github.spafka.flowable.deep;

import io.github.spafka.flowable.JumpTypeEnum;
import io.github.spafka.flowable.core.TopologyNode;
import io.github.spafka.flowable.service.Graphs;
import io.vavr.Tuple3;
import lombok.var;
import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class GraphTests {


    static String th = "3层并行网关";

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

        BpmnModel bpmnModel = init("src/main/resources/returntest/3层并行网关.bpmn20.xml");

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

        var tuple = Graphs.backTrack(bpmnModel, "T8", "T3");

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
}

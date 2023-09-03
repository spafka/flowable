package io.github.spafka.flowable.returnTests;

import io.github.spafka.flowable.JumpTypeEnum;
import io.github.spafka.flowable.core.TopologyNode;
import io.github.spafka.flowable.service.Graphs;
import lombok.var;
import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

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

        var jumpTypeEnum = Graphs.backStace(bpmnModel, "T5", "T3");

        System.out.println();
    }

    @Test
    public void testmultigate() {

        BpmnModel bpmnModel = init("src/main/resources/returntest/多路并行网关.bpmn20.xml");

        var jumpTypeEnum = Graphs.backStace(bpmnModel, "T4", "T2-1");
        var jumpTypeEnum2 = Graphs.backStace(bpmnModel, "T4", "T3-1");

        System.out.println();
    }

    @Test
    public void test3() {

        BpmnModel bpmnModel = init("src/main/resources/returntest/1开3并行流.bpmn20.xml");

        var tuple = Graphs.backStace(bpmnModel, "T3", "T2-1");


        System.out.println();
    }

    @Test
    public void test5loopoutcoming() {

        BpmnModel bpmnModel = init("src/main/resources/returntest/请假申请.bpmn20.xml");

        var tuple = Graphs.backStace(bpmnModel, "T4", "T1");

        System.out.println();

    }

    @Test
    public void testloopoutcoming() {

        BpmnModel bpmnModel = init("src/main/resources/returntest/请假申请.bpmn20.xml");

        var tuple = Graphs.backStace(bpmnModel, "T4", "T1");

        System.out.println();

    }

    @Test
    public void testcomplexpg() {

        BpmnModel bpmnModel = init("src/main/resources/returntest/复杂并行网关.bpmn20.xml");

        var tuple = Graphs.backStace(bpmnModel, "T7", "T5");

        System.out.println();

    }

}

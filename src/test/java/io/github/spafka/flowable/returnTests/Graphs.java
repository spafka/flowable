package io.github.spafka.flowable.returnTests;

import io.github.spafka.flowable.JumpTypeEnum;
import io.github.spafka.flowable.core.TopologyNode;
import lombok.var;
import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Graphs {


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
    public void test() {

        BpmnModel bpmnModel = init("src/main/resources/returntest/简单并行网关.bpmn20.xml");

        JumpTypeEnum jumpTypeEnum = io.github.spafka.flowable.service.Graphs.backStace(bpmnModel, "T5", "T2");

        System.out.println();
    }
}

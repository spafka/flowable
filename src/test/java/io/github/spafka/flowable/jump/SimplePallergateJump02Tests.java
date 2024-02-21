package io.github.spafka.flowable.jump;

import io.github.spafka.flowable.core.FlowService;
import io.github.spafka.flowable.core.TopologyNode;
import io.github.spafka.flowable.service.FlowBase;
import io.github.spafka.flowable.service.FlowNodeDto;
import io.github.spafka.flowable.service.Graphs;
import io.github.spafka.flowable.service.impl.returns.MainReturnService;
import io.vavr.Tuple3;
import lombok.var;
import org.flowable.bpmn.model.BaseElement;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.engine.*;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.Execution;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @link {{src/main/resources/returntest/复杂并行网关.bpmn20.xml}}
 */
@SpringBootTest

public class SimplePallergateJump02Tests extends FlowBase {

    private static final String key = "simplepg02";

    @Autowired
    DataSource dataSource;
    @Resource
    protected HistoryService historyService;
    @Autowired
    ProcessEngine processEngine;
    @Autowired
    RepositoryService repositoryService;
    @Autowired
    TaskService taskService;
    @Autowired
    RuntimeService runtimeService;
    @Autowired
    FlowService flowService;

    @Autowired
    public MainReturnService mainReturnService;


    String processName = "简单并行网关02";

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    public void deploy() {
        super.processName = this.processName;
        deploy(key, processName, String.format("returntest/%s.bpmn20.xml", processName));
    }


    @Test
    public void ok() {

        deploy();
        submit();

        complete("whf", "T2");
        complete("whf", "T4-2");
        complete("whf", "T4-1");
        complete("whf", "T3");
        List<FlowNodeDto> t5 = listCanRetuen("T5");
        complete("whf", "T5");
        show(super.processInstanceId);
    }


    @Test
    public void jumpT5ok() {

        deploy();

        submit();

        runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(processInstanceId)
                .moveActivityIdTo("T2", "T5")
                .changeState();

        complete("whf", "T5");
        show(processInstanceId);

    }

    @Test
    public void jumpT3() {


        deploy();

        submit();

        runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(processInstanceId)
                .moveActivityIdTo("T2", "T3")
                .changeState();
        mainReturnService.insertExecution("G2", processInstanceId, processDefinition.getId(), null);
        // mainReturnService.insertExecution("G3e", processInstanceId, processDefinition.getId(), null);

        complete("whf", "T3");
        complete("whf", "T5");
        show(processInstanceId);

    }

    @Test
    public void jumpT41() {


        deploy();

        submit();


        complete("whf", "T2");
        complete("whf", "T4-2");
        complete("whf", "T4-1");
        complete("whf", "T3");
        List<FlowNodeDto> t5 = listCanRetuen("T5");
        return2Node("T5","T3","T4-2","T4-1");
        List<Execution> execution = runtimeService.createExecutionQuery().processInstanceId(
                processInstanceId
        ).list();


        complete("whf", "T4-2");
        complete("whf", "T4-1");
        complete("whf", "T3");

        complete("whf","T5");
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(
                processInstanceId
        ).list();

        List<Task> list = taskService.createTaskQuery().processInstanceId(processInstanceId).list();


        show(processInstanceId);


    }

    @Test
    public void jumpTall() {


        deploy();
        submit();

        complete("whf", "T2");
        complete("whf", "T4-2");
        complete("whf", "T4-1");
        complete("whf", "T3");
        List<FlowNodeDto> t5 = listCanRetuen("T5");

        runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(processInstanceId)
                .moveSingleActivityIdToActivityIds("T5", Arrays.asList("T3", "T4-1", "T4-2"))
                .changeState();

        show(super.processInstanceId);

        System.out.println();

    }

}

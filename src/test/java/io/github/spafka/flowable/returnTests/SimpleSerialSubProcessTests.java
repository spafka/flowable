package io.github.spafka.flowable.returnTests;

import io.github.spafka.flowable.FlowBase;
import io.github.spafka.flowable.core.FlowService;
import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.constants.BpmnXMLConstants;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @link {{src/main/resources/returntest/复杂并行网关.bpmn20.xml}}
 */
@SpringBootTest

public class SimpleSerialSubProcessTests extends FlowBase {

    private static final String key = "simpleserial";

    @Autowired
    DataSource dataSource;
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

    String processName = "简单串行测试";

    static int i = 0;

    public void deploy() {
        Deployment deployment = repositoryService.createDeployment()
                .addClasspathResource("returntest/简单串行测试.bpmn20.xml")
                .name(processName)
                .key(key)
                .deploy(); // 执行部署操作
        System.out.println("deployment.getId() = " + deployment.getId());
        System.out.println("deployment.getName() = " + deployment.getName());

        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().deploymentId(deployment.getId()).singleResult();

        System.out.println("processDefinition = " + processDefinition);
    }

    public void submit() {
        ProcessDefinition processDefinition = repositoryService
                .createProcessDefinitionQuery()
                .processDefinitionName(processName)
                .latestVersion()
                .singleResult();


        Map<String, Object> variables = new HashMap<>();
        variables.put("days", 22);
        variables.put("initiator", "whf");
        variables.put("INITIATOR", "whf");

        variables.put("status", "approve");
        variables.put(BpmnXMLConstants.ATTRIBUTE_EVENT_START_INITIATOR, "whf");


        ProcessInstance processInstance = runtimeService
                .startProcessInstanceByKey(processDefinition.getKey(), variables);

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getProcessInstanceId()).singleResult();
        if (Objects.nonNull(task)) {
            String userIdStr = (String) variables.get(BpmnXMLConstants.ATTRIBUTE_EVENT_START_INITIATOR);
            if (StringUtils.equals(task.getAssignee(), userIdStr)) {
                taskService.complete(task.getId(), variables);
            }
        }

    }


    @Test
    @DisplayName("完整子流程")
    public void test_ok() {
        deploy();
        submit();
        complete("whf", "T2");
        complete("whf", "T2-2");
        complete("whf", "T3");
        complete("whf", "T7");
        complete("whf", "T8");
        complete("whf", "T9");
        complete("whf", "T10");
        complete("whf", "T11");
        complete("whf", "T12");
        complete("whf", "T0");
        complete("whf", "T4");
        assert listall().isEmpty();

    }

    @Test
    @DisplayName("子流程驳回回到主流程 T10-T3")
    public void test_backup2main() {
        deploy();
        submit();
        complete("whf", "T2");
        complete("whf", "T2-2");
        complete("whf", "T3");
        complete("whf", "T7");
        complete("whf", "T8");
        complete("whf", "T9");
        return2Node("T10", "T3");
        complete("whf", "T3");
        complete("whf", "T7");
        complete("whf", "T8");
        complete("whf", "T9");
        complete("whf", "T10");
        complete("whf", "T11");
        complete("whf", "T12");
        complete("whf", "T0");
        complete("whf", "T4");
        assert listall().isEmpty();

    }

    @Test
    @DisplayName("串行驳回穿过整个子流程 T0-T3")
    public void test_ok2() {
        deploy();
        submit();
        complete("whf", "T2");

        complete("whf", "T3");
        complete("whf", "T7");
        complete("whf", "T8");
        complete("whf", "T9");


        complete("whf", "T10");
        complete("whf", "T11");
        complete("whf", "T12");
        return2Node("T0", "T3");
        complete("whf", "T3");
        complete("whf", "T7");
        complete("whf", "T8");
        complete("whf", "T9");
        complete("whf", "T10");
        complete("whf", "T11");
        complete("whf", "T12");
        complete("whf", "T0");
        complete("whf", "T2-2");
        complete("whf", "T4");
        assert listall().isEmpty();

    }

    @Test
    @DisplayName("串行驳回 整个流程和子流程 T4-T1")
    public void test_ok3() {
        deploy();
        submit();
        complete("whf", "T2");
        complete("whf", "T2-2");
        complete("whf", "T3");
        complete("whf", "T7");
        complete("whf", "T8");
        complete("whf", "T9");

        complete("whf", "T10");
        complete("whf", "T11");
        complete("whf", "T12");
        complete("whf", "T0");

        return2Node("T4", "T1");
        complete("whf", "T1");
        complete("whf", "T2");
        complete("whf", "T2-2");
        complete("whf", "T3");
        complete("whf", "T7");
        complete("whf", "T8");
        complete("whf", "T9");

        complete("whf", "T10");
        complete("whf", "T11");
        complete("whf", "T12");
        complete("whf", "T0");
        complete("whf", "T4");

        assert listall().isEmpty();

    }

    @Test
    @DisplayName("串行驳回至子流程T0-T11")
    public void test_ok4() {
        deploy();
        submit();
        complete("whf", "T2");
        complete("whf", "T2-2");
        complete("whf", "T3");
        complete("whf", "T7");
        complete("whf", "T8");
        complete("whf", "T9");

        complete("whf", "T10");
        complete("whf", "T11");
        complete("whf", "T12");
        return2Node("T0", "T11");
        complete("whf", "T11");
        complete("whf", "T12");
        complete("whf", "T0");
        complete("whf", "T4");

        assert listall().isEmpty();
    }

    @Test
    @DisplayName("并行驳回 jump fater T3-T1")
    public void test_ok5() {
        deploy();
        submit();
        complete("whf", "T2");
        complete("whf", "T2-2");
        return2Node("T3", "T1");
        complete("whf", "T1");
        complete("whf", "T2");
        complete("whf", "T2-2");
        complete("whf", "T3");
        complete("whf", "T7");
        complete("whf", "T8");
        complete("whf", "T9");


        complete("whf", "T10");
        complete("whf", "T11");
        complete("whf", "T12");


        complete("whf", "T0");
        complete("whf", "T4");

        assert listall().isEmpty();
    }

    @Test
    @DisplayName("并行驳回 jump fater T7-T1")
    public void test_ok6() {
        deploy();
        submit();
        complete("whf", "T2");
        complete("whf", "T2-2");
        complete("whf", "T3");
        return2Node("T7", "T1");


        complete("whf", "T1");
        complete("whf", "T2");
        complete("whf", "T2-2");
        complete("whf", "T3");
        complete("whf", "T7");
        complete("whf", "T8");
        complete("whf", "T9");
        complete("whf", "T10");
        complete("whf", "T11");
        complete("whf", "T12");
        complete("whf", "T0");
        complete("whf", "T4");

        assert listall().isEmpty();
    }

    @Test
    @DisplayName("并行驳回 jump fater T11-T1")
    public void test_ok7() {
        deploy();
        submit();
        complete("whf", "T2");
        complete("whf", "T3");

        complete("whf", "T2-2");
        complete("whf", "T7");
        complete("whf", "T8");
        complete("whf", "T9");
        complete("whf", "T10");

        return2Node("T11", "T1");

        complete("whf", "T1");
        complete("whf", "T2");
        complete("whf", "T3");
        complete("whf", "T7");
        complete("whf", "T8");
        complete("whf", "T9");
        complete("whf", "T10");

        complete("whf", "T11");
        complete("whf", "T12");
        complete("whf", "T0");
        complete("whf", "T2-2");

        complete("whf", "T4");

        assert listall().isEmpty();
    }


    @Test
    @DisplayName("并行驳回 jump fater T0-T1")
    public void test_ok8() {
        deploy();
        submit();
        complete("whf", "T2");
        complete("whf", "T3");

        complete("whf", "T7");
        complete("whf", "T8");
        complete("whf", "T9");
        complete("whf", "T10");
        complete("whf", "T11");
        complete("whf", "T12");

        return2Node("T0", "T1");


        complete("whf", "T1");
        complete("whf", "T2");
        complete("whf", "T2-2");
        complete("whf", "T3");
        complete("whf", "T7");
        complete("whf", "T8");
        complete("whf", "T9");
        complete("whf", "T10");

        complete("whf", "T11");
        complete("whf", "T12");
        complete("whf", "T0");


        complete("whf", "T4");

        assert listall().isEmpty();
    }

    @Test
    @DisplayName("并行驳回 jump fater T4-T0")
    public void test_ok9() {
        deploy();
        submit();
        complete("whf", "T2");
        complete("whf", "T2-2");
        complete("whf", "T3");

        complete("whf", "T7");
        complete("whf", "T8");
        complete("whf", "T9");
        complete("whf", "T10");
        complete("whf", "T11");
        complete("whf", "T12");
        complete("whf", "T0");

        return2Node("T4", "T0");
        complete("whf", "T0");

        complete("whf", "T4");

        assert listall().isEmpty();
    }

    @Test
    @DisplayName("并行驳回 jump fater T4-T11")
    public void test_ok10() {
        deploy();
        submit();
        complete("whf", "T2");
        complete("whf", "T2-2");
        complete("whf", "T3");

        complete("whf", "T7");
        complete("whf", "T8");
        complete("whf", "T9");
        complete("whf", "T10");
        complete("whf", "T11");
        complete("whf", "T12");
        complete("whf", "T0");

        return2Node("T4", "T11");
        complete("whf", "T11");
        complete("whf", "T12");
        complete("whf", "T0");

        complete("whf", "T4");

        assert listall().isEmpty();
    }

    @Test
    @DisplayName("并行驳回 jump fater T4-T7")
    public void test_ok11() {
        deploy();
        submit();
        complete("whf", "T2");
        complete("whf", "T2-2");
        complete("whf", "T3");

        complete("whf", "T7");
        complete("whf", "T8");
        complete("whf", "T9");
        complete("whf", "T10");
        complete("whf", "T11");
        complete("whf", "T12");
        complete("whf", "T0");

        return2Node("T4", "T7");
        complete("whf", "T7");
        complete("whf", "T8");

        complete("whf", "T11");
        complete("whf", "T12");
        complete("whf", "T0");

        complete("whf", "T4");

        assert listall().isEmpty();
    }

    @Test
    @DisplayName("并行驳回 jump fater T4-T3")
    public void test_ok12() {
        deploy();
        submit();
        complete("whf", "T2");
        complete("whf", "T2-2");
        complete("whf", "T3");

        complete("whf", "T7");
        complete("whf", "T8");
        complete("whf", "T9");
        complete("whf", "T10");
        complete("whf", "T11");
        complete("whf", "T12");
        complete("whf", "T0");

        return2Node("T4", "T3");
        complete("whf", "T3");

        complete("whf", "T7");
        complete("whf", "T8");
        complete("whf", "T9");
        complete("whf", "T10");
        complete("whf", "T11");
        complete("whf", "T12");
        complete("whf", "T0");

        complete("whf", "T4");

        assert listall().isEmpty();
    }


}

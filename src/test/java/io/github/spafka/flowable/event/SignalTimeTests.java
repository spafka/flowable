package io.github.spafka.flowable.event;

import io.github.spafka.flowable.service.FlowBase;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.flowable.bpmn.constants.BpmnXMLConstants;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.LockSupport;

@SpringBootTest
public class SignalTimeTests extends FlowBase {

    private String key = "K1353567853857009";
    private String processName = "回归测试";

    public void deploy() {
        Deployment deployment = repositoryService.createDeployment()
                .addClasspathResource("回归测试.bpmn20.xml")
                .name(processName)
                .key(key)
                .deploy(); // 执行部署操作
        System.out.println("deployment.getId() = " + deployment.getId());
        System.out.println("deployment.getName() = " + deployment.getName());

        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().deploymentId(deployment.getId()).singleResult();

        System.out.println("processDefinition = " + processDefinition);
    }


    @Test
    public void submit() {


        ProcessDefinition processDefinition = repositoryService
                .createProcessDefinitionQuery()
                .processDefinitionName(processName)
                .latestVersion()
                .singleResult();


        Map<String, Object> variables = new HashMap<>();
        variables.put("date", FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss").format(System.currentTimeMillis() + 10 * 1000L));
        variables.put("initiator", "whf");
        variables.put("INITIATOR", "whf");
        variables.put("status", "approve");


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
    public void test() {
        deploy();
        submit();

        while (true) {
            List<Task> listall = listall();

            listall.forEach(x -> {

                System.out.printf("%s %s %s %s\n", x.getId(), x.getAssignee(), x.getTaskDefinitionKey(), x.getTaskDefinitionKey());
            });

            if (listall.size() > 1) {
                break;
            }
            LockSupport.parkNanos(10 * 1000_000_000L);
        }

        listCanRetuen("T4");
        return2Node("T4","T1");

        complete("whf","T1");
        complete("whf","T2");
        complete("whf","T3");

        System.out.println();
    }
}

package io.github.spafka.flowable.jump;

import io.github.spafka.flowable.config.FlowContext;
import io.github.spafka.flowable.core.FlowService;
import io.github.spafka.flowable.service.FlowBase;
import io.github.spafka.flowable.service.FlowNodeDto;
import io.github.spafka.flowable.service.impl.returns.MainReturnService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.constants.BpmnXMLConstants;
import org.flowable.engine.*;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @link {{src/main/resources/returntest/复杂并行网关.bpmn20.xml}}
 */
@SpringBootTest
@Slf4j
public class SimplePallergateJumpTests extends FlowBase {

    private static final String key = "simplepg";

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


    String processName = "简单并行网关";

    @Test
    public void deploy() {
        super.processName = this.processName;
        deploy(key, processName, "returntest/简单并行网关.bpmn20.xml");
    }

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    public void ok() {

        Runnable runnable = () -> TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {

                FlowContext flowContext = FlowContext.get();

                log.info("add {}, delete {}", flowContext.getPendingTasks(), flowContext.getCompleTasks());
                FlowContext.clear();
            }
        });
        transactionTemplate.execute(
                new TransactionCallbackWithoutResult() {
                    @Override
                    protected void doInTransactionWithoutResult(TransactionStatus status) {
                        deploy();
                        runnable.run();
                    }
                }
        );

        transactionTemplate.execute((
                new TransactionCallbackWithoutResult() {

                    @Override
                    protected void doInTransactionWithoutResult(TransactionStatus status) {
                        submit();
                        runnable.run();
                    }
                }

        ));

        transactionTemplate.execute((
                new TransactionCallbackWithoutResult() {

                    @Override
                    protected void doInTransactionWithoutResult(TransactionStatus status) {
                        complete("whf", "T1");
                        runnable.run();
                    }
                }

        ));

        transactionTemplate.execute((
                new TransactionCallbackWithoutResult() {

                    @Override
                    protected void doInTransactionWithoutResult(TransactionStatus status) {
                        complete("whf", "T2");
                        runnable.run();
                    }
                }

        ));

        transactionTemplate.execute((
                new TransactionCallbackWithoutResult() {

                    @Override
                    protected void doInTransactionWithoutResult(TransactionStatus status) {
                        complete("whf", "T3");
                        runnable.run();
                    }
                }

        ));
        transactionTemplate.execute((
                new TransactionCallbackWithoutResult() {

                    @Override
                    protected void doInTransactionWithoutResult(TransactionStatus status) {
                        complete("whf", "T4");
                        runnable.run();
                    }
                }

        ));

        transactionTemplate.execute((
                new TransactionCallbackWithoutResult() {

                    @Override
                    protected void doInTransactionWithoutResult(TransactionStatus status) {
                        complete("whf", "T5");
                        runnable.run();
                    }
                }

        ));


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

        complete("whf", "T3");
        complete("whf", "T5");
        show(processInstanceId);

    }

    @Test
    public void jumpEnd() {

        deploy();
        submit();

        runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(processInstanceId)
                .moveActivityIdTo("T2", "T3")
                .changeState();
        mainReturnService.insertExecution("G2", processInstanceId, processDefinition.getId(), null);

        complete("whf", "T3");
        complete("whf", "T5");
        show(processInstanceId);

    }





}

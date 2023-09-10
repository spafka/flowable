package io.github.spafka.flowable;

import io.github.spafka.flowable.listerener.listener.TaskCompleteListener;
import io.github.spafka.flowable.listerener.listener.TaskCreateListener;
import io.github.spafka.flowable.listerener.listener.TaskDeleteListener;
import io.github.spafka.flowable.listerener.listener.process.ProcessCompleteListener;
import io.github.spafka.flowable.listerener.listener.process.ProcessStartListener;
import lombok.extern.slf4j.Slf4j;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEventDispatcher;
import org.flowable.common.engine.api.delegate.event.FlowableEventListener;
import org.flowable.engine.delegate.event.impl.FlowableEntityEventImpl;
import org.flowable.engine.delegate.event.impl.FlowableEntityWithVariablesEventImpl;
import org.flowable.engine.impl.db.DbIdGenerator;
import org.flowable.spring.SpringExpressionManager;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.spring.boot.EngineConfigurationConfigurer;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.ContextRefreshedEvent;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j()
@Configuration
public class FlowableConfig implements EngineConfigurationConfigurer<SpringProcessEngineConfiguration>, ApplicationListener<ContextRefreshedEvent> {


    @Autowired
    @Lazy
    ApplicationContext applicationContext;
    @Autowired
    @Lazy
    TaskCreateListener taskCreateListener;
    @Autowired
    @Lazy
    TaskCompleteListener taskCompleteListener;
    @Autowired
    @Lazy
    TaskDeleteListener taskDeleteListener;
    @Autowired
    @Lazy
    ProcessStartListener processStartListener;
    @Autowired
    @Lazy
    ProcessCompleteListener processCompleteListener;

    @Autowired
    SpringProcessEngineConfiguration engineConfiguration;

    @Override
    public void configure(SpringProcessEngineConfiguration engineConfiguration) {
        engineConfiguration.setActivityFontName("宋体");
        engineConfiguration.setLabelFontName("宋体");
        engineConfiguration.setAnnotationFontName("宋体");
        engineConfiguration.setIdGenerator(new DbIdGenerator());
        engineConfiguration.setCreateDiagramOnDeploy(true);
        engineConfiguration.setDisableIdmEngine(true);
        engineConfiguration.setExpressionManager(new SpringExpressionManager(applicationContext, engineConfiguration.getBeans()));

        // 设置流程监听器
        Map<String, List<FlowableEventListener>> listenerMap = new HashMap<>(8);
        listenerMap.put(FlowableEngineEventType.PROCESS_COMPLETED.name(), Collections.singletonList(processCompleteListener));
        listenerMap.put(FlowableEngineEventType.PROCESS_STARTED.name(), Collections.singletonList(processStartListener));


    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {

        FlowableEventDispatcher dispatcher = engineConfiguration.getEventDispatcher();

        FlowableEventListener TASK_COMPLETED = new FlowableEventListener() {
            @Override
            public void onEvent(FlowableEvent flowableEvent) {
                if (flowableEvent instanceof FlowableEntityEventImpl) {

                    FlowableEntityEventImpl flowableEvent1 = (FlowableEntityEventImpl) flowableEvent;
                    TaskEntity taskEntity = (TaskEntity) flowableEvent1.getEntity();
                    log.info("任务完成 task: {} Assignee()={},executionId={}", taskEntity.getName(), taskEntity.getAssignee(), taskEntity.getExecutionId());

                } else {
                    FlowableEntityWithVariablesEventImpl taskComplete = (FlowableEntityWithVariablesEventImpl) flowableEvent;
                    TaskEntity taskEntity = (TaskEntity) taskComplete.getEntity();
                    log.info("任务完成 task: {} Assignee()={},executionId={}", taskEntity.getName(), taskEntity.getAssignee(), taskComplete.getExecutionId());
                }
            }

            @Override
            public boolean isFailOnException() {
                return false;
            }

            @Override
            public boolean isFireOnTransactionLifecycleEvent() {
                return false;
            }

            @Override
            public String getOnTransaction() {
                return null;
            }
        };
        FlowableEventListener TASK_CREATED = new FlowableEventListener() {
            @Override
            public void onEvent(FlowableEvent flowableEvent) {

                org.flowable.common.engine.impl.event.FlowableEntityEventImpl taskCreate = (org.flowable.common.engine.impl.event.FlowableEntityEventImpl) flowableEvent;
                TaskEntity taskEntity = (TaskEntity) taskCreate.getEntity();
                log.info("任务创建 task: {} Assignee()={},executionId={}", taskEntity.getName(), taskEntity.getAssignee(), taskCreate.getExecutionId());
            }

            @Override
            public boolean isFailOnException() {
                return false;
            }

            @Override
            public boolean isFireOnTransactionLifecycleEvent() {
                return false;
            }

            @Override
            public String getOnTransaction() {
                return null;
            }
        };


        FlowableEventListener PROCESS_COMPLETE = new FlowableEventListener() {
            @Override
            public void onEvent(FlowableEvent flowableEvent) {

                String processInstanceId = ((FlowableEntityEventImpl) flowableEvent).getProcessInstanceId();
                System.err.println(processInstanceId + "任务完成");

                log.info("{} 任务完成", processInstanceId);
            }

            @Override
            public boolean isFailOnException() {
                return false;
            }

            @Override
            public boolean isFireOnTransactionLifecycleEvent() {
                return false;
            }

            @Override
            public String getOnTransaction() {
                return null;
            }
        };

        dispatcher.addEventListener(TASK_COMPLETED, FlowableEngineEventType.TASK_COMPLETED);
        dispatcher.addEventListener(TASK_CREATED, FlowableEngineEventType.TASK_CREATED);

        dispatcher.addEventListener(PROCESS_COMPLETE, FlowableEngineEventType.PROCESS_COMPLETED);

    }
}
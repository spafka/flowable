package io.github.spafka.flowable.config;

import lombok.Data;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;

import java.util.ArrayList;
import java.util.List;

@Data
public class FlowContext {

    private static final ThreadLocal<FlowContext> flow = ThreadLocal.withInitial(() -> new FlowContext());
    List<TaskEntity> pendingTasks = new ArrayList<>();
    List<TaskEntity> compleTasks = new ArrayList<>();
    public static void clear() {
        flow.remove();
    }

    public static FlowContext get() {
       return flow.get();
    }

}

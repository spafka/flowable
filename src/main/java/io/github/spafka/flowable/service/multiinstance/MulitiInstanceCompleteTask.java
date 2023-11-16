package io.github.spafka.flowable.service.multiinstance;

import io.github.spafka.flowable.service.ReturnService;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.task.api.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.Serializable;

@Component("mulitiInstanceCompleteTask")
public class MulitiInstanceCompleteTask implements Serializable {

    @Autowired
    ReturnService returnService;

    public static ThreadLocal<Task> taskThreadLocal = new ThreadLocal<>();

    /**
     * 完成任务是需要触发的方法
     *
     * @param execution
     * @return false 表示会签任务还没有结束
     * <p>
     * true 表示会签任务结束了
     */
    public boolean completeTask(DelegateExecution execution) {
        System.out.println("总的会签任务数量：" + execution.getVariable("nrOfInstances")
                + "当前获取的会签任务数量：" + execution.getVariable("nrOfActiveInstances")
                + " - " + "已经完成的会签任务数量：" + execution.getVariable("nrOfCompletedInstances"));

        execution.setVariable("nrOfInstances",4);

        return false;
    }
}
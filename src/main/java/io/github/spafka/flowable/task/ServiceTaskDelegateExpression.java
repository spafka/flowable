package io.github.spafka.flowable.task;


import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;


/**
 * 服务任务代理类
 */
@Component
public class ServiceTaskDelegateExpression implements JavaDelegate {
    public static AtomicInteger INSTANCE_COUNT = new AtomicInteger(0);


    public void execute(DelegateExecution execution) {

        String id = execution.getId();

        System.out.println(execution);

        System.err.println("=====================PrototypeDelegateExpressionBean,INSTANCE_COUNT: " + INSTANCE_COUNT.incrementAndGet());

    }

}

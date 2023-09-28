package io.github.spafka.flowable.task;


import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;


@Component
public class ServiceTaskDelegateExpression implements JavaDelegate {
    public static AtomicInteger INSTANCE_COUNT = new AtomicInteger(0);


    public void execute(DelegateExecution execution) {

        System.err.printf("curl www.google.com");

        System.err.println("=====================PrototypeDelegateExpressionBean,INSTANCE_COUNT: " + INSTANCE_COUNT);

    }

}

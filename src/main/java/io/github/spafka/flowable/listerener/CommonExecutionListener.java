package io.github.spafka.flowable.listerener;


import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.springframework.stereotype.Component;

@Component("commonExecutionListener")
public class CommonExecutionListener implements ExecutionListener {

    @Override
    public void notify(DelegateExecution delegateExecution) {
        System.out.printf("[execution] %s  %s : %s\n ", delegateExecution.getEventName(), delegateExecution.getCurrentActivityId(), delegateExecution.getId());
        System.out.println();

    }
}

package io.github.spafka.flowable.listerener.listener.process;

import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEventListener;
import org.flowable.engine.delegate.event.*;
import org.springframework.stereotype.Component;

@Component
public class CustomTaskEventListener implements FlowableEventListener {

    @Override
    public void onEvent(FlowableEvent event) {
        // 处理任务事件的逻辑，可以获取任务的详细信息
        if ( event instanceof FlowableActivityEvent || event instanceof FlowableSequenceFlowTakenEvent) {
            System.err.println(event);
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
}
/**
 * Licensed to the Deep Blue SUPCON
 */
package io.github.spafka.flowable.listerener;

import lombok.extern.slf4j.Slf4j;
import org.flowable.common.engine.api.delegate.event.AbstractFlowableEventListener;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.engine.delegate.event.FlowableCancelledEvent;
import org.flowable.engine.delegate.event.impl.FlowableProcessTerminatedEventImpl;


@Slf4j
public abstract class AbstractProcessTerminateListener extends AbstractFlowableEventListener {

    @Override
    public void onEvent(FlowableEvent event) {
        // 取消事件有2种类型 分别是cancel和terminal
        if (event instanceof FlowableCancelledEvent) {
            FlowableCancelledEvent cancelledEvent = (FlowableCancelledEvent) event;
            updateProcess(cancelledEvent.getProcessInstanceId());
        } else if (event instanceof FlowableProcessTerminatedEventImpl) {
            FlowableProcessTerminatedEventImpl terminalEvent = (FlowableProcessTerminatedEventImpl) event;
            updateProcess(terminalEvent.getProcessInstanceId());
        } else {
            log.error("终止流程事件未捕获, 当前事件类型为: {}", event.getClass());
        }
    }

    /**
     * 删除流程
     *
     * @param processId 流程实例ID
     */
    public abstract void updateProcess(String processId);

    /**
     * @see org.flowable.common.engine.api.delegate.event.FlowableEventListener#isFailOnException()
     */
    @Override
    public boolean isFailOnException() {
        return false;
    }
}

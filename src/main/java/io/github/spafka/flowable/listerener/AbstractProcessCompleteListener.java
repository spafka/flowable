/**
 * Licensed to the Deep Blue SUPCON
 */
package io.github.spafka.flowable.listerener;

import org.flowable.common.engine.api.delegate.event.AbstractFlowableEventListener;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.engine.delegate.event.impl.FlowableEntityEventImpl;

/**
 * @author: zhuangmh
 * @date: 2020年6月16日 上午9:36:33
 */
public abstract class AbstractProcessCompleteListener extends AbstractFlowableEventListener {

    /**
     * @see org.flowable.common.engine.api.delegate.event.FlowableEventListener#onEvent(FlowableEvent)
     */
    @Override
    public void onEvent(FlowableEvent event) {
        FlowableEntityEventImpl completeEvent = (FlowableEntityEventImpl) event;
        updateProcess(completeEvent.getProcessInstanceId());
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

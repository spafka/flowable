/**
 * Licensed to the Deep Blue SUPCON
 */
package io.github.spafka.flowable.listerener.listener;

import io.github.spafka.flowable.listerener.AbstractTaskListener;
import lombok.extern.slf4j.Slf4j;
import org.flowable.task.service.delegate.DelegateTask;
import org.springframework.stereotype.Component;


@Component("taskCompleteListener")
@Slf4j
public class TaskCompleteListener extends AbstractTaskListener {

    private static final long serialVersionUID = -8786438230218847764L;

    @Override
    public void execute(DelegateTask task) {
        //判断是否是最后一个处理节点
        log.info("[task] complete {}", task);

    }


}

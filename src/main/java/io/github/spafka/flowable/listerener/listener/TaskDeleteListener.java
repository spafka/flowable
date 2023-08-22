/**
 * Licensed to the Deep Blue SUPCON
 */
package io.github.spafka.flowable.listerener.listener;


import io.github.spafka.flowable.listerener.AbstractTaskListener;
import lombok.extern.slf4j.Slf4j;
import org.flowable.task.service.delegate.DelegateTask;
import org.springframework.stereotype.Component;

@Slf4j
@Component("taskDeleteListener")
public class TaskDeleteListener extends AbstractTaskListener {


    @Override
    public void execute(DelegateTask task) {

        log.info("[task delete]{}", task);

    }


}

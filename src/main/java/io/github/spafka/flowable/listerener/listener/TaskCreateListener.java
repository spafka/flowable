/**
 * Licensed to the Deep Blue SUPCON
 */
package io.github.spafka.flowable.listerener.listener;


import io.github.spafka.flowable.listerener.AbstractTaskListener;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.UserTask;
import org.flowable.task.service.delegate.DelegateTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component("taskCreateListener")
@Slf4j
public class TaskCreateListener extends AbstractTaskListener {


    @Override
    public void execute(DelegateTask task) {
        log.info("[task] create{}", task);

    }


}

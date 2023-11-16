/**
 * Licensed to the Deep Blue SUPCON
 */
package io.github.spafka.flowable.listerener.listener.process;

import io.github.spafka.flowable.listerener.AbstractProcessStartListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ProcessStartListener extends AbstractProcessStartListener {


    @Override
    public void createProcess(String processId, String processKey, String processName, String processVersion) {

        log.info("流程创建 {} {} {} {}", processKey, processName, processVersion, processId);
    }

}

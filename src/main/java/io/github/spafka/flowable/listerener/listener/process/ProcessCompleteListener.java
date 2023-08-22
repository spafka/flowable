/**
 * Licensed to the Deep Blue SUPCON
 */
package io.github.spafka.flowable.listerener.listener.process;


import io.github.spafka.flowable.listerener.AbstractProcessCompleteListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author: zhuangmh
 * @date: 2020年11月10日 下午7:44:03
 */
@Component
@Slf4j
public class ProcessCompleteListener extends AbstractProcessCompleteListener {


    @Override
    public void updateProcess(String processId) {
        log.info("[deploy] {}结束",processId);

    }

}

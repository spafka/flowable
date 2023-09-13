/**
 * Licensed to the Deep Blue SUPCON
 */
package io.github.spafka.flowable.listerener;


import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author: zhuangmh
 * @date: 2021年1月19日 下午7:35:12
 */
@Component("timerEndEventDelagate")
@Slf4j
public class TimerEndEventDelegate implements JavaDelegate {


    /**
     * @see JavaDelegate#execute(DelegateExecution)
     */
    @Override
    public void execute(DelegateExecution execution) {
        // 由于定时器是异步执行, 下游环节将无法获取租户ID
        // 因此需要在定时任务结束前将租户ID设置到当前线程上下文
        log.info("定时器结束事件触发, 设置下游上下文租户: {}", execution.getTenantId());

    }
}

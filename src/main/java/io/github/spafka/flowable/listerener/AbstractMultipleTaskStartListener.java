package io.github.spafka.flowable.listerener;


import org.flowable.common.engine.api.delegate.event.AbstractFlowableEventListener;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.delegate.event.FlowableMultiInstanceActivityEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

import java.util.Set;

/**
 * @author: zhuangmh
 * @date: 2020年6月2日 下午3:17:23
 */
public abstract class AbstractMultipleTaskStartListener extends AbstractFlowableEventListener {

    private static final String MULTIPLE_TASK_ASSIGNEE_VARIABLE = "assigneeList";
    @Autowired
    @Lazy
    private RuntimeService runtimeService;

    /**
     * 获取会签成员列表
     *
     * @param taskDefinitionKey   节点ID
     * @param processDefinitionId 流程定义ID
     * @return
     */
    public abstract Set<String> getMultipleTaskAssigneeValue(String taskDefinitionKey, String processDefinitionId, String processId);

    @Override
    public void onEvent(FlowableEvent arg0) {
        FlowableMultiInstanceActivityEvent event = (FlowableMultiInstanceActivityEvent) arg0;


        Set<String> multipleTaskAssignees = getMultipleTaskAssigneeValue(event.getActivityId(), event.getProcessDefinitionId(), event.getProcessInstanceId());
        // 创建一个执行者为空字符串的待办, 这么做能触发空执行者逻辑判断
        if (multipleTaskAssignees.isEmpty()) {
            multipleTaskAssignees.add("");
        }
        runtimeService.setVariable(event.getExecutionId(), MULTIPLE_TASK_ASSIGNEE_VARIABLE, multipleTaskAssignees);
    }

    /**
     * @see org.flowable.common.engine.api.delegate.event.FlowableEventListener#isFailOnException()
     */
    @Override
    public boolean isFailOnException() {
        return false;
    }

}

package io.github.spafka.flowable.service;


import io.github.spafka.tuple.Tuple2;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.task.api.Task;

import java.util.List;

/**
 * 驳回功能
 * @author by wanghaifeng
 * @Date 2023/8/29 17:57
 */
public interface ReturnService {

    /**
     * 返回任务可返回的节点
     *
     * @param bpmnModel
     * @param instanceId
     * @param processInstanceId
     * @return
     */
    Tuple2<List<FlowNodeDto>, Boolean> getCanRejectedFlowNode(BpmnModel bpmnModel, String instanceId, String processInstanceId);

    /**
     * 任务跳转至target
     *
     * @param taskId
     * @param targetId userTask id
     * @return
     */
    boolean returnToTarget(Task task, String... targetId);
}

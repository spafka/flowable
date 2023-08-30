/**
 * Licensed to the Deep Blue SUPCON
 */
package io.github.spafka.flowable.service;


import org.flowable.bpmn.model.BpmnModel;

import java.util.LinkedList;

public interface BpmnService {
    

    BpmnModel getBpmnModelByFlowableTaskId(String taskId);

    LinkedList<FlowNodeDto> getCanReturnNodes(BpmnModel processId, String instanceId, String activityName);

}

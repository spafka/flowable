package io.github.spafka.flowable.service;

import org.flowable.bpmn.model.BpmnModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Primary
public class MainReturnService implements ReturnService {

    @Qualifier("gutzReturnService")
    @Autowired
    ReturnService gutzReturnService;

    @Qualifier("githubReturnService")
    @Autowired
    ReturnService githubReturnService;

    @Override
    public List<FlowNodeDto> getCanRejectedFlowNode(BpmnModel bpmnModel, String instanceId, String processInstanceId) {
        return gutzReturnService.getCanRejectedFlowNode(bpmnModel, instanceId, processInstanceId);
    }

    @Override
    public boolean returnToTarget(String taskId, String targetId) {



        return gutzReturnService.returnToTarget(taskId, targetId);
    }
}

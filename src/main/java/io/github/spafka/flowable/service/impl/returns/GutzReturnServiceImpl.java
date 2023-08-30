package io.github.spafka.flowable.service.impl.returns;


import io.github.spafka.flowable.service.FlowNodeDto;
import io.github.spafka.flowable.service.ReturnService;
import io.github.spafka.flowable.service.impl.returns.gutz.BpmUserTaskService;
import io.github.spafka.flowable.service.impl.returns.gutz.entity.BpmTaskModelEntity;
import io.github.spafka.flowable.service.impl.returns.gutz.form.BpmJumpForm;
import io.github.spafka.flowable.service.impl.returns.gutz.query.BpmBackTaskModelQuery;
import org.flowable.bpmn.model.BpmnModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author by wanghaifeng
 * @link {{<a href="https://blog.51cto.com/guzt/3995012">...</a>}}
 * @since 2023/8/30 15:37
 */
@Service(value = "gutzReturnService")
public class GutzReturnServiceImpl implements ReturnService {

    @Autowired
    BpmUserTaskService bpmUserTaskService;

    @Override
    public List<FlowNodeDto> getCanRejectedFlowNode(BpmnModel bpmnModel, String taskId, String processInstanceId) {
        BpmBackTaskModelQuery query = new BpmBackTaskModelQuery();
        query.setTaskId(taskId);
        List<BpmTaskModelEntity> bpmTaskModelEntities = bpmUserTaskService.listAllBackTaskModel(query, bpmnModel);
        return bpmTaskModelEntities.stream().map(x -> new FlowNodeDto(x.getTaskDefKey(), x.getTaskName())).collect(Collectors.toList());
    }

    @Override
    public boolean returnToTarget(String taskId, String toId) {
        BpmJumpForm bpmJumpForm = new BpmJumpForm();
        bpmJumpForm.setTaskId(taskId);
        bpmJumpForm.setTargetTaskDefineKes(Collections.singletonList(toId));
        bpmUserTaskService.jump(bpmJumpForm);
        return true;
    }
}

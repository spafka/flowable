/**
 * Licensed to the Deep Blue SUPCON
 */
package io.github.spafka.flowable.service.impl;


import io.github.spafka.flowable.service.BpmnService;
import io.github.spafka.flowable.service.FlowNodeDto;
import io.github.spafka.flowable.service.ReturnService;
import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.TaskService;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.task.api.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.LinkedList;

/**
 * @author: zhuangmh
 * @date: 2020年5月27日 上午11:15:30
 */
@Service
@Slf4j
public class BpmnServiceImpl implements BpmnService {


    @Autowired
    TaskService taskService;
    @Autowired
    RepositoryService repositoryService;

    @Autowired
    @Lazy
    ReturnService returnService;

    @Override
    public BpmnModel getBpmnModelByFlowableTaskId(String taskId) {
        // 当前任务 task
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        // 获取流程定义信息
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionId(task.getProcessDefinitionId()).singleResult();
        // 获取所有节点信息，暂不考虑子流程情况
        return repositoryService.getBpmnModel(processDefinition.getId());

    }

    @Override
    public LinkedList<FlowNodeDto> getCanReturnNodes(BpmnModel bpmnModel, String instanceId, String activityName) {

        return new LinkedList<>(returnService.getCanRejectedFlowNode(bpmnModel, instanceId, activityName));

    }


}

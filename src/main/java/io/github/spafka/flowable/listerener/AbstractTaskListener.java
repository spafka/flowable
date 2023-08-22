/**
 * Licensed to the Deep Blue SUPCON
 */
package io.github.spafka.flowable.listerener;


import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.UserTask;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.task.service.delegate.DelegateTask;
import org.flowable.task.service.delegate.TaskListener;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashSet;
import java.util.Set;


public abstract class AbstractTaskListener implements TaskListener {

    private static final long serialVersionUID = -4894295268301032189L;
    @Autowired
    protected transient RuntimeService runtimeService;
    @Autowired
    protected transient TaskService taskService;
    @Autowired
    protected transient RepositoryService repositoryService;


    //具体的执行逻辑
    public abstract void execute(DelegateTask task);

    @Override
    public void notify(DelegateTask task) {


        switch (task.getEventName()) {
            case EVENTNAME_CREATE:
            case EVENTNAME_COMPLETE:
            case EVENTNAME_DELETE:
                execute(task);
                break;
            default:
                break;
        }

    }


    // 针对指派,撤回等情况
    protected Set<String> getSpecialUser(DelegateTask delegateTask, UserTask userTask) {

        return new HashSet<>();
    }

    protected UserTask getUserTaskNode(String processDefinitionId, String taskDefinitionKey) {
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);
        FlowElement flowElement = bpmnModel.getFlowElement(taskDefinitionKey);
        return (UserTask) flowElement;
    }


}

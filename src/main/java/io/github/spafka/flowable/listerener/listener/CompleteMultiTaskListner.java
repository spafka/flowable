package io.github.spafka.flowable.listerener.listener;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowNode;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.flowable.engine.delegate.TaskListener;
import org.flowable.engine.impl.bpmn.behavior.MultiInstanceActivityBehavior;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ExecutionQuery;
import org.flowable.task.service.delegate.DelegateTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
public class CompleteMultiTaskListner implements TaskListener {

    private static RuntimeService runtimeService;
    private static RepositoryService repositoryService;
    @Autowired
    public void setRuntimeService(RuntimeService runtimeService){
        this.runtimeService=runtimeService;
    }
    @Autowired
    public void setRepositoryService(RepositoryService repositoryService){
        this.repositoryService=repositoryService;
    }


    @Override
    public void notify(DelegateTask delegateTask) {

        //获取当前的执行实例
        ExecutionQuery executionQuery =runtimeService.createExecutionQuery();
        ExecutionEntity executionEntity = (ExecutionEntity)executionQuery.executionId(delegateTask.getExecutionId()).singleResult();
        String activityId=executionEntity.getActivityId();
        //获取当前活动节点信息
        FlowNode flowNode=getFlowNode(delegateTask.getProcessDefinitionId(),activityId);

        //获取当前审批人的审批意向
        String circulationConditions=(String) delegateTask.getVariable("circulationConditions");
        //处理并行网关的多实例
        if ("N".equals(circulationConditions) && flowNode.getBehavior() instanceof MultiInstanceActivityBehavior){
        //            ExecutionEntity executionEntity = (ExecutionEntity)runtimeService.createExecutionQuery().executionId(delegateTask.getExecutionId()).singleResult();
            String parentId=executionEntity.getParentId();
//此处获得的Execution是包括所有Execution和他们的父Execution，减签的时候要先删除子的才能删除父的
            List<Execution> executions =runtimeService.createExecutionQuery().processInstanceId(delegateTask.getProcessInstanceId()).onlyChildExecutions().list();
            System.out.println(parentId);
            for (Execution execution:executions){
                if (!execution.getId().equals(parentId) && !executionEntity.getId().equals(execution.getId())){
                    System.out.println(execution.getParentId());
                    runtimeService.deleteMultiInstanceExecution(execution.getId(),false);
                }
            }

        }

    }

    //获取当前节点的节点信息
    private FlowNode getFlowNode(String processDefinitionId, String activityId){
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);
        FlowNode flowNode = (FlowNode) bpmnModel.getMainProcess().getFlowElement(activityId);
        return flowNode;
    }



}
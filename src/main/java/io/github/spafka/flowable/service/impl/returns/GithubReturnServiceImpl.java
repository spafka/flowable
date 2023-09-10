package io.github.spafka.flowable.service.impl.returns;


import io.github.spafka.flowable.service.BpmnService;
import io.github.spafka.flowable.service.FlowNodeDto;
import io.github.spafka.flowable.service.ReturnService;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.FlowNode;
import org.flowable.bpmn.model.Process;
import org.flowable.engine.*;
import org.flowable.engine.runtime.Execution;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author by wanghaifeng
 * @link <a href="http://blog.csdn.net/double_hll123/article/details/117215024?spm=1001.2014.3001.5502">...</a>
 * @since 2023/8/29 17:57
 */
@Service(value = "githubReturnService")
public class GithubReturnServiceImpl implements ReturnService {

    @Autowired
    TaskService taskService;
    @Autowired
    RuntimeService runtimeService;
    @Autowired
    HistoryService historyService;
    @Autowired
    ManagementService managementService;
    @Autowired
    RepositoryService repositoryService;
    @Autowired
    BpmnService bpmnService;


    @Override
    @Deprecated
    public List<FlowNodeDto> getCanRejectedFlowNode(BpmnModel bpmnModel, String instanceId, String toId) {
        throw new UnsupportedOperationException();
    }


    @Override
    public boolean returnToTarget(String taskId, String currentId,String targetId) {
        TaskEntity taskEntity = (TaskEntity) taskService.createTaskQuery().taskId(taskId).singleResult();
        //1.把当前的节点设置为空
        if (taskEntity != null) {
            //2.设置审批人
            taskService.saveTask(taskEntity);
            List<String> executionIds = new ArrayList<>();
            //5.判断节点是不是子流程内部的节点
            if (checkActivitySubprocessByActivityId(taskEntity.getProcessDefinitionId(), targetId)) {
                //5.1 子流程内部驳回
                Execution executionTask = runtimeService.createExecutionQuery().executionId(taskEntity.getExecutionId()).singleResult();
                String parentId = executionTask.getParentId();
                List<Execution> executions = runtimeService.createExecutionQuery().parentId(parentId).list();
                executions.forEach(execution -> executionIds.add(execution.getId()));
            } else {
                //5.2 普通驳回
                List<Execution> executions = runtimeService.createExecutionQuery().parentId(taskEntity.getProcessInstanceId()).list();
                executions.forEach(execution -> executionIds.add(execution.getId()));
            }
            moveExecutionsToSingleActivityId(executionIds, targetId);
            //6.删除节点-保证流程图输出
            deleteActivity(targetId, taskEntity.getProcessInstanceId());
            return true;
        }
        return false;
    }


    /**
     * 子流程判断
     */
    private boolean checkActivitySubprocessByActivityId(String processDefId, String activityId) {
        return !CollectionUtils.isEmpty(findFlowNodesByActivityId(processDefId, activityId));
    }

    public List<FlowNode> findFlowNodesByActivityId(String processDefId, String activityId) {
        List<FlowNode> activities = new ArrayList<>();
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefId);
        List<Process> processes = bpmnModel.getProcesses();
        for (Process process : processes) {
            FlowElement flowElement = process.getFlowElement(activityId);
            if (flowElement != null) {
                FlowNode flowNode = (FlowNode) flowElement;
                activities.add(flowNode);
            }
        }
        return activities;
    }


    /**
     * 执行驳回跳转
     */
    private void moveExecutionsToSingleActivityId(List<String> executionIds, String activityId) {
        runtimeService.createChangeActivityStateBuilder()
                .moveExecutionsToSingleActivityId(executionIds, activityId)
                .changeState();
    }

    /**
     * 删除跳转的历史节点信息
     *
     * @param disActivityId     跳转的节点id
     * @param processInstanceId 流程实例id
     */
    protected void deleteActivity(String disActivityId, String processInstanceId) {
//        String tableName = managementService.getTableName(ActivityInstanceEntity.class);
//        String sql = "select t.* from " + tableName + " t where t.PROC_INST_ID_=#{processInstanceId} and t.ACT_ID_ = #{disActivityId} " +
//                " order by t.END_TIME_ ASC";
//        List<ActivityInstance> disActivities = runtimeService.createNativeActivityInstanceQuery().sql(sql)
//                .parameter("processInstanceId", processInstanceId)
//                .parameter("disActivityId", disActivityId).list();
//        //删除运行时和历史节点信息
//        for (ActivityInstance activityInstance : disActivities) {
//            sql = "select t.* from " + tableName + " t where t.PROC_INST_ID_=#{processInstanceId} and (t.END_TIME_ >= #{endTime} or t.END_TIME_ is null)";
//            List<ActivityInstance> datas = runtimeService.createNativeActivityInstanceQuery().sql(sql).parameter("processInstanceId", processInstanceId)
//                    .parameter("endTime", activityInstance.getEndTime()).list();
//
//            //排除目标节点
//            datas = datas.stream().filter(data -> !data.getActivityId().equals(disActivityId)).collect(Collectors.toList());
//            String deleteRuActSql = "delete from ACT_RU_ACTINST where ID_=#{id} ";
//            if (CollectionUtils.isNotEmpty(datas)) {
//                datas.forEach(ai ->
//                        runtimeService.createNativeActivityInstanceQuery().sql(deleteRuActSql).parameter("id", ai.getId())
//                );
//                datas.forEach(ai -> {
//                    historyService.createHistoricActivityInstanceQuery().activityId(ai.getActivityId());
//                });
//            }
//        }
    }

}

package io.github.spafka.flowable.core;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.UserTask;
import org.flowable.engine.*;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.image.ProcessDiagramGenerator;
import org.flowable.image.impl.DefaultProcessDiagramGenerator;
import org.flowable.task.api.Task;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Service
public class FlowService {
    @Resource
    protected RepositoryService repositoryService;

    @Resource
    protected RuntimeService runtimeService;

    @Resource
    protected IdentityService identityService;
    @Resource
    protected HistoryService historyService;

    @Qualifier("processEngine")
    @Resource
    protected ProcessEngine processEngine;

    @Resource
    protected TaskService taskService;
    public List<UserTask> findReturnTaskList(String taskId) {
        // 当前任务 task
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        // 获取流程定义信息
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionId(task.getProcessDefinitionId()).singleResult();
        // 获取所有节点信息，暂不考虑子流程情况
        Process process = repositoryService.getBpmnModel(processDefinition.getId()).getProcesses().get(0);
        Collection<FlowElement> flowElements = process.getFlowElements();
        // 获取当前任务节点元素
        UserTask source = null;
        if (flowElements != null) {
            for (FlowElement flowElement : flowElements) {
                // 类型为用户节点
                if (flowElement.getId().equals(task.getTaskDefinitionKey())) {
                    source = (UserTask) flowElement;
                }
            }
        }


        // 获取节点的所有路线
        List<List<UserTask>> roads = FlowableUtils.findRoad(source, null, null, null);
        // 可回退的节点列表
        List<UserTask> userTaskList = new ArrayList<>();
        for (List<UserTask> road : roads) {
            if (userTaskList.isEmpty()) {
                // 还没有可回退节点直接添加
                userTaskList = road;
            } else {
                // 如果已有回退节点，则比对取交集部分
                userTaskList.retainAll(road);
            }
        }
        return userTaskList;
    }

    /**
     * 获取流程过程图
     *
     * @param processId
     * @return
     */
    public InputStream diagram(String processId) {
        String processDefinitionId;
        // 获取当前的流程实例
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(processId).singleResult();
        // 如果流程已经结束，则得到结束节点
        if (Objects.isNull(processInstance)) {
            HistoricProcessInstance pi = historyService.createHistoricProcessInstanceQuery().processInstanceId(processId).singleResult();

            processDefinitionId = pi.getProcessDefinitionId();
        } else {// 如果流程没有结束，则取当前活动节点
            // 根据流程实例ID获得当前处于活动状态的ActivityId合集
            ProcessInstance pi = runtimeService.createProcessInstanceQuery().processInstanceId(processId).singleResult();
            processDefinitionId = pi.getProcessDefinitionId();
        }

        // 获得活动的节点
        List<HistoricActivityInstance> highLightedFlowList = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processId).orderByHistoricActivityInstanceStartTime().asc().list();

        List<String> highLightedFlows = new ArrayList<>();
        List<String> highLightedNodes = new ArrayList<>();
        //高亮线
        for (HistoricActivityInstance tempActivity : highLightedFlowList) {
            if ("sequenceFlow".equals(tempActivity.getActivityType())) {
                //高亮线
                highLightedFlows.add(tempActivity.getActivityId());
            } else {
                //高亮节点
                highLightedNodes.add(tempActivity.getActivityId());
            }
        }

        //获取流程图
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);
        ProcessEngineConfiguration configuration = processEngine.getProcessEngineConfiguration();
        //获取自定义图片生成器
        ProcessDiagramGenerator diagramGenerator = new DefaultProcessDiagramGenerator();
        return diagramGenerator.generateDiagram(bpmnModel, "png", highLightedNodes, highLightedFlows, configuration.getActivityFontName(),
                configuration.getLabelFontName(), configuration.getAnnotationFontName(), configuration.getClassLoader(), 1.0, true);

    }

//    public void rollbackToNode(String processInstanceId, String targetNodeId) {
//        // 获取当前流程实例的历史活动记录
//        List<HistoricActivityInstance> historicActivityInstances = historyService.createHistoricActivityInstanceQuery()
//                .processInstanceId(processInstanceId)
//                .orderByHistoricActivityInstanceStartTime()
//                .asc()
//                .list();
//
//        // 确定要回退到的目标节点
//        HistoricActivityInstance targetNode = null;
//        for (HistoricActivityInstance historicActivityInstance : historicActivityInstances) {
//            if (historicActivityInstance.getActivityId().equals(targetNodeId)) {
//                targetNode = historicActivityInstance;
//                break;
//            }
//        }
//
//        if (targetNode != null) {
//            // 执行回退操作
//            String currentActivityId = taskService.createTaskQuery()
//                    .processInstanceId(processInstanceId)
//                    .singleResult()
//                    .getTaskDefinitionKey();
//
//            ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
//                    .processInstanceId(processInstanceId)
//                    .singleResult();
//
//            if (processInstance != null && processInstance.isSuspended() && processInstance.getActivityId().equals(currentActivityId)) {
//                runtimeService.activateProcessInstanceById(processInstanceId);
//            }
//
//            runtimeService.createProcessInstanceModification(processInstanceId)
//                    .cancelActivityInstance(currentActivityId)
//                    .startBeforeActivity(targetNode.getActivityId())
//                    .execute();
//        }
//    }
}

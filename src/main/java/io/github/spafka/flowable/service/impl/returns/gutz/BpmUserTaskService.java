package io.github.spafka.flowable.service.impl.returns.gutz;


import io.github.spafka.flowable.service.impl.returns.gutz.entity.BpmTaskModelEntity;
import io.github.spafka.flowable.service.impl.returns.gutz.form.BpmJumpForm;
import io.github.spafka.flowable.service.impl.returns.gutz.query.BpmBackTaskModelQuery;
import org.flowable.bpmn.model.BpmnModel;

import java.util.List;

/**
 * 流程中的任务管理
 *
 * @author guzt
 */
public interface BpmUserTaskService {


    /**
     * 【本任务节点】之前的【历史任务节点】列表
     *
     * @param query     ignore
     * @param bpmnModel
     * @return ignore
     */
    List<BpmTaskModelEntity> listAllBackTaskModel(BpmBackTaskModelQuery query, BpmnModel bpmnModel);

    /**
     * 【本任务节点】跳转指定的【任务节点】 例如驳回等操作，前跳后跳等操作
     * <p>
     * A（本任务节点）    B（目标任务节点集合）
     * <p>
     * （1）如果B有多个节点
     * 必须为同一个并行网关内的任务节点（网关开始、合并节点必须一致）
     * 必须不是同一条流程线上的任务节点
     * <p>
     * （2）如果A和B为同一条顺序流程线上，则可以直接跳转
     * <p>
     * （3）如果A非并行分支上的任务节点
     * B是为并行网关上节点，需要创建其B所在并行网关内其他任务节点已完成日志
     * <p>
     * （4）如果A是并行分支上的任务节点
     * <p>
     * 4.1 从外向里面跳转（父并网关 》子并网关）
     * B是为并行网关上节点，需要创建其B所在并行网关内其他任务节点已完成日志
     * <p>
     * <p>
     * 4.2 从里向外面跳转 （子并网关 》父并网关 【或】 非并行网关上的节点 【或】 其他非父子关系的并行网关节点）
     * 需要清除本任务节点并行网关上（包括父网关）所有的其他未完成的用户任务
     * B是为并行网关上节点，需要创建其B所在并行网关内其他任务节点已完成日志
     *
     * @param form ignore
     */
    void jump(BpmJumpForm form);


}

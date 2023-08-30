package io.github.spafka.flowable.service.impl.returns.gutz;


import io.github.spafka.flowable.service.impl.returns.gutz.dto.UserTaskModelDTO;
import io.github.spafka.flowable.service.impl.returns.gutz.entity.BpmTaskModelEntity;
import io.github.spafka.flowable.service.impl.returns.gutz.query.BpmTaskModelQuery;

import java.util.List;

/**
 * 流程管理
 *
 * @author guzt
 */
public interface BpmProcessService {



    /**
     * 查询流程所有的用户任务节点信息，分并行网关节点和非并行网关节点
     *
     * @param query 查询参数
     * @return List BpmTaskModelEntity
     */
    List<BpmTaskModelEntity> listUserTaskModels(BpmTaskModelQuery query);

    /**
     * 查询流程所有的用户任务节点信息，分并行网关节点和非并行网关节点
     *
     * @param query 查询参数
     * @return List BpmTaskModelEntity
     */
    UserTaskModelDTO getUserTaskModelDto(BpmTaskModelQuery query);





}

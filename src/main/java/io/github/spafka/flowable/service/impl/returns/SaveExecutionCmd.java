package io.github.spafka.flowable.service.impl.returns;

import lombok.extern.slf4j.Slf4j;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.idm.engine.impl.util.CommandContextUtil;

import java.io.Serializable;

/**
 * @author guzt
 */
@Slf4j
public class SaveExecutionCmd implements Command<Void>, Serializable {
    private static final long serialVersionUID = 1L;
    protected ExecutionEntity entity;

    public SaveExecutionCmd(ExecutionEntity entity) {
        this.entity = entity;
    }

    @Override
    public Void execute(CommandContext commandContext) {
        if (this.entity == null) {
            throw new RuntimeException("executionEntity is null");
        } else {
            log.info("执行 SaveExecutionCmd {}", entity);
            CommandContextUtil.getDbSqlSession(commandContext).insert(this.entity);
        }
        return null;
    }
}


package io.github.spafka.flowable.service.impl.returns;

import lombok.extern.slf4j.Slf4j;
import org.flowable.common.engine.impl.cfg.IdGenerator;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.idm.engine.impl.util.CommandContextUtil;

import java.io.Serializable;

/**
 * @author spafka
 */
@Slf4j
public class SaveExecutionCmd implements Command<Void>, Serializable {
    private static final long serialVersionUID = 1L;
    protected ExecutionEntity entity;
    IdGenerator idGenerator;


    public SaveExecutionCmd(ExecutionEntity entity, IdGenerator idGenerator) {
        this.entity = entity;
        this.idGenerator = idGenerator;
    }

    @Override
    public Void execute(CommandContext commandContext) {
        if (this.entity == null) {
            throw new RuntimeException("executionEntity is null");
        } else {
            log.info("执行 SaveExecutionCmd {}", entity);
            CommandContextUtil.getDbSqlSession(commandContext).insert(this.entity, idGenerator);
        }
        return null;
    }
}


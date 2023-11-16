package io.github.spafka.flowable.listerener;

import org.flowable.common.engine.api.delegate.event.AbstractFlowableEventListener;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.delegate.event.impl.FlowableProcessStartedEventImpl;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.springframework.beans.factory.annotation.Autowired;


public abstract class AbstractProcessStartListener extends AbstractFlowableEventListener {

    @Autowired
    private RepositoryService repositoryService;

    @Override
    public void onEvent(FlowableEvent event) {
        FlowableProcessStartedEventImpl startEvent = (FlowableProcessStartedEventImpl) event;
        ProcessDefinition pd = repositoryService.createProcessDefinitionQuery().processDefinitionId(startEvent.getProcessDefinitionId()).singleResult();
        if (pd != null) {
            Deployment deployment = repositoryService.createDeploymentQuery().deploymentId(pd.getDeploymentId()).singleResult();
            String processName = (String) startEvent.getVariables().get("processName");
            if (processName == null) {
                processName = pd.getName();
            }
            createProcess(startEvent.getProcessInstanceId(), pd.getKey(), processName,deployment.getKey());
        }
    }

    public abstract void createProcess(String processId, String processKey, String processName, String processVersion);


    @Override
    public boolean isFailOnException() {
        return false;
    }

}

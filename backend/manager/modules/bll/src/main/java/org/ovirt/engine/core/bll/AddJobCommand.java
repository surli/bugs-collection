package org.ovirt.engine.core.bll;

import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.ovirt.engine.core.bll.context.CommandContext;
import org.ovirt.engine.core.bll.job.ExecutionHandler;
import org.ovirt.engine.core.bll.job.JobRepository;
import org.ovirt.engine.core.common.action.AddJobParameters;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.errors.EngineMessage;
import org.ovirt.engine.core.common.job.Job;
import org.ovirt.engine.core.compat.Guid;

public abstract class AddJobCommand<T extends AddJobParameters> extends CommandBase<T> {

    protected AddJobCommand(T parameters, CommandContext cmdContext) {
        super(parameters, cmdContext);
    }

    @Inject
    private JobRepository jobRepository;

    @Override
    protected boolean validate() {
        boolean retValue = true;
        if (StringUtils.isBlank(getParameters().getDescription())) {
            addValidationMessage(EngineMessage.ACTION_TYPE_EMPTY_DESCRIPTION);
            retValue = false;
        }
        return retValue;
    }

    protected void createJob(VdcActionType actionType, boolean isExternal) {
        Job job = ExecutionHandler.createJob(actionType, this);
        job.setDescription(getParameters().getDescription());
        job.setAutoCleared(getParameters().isAutoCleared());
        Guid id = job.getId();
        job.setExternal(isExternal);
        jobRepository.saveJob(job);
        if (jobDao.get(id) != null) {
            setActionReturnValue(id);
            setSucceeded(true);
        }
        else {
            setSucceeded(false);
        }
    }

}

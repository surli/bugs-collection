package org.ovirt.engine.core.bll;

import java.util.List;

import javax.inject.Inject;

import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.dao.VmDao;

public class GetVmsRunningOnOrMigratingToVdsQuery<P extends IdQueryParameters> extends QueriesCommandBase<P> {

    @Inject
    private VmHandler vmHandler;

    @Inject
    private VmDao vmDao;

    public GetVmsRunningOnOrMigratingToVdsQuery(P parameters) {
        super(parameters);
    }

    @Override
    protected void executeQueryCommand() {
        List<VM> allRunningOnOrMigratingToVds = vmDao.getAllRunningOnOrMigratingToVds(getParameters().getId());
        updateStatistics(allRunningOnOrMigratingToVds);

        getQueryReturnValue().setReturnValue(
                allRunningOnOrMigratingToVds);
    }

    protected void updateStatistics(List<VM> allRunningOnOrMigratingToVds) {
        if (allRunningOnOrMigratingToVds != null) {
            allRunningOnOrMigratingToVds.forEach(vmHandler::updateVmStatistics);
        }
    }

}

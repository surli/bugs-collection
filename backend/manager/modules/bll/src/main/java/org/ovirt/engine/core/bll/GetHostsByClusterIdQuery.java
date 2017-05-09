package org.ovirt.engine.core.bll;

import javax.inject.Inject;

import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.dao.VdsDao;

public class GetHostsByClusterIdQuery<P extends IdQueryParameters> extends QueriesCommandBase<P> {
    @Inject
    private VdsDao vdsDao;

    public GetHostsByClusterIdQuery(P parameters) {
        super(parameters);
    }

    @Override
    protected void executeQueryCommand() {
        getQueryReturnValue().setReturnValue(
                vdsDao.getAllForCluster(getParameters().getId(), getUserID(), getParameters().isFiltered()));
    }

}

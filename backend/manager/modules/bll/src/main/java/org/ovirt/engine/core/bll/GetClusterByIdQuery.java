package org.ovirt.engine.core.bll;

import javax.inject.Inject;

import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.dao.ClusterDao;

public class GetClusterByIdQuery<P extends IdQueryParameters> extends QueriesCommandBase<P> {
    @Inject
    private ClusterDao clusterDao;

    public GetClusterByIdQuery(P parameters) {
        super(parameters);
    }

    @Override
    protected void executeQueryCommand() {
        getQueryReturnValue().setReturnValue(clusterDao.get(getParameters().getId(), getUserID(), getParameters().isFiltered()));
    }
}

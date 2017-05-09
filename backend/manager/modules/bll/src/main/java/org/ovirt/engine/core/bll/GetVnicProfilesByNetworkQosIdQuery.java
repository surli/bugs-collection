package org.ovirt.engine.core.bll;

import javax.inject.Inject;

import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.dao.network.VnicProfileViewDao;

public class GetVnicProfilesByNetworkQosIdQuery<P extends IdQueryParameters> extends QueriesCommandBase<P> {
    @Inject
    private VnicProfileViewDao vnicProfileViewDao;

    public GetVnicProfilesByNetworkQosIdQuery(P parameters) {
        super(parameters);
    }

    @Override
    protected void executeQueryCommand() {
        getQueryReturnValue().setReturnValue(vnicProfileViewDao.getAllForNetworkQos(getParameters().getId()));
    }
}

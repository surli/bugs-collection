package org.ovirt.engine.core.bll.network.vm;

import javax.inject.Inject;

import org.ovirt.engine.core.bll.QueriesCommandBase;
import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.dao.network.VmNicFilterParameterDao;

public class GetVmInterfaceFilterParametersByVmInterfaceIdQuery<P extends IdQueryParameters> extends QueriesCommandBase<P> {
    @Inject
    private VmNicFilterParameterDao vmNicFilterParameterDao;

    public GetVmInterfaceFilterParametersByVmInterfaceIdQuery(P parameters) {
        super(parameters);
    }

    @Override
    protected void executeQueryCommand() {
        getQueryReturnValue().setReturnValue(vmNicFilterParameterDao.getAllForVmNic(getParameters().getId(),
                getUserID(),
                getParameters().isFiltered()));
    }
}

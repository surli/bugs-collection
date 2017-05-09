package org.ovirt.engine.core.bll;

import javax.inject.Inject;

import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.dao.qos.QosBaseDao;

public class GetQosByIdQuery extends QueriesCommandBase<IdQueryParameters> {
    @Inject
    private QosBaseDao qosBaseDao;

    public GetQosByIdQuery(IdQueryParameters parameters) {
        super(parameters);
    }

    @Override
    protected void executeQueryCommand() {
        getQueryReturnValue().setReturnValue(qosBaseDao.get(getParameters().getId()));
    }

}

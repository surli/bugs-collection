package org.ovirt.engine.core.bll;

import javax.inject.Inject;

import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.dao.AuditLogDao;

/** A query to return all the Audit Logs according to a given VM Template ID */
public class GetAllAuditLogsByVMTemplateIdQuery<P extends IdQueryParameters> extends QueriesCommandBase<P> {
    @Inject
    private AuditLogDao auditLogDao;

    public GetAllAuditLogsByVMTemplateIdQuery(P parameters) {
        super(parameters);
    }

    /** Actually executes the query, and stores the result in {@link #getQueryReturnValue()} */
    @Override
    protected void executeQueryCommand() {
        getQueryReturnValue().setReturnValue(
                auditLogDao.getAllByVMTemplateId(getParameters().getId(), getUserID(), getParameters().isFiltered()));
    }
}

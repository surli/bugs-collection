package org.ovirt.engine.core.bll.numa.host;

import java.util.List;

import javax.inject.Inject;

import org.ovirt.engine.core.bll.QueriesCommandBase;
import org.ovirt.engine.core.common.businessentities.VdsNumaNode;
import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.dao.VdsNumaNodeDao;

public class GetVdsNumaNodesByVdsIdQuery<P extends IdQueryParameters> extends QueriesCommandBase<P> {
    @Inject
    private VdsNumaNodeDao vdsNumaNodeDao;

    public GetVdsNumaNodesByVdsIdQuery(P parameters) {
        super(parameters);
    }

    @Override
    protected void executeQueryCommand() {
        List<VdsNumaNode> numaNodes = vdsNumaNodeDao.getAllVdsNumaNodeByVdsId(getParameters().getId());
        getQueryReturnValue().setReturnValue(numaNodes);
    }

}

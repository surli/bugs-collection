package org.ovirt.engine.core.bll;

import org.ovirt.engine.core.bll.context.EngineContext;
import org.ovirt.engine.core.common.queries.VdcQueryParametersBase;
import org.ovirt.engine.core.compat.Guid;

public abstract class FenceQueryBase<P extends VdcQueryParametersBase> extends QueriesCommandBase<P> {
    protected FenceQueryBase(P parameters) {
        super(parameters);
    }

    public FenceQueryBase(P parameters, EngineContext engineContext) {
        super(parameters, engineContext);
    }

    private Guid privateVdsId;

    protected Guid getVdsId() {
        return privateVdsId;
    }

    protected void setVdsId(Guid value) {
        privateVdsId = value;
    }

    private String privateVdsName;

    protected String getVdsName() {
        return privateVdsName;
    }

    protected void setVdsName(String value) {
        privateVdsName = value;
    }
}

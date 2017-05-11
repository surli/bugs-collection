package org.ovirt.engine.api.restapi.resource;

import javax.ws.rs.core.Response;

import org.ovirt.engine.api.model.AffinityGroup;
import org.ovirt.engine.api.resource.AffinityGroupResource;
import org.ovirt.engine.api.resource.AffinityGroupVmsResource;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.common.scheduling.parameters.AffinityGroupCRUDParameters;

public class BackendAffinityGroupResource
        extends AbstractBackendSubResource<AffinityGroup, org.ovirt.engine.core.common.scheduling.AffinityGroup>
        implements AffinityGroupResource {

    public BackendAffinityGroupResource(String id) {
        super(id, AffinityGroup.class,
                org.ovirt.engine.core.common.scheduling.AffinityGroup.class);
    }

    @Override
    public AffinityGroup get() {
        return performGet(VdcQueryType.GetAffinityGroupById, new IdQueryParameters(guid));
    }

    @Override
    public AffinityGroup update(final AffinityGroup incoming) {
        return performUpdate(incoming,
                new QueryIdResolver<>(VdcQueryType.GetAffinityGroupById, IdQueryParameters.class),
                VdcActionType.EditAffinityGroup,
                (model, entity) -> new AffinityGroupCRUDParameters(guid, map(incoming, entity)));
    }

    @Override
    public AffinityGroupVmsResource getVmsResource() {
        return inject(new BackendAffinityGroupVmsResource(guid));
    }


    @Override
    public Response remove() {
        get();
        AffinityGroupCRUDParameters params = new AffinityGroupCRUDParameters();
        params.setAffinityGroupId(asGuid(id));
        return performAction(VdcActionType.RemoveAffinityGroup, params);
    }
}

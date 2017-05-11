package org.ovirt.engine.core.bll;

import org.ovirt.engine.core.common.businessentities.HostJobInfo.HostJobType;

public class StorageJobCallback extends HostJobCallback {

    @Override
    protected HostJobType getHostJobType() {
        return HostJobType.storage;
    }

}

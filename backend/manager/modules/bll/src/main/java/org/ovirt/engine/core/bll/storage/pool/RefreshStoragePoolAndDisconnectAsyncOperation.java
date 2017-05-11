package org.ovirt.engine.core.bll.storage.pool;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.ovirt.engine.core.bll.storage.connection.StorageHelperDirector;
import org.ovirt.engine.core.common.businessentities.StorageDomain;
import org.ovirt.engine.core.common.businessentities.StoragePool;
import org.ovirt.engine.core.common.businessentities.StoragePoolIsoMap;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.interfaces.VDSBrokerFrontend;
import org.ovirt.engine.core.common.vdscommands.ConnectStoragePoolVDSCommandParameters;
import org.ovirt.engine.core.common.vdscommands.VDSCommandType;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dao.StorageDomainDao;
import org.ovirt.engine.core.dao.StoragePoolIsoMapDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RefreshStoragePoolAndDisconnectAsyncOperation extends ActivateDeactivateSingleAsyncOperation {
    private static final Logger log = LoggerFactory.getLogger(RefreshStoragePoolAndDisconnectAsyncOperation.class);

    private Guid masterStorageDomainId;

    private List<StoragePoolIsoMap> storagePoolIsoMap;

    @Inject
    private VDSBrokerFrontend resourceManager;

    @Inject
    private StorageDomainDao storageDomainDao;

    @Inject
    private StoragePoolIsoMapDao storagePoolIsoMapDao;

    @Inject
    private StorageHelperDirector storageHelperDirector;

    public RefreshStoragePoolAndDisconnectAsyncOperation(ArrayList<VDS> vdss, StorageDomain domain,
            StoragePool storagePool) {
        super(vdss, domain, storagePool);
    }

    @PostConstruct
    private void init() {
        masterStorageDomainId = storageDomainDao.getMasterStorageDomainIdForPool(getStoragePool().getId());
        storagePoolIsoMap = storagePoolIsoMapDao.getAllForStoragePool(getStoragePool().getId());
    }

    @Override
    public void execute(int iterationId) {
        try {
            resourceManager.runVdsCommand(
                    VDSCommandType.ConnectStoragePool,
                    new ConnectStoragePoolVDSCommandParameters(getVdss().get(iterationId), getStoragePool(),
                            masterStorageDomainId, storagePoolIsoMap, true));
            storageHelperDirector.getItem(getStorageDomain().getStorageType())
                    .disconnectStorageFromDomainByVdsId(getStorageDomain(), getVdss().get(iterationId).getId());
        } catch (RuntimeException e) {
            log.error("Failed to connect/refresh storagePool. Host '{}' to storage pool '{}': {}",
                    getVdss().get(iterationId).getName(),
                    getStoragePool().getName(),
                    e.getMessage());
            log.debug("Exception", e);
        }

    }
}

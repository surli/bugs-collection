package org.ovirt.engine.core.bll.storage.pool;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.ovirt.engine.core.bll.MoveMacs;
import org.ovirt.engine.core.bll.NonTransactiveCommandAttribute;
import org.ovirt.engine.core.bll.RenamedEntityInfoProvider;
import org.ovirt.engine.core.bll.context.CommandContext;
import org.ovirt.engine.core.bll.network.cluster.ManagementNetworkUtil;
import org.ovirt.engine.core.bll.storage.connection.StorageHelperDirector;
import org.ovirt.engine.core.bll.utils.VersionSupport;
import org.ovirt.engine.core.bll.validator.NetworkValidator;
import org.ovirt.engine.core.bll.validator.storage.StorageDomainToPoolRelationValidator;
import org.ovirt.engine.core.bll.validator.storage.StoragePoolValidator;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.FeatureSupported;
import org.ovirt.engine.core.common.VdcObjectType;
import org.ovirt.engine.core.common.action.StoragePoolManagementParameter;
import org.ovirt.engine.core.common.businessentities.Cluster;
import org.ovirt.engine.core.common.businessentities.StorageDomain;
import org.ovirt.engine.core.common.businessentities.StorageDomainStatic;
import org.ovirt.engine.core.common.businessentities.StorageDomainStatus;
import org.ovirt.engine.core.common.businessentities.StorageDomainType;
import org.ovirt.engine.core.common.businessentities.StorageFormatType;
import org.ovirt.engine.core.common.businessentities.StoragePool;
import org.ovirt.engine.core.common.businessentities.StoragePoolStatus;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.network.Network;
import org.ovirt.engine.core.common.businessentities.storage.StorageType;
import org.ovirt.engine.core.common.errors.EngineError;
import org.ovirt.engine.core.common.errors.EngineException;
import org.ovirt.engine.core.common.errors.EngineMessage;
import org.ovirt.engine.core.common.utils.Pair;
import org.ovirt.engine.core.common.utils.VersionStorageFormatUtil;
import org.ovirt.engine.core.common.vdscommands.UpgradeStoragePoolVDSCommandParameters;
import org.ovirt.engine.core.common.vdscommands.VDSCommandType;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.TransactionScopeOption;
import org.ovirt.engine.core.compat.Version;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.AuditLogableBase;
import org.ovirt.engine.core.utils.ReplacementUtils;
import org.ovirt.engine.core.utils.threadpool.ThreadPoolUtil;
import org.ovirt.engine.core.utils.transaction.TransactionSupport;

@NonTransactiveCommandAttribute
public class UpdateStoragePoolCommand<T extends StoragePoolManagementParameter> extends
        StoragePoolManagementCommandBase<T>  implements RenamedEntityInfoProvider{

    @Inject
    private ManagementNetworkUtil managementNetworkUtil;

    @Inject
    private MoveMacs moveMacs;

    /**
     * Constructor for command creation when compensation is applied on startup
     */
    public UpdateStoragePoolCommand(Guid commandId) {
        super(commandId);
    }

    public UpdateStoragePoolCommand(T parameters, CommandContext commandContext) {
        super(parameters, commandContext);
    }

    private StoragePool oldStoragePool;
    private StorageDomain masterDomainForPool;

    @Override
    protected void executeCommand() {
        updateQuotaCache();
        copyUnchangedStoragePoolProperties(getStoragePool(), oldStoragePool);
        storagePoolDao.updatePartial(getStoragePool());

        updateStoragePoolFormatType();
        updateAllClustersMacPool();
        syncLunsForBlockStorageDomains();
        setSucceeded(true);
    }

    private void updateAllClustersMacPool() {
        final Guid newMacPoolId = getParameters().getStoragePool().getMacPoolId();
        final boolean shouldSetNewMacPoolOnAllClusters = newMacPoolId != null;
        if (shouldSetNewMacPoolOnAllClusters) {
            List<Cluster> clusters = clusterDao.getAllForStoragePool(getStoragePoolId());
            for (Cluster cluster : clusters) {
                moveMacs.updateClusterAndMoveMacs(cluster, newMacPoolId, getContext());
            }
        }
    }

    private void syncLunsForBlockStorageDomains() {
        if (!FeatureSupported.discardAfterDeleteSupported(getOldStoragePool().getCompatibilityVersion()) &&
                FeatureSupported.discardAfterDeleteSupported(getStoragePool().getCompatibilityVersion())) {
            // Discard was not supported, and now it should be.
            Collection<Guid> unSyncedStorageDomains =
                    syncLunsForStorageDomains(storageDomainDao.getAllForStoragePool(getStoragePoolId()));
            if (!unSyncedStorageDomains.isEmpty()) {
                String unSyncedStorageDomainsList = unSyncedStorageDomains.stream()
                        .map(Guid::toString)
                        .collect(Collectors.joining(", "));
                addCustomValue("StorageDomainsIds", unSyncedStorageDomainsList);
                auditLogDirector.log(this, AuditLogType.STORAGE_DOMAINS_COULD_NOT_BE_SYNCED);
            }
        }
    }

    /**
     * Gets a collection of storage domains and calls
     * SyncLunsInfoForBlockStorageDomainCommand for each active block
     * domain with a random active host in the dc.
     * @param storageDomains a collection of storage domains to sync.
     * @return the storage domains that could not be synced.
     */
    protected Collection<Guid> syncLunsForStorageDomains(Collection<StorageDomain> storageDomains) {
        List<Callable<Pair<Boolean, Guid>>> syncLunsCommands = storageDomains.stream()
                .filter(storageDomain -> StorageDomainStatus.Active == storageDomain.getStatus())
                .filter(storageDomain -> storageDomain.getStorageType().isBlockDomain())
                .map(storageDomain -> (Callable<Pair<Boolean, Guid>>) () -> syncDomainLuns(storageDomain))
                .collect(Collectors.toList());
        if (syncLunsCommands.isEmpty()) {
            return Collections.emptyList();
        }
        return ThreadPoolUtil.invokeAll(syncLunsCommands).stream()
                .filter(domainSyncedPair -> !domainSyncedPair.getFirst())
                .map(Pair::getSecond)
                .collect(Collectors.toList());
    }

    protected Pair<Boolean, Guid> syncDomainLuns(StorageDomain storageDomain) {
        return new Pair<>(
                StorageHelperDirector.getInstance()
                        .getItem(storageDomain.getStorageType()).syncDomainInfo(storageDomain, null),
                storageDomain.getId());
    }

    private void updateQuotaCache() {
        if(wasQuotaEnforcementChanged()){
            getQuotaManager().removeStoragePoolFromCache(getStoragePool().getId());
        }
    }

    /**
     * Checks whether part of the update was disabling quota enforcement on the Data Center
     */
    private boolean wasQuotaEnforcementChanged() {
        return getOldStoragePool().getQuotaEnforcementType() != getStoragePool().getQuotaEnforcementType();
    }

    private StorageFormatType updatePoolAndDomainsFormat(final Version spVersion) {
        final StoragePool storagePool = getStoragePool();

        final StorageFormatType targetFormat = VersionStorageFormatUtil.getForVersion(spVersion);

        storagePool.setCompatibilityVersion(spVersion);
        storagePool.setStoragePoolFormatType(targetFormat);

        TransactionSupport.executeInScope(TransactionScopeOption.RequiresNew,
                () -> {
                    storagePoolDao.updatePartial(storagePool);
                    updateMemberDomainsFormat(targetFormat);
                    vmStaticDao.incrementDbGenerationForAllInStoragePool(storagePool.getId());
                    return null;
                });

        return targetFormat;
    }

    private void updateStoragePoolFormatType() {
        final StoragePool storagePool = getStoragePool();
        final Guid spId = storagePool.getId();
        final Version spVersion = storagePool.getCompatibilityVersion();
        final Version oldSpVersion = getOldStoragePool().getCompatibilityVersion();

        if (oldSpVersion.equals(spVersion)) {
            return;
        }

        StorageFormatType targetFormat = updatePoolAndDomainsFormat(spVersion);

        if (getOldStoragePool().getStatus() == StoragePoolStatus.Up) {
            try {
                // No need to worry about "reupgrading" as VDSM will silently ignore
                // the request.
                runVdsCommand(VDSCommandType.UpgradeStoragePool,
                    new UpgradeStoragePoolVDSCommandParameters(spId, targetFormat));
            } catch (EngineException e) {
                log.warn("Upgrade process of Storage Pool '{}' has encountered a problem due to following reason: {}",
                        spId, e.getMessage());
                auditLogDirector.log(this, AuditLogType.UPGRADE_STORAGE_POOL_ENCOUNTERED_PROBLEMS);

                // if we get this error we know that no update was made, so we can safely revert the db updates
                // and return.
                if (e.getVdsError() != null && e.getErrorCode() == EngineError.PoolUpgradeInProgress) {
                    updatePoolAndDomainsFormat(oldSpVersion);
                    return;
                }
            }
        }

        runSynchronizeOperation(new RefreshPoolSingleAsyncOperationFactory(), new ArrayList<Guid>());
    }

    private void updateMemberDomainsFormat(StorageFormatType targetFormat) {
        Guid spId = getStoragePool().getId();
        List<StorageDomainStatic> domains = storageDomainStaticDao.getAllForStoragePool(spId);
        for (StorageDomainStatic domain : domains) {
            StorageDomainType sdType = domain.getStorageDomainType();

            if (sdType == StorageDomainType.Data || sdType == StorageDomainType.Master) {
                log.info("Setting storage domain '{}' (type '{}') to format '{}'",
                               domain.getId(), sdType, targetFormat);

                domain.setStorageFormat(targetFormat);
                storageDomainStaticDao.update(domain);
            }
        }
    }

    @Override
    public AuditLogType getAuditLogTypeValue() {
        return getSucceeded() ? AuditLogType.USER_UPDATE_STORAGE_POOL : AuditLogType.USER_UPDATE_STORAGE_POOL_FAILED;
    }

    @Override
    protected void setActionMessageParameters() {
        super.setActionMessageParameters();
        addValidationMessage(EngineMessage.VAR__ACTION__UPDATE);
    }

    @Override
    protected boolean validate() {
        if (!checkStoragePool()) {
            return false;
        }

        // Name related validations
        if (!StringUtils.equals(getOldStoragePool().getName(), getStoragePool().getName())
                && !isStoragePoolUnique(getStoragePool().getName())) {
            return failValidation(EngineMessage.ACTION_TYPE_FAILED_STORAGE_POOL_NAME_ALREADY_EXIST);
        }
        if (!checkStoragePoolNameLengthValid()) {
            return false;
        }

        List<StorageDomainStatic> poolDomains = storageDomainStaticDao.getAllForStoragePool(getStoragePool().getId());
        if (getOldStoragePool().isLocal() && !getStoragePool().isLocal()
                && poolDomains.stream().anyMatch(sdc -> sdc.getStorageType() == StorageType.LOCALFS)) {
            return failValidation(EngineMessage.ERROR_CANNOT_CHANGE_STORAGE_POOL_TYPE_WITH_LOCAL);
        }
        if (!getOldStoragePool().isLocal() && getStoragePool().isLocal()) {
            List<Cluster> clusters = clusterDao.getAllForStoragePool(getStoragePool().getId());
            if (clusters.size() > 1) {
                return failValidation(EngineMessage.CLUSTER_CANNOT_ADD_MORE_THEN_ONE_HOST_TO_LOCAL_STORAGE);
            }
            List<VDS> hosts = vdsDao.getAllForStoragePool(getStoragePool().getId());
            if (hosts.size() > 1) {
                return failValidation(EngineMessage.VDS_CANNOT_ADD_MORE_THEN_ONE_HOST_TO_LOCAL_STORAGE);
            }
        }
        if ( !getOldStoragePool().getCompatibilityVersion().equals(getStoragePool()
                .getCompatibilityVersion())) {
            if (!isStoragePoolVersionSupported()) {
                return failValidation(VersionSupport.getUnsupportedVersionMessage());
            }
            // decreasing of compatibility version is allowed under conditions
            else if (getStoragePool().getCompatibilityVersion().compareTo(getOldStoragePool().getCompatibilityVersion()) < 0) {
                if (!poolDomains.isEmpty() && !isCompatibilityVersionChangeAllowedForDomains(poolDomains)) {
                    return false;
                }
                List<Network> networks = networkDao.getAllForDataCenter(getStoragePoolId());
                for (Network network : networks) {
                    NetworkValidator validator = getNetworkValidator(network);
                    validator.setDataCenter(getStoragePool());
                    if (!getManagementNetworkUtil().isManagementNetwork(network.getId())
                            || !validator.canNetworkCompatibilityBeDecreased()) {
                        return failValidation(EngineMessage.ACTION_TYPE_FAILED_CANNOT_DECREASE_DATA_CENTER_COMPATIBILITY_VERSION);
                    }
                }
            } else if (!checkAllClustersLevel()) {  // Check all clusters has at least the same compatibility version.
                return false;
            }
        }

        StoragePoolValidator validator = createStoragePoolValidator();
        return validate(validator.isNotLocalfsWithDefaultCluster());
    }

    private boolean isCompatibilityVersionChangeAllowedForDomains(List<StorageDomainStatic> poolDomains) {
        List<String> formatProblematicDomains = new ArrayList<>();

        for (StorageDomainStatic domainStatic : poolDomains) {
            StorageDomainToPoolRelationValidator attachDomainValidator = getAttachDomainValidator(domainStatic);

            if (!attachDomainValidator.isStorageDomainFormatCorrectForDC().isValid()) {
                formatProblematicDomains.add(domainStatic.getName());
            }
        }

        return manageCompatibilityVersionChangeCheckResult(formatProblematicDomains);
    }

    private boolean manageCompatibilityVersionChangeCheckResult(List<String> formatProblematicDomains) {
        if (!formatProblematicDomains.isEmpty()) {
            addValidationMessage(EngineMessage.ACTION_TYPE_FAILED_DECREASING_COMPATIBILITY_VERSION_CAUSES_STORAGE_FORMAT_DOWNGRADING);
            getReturnValue().getValidationMessages().addAll(ReplacementUtils.replaceWith("formatDowngradedDomains", formatProblematicDomains, "," , formatProblematicDomains.size()));
        }
        return formatProblematicDomains.isEmpty();
    }

    protected StorageDomainToPoolRelationValidator getAttachDomainValidator(StorageDomainStatic domainStatic) {
        return new StorageDomainToPoolRelationValidator(domainStatic, getStoragePool());
    }

    protected boolean checkAllClustersLevel() {
        boolean returnValue = true;
        List<Cluster> clusters = clusterDao.getAllForStoragePool(getStoragePool().getId());
        List<String> lowLevelClusters = new ArrayList<>();
        for (Cluster cluster : clusters) {
            if (getStoragePool().getCompatibilityVersion().compareTo(cluster.getCompatibilityVersion()) > 0) {
                lowLevelClusters.add(cluster.getName());
            }
        }
        if (!lowLevelClusters.isEmpty()) {
            returnValue = false;
            getReturnValue().getValidationMessages().add(String.format("$ClustersList %1$s",
                    StringUtils.join(lowLevelClusters, ",")));
            getReturnValue()
                    .getValidationMessages()
                    .add(EngineMessage.ERROR_CANNOT_UPDATE_STORAGE_POOL_COMPATIBILITY_VERSION_BIGGER_THAN_CLUSTERS
                            .toString());
        }
        return returnValue;
    }

    private StorageDomain getMasterDomain() {
        if (masterDomainForPool == null) {
            Guid masterId = storageDomainDao.getMasterStorageDomainIdForPool(getStoragePoolId());
            if (Guid.Empty.equals(masterId)) {
                masterDomainForPool = storageDomainDao.get(masterId);
            }
        }
        return masterDomainForPool;
    }

    protected NetworkValidator getNetworkValidator(Network network) {
        return new NetworkValidator(vmDao, network);
    }

    protected StoragePoolValidator createStoragePoolValidator() {
        return new StoragePoolValidator(getStoragePool());
    }

    protected boolean isStoragePoolVersionSupported() {
        return VersionSupport.checkVersionSupported(getStoragePool().getCompatibilityVersion());
    }

    /**
     * Copy properties from old entity which assumed not to be available in the param object.
     */
    private static void copyUnchangedStoragePoolProperties(StoragePool newStoragePool, StoragePool oldStoragePool) {
        newStoragePool.setStoragePoolFormatType(oldStoragePool.getStoragePoolFormatType());
    }

    @Override
    public String getEntityType() {
        return VdcObjectType.StoragePool.getVdcObjectTranslation();
    }

    @Override
    public String getEntityOldName() {
        return getOldStoragePool().getName();
    }

    @Override
    public String getEntityNewName() {
        return getParameters().getStoragePool().getName();
    }

    @Override
    public void setEntityId(AuditLogableBase logable) {
        logable.setStoragePoolId(getOldStoragePool().getId());
    }

    private StoragePool getOldStoragePool() {
        if (oldStoragePool == null) {
            oldStoragePool = storagePoolDao.get(getStoragePool().getId());
        }

        return oldStoragePool;
    }

    ManagementNetworkUtil getManagementNetworkUtil() {
        return managementNetworkUtil;
    }
}

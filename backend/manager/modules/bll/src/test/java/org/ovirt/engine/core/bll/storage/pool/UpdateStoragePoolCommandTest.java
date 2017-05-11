package org.ovirt.engine.core.bll.storage.pool;

import static org.apache.commons.collections.CollectionUtils.isEqualCollection;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.ovirt.engine.core.utils.MockConfigRule.mockConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.ovirt.engine.core.bll.BaseCommandTest;
import org.ovirt.engine.core.bll.ValidateTestUtils;
import org.ovirt.engine.core.bll.ValidationResult;
import org.ovirt.engine.core.bll.network.cluster.ManagementNetworkUtil;
import org.ovirt.engine.core.bll.utils.VersionSupport;
import org.ovirt.engine.core.bll.validator.NetworkValidator;
import org.ovirt.engine.core.bll.validator.storage.StoragePoolValidator;
import org.ovirt.engine.core.common.action.StoragePoolManagementParameter;
import org.ovirt.engine.core.common.businessentities.Cluster;
import org.ovirt.engine.core.common.businessentities.StorageDomain;
import org.ovirt.engine.core.common.businessentities.StorageDomainStatic;
import org.ovirt.engine.core.common.businessentities.StorageDomainStatus;
import org.ovirt.engine.core.common.businessentities.StoragePool;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.network.Network;
import org.ovirt.engine.core.common.businessentities.storage.StorageType;
import org.ovirt.engine.core.common.config.ConfigValues;
import org.ovirt.engine.core.common.errors.EngineMessage;
import org.ovirt.engine.core.common.utils.Pair;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.Version;
import org.ovirt.engine.core.dao.ClusterDao;
import org.ovirt.engine.core.dao.StorageDomainStaticDao;
import org.ovirt.engine.core.dao.StoragePoolDao;
import org.ovirt.engine.core.dao.VdsDao;
import org.ovirt.engine.core.dao.network.NetworkDao;
import org.ovirt.engine.core.utils.MockConfigRule;

public class UpdateStoragePoolCommandTest extends BaseCommandTest {

    private static final Version VERSION_1_0 = new Version(1, 0);
    private static final Version VERSION_1_1 = new Version(1, 1);
    private static final Version VERSION_1_2 = new Version(1, 2);
    private static final Version VERSION_2_0 = new Version(2, 0);
    private static final Set<Version> SUPPORTED_VERSIONS =
            new HashSet<>(Arrays.asList(VERSION_1_0, VERSION_1_1, VERSION_1_2));
    private static final Guid DEFAULT_CLUSTER_ID = Guid.newGuid();
    private static final Guid NON_DEFAULT_CLUSTER_ID = Guid.newGuid();

    @ClassRule
    public static MockConfigRule mcr = new MockConfigRule(
            mockConfig(ConfigValues.AutoRegistrationDefaultClusterID, DEFAULT_CLUSTER_ID),
            mockConfig(ConfigValues.StoragePoolNameSizeLimit, 10),
            mockConfig(ConfigValues.SupportedClusterLevels, SUPPORTED_VERSIONS)
    );

    @Spy
    @InjectMocks
    private UpdateStoragePoolCommand<StoragePoolManagementParameter> cmd =
            new UpdateStoragePoolCommand<>(new StoragePoolManagementParameter(createStoragePool()), null);

    @Mock
    private StoragePoolDao spDao;
    @Mock
    private StorageDomainStaticDao sdDao;
    @Mock
    private ClusterDao clusterDao;
    @Mock
    private VdsDao vdsDao;
    @Mock
    private NetworkDao networkDao;
    @Mock
    private ManagementNetworkUtil managementNetworkUtil;
    private StoragePoolValidator poolValidator;

    @Before
    public void setUp() {
        when(spDao.get(any())).thenReturn(createStoragePool());
        when(clusterDao.getAllForStoragePool(any())).thenReturn(createClusterList());

        // Spy the StoragePoolValidator:
        poolValidator = spy(new StoragePoolValidator(cmd.getStoragePool()));
        doReturn(ValidationResult.VALID).when(poolValidator).isNotLocalfsWithDefaultCluster();
        doReturn(poolValidator).when(cmd).createStoragePoolValidator();
    }

    @Test
    public void happyPath() {
        ValidateTestUtils.runAndAssertValidateSuccess(cmd);
    }

    @Test
    public void nameExists() {
        newPoolNameIsAlreadyTaken();
        ValidateTestUtils.runAndAssertValidateFailure(cmd, EngineMessage.ACTION_TYPE_FAILED_STORAGE_POOL_NAME_ALREADY_EXIST);
    }

    @Test
    public void hasLocalDomain() {
        StorageDomainStatic sdc = new StorageDomainStatic();
        sdc.setStorageType(StorageType.LOCALFS);
        StoragePool existingSp = createStoragePool();
        existingSp.setIsLocal(true);
        when(spDao.get(any())).thenReturn(existingSp);
        when(sdDao.getAllForStoragePool(any())).thenReturn(Collections.singletonList(sdc));
        ValidateTestUtils.runAndAssertValidateFailure(cmd,
                EngineMessage.ERROR_CANNOT_CHANGE_STORAGE_POOL_TYPE_WITH_LOCAL);
    }

    @Test
    public void hasSharedDomain() {
        StorageDomainStatic sdc = new StorageDomainStatic();
        sdc.setStorageType(StorageType.NFS);
        when(sdDao.getAllForStoragePool(any())).thenReturn(Collections.singletonList(sdc));
        ValidateTestUtils.runAndAssertValidateSuccess(cmd);
    }

    @Test
    public void hasNoStorageDomains() {
        ValidateTestUtils.runAndAssertValidateSuccess(cmd);
    }

    @Test
    public void hasMultipleClustersForLocalDC() {
        List<Cluster> clusters = Arrays.asList(new Cluster(), new Cluster());
        when(clusterDao.getAllForStoragePool(any())).thenReturn(clusters);
        cmd.getStoragePool().setIsLocal(true);
        ValidateTestUtils.runAndAssertValidateFailure(cmd,
                EngineMessage.CLUSTER_CANNOT_ADD_MORE_THEN_ONE_HOST_TO_LOCAL_STORAGE);
    }

    @Test
    public void hasMultipleHostsForLocalDC() {
        when(sdDao.getAllForStoragePool(any())).thenReturn(Collections.emptyList());
        List<VDS> hosts = Arrays.asList(new VDS(), new VDS());
        when(vdsDao.getAllForStoragePool(any())).thenReturn(hosts);
        cmd.getStoragePool().setIsLocal(true);
        ValidateTestUtils.runAndAssertValidateFailure(cmd,
                EngineMessage.VDS_CANNOT_ADD_MORE_THEN_ONE_HOST_TO_LOCAL_STORAGE);
    }

    @Test
    public void unsupportedVersion() {
        cmd.getStoragePool().setCompatibilityVersion(VERSION_2_0);
        ValidateTestUtils.runAndAssertValidateFailure(cmd, VersionSupport.getUnsupportedVersionMessage());
    }

    @Test
    public void lowerVersionNoHostsNoNetwork() {
        cmd.getStoragePool().setCompatibilityVersion(VERSION_1_0);
        addNonDefaultClusterToPool();
        ValidateTestUtils.runAndAssertValidateSuccess(cmd);
    }

    @Test
    public void lowerVersionHostsNoNetwork() {
        cmd.getStoragePool().setCompatibilityVersion(VERSION_1_0);
        addNonDefaultClusterToPool();
        addHostsToCluster();
        ValidateTestUtils.runAndAssertValidateSuccess(cmd);
    }

    @Test
    public void lowerVersionNoHostsWithNetwork() {
        cmd.getStoragePool().setCompatibilityVersion(VERSION_1_0);
        addNonDefaultClusterToPool();
        addNonManagementNetworkToPool();
        ValidateTestUtils.runAndAssertValidateFailure(cmd, EngineMessage.ACTION_TYPE_FAILED_CANNOT_DECREASE_DATA_CENTER_COMPATIBILITY_VERSION);
    }

    @Test
    public void lowerVersionMgmtNetworkAndRegularNetworks() {
        cmd.getStoragePool().setCompatibilityVersion(VERSION_1_0);
        addNonDefaultClusterToPool();
        addManagementNetworkToPool();
        addNonManagementNetworksToPool(2);
        setupNetworkValidator(true);
        ValidateTestUtils.runAndAssertValidateFailure(cmd, EngineMessage.ACTION_TYPE_FAILED_CANNOT_DECREASE_DATA_CENTER_COMPATIBILITY_VERSION);
    }

    @Test
    public void lowerVersionHostsAndNetwork() {
        cmd.getStoragePool().setCompatibilityVersion(VERSION_1_0);
        addNonDefaultClusterToPool();
        addHostsToCluster();
        addNonManagementNetworkToPool();
        ValidateTestUtils.runAndAssertValidateFailure(cmd, EngineMessage.ACTION_TYPE_FAILED_CANNOT_DECREASE_DATA_CENTER_COMPATIBILITY_VERSION);
    }

    @Test
    public void lowerVersionMgmtNetworkSupportedFeatures() {
        cmd.getStoragePool().setCompatibilityVersion(VERSION_1_0);
        addNonDefaultClusterToPool();
        addManagementNetworksToPool(2);
        setupNetworkValidator(true);
        ValidateTestUtils.runAndAssertValidateSuccess(cmd);
    }

    @Test
    public void lowerVersionMgmtNetworkNonSupportedFeatures() {
        cmd.getStoragePool().setCompatibilityVersion(VERSION_1_0);
        addNonDefaultClusterToPool();
        addManagementNetworksToPool(2);
        setupNetworkValidator(false);
        ValidateTestUtils.runAndAssertValidateFailure(cmd, EngineMessage.ACTION_TYPE_FAILED_CANNOT_DECREASE_DATA_CENTER_COMPATIBILITY_VERSION);
    }

    @Test
    public void versionHigherThanCluster() {
        cmd.getStoragePool().setCompatibilityVersion(VERSION_1_2);
        ValidateTestUtils.runAndAssertValidateFailure(cmd, EngineMessage.ERROR_CANNOT_UPDATE_STORAGE_POOL_COMPATIBILITY_VERSION_BIGGER_THAN_CLUSTERS);
    }

    @Test
    public void testValidateAllClustersLevel() {
        cmd.getStoragePool().setCompatibilityVersion(VERSION_1_2);
        List<Cluster> clusterList = createClusterList();
        // Create new supported cluster.
        Cluster secondCluster = new Cluster();
        secondCluster.setCompatibilityVersion(VERSION_1_2);
        secondCluster.setName("secondCluster");
        clusterList.add(secondCluster);

        // Create new unsupported cluster.
        Cluster thirdCluster = new Cluster();
        thirdCluster.setCompatibilityVersion(VERSION_1_1);
        thirdCluster.setName("thirdCluster");
        clusterList.add(thirdCluster);

        // Test upgrade
        when(clusterDao.getAllForStoragePool(any())).thenReturn(clusterList);
        assertFalse(cmd.checkAllClustersLevel());
        List<String> messages = cmd.getReturnValue().getValidationMessages();
        assertTrue(messages.contains(EngineMessage.ERROR_CANNOT_UPDATE_STORAGE_POOL_COMPATIBILITY_VERSION_BIGGER_THAN_CLUSTERS.toString()));
        assertTrue(messages.get(0).contains("firstCluster"));
        assertFalse(messages.get(0).contains("secondCluster"));
        assertTrue(messages.get(0).contains("thirdCluster"));
    }

    @Test
    public void poolHasDefaultCluster() {
        mcr.mockConfigValue(ConfigValues.AutoRegistrationDefaultClusterID, DEFAULT_CLUSTER_ID);
        addDefaultClusterToPool();
        doReturn(new ValidationResult
                (EngineMessage.ACTION_TYPE_FAILED_STORAGE_POOL_WITH_DEFAULT_CLUSTER_CANNOT_BE_LOCALFS))
                .when(poolValidator).isNotLocalfsWithDefaultCluster();
        ValidateTestUtils.runAndAssertValidateFailure(cmd, EngineMessage.ACTION_TYPE_FAILED_STORAGE_POOL_WITH_DEFAULT_CLUSTER_CANNOT_BE_LOCALFS);
    }

    @Test
    public void syncLunsForStorageDomainsNoActiveDomain() {
        List<StorageDomain> storageDomains = Arrays.asList(
                createStorageDomainForSyncLuns(StorageType.ISCSI, StorageDomainStatus.Maintenance, false),
                createStorageDomainForSyncLuns(StorageType.ISCSI, StorageDomainStatus.Locked, false));
        assertTrue(cmd.syncLunsForStorageDomains(storageDomains).isEmpty());
    }

    @Test
    public void syncLunsForStorageDomainsNoBlockDomain() {
        List<StorageDomain> storageDomains = Arrays.asList(
                createStorageDomainForSyncLuns(StorageType.NFS, StorageDomainStatus.Active, false),
                createStorageDomainForSyncLuns(StorageType.GLANCE, StorageDomainStatus.Active, false));
        assertTrue(cmd.syncLunsForStorageDomains(storageDomains).isEmpty());
    }

    @Test
    public void syncLunsForStorageDomainsNoDomain() {
        assertTrue(cmd.syncLunsForStorageDomains(Collections.emptyList()).isEmpty());
    }

    @Test
    public void syncLunsForStorageDomainsWithFailedToSyncDomains() {
        StorageDomain failedToSyncStorageDomain1 =
                createStorageDomainForSyncLuns(StorageType.ISCSI, StorageDomainStatus.Active, false);
        StorageDomain failedToSyncStorageDomain2 =
                createStorageDomainForSyncLuns(StorageType.FCP, StorageDomainStatus.Active, false);
        List<StorageDomain> storageDomains = Arrays.asList(failedToSyncStorageDomain1, failedToSyncStorageDomain2,
                createStorageDomainForSyncLuns(StorageType.ISCSI, StorageDomainStatus.Active, true));
        assertTrue(isEqualCollection(cmd.syncLunsForStorageDomains(storageDomains),
                Arrays.asList(failedToSyncStorageDomain1.getId(), failedToSyncStorageDomain2.getId())));
    }

    @Test
    public void syncLunsForStorageDomainsAllActiveBlockDomainsWereSynced() {
        List<StorageDomain> storageDomains = Arrays.asList(
                createStorageDomainForSyncLuns(StorageType.ISCSI, StorageDomainStatus.Active, true),
                createStorageDomainForSyncLuns(StorageType.FCP, StorageDomainStatus.Locked, false),
                createStorageDomainForSyncLuns(StorageType.FCP, StorageDomainStatus.Active, true));
        assertTrue(cmd.syncLunsForStorageDomains(storageDomains).isEmpty());
    }

    private StorageDomain createStorageDomainForSyncLuns(StorageType storageType, StorageDomainStatus status,
            boolean canSync) {
        StorageDomain domain = new StorageDomain();
        domain.setId(Guid.newGuid());
        domain.setStorageType(storageType);
        domain.setStatus(status);
        doReturn(new Pair<>(canSync, domain.getId())).when(cmd).syncDomainLuns(eq(domain));
        return domain;
    }

    private void newPoolNameIsAlreadyTaken() {
        when(spDao.get(any())).thenReturn(new StoragePool());
        List<StoragePool> storagePoolList = new ArrayList<>();
        storagePoolList.add(createStoragePool());
        when(spDao.getByName(anyString(), anyBoolean())).thenReturn(new ArrayList<>(storagePoolList));
    }

    private static StoragePool createStoragePool() {
        StoragePool pool = new StoragePool();
        pool.setId(Guid.newGuid());
        pool.setName("Default");
        pool.setCompatibilityVersion(VERSION_1_1);
        return pool;
    }

    private static List<Cluster> createClusterList() {
        List<Cluster> clusters = new ArrayList<>();
        Cluster cluster = new Cluster();
        cluster.setCompatibilityVersion(VERSION_1_0);
        cluster.setName("firstCluster");
        clusters.add(cluster);
        return clusters;
    }

    private void addDefaultClusterToPool() {
        Cluster defaultCluster = new Cluster();
        defaultCluster.setCompatibilityVersion(VERSION_1_1);
        defaultCluster.setId(DEFAULT_CLUSTER_ID);
        List<Cluster> clusters = new ArrayList<>();
        clusters.add(defaultCluster);
        when(clusterDao.getAllForStoragePool(any())).thenReturn(clusters);
    }

    private void addNonDefaultClusterToPool() {
        Cluster defaultCluster = new Cluster();
        defaultCluster.setCompatibilityVersion(VERSION_1_1);
        defaultCluster.setId(NON_DEFAULT_CLUSTER_ID);
        List<Cluster> clusters = new ArrayList<>();
        clusters.add(defaultCluster);
        when(clusterDao.getAllForStoragePool(any())).thenReturn(clusters);
    }

    private void addHostsToCluster() {
        VDS host = new VDS();
        List<VDS> hosts = new ArrayList<>();
        hosts.add(host);
        when(vdsDao.getAllForStoragePool(any())).thenReturn(hosts);
    }

    private void addManagementNetworkToPool() {
        addManagementNetworksToPool(1);
    }

    private void addManagementNetworksToPool(int numberOfNetworks) {
        addNetworksToPool(numberOfNetworks, true);
    }

    private Network createNetwork(Guid networkId) {
        Network network = new Network();
        network.setId(networkId);
        return network;
    }

    private void setupNetworkValidator(boolean valid) {
        NetworkValidator validator = mock(NetworkValidator.class);
        when(validator.canNetworkCompatibilityBeDecreased()).thenReturn(valid);
        when(cmd.getNetworkValidator(any())).thenReturn(validator);
    }

    private void addNonManagementNetworkToPool() {
        addNonManagementNetworksToPool(1);
    }

    private void addNonManagementNetworksToPool(int numberOfNetworks) {
        addNetworksToPool(numberOfNetworks, false);
    }

    private void addNetworksToPool(int numberOfNetworks, boolean isManagement) {
        List<Network> allDcNetworks = new ArrayList<>();
        for (int i = 0; i < numberOfNetworks; i++) {
            final Guid networkId = Guid.newGuid();
            Network network = createNetwork(networkId);
            network.setId(networkId);
            allDcNetworks.add(network);
            when(managementNetworkUtil.isManagementNetwork(networkId)).thenReturn(isManagement);
        }
        when(networkDao.getAllForDataCenter(any())).thenReturn(allDcNetworks);
    }
}

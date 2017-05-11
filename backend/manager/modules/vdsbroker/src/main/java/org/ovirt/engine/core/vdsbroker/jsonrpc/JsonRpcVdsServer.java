package org.ovirt.engine.core.vdsbroker.jsonrpc;

import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.apache.commons.httpclient.HttpClient;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.utils.threadpool.ThreadPoolUtil;
import org.ovirt.engine.core.vdsbroker.HttpUtils;
import org.ovirt.engine.core.vdsbroker.gluster.GlusterHookContentInfoReturn;
import org.ovirt.engine.core.vdsbroker.gluster.GlusterHooksListReturn;
import org.ovirt.engine.core.vdsbroker.gluster.GlusterHostsPubKeyReturn;
import org.ovirt.engine.core.vdsbroker.gluster.GlusterServersListReturn;
import org.ovirt.engine.core.vdsbroker.gluster.GlusterServicesReturn;
import org.ovirt.engine.core.vdsbroker.gluster.GlusterTaskInfoReturn;
import org.ovirt.engine.core.vdsbroker.gluster.GlusterTasksListReturn;
import org.ovirt.engine.core.vdsbroker.gluster.GlusterVolumeGeoRepConfigList;
import org.ovirt.engine.core.vdsbroker.gluster.GlusterVolumeGeoRepStatus;
import org.ovirt.engine.core.vdsbroker.gluster.GlusterVolumeGeoRepStatusDetail;
import org.ovirt.engine.core.vdsbroker.gluster.GlusterVolumeOptionsInfoReturn;
import org.ovirt.engine.core.vdsbroker.gluster.GlusterVolumeProfileInfoReturn;
import org.ovirt.engine.core.vdsbroker.gluster.GlusterVolumeSnapshotConfigReturn;
import org.ovirt.engine.core.vdsbroker.gluster.GlusterVolumeSnapshotCreateReturn;
import org.ovirt.engine.core.vdsbroker.gluster.GlusterVolumeSnapshotInfoReturn;
import org.ovirt.engine.core.vdsbroker.gluster.GlusterVolumeStatusReturn;
import org.ovirt.engine.core.vdsbroker.gluster.GlusterVolumeTaskReturn;
import org.ovirt.engine.core.vdsbroker.gluster.GlusterVolumesHealInfoReturn;
import org.ovirt.engine.core.vdsbroker.gluster.GlusterVolumesListReturn;
import org.ovirt.engine.core.vdsbroker.gluster.OneStorageDeviceReturn;
import org.ovirt.engine.core.vdsbroker.gluster.StorageDeviceListReturn;
import org.ovirt.engine.core.vdsbroker.irsbroker.OneUuidReturn;
import org.ovirt.engine.core.vdsbroker.irsbroker.StatusReturn;
import org.ovirt.engine.core.vdsbroker.irsbroker.StoragePoolInfo;
import org.ovirt.engine.core.vdsbroker.vdsbroker.AlignmentScanReturn;
import org.ovirt.engine.core.vdsbroker.vdsbroker.BooleanReturn;
import org.ovirt.engine.core.vdsbroker.vdsbroker.DevicesVisibilityMapReturn;
import org.ovirt.engine.core.vdsbroker.vdsbroker.FenceStatusReturn;
import org.ovirt.engine.core.vdsbroker.vdsbroker.HostDevListReturn;
import org.ovirt.engine.core.vdsbroker.vdsbroker.HostJobsReturn;
import org.ovirt.engine.core.vdsbroker.vdsbroker.IQNListReturn;
import org.ovirt.engine.core.vdsbroker.vdsbroker.IVdsServer;
import org.ovirt.engine.core.vdsbroker.vdsbroker.ImageSizeReturn;
import org.ovirt.engine.core.vdsbroker.vdsbroker.LUNListReturn;
import org.ovirt.engine.core.vdsbroker.vdsbroker.MigrateStatusReturn;
import org.ovirt.engine.core.vdsbroker.vdsbroker.OneMapReturn;
import org.ovirt.engine.core.vdsbroker.vdsbroker.OneStorageDomainInfoReturn;
import org.ovirt.engine.core.vdsbroker.vdsbroker.OneStorageDomainStatsReturn;
import org.ovirt.engine.core.vdsbroker.vdsbroker.OneVGReturn;
import org.ovirt.engine.core.vdsbroker.vdsbroker.OneVmReturn;
import org.ovirt.engine.core.vdsbroker.vdsbroker.OvfReturn;
import org.ovirt.engine.core.vdsbroker.vdsbroker.PrepareImageReturn;
import org.ovirt.engine.core.vdsbroker.vdsbroker.QemuImageInfoReturn;
import org.ovirt.engine.core.vdsbroker.vdsbroker.ServerConnectionStatusReturn;
import org.ovirt.engine.core.vdsbroker.vdsbroker.SpmStatusReturn;
import org.ovirt.engine.core.vdsbroker.vdsbroker.StatusOnlyReturn;
import org.ovirt.engine.core.vdsbroker.vdsbroker.StorageDomainListReturn;
import org.ovirt.engine.core.vdsbroker.vdsbroker.TaskInfoListReturn;
import org.ovirt.engine.core.vdsbroker.vdsbroker.TaskStatusListReturn;
import org.ovirt.engine.core.vdsbroker.vdsbroker.TaskStatusReturn;
import org.ovirt.engine.core.vdsbroker.vdsbroker.VDSInfoReturn;
import org.ovirt.engine.core.vdsbroker.vdsbroker.VMInfoListReturn;
import org.ovirt.engine.core.vdsbroker.vdsbroker.VMListReturn;
import org.ovirt.engine.core.vdsbroker.vdsbroker.VMNamesListReturn;
import org.ovirt.engine.core.vdsbroker.vdsbroker.VdsProperties;
import org.ovirt.engine.core.vdsbroker.vdsbroker.VolumeInfoReturn;
import org.ovirt.vdsm.jsonrpc.client.ClientConnectionException;
import org.ovirt.vdsm.jsonrpc.client.JsonRpcClient;
import org.ovirt.vdsm.jsonrpc.client.JsonRpcRequest;
import org.ovirt.vdsm.jsonrpc.client.RequestBuilder;
import org.ovirt.vdsm.jsonrpc.client.internal.ClientPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of <code>IVdsServer</code> interface which provides JSONRPC by
 * using {@link JsonRpcClient}.
 * Each method uses {@link RequestBuilder} to build request object and sends it
 * using client. The response is represented as {@link FutureMap} which is lazy
 * evaluated.
 *
 */
public class JsonRpcVdsServer implements IVdsServer {

    private static final Logger logger = LoggerFactory.getLogger(JsonRpcVdsServer.class);
    private final JsonRpcClient client;
    private final HttpClient httpClient;

    public JsonRpcVdsServer(JsonRpcClient client, HttpClient httpClient) {
        this.client = client;
        this.httpClient = httpClient;
    }

    @Override
    public void close() {
        HttpUtils.shutDownConnection(this.httpClient);
        this.client.close();
    }

    @Override
    public List<Certificate> getPeerCertificates() {
        try {
            return client.getClient().getPeerCertificates();
        } catch (ClientConnectionException | IllegalStateException e) {
            logger.error("Failed to get peer certification for host '{}': {}", client.getHostname(), e.getMessage());
            logger.debug("Exception", e);
            return null;
        }
    }

    @Override
    public HttpClient getHttpClient() {
        return this.httpClient;
    }

    @SuppressWarnings("rawtypes")
    private String getVmId(Map map) {
        return (String) map.get(VdsProperties.vm_guid);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public OneVmReturn create(Map createInfo) {
        JsonRpcRequest request =
                new RequestBuilder("VM.create").withParameter("vmID", getVmId(createInfo))
                        .withParameter("vmParams", createInfo)
                        .build();
        Map<String, Object> response =
                new FutureMap(this.client, request).withResponseKey("vmList");
        return new OneVmReturn(response);
    }

    @Override
    public StatusOnlyReturn allocateVolume(String spUUID, String sdUUID, String imgGUID, String volUUID, String size) {
        JsonRpcRequest request =
                new RequestBuilder("Volume.allocate").withParameter("volumeID", volUUID)
                        .withParameter("storagepoolID", spUUID)
                        .withParameter("storagedomainID", sdUUID)
                        .withParameter("imageID", imgGUID)
                        .withParameter("size", size)
                        .build();
        Map<String, Object> response =
                new FutureMap(this.client, request).withResponseKey("uuid");
        return new StatusOnlyReturn(response);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public StatusOnlyReturn copyData(String jobId, Map src, Map dst) {
        JsonRpcRequest request =
                new RequestBuilder("SDM.copy_data")
                        .withParameter("source", src)
                        .withParameter("destination", dst)
                        .withParameter("job_id", jobId)
                        .build();
        Map<String, Object> response = new FutureMap(this.client, request);
        return new StatusOnlyReturn(response);
    }

    @Override
    public StatusOnlyReturn updateVolume(String jobId, Map<?, ?> volumeInfo, Map<?, ?> volumeAttributes) {
        JsonRpcRequest request =
                new RequestBuilder("SDM.update_volume")
                        .withParameter("job_id", jobId)
                        .withParameter("vol_info", volumeInfo)
                        .withParameter("vol_attr", volumeAttributes)
                        .build();
        Map<String, Object> response = new FutureMap(this.client, request);
        return new StatusOnlyReturn(response);
    }

    @Override
    public StatusOnlyReturn moveDomainDevice(String jobId, Map<String, Object> moveParams) {
        JsonRpcRequest request =
                new RequestBuilder("SDM.move_domain_device")
                        .withParameter("job_id", jobId)
                        .withParameter("move_params", moveParams)
                        .build();
        Map<String, Object> response = new FutureMap(this.client, request);
        return new StatusOnlyReturn(response);
    }


    @Override
    public StatusOnlyReturn reduceDomain(String jobId, Map<String, Object> reduceParams) {
        JsonRpcRequest request =
                new RequestBuilder("SDM.reduce_domain")
                        .withParameter("job_id", jobId)
                        .withParameter("reduce_params", reduceParams)
                        .build();
        Map<String, Object> response = new FutureMap(this.client, request);
        return new StatusOnlyReturn(response);
    }

    @Override
    public StatusOnlyReturn createVolumeContainer(String jobId, Map<String, Object> createVolumeInfo) {
        JsonRpcRequest request =
                new RequestBuilder("SDM.create_volume")
                        .withParameter("job_id", jobId)
                        .withParameter("vol_info", createVolumeInfo)
                        .build();
        Map<String, Object> response = new FutureMap(this.client, request);
        return new StatusOnlyReturn(response);
    }

    @Override
    public StatusOnlyReturn mergeSubchain(String jobId, Map<String, Object> subchainInfo) {
        JsonRpcRequest request =
                new RequestBuilder("SDM.merge")
                        .withParameter("job_id", jobId)
                        .withParameter("subchain_info", subchainInfo)
                        .build();
        Map<String, Object> response = new FutureMap(this.client, request);
        return new StatusOnlyReturn(response);
    }

    @Override
    public StatusOnlyReturn destroy(String vmId) {
        JsonRpcRequest request = new RequestBuilder("VM.destroy").withParameter("vmID", vmId).build();
        Map<String, Object> response = new FutureMap(this.client, request);
        return new StatusOnlyReturn(response);
    }

    @Override
    public StatusOnlyReturn shutdown(String vmId, String timeout, String message) {
        JsonRpcRequest request =
                new RequestBuilder("VM.shutdown").withParameter("vmID", vmId)
                        .withOptionalParameter("delay", timeout)
                        .withOptionalParameter("message", message)
                        .build();
        Map<String, Object> response = new FutureMap(this.client, request);
        return new StatusOnlyReturn(response);
    }

    @Override
    public StatusOnlyReturn shutdown(String vmId, String timeout, String message, boolean reboot) {
        JsonRpcRequest request =
                new RequestBuilder("VM.shutdown").withParameter("vmID", vmId)
                        .withOptionalParameter("delay", timeout)
                        .withOptionalParameter("message", message)
                        .withParameter("reboot", reboot)
                        .build();
        Map<String, Object> response = new FutureMap(this.client, request);
        return new StatusOnlyReturn(response);
    }

    @Override
    public OneVmReturn pause(String vmId) {
        JsonRpcRequest request = new RequestBuilder("VM.pause").withParameter("vmID", vmId).build();
        Map<String, Object> response =
                new FutureMap(this.client, request).withResponseKey("vmList");
        return new OneVmReturn(response);
    }

    @Override
    public StatusOnlyReturn hibernate(String vmId, String hiberVolHandle) {
        JsonRpcRequest request =
                new RequestBuilder("VM.hibernate").withParameter("vmID", vmId)
                        .withParameter("hibernationVolHandle", hiberVolHandle)
                        .build();
        Map<String, Object> response = new FutureMap(this.client, request);
        return new StatusOnlyReturn(response);
    }

    @Override
    public OneVmReturn resume(String vmId) {
        JsonRpcRequest request = new RequestBuilder("VM.cont").withParameter("vmID", vmId).build();
        Map<String, Object> response =
                new FutureMap(this.client, request).withResponseKey("vmList");
        return new OneVmReturn(response);
    }

    @Override
    public VMListReturn list() {
        JsonRpcRequest request =
                new RequestBuilder("Host.getVMList").withOptionalParameterAsList("vmList",
                        new ArrayList<>(Arrays.asList(new String[]{}))).withParameter("onlyUUID", false).build();
        Map<String, Object> response =
                new FutureMap(this.client, request).withResponseKey("vmList")
                        .withResponseType(Object[].class);
        return new VMListReturn(response);
    }

    @Override
    public VMListReturn fullList(List<String> vmIds) {
        JsonRpcRequest request =
                new RequestBuilder("Host.getVMFullList").withOptionalParameterAsList("vmList", vmIds).build();
        Map<String, Object> response =
                new FutureMap(this.client, request).withResponseKey("vmList")
                        .withResponseType(Object[].class);
        return new VMListReturn(response);
    }

    @Override
    public VDSInfoReturn getCapabilities() {
        JsonRpcRequest request = new RequestBuilder("Host.getCapabilities").build();
        Map<String, Object> response =
                new FutureMap(this.client, request).withResponseKey("info");
        return new VDSInfoReturn(response);
    }

    @Override
    public VDSInfoReturn getHardwareInfo() {
        JsonRpcRequest request = new RequestBuilder("Host.getHardwareInfo").build();
        Map<String, Object> response =
                new FutureMap(this.client, request).withResponseKey("info");
        return new VDSInfoReturn(response);
    }

    @Override
    public VDSInfoReturn getVdsStats() {
        JsonRpcRequest request = new RequestBuilder("Host.getStats").build();
        Map<String, Object> response =
                new FutureMap(this.client, request).withResponseKey("info");
        return new VDSInfoReturn(response);
    }

    @Override
    public StatusOnlyReturn setMOMPolicyParameters(Map<String, Object> values) {
        JsonRpcRequest request =
                new RequestBuilder("Host.setMOMPolicyParameters").withParameter("key_value_store", values).build();
        Map<String, Object> response = new FutureMap(this.client, request);
        return new StatusOnlyReturn(response);
    }

    @Override
    public StatusOnlyReturn desktopLogin(String vmId, String domain, String user, String password) {
        JsonRpcRequest request =
                new RequestBuilder("VM.desktopLogin").withParameter("vmID", vmId)
                        .withParameter("domain", domain)
                        .withParameter("username", user)
                        .withParameter("password", password)
                        .build();
        Map<String, Object> response = new FutureMap(this.client, request);
        return new StatusOnlyReturn(response);
    }

    @Override
    public StatusOnlyReturn desktopLogoff(String vmId, String force) {
        JsonRpcRequest request =
                new RequestBuilder("VM.desktopLogoff").withParameter("vmID", vmId)
                        .withParameter("force", force)
                        .build();
        Map<String, Object> response = new FutureMap(this.client, request);
        return new StatusOnlyReturn(response);
    }

    @Override
    public VMInfoListReturn getVmStats(String vmId) {
        JsonRpcRequest request = new RequestBuilder("VM.getStats").withParameter("vmID", vmId).build();
        Map<String, Object> response =
                new FutureMap(this.client, request).withResponseKey("statsList");
        return new VMInfoListReturn(response);
    }

    @Override
    public VMInfoListReturn getAllVmStats() {
        JsonRpcRequest request = new RequestBuilder("Host.getAllVmStats").build();
        Map<String, Object> response =
                new FutureMap(this.client, request).withResponseKey("statsList")
                        .withResponseType(Object[].class);
        return new VMInfoListReturn(response);
    }

    @Override
    public HostDevListReturn hostDevListByCaps() {
        JsonRpcRequest request = new RequestBuilder("Host.hostdevListByCaps").build();
        Map<String, Object> response =
                new FutureMap(this.client, request).withResponseKey("deviceList");
        return new HostDevListReturn(response);
    }

    @Override
    public StatusOnlyReturn migrate(Map<String, Object> migrationInfo) {
        JsonRpcRequest request =
                new RequestBuilder("VM.migrate").withParameter("vmID", getVmId(migrationInfo))
                        .withParameter("params", migrationInfo)
                        .build();
        Map<String, Object> response = new FutureMap(this.client, request);
        return new StatusOnlyReturn(response);
    }

    @Override
    public MigrateStatusReturn migrateStatus(String vmId) {
        JsonRpcRequest request = new RequestBuilder("VM.getMigrationStatus").withParameter("vmID", vmId).build();
        Map<String, Object> response = new FutureMap(this.client, request).withResponseKey("response")
                .withResponseType(Long.class);
        return new MigrateStatusReturn(response);
    }

    @Override
    public StatusOnlyReturn migrateCancel(String vmId) {
        JsonRpcRequest request = new RequestBuilder("VM.migrateCancel").withParameter("vmID", vmId).build();
        Map<String, Object> response = new FutureMap(this.client, request);
        return new StatusOnlyReturn(response);
    }

    @Override
    public OneVmReturn changeDisk(String vmId, String imageLocation) {
        JsonRpcRequest request = new RequestBuilder("VM.changeCD").withParameter("vmID", vmId)
                .withParameter("driveSpec", imageLocation)
                .build();
        Map<String, Object> response =
                new FutureMap(this.client, request).withResponseKey("vmList");
        return new OneVmReturn(response);
    }

    @Override
    public OneVmReturn changeDisk(String vmId, Map<String, Object> driveSpec) {
        JsonRpcRequest request = new RequestBuilder("VM.changeCD").withParameter("vmID", vmId)
                .withParameter("driveSpec", driveSpec)
                .build();
        Map<String, Object> response =
                new FutureMap(this.client, request).withResponseKey("vmList");
        return new OneVmReturn(response);
    }

    @Override
    public StatusOnlyReturn addNetwork(String bridge,
            String vlan,
            String bond,
            String[] nics,
            Map<String, String> options) {
        JsonRpcRequest request =
                new RequestBuilder("Host.addNetwork").withParameter("bridge", bridge)
                        .withOptionalParameter("vlan", vlan)
                        .withOptionalParameter("bond", bond)
                        .withOptionalParameterAsList("nics", new ArrayList<>(Arrays.asList(nics)))
                        .withOptionalParameterAsMap("options", options)
                        .build();
        Map<String, Object> response = new FutureMap(this.client, request);
        return new StatusOnlyReturn(response);
    }

    @Override
    public StatusOnlyReturn delNetwork(String bridge, String vlan, String bond, String[] nics) {
        // No options params (do we need it during this operation)
        JsonRpcRequest request = new RequestBuilder("Host.delNetwork").withParameter("bridge", bridge)
                .withOptionalParameter("vlan", vlan)
                .withOptionalParameter("bond", bond)
                .withOptionalParameterAsList("nics", new ArrayList<>(Arrays.asList(nics)))
                .build();
        Map<String, Object> response = new FutureMap(this.client, request);
        return new StatusOnlyReturn(response);
    }

    @Override
    public StatusOnlyReturn editNetwork(String oldBridge,
            String newBridge,
            String vlan,
            String bond,
            String[] nics,
            Map<String, String> options) {
        JsonRpcRequest request =
                new RequestBuilder("Host.editNetwork").withParameter("oldBridge", oldBridge)
                        .withParameter("newBridge", newBridge)
                        .withOptionalParameter("vlan", vlan)
                        .withOptionalParameter("bond", bond)
                        .withOptionalParameterAsList("nics", new ArrayList<>(Arrays.asList(nics)))
                        .withOptionalParameterAsMap("options", options)
                        .build();
        Map<String, Object> response = new FutureMap(this.client, request);
        return new StatusOnlyReturn(response);
    }

    static class FutureCallable implements Callable<Map<String, Object>> {
        private final Callable<Map<String, Object>> callable;

        private FutureMap map;

        public FutureCallable(Callable<Map<String, Object>> callable) {
            this.callable = callable;
        }

        @Override
        public Map<String, Object> call() throws Exception {
            this.map = (FutureMap) this.callable.call();
            return this.map;
        }

        public boolean isDone() {
            if (this.map == null) {
                return false;
            }
            return this.map.isDone();
        }
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Future<Map<String, Object>> setupNetworks(Map networks, Map bonding, Map options, final boolean isPolicyReset) {
        final JsonRpcRequest request =
                new RequestBuilder("Host.setupNetworks").withParameter("networks", networks)
                        .withParameter("bondings", bonding)
                        .withParameter("options", options)
                        .build();
        final FutureCallable callable = new FutureCallable(() -> {
            if (isPolicyReset) {
                updateHeartbeatPolicy(client.getClientRetryPolicy().clone(), false);
            }
            return new FutureMap(client, request).withResponseKey("status");
        });
        FutureTask<Map<String, Object>> future = new FutureTask<Map<String, Object>>(callable) {

                    @Override
                    public boolean isDone() {
                        if (callable.isDone()) {
                            if (isPolicyReset) {
                                updateHeartbeatPolicy(client.getClientRetryPolicy(), true);
                            }
                            return true;
                        }
                        return false;
                    }
                };
        ThreadPoolUtil.execute(future);
        return future;
    }

    private void updateHeartbeatPolicy(ClientPolicy policy, boolean isHeartbeat) {
        policy.setIncomingHeartbeat(isHeartbeat);
        policy.setOutgoingHeartbeat(isHeartbeat);
        client.setClientRetryPolicy(policy);
    }

    @Override
    public StatusOnlyReturn setSafeNetworkConfig() {
        JsonRpcRequest request = new RequestBuilder("Host.setSafeNetworkConfig").build();
        Map<String, Object> response = new FutureMap(this.client, request);
        return new StatusOnlyReturn(response);
    }

    @Override
    public FenceStatusReturn fenceNode(String ip,
            String port,
            String type,
            String user,
            String password,
            String action,
            String secured,
            String options,
            Map<String, Object> fencingPolicy) {
        JsonRpcRequest request =
                new RequestBuilder("Host.fenceNode").withParameter("addr", ip)
                        .withParameter("port", port)
                        .withParameter("agent", type)
                        .withParameter("username", user)
                        .withParameter("password", password)
                        .withParameter("action", action)
                        .withOptionalParameter("secure", secured)
                        .withOptionalParameter("options", options)
                        .withOptionalParameterAsMap("policy", fencingPolicy)
                        .build();
        Map<String, Object> response =
                new FutureMap(this.client, request).withIgnoreResponseKey();

        return new FenceStatusReturn(response);
    }

    @Override
    public ServerConnectionStatusReturn connectStorageServer(int serverType,
            String spUUID,
            Map<String, String>[] args) {
        JsonRpcRequest request =
                new RequestBuilder("StoragePool.connectStorageServer").withParameter("storagepoolID", spUUID)
                        .withParameter("domainType", serverType)
                        .withParameter("connectionParams", args)
                        .build();
        Map<String, Object> response =
                new FutureMap(this.client, request).withResponseKey("statuslist")
                        .withResponseType(Object[].class);
        return new ServerConnectionStatusReturn(response);
    }

    @Override
    public ServerConnectionStatusReturn disconnectStorageServer(int serverType,
            String spUUID,
            Map<String, String>[] args) {
        JsonRpcRequest request =
                new RequestBuilder("StoragePool.disconnectStorageServer").withParameter("storagepoolID", spUUID)
                        .withParameter("domainType", serverType)
                        .withParameter("connectionParams", args)
                        .build();
        Map<String, Object> response =
                new FutureMap(this.client, request).withResponseKey("statuslist")
                        .withResponseType(Object[].class);
        return new ServerConnectionStatusReturn(response);
    }

    @Override
    public StatusOnlyReturn createStorageDomain(int domainType,
            String sdUUID,
            String domainName,
            String arg,
            int storageType,
            String storageFormatType) {
        JsonRpcRequest request =
                new RequestBuilder("StorageDomain.create").withParameter("storagedomainID", sdUUID)
                        .withParameter("domainType", domainType)
                        .withParameter("typeArgs", arg)
                        .withParameter("name", domainName)
                        .withParameter("domainClass", storageType)
                        .withOptionalParameter("version", storageFormatType)
                        .build();
        Map<String, Object> response = new FutureMap(this.client, request);
        return new StatusOnlyReturn(response);
    }

    @Override
    public StatusOnlyReturn formatStorageDomain(String sdUUID) {
        JsonRpcRequest request =
                new RequestBuilder("StorageDomain.format").withParameter("storagedomainID", sdUUID)
                        .withParameter("autoDetach", false)
                        .build();
        Map<String, Object> response = new FutureMap(this.client, request);
        return new StatusOnlyReturn(response);
    }

    @Override
    public StatusOnlyReturn connectStoragePool(String spUUID,
            int hostSpmId,
            String SCSIKey,
            String masterdomainId,
            int masterVersion,
            Map<String, String> storageDomains) {
        JsonRpcRequest request =
                new RequestBuilder("StoragePool.connect").withParameter("storagepoolID", spUUID)
                        .withParameter("hostID", hostSpmId)
                        .withParameter("scsiKey", SCSIKey)
                        .withParameter("masterSdUUID", masterdomainId)
                        .withParameter("masterVersion", masterVersion)
                        .withOptionalParameterAsMap("domainDict", storageDomains)
                        .build();
        Map<String, Object> response = new FutureMap(this.client, request);
        return new StatusOnlyReturn(response);
    }

    @Override
    public StatusOnlyReturn disconnectStoragePool(String spUUID, int hostSpmId, String SCSIKey) {
        JsonRpcRequest request =
                new RequestBuilder("StoragePool.disconnect").withParameter("storagepoolID", spUUID)
                        .withParameter("hostID", hostSpmId)
                        .withParameter("scsiKey", SCSIKey)
                        .build();
        Map<String, Object> response = new FutureMap(this.client, request);
        return new StatusOnlyReturn(response);
    }

    @Override
    public StatusOnlyReturn createStoragePool(int poolType,
            String spUUID,
            String poolName,
            String msdUUID,
            String[] domList,
            int masterVersion,
            String lockPolicy,
            int lockRenewalIntervalSec,
            int leaseTimeSec,
            int ioOpTimeoutSec,
            int leaseRetries) {
        // poolType and lockPolicy not used in vdsm. We can remove from the interface
        JsonRpcRequest request =
                new RequestBuilder("StoragePool.create")
                        .withParameter("storagepoolID", spUUID)
                        .withParameter("name", poolName)
                        .withParameter("masterSdUUID", msdUUID)
                        .withParameter("masterVersion", masterVersion)
                        .withParameter("domainList", new ArrayList<>(Arrays.asList(domList)))
                        .withParameter("lockRenewalIntervalSec", lockRenewalIntervalSec)
                        .withParameter("leaseTimeSec", leaseTimeSec)
                        .withParameter("ioOpTimeoutSec", ioOpTimeoutSec)
                        .withParameter("leaseRetries", leaseRetries)
                        .build();
        Map<String, Object> response = new FutureMap(this.client, request);
        return new StatusOnlyReturn(response);
    }

    @Override
    public StatusOnlyReturn reconstructMaster(String spUUID,
            String poolName,
            String masterDom,
            Map<String, String> domDict,
            int masterVersion,
            String lockPolicy,
            int lockRenewalIntervalSec,
            int leaseTimeSec,
            int ioOpTimeoutSec,
            int leaseRetries,
            int hostSpmId) {
        // no lockPolicy and hostSpmId not needed can be removed from the interface
        JsonRpcRequest request =
                new RequestBuilder("StoragePool.reconstructMaster").withParameter("storagepoolID", spUUID)
                        .withParameter("hostId", hostSpmId)
                        .withParameter("name", poolName)
                        .withParameter("masterSdUUID", masterDom)
                        .withParameter("masterVersion", masterVersion)
                        .withParameter("domainDict", domDict)
                        .withParameter("lockRenewalIntervalSec", lockRenewalIntervalSec)
                        .withParameter("leaseTimeSec", leaseTimeSec)
                        .withParameter("ioOpTimeoutSec", ioOpTimeoutSec)
                        .withParameter("leaseRetries", leaseRetries)
                        .build();
        Map<String, Object> response = new FutureMap(this.client, request);
        return new StatusOnlyReturn(response);
    }

    @Override
    public OneStorageDomainStatsReturn getStorageDomainStats(String sdUUID) {
        JsonRpcRequest request =
                new RequestBuilder("StorageDomain.getStats").withParameter("storagedomainID", sdUUID).build();
        Map<String, Object> response =
                new FutureMap(this.client, request).withResponseKey("stats");
        return new OneStorageDomainStatsReturn(response);
    }

    @Override
    public OneStorageDomainInfoReturn getStorageDomainInfo(String sdUUID) {
        JsonRpcRequest request =
                new RequestBuilder("StorageDomain.getInfo").withParameter("storagedomainID", sdUUID).build();
        Map<String, Object> response =
                new FutureMap(this.client, request).withResponseKey("info");
        return new OneStorageDomainInfoReturn(response);
    }

    @Override
    public StorageDomainListReturn getStorageDomainsList(String spUUID,
            int domainType,
            String poolType,
            String path) {
        JsonRpcRequest request =
                new RequestBuilder("Host.getStorageDomains").withParameter("storagepoolID", spUUID)
                        .withParameter("domainClass", domainType)
                        .withParameter("storageType", poolType)
                        .withParameter("remotePath", path)
                        .build();
        Map<String, Object> response =
                new FutureMap(this.client, request).withResponseKey("domlist")
                        .withResponseType(Object[].class);
        return new StorageDomainListReturn(response);
    }

    @Override
    public OneUuidReturn createVG(String sdUUID, String[] deviceList, boolean force) {
        JsonRpcRequest request =
                new RequestBuilder("LVMVolumeGroup.create").withParameter("name", sdUUID)
                        .withParameter("devlist", new ArrayList<>(Arrays.asList(deviceList)))
                        .withParameter("force", force)
                        .build();
        Map<String, Object> response =
                new FutureMap(this.client, request).withResponseKey("uuid");
        return new OneUuidReturn(response);
    }

    @Override
    public OneVGReturn getVGInfo(String vgUUID) {
        JsonRpcRequest request =
                new RequestBuilder("LVMVolumeGroup.getInfo").withParameter("lvmvolumegroupID", vgUUID).build();
        Map<String, Object> response =
                new FutureMap(this.client, request).withResponseKey("info");
        return new OneVGReturn(response);
    }

    @Override
    public LUNListReturn getDeviceList(int storageType, String[] devicesList, boolean checkStatus) {
        ArrayList<String> devicesListArray =
                devicesList != null ? new ArrayList<>(Arrays.asList(devicesList)) : null;
        JsonRpcRequest request =
                new RequestBuilder("Host.getDeviceList").withParameter("storageType",
                        storageType)
                        .withOptionalParameterAsList("guids", devicesListArray)
                        .withParameter("checkStatus", checkStatus)
                        .build();
        Map<String, Object> response =
                new FutureMap(this.client, request).withResponseType(Object[].class)
                        .withResponseKey("devList");
        return new LUNListReturn(response);
    }

    @Override
    public DevicesVisibilityMapReturn getDevicesVisibility(String[] devicesList) {
        JsonRpcRequest request =
                new RequestBuilder("Host.getDevicesVisibility").withParameter("guidList",
                        new ArrayList<>(Arrays.asList(devicesList))).build();
        Map<String, Object> response =
                new FutureMap(this.client, request).withResponseKey("visible");
        return new DevicesVisibilityMapReturn(response);
    }

    @Override
    public IQNListReturn discoverSendTargets(Map<String, String> args) {
        JsonRpcRequest request =
                new RequestBuilder("ISCSIConnection.discoverSendTargets").withParameter("host", args.get("connection"))
                        .withParameter("port", args.get("port"))
                        .withOptionalParameter("user", args.get("user"))
                        .withOptionalParameter("password", args.get("password"))
                        .build();
        Map<String, Object> response = new FutureMap(this.client, request).withResponseKey("fullTargets");
        return new IQNListReturn(response);
    }

    @Override
    public OneUuidReturn spmStart(String spUUID,
            int prevID,
            String prevLVER,
            int recoveryMode,
            String SCSIFencing,
            int maxHostId,
            String storagePoolFormatType) {
        // storagePoolFormatType not used and can be removed from the interface
        JsonRpcRequest request =
                new RequestBuilder("StoragePool.spmStart").withParameter("storagepoolID", spUUID)
                        .withParameter("prevID", prevID)
                        .withParameter("prevLver", prevLVER)
                        .withParameter("enableScsiFencing", SCSIFencing)
                        .withParameter("maxHostID", maxHostId)
                        .withOptionalParameter("domVersion", storagePoolFormatType)
                        .build();
        Map<String, Object> response =
                new FutureMap(this.client, request).withResponseKey("uuid")
                        .withResponseType(String.class);
        return new OneUuidReturn(response);
    }

    @Override
    public StatusOnlyReturn spmStop(String spUUID) {
        JsonRpcRequest request =
                new RequestBuilder("StoragePool.spmStop").withParameter("storagepoolID", spUUID).build();
        Map<String, Object> response = new FutureMap(this.client, request);
        return new StatusOnlyReturn(response);
    }

    @Override
    public SpmStatusReturn spmStatus(String spUUID) {
        JsonRpcRequest request =
                new RequestBuilder("StoragePool.getSpmStatus").withParameter("storagepoolID", spUUID).build();
        Map<String, Object> response =
                new FutureMap(this.client, request).withResponseKey("spm_st");
        return new SpmStatusReturn(response);
    }

    @Override
    public HostJobsReturn getHostJobs(String jobType, List<String> jobIds) {
        JsonRpcRequest request = new RequestBuilder("Host.getJobs").withOptionalParameter("job_type", jobType).
                withOptionalParameterAsList("job_ids", jobIds).build();
        Map<String, Object> response =
                new FutureMap(this.client, request).withResponseKey("jobs");
        return new HostJobsReturn(response);
    }

    @Override
    public TaskStatusReturn getTaskStatus(String taskUUID) {
        JsonRpcRequest request = new RequestBuilder("Task.getStatus").withParameter("taskID", taskUUID).build();
        Map<String, Object> response =
                new FutureMap(this.client, request).withResponseKey("taskStatus");
        return new TaskStatusReturn(response);
    }

    @Override
    public TaskStatusListReturn getAllTasksStatuses() {
        JsonRpcRequest request = new RequestBuilder("Host.getAllTasksStatuses").build();
        Map<String, Object> response =
                new FutureMap(this.client, request).withResponseKey("allTasksStatus");
        return new TaskStatusListReturn(response);
    }

    @Override
    public TaskInfoListReturn getAllTasksInfo() {
        JsonRpcRequest request = new RequestBuilder("Host.getAllTasksInfo").build();
        Map<String, Object> response =
                new FutureMap(this.client, request).withResponseKey("allTasksInfo");
        return new TaskInfoListReturn(response);
    }

    @Override
    public StatusOnlyReturn stopTask(String taskUUID) {
        JsonRpcRequest request = new RequestBuilder("Task.stop").withParameter("taskID", taskUUID).build();
        Map<String, Object> response = new FutureMap(this.client, request);
        return new StatusOnlyReturn(response);
    }

    @Override
    public StatusOnlyReturn clearTask(String taskUUID) {
        JsonRpcRequest request = new RequestBuilder("Task.clear").withParameter("taskID", taskUUID).build();
        Map<String, Object> response = new FutureMap(this.client, request);
        return new StatusOnlyReturn(response);
    }

    @Override
    public StatusOnlyReturn revertTask(String taskUUID) {
        JsonRpcRequest request = new RequestBuilder("Task.revert").withParameter("taskID", taskUUID).build();
        Map<String, Object> response = new FutureMap(this.client, request);
        return new StatusOnlyReturn(response);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public StatusOnlyReturn hotplugDisk(Map info) {
        JsonRpcRequest request =
                new RequestBuilder("VM.hotplugDisk").withParameter("vmID", getVmId(info))
                        .withParameter("params", info)
                        .build();
        Map<String, Object> response = new FutureMap(this.client, request);
        return new StatusOnlyReturn(response);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public StatusOnlyReturn hotunplugDisk(Map info) {
        JsonRpcRequest request =
                new RequestBuilder("VM.hotunplugDisk").withParameter("vmID", getVmId(info))
                        .withParameter("params", info)
                        .build();
        Map<String, Object> response = new FutureMap(this.client, request);
        return new StatusOnlyReturn(response);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public StatusOnlyReturn hotPlugNic(Map info) {
        JsonRpcRequest request =
                new RequestBuilder("VM.hotplugNic").withParameter("vmID", getVmId(info))
                        .withParameter("params", info)
                        .build();
        Map<String, Object> response = new FutureMap(this.client, request);
        return new StatusOnlyReturn(response);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public StatusOnlyReturn hotUnplugNic(Map info) {
        JsonRpcRequest request =
                new RequestBuilder("VM.hotunplugNic").withParameter("vmID", getVmId(info))
                        .withParameter("params", info)
                        .build();
        Map<String, Object> response = new FutureMap(this.client, request);
        return new StatusOnlyReturn(response);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public StatusOnlyReturn vmUpdateDevice(String vmId, Map device) {
        JsonRpcRequest request =
                new RequestBuilder("VM.updateDevice").withParameter("vmID", vmId)
                        .withParameter("params", device)
                        .build();
        Map<String, Object> response = new FutureMap(this.client, request);
        return new StatusOnlyReturn(response);
    }

    @Override
    public FutureTask<Map<String, Object>> poll() {
        return timeBoundPoll(2, TimeUnit.SECONDS);
    }

    @Override
    public FutureTask<Map<String, Object>> timeBoundPoll(final long timeout, final TimeUnit unit) {
        final JsonRpcRequest request = new RequestBuilder("Host.ping").build();
        final FutureCallable callable = new FutureCallable(() -> new FutureMap(client, request, timeout, unit, true));

        FutureTask<Map<String, Object>> future = new FutureTask<Map<String, Object>>(callable) {

                    @Override
                    public boolean isDone() {
                        return callable.isDone();
                    }
                };

        ThreadPoolUtil.execute(future);
        return future;
    }

    @Override
    public StatusOnlyReturn snapshot(String vmId, Map<String, String>[] disks) {
        return snapshot(vmId, disks, null, false);
    }

    @Override
    public StatusOnlyReturn snapshot(String vmId, Map<String, String>[] disks, String memory) {
        return snapshot(vmId, disks, memory, false);
    }

    @Override
    public StatusOnlyReturn snapshot(String vmId, Map<String, String>[] disks, String memory, boolean frozen) {
        JsonRpcRequest request =
                new RequestBuilder("VM.snapshot").withParameter("vmID", vmId)
                        .withParameter("snapDrives", new ArrayList<>(Arrays.asList(disks)))
                        .withOptionalParameter("snapMemory", memory)
                        .withParameter("frozen", frozen)
                        .build();
        Map<String, Object> response = new FutureMap(this.client, request);
        return new StatusOnlyReturn(response);
    }

    @Override
    public AlignmentScanReturn getDiskAlignment(String vmId, Map<String, String> driveSpecs) {
        JsonRpcRequest request =
                new RequestBuilder("VM.getDiskAlignment").withParameter("vmID", vmId)
                        .withParameter("disk", driveSpecs)
                        .build();
        Map<String, Object> response =
                new FutureMap(this.client, request).withResponseKey("alignment");
        return new AlignmentScanReturn(response);
    }

    @Override
    public ImageSizeReturn diskSizeExtend(String vmId, Map<String, String> diskParams, String newSize) {
        JsonRpcRequest request =
                new RequestBuilder("VM.diskSizeExtend").withParameter("vmID", vmId)
                        .withParameter("driveSpecs", diskParams)
                        .withParameter("newSize", newSize)
                        .build();
        Map<String, Object> response =
                new FutureMap(this.client, request).withResponseKey("size");
        return new ImageSizeReturn(response);
    }

    @Override
    public StatusOnlyReturn merge(String vmId, Map<String, String> drive,
            String baseVolUUID, String topVolUUID, String bandwidth, String jobUUID) {
        JsonRpcRequest request =
                new RequestBuilder("VM.merge").withParameter("vmID", vmId)
                        .withParameter("drive", drive)
                        .withParameter("baseVolUUID", baseVolUUID)
                        .withParameter("topVolUUID", topVolUUID)
                        .withParameter("bandwidth", bandwidth)
                        .withParameter("jobUUID", jobUUID)
                        .build();
        Map<String, Object> response = new FutureMap(this.client, request);
        return new StatusOnlyReturn(response);
    }

    @Override
    public OneUuidReturn glusterVolumeCreate(String volumeName,
            String[] brickList,
            int replicaCount,
            int stripeCount,
            String[] transportList,
            boolean force,
            boolean isArbiter) {
        JsonRpcRequest request =
                new RequestBuilder("GlusterVolume.create").withParameter("volumeName", volumeName)
                        .withParameter("bricklist", new ArrayList<>(Arrays.asList(brickList)))
                        .withParameter("replicaCount", replicaCount)
                        .withParameter("stripeCount", stripeCount)
                        .withParameter("transportList", new ArrayList<>(Arrays.asList(transportList)))
                        .withParameter("force", force)
                        .withParameter("arbiter", isArbiter)
                        .build();
        Map<String, Object> response =
                new FutureMap(this.client, request).withIgnoreResponseKey();
        return new OneUuidReturn(response);
    }

    @Override
    public StatusOnlyReturn glusterVolumeSet(String volumeName, String key, String value) {
        JsonRpcRequest request =
                new RequestBuilder("GlusterVolume.set").withParameter("volumeName", volumeName)
                        .withParameter("option", key)
                        .withParameter("value", value)
                        .build();
        Map<String, Object> response = new FutureMap(this.client, request);
        return new StatusOnlyReturn(response);
    }

    @Override
    public StatusOnlyReturn glusterVolumeStart(String volumeName, Boolean force) {
        JsonRpcRequest request =
                new RequestBuilder("GlusterVolume.start").withParameter("volumeName", volumeName)
                        .withParameter("force", force)
                        .build();
        Map<String, Object> response = new FutureMap(this.client, request);
        return new StatusOnlyReturn(response);
    }

    @Override
    public StatusOnlyReturn glusterVolumeStop(String volumeName, Boolean force) {
        JsonRpcRequest request =
                new RequestBuilder("GlusterVolume.stop").withParameter("volumeName", volumeName)
                        .withParameter("force", force)
                        .build();
        Map<String, Object> response = new FutureMap(this.client, request);
        return new StatusOnlyReturn(response);
    }

    @Override
    public StatusOnlyReturn glusterVolumeDelete(String volumeName) {
        JsonRpcRequest request =
                new RequestBuilder("GlusterVolume.delete").withParameter("volumeName", volumeName).build();
        Map<String, Object> response = new FutureMap(this.client, request);
        return new StatusOnlyReturn(response);
    }

    @Override
    public StatusOnlyReturn glusterVolumeReset(String volumeName, String volumeOption, Boolean force) {
        JsonRpcRequest request =
                new RequestBuilder("GlusterVolume.reset").withParameter("volumeName", volumeName)
                        .withParameter("option", volumeOption)
                        .withParameter("force", force)
                        .build();
        Map<String, Object> response = new FutureMap(this.client, request);
        return new StatusOnlyReturn(response);
    }

    @Override
    public GlusterVolumeOptionsInfoReturn glusterVolumeSetOptionsList() {
        JsonRpcRequest request = new RequestBuilder("GlusterVolume.setOptionsList").build();
        Map<String, Object> response =
                new FutureMap(this.client, request).withIgnoreResponseKey();
        return new GlusterVolumeOptionsInfoReturn(response);
    }

    @Override
    public GlusterTaskInfoReturn glusterVolumeRemoveBricksStart(String volumeName,
            String[] brickList,
            int replicaCount,
            Boolean forceRemove) {
        String command = "GlusterVolume.removeBrickStart";
        if(forceRemove) {
            command = "GlusterVolume.removeBrickForce";
        }
        JsonRpcRequest request = new RequestBuilder(command).withParameter("volumeName", volumeName)
                        .withParameter("brickList", new ArrayList<>(Arrays.asList(brickList)))
                        .withParameter("replicaCount", replicaCount)
                        .build();
        Map<String, Object> response =
                new FutureMap(this.client, request).withIgnoreResponseKey();
        return new GlusterTaskInfoReturn(response);
    }

    @Override
    public GlusterVolumeTaskReturn glusterVolumeRemoveBricksStop(String volumeName,
            String[] brickList,
            int replicaCount) {
        JsonRpcRequest request =
                new RequestBuilder("GlusterVolume.removeBrickStop").withParameter("volumeName", volumeName)
                        .withParameter("brickList", new ArrayList<>(Arrays.asList(brickList)))
                        .withParameter("replicaCount", replicaCount)
                        .build();
        Map<String, Object> response = new FutureMap(this.client, request);
        return new GlusterVolumeTaskReturn(response);
    }

    @Override
    public StatusOnlyReturn glusterVolumeRemoveBricksCommit(String volumeName,
            String[] brickList,
            int replicaCount) {
        JsonRpcRequest request =
                new RequestBuilder("GlusterVolume.removeBrickCommit").withParameter("volumeName", volumeName)
                        .withParameter("brickList", new ArrayList<>(Arrays.asList(brickList)))
                        .withParameter("replicaCount", replicaCount)
                        .build();
        Map<String, Object> response = new FutureMap(this.client, request);
        return new StatusOnlyReturn(response);
    }

    @Override
    public StatusOnlyReturn glusterVolumeBrickAdd(String volumeName,
            String[] bricks,
            int replicaCount,
            int stripeCount,
            boolean force) {
        JsonRpcRequest request =
                new RequestBuilder("GlusterVolume.addBrick").withParameter("volumeName", volumeName)
                        .withParameter("brickList", new ArrayList<>(Arrays.asList(bricks)))
                        .withParameter("replicaCount", replicaCount)
                        .withParameter("stripeCount", stripeCount)
                        .withParameter("force", force)
                        .build();
        Map<String, Object> response = new FutureMap(this.client, request);
        return new StatusOnlyReturn(response);
    }

    @Override
    public GlusterTaskInfoReturn glusterVolumeRebalanceStart(String volumeName,
            Boolean fixLayoutOnly,
            Boolean force) {
        JsonRpcRequest request =
                new RequestBuilder("GlusterVolume.rebalanceStart").withParameter("volumeName", volumeName)
                        .withParameter("rebalanceType", fixLayoutOnly)
                        .withParameter("force", force)
                        .build();
        Map<String, Object> response =
                new FutureMap(this.client, request).withIgnoreResponseKey();
        return new GlusterTaskInfoReturn(response);
    }

    @Override
    public BooleanReturn glusterVolumeEmptyCheck(String volumeName) {
        JsonRpcRequest request = new RequestBuilder("GlusterVolume.volumeEmptyCheck").withParameter("volumeName", volumeName).build();
        Map<String, Object> response = new FutureMap(this.client, request);
        return new BooleanReturn(response, "volumeEmptyCheck");
    }

    @Override
    public GlusterHostsPubKeyReturn glusterGeoRepKeysGet() {
        JsonRpcRequest request = new RequestBuilder("GlusterVolume.geoRepKeysGet").build();
        Map<String, Object> response = new FutureMap(this.client, request);
        return new GlusterHostsPubKeyReturn(response);
    }

    @Override
    public StatusOnlyReturn glusterGeoRepKeysUpdate(List<String> geoRepPubKeys, String userName) {
        JsonRpcRequest request =
                new RequestBuilder("GlusterVolume.geoRepKeysUpdate")
                        .withParameter("geoRepPubKeys", geoRepPubKeys)
                        .withOptionalParameter("userName", userName).build();
        Map<String, Object> response = new FutureMap(this.client, request);
        return new StatusOnlyReturn(response);
    }

    @Override
    public StatusOnlyReturn glusterGeoRepMountBrokerSetup(String remoteVolumeName, String userName, String remoteGroupName, Boolean partial) {
        JsonRpcRequest request =
                new RequestBuilder("GlusterVolume.geoRepMountBrokerSetup").withParameter("remoteVolumeName", remoteVolumeName)
                        .withParameter("partial", partial)
                        .withOptionalParameter("remoteUserName", userName)
                        .withOptionalParameter("remoteGroupName", remoteGroupName).build();
        Map<String, Object> response = new FutureMap(this.client, request);
        return new StatusOnlyReturn(response);
    }

    @Override
    public StatusOnlyReturn glusterVolumeGeoRepSessionCreate(String volumeName, String remoteHost, String remotVolumeName, String userName, Boolean force) {
        JsonRpcRequest request =
                new RequestBuilder("GlusterVolume.geoRepSessionCreate").withParameter("volumeName", volumeName)
                .withParameter("remoteHost", remoteHost)
                .withParameter("remoteVolumeName", remotVolumeName)
                .withParameter("force", force)
                .withOptionalParameter("remoteUserName", userName).build();
        Map<String, Object> response = new FutureMap(this.client, request);
        return new StatusOnlyReturn(response);
    }

    @Override
    public StatusOnlyReturn glusterVolumeGeoRepSessionResume(String volumeName,
            String slaveHostName,
            String slaveVolumeName,
            String userName,
            boolean force) {
        JsonRpcRequest request = new RequestBuilder("GlusterVolume.geoRepSessionResume").withParameter("volumeName", volumeName)
                        .withParameter("remoteHost", slaveHostName)
                        .withParameter("remoteVolumeName", slaveVolumeName)
                        .withOptionalParameter("remoteUserName", userName)
                        .withParameter("force", force).build();
        Map<String, Object> response = new FutureMap(this.client, request);
        return new StatusOnlyReturn(response);
    }

    @Override
    public GlusterVolumeTaskReturn glusterVolumeRebalanceStop(String volumeName) {
        JsonRpcRequest request =
                new RequestBuilder("GlusterVolume.rebalanceStop").withParameter("volumeName", volumeName).build();
        Map<String, Object> response =
                new FutureMap(this.client, request).withIgnoreResponseKey();
        return new GlusterVolumeTaskReturn(response);
    }

    @Override
    public StatusOnlyReturn glusterVolumeReplaceBrickCommitForce(String volumeName,
            String existingBrickDir,
            String newBrickDir) {
        JsonRpcRequest request =
                new RequestBuilder("GlusterVolume.replaceBrickCommitForce").withParameter("volumeName", volumeName)
                        .withParameter("existingBrick", existingBrickDir)
                        .withParameter("newBrick", newBrickDir)
                        .build();
        Map<String, Object> response = new FutureMap(this.client, request);
        return new StatusOnlyReturn(response);
    }

    @Override
    public StatusOnlyReturn glusterHostRemove(String hostName, Boolean force) {
        JsonRpcRequest request =
                new RequestBuilder("GlusterHost.remove").withParameter("hostName", hostName)
                        .withParameter("force", force)
                        .build();
        Map<String, Object> response = new FutureMap(this.client, request);
        return new StatusOnlyReturn(response);
    }

    @Override
    public StatusOnlyReturn glusterHostAdd(String hostName) {
        JsonRpcRequest request = new RequestBuilder("GlusterHost.add").withParameter("hostName", hostName).build();
        Map<String, Object> response = new FutureMap(this.client, request);
        return new StatusOnlyReturn(response);
    }

    @Override
    public StatusOnlyReturn glusterVolumeGeoRepSessionDelete(String volumeName,
            String remoteHost,
            String remoteVolumeName,
            String userName) {
        JsonRpcRequest request = new RequestBuilder("GlusterVolume.geoRepSessionDelete")
                .withParameter("volumeName", volumeName)
                .withParameter("remoteHost", remoteHost)
                .withParameter("remoteVolumeName", remoteVolumeName)
                .withOptionalParameter("remoteUserName", userName)
                .build();

        Map<String, Object> response = new FutureMap(this.client, request);
        return new StatusOnlyReturn(response);
    }

    @Override
    public StatusOnlyReturn glusterVolumeGeoRepSessionStop(String volumeName,
            String remoteHost,
            String remoteVolumeName,
            String userName,
            Boolean force) {
        JsonRpcRequest request = new RequestBuilder("GlusterVolume.geoRepSessionStop")
                .withParameter("volumeName", volumeName)
                .withParameter("remoteHost", remoteHost)
                .withParameter("remoteVolumeName", remoteVolumeName)
                .withOptionalParameter("remoteUserName", userName)
                .withParameter("force", force)
                .build();

        Map<String, Object> response = new FutureMap(this.client, request);
        return new StatusOnlyReturn(response);
    }

    @Override
    public GlusterServersListReturn glusterServersList() {
        JsonRpcRequest request = new RequestBuilder("GlusterHost.list").build();
        Map<String, Object> response =
                new FutureMap(this.client, request).withIgnoreResponseKey();
        return new GlusterServersListReturn(response);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public StatusOnlyReturn diskReplicateStart(String vmUUID, Map srcDisk, Map dstDisk) {
        JsonRpcRequest request =
                new RequestBuilder("VM.diskReplicateStart").withParameter("vmID", vmUUID)
                        .withParameter("srcDisk", srcDisk)
                        .withParameter("dstDisk", dstDisk)
                        .build();
        Map<String, Object> response = new FutureMap(this.client, request);
        return new StatusOnlyReturn(response);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public StatusOnlyReturn diskReplicateFinish(String vmUUID, Map srcDisk, Map dstDisk) {
        JsonRpcRequest request =
                new RequestBuilder("VM.diskReplicateFinish").withParameter("vmID", vmUUID)
                        .withParameter("srcDisk", srcDisk)
                        .withParameter("dstDisk", dstDisk)
                        .build();
        Map<String, Object> response = new FutureMap(this.client, request);
        return new StatusOnlyReturn(response);
    }

    @Override
    public StatusOnlyReturn glusterVolumeProfileStart(String volumeName) {
        JsonRpcRequest request =
                new RequestBuilder("GlusterVolume.profileStart").withParameter("volumeName", volumeName).build();
        Map<String, Object> response = new FutureMap(this.client, request);
        return new StatusOnlyReturn(response);
    }

    @Override
    public StatusOnlyReturn glusterVolumeProfileStop(String volumeName) {
        JsonRpcRequest request =
                new RequestBuilder("GlusterVolume.profileStop").withParameter("volumeName", volumeName).build();
        Map<String, Object> response = new FutureMap(this.client, request);
        return new StatusOnlyReturn(response);
    }

    @Override
    public StatusOnlyReturn glusterVolumeGeoRepSessionPause(String masterVolumeName,
            String slaveHost,
            String slaveVolumeName,
            String userName,
            boolean force) {
        JsonRpcRequest request = new RequestBuilder("GlusterVolume.geoRepSessionPause").withParameter("volumeName", masterVolumeName)
                        .withParameter("remoteHost", slaveHost)
                        .withParameter("remoteVolumeName", slaveVolumeName)
                        .withOptionalParameter("remoteUserName", userName)
                        .withParameter("force", force).build();
        Map<String, Object> response = new FutureMap(this.client, request);
        return new StatusOnlyReturn(response);
    }

    @Override
    public StatusOnlyReturn glusterVolumeGeoRepSessionStart(String volumeName,
            String remoteHost,
            String remoteVolumeName,
            String userName,
            Boolean force) {
        JsonRpcRequest request = new RequestBuilder("GlusterVolume.geoRepSessionStart").withParameter("volumeName", volumeName)
                        .withParameter("remoteHost", remoteHost)
                        .withParameter("remoteVolumeName", remoteVolumeName)
                        .withOptionalParameter("remoteUserName", userName)
                        .withParameter("force", force).build();
        Map<String, Object> response = new FutureMap(this.client, request);
        return new StatusOnlyReturn(response);
    }

    @Override
    public StatusOnlyReturn glusterVolumeGeoRepConfigSet(String volumeName,
            String slaveHost,
            String slaveVolumeName,
            String configKey,
            String configValue,
            String userName) {
        JsonRpcRequest request = new RequestBuilder("GlusterVolume.geoRepConfigSet")
                .withParameter("volumeName", volumeName)
                .withParameter("remoteHost", slaveHost)
                .withParameter("remoteVolumeName", slaveVolumeName)
                .withParameter("optionName", configKey)
                .withParameter("optionValue", configValue)
                .withOptionalParameter("remoteUserName", userName).build();
        Map<String, Object> response = new FutureMap(this.client, request);
        return new StatusOnlyReturn(response);
    }

    @Override
    public StatusOnlyReturn glusterVolumeGeoRepConfigReset(String volumeName,
            String slaveHost,
            String slaveVolumeName,
            String configKey,
            String userName) {
        JsonRpcRequest request = new RequestBuilder("GlusterVolume.geoRepConfigReset")
                .withParameter("volumeName", volumeName)
                .withParameter("remoteHost", slaveHost)
                .withParameter("remoteVolumeName", slaveVolumeName)
                .withParameter("optionName", configKey)
                .withOptionalParameter("remoteUserName", userName)
                .build();

        Map<String, Object> response = new FutureMap(this.client, request);
        return new StatusOnlyReturn(response);
    }

    @Override
    public GlusterVolumeGeoRepConfigList glusterVolumeGeoRepConfigList(String volumeName,
            String slaveHost,
            String slaveVolumeName,
            String userName) {
        JsonRpcRequest request = new RequestBuilder("GlusterVolume.geoRepConfigList")
                .withParameter("volumeName", volumeName)
                .withParameter("remoteHost", slaveHost)
                .withParameter("remoteVolumeName", slaveVolumeName)
                .withOptionalParameter("remoteUserName", userName)
                .build();
        Map<String, Object> response = new FutureMap(this.client, request);
        return new GlusterVolumeGeoRepConfigList(response);
    }

    @Override
    public GlusterVolumeStatusReturn glusterVolumeStatus(Guid clusterId,
            String volumeName,
            String brickName,
            String volumeStatusOption) {
        JsonRpcRequest request =
                new RequestBuilder("GlusterVolume.status").withParameter("volumeName", volumeName)
                        .withParameter("brick", brickName)
                        .withParameter("statusOption", volumeStatusOption)
                        .build();
        Map<String, Object> response =
                new FutureMap(this.client, request).withIgnoreResponseKey();
        return new GlusterVolumeStatusReturn(clusterId, response);
    }

    @Override
    public GlusterVolumesListReturn glusterVolumesList(Guid clusterId) {
        JsonRpcRequest request = new RequestBuilder("GlusterVolume.list").build();
        Map<String, Object> response =
                new FutureMap(this.client, request).withIgnoreResponseKey();
        return new GlusterVolumesListReturn(clusterId, response);
    }

    @Override
    public GlusterVolumesListReturn glusterVolumeInfo(Guid clusterId, String volumeName) {
        JsonRpcRequest request =
                new RequestBuilder("GlusterVolume.list").withParameter("volumeName", volumeName).build();
        Map<String, Object> response =
                new FutureMap(this.client, request).withIgnoreResponseKey();
        return new GlusterVolumesListReturn(clusterId, response);
    }

    @Override
    public GlusterVolumesHealInfoReturn glusterVolumeHealInfo(String volumeName) {
        JsonRpcRequest request =
                new RequestBuilder("GlusterVolume.healInfo").withParameter("volumeName", volumeName).build();
        Map<String, Object> response =
                new FutureMap(this.client, request).withIgnoreResponseKey();
        return new GlusterVolumesHealInfoReturn(response);
    }

    @Override
    public GlusterVolumeProfileInfoReturn glusterVolumeProfileInfo(Guid clusterId, String volumeName, boolean nfs) {
        JsonRpcRequest request =
                new RequestBuilder("GlusterVolume.profileInfo").withParameter("volumeName", volumeName)
                .withParameter("nfs", nfs).build();
        Map<String, Object> response =
                new FutureMap(this.client, request).withIgnoreResponseKey();
        return new GlusterVolumeProfileInfoReturn(clusterId, response);
    }

    @Override
    public StatusOnlyReturn glusterHookEnable(String glusterCommand, String stage, String hookName) {
        JsonRpcRequest request =
                new RequestBuilder("GlusterHook.enable").withParameter("glusterCmd", glusterCommand)
                        .withParameter("hookLevel", stage)
                        .withParameter("hookName", hookName)
                        .build();
        Map<String, Object> response = new FutureMap(this.client, request);
        return new StatusOnlyReturn(response);
    }

    @Override
    public StatusOnlyReturn glusterHookDisable(String glusterCommand, String stage, String hookName) {
        JsonRpcRequest request =
                new RequestBuilder("GlusterHook.disable").withParameter("glusterCmd", glusterCommand)
                        .withParameter("hookLevel", stage)
                        .withParameter("hookName", hookName)
                        .build();
        Map<String, Object> response = new FutureMap(this.client, request);
        return new StatusOnlyReturn(response);
    }

    @Override
    public GlusterHooksListReturn glusterHooksList() {
        JsonRpcRequest request = new RequestBuilder("GlusterHook.list").build();
        Map<String, Object> response =
                new FutureMap(this.client, request).withIgnoreResponseKey();
        return new GlusterHooksListReturn(response);
    }

    @Override
    public OneUuidReturn glusterHostUUIDGet() {
        JsonRpcRequest request = new RequestBuilder("GlusterHost.uuid").build();
        Map<String, Object> response =
                new FutureMap(this.client, request).withIgnoreResponseKey();
        return new OneUuidReturn(response);
    }

    @Override
    public GlusterServicesReturn glusterServicesList(Guid serverId, String[] serviceNames) {
        JsonRpcRequest request =
                new RequestBuilder("GlusterService.get").withParameter("serviceNames",
                        new ArrayList<>(Arrays.asList(serviceNames))).build();
        Map<String, Object> response =
                new FutureMap(this.client, request).withIgnoreResponseKey();
        return new GlusterServicesReturn(serverId, response);
    }

    @Override
    public GlusterHookContentInfoReturn glusterHookRead(String glusterCommand, String stage, String hookName) {
        JsonRpcRequest request =
                new RequestBuilder("GlusterHook.read").withParameter("glusterCmd", glusterCommand)
                        .withParameter("hookLevel", stage)
                        .withParameter("hookName", hookName)
                        .build();
        Map<String, Object> response =
                new FutureMap(this.client, request).withIgnoreResponseKey();
        return new GlusterHookContentInfoReturn(response);
    }

    @Override
    public StatusOnlyReturn glusterHookUpdate(String glusterCommand,
            String stage,
            String hookName,
            String content,
            String checksum) {
        JsonRpcRequest request =
                new RequestBuilder("GlusterHook.update").withParameter("glusterCmd", glusterCommand)
                        .withParameter("hookLevel", stage)
                        .withParameter("hookName", hookName)
                        .withParameter("hookData", content)
                        .withParameter("hookMd5Sum", checksum)
                        .build();
        Map<String, Object> response = new FutureMap(this.client, request);
        return new StatusOnlyReturn(response);
    }

    @Override
    public StatusOnlyReturn glusterHookAdd(String glusterCommand,
            String stage,
            String hookName,
            String content,
            String checksum,
            Boolean enabled) {
        JsonRpcRequest request =
                new RequestBuilder("GlusterHook.add").withParameter("glusterCmd", glusterCommand)
                        .withParameter("hookLevel", stage)
                        .withParameter("hookName", hookName)
                        .withParameter("hookData", content)
                        .withParameter("hookMd5Sum", checksum)
                        .withParameter("enable", enabled)
                        .build();
        Map<String, Object> response = new FutureMap(this.client, request);
        return new StatusOnlyReturn(response);
    }

    @Override
    public StatusOnlyReturn glusterHookRemove(String glusterCommand, String stage, String hookName) {
        JsonRpcRequest request =
                new RequestBuilder("GlusterHook.remove").withParameter("glusterCmd", glusterCommand)
                        .withParameter("hookLevel", stage)
                        .withParameter("hookName", hookName)
                        .build();
        Map<String, Object> response = new FutureMap(this.client, request);
        return new StatusOnlyReturn(response);
    }

    @Override
    public GlusterServicesReturn glusterServicesAction(Guid serverId, String[] serviceList, String actionType) {
        JsonRpcRequest request =
                new RequestBuilder("GlusterService.action").withParameter("serviceNames",
                        new ArrayList<>(Arrays.asList(serviceList)))
                        .withParameter("action", actionType)
                        .build();
        Map<String, Object> response =
                new FutureMap(this.client, request).withIgnoreResponseKey();
        return new GlusterServicesReturn(serverId, response);
    }

    @Override
    public StoragePoolInfo getStoragePoolInfo(String spUUID) {
        JsonRpcRequest request =
                new RequestBuilder("StoragePool.getInfo").withParameter("storagepoolID", spUUID).build();
        Map<String, Object> response =
                new FutureMap(this.client, request).withIgnoreResponseKey();
        return new StoragePoolInfo(response);
    }

    @Override
    public GlusterTasksListReturn glusterTasksList() {
        JsonRpcRequest request = new RequestBuilder("GlusterTask.list").build();
        Map<String, Object> response =
                new FutureMap(this.client, request).withIgnoreResponseKey();
        return new GlusterTasksListReturn(response);
    }

    @Override
    public GlusterVolumeTaskReturn glusterVolumeRebalanceStatus(String volumeName) {
        JsonRpcRequest request =
                new RequestBuilder("GlusterVolume.rebalanceStatus").withParameter("volumeName", volumeName).build();
        Map<String, Object> response =
                new FutureMap(this.client, request).withIgnoreResponseKey();
        return new GlusterVolumeTaskReturn(response);
    }

    @Override
    public GlusterVolumeGeoRepStatus glusterVolumeGeoRepSessionList() {
        JsonRpcRequest request = new RequestBuilder("GlusterVolume.geoRepSessionList").build();
        Map<String, Object> response = new FutureMap(this.client, request).withIgnoreResponseKey();
        return new GlusterVolumeGeoRepStatus(response);
    }

    @Override
    public GlusterVolumeGeoRepStatus glusterVolumeGeoRepSessionList(String volumeName) {
        JsonRpcRequest request = new RequestBuilder("GlusterVolume.geoRepSessionList")
                .withParameter("volumeName", volumeName)
                    .build();
        Map<String, Object> response = new FutureMap(this.client, request).withIgnoreResponseKey();
        return new GlusterVolumeGeoRepStatus(response);
    }

    @Override
    public GlusterVolumeGeoRepStatus glusterVolumeGeoRepSessionList(String volumeName,
            String slaveHost,
            String slaveVolumeName,
            String userName) {
        JsonRpcRequest request =
                new RequestBuilder("GlusterVolume.geoRepSessionList").withParameter("volumeName", volumeName)
                        .withParameter("remoteHost", slaveHost)
                        .withParameter("remoteVolumeName", slaveVolumeName)
                        .withOptionalParameter("remoteUserName", userName).build();
        Map<String, Object> response = new FutureMap(this.client, request).withIgnoreResponseKey();
        return new GlusterVolumeGeoRepStatus(response);
    }

    @Override
    public GlusterVolumeGeoRepStatusDetail glusterVolumeGeoRepSessionStatus(String volumeName,
            String slaveHost,
            String slaveVolumeName,
            String userName) {
        JsonRpcRequest request =
                new RequestBuilder("GlusterVolume.geoRepSessionStatus").withParameter("volumeName", volumeName)
                        .withParameter("remoteHost", slaveHost)
                        .withParameter("remoteVolumeName", slaveVolumeName)
                        .withOptionalParameter("remoteUserName", userName).build();
        Map<String, Object> response = new FutureMap(this.client, request).withIgnoreResponseKey();
        return new GlusterVolumeGeoRepStatusDetail(response);
    }

    @Override
    public GlusterVolumeTaskReturn glusterVolumeRemoveBrickStatus(String volumeName, String[] bricksList) {
        JsonRpcRequest request =
                new RequestBuilder("GlusterVolume.removeBrickStatus").withParameter("volumeName", volumeName)
                        .withParameter("brickList", new ArrayList<>(Arrays.asList(bricksList)))
                        .build();
        Map<String, Object> response =
                new FutureMap(this.client, request).withIgnoreResponseKey();
        return new GlusterVolumeTaskReturn(response);
    }

    @Override
    public StatusOnlyReturn setNumberOfCpus(String vmId, String numberOfCpus) {
        JsonRpcRequest request =
                new RequestBuilder("VM.setNumberOfCpus").withParameter("vmID", vmId)
                        .withParameter("numberOfCpus", numberOfCpus)
                        .build();
        Map<String, Object> response =
                new FutureMap(this.client, request);
        return new StatusOnlyReturn(response);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public StatusOnlyReturn hotplugMemory(Map info) {
        JsonRpcRequest request =
                new RequestBuilder("VM.hotplugMemory")
                        .withParameter("vmID", getVmId(info))
                        .withParameter("params", info)
                        .build();
        Map<String, Object> response =
                new FutureMap(this.client, request);
        return new StatusOnlyReturn(response);
    }

    @Override
    public StatusOnlyReturn hotUnplugMemory(Map<String, Object> params) {
        JsonRpcRequest request =
                new RequestBuilder("VM.hotunplugMemory")
                        .withParameter("vmID", getVmId(params))
                        .withParameter("params", params)
                        .build();
        Map<String, Object> response =
                new FutureMap(this.client, request);
        return new StatusOnlyReturn(response);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public StatusOnlyReturn updateVmPolicy(Map params) {
        JsonRpcRequest request =
                new RequestBuilder("VM.updateVmPolicy").withParameter("vmID", (String) params.get("vmId"))
                        .withParameter("params", params)
                        .build();
        Map<String, Object> response =
                new FutureMap(this.client, request);
        return new StatusOnlyReturn(response);
    }

    @Override
    public StatusOnlyReturn setHaMaintenanceMode(String mode, boolean enabled) {
        JsonRpcRequest request =
                new RequestBuilder("Host.setHaMaintenanceMode").withParameter("mode", mode)
                        .withParameter("enabled", enabled)
                        .build();
        Map<String, Object> response =
                new FutureMap(this.client, request);
        return new StatusOnlyReturn(response);
    }

    @Override
    public StatusOnlyReturn add_image_ticket(String ticketId, String[] ops, long timeout,
            long size, String url) {
        HashMap<String, Object> ticketDict = new HashMap<>();
        ticketDict.put("uuid", ticketId);
        ticketDict.put("timeout", timeout);
        ticketDict.put("ops", ops);
        ticketDict.put("size", size);
        ticketDict.put("url", url);

        JsonRpcRequest request =
                new RequestBuilder("Host.add_image_ticket")
                        .withParameter("ticket", ticketDict)
                        .build();
        Map<String, Object> response =
                new FutureMap(this.client, request);
        return new StatusOnlyReturn(response);
    }

    @Override
    public StatusOnlyReturn remove_image_ticket(String ticketId) {
        JsonRpcRequest request =
                new RequestBuilder("Host.remove_image_ticket")
                        .withParameter("uuid", ticketId)
                        .build();
        Map<String, Object> response =
                new FutureMap(this.client, request);
        return new StatusOnlyReturn(response);

    }

    @Override
    public StatusOnlyReturn extend_image_ticket(String ticketId, long timeout) {
        JsonRpcRequest request =
                new RequestBuilder("Host.extend_image_ticket")
                        .withParameter("uuid", ticketId)
                        .withParameter("timeout", timeout)
                        .build();
        Map<String, Object> response =
                new FutureMap(this.client, request);
        return new StatusOnlyReturn(response);
    }

    @Override
    public OneMapReturn get_image_transfer_session_stats(String ticketId) {
        JsonRpcRequest request =
                new RequestBuilder("Host.get_image_transfer_session_stats")
                        .withParameter("ticketUUID", ticketId)
                        .build();
        Map<String, Object> response =
                new FutureMap(this.client, request).withResponseKey("statsMap");
        return new OneMapReturn(response);
    }

    @Override
    public PrepareImageReturn prepareImage(String spID, String sdID, String imageID,
            String volumeID, boolean allowIllegal) {
        JsonRpcRequest request =
                new RequestBuilder("Image.prepare")
                        .withParameter("storagepoolID", spID)
                        .withParameter("storagedomainID", sdID)
                        .withParameter("imageID", imageID)
                        .withParameter("volumeID", volumeID)
                        .withParameter("allowIllegal", allowIllegal)
                        .build();
        Map<String, Object> response =
                new FutureMap(this.client, request);
        return new PrepareImageReturn(response);
    }

    @Override
    public StatusReturn teardownImage(String spID, String sdID, String imageID, String volumeID) {
        JsonRpcRequest request =
                new RequestBuilder("Image.teardown")
                        .withParameter("storagepoolID", spID)
                        .withParameter("storagedomainID", sdID)
                        .withParameter("imageID", imageID)
                        .withParameter("leafVolID", volumeID)
                        .build();
        Map<String, Object> response =
                new FutureMap(this.client, request);
        return new StatusReturn(response);
    }

    @Override
    public StatusReturn verifyUntrustedVolume(String spID, String sdID, String imageID, String volumeID) {
        JsonRpcRequest request =
                new RequestBuilder("Volume.verify_untrusted")
                        .withParameter("storagepoolID", spID)
                        .withParameter("storagedomainID", sdID)
                        .withParameter("imageID", imageID)
                        .withParameter("volumeID", volumeID)
                        .build();
        Map<String, Object> response =
                new FutureMap(this.client, request);
        return new StatusReturn(response);
    }

    @Override
    public VMListReturn getExternalVmList(String uri, String username, String password, List<String> vmsNames) {
        RequestBuilder requestBuilder = new RequestBuilder("Host.getExternalVMs")
                .withParameter("uri", uri)
                .withParameter("username", username)
                .withParameter("password", password)
                .withOptionalParameterAsList("vm_names", vmsNames);
        JsonRpcRequest request = requestBuilder.build();

        Map<String, Object> response =
                new FutureMap(this.client, request).withResponseKey("vmList")
                        .withResponseType(Object[].class);
        return new VMListReturn(response);
    }

    @Override
    public VMNamesListReturn getExternalVmNamesList(String uri, String username, String password) {
        JsonRpcRequest request =
                new RequestBuilder("Host.getExternalVMNames")
                        .withParameter("uri", uri)
                        .withParameter("username", username)
                        .withParameter("password", password)
                        .build();
        Map<String, Object> response =
                new FutureMap(this.client, request).withResponseKey("vmNames")
                        .withResponseType(Object[].class);
        return new VMNamesListReturn(response);
    }

    @Override
    public GlusterVolumeSnapshotInfoReturn glusterVolumeSnapshotList(Guid clusterId, String volumeName) {
        JsonRpcRequest request =
                new RequestBuilder("GlusterVolume.snapshotList").withOptionalParameter("volumeName", volumeName)
                        .build();
        Map<String, Object> response = new FutureMap(this.client, request).withIgnoreResponseKey();
        return new GlusterVolumeSnapshotInfoReturn(clusterId, response);
    }

    @Override
    public GlusterVolumeSnapshotConfigReturn glusterSnapshotConfigList(Guid clusterId) {
        JsonRpcRequest request =
                new RequestBuilder("GlusterSnapshot.configList").build();
        Map<String, Object> response = new FutureMap(this.client, request).withIgnoreResponseKey();
        return new GlusterVolumeSnapshotConfigReturn(clusterId, response);
    }

    @Override
    public StatusOnlyReturn glusterSnapshotDelete(String snapshotName) {
        JsonRpcRequest request =
                new RequestBuilder("GlusterSnapshot.delete").withOptionalParameter("snapName", snapshotName)
                        .build();
        Map<String, Object> response =
                new FutureMap(this.client, request);
        return new StatusOnlyReturn(response);
    }

    @Override
    public StatusOnlyReturn glusterVolumeSnapshotDeleteAll(String volumeName) {
        JsonRpcRequest request =
                new RequestBuilder("GlusterVolume.snapshotDeleteAll").withParameter("volumeName", volumeName)
                    .build();
        Map<String, Object> response = new FutureMap(this.client, request);
        return new StatusOnlyReturn(response);
    }

    @Override
    public StatusOnlyReturn glusterSnapshotActivate(String snapshotName, boolean force) {
        JsonRpcRequest request =
                new RequestBuilder("GlusterSnapshot.activate").withParameter("snapName", snapshotName)
                        .withParameter("force", force)
                        .build();
        Map<String, Object> response =
                new FutureMap(this.client, request);
        return new StatusOnlyReturn(response);
    }

    @Override
    public StatusOnlyReturn glusterSnapshotDeactivate(String snapshotName) {
        JsonRpcRequest request =
                new RequestBuilder("GlusterSnapshot.deactivate").withParameter("snapName", snapshotName)
                        .build();
        Map<String, Object> response =
                new FutureMap(this.client, request);
        return new StatusOnlyReturn(response);
    }

    @Override
    public StatusOnlyReturn glusterSnapshotRestore(String snapshotName) {
        JsonRpcRequest request =
                new RequestBuilder("GlusterSnapshot.restore").withParameter("snapName", snapshotName)
                    .build();
        Map<String, Object> response = new FutureMap(this.client, request);
        return new StatusOnlyReturn(response);
    }

    @Override
    public GlusterVolumeSnapshotCreateReturn glusterVolumeSnapshotCreate(String volumeName,
            String snapshotName,
            String description,
            boolean force) {
        JsonRpcRequest request =
                new RequestBuilder("GlusterVolume.snapshotCreate").withParameter("volumeName", volumeName)
                        .withParameter("snapName", snapshotName)
                        .withOptionalParameter("snapDescription", description)
                        .withParameter("force", force)
                        .build();
        Map<String, Object> response =
                new FutureMap(this.client, request).withIgnoreResponseKey();
        return new GlusterVolumeSnapshotCreateReturn(response);
    }

    @Override
    public StatusOnlyReturn glusterVolumeSnapshotConfigSet(String volumeName, String configName, String configValue) {
        JsonRpcRequest request =
                new RequestBuilder("GlusterVolume.snapshotConfigSet").withParameter("volumeName", volumeName)
                        .withParameter("optionName", configName)
                        .withParameter("optionValue", configValue)
                        .build();

        Map<String, Object> response = new FutureMap(this.client, request).withIgnoreResponseKey();
        return new StatusOnlyReturn(response);
    }

    @Override
    public StatusOnlyReturn glusterSnapshotConfigSet(String configName, String configValue) {
        JsonRpcRequest request =
                new RequestBuilder("GlusterSnapshot.configSet").withParameter("optionName", configName)
                        .withParameter("optionValue", configValue)
                        .build();

        Map<String, Object> response = new FutureMap(this.client, request).withIgnoreResponseKey();
        return new StatusOnlyReturn(response);
    }

    @Override
    public StorageDeviceListReturn glusterStorageDeviceList() {
        JsonRpcRequest request = new RequestBuilder("GlusterHost.storageDevicesList").build();
        Map<String, Object> response = new FutureMap(this.client, request).withIgnoreResponseKey();
        return new StorageDeviceListReturn(response);
    }

    @Override
    public OneStorageDeviceReturn glusterCreateBrick(String lvName,
            String mountPoint,
            Map<String, Object> raidParams, String fsType,
            String[] storageDevices) {
        JsonRpcRequest request = new RequestBuilder("GlusterHost.createBrick").withParameter("name", lvName)
                .withParameter("mountPoint", mountPoint)
                .withParameter("devList", storageDevices)
                .withParameter("fsType", fsType)
                .withOptionalParameterAsMap("raidParams", raidParams).build();

        Map<String, Object> response = new FutureMap(this.client, request).withIgnoreResponseKey();
        return new OneStorageDeviceReturn(response);
    }

    @Override
    public StatusOnlyReturn hostdevChangeNumvfs(String deviceName, int numOfVfs) {
        JsonRpcRequest request =
                new RequestBuilder("Host.hostdevChangeNumvfs").withParameter("deviceName", deviceName)
                        .withParameter("numvfs", numOfVfs)
                        .build();
        Map<String, Object> response = new FutureMap(this.client, request);
        return new StatusOnlyReturn(response);
    }

    @Override
    public StatusOnlyReturn convertVmFromExternalSystem(String uri,
            String username,
            String password,
            Map<String, Object> vm,
            String jobUUID) {
        JsonRpcRequest request =
                new RequestBuilder("Host.convertExternalVm")
        .withParameter("uri", uri)
        .withParameter("username", username)
        .withParameter("password", password)
        .withParameter("vminfo", vm)
        .withParameter("jobid", jobUUID)
        .build();
        Map<String, Object> response = new FutureMap(this.client, request);
        return new StatusOnlyReturn(response);
    }

    @Override
    public StatusOnlyReturn convertVmFromOva(String ovaPath, Map<String, Object> vm, String jobUUID) {
        JsonRpcRequest request =
                new RequestBuilder("Host.convertExternalVmFromOva")
        .withParameter("ova_path", ovaPath)
        .withParameter("vminfo", vm)
        .withParameter("jobid", jobUUID)
        .build();
        Map<String, Object> response = new FutureMap(this.client, request);
        return new StatusOnlyReturn(response);
    }

    @Override
    public OvfReturn getConvertedVm(String jobUUID) {
        JsonRpcRequest request =
                new RequestBuilder("Host.getConvertedVm")
        .withParameter("jobid", jobUUID)
        .build();
        Map<String, Object> response =
                new FutureMap(this.client, request)
        .withResponseKey("ovf")
        .withResponseType(String.class);
        return new OvfReturn(response);
    }

    @Override
    public StatusOnlyReturn deleteV2VJob(String jobUUID) {
        JsonRpcRequest request =
                new RequestBuilder("Host.deleteV2VJob")
        .withParameter("jobid", jobUUID)
        .build();
        Map<String, Object> response = new FutureMap(this.client, request);
        return new StatusOnlyReturn(response);
    }

    @Override
    public StatusOnlyReturn abortV2VJob(String jobUUID) {
        JsonRpcRequest request =
                new RequestBuilder("Host.abortV2VJob")
        .withParameter("jobid", jobUUID)
        .build();
        Map<String, Object> response = new FutureMap(this.client, request);
        return new StatusOnlyReturn(response);
    }

    @Override
    public StatusOnlyReturn glusterSnapshotScheduleOverride(boolean force) {
        JsonRpcRequest request =
                new RequestBuilder("GlusterVolume.snapshotScheduleOverride").withParameter("force", force).build();

        Map<String, Object> response = new FutureMap(this.client, request).withIgnoreResponseKey();
        return new StatusOnlyReturn(response);
    }

    @Override
    public StatusOnlyReturn glusterSnapshotScheduleReset() {
        JsonRpcRequest request =
                new RequestBuilder("GlusterVolume.snapshotScheduleReset").build();

        Map<String, Object> response = new FutureMap(this.client, request);
        return new StatusOnlyReturn(response);
    }

    public StatusOnlyReturn registerSecrets(Map<String, String>[] libvirtSecrets, boolean clearUnusedSecrets) {
        JsonRpcRequest request =
                new RequestBuilder("Host.registerSecrets").withParameter("secrets", libvirtSecrets)
                        .withParameter("clear", clearUnusedSecrets)
                        .build();
        Map<String, Object> response = new FutureMap(this.client, request);
        return new StatusOnlyReturn(response);
    }

    @Override
    public StatusOnlyReturn unregisterSecrets(String[] libvirtSecretsUuids) {
        JsonRpcRequest request =
                new RequestBuilder("Host.unregisterSecrets").withParameter("uuids", libvirtSecretsUuids)
                        .build();
        Map<String, Object> response = new FutureMap(this.client, request);
        return new StatusOnlyReturn(response);
    }

    @Override
    public StatusOnlyReturn freeze(String vmId) {
        JsonRpcRequest request =
                new RequestBuilder("VM.freeze").withParameter("vmID", vmId)
                        .build();
        Map<String, Object> response = new FutureMap(this.client, request);
        return new StatusOnlyReturn(response);
    }

    @Override
    public StatusOnlyReturn thaw(String vmId) {
        JsonRpcRequest request =
                new RequestBuilder("VM.thaw").withParameter("vmID", vmId)
                        .build();
        Map<String, Object> response = new FutureMap(this.client, request);
        return new StatusOnlyReturn(response);
    }

    @Override
    public StatusOnlyReturn isolateVolume(String sdUUID, String srcImageID, String dstImageID, String volumeID) {
        JsonRpcRequest request =
                new RequestBuilder("SDM.isolateVolume")
                        .withParameter("storagedomainID", sdUUID)
                        .withParameter("srcImageID", srcImageID)
                        .withParameter("dstImageID", dstImageID)
                        .withParameter("volumeID", volumeID).build();
        Map<String, Object> response =
                new FutureMap(this.client, request);
        return new StatusOnlyReturn(response);
    }

    @Override
    public StatusOnlyReturn wipeVolume(String sdUUID, String imgUUID, String volUUID) {
        JsonRpcRequest request =
                new RequestBuilder("SDM.wipeVolume")
                 .withParameter("storagedomainID", sdUUID)
                .withParameter("imageID", imgUUID)
                .withParameter("volumeID", volUUID)
                        .build();
        Map<String, Object> response =
                new FutureMap(this.client, request);
        return new StatusOnlyReturn(response);
    }

    @Override
    public OneVmReturn getExternalVmFromOva(String ovaPath) {
        JsonRpcRequest request =
                new RequestBuilder("Host.getExternalVmFromOva")
                        .withParameter("ova_path", ovaPath)
                        .build();
        Map<String, Object> response =
                new FutureMap(this.client, request).withResponseKey("vmList")
                .withResponseType(Object[].class);
        return new OneVmReturn(response);
    }

    @Override
    public StatusOnlyReturn refreshVolume(String sdUUID, String spUUID, String imgUUID, String volUUID) {
        JsonRpcRequest request =
                new RequestBuilder("Volume.refresh")
                        .withParameter("storagepoolID", spUUID)
                        .withParameter("storagedomainID", sdUUID)
                        .withParameter("imageID", imgUUID)
                        .withParameter("volumeID", volUUID)
                        .build();
        Map<String, Object> response = new FutureMap(this.client, request);
        return new StatusOnlyReturn(response);
    }

    @Override
    public VolumeInfoReturn getVolumeInfo(String sdUUID, String spUUID, String imgUUID, String volUUID) {
        JsonRpcRequest request =
                new RequestBuilder("Volume.getInfo")
                        .withParameter("storagepoolID", spUUID)
                        .withParameter("storagedomainID", sdUUID)
                        .withParameter("imageID", imgUUID)
                        .withParameter("volumeID", volUUID)
                        .build();
        Map<String, Object> response = new FutureMap(this.client, request);
        return new VolumeInfoReturn(response);
    }

    @Override
    public QemuImageInfoReturn getQemuImageInfo(String sdUUID, String spUUID, String imgUUID, String volUUID) {
        JsonRpcRequest request =
                new RequestBuilder("Volume.getQemuImageInfo")
                        .withParameter("storagepoolID", spUUID)
                        .withParameter("storagedomainID", sdUUID)
                        .withParameter("imageID", imgUUID)
                        .withParameter("volumeID", volUUID)
                        .build();
        Map<String, Object> response = new FutureMap(this.client, request);
        return new QemuImageInfoReturn(response);
    }

    @Override
    public StatusOnlyReturn glusterStopProcesses() {
        JsonRpcRequest request =
                new RequestBuilder("GlusterHost.processesStop").build();
        Map<String, Object> response = new FutureMap(this.client, request);
        return new StatusOnlyReturn(response);
    }

    @Override
    public StatusOnlyReturn sparsifyVolume(String jobId, Map<String, Object> volumeAddress) {
        JsonRpcRequest request =
                new RequestBuilder("SDM.sparsify_volume")
                        .withParameter("job_id", jobId)
                        .withParameter("vol_info", volumeAddress)
                        .build();
        Map<String, Object> response = new FutureMap(this.client, request);
        return new StatusOnlyReturn(response);
    }

    @Override
    public StatusOnlyReturn amendVolume(String jobId, Map<String, Object> volInfo, Map<String, Object> qcow2_attr) {
        JsonRpcRequest request =
                new RequestBuilder("SDM.amend_volume")
                        .withParameter("job_id", jobId)
                        .withParameter("vol_info", volInfo)
                        .withParameter("qcow2_attr", qcow2_attr)
                        .build();
        Map<String, Object> response = new FutureMap(this.client, request);
        return new StatusOnlyReturn(response);
    }

    @Override
    public StatusOnlyReturn sealDisks(String templateId, String jobId, String storagePoolId, List<Map<String, Object>> images) {
        JsonRpcRequest request =
                new RequestBuilder("VM.seal")
                        .withParameter("vmID", templateId)
                        .withParameter("job_id", jobId)
                        .withParameter("sp_id", storagePoolId)
                        .withOptionalParameterAsList("images", images)
                        .build();
        Map<String, Object> response = new FutureMap(this.client, request);
        return new StatusOnlyReturn(response);
    }

}

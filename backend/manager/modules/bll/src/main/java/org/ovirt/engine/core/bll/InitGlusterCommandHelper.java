package org.ovirt.engine.core.bll;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang.Validate;
import org.ovirt.engine.core.bll.interfaces.BackendInternal;
import org.ovirt.engine.core.bll.utils.GlusterUtil;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.action.gluster.SyncGlusterStorageDevicesParameter;
import org.ovirt.engine.core.common.businessentities.NonOperationalReason;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.VDSStatus;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterServer;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterServerInfo;
import org.ovirt.engine.core.common.businessentities.gluster.PeerStatus;
import org.ovirt.engine.core.common.config.Config;
import org.ovirt.engine.core.common.config.ConfigValues;
import org.ovirt.engine.core.common.errors.EngineException;
import org.ovirt.engine.core.common.vdscommands.VDSCommandType;
import org.ovirt.engine.core.common.vdscommands.VDSParametersBase;
import org.ovirt.engine.core.common.vdscommands.VDSReturnValue;
import org.ovirt.engine.core.common.vdscommands.VdsIdVDSCommandParametersBase;
import org.ovirt.engine.core.common.vdscommands.gluster.AddGlusterServerVDSParameters;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.AuditLogDirector;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.AuditLogableBase;
import org.ovirt.engine.core.dao.VdsDao;
import org.ovirt.engine.core.dao.gluster.GlusterServerDao;
import org.ovirt.engine.core.di.Injector;
import org.ovirt.engine.core.utils.lock.EngineLock;
import org.ovirt.engine.core.vdsbroker.ResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class InitGlusterCommandHelper {

    private final ResourceManager resourceManager;
    private final GlusterServerDao glusterServerDao;
    private final AuditLogDirector auditLogDirector;
    private static Integer MAX_RETRIES_GLUSTER_PROBE_STATUS;
    private final BackendInternal backend;
    private final VdsDao vdsDao;

    private Logger log = LoggerFactory.getLogger(getClass());

    @Inject
    private GlusterUtil glusterUtil;

    @Inject
    public InitGlusterCommandHelper(ResourceManager resourceManager,
            GlusterServerDao glusterServerDao,
            VdsDao vdsDao,
            BackendInternal backend,
            AuditLogDirector auditLogDirector) {
        Validate.notNull(resourceManager, "resourceManager can not be null");
        Validate.notNull(glusterServerDao, "glusterServerDao can not be null");
        Validate.notNull(vdsDao, "vdsDao can not be null");
        Validate.notNull(auditLogDirector, "auditLogDirector can not be null");
        Validate.notNull(backend, "backend can not be null");

        this.resourceManager = resourceManager;
        this.glusterServerDao = glusterServerDao;
        this.vdsDao = vdsDao;
        this.backend = backend;
        this.auditLogDirector = auditLogDirector;
    }

    public boolean initGlusterHost(VDS vds) {
        VDSReturnValue returnValue = runVdsCommand(VDSCommandType.GetGlusterHostUUID,
                new VdsIdVDSCommandParametersBase(vds.getId()));
        if (returnValue.getSucceeded() && returnValue.getReturnValue() != null) {
            Guid addedServerUuid = Guid.createGuidFromString((String) returnValue.getReturnValue());
            if (hostUuidExists(vds, addedServerUuid)) {
                setNonOperational(vds, NonOperationalReason.GLUSTER_HOST_UUID_ALREADY_EXISTS, null);
                return false;
            }
            saveGlusterHostUuid(vds, addedServerUuid);
        }
        else {
            setNonOperational(vds, NonOperationalReason.GLUSTER_HOST_UUID_NOT_FOUND, null);
            return false;
        }
        refreshGlusterStorageDevices(vds);
        boolean ret = initGlusterPeerProcess(vds);
        glusterServerDao.updatePeerStatus(vds.getId(), ret ? PeerStatus.CONNECTED : PeerStatus.DISCONNECTED);
        return ret;
    }

    private void refreshGlusterStorageDevices(VDS vds) {
        try {
            backend.runInternalAction(VdcActionType.SyncStorageDevices,
                    new SyncGlusterStorageDevicesParameter(vds.getId(), true));
        } catch (EngineException e) {
            log.error("Could not refresh storage devices from gluster host '{}'", vds.getName());
        }
    }

    /**
     *
     * This method executes a "gluster peer probe" to add the newly added host to the cluster - this
     * is done only if there's another UP server in cluster and the host being added is not already
     * part of the UP server's peer list.
     * Also, acquiring a wait lock only during a gluster peer process (wait as there's periodic job that also
     * acquires lock.
     */
    private boolean initGlusterPeerProcess(VDS vds) {
       // If "gluster peer probe" and "gluster peer status" are executed simultaneously, the results
       // are unpredictable. Hence locking the cluster to ensure the sync job does not lead to race
       // condition.
        try (EngineLock lock = glusterUtil.acquireGlusterLockWait(vds.getClusterId())) {
            Map<String, String> customLogValues = new HashMap<>();
            List<VDS> vdsList = vdsDao.getAllForClusterWithStatus(vds.getClusterId(), VDSStatus.Up);
            // If the cluster already having Gluster servers, get an up server
            if (vdsList != null && vdsList.size() > 0) {
                VDS upServer = null;
                for (VDS existingVds : vdsList) {
                    if (!vds.getId().equals(existingVds.getId())) {
                        upServer = existingVds;
                        break;
                    }
                }

                // If new server is not part of the existing gluster peers, add into peer group
                if (upServer != null) {
                    List<GlusterServerInfo> glusterServers = getGlusterPeers(upServer.getId());
                    customLogValues.put("Server", upServer.getHostName());
                    if (glusterServers.size() == 0) {
                        customLogValues.put("Command", "gluster peer status");
                        setNonOperational(vds, NonOperationalReason.GLUSTER_COMMAND_FAILED, customLogValues);
                        return false;
                    }
                    else if (!glusterUtil.isHostExists(glusterServers, vds)) {
                        if (!glusterPeerProbe(vds, upServer.getId(), vds.getHostName())) {
                            customLogValues.put("Command", "gluster peer probe " + vds.getHostName());
                            setNonOperational(vds, NonOperationalReason.GLUSTER_COMMAND_FAILED, customLogValues);
                            return false;
                        }

                        int retries = 0;
                        while (retries < getMaxRetriesGlusterProbeStatus()) {
                            // though gluster peer probe succeeds, it takes some time for the host to be
                            // listed as a peer. Return success only when the host is acknowledged as peer
                            // from another upServer.
                            VDS newUpServer =  getNewUpServer(vds, upServer);
                            if (newUpServer == null) {
                                //there's no other up server. so there's no issue with peer status results
                                return true;
                            }
                            List<GlusterServerInfo> newGlusterServers = getGlusterPeers(newUpServer.getId());
                            if (!glusterUtil.isHostExists(newGlusterServers, vds)) {
                                log.info("Failed to find host '{}' in gluster peer list from '{}' on attempt {}",
                                        vds, newUpServer, ++retries);
                                // if num of attempts done
                                if (retries == getMaxRetriesGlusterProbeStatus()) {
                                    customLogValues.put("Command", "gluster peer status " + vds.getHostName());
                                    setNonOperational(vds, NonOperationalReason.GLUSTER_COMMAND_FAILED, customLogValues);
                                    return false;
                                }
                                try { //give time for gluster peer probe to propogate to servers.
                                    Thread.sleep(1000);
                                } catch (Exception e) {
                                    log.error(e.getMessage());
                                    break;
                                }
                            } else {
                                return true;
                            }
                        }
                    }
                }
            }
            return true;
        }
    }

    private void saveGlusterHostUuid(VDS vds, Guid addedServerUuid) {
        GlusterServer glusterServer = glusterServerDao.getByServerId(vds.getId());
        if (glusterServer == null) {
            glusterServer = new GlusterServer();
            glusterServer.setId(vds.getId());
            glusterServer.setGlusterServerUuid(addedServerUuid);
            glusterServer.setPeerStatus(PeerStatus.CONNECTED);
            glusterServerDao.save(glusterServer);
        } else if (!glusterServer.getGlusterServerUuid().equals(addedServerUuid)) {
            // it's possible that the server is re-installed and gluster uuid has changed,
            // update this in the database.
            glusterServer.setGlusterServerUuid(addedServerUuid);
            glusterServerDao.update(glusterServer);
        }
    }

    private VDS getNewUpServer(VDS vds, VDS upServer) {
        List<VDS> vdsList = vdsDao.getAllForClusterWithStatus(vds.getClusterId(), VDSStatus.Up);
        VDS newUpServer = null;
        for (VDS existingVds : vdsList) {
            if (!vds.getId().equals(existingVds.getId()) && !upServer.getId().equals(existingVds.getId())) {
                newUpServer = vds;
                break;
            }
        }
        return newUpServer;
    }


    private boolean hostUuidExists(VDS vds, Guid addedServerUuid) {
        GlusterServer glusterServer = glusterServerDao.getByGlusterServerUuid(addedServerUuid);
        if (glusterServer == null || glusterServer.getId().equals(vds.getId())) {
            return false;
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private List<GlusterServerInfo> getGlusterPeers(Guid upServerId) {
        List<GlusterServerInfo> glusterServers = new ArrayList<>();
        VDSReturnValue returnValue = runVdsCommand(VDSCommandType.GlusterServersList,
                        new VdsIdVDSCommandParametersBase(upServerId));
        if (!returnValue.getSucceeded()) {
            AuditLogableBase logable = Injector.injectMembers(new AuditLogableBase(upServerId));
            logable.addCustomValue("ErrorMessage", returnValue.getVdsError().getMessage());
            logable.updateCallStackFromThrowable(returnValue.getExceptionObject());
            auditLogDirector.log(logable, AuditLogType.GLUSTER_SERVERS_LIST_FAILED);
        } else {
            glusterServers = (List<GlusterServerInfo>) returnValue.getReturnValue();
        }
        return glusterServers;
    }

    private boolean glusterPeerProbe(VDS vds, Guid upServerId, String newServerName) {
        try {
            VDSReturnValue returnValue = runVdsCommand(VDSCommandType.AddGlusterServer,
                    new AddGlusterServerVDSParameters(upServerId, newServerName));
            if (!returnValue.getSucceeded()) {
                AuditLogableBase logable = Injector.injectMembers(new AuditLogableBase(vds.getId()));
                logable.addCustomValue("ErrorMessage", returnValue.getVdsError().getMessage());
                logable.updateCallStackFromThrowable(returnValue.getExceptionObject());
                auditLogDirector.log(logable, AuditLogType.GLUSTER_SERVER_ADD_FAILED);
            }
            return returnValue.getSucceeded();
        } catch (Exception e) {
            log.error("Could not peer probe the gluster server '{}': {}",
                    vds.getHostName(),
                    e.getMessage());
            log.debug("Exception", e);
            return false;
        }
    }

    private VDSReturnValue runVdsCommand(VDSCommandType commandType, VDSParametersBase params) {
        return resourceManager.runVdsCommand(commandType, params);
    }

    private void setNonOperational(VDS host, NonOperationalReason reason, Map<String, String> customLogValues) {
        resourceManager.getEventListener().vdsNonOperational(host.getId(), reason, true, Guid.Empty, customLogValues);
    }

    private int getMaxRetriesGlusterProbeStatus() {
        if (MAX_RETRIES_GLUSTER_PROBE_STATUS == null) {
            MAX_RETRIES_GLUSTER_PROBE_STATUS = Config.<Integer> getValue(ConfigValues.GlusterPeerStatusRetries);
        }
        return MAX_RETRIES_GLUSTER_PROBE_STATUS;
    }
}

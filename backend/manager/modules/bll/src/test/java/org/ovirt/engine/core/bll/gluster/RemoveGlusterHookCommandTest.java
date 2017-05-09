package org.ovirt.engine.core.bll.gluster;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner.Silent;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.action.gluster.GlusterHookManageParameters;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.VDSStatus;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterHookEntity;
import org.ovirt.engine.core.common.errors.EngineError;
import org.ovirt.engine.core.common.errors.EngineMessage;
import org.ovirt.engine.core.common.errors.VDSError;
import org.ovirt.engine.core.common.vdscommands.VDSCommandType;
import org.ovirt.engine.core.common.vdscommands.VDSReturnValue;
import org.ovirt.engine.core.common.vdscommands.gluster.GlusterHookVDSParameters;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dao.VdsDao;


@RunWith(Silent.class)
public class RemoveGlusterHookCommandTest extends GlusterHookCommandTest<RemoveGlusterHookCommand> {
    @Override
    protected RemoveGlusterHookCommand createCommand() {
        return new RemoveGlusterHookCommand(new GlusterHookManageParameters(HOOK_ID), null);
    }

    @Mock
    private VdsDao vdsDao;

    private void setUpMocksForRemove() {
        setUpMocksForRemove(true);
    }

    private void setUpMocksForRemove(boolean hookFound) {
        setUpMocksForRemove(hookFound, getHookEntity(), VDSStatus.Up);
    }

    private void setUpMocksForRemove(boolean hookFound, GlusterHookEntity hook, VDSStatus status) {
        setupMocks(hookFound, hook);
        when(vdsDao.getAllForCluster(any(Guid.class))).thenReturn(getServers(status));
    }

    private List<VDS> getServers(VDSStatus status) {
        List<VDS> servers = new ArrayList<>();
        servers.add(getServer(GUIDS[0], "gfs1", CLUSTER_ID, status));
        servers.add(getServer(GUIDS[1], "gfs2", CLUSTER_ID, status));
        servers.add(getServer(GUIDS[2], "gfs3", CLUSTER_ID, status));
        servers.add(getServer(GUIDS[3], "gfs4", CLUSTER_ID, status));
        return servers;
    }

    private void mockBackend(boolean succeeded, EngineError errorCode) {
        doReturn(backend).when(cmd).getBackend();

        VDSReturnValue vdsReturnValue = new VDSReturnValue();
        vdsReturnValue.setSucceeded(succeeded);
        if (!succeeded) {
            vdsReturnValue.setVdsError(new VDSError(errorCode, ""));
        }
        when(vdsBrokerFrontend.runVdsCommand(eq(VDSCommandType.RemoveGlusterHook), any(GlusterHookVDSParameters.class))).thenReturn(vdsReturnValue);
     }

    @Test
    public void executeCommand() {
        setUpMocksForRemove();
        mockBackend(true, null);
        cmd.executeCommand();
        verify(hooksDao, times(1)).remove(any(Guid.class));
        assertEquals(AuditLogType.GLUSTER_HOOK_REMOVED, cmd.getAuditLogTypeValue());
    }


    @Test
    public void executeCommandWhenFailed() {
        setUpMocksForRemove();
        mockBackend(false, EngineError.GlusterHookRemoveFailed);
        cmd.executeCommand();
        verify(hooksDao, never()).remove(any(Guid.class));
        assertEquals(AuditLogType.GLUSTER_HOOK_REMOVE_FAILED, cmd.getAuditLogTypeValue());
    }

    @Test
    public void validateSucceeds() {
        setUpMocksForRemove();
        assertTrue(cmd.validate());
    }

    @Test
    public void validateFailsOnNullHookId() {
        cmd.getParameters().setHookId(null);
        setUpMocksForRemove();
        assertFalse(cmd.validate());
        assertTrue(cmd.getReturnValue().getValidationMessages().contains(EngineMessage.ACTION_TYPE_FAILED_GLUSTER_HOOK_ID_IS_REQUIRED.toString()));
    }

    @Test
    public void validateFailsOnNoHook() {
        setUpMocksForRemove(false);
        assertFalse(cmd.validate());
        assertTrue(cmd.getReturnValue().getValidationMessages().contains(EngineMessage.ACTION_TYPE_FAILED_GLUSTER_HOOK_DOES_NOT_EXIST.toString()));
    }

    @Test
    public void validateFailsOnServerNotUp() {
        setUpMocksForRemove(true, getHookEntity(), VDSStatus.Down);
        assertFalse(cmd.validate());
        assertTrue(cmd.getReturnValue().getValidationMessages().contains(EngineMessage.ACTION_TYPE_FAILED_SERVER_STATUS_NOT_UP.toString()));
    }

}

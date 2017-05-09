package org.ovirt.engine.core.bll.gluster;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.ovirt.engine.core.bll.BaseCommandTest;
import org.ovirt.engine.core.bll.gluster.tasks.GlusterTaskUtils;
import org.ovirt.engine.core.common.action.gluster.GlusterVolumeActionParameters;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.VDSStatus;
import org.ovirt.engine.core.common.businessentities.gluster.AccessProtocol;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterStatus;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterVolumeEntity;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterVolumeType;
import org.ovirt.engine.core.common.businessentities.gluster.TransportType;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dao.gluster.GlusterVolumeDao;

public class StopGlusterVolumeCommandTest extends BaseCommandTest {
    @Spy
    private GlusterTaskUtils glusterTaskUtils;

    @Mock
    GlusterVolumeDao volumeDao;

    private Guid stoppedVolumeId = new Guid("8bc6f108-c0ef-43ab-ba20-ec41107220f5");
    private Guid startedVolumeId = new Guid("b2cb2f73-fab3-4a42-93f0-d5e4c069a43e");
    private Guid CLUSTER_ID = new Guid("b399944a-81ab-4ec5-8266-e19ba7c3c9d1");

    /**
     * The command under test.
     */
    @Spy
    @InjectMocks
    private StopGlusterVolumeCommand cmd = new StopGlusterVolumeCommand(new GlusterVolumeActionParameters(), null);

    @Before
    public void prepareMocks() {
        doReturn(getVds(VDSStatus.Up)).when(cmd).getUpServer();
        doReturn(getGlusterVolume(stoppedVolumeId)).when(volumeDao).getById(stoppedVolumeId);
        doReturn(getGlusterVolume(startedVolumeId)).when(volumeDao).getById(startedVolumeId);
    }

    private VDS getVds(VDSStatus status) {
        VDS vds = new VDS();
        vds.setId(Guid.newGuid());
        vds.setVdsName("gfs1");
        vds.setClusterId(CLUSTER_ID);
        vds.setStatus(status);
        return vds;
    }

    private GlusterVolumeEntity getGlusterVolume(Guid volumeId) {
        GlusterVolumeEntity volumeEntity = new GlusterVolumeEntity();
        volumeEntity.setId(volumeId);
        volumeEntity.setName("test-vol");
        volumeEntity.addAccessProtocol(AccessProtocol.GLUSTER);
        volumeEntity.addTransportType(TransportType.TCP);
        volumeEntity.setVolumeType(GlusterVolumeType.DISTRIBUTE);
        volumeEntity.setStatus(volumeId.equals(startedVolumeId) ? GlusterStatus.UP : GlusterStatus.DOWN);
        volumeEntity.setClusterId(CLUSTER_ID);
        return volumeEntity;
    }

    @Test
    public void validateSucceeds() {
        cmd.setGlusterVolumeId(startedVolumeId);
        assertTrue(cmd.validate());
    }

    @Test
    public void validateFails() {
        cmd.setGlusterVolumeId(stoppedVolumeId);
        assertFalse(cmd.validate());
    }

    @Test
    public void validateFailsOnNull() {
        assertFalse(cmd.validate());
    }
}

package org.ovirt.engine.core.bll.gluster;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.ovirt.engine.core.bll.BaseCommandTest;
import org.ovirt.engine.core.bll.utils.GlusterUtil;
import org.ovirt.engine.core.common.action.gluster.GlusterVolumeGeoRepSessionParameters;
import org.ovirt.engine.core.common.businessentities.Cluster;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.VDSStatus;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterGeoRepSession;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterStatus;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterVolumeEntity;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.Version;
import org.ovirt.engine.core.dao.ClusterDao;
import org.ovirt.engine.core.dao.VdsDao;
import org.ovirt.engine.core.dao.gluster.GlusterGeoRepDao;
import org.ovirt.engine.core.dao.gluster.GlusterVolumeDao;

public class CreateGlusterVolumeGeoRepSessionCommandTest extends BaseCommandTest {
    private static final Version SUPPORTED_VERSION = Version.v3_6;

    private final String slaveVolumeName = "slaveVol";
    private final Guid masterVolumeId = Guid.newGuid();

    @Mock
    GlusterVolumeDao volumeDao;

    @Mock
    VdsDao vdsDao;

    @Mock
    ClusterDao clusterDao;

    @Mock
    GlusterGeoRepDao geoRepDao;

    @Mock
    protected Cluster cluster;

    @Mock
    private GlusterUtil glusterUtil;

    @Mock
    protected GlusterVolumeEntity volume;

    @Mock
    protected VDS vds;

    @Spy
    @InjectMocks
    CreateGlusterVolumeGeoRepSessionCommand command =
            new CreateGlusterVolumeGeoRepSessionCommand
                    (new GlusterVolumeGeoRepSessionParameters
                            (masterVolumeId, slaveVolumeName, Guid.newGuid(), null, null, false),
                     null);


    @Test
    public void commandSucceeds() {
        doReturn(SUPPORTED_VERSION).when(cluster).getCompatibilityVersion();
        doReturn(volume).when(command).getSlaveVolume();
        doReturn(vds).when(command).getSlaveHost();
        assertTrue(command.validate());
    }

    @Before
    public void prepareMocks() {
        doReturn(volume).when(volumeDao).getById(masterVolumeId);
        doReturn(GlusterStatus.UP).when(volume).getStatus();
        doReturn(cluster).when(command).getCluster();
        doReturn(vds).when(command).getUpServer();
        doReturn(VDSStatus.Up).when(vds).getStatus();
    }

    @Test
    public void commandFailsSlaveVolumeNotMonitoredByOvirt() {
        doReturn(SUPPORTED_VERSION).when(cluster).getCompatibilityVersion();
        doReturn(vds).when(command).getSlaveHost();
        assertFalse(command.validate());
    }

    @Test
    public void commandFailsSessionExists() {
        doReturn(volume).when(command).getSlaveVolume();
        doReturn(vds).when(command).getSlaveHost();
        doReturn(SUPPORTED_VERSION).when(cluster).getCompatibilityVersion();
        doReturn(new GlusterGeoRepSession()).when(geoRepDao).getGeoRepSession(any(), any(), any());
        assertFalse(command.validate());
    }

    @Test
    public void commandFailsSlaveHostInvalid() {
        doReturn(vds).when(command).getUpServer();
        doReturn(SUPPORTED_VERSION).when(cluster).getCompatibilityVersion();
        doReturn(volume).when(command).getSlaveVolume();
        assertFalse(command.validate());
    }
}

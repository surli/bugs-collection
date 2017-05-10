package org.ovirt.engine.core.bll.gluster;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner.Silent;
import org.ovirt.engine.core.common.businessentities.Cluster;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterBrickEntity;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterStatus;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterVolumeEntity;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterVolumeType;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dao.gluster.GlusterBrickDao;
import org.ovirt.engine.core.dao.gluster.GlusterVolumeDao;

@RunWith(Silent.class)
public class GlusterHostValidatorTest {

    private static final Guid CLUSTER_ID = new Guid("ae956031-6be2-43d6-bb8f-5191c9253314");
    private static final Guid VOL_ID_1 = new Guid("0c3f45f6-3fe9-4b35-a30c-be0d1a835ea8");
    private static final Guid VOL_ID_2 = new Guid("b2cb2f73-fab3-4a42-93f0-d5e4c069a43e");
    private static final Guid SERVER_ID_1 = new Guid("23f6d691-5dfb-472b-86dc-9e1d2d3c18f3");
    private static final Guid SERVER_ID_2 = new Guid("2001751e-549b-4e7a-aff6-32d36856c125");
    private static final Guid SERVER_ID_3 = new Guid("2001751e-549b-4e7a-aff6-32d36856c126");
    private static final Guid DUMMY_SERVER_ID = new Guid("2001751e-549b-4e7a-aff6-32d36856c127");

    @Mock
    private GlusterVolumeDao volumeDao;
    @Mock
    private GlusterBrickDao brickDao;

    private GlusterHostValidator hostValidator;

    @Before
    public void setUp() throws Exception {
        setupMock();
        hostValidator = new GlusterHostValidator(volumeDao, brickDao);
    }

    private void setupMock() throws Exception {
        doReturn(getBricksFromServer(SERVER_ID_1, GlusterStatus.UP)).when(brickDao)
                .getGlusterVolumeBricksByServerId(SERVER_ID_1);
        doReturn(getBricksFromServer(SERVER_ID_2, GlusterStatus.UP)).when(brickDao)
                .getGlusterVolumeBricksByServerId(SERVER_ID_2);
        doReturn(getBricksFromServer(SERVER_ID_3, GlusterStatus.UP)).when(brickDao)
                .getGlusterVolumeBricksByServerId(SERVER_ID_3);
        doReturn(getGlusterVolumes(CLUSTER_ID, GlusterStatus.UP)).when(volumeDao).getByClusterId(CLUSTER_ID);
    }

    @Test
    public void testCheckGlusterQuorumWithoutGluster() {
        Cluster cluster = getCluster(false, CLUSTER_ID);
        Iterable<Guid> hostIds = new LinkedList<>();
        assertTrue("Quorum checks runs for Cluster without gluster service",
                hostValidator.checkGlusterQuorum(cluster, hostIds).isEmpty());
    }

    @Test
    public void testCheckGlusterQuorumWithoutBricks() {
        Cluster cluster = getCluster(true, CLUSTER_ID);
        Iterable<Guid> hostIds = Arrays.asList(DUMMY_SERVER_ID, SERVER_ID_1);
        assertTrue(hostValidator.checkGlusterQuorum(cluster, hostIds).isEmpty());
    }

    @Test
    public void testCheckGlusterQuorumWithoutRequiredVolumeOptions() {
        Cluster cluster = getCluster(true, CLUSTER_ID);
        Iterable<Guid> hostIds = Arrays.asList(SERVER_ID_1, SERVER_ID_2);
        // Reset the quroum related volume options
        List<GlusterVolumeEntity> glusterVolumes = getGlusterVolumes(CLUSTER_ID, GlusterStatus.UP);
        for (GlusterVolumeEntity volume : glusterVolumes) {
            volume.setOptions("");
        }
        doReturn(glusterVolumes).when(volumeDao).getByClusterId(CLUSTER_ID);
        assertTrue(hostValidator.checkGlusterQuorum(cluster, hostIds).isEmpty());
    }

    @Test
    public void testCheckGlusterQuorumTwoServersUp() {
        Cluster cluster = getCluster(true, CLUSTER_ID);
        Iterable<Guid> hostIds = Arrays.asList(SERVER_ID_1);
        assertTrue(hostValidator.checkGlusterQuorum(cluster, hostIds).isEmpty());
    }

    @Test
    public void testCheckGlusterQuorumWithTwoServersDown() {
        Cluster cluster = getCluster(true, CLUSTER_ID);
        Iterable<Guid> hostIds = Arrays.asList(SERVER_ID_1, SERVER_ID_2);
        assertTrue("Quorum check is failing", hostValidator.checkGlusterQuorum(cluster, hostIds).size() == 2);
        assertTrue(Arrays.asList("Vol-1", "Vol-2").equals(hostValidator.checkGlusterQuorum(cluster, hostIds)));
    }

    @Test
    public void testCheckGlusterQuorumWithBricksDown() {
        Cluster cluster = getCluster(true, CLUSTER_ID);
        // Make sure first brick in all the subvolumes are down
        List<GlusterVolumeEntity> glusterVolumes = getGlusterVolumes(CLUSTER_ID, GlusterStatus.UP);
        for (GlusterVolumeEntity volume : glusterVolumes) {
            for (int index = 0; index < volume.getBricks().size(); index += volume.getReplicaCount()) {
                volume.getBricks().get(index).setStatus(GlusterStatus.DOWN);
            }
        }
        doReturn(glusterVolumes).when(volumeDao).getByClusterId(CLUSTER_ID);
        Iterable<Guid> hostIds = Arrays.asList(SERVER_ID_2);
        assertTrue("Quorum check is failing", hostValidator.checkGlusterQuorum(cluster, hostIds).size() == 2);
        assertTrue(Arrays.asList("Vol-1", "Vol-2").equals(hostValidator.checkGlusterQuorum(cluster, hostIds)));
    }

    @Test
    public void testCheckGlusterQuorumWithVolumeDown() {
        Cluster cluster = getCluster(true, CLUSTER_ID);
        doReturn(getGlusterVolumes(CLUSTER_ID, GlusterStatus.DOWN)).when(volumeDao).getByClusterId(CLUSTER_ID);
        Iterable<Guid> hostIds = Arrays.asList(SERVER_ID_1, SERVER_ID_2);
        assertTrue("Quorum check is failing with volumes in down status",
                hostValidator.checkGlusterQuorum(cluster, hostIds).isEmpty());
    }

    @Test
    public void testcheckUnsyncedEntriesWithDownBricks() {
        doReturn(getBricksFromServer(SERVER_ID_1, GlusterStatus.DOWN)).when(brickDao)
                .getGlusterVolumeBricksByServerId(SERVER_ID_1);
        assertTrue("Unsynced entries test is failing for bricks with down status",
                hostValidator.checkUnsyncedEntries(Arrays.asList(SERVER_ID_1)).isEmpty());

    }

    @Test
    public void testcheckUnsyncedEntriesWithoutUnSyncedEntries() {
        doReturn(getBricksFromServer(SERVER_ID_1, GlusterStatus.UP)).when(brickDao)
                .getGlusterVolumeBricksByServerId(SERVER_ID_1);
        assertTrue("Unsynced entries test is failing for bricks without unsynced entries",
                hostValidator.checkUnsyncedEntries(Arrays.asList(SERVER_ID_1)).isEmpty());
    }

    @Test
    public void testcheckUnsyncedEntriesWithUnSyncedEntries() {
        List<GlusterBrickEntity> bricks = getBricksFromServer(SERVER_ID_1, GlusterStatus.UP);
        bricks.get(0).setUnSyncedEntries(100);
        bricks.get(1).setUnSyncedEntries(10);
        bricks.get(2).setUnSyncedEntries(0);
        doReturn(bricks).when(brickDao).getGlusterVolumeBricksByServerId(SERVER_ID_1);
        assertTrue("Unsynced entries test is failing for bricks with unsynced entries",
                hostValidator.checkUnsyncedEntries(Arrays.asList(SERVER_ID_1, SERVER_ID_2))
                        .get(SERVER_ID_1)
                        .size() == 2);
    }

    private Cluster getCluster(boolean supportGlusterService, Guid clusterId) {
        Cluster cluster = new Cluster();
        cluster.setId(clusterId);
        cluster.setGlusterService(supportGlusterService);
        return cluster;
    }

    private List<GlusterVolumeEntity> getGlusterVolumes(Guid clusterId, GlusterStatus status) {
        List<GlusterVolumeEntity> volumesList = new ArrayList<>();
        String volumeOptions = "cluster.quorum-type=fixed,cluster.quorum-count=2";

        volumesList.add(getGlusterVolume("Vol-1",
                VOL_ID_1,
                GlusterVolumeType.REPLICATE,
                3,
                volumeOptions,
                getBricksForVolume(1),
                status));

        volumesList.add(getGlusterVolume("Vol-2",
                VOL_ID_2,
                GlusterVolumeType.DISTRIBUTED_REPLICATE,
                3,
                "cluster.quorum-type=auto",
                getBricksForVolume(2),
                status));
        return volumesList;
    }

    private List<GlusterBrickEntity> getBricksFromServer(Guid serverId, GlusterStatus status) {
        List<GlusterBrickEntity> bricks = new ArrayList<>();
        bricks.add(getGlusterBrick(status, serverId));
        bricks.add(getGlusterBrick(status, serverId));
        bricks.add(getGlusterBrick(status, serverId));
        return bricks;
    }

    private List<GlusterBrickEntity> getBricksForVolume(int bricksPerServer) {
        List<GlusterBrickEntity> bricks = new ArrayList<>();
        for (int i = 0; i < bricksPerServer; i++) {
            bricks.add(getGlusterBrick(GlusterStatus.UP, SERVER_ID_1));
            bricks.add(getGlusterBrick(GlusterStatus.UP, SERVER_ID_2));
            bricks.add(getGlusterBrick(GlusterStatus.UP, SERVER_ID_3));
        }
        return bricks;
    }

    private GlusterVolumeEntity getGlusterVolume(String name,
            Guid volumeID,
            GlusterVolumeType volumeType,
            int replicaCount,
            String volumeOptions,
            List<GlusterBrickEntity> bricks,
            GlusterStatus status) {
        GlusterVolumeEntity volume = new GlusterVolumeEntity();
        volume.setName(name);
        volume.setId(volumeID);
        volume.setReplicaCount(replicaCount);
        volume.setVolumeType(volumeType);
        volume.setOptions(volumeOptions);
        volume.setBricks(bricks);
        volume.setStatus(status);
        return volume;
    }

    private GlusterBrickEntity getGlusterBrick(GlusterStatus status, Guid serverId) {
        GlusterBrickEntity brick = new GlusterBrickEntity();
        brick.setStatus(status);
        brick.setServerId(serverId);
        return brick;
    }
}

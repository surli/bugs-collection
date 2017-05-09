package org.ovirt.engine.core.vdsbroker.monitoring;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.spy;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.ovirt.engine.core.common.businessentities.VDSStatus.Connecting;
import static org.ovirt.engine.core.common.businessentities.VDSStatus.Down;
import static org.ovirt.engine.core.common.businessentities.VDSStatus.Error;
import static org.ovirt.engine.core.common.businessentities.VDSStatus.Initializing;
import static org.ovirt.engine.core.common.businessentities.VDSStatus.InstallFailed;
import static org.ovirt.engine.core.common.businessentities.VDSStatus.Installing;
import static org.ovirt.engine.core.common.businessentities.VDSStatus.InstallingOS;
import static org.ovirt.engine.core.common.businessentities.VDSStatus.Kdumping;
import static org.ovirt.engine.core.common.businessentities.VDSStatus.Maintenance;
import static org.ovirt.engine.core.common.businessentities.VDSStatus.NonOperational;
import static org.ovirt.engine.core.common.businessentities.VDSStatus.NonResponsive;
import static org.ovirt.engine.core.common.businessentities.VDSStatus.PendingApproval;
import static org.ovirt.engine.core.common.businessentities.VDSStatus.PreparingForMaintenance;
import static org.ovirt.engine.core.common.businessentities.VDSStatus.Reboot;
import static org.ovirt.engine.core.common.businessentities.VDSStatus.Unassigned;
import static org.ovirt.engine.core.common.businessentities.VDSStatus.Up;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.ovirt.engine.core.common.businessentities.VDSStatus;
import org.ovirt.engine.core.utils.MockConfigRule;
import org.ovirt.engine.core.vdsbroker.VdsManager;

@RunWith(Parameterized.class)
public class PollVmStatsRefresherTest {

    @Parameterized.Parameter(0)
    public VDSStatus status;
    @Parameterized.Parameter(1)
    public boolean monitoringNeeded;

    private PollVmStatsRefresher underTest;

    @Mock
    private VdsManager vdsManager;

    @Rule
    public final MockConfigRule mcr = new MockConfigRule();

    @Parameterized.Parameters(name = "status {0} is monitoring needed - {1}")
    public static Object[][] data() {
        return new Object[][] {
                // host status              is monitoring needed
                { Up,                       true},
                { NonResponsive,            true },
                { Error,                    true },
                { NonOperational,           true },
                { PreparingForMaintenance,  true },
                { Initializing,             true },
                { Connecting,               true },
                { Unassigned,               false },
                { Down,                     false },
                { Maintenance,              false },
                { Installing,               false },
                { InstallFailed,            false },
                { Reboot,                   false },
                { PendingApproval,          false },
                { InstallingOS,             false },
                { Kdumping,                 false },
        };
    }

    @Before
    public void setup() {
        initMocks(this);
        underTest = spy(new PollVmStatsRefresher(vdsManager));
    }

    @Test
    public void testMonitoringNeededByStatus() {
        assertEquals(underTest.isMonitoringNeeded(status), monitoringNeeded);
    }

}

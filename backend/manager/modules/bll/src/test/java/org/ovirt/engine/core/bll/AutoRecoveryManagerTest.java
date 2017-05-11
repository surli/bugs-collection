package org.ovirt.engine.core.bll;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.ovirt.engine.core.utils.MockConfigRule.mockConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.ovirt.engine.core.bll.interfaces.BackendInternal;
import org.ovirt.engine.core.common.action.VdcActionParametersBase;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.businessentities.StorageDomain;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.config.Config;
import org.ovirt.engine.core.common.config.ConfigValues;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dao.StorageDomainDao;
import org.ovirt.engine.core.dao.VdsDao;
import org.ovirt.engine.core.dao.network.InterfaceDao;
import org.ovirt.engine.core.dao.network.NetworkDao;
import org.ovirt.engine.core.utils.MockConfigRule;

@RunWith(MockitoJUnitRunner.class)
public class AutoRecoveryManagerTest {

    @InjectMocks
    private AutoRecoveryManager manager;

    @ClassRule
    public static MockConfigRule mcr =
    new MockConfigRule(mockConfig(ConfigValues.AutoRecoveryAllowedTypes, new HashMap<>()));

    @Mock
    private BackendInternal backendMock;

    @Mock
    private VdsDao vdsDaoMock;

    @Mock
    private StorageDomainDao storageDomainDaoMock;

    @Mock
    private InterfaceDao interfaceDaoMock;

    @Mock
    private NetworkDao networkDaoMock;

    // Entities needing recovery
    private List<VDS> vdss = new ArrayList<>();
    private List<StorageDomain> storageDomains = new ArrayList<>();

    @Before
    public void setup() {
        final VDS vds = new VDS();
        vdss.add(vds);
        when(vdsDaoMock.listFailedAutorecoverables()).thenReturn(vdss);

        StorageDomain domain = new StorageDomain();
        domain.setStoragePoolId(Guid.newGuid());
        storageDomains.add(domain);
        when(storageDomainDaoMock.listFailedAutorecoverables()).thenReturn(storageDomains);
    }

    @Test
    public void onTimerFullConfig() {
        Config.<Map<String, String>> getValue(ConfigValues.AutoRecoveryAllowedTypes).put("storage domains",
                Boolean.TRUE.toString());
        Config.<Map<String, String>> getValue(ConfigValues.AutoRecoveryAllowedTypes).put("hosts",
                Boolean.TRUE.toString());
        manager.onTimer();
        verify(backendMock, times(vdss.size())).runInternalAction(eq(VdcActionType.ActivateVds),
                any(VdcActionParametersBase.class));
        verify(backendMock, times(storageDomains.size())).runInternalAction(eq(VdcActionType.ConnectDomainToStorage),
                any(VdcActionParametersBase.class));
    }

    @Test
    public void onTimerFalseConfig() {
        Config.<Map<String, String>> getValue(ConfigValues.AutoRecoveryAllowedTypes).put("storage domains",
                Boolean.FALSE.toString());
        Config.<Map<String, String>> getValue(ConfigValues.AutoRecoveryAllowedTypes).put("hosts",
                Boolean.FALSE.toString());
        manager.onTimer();
        verify(backendMock, never()).runInternalAction(eq(VdcActionType.ActivateVds),
                any(VdcActionParametersBase.class));
        verify(backendMock, never()).runInternalAction(eq(VdcActionType.ConnectDomainToStorage),
                any(VdcActionParametersBase.class));
    }
}

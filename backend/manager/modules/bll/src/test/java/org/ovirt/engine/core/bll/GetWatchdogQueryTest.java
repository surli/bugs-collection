package org.ovirt.engine.core.bll;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.ovirt.engine.core.common.businessentities.VmDevice;
import org.ovirt.engine.core.common.businessentities.VmDeviceGeneralType;
import org.ovirt.engine.core.common.businessentities.VmDeviceId;
import org.ovirt.engine.core.common.businessentities.VmWatchdog;
import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.common.utils.VmDeviceType;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dao.VmDeviceDao;

@RunWith(MockitoJUnitRunner.class)
public class GetWatchdogQueryTest extends AbstractQueryTest<IdQueryParameters, GetWatchdogQuery<IdQueryParameters>> {

    private static final Guid TEST_VM_ID = new Guid("ee655a4d-effc-4aab-be2b-2f80ff40cd1c");

    @Mock
    VmDeviceDao vmDeviceDao;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        when(getQueryParameters().getId()).thenReturn(TEST_VM_ID);
    }

    @Test
    public void executeQueryCommandWithNull() {
        getQuery().executeQueryCommand();
        assertTrue(((List<?>) getQuery().getQueryReturnValue().getReturnValue()).isEmpty());
    }

    @Test
    public void executeQueryCommandWithWatchdog() {
        Map<String, Object> watchdogSpecParams = new HashMap<>();
        watchdogSpecParams.put("model", "i6300esb");
        watchdogSpecParams.put("action", "reset");
        VmDevice vmDevice = new VmDevice(new VmDeviceId(new Guid("6f86b8a4-e721-4149-b2df-056eb621b16a"),
                TEST_VM_ID), VmDeviceGeneralType.WATCHDOG, VmDeviceType.WATCHDOG.getName(), "", watchdogSpecParams,
                true, true, true, "", null, null, null);
        when(vmDeviceDao.getVmDeviceByVmIdAndType(TEST_VM_ID, VmDeviceGeneralType.WATCHDOG))
                .thenReturn(Collections.singletonList(vmDevice));

        getQuery().executeQueryCommand();

        List<VmWatchdog> result = getQuery().getQueryReturnValue().getReturnValue();
        assertNotNull(result);
        assertFalse(result.isEmpty());
        VmWatchdog watchdog = result.get(0);
        assertEquals("reset", watchdog.getAction().name().toLowerCase());
        assertEquals("i6300esb", watchdog.getModel().name().toLowerCase());
    }

}

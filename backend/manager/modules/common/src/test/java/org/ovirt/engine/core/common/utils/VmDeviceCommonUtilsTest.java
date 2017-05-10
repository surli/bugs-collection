package org.ovirt.engine.core.common.utils;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.ovirt.engine.core.common.businessentities.BootSequence;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.VmDevice;
import org.ovirt.engine.core.common.businessentities.VmDeviceGeneralType;
import org.ovirt.engine.core.common.businessentities.VmDeviceId;
import org.ovirt.engine.core.common.businessentities.network.VmNetworkInterface;
import org.ovirt.engine.core.common.businessentities.storage.DiskVmElement;
import org.ovirt.engine.core.compat.Guid;

@RunWith(MockitoJUnitRunner.class)
public class VmDeviceCommonUtilsTest {

    private static final String NIC_1_NAME = "nic1";
    private static final String NIC_2_NAME = "nic2";

    @Mock
    private VM vm;

    @Test
    public void testUpdateVmDevicesBootOrder() {
        Map<VmDeviceId, DiskVmElement> idToDiskElement = new HashMap<>();
        List<VmNetworkInterface> interfaces = new LinkedList<>();

        VmDevice nic1 = createNetworkInterface(true, NIC_1_NAME, interfaces);
        VmDevice unmanagedNic = createUnmanagedNetworkInterface(true);
        VmDevice nic2 = createNetworkInterface(true, NIC_2_NAME, interfaces);
        VmDevice nonBootableNic = createNetworkInterface(false, "", interfaces);

        VmDevice bootableDisk = createDiskDevice(true, idToDiskElement);
        VmDevice nonBootableDisk = createDiskDevice(false, idToDiskElement);

        VmDevice cd = createCdRomDevice();

        doReturn(BootSequence.DNC).when(vm).getDefaultBootSequence();

        // it is important that nic2 will be before nic1 to ensure their boot order is
        // ordered according to their names and not according to their position in the list
        VmDeviceCommonUtils.updateVmDevicesBootOrder(
                vm.getDefaultBootSequence(),
                Arrays.asList(bootableDisk, nic2, cd, nic1, nonBootableDisk, unmanagedNic),
                interfaces,
                idToDiskElement);

        int index = 1;
        assertEquals("Wrong boot order for CD", index++, cd.getBootOrder());
        assertEquals("Wrong boot order for nic1", index++, nic1.getBootOrder());
        assertEquals("Wrong boot order for nic2", index++, nic2.getBootOrder());
        assertEquals("Wrong boot order for non bootable nic", 0, nonBootableNic.getBootOrder());
        assertEquals("Wrong boot order for unmanaged nic", 0, unmanagedNic.getBootOrder());
        assertEquals("Wrong boot order for bootable disk", index++, bootableDisk.getBootOrder());
        assertEquals("Wrong boot order for non bootable disk", 0, nonBootableDisk.getBootOrder());
    }

    private VmDevice createNetworkInterface(boolean plugged, String name,
            List<VmNetworkInterface> interfaces) {
        Guid id = Guid.newGuid();

        VmNetworkInterface vmNic = new VmNetworkInterface();
        vmNic.setId(id);
        vmNic.setName(name);
        interfaces.add(vmNic);

        VmDevice device = createNetworkInterfaceDevice(plugged, id);
        device.setManaged(true);
        return device;
    }

    private VmDevice createUnmanagedNetworkInterface(boolean plugged) {
        VmDevice device = createNetworkInterfaceDevice(plugged, Guid.newGuid());
        device.setManaged(false);
        return device;
    }

    private VmDevice createNetworkInterfaceDevice(boolean plugged, Guid id) {
        VmDevice device = new VmDevice();
        device.setType(VmDeviceGeneralType.INTERFACE);
        device.setDevice(VmDeviceType.BRIDGE.getName());
        device.setPlugged(plugged);
        device.setId(new VmDeviceId(id, null));
        return device;
    }

    private VmDevice createDiskDevice(boolean boot, Map<VmDeviceId, DiskVmElement> idToDiskElement) {
        Guid id = Guid.newGuid();
        VmDevice device = new VmDevice();
        device.setType(VmDeviceGeneralType.DISK);
        device.setDevice(VmDeviceType.DISK.getName());
        device.setId(new VmDeviceId(id, null));

        DiskVmElement dve = new DiskVmElement(new VmDeviceId(id, null));
        dve.setBoot(boot);
        idToDiskElement.put(dve.getId(), dve);

        return device;
    }

    private VmDevice createCdRomDevice() {
        Guid id = Guid.newGuid();
        VmDevice device = new VmDevice();
        device.setType(VmDeviceGeneralType.DISK);
        device.setDevice(VmDeviceType.CDROM.getName());
        device.setPlugged(true);
        device.setId(new VmDeviceId(id, null));

        return device;
    }
}

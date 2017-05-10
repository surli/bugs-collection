package org.ovirt.engine.core.bll.storage.disk;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.ovirt.engine.core.bll.ValidateTestUtils;
import org.ovirt.engine.core.bll.ValidationResult;
import org.ovirt.engine.core.bll.snapshots.SnapshotsValidator;
import org.ovirt.engine.core.bll.validator.storage.DiskValidator;
import org.ovirt.engine.core.bll.validator.storage.DiskVmElementValidator;
import org.ovirt.engine.core.bll.validator.storage.StorageDomainValidator;
import org.ovirt.engine.core.common.action.AttachDetachVmDiskParameters;
import org.ovirt.engine.core.common.businessentities.StoragePoolIsoMap;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.VMStatus;
import org.ovirt.engine.core.common.businessentities.storage.DiskImage;
import org.ovirt.engine.core.common.businessentities.storage.DiskVmElement;
import org.ovirt.engine.core.common.errors.EngineMessage;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dao.StorageDomainDao;
import org.ovirt.engine.core.dao.StoragePoolIsoMapDao;
import org.ovirt.engine.core.dao.VmDeviceDao;

@RunWith(MockitoJUnitRunner.class)
public class AttachDiskToVmCommandTest {

    private Guid vmId = Guid.newGuid();
    private Guid diskId = Guid.newGuid();
    private Guid storageId = Guid.newGuid();

    @Mock
    private VmDeviceDao vmDeviceDao;

    @Mock
    private StorageDomainDao storageDomainDao;

    @Mock
    private StoragePoolIsoMapDao storagePoolIsoMapDao;

    @Mock
    private DiskValidator diskValidator;

    @Mock
    private DiskVmElementValidator diskVmElementValidator;

    @Mock
    private StorageDomainValidator storageDomainValidator;

    @Mock
    private SnapshotsValidator snapshotsValidator;

    @Mock
    private DiskHandler diskHandler;

    private AttachDetachVmDiskParameters parameters = createParameters();

    @Spy
    @InjectMocks
    private AttachDiskToVmCommand<AttachDetachVmDiskParameters> command = new AttachDiskToVmCommand<>(parameters, null);

    @Before
    public void initTest() {
        initialSetup();
        initCommand();
    }

    private void initialSetup() {
        mockValidators();
        doNothing().when(command).updateDisksFromDb();
        doReturn(mockVm()).when(command).getVm();

        doReturn(createDiskImage()).when(diskHandler).loadActiveDisk(any());
        doReturn(createDiskImage()).when(diskHandler).loadDiskFromSnapshot(any(), any());

        doReturn(true).when(command).isDiskPassPciAndIdeLimit();
        doReturn(true).when(command).checkDiskUsedAsOvfStore(diskValidator);
        doReturn(false).when(command).isOperationPerformedOnDiskSnapshot();

        mockStoragePoolIsoMap();
    }

    private void initCommand() {
        command.init();
    }

    @Test
    public void testValidateSucceed() {
        ValidateTestUtils.runAndAssertValidateSuccess(command);
    }

    @Test
    public void testValidateSucceedReadOnlyWithInterface() {
        ValidateTestUtils.runAndAssertValidateSuccess(command);
        verify(diskVmElementValidator).isReadOnlyPropertyCompatibleWithInterface();
    }

    @Test
    public void testValidateFailReadOnlyOnInterface() {
        when(diskVmElementValidator.isReadOnlyPropertyCompatibleWithInterface()).thenReturn(
                new ValidationResult(EngineMessage.ACTION_TYPE_FAILED_INTERFACE_DOES_NOT_SUPPORT_READ_ONLY_ATTR));

        ValidateTestUtils.runAndAssertValidateFailure(command,
                EngineMessage.ACTION_TYPE_FAILED_INTERFACE_DOES_NOT_SUPPORT_READ_ONLY_ATTR);
        verify(diskVmElementValidator).isReadOnlyPropertyCompatibleWithInterface();
    }

    @Test
    public void testValidateFailsWhenDiscardIsNotSupported() {
        when(diskVmElementValidator.isPassDiscardSupported(any())).thenReturn(
                new ValidationResult(EngineMessage.ACTION_TYPE_FAILED_PASS_DISCARD_NOT_SUPPORTED_BY_DISK_INTERFACE));
        ValidateTestUtils.runAndAssertValidateFailure(command,
                EngineMessage.ACTION_TYPE_FAILED_PASS_DISCARD_NOT_SUPPORTED_BY_DISK_INTERFACE);
    }

    private AttachDetachVmDiskParameters createParameters() {
        AttachDetachVmDiskParameters parameters = new AttachDetachVmDiskParameters(new DiskVmElement(diskId, vmId));
        parameters.setReadOnly(true);
        return parameters;
    }

    private VM mockVm() {
        VM vm = new VM();
        vm.setStatus(VMStatus.Down);
        vm.setStoragePoolId(Guid.newGuid());
        vm.setId(vmId);

        return vm;
    }

    private void mockStorageDomainValidator() {
        doReturn(storageDomainValidator).when(command).getStorageDomainValidator(any());
    }

    private void mockDiskValidator() {
        doReturn(diskValidator).when(command).getDiskValidator(any());
        doReturn(diskVmElementValidator).when(command).getDiskVmElementValidator(any(), any());
    }

    private void mockValidators() {
        mockStorageDomainValidator();
        mockDiskValidator();
    }

    private void mockStoragePoolIsoMap() {
        StoragePoolIsoMap spim = new StoragePoolIsoMap();
        when(storagePoolIsoMapDao.get(any())).thenReturn(spim);
    }

    private DiskImage createDiskImage() {
        DiskImage disk = new DiskImage();
        disk.setId(diskId);
        Collections.singletonList(storageId);
        disk.setStorageIds(new ArrayList<>(Collections.singletonList(storageId)));
        return disk;
    }
}

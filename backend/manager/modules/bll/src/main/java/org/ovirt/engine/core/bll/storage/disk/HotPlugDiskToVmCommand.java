package org.ovirt.engine.core.bll.storage.disk;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.ovirt.engine.core.bll.LockMessagesMatchUtil;
import org.ovirt.engine.core.bll.NonTransactiveCommandAttribute;
import org.ovirt.engine.core.bll.context.CommandContext;
import org.ovirt.engine.core.bll.validator.VmValidator;
import org.ovirt.engine.core.bll.validator.storage.DiskVmElementValidator;
import org.ovirt.engine.core.bll.validator.storage.StorageDomainValidator;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.action.LockProperties;
import org.ovirt.engine.core.common.action.LockProperties.Scope;
import org.ovirt.engine.core.common.action.VmDiskOperationParameterBase;
import org.ovirt.engine.core.common.businessentities.StorageDomain;
import org.ovirt.engine.core.common.businessentities.VmDevice;
import org.ovirt.engine.core.common.businessentities.VmDeviceId;
import org.ovirt.engine.core.common.businessentities.storage.Disk;
import org.ovirt.engine.core.common.businessentities.storage.DiskImage;
import org.ovirt.engine.core.common.businessentities.storage.DiskStorageType;
import org.ovirt.engine.core.common.businessentities.storage.DiskVmElement;
import org.ovirt.engine.core.common.errors.EngineMessage;
import org.ovirt.engine.core.common.locks.LockingGroup;
import org.ovirt.engine.core.common.utils.Pair;
import org.ovirt.engine.core.common.vdscommands.VDSCommandType;
import org.ovirt.engine.core.compat.Guid;

@NonTransactiveCommandAttribute
public class HotPlugDiskToVmCommand<T extends VmDiskOperationParameterBase> extends AbstractDiskVmCommand<T> {

    private Disk disk;
    private DiskVmElement diskVmElement;
    protected VmDevice oldVmDevice;

    public HotPlugDiskToVmCommand(T parameters, CommandContext commandContext) {
        super(parameters, commandContext);
    }

    @Override
    protected LockProperties applyLockProperties(LockProperties lockProperties) {
        return lockProperties.withScope(Scope.Execution);
    }

    @Override
    protected void setActionMessageParameters() {
        addValidationMessage(EngineMessage.VAR__ACTION__HOT_PLUG);
        addValidationMessage(EngineMessage.VAR__TYPE__DISK);
    }

    @Override
    protected boolean validate() {
        performDbLoads();

        return
                validate(new VmValidator(getVm()).isVmExists()) &&
                isVmInUpPausedDownStatus() &&
                canRunActionOnNonManagedVm() &&
                isDiskExistAndAttachedToVm(getDisk()) &&
                interfaceDiskValidation() &&
                checkCanPerformPlugUnPlugDisk() &&
                isVmNotInPreviewSnapshot() &&
                imageStorageValidation() &&
                virtIoScsiDiskValidation() &&
                isPassDiscardSupported();
    }

    private boolean virtIoScsiDiskValidation() {
        DiskVmElementValidator diskVmElementValidator = getDiskVmElementValidator(disk, getDiskVmElement());
        return validate(diskVmElementValidator.isVirtIoScsiValid(getVm()));
    }

    private boolean isPassDiscardSupported() {
        Guid storageDomainId = getDisk().getDiskStorageType() == DiskStorageType.IMAGE ?
                ((DiskImage) getDisk()).getStorageIds().get(0) : null;
        return validate(getDiskVmElementValidator(getDisk(),
                getDiskVmElement()).isPassDiscardSupported(storageDomainId));
    }

    private boolean interfaceDiskValidation() {
        DiskVmElementValidator diskVmElementValidator = getDiskVmElementValidator(disk, getDiskVmElement());
        return validate(diskVmElementValidator.isDiskInterfaceSupported(getVm()));
    }

    private boolean imageStorageValidation() {
        // If the VM is not an image then it does not use the storage domain.
        // If the VM is not in UP or PAUSED status, then we know that there is no running qemu process,
        // so we don't need to check the storage domain activity.
        if (getDisk().getDiskStorageType() != DiskStorageType.IMAGE || !getVm().getStatus().isRunningOrPaused()) {
            return true;
        }
        DiskImage diskImage = (DiskImage) getDisk();
        StorageDomain storageDomain = storageDomainDao.getForStoragePool(
                diskImage.getStorageIds().get(0), diskImage.getStoragePoolId());
        StorageDomainValidator storageDomainValidator = getStorageDomainValidator(storageDomain);
        return validate(storageDomainValidator.isDomainExistAndActive());
    }

    protected StorageDomainValidator getStorageDomainValidator(StorageDomain storageDomain) {
        return new StorageDomainValidator(storageDomain);
    }

    private void performDbLoads() {
        if (getDiskVmElement() == null) {
            return;
        }
        oldVmDevice = vmDeviceDao.get(new VmDeviceId(getDiskVmElement().getDiskId(), getVmId()));
        if (oldVmDevice != null) {
            if (oldVmDevice.getSnapshotId() != null) {
                disk = diskImageDao.getDiskSnapshotForVmSnapshot(getDiskVmElement().getDiskId(), oldVmDevice.getSnapshotId());
            } else {
                disk = diskDao.get(getDiskVmElement().getDiskId());
            }
        }
    }

    private boolean checkCanPerformPlugUnPlugDisk() {
        if (getVm().getStatus().isUpOrPaused()) {
            setVdsId(getVm().getRunOnVds());
            if (!isDiskSupportedForPlugUnPlug(getDiskVmElement(), disk.getDiskAlias())) {
                return false;
            }
        }

        if (getPlugAction() == VDSCommandType.HotPlugDisk && oldVmDevice.isPlugged()) {
            return failValidation(EngineMessage.HOT_PLUG_DISK_IS_NOT_UNPLUGGED);
        }

        if (getPlugAction() == VDSCommandType.HotUnPlugDisk && !oldVmDevice.isPlugged()) {
            return failValidation(EngineMessage.HOT_UNPLUG_DISK_IS_NOT_PLUGGED);
        }

        return true;
    }

    protected VDSCommandType getPlugAction() {
        return VDSCommandType.HotPlugDisk;
    }

    @Override
    protected void executeVmCommand() {
        if (getVm().getStatus().isUpOrPaused()) {
            updateDisksFromDb();
            performPlugCommand(getPlugAction(), getDisk(), oldVmDevice);
        }

        // At this point disk is already plugged to or unplugged from VM (depends on the command),
        // so we can update the needed device properties
        updateDeviceProperties();

        vmStaticDao.incrementDbGeneration(getVm().getId());
        setSucceeded(true);
    }

    protected void updateDeviceProperties() {
        VmDevice device = vmDeviceDao.get(oldVmDevice.getId());
        device.setPlugged(true);
        vmDeviceDao.updateHotPlugDisk(device);
    }

    @Override
    protected Map<String, Pair<String, String>> getSharedLocks() {
        return Collections.singletonMap(getVmId().toString(),
                LockMessagesMatchUtil.makeLockingPair(LockingGroup.VM, EngineMessage.ACTION_TYPE_FAILED_VM_IS_LOCKED));
    }

    @Override
    protected Map<String, Pair<String, String>> getExclusiveLocks() {
        Map<String, Pair<String, String>> exclusiveLock = null;

        if (getDisk() != null) {
            exclusiveLock = new HashMap<>();

            exclusiveLock.put(getDisk().getId().toString(),
                    LockMessagesMatchUtil.makeLockingPair(LockingGroup.DISK,
                            EngineMessage.ACTION_TYPE_FAILED_DISKS_LOCKED.name() +
                                    String.format("$diskAliases %1$s", getDiskAlias())));

            if (getDiskVmElement() != null && getDiskVmElement().isBoot()) {
                exclusiveLock.put(getVmId().toString(),
                    LockMessagesMatchUtil.makeLockingPair(LockingGroup.VM_DISK_BOOT,
                            EngineMessage.ACTION_TYPE_FAILED_OBJECT_LOCKED));

            }
        }
        return exclusiveLock;
    }

    @Override
    public AuditLogType getAuditLogTypeValue() {
        return getSucceeded() ? AuditLogType.USER_HOTPLUG_DISK : AuditLogType.USER_FAILED_HOTPLUG_DISK;
    }

    @Override
    public String getDiskAlias() {
        return getDisk().getDiskAlias();
    }

    protected Disk getDisk() {
        if (disk == null) {
            disk = diskDao.get(super.getDiskVmElement().getDiskId());
        }
        return disk;
    }

    // As all the validation should be done against the DiskVmElement loaded from the DB since the parameters may
    // not contain all relevant data
    @Override
    protected DiskVmElement getDiskVmElement() {
        if (diskVmElement == null && getDisk() != null) {
            diskVmElement = diskVmElementDao.get(new VmDeviceId(getDisk().getId(), getVmId()));
        }
        return diskVmElement;
    }
}

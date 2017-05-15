package org.ovirt.engine.core.bll.validator;

import static org.ovirt.engine.core.bll.storage.disk.image.DisksFilter.ONLY_NOT_SHAREABLE;
import static org.ovirt.engine.core.bll.storage.disk.image.DisksFilter.ONLY_PLUGGED;
import static org.ovirt.engine.core.bll.storage.disk.image.DisksFilter.ONLY_SNAPABLE;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.ovirt.engine.core.bll.Backend;
import org.ovirt.engine.core.bll.ValidationResult;
import org.ovirt.engine.core.bll.interfaces.BackendInternal;
import org.ovirt.engine.core.bll.scheduling.SchedulingManager;
import org.ovirt.engine.core.bll.snapshots.SnapshotsValidator;
import org.ovirt.engine.core.bll.storage.disk.DiskHandler;
import org.ovirt.engine.core.bll.storage.disk.image.DisksFilter;
import org.ovirt.engine.core.bll.storage.disk.image.ImagesHandler;
import org.ovirt.engine.core.bll.validator.storage.DiskImagesValidator;
import org.ovirt.engine.core.bll.validator.storage.MultipleDiskVmElementValidator;
import org.ovirt.engine.core.bll.validator.storage.MultipleStorageDomainsValidator;
import org.ovirt.engine.core.bll.validator.storage.StoragePoolValidator;
import org.ovirt.engine.core.common.VdcActionUtils;
import org.ovirt.engine.core.common.action.RunVmParams;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.businessentities.BootSequence;
import org.ovirt.engine.core.common.businessentities.Cluster;
import org.ovirt.engine.core.common.businessentities.GraphicsType;
import org.ovirt.engine.core.common.businessentities.StoragePool;
import org.ovirt.engine.core.common.businessentities.VDSStatus;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.VMStatus;
import org.ovirt.engine.core.common.businessentities.VdsDynamic;
import org.ovirt.engine.core.common.businessentities.VmDevice;
import org.ovirt.engine.core.common.businessentities.VmDeviceGeneralType;
import org.ovirt.engine.core.common.businessentities.network.Network;
import org.ovirt.engine.core.common.businessentities.network.VmNetworkInterface;
import org.ovirt.engine.core.common.businessentities.storage.Disk;
import org.ovirt.engine.core.common.businessentities.storage.DiskImage;
import org.ovirt.engine.core.common.businessentities.storage.DiskStorageType;
import org.ovirt.engine.core.common.businessentities.storage.DiskVmElement;
import org.ovirt.engine.core.common.businessentities.storage.ImageFileType;
import org.ovirt.engine.core.common.businessentities.storage.RepoImage;
import org.ovirt.engine.core.common.businessentities.storage.VolumeType;
import org.ovirt.engine.core.common.errors.EngineMessage;
import org.ovirt.engine.core.common.queries.GetImagesListParameters;
import org.ovirt.engine.core.common.queries.VdcQueryReturnValue;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.common.utils.VmCommonUtils;
import org.ovirt.engine.core.common.utils.customprop.VmPropertiesUtils;
import org.ovirt.engine.core.common.vdscommands.IsVmDuringInitiatingVDSCommandParameters;
import org.ovirt.engine.core.common.vdscommands.VDSCommandType;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.dao.DiskDao;
import org.ovirt.engine.core.dao.VdsDynamicDao;
import org.ovirt.engine.core.dao.network.NetworkDao;
import org.ovirt.engine.core.dao.network.VmNicDao;
import org.ovirt.engine.core.di.Injector;
import org.ovirt.engine.core.utils.NetworkUtils;

public class RunVmValidator {

    private VM vm;
    private RunVmParams runVmParam;
    private boolean isInternalExecution;
    private Guid activeIsoDomainId;

    private List<Disk> cachedVmDisks;
    private List<DiskImage> cachedVmImageDisks;
    private Set<String> cachedInterfaceNetworkNames;
    private List<Network> cachedClusterNetworks;
    private Set<String> cachedClusterNetworksNames;

    @Inject
    private SnapshotsValidator snapshotsValidator;

    public RunVmValidator(VM vm, RunVmParams rumVmParam, boolean isInternalExecution, Guid activeIsoDomainId) {
        this.vm = vm;
        this.runVmParam = rumVmParam;
        this.isInternalExecution = isInternalExecution;
        this.activeIsoDomainId = activeIsoDomainId;
    }

    /**
     * Used for testings
     */
    protected RunVmValidator() {
    }

    /**
     * A general method for run vm validations. used in runVmCommand and in VmPoolCommandBase
     *
     * @param vdsBlackList
     *            - hosts that we already tried to run on
     * @param vdsWhiteList
     *            - initial host list, mainly runOnSpecificHost (runOnce/migrateToHost)
     */
    public boolean canRunVm(List<String> messages, StoragePool storagePool,
            List<Guid> vdsBlackList,
            List<Guid> vdsWhiteList,
            Cluster cluster,
            boolean runInUnknownStatus) {

        if (vm.getStatus() == VMStatus.Paused) {
            // if the VM is paused, we should only check the VDS status
            // as the rest of the checks were already checked before
            return validate(validateVdsStatus(vm), messages);
        } else if (vm.getStatus() == VMStatus.Suspended) {
            return validate(new VmValidator(vm).vmNotLocked(), messages) &&
                   validate(snapshotsValidator.vmNotDuringSnapshot(vm.getId()), messages) &&
                   validate(validateVmStatusUsingMatrix(vm), messages) &&
                   validate(validateStoragePoolUp(vm, storagePool, getVmImageDisks()), messages) &&
                   validate(vmDuringInitialization(vm), messages) &&
                    validate(validateStorageDomains(vm,
                            isInternalExecution,
                            filterReadOnlyAndPreallocatedDisks(getVmImageDisks())), messages)
                    &&
                   validate(validateImagesForRunVm(vm, getVmImageDisks()), messages) &&
                   validate(validateDisksPassDiscard(vm), messages) &&
                   getSchedulingManager().canSchedule(
                           cluster, vm, vdsBlackList, vdsWhiteList, messages);
        }

        return
                validateVmProperties(vm, messages) &&
                validate(validateBootSequence(vm, getVmDisks(), activeIsoDomainId), messages) &&
                validate(validateDisplayType(), messages) &&
                validate(new VmValidator(vm).vmNotLocked(), messages) &&
                validate(snapshotsValidator.vmNotDuringSnapshot(vm.getId()), messages) &&
                ((runInUnknownStatus && vm.getStatus() == VMStatus.Unknown) || validate(validateVmStatusUsingMatrix(vm), messages)) &&
                validate(validateStoragePoolUp(vm, storagePool, getVmImageDisks()), messages) &&
                validate(validateIsoPath(vm, runVmParam.getDiskPath(), runVmParam.getFloppyPath(), activeIsoDomainId), messages)  &&
                validate(vmDuringInitialization(vm), messages) &&
                validate(validateStatelessVm(vm, runVmParam.getRunAsStateless()), messages) &&
                validate(validateFloppy(), messages) &&
                validate(validateStorageDomains(vm,
                        isInternalExecution,
                        filterReadOnlyAndPreallocatedDisks(getVmImageDisks())), messages)
                &&
                validate(validateImagesForRunVm(vm, getVmImageDisks()), messages) &&
                validate(validateDisksPassDiscard(vm), messages) &&
                validate(validateMemorySize(vm), messages) &&
                getSchedulingManager().canSchedule(
                        cluster, vm, vdsBlackList, vdsWhiteList, messages);
    }

    private List<DiskImage> filterReadOnlyAndPreallocatedDisks(List<DiskImage> vmImageDisks) {
        return vmImageDisks.stream()
                .filter(disk -> !(disk.getVolumeType() == VolumeType.Preallocated || disk.getReadOnly()))
                .collect(Collectors.toList());
    }

    private SchedulingManager getSchedulingManager() {
        return Injector.get(SchedulingManager.class);
    }

    protected ValidationResult validateMemorySize(VM vm) {
        int maxSize = VmCommonUtils.maxMemorySizeWithHotplugInMb(vm);
        if (vm.getMemSizeMb() > maxSize) {
            return new ValidationResult(EngineMessage.ACTION_TYPE_FAILED_MEMORY_EXCEEDS_SUPPORTED_LIMIT);
        }

        return ValidationResult.VALID;
    }

    private ValidationResult validateFloppy() {

        if (StringUtils.isNotEmpty(runVmParam.getFloppyPath())
                && !VmValidationUtils.isFloppySupported(vm.getOs(), vm.getCompatibilityVersion())) {
            return new ValidationResult(EngineMessage.ACTION_TYPE_FAILED_ILLEGAL_FLOPPY_IS_NOT_SUPPORTED_BY_OS);
        }

        return ValidationResult.VALID;
    }

    /**
     * @return true if all VM network interfaces are valid
     */
    public ValidationResult validateNetworkInterfaces() {
        ValidationResult validationResult = validateInterfacesAttachedToClusterNetworks(getClusterNetworksNames(), getInterfaceNetworkNames());
        if (!validationResult.isValid()) {
            return validationResult;
        }

        validationResult = validateInterfacesAttachedToVmNetworks(getClusterNetworks(), getInterfaceNetworkNames());
        if (!validationResult.isValid()) {
            return validationResult;
        }

        return ValidationResult.VALID;
    }

    private ValidationResult validateDisplayType() {
        if (!VmValidationUtils.isGraphicsAndDisplaySupported(vm.getOs(),
                vm.getCompatibilityVersion(),
                getVmActiveGraphics(),
                vm.getDefaultDisplayType())) {
            return new ValidationResult(
                    EngineMessage.ACTION_TYPE_FAILED_ILLEGAL_VM_DISPLAY_TYPE_IS_NOT_SUPPORTED_BY_OS);
        }

        return ValidationResult.VALID;
    }

    private Set<GraphicsType> getVmActiveGraphics() {
        if (vm.getGraphicsInfos() != null && !vm.getGraphicsInfos().isEmpty()) { // graphics overriden in runonce
            return vm.getGraphicsInfos().keySet();
        } else {
            List<VmDevice> graphicDevices =
                    DbFacade.getInstance().getVmDeviceDao().getVmDeviceByVmIdAndType(vm.getId(), VmDeviceGeneralType.GRAPHICS);

            Set<GraphicsType> graphicsTypes = new HashSet<>();

            for (VmDevice graphicDevice : graphicDevices) {
                GraphicsType type = GraphicsType.fromString(graphicDevice.getDevice());
                graphicsTypes.add(type);
            }

            return graphicsTypes;
        }
    }

    protected boolean validateVmProperties(VM vm, List<String> messages) {
        return getVmPropertiesUtils().validateVmProperties(
                        vm.getCompatibilityVersion(),
                        vm.getCustomProperties(),
                        messages);
    }

    protected ValidationResult validateBootSequence(VM vm, List<Disk> vmDisks, Guid activeIsoDomainId) {
        BootSequence bootSequence = vm.getBootSequence();
        // Block from running a VM with no HDD when its first boot device is
        // HD and no other boot devices are configured
        if (bootSequence == BootSequence.C && vmDisks.isEmpty()) {
            return new ValidationResult(EngineMessage.VM_CANNOT_RUN_FROM_DISK_WITHOUT_DISK);
        }

        // If CD appears as first and there is no ISO in storage
        // pool/ISO inactive - you cannot run this VM
        if (bootSequence == BootSequence.CD && activeIsoDomainId == null) {
            return new ValidationResult(EngineMessage.VM_CANNOT_RUN_FROM_CD_WITHOUT_ACTIVE_STORAGE_DOMAIN_ISO);
        }

        // if there is network in the boot sequence, check that the
        // vm has network, otherwise the vm cannot be run in vdsm
        if (bootSequence == BootSequence.N
                && getVmNicDao().getAllForVm(vm.getId()).isEmpty()) {
            return new ValidationResult(EngineMessage.VM_CANNOT_RUN_FROM_NETWORK_WITHOUT_NETWORK);
        }

        return ValidationResult.VALID;
    }

    /**
     * Check storage domains. Storage domain status and disk space are checked only for non-HA VMs.
     *
     * @param vm
     *            The VM to run
     * @param isInternalExecution
     *            Command is internal?
     * @param vmImages
     *            The VM's image disks
     * @return <code>true</code> if the VM can be run, <code>false</code> if not
     */
    private ValidationResult validateStorageDomains(VM vm, boolean isInternalExecution,
            List<DiskImage> vmImages) {
        if (vmImages.isEmpty()) {
            return ValidationResult.VALID;
        }

        if (!vm.isAutoStartup() || !isInternalExecution) {
            Set<Guid> storageDomainIds = ImagesHandler.getAllStorageIdsForImageIds(vmImages);
            MultipleStorageDomainsValidator storageDomainValidator =
                    new MultipleStorageDomainsValidator(vm.getStoragePoolId(), storageDomainIds);

            ValidationResult result = storageDomainValidator.allDomainsExistAndActive();
            if (!result.isValid()) {
                return result;
            }

            result = !vm.isAutoStartup() ? storageDomainValidator.allDomainsWithinThresholds()
                    : ValidationResult.VALID;
            if (!result.isValid()) {
                return result;
            }
        }

        return ValidationResult.VALID;
    }

    /**
     * Check isValid only if VM is not HA VM
     */
    private ValidationResult validateImagesForRunVm(VM vm, List<DiskImage> vmDisks) {
        if (vmDisks.isEmpty()) {
            return ValidationResult.VALID;
        }

        return !vm.isAutoStartup() ?
                new DiskImagesValidator(vmDisks).diskImagesNotLocked() : ValidationResult.VALID;
    }

    protected ValidationResult validateDisksPassDiscard(VM vm) {
        Map<Guid, Disk> disksMap = getVmDisks().stream().collect(Collectors.toMap(Disk::getId, Function.identity()));
        Map<Disk, DiskVmElement> diskToDiskVmElement = Injector.get(DiskHandler.class).getDiskToDiskVmElementMap(
                vm.getId(), disksMap);
        Map<Guid, Guid> diskIdToDestSdId = getVmDisks().stream()
                .collect(Collectors.toMap(Disk::getId,
                        disk -> disk.getDiskStorageType() == DiskStorageType.IMAGE ?
                                ((DiskImage) disk).getStorageIds().get(0) : Guid.Empty));

        MultipleDiskVmElementValidator multipleDiskVmElementValidator =
                createMultipleDiskVmElementValidator(diskToDiskVmElement);
        return multipleDiskVmElementValidator.isPassDiscardSupportedForDestSds(diskIdToDestSdId);
    }

    protected MultipleDiskVmElementValidator createMultipleDiskVmElementValidator(
            Map<Disk, DiskVmElement> diskToDiskVmElement) {
        return new MultipleDiskVmElementValidator(diskToDiskVmElement);
    }

    private ValidationResult validateIsoPath(VM vm, String diskPath, String floppyPath, Guid activeIsoDomainId) {
        if (vm.isAutoStartup()) {
            return ValidationResult.VALID;
        }

        if (StringUtils.isEmpty(vm.getIsoPath()) && StringUtils.isEmpty(diskPath) && StringUtils.isEmpty(floppyPath)) {
            return ValidationResult.VALID;
        }

        if (activeIsoDomainId == null) {
            return new ValidationResult(EngineMessage.VM_CANNOT_RUN_FROM_CD_WITHOUT_ACTIVE_STORAGE_DOMAIN_ISO);
        }

        if (!StringUtils.isEmpty(diskPath) && !isRepoImageExists(diskPath, activeIsoDomainId, ImageFileType.ISO)) {
            return new ValidationResult(EngineMessage.ERROR_CANNOT_FIND_ISO_IMAGE_PATH);
        }

        if (!StringUtils.isEmpty(floppyPath) && !isRepoImageExists(floppyPath, activeIsoDomainId, ImageFileType.Floppy)) {
            return new ValidationResult(EngineMessage.ERROR_CANNOT_FIND_FLOPPY_IMAGE_PATH);
        }

        return ValidationResult.VALID;
    }

    protected ValidationResult vmDuringInitialization(VM vm) {
        if (vm.isRunning() || vm.getStatus() == VMStatus.NotResponding ||
                isVmDuringInitiating(vm)) {
            return new ValidationResult(EngineMessage.ACTION_TYPE_FAILED_VM_IS_RUNNING);
        }

        return ValidationResult.VALID;
    }

    private ValidationResult validateVdsStatus(VM vm) {
        if (vm.getStatus() == VMStatus.Paused && vm.getRunOnVds() != null &&
                getVdsDynamic(vm.getRunOnVds()).getStatus() != VDSStatus.Up) {
            return new ValidationResult(
                    EngineMessage.ACTION_TYPE_FAILED_VDS_STATUS_ILLEGAL,
                    EngineMessage.VAR__HOST_STATUS__UP.toString());
        }

        return ValidationResult.VALID;
    }

    protected ValidationResult validateStatelessVm(VM vm, Boolean stateless) {
        // if the VM is not stateless, there is nothing to check
        if (stateless != null ? !stateless : !vm.isStateless()) {
            return ValidationResult.VALID;
        }

        ValidationResult previewValidation = snapshotsValidator.vmNotInPreview(vm.getId());
        if (!previewValidation.isValid()) {
            return previewValidation;
        }

        // if the VM itself is stateless or run once as stateless
        if (vm.isAutoStartup()) {
            return new ValidationResult(EngineMessage.VM_CANNOT_RUN_STATELESS_HA);
        }

        ValidationResult hasSpaceValidation = hasSpaceForSnapshots();
        if (!hasSpaceValidation.isValid()) {
            return hasSpaceValidation;
        }
        return isImagesExceededVolumesInImageChain();
    }

    private ValidationResult validateVmStatusUsingMatrix(VM vm) {
        if (!VdcActionUtils.canExecute(Collections.singletonList(vm), VM.class,
                VdcActionType.RunVm)) {
            return new ValidationResult(EngineMessage.ACTION_TYPE_FAILED_VM_STATUS_ILLEGAL, LocalizedVmStatus.from(vm.getStatus()));
        }

        return ValidationResult.VALID;
    }


    /**
     * Validates the number of volumes have not exceeded the maximum limit of volumes in an image chain.
     */
    private ValidationResult isImagesExceededVolumesInImageChain() {
        List<DiskImage> allImageDisks =
                DisksFilter.filterImageDisks(getDiskDao().getAllForVm(vm.getId()), ONLY_SNAPABLE);

        DiskImagesValidator diskImagesValidatorForChain = createDiskImageValidator(allImageDisks);
        return diskImagesValidatorForChain.diskImagesHaveNotExceededMaxNumberOfVolumesInImageChain();
    }


    /**
     * check that we can create snapshots for all disks
     * return true if all storage domains have enough space to create snapshots for this VM plugged disks
     */
    protected ValidationResult hasSpaceForSnapshots() {
        List<Disk> disks = DbFacade.getInstance().getDiskDao().getAllForVm(vm.getId());
        List<DiskImage> allDisks = DisksFilter.filterImageDisks(disks, ONLY_SNAPABLE);

        Set<Guid> sdIds = ImagesHandler.getAllStorageIdsForImageIds(allDisks);

        MultipleStorageDomainsValidator msdValidator = getStorageDomainsValidator(sdIds);
        ValidationResult retVal = msdValidator.allDomainsWithinThresholds();
        if (retVal == ValidationResult.VALID) {
            return msdValidator.allDomainsHaveSpaceForNewDisks(allDisks);
        }
        return retVal;
    }

    private MultipleStorageDomainsValidator getStorageDomainsValidator(Collection<Guid> sdIds) {
        Guid spId = vm.getStoragePoolId();
        return new MultipleStorageDomainsValidator(spId, sdIds);
    }

    private DiskImagesValidator createDiskImageValidator(List<DiskImage> disksList) {
        return new DiskImagesValidator(disksList);
    }

    private ValidationResult validateStoragePoolUp(VM vm, StoragePool storagePool, List<DiskImage> vmImages) {
        if (vmImages.isEmpty() || vm.isAutoStartup()) {
            return ValidationResult.VALID;
        }

        return new StoragePoolValidator(storagePool).isUp();
    }

    /**
     * @param clusterNetworkNames cluster logical networks names
     * @param interfaceNetworkNames VM interface network names
     * @return true if all VM network interfaces are attached to existing cluster networks
     */
    private ValidationResult validateInterfacesAttachedToClusterNetworks(
            final Set<String> clusterNetworkNames, final Set<String> interfaceNetworkNames) {

        Set<String> result = new HashSet<>(interfaceNetworkNames);
        result.removeAll(clusterNetworkNames);
        result.remove(null);

        // If after removing the cluster network names we still have objects, then we have interface on networks that
        // aren't attached to the cluster
        return result.isEmpty() ?
                ValidationResult.VALID
                : new ValidationResult(
                        EngineMessage.ACTION_TYPE_FAILED_NETWORK_NOT_IN_CLUSTER,
                        String.format("$networks %1$s", StringUtils.join(result, ",")));
    }

    /**
     * @param clusterNetworks
     *            cluster logical networks
     * @param interfaceNetworkNames
     *            VM interface network names
     * @return true if all VM network interfaces are attached to VM networks
     */
    private ValidationResult validateInterfacesAttachedToVmNetworks(final List<Network> clusterNetworks,
            Set<String> interfaceNetworkNames) {
        List<String> nonVmNetworkNames =
                NetworkUtils.filterNonVmNetworkNames(clusterNetworks, interfaceNetworkNames);

        return nonVmNetworkNames.isEmpty() ?
                ValidationResult.VALID
                : new ValidationResult(
                        EngineMessage.ACTION_TYPE_FAILED_NOT_A_VM_NETWORK,
                        String.format("$networks %1$s", StringUtils.join(nonVmNetworkNames, ",")));
    }

    ///////////////////////
    /// Utility methods ///
    ///////////////////////

    private boolean validate(ValidationResult validationResult, List<String> message) {
        if (!validationResult.isValid()) {
            message.addAll(validationResult.getMessagesAsStrings());
            message.addAll(validationResult.getVariableReplacements());
        }
        return validationResult.isValid();
    }

    private NetworkDao getNetworkDao() {
        return DbFacade.getInstance().getNetworkDao();
    }

    private VdsDynamicDao getVdsDynamicDao() {
        return DbFacade.getInstance().getVdsDynamicDao();
    }

    private BackendInternal getBackend() {
        return Backend.getInstance();
    }

    protected VmNicDao getVmNicDao() {
        return DbFacade.getInstance().getVmNicDao();
    }

    protected VmPropertiesUtils getVmPropertiesUtils() {
        return VmPropertiesUtils.getInstance();
    }

    private DiskDao getDiskDao() {
        return DbFacade.getInstance().getDiskDao();
    }

    private boolean isRepoImageExists(String repoImagePath, Guid storageDomainId, ImageFileType imageFileType) {
        VdcQueryReturnValue ret = getBackend().runInternalQuery(
                VdcQueryType.GetImagesList,
                new GetImagesListParameters(storageDomainId, imageFileType));

        if (ret != null && ret.getReturnValue() != null && ret.getSucceeded()) {
            for (RepoImage isoFileMetaData : ret.<List<RepoImage>>getReturnValue()) {
                if (repoImagePath.equals(isoFileMetaData.getRepoImageId())) {
                    return true;
                }
            }
        }
        return false;
    }

    protected boolean isVmDuringInitiating(VM vm) {
        return (Boolean) getBackend()
                .getResourceManager()
                .runVdsCommand(VDSCommandType.IsVmDuringInitiating,
                        new IsVmDuringInitiatingVDSCommandParameters(vm.getId()))
                .getReturnValue();
    }

    private VdsDynamic getVdsDynamic(Guid vdsId) {
        return getVdsDynamicDao().get(vdsId);
    }

    protected List<Disk> getVmDisks() {
        if (cachedVmDisks == null) {
            cachedVmDisks = getDiskDao().getAllForVm(vm.getId(), true);
        }

        return cachedVmDisks;
    }

    private List<DiskImage> getVmImageDisks() {
        if (cachedVmImageDisks == null) {
            cachedVmImageDisks = DisksFilter.filterImageDisks(getVmDisks(), ONLY_NOT_SHAREABLE);
            cachedVmImageDisks.addAll(DisksFilter.filterCinderDisks(getVmDisks(), ONLY_PLUGGED));
        }

        return cachedVmImageDisks;
    }

    private Set<String> getInterfaceNetworkNames() {
        if (cachedInterfaceNetworkNames == null) {
            cachedInterfaceNetworkNames =
                    vm.getInterfaces().stream().map(VmNetworkInterface::getNetworkName).collect(Collectors.toSet());
        }

        return cachedInterfaceNetworkNames;
    }

    private List<Network> getClusterNetworks() {
        if (cachedClusterNetworks == null) {
            cachedClusterNetworks = getNetworkDao().getAllForCluster(vm.getClusterId());
        }

        return cachedClusterNetworks;
    }

    private Set<String> getClusterNetworksNames() {
        if (cachedClusterNetworksNames == null) {
            cachedClusterNetworksNames = getClusterNetworks().stream().map(Network::getName).collect(Collectors.toSet());
        }

        return cachedClusterNetworksNames;
    }
}

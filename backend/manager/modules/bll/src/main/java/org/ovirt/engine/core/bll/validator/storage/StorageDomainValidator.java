package org.ovirt.engine.core.bll.validator.storage;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Supplier;

import org.ovirt.engine.core.bll.ValidationResult;
import org.ovirt.engine.core.bll.storage.disk.image.ImagesHandler;
import org.ovirt.engine.core.bll.storage.utils.BlockStorageDiscardFunctionalityHelper;
import org.ovirt.engine.core.common.FeatureSupported;
import org.ovirt.engine.core.common.businessentities.StorageDomain;
import org.ovirt.engine.core.common.businessentities.StorageDomainDynamic;
import org.ovirt.engine.core.common.businessentities.StorageDomainStatic;
import org.ovirt.engine.core.common.businessentities.StorageDomainStatus;
import org.ovirt.engine.core.common.businessentities.StorageDomainType;
import org.ovirt.engine.core.common.businessentities.StorageFormatType;
import org.ovirt.engine.core.common.businessentities.StoragePoolIsoMap;
import org.ovirt.engine.core.common.businessentities.storage.DiskImage;
import org.ovirt.engine.core.common.businessentities.storage.LUNs;
import org.ovirt.engine.core.common.businessentities.storage.StorageType;
import org.ovirt.engine.core.common.businessentities.storage.VolumeFormat;
import org.ovirt.engine.core.common.businessentities.storage.VolumeType;
import org.ovirt.engine.core.common.constants.StorageConstants;
import org.ovirt.engine.core.common.errors.EngineMessage;
import org.ovirt.engine.core.compat.Version;
import org.ovirt.engine.core.di.Injector;

public class StorageDomainValidator {

    private static final long INITIAL_BLOCK_ALLOCATION_SIZE = 1024L * 1024L * 1024L;
    private static final long EMPTY_QCOW_HEADER_SIZE = 1024L * 1024L;

    private final StorageDomain storageDomain;

    public StorageDomainValidator(StorageDomain domain) {
        storageDomain = domain;
    }

    public ValidationResult isDomainExist() {
        if (storageDomain == null) {
            return new ValidationResult(EngineMessage.ACTION_TYPE_FAILED_STORAGE_DOMAIN_NOT_EXIST);
        }
        return ValidationResult.VALID;
    }

    public ValidationResult isDomainExistAndActive() {
        ValidationResult domainExistValidation = isDomainExist();
        if (!ValidationResult.VALID.equals(domainExistValidation)) {
            return domainExistValidation;
        }
        if (storageDomain.getStatus() != StorageDomainStatus.Active) {
            return new ValidationResult(EngineMessage.ACTION_TYPE_FAILED_STORAGE_DOMAIN_STATUS_ILLEGAL2,
                    String.format("$%1$s %2$s", "status", storageDomain.getStatus().name()));
        }
        return ValidationResult.VALID;
    }

    public ValidationResult domainIsValidDestination() {
        if (storageDomain.getStorageDomainType().isIsoOrImportExportDomain()) {
            return new ValidationResult(EngineMessage.ACTION_TYPE_FAILED_STORAGE_DOMAIN_TYPE_ILLEGAL);
        }
        return ValidationResult.VALID;
    }

    public ValidationResult isDomainWithinThresholds() {
        if (storageDomain.getStorageType().isCinderDomain()) {
            return ValidationResult.VALID;
        }
        StorageDomainDynamic dynamicData = storageDomain.getStorageDynamicData();
        StorageDomainStatic staticData = storageDomain.getStorageStaticData();
        if (dynamicData != null && staticData != null
                && dynamicData.getAvailableDiskSize() != null
                && staticData.getCriticalSpaceActionBlocker() != null
                && dynamicData.getAvailableDiskSize() < staticData.getCriticalSpaceActionBlocker()) {
            return new ValidationResult(EngineMessage.ACTION_TYPE_FAILED_DISK_SPACE_LOW_ON_STORAGE_DOMAIN,
                    storageName());
        }
        return ValidationResult.VALID;
    }

    private String storageName() {
        return String.format("$%1$s %2$s", "storageName", storageDomain.getStorageName());
    }

    /**
     * Verify there's enough space in the storage domain for creating new DiskImages.
     * Some space should be allocated on the storage domain according to the volumes type and format, and allocation policy,
     * according to the following table:
     *
     *      | File Domain                             | Block Domain
     * -----|-----------------------------------------|-------------
     * qcow | 1M (header size)                        | 1G
     * -----|-----------------------------------------|-------------
     * raw  | preallocated: disk capacity (getSize()) | disk capacity
     *      | thin (sparse): 1M                       | (there is no raw sparse on
     *      |                                         | block domains)
     *
     */
    private double getTotalSizeForNewDisks(Collection<DiskImage> diskImages) {
        return getTotalSizeForDisksByMethod(diskImages, diskImage -> {
            double sizeForDisk = diskImage.getSize();
            if (diskImage.getVolumeFormat() == VolumeFormat.COW) {
                if (storageDomain.getStorageType().isFileDomain()) {
                    sizeForDisk = EMPTY_QCOW_HEADER_SIZE;
                } else {
                    sizeForDisk = INITIAL_BLOCK_ALLOCATION_SIZE;
                }
            } else if (diskImage.getVolumeType() == VolumeType.Sparse) {
                sizeForDisk = EMPTY_QCOW_HEADER_SIZE;
            }
            return sizeForDisk;
        });
    }

    /**
     * Returns the required space in the storage domain for creating cloned DiskImages with collapse.
     * */
    private double getTotalSizeForClonedDisks(Collection<DiskImage> diskImages) {
        return getTotalSizeForDisksByMethod(diskImages, this::getTotalSizeForClonedDisk);
    }

    /**
     * Calculates the required space in the storage domain for creating cloned DiskImages with collapse.
     * When creating COW volume the actual used space will be the needed space * QCOW_OVERHEAD_FACTOR as implemented
     * currently in the VDSM code.
     *
     * */
    private double getTotalSizeForClonedDisk(DiskImage diskImage) {
        double sizeForDisk = ImagesHandler.getTotalActualSizeOfDisk(diskImage, storageDomain.getStorageStaticData());

        if (diskImage.getVolumeFormat() == VolumeFormat.COW) {
            sizeForDisk = Math.ceil(StorageConstants.QCOW_OVERHEAD_FACTOR * sizeForDisk);
        }
        return sizeForDisk;
    }

    /**
     * Verify there's enough space in the storage domain for creating cloned DiskImages with snapshots without collapse.
     * Space should be allocated according to the volumes type and format, and allocation policy,
     * according to the following table:
     *
     *      | File Domain                             | Block Domain
     * -----|-----------------------------------------|----------------
     * qcow | 1.1 * used space                        |1.1 * used space
     * -----|-----------------------------------------|----------------
     * raw  | preallocated: disk capacity             |disk capacity
     *      | sparse: used space                      |
     *
     * */
    private double getTotalSizeForDisksWithSnapshots(Collection<DiskImage> diskImages) {
        return getTotalSizeForDisksByMethod(diskImages, diskImage -> {
            double sizeForDisk = diskImage.getSize();
            if ((storageDomain.getStorageType().isFileDomain() && diskImage.getVolumeType() == VolumeType.Sparse)
                || diskImage.getVolumeFormat() == VolumeFormat.COW) {
                sizeForDisk = diskImage.getActualDiskWithSnapshotsSizeInBytes();
            }

            if (diskImage.getVolumeFormat() == VolumeFormat.COW) {
                sizeForDisk = Math.ceil(StorageConstants.QCOW_OVERHEAD_FACTOR * sizeForDisk);
            }
            return sizeForDisk;
        });
    }

    /**
     * Validate space for new, empty disks. Used for a new Active Image.
     */
    public ValidationResult hasSpaceForNewDisks(Collection<DiskImage> diskImages) {
        if (storageDomain.getStorageType().isCinderDomain()) {
            return ValidationResult.VALID;
        }
        Long availableSize = storageDomain.getAvailableDiskSizeInBytes();
        double totalSizeForDisks = getTotalSizeForNewDisks(diskImages);

        return validateRequiredSpace(availableSize, totalSizeForDisks);
    }

    /**
     * Validate space for a cloned disk with the collapse option.
     */
    public ValidationResult hasSpaceForClonedDisks(Collection<DiskImage> diskImages) {
        if (storageDomain.getStorageType().isCinderDomain()) {
            return ValidationResult.VALID;
        }
        Long availableSize = storageDomain.getAvailableDiskSizeInBytes();
        double totalSizeForDisks = getTotalSizeForClonedDisks(diskImages);

        return validateRequiredSpace(availableSize, totalSizeForDisks);
    }

    /**
     * Validate space for cloned disks without the collapse option. Every snapshot will be cloned.
     */
    public ValidationResult hasSpaceForDisksWithSnapshots(Collection<DiskImage> diskImages) {
        if (storageDomain.getStorageType().isCinderDomain()) {
            return ValidationResult.VALID;
        }
        Long availableSize = storageDomain.getAvailableDiskSizeInBytes();
        double totalSizeForDisks = getTotalSizeForDisksWithSnapshots(diskImages);

        return validateRequiredSpace(availableSize, totalSizeForDisks);
    }

    /**
     * Validate space for new and cloned (with collapse) disks. When this option is needed a combined method should be
     * used in order to check the space on the domain for the two types of validation, done here.
     * Note that at this time there is no need for the same functionality for clone without collapse,
     * so there's no method for this.
     */
    public ValidationResult hasSpaceForAllDisks(Collection<DiskImage> newDiskImages, Collection<DiskImage> clonedDiskImages) {
        if (storageDomain.getStorageType().isCinderDomain()) {
            return ValidationResult.VALID;
        }
        Long availableSize = storageDomain.getAvailableDiskSizeInBytes();
        double totalSizeForNewDisks = getTotalSizeForNewDisks(newDiskImages);
        double totalSizeForClonedDisks = getTotalSizeForClonedDisks(clonedDiskImages);
        double totalSizeForDisks = totalSizeForNewDisks + totalSizeForClonedDisks;

        return validateRequiredSpace(availableSize, totalSizeForDisks);
    }

    /**
     * Validate space for a cloned disk without the collapse option. Every snapshot will be cloned.
     */
    public ValidationResult hasSpaceForDiskWithSnapshots(DiskImage diskImage) {
        return hasSpaceForDisksWithSnapshots(Collections.singleton(diskImage));
    }

    /**
     * Validate space for a cloned disk with the collapse option.
     */
    public ValidationResult hasSpaceForClonedDisk(DiskImage diskImage) {
        return hasSpaceForClonedDisks(Collections.singleton(diskImage));
    }

    /**
     * Validate space for a new, empty disk. Used for a new Active Image.
     */
    public ValidationResult hasSpaceForNewDisk(DiskImage diskImage) {
        return hasSpaceForNewDisks(Collections.singleton(diskImage));
    }

    private ValidationResult validateRequiredSpace(Long availableSize, double requiredSize) {
        // If availableSize is not yet set, we'll allow the operation.
        if (availableSize == null || availableSize.doubleValue() >= requiredSize) {
            return ValidationResult.VALID;
        }

        return new ValidationResult(EngineMessage.ACTION_TYPE_FAILED_DISK_SPACE_LOW_ON_STORAGE_DOMAIN,
                storageName());
    }

    /**
     * Validates all the storage domains by a given predicate.
     *
     * @return {@link ValidationResult#VALID} if all the domains are OK, or the
     * first validation error if they aren't.
     */
    private double getTotalSizeForDisksByMethod(Collection<DiskImage> diskImages, SizeAssessment sizeAssessment) {
        double totalSizeForDisks = 0.0;
        if (diskImages != null) {
            for (DiskImage diskImage : diskImages) {
                double sizeForDisk = sizeAssessment.getSizeForDisk(diskImage);
                totalSizeForDisks += sizeForDisk;
            }
        }
        return totalSizeForDisks;
    }

    @FunctionalInterface
    private static interface SizeAssessment {
        public double getSizeForDisk(DiskImage diskImage);
    }


    public ValidationResult isInProcess() {
        StoragePoolIsoMap domainIsoMap = storageDomain.getStoragePoolIsoMapData();

        if (domainIsoMap.getStatus() != null && domainIsoMap.getStatus().isStorageDomainInProcess()) {
            return new ValidationResult(EngineMessage.ACTION_TYPE_FAILED_STORAGE_DOMAIN_STATUS_ILLEGAL2,
                    String.format("$status %1$s", domainIsoMap.getStatus()));
        }
        return ValidationResult.VALID;
    }

    public ValidationResult isStorageFormatCompatibleWithDomain() {
        StorageFormatType storageFormat = storageDomain.getStorageFormat();
        StorageType storageType = storageDomain.getStorageType();
        StorageDomainType storageDomainFunction = storageDomain.getStorageDomainType();
        boolean validationSucceeded = true;

        // V2 is applicable only for block data storage domains
        if (storageFormat == StorageFormatType.V2) {
            if ( !(storageDomainFunction.isDataDomain() && storageType.isBlockDomain()) ) {
                validationSucceeded = false;
            }
        }

        // Above V3 is applicable only for data storage domains
        if (storageFormat.compareTo(StorageFormatType.V3) >= 0) {
            if (!storageDomainFunction.isDataDomain()) {
                validationSucceeded = false;
            }
        }

        return validationSucceeded? ValidationResult.VALID : new ValidationResult(EngineMessage.ACTION_TYPE_FAILED_STORAGE_DOMAIN_FORMAT_ILLEGAL_HOST,
                    String.format("$storageFormat %1$s", storageDomain.getStorageFormat()));
    }

    public ValidationResult isDataDomain() {
        if (storageDomain.getStorageDomainType().isDataDomain()) {
            return ValidationResult.VALID;
        }
        return new ValidationResult(EngineMessage.ACTION_TYPE_FAILED_ACTION_IS_SUPPORTED_ONLY_FOR_DATA_DOMAINS);
    }

    public ValidationResult isDiscardAfterDeleteLegalForExistingStorageDomain() {
        return isDiscardAfterDeleteLegal(this::discardAfterDeleteLegalForExistingStorageDomainPredicate);
    }

    protected Boolean discardAfterDeleteLegalForExistingStorageDomainPredicate() {
        return Boolean.TRUE.equals(storageDomain.getSupportsDiscard());
    }

    public ValidationResult isDiscardAfterDeleteLegalForNewBlockStorageDomain(Collection<LUNs> luns) {
        return isDiscardAfterDeleteLegal(getDiscardAfterDeleteLegalForNewBlockStorageDomainPredicate(luns));
    }

    protected Supplier<Boolean> getDiscardAfterDeleteLegalForNewBlockStorageDomainPredicate(Collection<LUNs> luns) {
        return () -> Injector.get(BlockStorageDiscardFunctionalityHelper.class).allLunsSupportDiscard(luns);
    }

    protected ValidationResult isDiscardAfterDeleteLegal(Supplier<Boolean> supportsDiscardSupplier) {
        if (!storageDomain.isDiscardAfterDelete()) {
            return ValidationResult.VALID;
        }

        if (storageDomain.getStorageType().isBlockDomain()) {
            if (supportsDiscardSupplier.get()) {
                return ValidationResult.VALID;
            }
            return new ValidationResult(
                    EngineMessage.ACTION_TYPE_FAILED_DISCARD_AFTER_DELETE_NOT_SUPPORTED_BY_UNDERLYING_STORAGE,
                    String.format("$storageDomainName %s", storageDomain.getName()));
        }
        return new ValidationResult(
                EngineMessage.ACTION_TYPE_FAILED_DISCARD_AFTER_DELETE_SUPPORTED_ONLY_BY_BLOCK_DOMAINS);
    }

    public ValidationResult isDiscardAfterDeleteSupportedByDcVersion(Version version) {
        if (storageDomain.isDiscardAfterDelete() && !FeatureSupported.discardAfterDeleteSupported(version)) {
            return new ValidationResult(
                    EngineMessage.ACTION_TYPE_FAILED_DISCARD_AFTER_DELETE_NOT_SUPPORTED_BY_DC_VERSION,
                    String.format("$dataCenterVersion %s", version.toString()));
        }
        return ValidationResult.VALID;
    }
}

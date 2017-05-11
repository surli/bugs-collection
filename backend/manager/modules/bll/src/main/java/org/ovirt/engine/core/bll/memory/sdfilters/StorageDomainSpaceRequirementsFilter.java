package org.ovirt.engine.core.bll.memory.sdfilters;

import java.util.List;
import java.util.function.Predicate;

import org.ovirt.engine.core.bll.memory.MemoryStorageHandler;
import org.ovirt.engine.core.bll.validator.storage.StorageDomainValidator;
import org.ovirt.engine.core.common.businessentities.StorageDomain;
import org.ovirt.engine.core.common.businessentities.storage.DiskImage;


public class StorageDomainSpaceRequirementsFilter implements Predicate<StorageDomain> {

    private MemoryStorageHandler memoryStorageHandler;
    private List<DiskImage> memoryDisks;

    public StorageDomainSpaceRequirementsFilter(MemoryStorageHandler memoryStorageHandler, List<DiskImage> memoryDisks) {
        this.memoryStorageHandler = memoryStorageHandler;
        this.memoryDisks = memoryDisks;
    }

    @Override
    public boolean test(StorageDomain storageDomain) {
        memoryStorageHandler.updateDisksStorage(storageDomain, memoryDisks);
        StorageDomainValidator storageDomainValidator = getStorageDomainValidator(storageDomain);
        return storageDomainValidator.isDomainWithinThresholds().isValid() &&
                storageDomainValidator.hasSpaceForClonedDisks(memoryDisks).isValid();
    }

    protected StorageDomainValidator getStorageDomainValidator(StorageDomain storageDomain) {
        return new StorageDomainValidator(storageDomain);
    }
}

package org.ovirt.engine.core.bll.storage.disk.image;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.lang.StringUtils;
import org.ovirt.engine.core.bll.QueriesCommandBase;
import org.ovirt.engine.core.bll.context.EngineContext;
import org.ovirt.engine.core.common.FeatureSupported;
import org.ovirt.engine.core.common.action.GetCinderEntityByStorageDomainIdParameters;
import org.ovirt.engine.core.common.businessentities.StorageDomain;
import org.ovirt.engine.core.common.businessentities.StoragePool;
import org.ovirt.engine.core.common.businessentities.storage.DiskImage;
import org.ovirt.engine.core.common.businessentities.storage.QemuImageInfo;
import org.ovirt.engine.core.common.businessentities.storage.VolumeFormat;
import org.ovirt.engine.core.common.errors.EngineMessage;
import org.ovirt.engine.core.common.queries.GetUnregisteredDiskQueryParameters;
import org.ovirt.engine.core.common.queries.VdcQueryReturnValue;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.common.vdscommands.GetImageInfoVDSCommandParameters;
import org.ovirt.engine.core.common.vdscommands.StoragePoolDomainAndGroupIdBaseVDSCommandParameters;
import org.ovirt.engine.core.common.vdscommands.VDSCommandType;
import org.ovirt.engine.core.common.vdscommands.VDSReturnValue;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dao.StorageDomainDao;
import org.ovirt.engine.core.dao.StoragePoolDao;

public class GetUnregisteredDiskQuery<P extends GetUnregisteredDiskQueryParameters> extends QueriesCommandBase<P> {
    @Inject
    private StorageDomainDao storageDomainDao;

    @Inject
    private StoragePoolDao storagePoolDao;

    public GetUnregisteredDiskQuery(P parameters) {
        super(parameters);
    }

    public GetUnregisteredDiskQuery(P parameters, EngineContext context) {
        super(parameters, context);
    }

    @Override
    protected void executeQueryCommand() {
        Guid storagePoolId = getParameters().getStoragePoolId();
        Guid storageDomainId = getParameters().getStorageDomainId();
        Guid diskId = getParameters().getDiskId();
        StorageDomain storageDomain = storageDomainDao.get(storageDomainId);
        if (storageDomain == null) {
            getQueryReturnValue().setExceptionString(EngineMessage.STORAGE_DOMAIN_DOES_NOT_EXIST.toString());
            getQueryReturnValue().setSucceeded(false);
            return;
        }

        if (storageDomain.getStorageType().isCinderDomain()) {
            VdcQueryReturnValue returnValue = runInternalQuery(VdcQueryType.GetUnregisteredCinderDiskByIdAndStorageDomainId,
                    new GetCinderEntityByStorageDomainIdParameters(diskId, getParameters().getStorageDomainId()));
            setReturnValue(returnValue.getReturnValue());
            return;
        }

        // Now get the list of volumes for each new image.
        StoragePoolDomainAndGroupIdBaseVDSCommandParameters getVolumesParameters = new StoragePoolDomainAndGroupIdBaseVDSCommandParameters(
                storagePoolId, storageDomainId, diskId);
        VDSReturnValue volumesListReturn = runVdsCommand(VDSCommandType.GetVolumesList, getVolumesParameters);
        if (!volumesListReturn.getSucceeded()) {
            getQueryReturnValue().setExceptionString(volumesListReturn.getExceptionString());
            getQueryReturnValue().setSucceeded(false);
            return;
        }

        @SuppressWarnings("unchecked")
        List<Guid> volumesList = (List<Guid>) volumesListReturn.getReturnValue();

        // We can't deal with snapshots, so there should only be a single volume associated with the
        // image. If there are multiple volumes, skip the image and move on to the next.
        if (volumesList.size() != 1) {
            getQueryReturnValue().setSucceeded(false);
            return;
        }

        Guid volumeId = volumesList.get(0);

        // Get the information about the volume from VDSM.
        GetImageInfoVDSCommandParameters imageInfoParameters = new GetImageInfoVDSCommandParameters(
                storagePoolId, storageDomainId, diskId, volumeId);
        VDSReturnValue imageInfoReturn = runVdsCommand(VDSCommandType.GetImageInfo, imageInfoParameters);

        if (!imageInfoReturn.getSucceeded()) {
            getQueryReturnValue().setExceptionString(imageInfoReturn.getExceptionString());
            getQueryReturnValue().setSucceeded(false);
            return;
        }
        DiskImage newDiskImage = (DiskImage) imageInfoReturn.getReturnValue();
        if (!fetchQcowCompat(storagePoolId, storageDomainId, diskId, volumeId, newDiskImage)) {
            getQueryReturnValue().setSucceeded(false);
            return;
        }
        if (StringUtils.isNotEmpty(newDiskImage.getDescription())) {
            try {
                MetadataDiskDescriptionHandler.getInstance()
                        .enrichDiskByJsonDescription(newDiskImage.getDescription(), newDiskImage);
            } catch (IOException | DecoderException e) {
                log.warn("Exception while parsing JSON for disk. Exception: '{}'", e);
            }
        }
        newDiskImage.setStoragePoolId(storagePoolId);
        getQueryReturnValue().setReturnValue(newDiskImage);
        getQueryReturnValue().setSucceeded(true);
    }

    private boolean fetchQcowCompat(Guid storagePoolId,
            Guid storageDomainId,
            Guid diskId,
            Guid volumeId,
            DiskImage newDiskImage) {
        if (newDiskImage.getVolumeFormat().equals(VolumeFormat.COW)) {
            StoragePool sp = storagePoolDao.get(storagePoolId);
            QemuImageInfo qemuImageInfo = null;
            if (sp != null && FeatureSupported.qcowCompatSupported(sp.getCompatibilityVersion())) {
                qemuImageInfo = ImagesHandler.getQemuImageInfoFromVdsm(storagePoolId,
                        storageDomainId,
                        diskId,
                        volumeId,
                        null,
                        true);
                if (qemuImageInfo == null) {
                    getQueryReturnValue().setExceptionString("Failed to fetch qemu image info from storage");
                    return false;
                }
                newDiskImage.setQcowCompat(qemuImageInfo.getQcowCompat());
            }
        }
        return true;
    }
}

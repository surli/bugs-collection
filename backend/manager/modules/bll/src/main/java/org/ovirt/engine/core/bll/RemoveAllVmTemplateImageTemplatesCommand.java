package org.ovirt.engine.core.bll;

import static org.ovirt.engine.core.bll.storage.disk.image.DisksFilter.ONLY_ACTIVE;

import java.util.Collection;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.ovirt.engine.core.bll.context.CommandContext;
import org.ovirt.engine.core.bll.storage.disk.image.DisksFilter;
import org.ovirt.engine.core.common.action.ImagesContainterParametersBase;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.action.VdcReturnValueBase;
import org.ovirt.engine.core.common.action.VmTemplateManagementParameters;
import org.ovirt.engine.core.common.businessentities.VmDeviceId;
import org.ovirt.engine.core.common.businessentities.storage.DiskImage;
import org.ovirt.engine.core.common.businessentities.storage.ImageStorageDomainMapId;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.TransactionScopeOption;

/**
 * This command responsible to removing all Image Templates, of a VmTemplate
 * on all domains specified in the parameters
 */

@InternalCommandAttribute
public class RemoveAllVmTemplateImageTemplatesCommand<T extends VmTemplateManagementParameters> extends VmTemplateManagementCommand<T> {
    public RemoveAllVmTemplateImageTemplatesCommand(T parameters, CommandContext cmdContext) {
        super(parameters, cmdContext);
        super.setVmTemplateId(parameters.getVmTemplateId());
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void executeCommand() {
        List<DiskImage> imageTemplates = DisksFilter.filterImageDisks(diskDao.getAllForVm(getVmTemplateId()),
                ONLY_ACTIVE);
        for (DiskImage template : imageTemplates) {
            // remove this disk in all domain that were sent
            for (Guid domain : (Collection<Guid>)CollectionUtils.intersection(getParameters().getStorageDomainsList(), template.getStorageIds())) {
                ImagesContainterParametersBase tempVar = new ImagesContainterParametersBase(template.getImageId(),
                        getVmTemplateId());
                tempVar.setStorageDomainId(domain);
                tempVar.setStoragePoolId(template.getStoragePoolId());
                tempVar.setImageGroupID(template.getId());
                tempVar.setEntityInfo(getParameters().getEntityInfo());
                tempVar.setWipeAfterDelete(template.isWipeAfterDelete());
                tempVar.setTransactionScopeOption(TransactionScopeOption.RequiresNew);
                tempVar.setParentCommand(getActionType());
                tempVar.setParentParameters(getParameters());
                VdcReturnValueBase vdcReturnValue = runInternalActionWithTasksContext(
                                VdcActionType.RemoveTemplateSnapshot,
                                tempVar);

                if (vdcReturnValue.getSucceeded()) {
                    getReturnValue().getInternalVdsmTaskIdList().addAll(vdcReturnValue.getInternalVdsmTaskIdList());
                } else {
                    log.error("Can't remove image id '{}' for template id '{}' from domain id '{}' due to: {}.",
                            template.getImageId(), getVmTemplateId(), domain,
                            vdcReturnValue.getFault().getMessage());
                }

                imageStorageDomainMapDao.remove(new ImageStorageDomainMapId(template.getImageId(), domain));
            }

            DiskImage diskImage = diskImageDao.get(template.getImageId());
            if (diskImage != null) {
                baseDiskDao.remove(template.getId());
                vmDeviceDao.remove(new VmDeviceId(diskImage.getImageId(), getVmTemplateId()));
                imageStorageDomainMapDao.remove(diskImage.getImageId());
                imageDao.remove(template.getImageId());
            }
        }
        setSucceeded(true);
    }
}

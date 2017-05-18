package org.ovirt.engine.ui.uicommonweb.models.vms.register;

import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.ImportVmData;

public class RegisterVmData extends ImportVmData {
    private EntityModel<Boolean> reassignMacs;
    private EntityModel<Boolean> badMacsExist;

    public RegisterVmData(VM vm) {
        super(vm);

        setReassignMacs(new EntityModel<>(false));
        setBadMacsExist(new EntityModel<>(false));
    }

    public EntityModel<Boolean> getReassignMacs() {
        return reassignMacs;
    }

    public void setReassignMacs(EntityModel<Boolean> reassignMacs) {
        this.reassignMacs = reassignMacs;
    }

    public EntityModel<Boolean> getBadMacsExist() {
        return badMacsExist;
    }

    public void setBadMacsExist(EntityModel<Boolean> badMacsExist) {
        this.badMacsExist = badMacsExist;
    }
}

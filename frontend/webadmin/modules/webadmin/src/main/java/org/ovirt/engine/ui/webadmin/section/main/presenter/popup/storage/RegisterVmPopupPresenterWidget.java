package org.ovirt.engine.ui.webadmin.section.main.presenter.popup.storage;

import org.ovirt.engine.ui.common.presenter.AbstractModelBoundPopupPresenterWidget;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.uicommonweb.models.Model;
import org.ovirt.engine.ui.uicommonweb.models.storage.RegisterVmModel;
import org.ovirt.engine.ui.webadmin.section.main.presenter.popup.vm.register.VnicProfileMappingPopupPresenterWidget;

import com.google.gwt.event.shared.EventBus;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class RegisterVmPopupPresenterWidget extends AbstractModelBoundPopupPresenterWidget<RegisterVmModel, RegisterVmPopupPresenterWidget.ViewDef> {

    private final Provider<VnicProfileMappingPopupPresenterWidget> vnicProfileMappingPopupProvider;

    public interface ViewDef extends AbstractModelBoundPopupPresenterWidget.ViewDef<RegisterVmModel> {
    }

    @Inject
    public RegisterVmPopupPresenterWidget(EventBus eventBus,
            ViewDef view,
            Provider<VnicProfileMappingPopupPresenterWidget> vnicProfileMappingPopupProvider) {
        super(eventBus, view);
        this.vnicProfileMappingPopupProvider = vnicProfileMappingPopupProvider;
    }

    @Override
    public AbstractModelBoundPopupPresenterWidget<? extends Model, ?> getModelPopup(RegisterVmModel source,
            UICommand lastExecutedCommand,
            Model windowModel) {
        if (lastExecutedCommand == source.getVnicProfileMappingCommand()) {
            return vnicProfileMappingPopupProvider.get();
        }
        return super.getModelPopup(source, lastExecutedCommand, windowModel);
    }
}

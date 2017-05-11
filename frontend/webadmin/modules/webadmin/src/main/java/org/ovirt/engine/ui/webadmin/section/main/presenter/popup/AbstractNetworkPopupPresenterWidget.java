package org.ovirt.engine.ui.webadmin.section.main.presenter.popup;

import org.ovirt.engine.ui.common.presenter.AbstractTabbedModelBoundPopupPresenterWidget;
import org.ovirt.engine.ui.common.widget.UiCommandButton;
import org.ovirt.engine.ui.uicommonweb.models.HasValidatedTabs;
import org.ovirt.engine.ui.uicommonweb.models.datacenters.NetworkModel;
import com.google.gwt.event.shared.EventBus;

public class AbstractNetworkPopupPresenterWidget<T extends NetworkModel & HasValidatedTabs,
    V extends AbstractNetworkPopupPresenterWidget.ViewDef<T>>
        extends AbstractTabbedModelBoundPopupPresenterWidget<T, V> {

    public interface ViewDef<T extends NetworkModel> extends AbstractTabbedModelBoundPopupPresenterWidget.ViewDef<T> {

        void setMessageLabel(String label);

        void updateVisibility();

        void toggleSubnetVisibility(boolean visible);

        void toggleProfilesVisibility(boolean visible);

        UiCommandButton getQosButton();

        void addMtuEditor();
    }

    public AbstractNetworkPopupPresenterWidget(EventBus eventBus, V view) {
        super(eventBus, view);
    }

    @Override
    public void init(final T model) {
        // Let the parent do its work
        super.init(model);

        // Listen to Properties
        model.getPropertyChangedEvent().addListener((ev, sender, args) -> {
            NetworkModel senderModel = (NetworkModel) sender;
            String propertyName = args.propertyName;

            if ("Message".equals(propertyName)) { //$NON-NLS-1$
                getView().setMessageLabel(senderModel.getMessage());
            }
        });

        getView().toggleSubnetVisibility(model.getExport().getEntity());
        model.getExport().getEntityChangedEvent().addListener((ev, sender, args) ->
                getView().toggleSubnetVisibility(model.getExport().getEntity()));

        getView().toggleProfilesVisibility(model.getProfiles().getIsAvailable());
        model.getProfiles().getPropertyChangedEvent().addListener((ev, sender, args) -> {
            if ("IsAvailable".equals(args.propertyName)) { //$NON-NLS-1$
                getView().toggleProfilesVisibility(model.getProfiles().getIsAvailable());
            }
        });

        getView().getQosButton().setCommand(model.getAddQosCommand());
        getView().getQosButton().addClickHandler(event -> getView().getQosButton().getCommand().execute());

        getView().addMtuEditor();
    }

    @Override
    protected void onReveal() {
        super.onReveal();
        getView().updateVisibility();
    }
}

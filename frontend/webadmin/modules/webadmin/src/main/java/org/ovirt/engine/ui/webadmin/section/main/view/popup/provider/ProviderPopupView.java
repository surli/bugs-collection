package org.ovirt.engine.ui.webadmin.section.main.view.popup.provider;

import org.gwtbootstrap3.client.ui.Row;
import org.ovirt.engine.core.common.businessentities.ProviderType;
import org.ovirt.engine.core.common.businessentities.StoragePool;
import org.ovirt.engine.ui.common.editor.UiCommonEditorDriver;
import org.ovirt.engine.ui.common.idhandler.ElementIdHandler;
import org.ovirt.engine.ui.common.idhandler.WithElementId;
import org.ovirt.engine.ui.common.view.popup.AbstractModelBoundPopupView;
import org.ovirt.engine.ui.common.widget.Align;
import org.ovirt.engine.ui.common.widget.HasUiCommandClickHandlers;
import org.ovirt.engine.ui.common.widget.UiCommandButton;
import org.ovirt.engine.ui.common.widget.dialog.SimpleDialogPanel;
import org.ovirt.engine.ui.common.widget.dialog.tab.DialogTab;
import org.ovirt.engine.ui.common.widget.editor.ListModelListBoxEditor;
import org.ovirt.engine.ui.common.widget.editor.generic.EntityModelCheckBoxEditor;
import org.ovirt.engine.ui.common.widget.editor.generic.ListModelSuggestBoxEditor;
import org.ovirt.engine.ui.common.widget.editor.generic.StringEntityModelPasswordBoxEditor;
import org.ovirt.engine.ui.common.widget.editor.generic.StringEntityModelTextBoxEditor;
import org.ovirt.engine.ui.common.widget.renderer.EnumRenderer;
import org.ovirt.engine.ui.uicommonweb.models.providers.ProviderModel;
import org.ovirt.engine.ui.uicompat.ConstantsManager;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.ApplicationResources;
import org.ovirt.engine.ui.webadmin.gin.AssetProvider;
import org.ovirt.engine.ui.webadmin.section.main.presenter.popup.provider.ProviderPopupPresenterWidget;
import org.ovirt.engine.ui.webadmin.widget.provider.KVMPropertiesWidget;
import org.ovirt.engine.ui.webadmin.widget.provider.NeutronAgentWidget;
import org.ovirt.engine.ui.webadmin.widget.provider.VmwarePropertiesWidget;
import org.ovirt.engine.ui.webadmin.widget.provider.XENPropertiesWidget;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.text.shared.AbstractRenderer;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.inject.Inject;

public class ProviderPopupView extends AbstractModelBoundPopupView<ProviderModel> implements ProviderPopupPresenterWidget.ViewDef {

    interface Driver extends UiCommonEditorDriver<ProviderModel, ProviderPopupView> {}

    private final Driver driver = GWT.create(Driver.class);

    interface ViewUiBinder extends UiBinder<SimpleDialogPanel, ProviderPopupView> {
        ViewUiBinder uiBinder = GWT.create(ViewUiBinder.class);
    }

    interface ViewIdHandler extends ElementIdHandler<ProviderPopupView> {
        ViewIdHandler idHandler = GWT.create(ViewIdHandler.class);
    }

    private static final ApplicationResources resources = AssetProvider.getResources();
    private static final ApplicationConstants constants = AssetProvider.getConstants();

    @UiField
    @Path(value = "name.entity")
    @WithElementId
    StringEntityModelTextBoxEditor nameEditor;

    @UiField
    @Path(value = "description.entity")
    @WithElementId
    StringEntityModelTextBoxEditor descriptionEditor;

    @UiField(provided = true)
    @Path(value = "type.selectedItem")
    @WithElementId
    ListModelListBoxEditor<ProviderType> typeEditor;

    @UiField(provided = true)
    @Path(value = "dataCenter.selectedItem")
    @WithElementId
    ListModelListBoxEditor<StoragePool> datacenterEditor;

    @UiField
    @Path(value = "url.entity")
    @WithElementId
    StringEntityModelTextBoxEditor urlEditor;

    @UiField
    UiCommandButton testButton;

    @UiField
    Image testResultImage;

    @UiField
    @Ignore
    Label testResultMessage;

    @UiField(provided = true)
    @Path(value = "requiresAuthentication.entity")
    @WithElementId
    EntityModelCheckBoxEditor requiresAuthenticationEditor;

    @UiField
    @Path(value = "username.entity")
    @WithElementId
    StringEntityModelTextBoxEditor usernameEditor;

    @UiField
    @Path(value = "password.entity")
    @WithElementId
    StringEntityModelPasswordBoxEditor passwordEditor;

    @UiField
    @Path(value = "tenantName.entity")
    @WithElementId
    StringEntityModelTextBoxEditor tenantNameEditor;

    @UiField
    @Path(value = "authUrl.entity")
    @WithElementId
    StringEntityModelTextBoxEditor authUrlEditor;

    @UiField
    @Path(value = "pluginType.selectedItem")
    @WithElementId
    ListModelSuggestBoxEditor pluginTypeEditor;

    @UiField
    @WithElementId
    DialogTab generalTab;

    @UiField
    @Ignore
    DialogTab agentConfigurationTab;

    @UiField
    Row typeEditorRow;

    @UiField
    Row datacenterEditorRow;

    @UiField
    @Ignore
    NeutronAgentWidget neutronAgentWidget;

    @UiField
    @Ignore
    VmwarePropertiesWidget vmwarePropertiesWidget;

    @UiField
    @Ignore
    KVMPropertiesWidget kvmPropertiesWidget;

    @UiField
    @Ignore
    XENPropertiesWidget xenPropertiesWidget;

    @UiField(provided = true)
    @Path(value = "readOnly.entity")
    @WithElementId
    EntityModelCheckBoxEditor readOnlyEditor;

    @UiField
    Style style;

    private ProviderModel providerModel;

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Inject
    public ProviderPopupView(EventBus eventBus) {
        super(eventBus);

        typeEditor = new ListModelListBoxEditor<>(new EnumRenderer());
        datacenterEditor = new ListModelListBoxEditor<>(new AbstractRenderer<StoragePool>() {
            @Override
            public String render(StoragePool storagePool) {
                return storagePool != null ? storagePool.getName() :
                    ConstantsManager.getInstance().getConstants().anyDataCenter();
            }
        });
        requiresAuthenticationEditor = new EntityModelCheckBoxEditor(Align.RIGHT);
        readOnlyEditor = new EntityModelCheckBoxEditor(Align.RIGHT);

        initWidget(ViewUiBinder.uiBinder.createAndBindUi(this));
        ViewIdHandler.idHandler.generateAndSetIds(this);
        driver.initialize(this);
    }

    @Override
    public void edit(ProviderModel model) {
        providerModel = model;
        setAgentTabVisibility(model.getNeutronAgentModel().isPluginConfigurationAvailable().getEntity());
        driver.edit(model);
        neutronAgentWidget.edit(model.getNeutronAgentModel());
        vmwarePropertiesWidget.edit(model.getVmwarePropertiesModel());
        kvmPropertiesWidget.edit(model.getKvmPropertiesModel());
        xenPropertiesWidget.edit(model.getXenPropertiesModel());
    }

    @Override
    public ProviderModel flush() {
        neutronAgentWidget.flush();
        vmwarePropertiesWidget.flush();
        kvmPropertiesWidget.flush();
        xenPropertiesWidget.flush();
        return driver.flush();
    }

    @Override
    public void cleanup() {
        driver.cleanup();
    }

    @Override
    public void focusInput() {
        nameEditor.setFocus(true);
    }

    interface Style extends CssResource {
        String contentStyle();
        String testResultImage();
        String headerSeparator();
    }

    @Override
    public HasUiCommandClickHandlers getTestButton() {
        return testButton;
    }

    @Override
    public void setTestResultImage(String errorMessage) {
        testResultImage.setResource(errorMessage.isEmpty() ? resources.logNormalImage() : resources.logErrorImage());
        testResultImage.setStylePrimaryName(style.testResultImage());
        testResultMessage.setText(errorMessage.isEmpty() ? constants.testSuccessMessage() : errorMessage);
    }

    @Override
    public void setAgentTabVisibility(boolean visible) {
        agentConfigurationTab.setVisible(visible);
    }

    @Override
    public void setCurrentActiveProviderWidget() {
        if (providerModel != null) {
            if (providerModel.getDataCenter().getIsAvailable()) {
                typeEditorRow.removeStyleName(style.headerSeparator());
                datacenterEditorRow.addStyleName(style.headerSeparator());
            } else {
                typeEditorRow.addStyleName(style.headerSeparator());
                datacenterEditorRow.removeStyleName(style.headerSeparator());
            }
            kvmPropertiesWidget.setVisible(providerModel.getKvmPropertiesModel().getIsAvailable());
            vmwarePropertiesWidget.setVisible(providerModel.getVmwarePropertiesModel().getIsAvailable());
            xenPropertiesWidget.setVisible(providerModel.getXenPropertiesModel().getIsAvailable());
        }
    }

    @Override
    public int setTabIndexes(int nextTabIndex) {
        nameEditor.setTabIndex(nextTabIndex++);
        descriptionEditor.setTabIndex(nextTabIndex++);
        typeEditor.setTabIndex(nextTabIndex++);
        datacenterEditor.setTabIndex(nextTabIndex++);
        pluginTypeEditor.setTabIndex(nextTabIndex++);
        urlEditor.setTabIndex(nextTabIndex++);
        readOnlyEditor.setTabIndex(nextTabIndex++);
        nextTabIndex = vmwarePropertiesWidget.setTabIndexes(nextTabIndex);
        kvmPropertiesWidget.setTabIndexes(nextTabIndex++);
        xenPropertiesWidget.setTabIndexes(nextTabIndex++);
        requiresAuthenticationEditor.setTabIndex(nextTabIndex++);
        usernameEditor.setTabIndex(nextTabIndex++);
        passwordEditor.setTabIndex(nextTabIndex++);
        tenantNameEditor.setTabIndex(nextTabIndex++);
        authUrlEditor.setTabIndex(nextTabIndex++);
        testButton.setTabIndex(nextTabIndex++);
        return nextTabIndex;
    }
}

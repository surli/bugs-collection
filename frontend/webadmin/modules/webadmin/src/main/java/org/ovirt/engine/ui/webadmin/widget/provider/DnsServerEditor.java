package org.ovirt.engine.ui.webadmin.widget.provider;

import org.ovirt.engine.ui.common.editor.UiCommonEditorDriver;
import org.ovirt.engine.ui.common.widget.editor.generic.StringEntityModelTextBoxEditor;
import org.ovirt.engine.ui.common.widget.uicommon.popup.AbstractModelBoundPopupWidget;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.event.logical.shared.HasValueChangeHandlers;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.HasEnabled;

public class DnsServerEditor extends AbstractModelBoundPopupWidget<EntityModel<String>>
    implements HasValueChangeHandlers<EntityModel<String>>, HasEnabled {

    public interface Driver extends UiCommonEditorDriver<EntityModel<String>, DnsServerEditor> {
    }


    private final Driver driver = GWT.create(Driver.class);

    @Path("entity")
    protected StringEntityModelTextBoxEditor stringEditor;

    public DnsServerEditor() {
        stringEditor = new StringEntityModelTextBoxEditor();
        initWidget(stringEditor);
        driver.initialize(this);
    }

    @Override
    public void edit(final EntityModel<String> model) {
        driver.edit(model);
        stringEditor.fireValueChangeOnKeyDown();
        stringEditor.asValueBox().addValueChangeHandler(event -> ValueChangeEvent.fire(DnsServerEditor.this, model));
    }

    @Override
    public EntityModel<String> flush() {
        return driver.flush();
    }

    @Override
    public void cleanup() {
        driver.cleanup();
    }

    @Override
    public HandlerRegistration addValueChangeHandler(ValueChangeHandler<EntityModel<String>> handler) {
        return addHandler(handler, ValueChangeEvent.getType());
    }

    public void setUsePatternFly(boolean use) {
        stringEditor.setUsePatternFly(use);
    }

    public void hideLabel() {
        stringEditor.hideLabel();
    }

    @Override
    public boolean isEnabled() {
        return stringEditor.isEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        stringEditor.setEnabled(enabled);
    }

    @Override
    public int setTabIndexes(int nextTabIndex) {
        nextTabIndex = stringEditor.setTabIndexes(nextTabIndex);
        return nextTabIndex;
    }

}

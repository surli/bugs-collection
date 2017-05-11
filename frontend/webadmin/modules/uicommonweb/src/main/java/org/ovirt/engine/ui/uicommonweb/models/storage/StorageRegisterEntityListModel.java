package org.ovirt.engine.ui.uicommonweb.models.storage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.ovirt.engine.core.common.businessentities.IVdcQueryable;
import org.ovirt.engine.core.common.businessentities.StorageDomain;
import org.ovirt.engine.core.common.businessentities.StorageDomainSharedStatus;
import org.ovirt.engine.core.common.businessentities.comparators.LexoNumericComparator;
import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.ui.frontend.Frontend;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.uicommonweb.models.SearchableListModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.ImportEntityData;

/**
 * @param <T> business entity type
 * @param <D> an <code>ImportEntityData</code> that wraps the business entity
 */
public abstract class StorageRegisterEntityListModel<T extends IVdcQueryable, D extends ImportEntityData<T>>
        extends SearchableListModel<StorageDomain, T> {

    private UICommand importCommand;

    public UICommand getImportCommand() {
        return importCommand;
    }

    private void setImportCommand(UICommand value) {
        importCommand = value;
    }

    public StorageRegisterEntityListModel() {
        setIsTimerDisabled(true);

        setImportCommand(new UICommand("Import", this)); //$NON-NLS-1$

        updateActionAvailability();
    }

    abstract RegisterEntityModel<T, D> createRegisterEntityModel();

    abstract D createImportEntityData(T entity);

    @Override
    protected void onEntityChanged() {
        super.onEntityChanged();

        getSearchCommand().execute();

        if (getEntity() != null) {
            updateActionAvailability();
        }
    }

    @Override
    protected void selectedItemsChanged() {
        super.selectedItemsChanged();
        updateActionAvailability();
    }

    private void updateActionAvailability() {
        boolean isItemSelected = getSelectedItems() != null && getSelectedItems() != null
                && getSelectedItems().size() > 0;

        boolean isDomainActive =
                getEntity() != null && getEntity().getStorageDomainSharedStatus() == StorageDomainSharedStatus.Active;

        getImportCommand().setIsExecutionAllowed(isItemSelected && isDomainActive);
    }

    @Override
    public void search() {
        if (getEntity() != null) {
            super.search();
        }
    }

    protected void syncSearch(VdcQueryType vdcQueryType, final Comparator<? super T> comparator) {
        if (getEntity() == null) {
            return;
        }

        IdQueryParameters parameters = new IdQueryParameters(getEntity().getId());
        parameters.setRefresh(getIsQueryFirstTime());

        Frontend.getInstance().runQuery(vdcQueryType, parameters, new SetSortedItemsAsyncQuery(comparator));
    }

    @Override
    public void executeCommand(UICommand command) {
        super.executeCommand(command);

        if (command == getImportCommand()) {
            restore();
        } else if (command.getName().equals("Cancel")) { //$NON-NLS-1$
            cancel();
        }
    }

    protected List<D> getImportEntities() {
        List<D> entities = new ArrayList<>();
        for (T item : getSelectedItems()) {
            entities.add(createImportEntityData(item));
        }
        Collections.sort(entities, Comparator.comparing(ImportEntityData::getName, new LexoNumericComparator()));

        return entities;
    }

    protected void restore() {
        if (getWindow() != null) {
            return;
        }

        RegisterEntityModel<T, D> model = createRegisterEntityModel();
        model.setStorageDomainId(getEntity().getId());
        setWindow(model);

        UICommand cancelCommand = UICommand.createCancelUiCommand("Cancel", this); //$NON-NLS-1$
        model.setCancelCommand(cancelCommand);

        model.getEntities().setItems(getImportEntities());
        model.initialize();
    }

    private void cancel() {
        setWindow(null);
    }
}

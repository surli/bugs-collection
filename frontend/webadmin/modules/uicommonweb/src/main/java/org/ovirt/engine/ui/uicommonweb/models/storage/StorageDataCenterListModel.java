package org.ovirt.engine.ui.uicommonweb.models.storage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.ovirt.engine.core.common.VdcActionUtils;
import org.ovirt.engine.core.common.action.AttachStorageDomainToPoolParameters;
import org.ovirt.engine.core.common.action.DetachStorageDomainFromPoolParameters;
import org.ovirt.engine.core.common.action.RemoveStorageDomainParameters;
import org.ovirt.engine.core.common.action.StorageDomainPoolParametersBase;
import org.ovirt.engine.core.common.action.VdcActionParametersBase;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.businessentities.StorageDomain;
import org.ovirt.engine.core.common.businessentities.StorageDomainSharedStatus;
import org.ovirt.engine.core.common.businessentities.StorageDomainStatic;
import org.ovirt.engine.core.common.businessentities.StorageDomainType;
import org.ovirt.engine.core.common.businessentities.StoragePool;
import org.ovirt.engine.core.common.businessentities.StoragePoolStatus;
import org.ovirt.engine.core.common.businessentities.comparators.LexoNumericComparator;
import org.ovirt.engine.core.common.businessentities.storage.StorageType;
import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.common.queries.VdcQueryReturnValue;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.ui.frontend.Frontend;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.uicommonweb.dataprovider.AsyncDataProvider;
import org.ovirt.engine.ui.uicommonweb.help.HelpTag;
import org.ovirt.engine.ui.uicommonweb.models.ConfirmationModel;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicommonweb.models.ListModel;
import org.ovirt.engine.ui.uicommonweb.models.Model;
import org.ovirt.engine.ui.uicommonweb.models.SearchableListModel;
import org.ovirt.engine.ui.uicompat.ConstantsManager;
import org.ovirt.engine.ui.uicompat.PropertyChangedEventArgs;

@SuppressWarnings("unused")
public class StorageDataCenterListModel extends SearchableListModel<StorageDomain, StorageDomain> {

    private UICommand attachCommand;

    public UICommand getAttachCommand() {
        return attachCommand;
    }

    private void setAttachCommand(UICommand value) {
        attachCommand = value;
    }

    private UICommand detachCommand;

    public UICommand getDetachCommand() {
        return detachCommand;
    }

    private void setDetachCommand(UICommand value) {
        detachCommand = value;
    }

    private UICommand activateCommand;

    public UICommand getActivateCommand() {
        return activateCommand;
    }

    private void setActivateCommand(UICommand value) {
        activateCommand = value;
    }

    private UICommand maintenanceCommand;

    public UICommand getMaintenanceCommand() {
        return maintenanceCommand;
    }

    private void setMaintenanceCommand(UICommand value) {
        maintenanceCommand = value;
    }

    /**
     * Gets the value indicating whether multiple data centers can be selected to attach storage to.
     */
    private boolean attachMultiple;

    public boolean getAttachMultiple() {
        return attachMultiple;
    }

    private void setAttachMultiple(boolean value) {
        attachMultiple = value;
    }

    private ArrayList<VdcActionParametersBase> detachPrms;

    public ArrayList<VdcActionParametersBase> getdetachPrms() {
        return detachPrms;
    }

    public void setdetachPrms(ArrayList<VdcActionParametersBase> value) {
        detachPrms = value;
    }

    private ArrayList<VdcActionParametersBase> removePrms;

    public ArrayList<VdcActionParametersBase> getremovePrms() {
        return removePrms;
    }

    public void setremovePrms(ArrayList<VdcActionParametersBase> value) {
        removePrms = value;
    }

    private ArrayList<EntityModel> attachCandidateDatacenters;

    public ArrayList<EntityModel> getattachCandidateDatacenters() {
        return attachCandidateDatacenters;
    }

    public void setattachCandidateDatacenters(ArrayList<EntityModel> value) {
        attachCandidateDatacenters = value;
    }

    private List<StoragePool> availableDataCenters;

    public List<StoragePool> getAvailableDataCenters() {
        return availableDataCenters;
    }

    public void setAvailableDataCenters(List<StoragePool> value) {
        availableDataCenters = value;
    }

    private List<StoragePool> selectedDataCentersForAttach;

    public List<StoragePool> getSelectedDataCentersForAttach() {
        return selectedDataCentersForAttach;
    }

    public void setSelectedDataCentersForAttach(List<StoragePool> selectedDataCentersForAttach) {
        this.selectedDataCentersForAttach = selectedDataCentersForAttach;
    }

    public StorageDataCenterListModel() {
        setTitle(ConstantsManager.getInstance().getConstants().dataCenterTitle());
        setHelpTag(HelpTag.data_center);
        setHashName("data_center"); //$NON-NLS-1$

        setAttachCommand(new UICommand("Attach", this)); //$NON-NLS-1$
        setDetachCommand(new UICommand("Detach", this)); //$NON-NLS-1$
        setActivateCommand(new UICommand("Activate", this)); //$NON-NLS-1$
        setMaintenanceCommand(new UICommand("Maintenance", this)); //$NON-NLS-1$

        updateActionAvailability();
    }

    @Override
    protected void onEntityChanged() {
        super.onEntityChanged();

        getSearchCommand().execute();
        updateActionAvailability();
    }

    @Override
    public void search() {
        if (getEntity() != null) {
            super.search();
        }
    }

    @Override
    protected void syncSearch() {
        if (getEntity() == null) {
            return;
        }

        super.syncSearch();

        IdQueryParameters tempVar = new IdQueryParameters(getEntity().getId());
        tempVar.setRefresh(getIsQueryFirstTime());
        Frontend.getInstance().runQuery(VdcQueryType.GetStorageDomainListById, tempVar, new AsyncQuery<VdcQueryReturnValue>(returnValue -> {
            ArrayList<StorageDomain> domains = returnValue.getReturnValue();
            for (StorageDomain domain : domains) {
                domain.setId(domain.getStoragePoolId());
            }
            Collections.sort
                    (domains, Comparator.comparing(StorageDomain::getStoragePoolName, new LexoNumericComparator()));
            setItems(domains);
            setIsEmpty(getItems().size() == 0);
        }));
    }

    private void attach() {
        if (getWindow() != null) {
            return;
        }

        setattachCandidateDatacenters(new ArrayList<EntityModel>());
        setAttachMultiple(getEntity().getStorageDomainType() == StorageDomainType.ISO);

        AsyncDataProvider.getInstance().getDataCenterList(new AsyncQuery<>(
                returnValue -> {

                    setAvailableDataCenters(returnValue);
                    boolean addDatacenter = false;
                    for (final StoragePool dataCenter : getAvailableDataCenters()) {
                        switch (getEntity().getStorageDomainType()) {
                        case Master:
                        case Data:
                            addDatacenter =
                                    (dataCenter.getStatus() == StoragePoolStatus.Uninitialized
                                            || dataCenter.getStatus() == StoragePoolStatus.Up)
                                            && (dataCenter.getStoragePoolFormatType() == null
                                                    || dataCenter.getStoragePoolFormatType().compareTo(getEntity()
                                                            .getStorageStaticData()
                                                            .getStorageFormat()) >= 0
                                                            && (dataCenter.isLocal()
                                                                    || !getEntity().getStorageType().isLocal()));
                            addToAttachCandidateDatacenters(dataCenter, addDatacenter);
                            break;
                        case Volume:
                            addDatacenter = dataCenter.getStatus() == StoragePoolStatus.Up;
                            addToAttachCandidateDatacenters(dataCenter, addDatacenter);
                            break;
                        case ISO:
                            AsyncDataProvider.getInstance().getIsoDomainByDataCenterId(new AsyncQuery<>(
                                            storageDomain -> {

                                                boolean addDatacenter12 =
                                                        dataCenter.getStatus() == StoragePoolStatus.Up
                                                                && storageDomain == null;
                                                addToAttachCandidateDatacenters(dataCenter, addDatacenter12);

                                            }),
                                    dataCenter.getId());
                            break;
                        case ImportExport:
                            AsyncDataProvider.getInstance().getExportDomainByDataCenterId(new AsyncQuery<>(
                                            storageDomain -> {

                                                boolean addDatacenter1 =
                                                        dataCenter.getStatus() == StoragePoolStatus.Up
                                                                && storageDomain == null;
                                                addToAttachCandidateDatacenters(dataCenter, addDatacenter1);

                                            }),
                                    dataCenter.getId());
                            break;
                        }
                    }

                }));
    }

    public void addToAttachCandidateDatacenters(StoragePool dataCenter, boolean addDatacenter) {
        // Add a new datacenter EntityModel
        EntityModel dcEntityModel = new EntityModel();
        if (addDatacenter) {
            dcEntityModel.setEntity(dataCenter);
        }
        getattachCandidateDatacenters().add(dcEntityModel);

        // If not finished going through the datacenters list - return
        if (getattachCandidateDatacenters().size() != getAvailableDataCenters().size()) {
            return;
        }

        // Filter datacenters list
        ArrayList<EntityModel> datacenters = new ArrayList<>();
        for (EntityModel datacenter : getattachCandidateDatacenters()) {
            if (datacenter.getEntity() != null) {
                datacenters.add(datacenter);
            }
        }

        postAttachInit(datacenters);
    }

    public void postAttachInit(ArrayList<EntityModel> datacenters) {
        ListModel<EntityModel> model = new ListModel<>();
        model.setHelpTag(HelpTag.attach_storage);
        setWindow(model);
        model.setTitle(ConstantsManager.getInstance().getConstants().attachToDataCenterTitle());
        if (getEntity() != null) {
            switch (getEntity().getStorageDomainType()) {
                case ISO:
                    setHelpTag(HelpTag.attach_iso_library);
                    break;
                case Data:
                    setHelpTag(HelpTag.attach_storage);
                    break;
                case ImportExport:
                    setHelpTag(HelpTag.attach_export_domain);
                    break;
            }
        }

        if (datacenters.isEmpty()) {
            model.setMessage(ConstantsManager.getInstance()
                    .getConstants()
                    .thereAreNoDataCenterStorageDomainAttachedMsg());

            UICommand tempVar = new UICommand("Cancel", this); //$NON-NLS-1$
            tempVar.setTitle(ConstantsManager.getInstance().getConstants().close());
            tempVar.setIsDefault(true);
            tempVar.setIsCancel(true);
            model.getCommands().add(tempVar);
        }
        else {
            model.setItems(datacenters);
            List<EntityModel> initialSelection = new ArrayList<>();
            initialSelection.add(datacenters.get(0));
            model.setSelectedItems(initialSelection);
            UICommand tempVar2 = UICommand.createDefaultOkUiCommand("OnAttach", this); //$NON-NLS-1$
            model.getCommands().add(tempVar2);
            UICommand tempVar3 = UICommand.createCancelUiCommand("Cancel", this); //$NON-NLS-1$
            model.getCommands().add(tempVar3);
        }
    }

    private void onAttach() {
        final ListModel<EntityModel<StoragePool>> model = (ListModel<EntityModel<StoragePool>>) getWindow();

        if (model.getProgress() != null) {
            return;
        }

        if (getEntity() == null) {
            cancel();
            return;
        }

        ArrayList<StoragePool> items = new ArrayList<>();
        for (EntityModel<StoragePool> a : model.getItems()) {
            if (a.getIsSelected()) {
                items.add(a.getEntity());
            }
        }

        if (items.size() == 0) {
            cancel();
            return;
        }

        setSelectedDataCentersForAttach(items);
        model.startProgress();

        if (getEntity().getStorageDomainType() == StorageDomainType.Data) {
            StoragePool dataCenter = items.get(0);
            ArrayList<StorageDomain> storageDomains = new ArrayList<>();
            storageDomains.add(getEntity());

            AsyncDataProvider.getInstance().getStorageDomainsWithAttachedStoragePoolGuid(
                new AsyncQuery<>(attachedStorageDomains -> {
                    if (!attachedStorageDomains.isEmpty()) {
                        ConfirmationModel confirmationModel = new ConfirmationModel();
                        setWindow(null);
                        setWindow(confirmationModel);

                        List<String> stoageDomainNames = new ArrayList<>();
                        for (StorageDomainStatic domain : attachedStorageDomains) {
                            stoageDomainNames.add(domain.getStorageName());
                        }
                        confirmationModel.setItems(stoageDomainNames);

                        confirmationModel.setTitle(ConstantsManager.getInstance().getConstants().storageDomainsAttachedToDataCenterWarningTitle());
                        confirmationModel.setMessage(ConstantsManager.getInstance().getConstants().storageDomainsAttachedToDataCenterWarningMessage());
                        confirmationModel.setHelpTag(HelpTag.attach_storage_domain_confirmation);
                        confirmationModel.setHashName("attach_storage_domain_confirmation"); //$NON-NLS-1$
                        confirmationModel.getLatch().setIsAvailable(true);
                        confirmationModel.getLatch().setIsChangeable(true);

                        UICommand onApprove = new UICommand("OnAttachApprove", StorageDataCenterListModel.this); //$NON-NLS-1$
                        onApprove.setTitle(ConstantsManager.getInstance().getConstants().ok());
                        onApprove.setIsDefault(true);
                        confirmationModel.getCommands().add(onApprove);

                        UICommand cancel = new UICommand("Cancel", StorageDataCenterListModel.this); //$NON-NLS-1$
                        cancel.setTitle(ConstantsManager.getInstance().getConstants().cancel());
                        cancel.setIsCancel(true);
                        confirmationModel.getCommands().add(cancel);
                    } else {
                        executeAttachStorageDomains(model);
                    }
                }), dataCenter, storageDomains);
        } else {
            executeAttachStorageDomains(model);
        }
    }

    public void onAttachApprove() {
        ConfirmationModel model = (ConfirmationModel) getWindow();
        if (!model.validate()) {
            return;
        }
        executeAttachStorageDomains(model);
    }

    public void executeAttachStorageDomains(Model model) {
        ArrayList<VdcActionParametersBase> parameters = new ArrayList<>();
        for (StoragePool dataCenter : getSelectedDataCentersForAttach()) {
            parameters.add(new AttachStorageDomainToPoolParameters(getEntity().getId(), dataCenter.getId()));
        }
        Frontend.getInstance().runMultipleAction(VdcActionType.AttachStorageDomainToPool, parameters,
                result -> {
                    ListModel localModel = (ListModel) result.getState();
                    localModel.stopProgress();
                    cancel();
                }, model);
    }

    private void detach() {
        if (getWindow() != null) {
            return;
        }

        ConfirmationModel model = new ConfirmationModel();
        setWindow(model);
        model.setTitle(ConstantsManager.getInstance().getConstants().detachStorageTitle());
        model.setHelpTag(HelpTag.detach_storage);
        model.setHashName("detach_storage"); //$NON-NLS-1$
        model.setMessage(ConstantsManager.getInstance().getConstants().areYouSureYouWantDetachStorageFromDcsMsg());

        ArrayList<String> items = new ArrayList<>();
        boolean shouldAddressWarnning = false;
        for (Object item : getSelectedItems()) {
            StorageDomain a = (StorageDomain) item;
            items.add(a.getStoragePoolName());
            if (a.getStorageDomainType().isDataDomain()) {
                shouldAddressWarnning = true;
                break;
            }
        }
        model.setItems(items);

        if (containsLocalStorage(model)) {
            model.getForce().setIsAvailable(true);
            model.getForce().setIsChangeable(true);
            model.setForceLabel(ConstantsManager.getInstance().getConstants().storageRemovePopupFormatLabel());
            shouldAddressWarnning = false;
            model.setNote(ConstantsManager.getInstance().getMessages().detachNote(getLocalStoragesFormattedString()));
        }
        if (shouldAddressWarnning) {
            model.setNote(ConstantsManager.getInstance().getConstants().detachWarnningNote());
        }

        UICommand tempVar = UICommand.createDefaultOkUiCommand("OnDetach", this); //$NON-NLS-1$
        model.getCommands().add(tempVar);
        UICommand tempVar2 = UICommand.createCancelUiCommand("Cancel", this); //$NON-NLS-1$
        model.getCommands().add(tempVar2);
    }

    private String getLocalStoragesFormattedString() {
        StringBuilder localStorages = new StringBuilder();
        for (StorageDomain a : getSelectedItems()) {
            if (a.getStorageType() == StorageType.LOCALFS) {
                localStorages.append(a.getStorageName()).append(", "); //$NON-NLS-1$
            }
        }
        return localStorages.substring(0, localStorages.length() - 2);
    }

    private boolean containsLocalStorage(ConfirmationModel model) {
        for (StorageDomain a : getSelectedItems()) {
            if (a.getStorageType() == StorageType.LOCALFS) {
                return true;
            }
        }
        return false;
    }

    private void onDetach() {
        final ConfirmationModel model = (ConfirmationModel) getWindow();

        if (!model.validate()) {
            return;
        }

        setdetachPrms(new ArrayList<VdcActionParametersBase>());
        setremovePrms(new ArrayList<VdcActionParametersBase>());

        for (Object item : getSelectedItems()) {
            StorageDomain storageDomain = (StorageDomain) item;
            if (storageDomain.getStorageType() != StorageType.LOCALFS) {
                DetachStorageDomainFromPoolParameters param = new DetachStorageDomainFromPoolParameters();
                param.setStorageDomainId(getEntity().getId());
                if (storageDomain.getStoragePoolId() != null) {
                    param.setStoragePoolId(storageDomain.getStoragePoolId());
                }

                getdetachPrms().add(param);
            }
            else {
                AsyncDataProvider.getInstance().getLocalStorageHost(new AsyncQuery<>(
                                locaVds -> {

                                    StorageDomain storage = getEntity();
                                    RemoveStorageDomainParameters tempVar =
                                            new RemoveStorageDomainParameters(storage.getId());
                                    tempVar.setVdsId(locaVds != null ? locaVds.getId() : null);
                                    tempVar.setDoFormat(model.getForce().getEntity());
                                    RemoveStorageDomainParameters removeStorageDomainParameters = tempVar;
                                    getremovePrms().add(removeStorageDomainParameters);
                                    if (getremovePrms().size() + getdetachPrms().size() == getSelectedItems()
                                            .size()) {
                                        Frontend.getInstance().runMultipleAction(VdcActionType.RemoveStorageDomain,
                                                getremovePrms());
                                    }

                                }),
                        storageDomain.getStoragePoolName());
            }

            if (getdetachPrms().size() > 0) {
                Frontend.getInstance().runMultipleAction(VdcActionType.DetachStorageDomainFromPool, getdetachPrms());
            }
        }

        cancel();
    }

    private void maintenance() {
        ConfirmationModel model = new ConfirmationModel();
        model.setTitle(ConstantsManager.getInstance().getConstants().maintenanceStorageDomainsTitle());
        model.setMessage(ConstantsManager.getInstance().getConstants().areYouSureYouWantToPlaceFollowingStorageDomainsIntoMaintenanceModeMsg());
        model.setHashName("maintenance_storage_domain"); //$NON-NLS-1$
        setWindow(model);

        ArrayList<String> items = new ArrayList<>();
        for (StorageDomain selected : getSelectedItems()) {
            items.add(selected.getName());
        }
        model.setItems(items);

        UICommand maintenance = UICommand.createDefaultOkUiCommand("OnMaintenance", this); //$NON-NLS-1$
        model.getCommands().add(maintenance);

        UICommand cancel = UICommand.createCancelUiCommand("Cancel", this); //$NON-NLS-1$
        model.getCommands().add(cancel);
    }

    private void onMaintenance() {
        ArrayList<VdcActionParametersBase> list = new ArrayList<>();
        for (StorageDomain item : getSelectedItems()) {
            StorageDomainPoolParametersBase parameters = new StorageDomainPoolParametersBase();
            parameters.setStorageDomainId(getEntity().getId());
            if (item.getStoragePoolId() != null) {
                parameters.setStoragePoolId(item.getStoragePoolId());
            }

            list.add(parameters);
        }

        final ConfirmationModel confirmationModel = (ConfirmationModel) getWindow();
        confirmationModel.startProgress();

        Frontend.getInstance().runMultipleAction(VdcActionType.DeactivateStorageDomainWithOvfUpdate, list,
                result -> {
                    confirmationModel.stopProgress();
                    setWindow(null);
                }, null);
    }

    private void activate() {
        ArrayList<VdcActionParametersBase> list = new ArrayList<>();
        for (Object item : getSelectedItems()) {
            StorageDomain a = (StorageDomain) item;

            StorageDomainPoolParametersBase parameters = new StorageDomainPoolParametersBase();
            parameters.setStorageDomainId(getEntity().getId());
            if (a.getStoragePoolId() != null) {
                parameters.setStoragePoolId(a.getStoragePoolId());
            }

            list.add(parameters);
        }

        Frontend.getInstance().runMultipleAction(VdcActionType.ActivateStorageDomain, list, result -> {}, null);
    }

    private void cancel() {
        setWindow(null);
    }

    @Override
    protected void onSelectedItemChanged() {
        super.onSelectedItemChanged();
        updateActionAvailability();
    }

    @Override
    protected void selectedItemsChanged() {
        super.selectedItemsChanged();
        updateActionAvailability();
    }

    @Override
    protected void selectedItemPropertyChanged(Object sender, PropertyChangedEventArgs e) {
        super.selectedItemPropertyChanged(sender, e);

        if (e.propertyName.equals("status")) { //$NON-NLS-1$
            updateActionAvailability();
        }
    }

    private void updateActionAvailability() {
        List<StorageDomain> items = getSelectedItems() != null ? getSelectedItems() : new ArrayList<StorageDomain>();

        getActivateCommand().setIsExecutionAllowed(items.size() == 1
                && VdcActionUtils.canExecute(items, StorageDomain.class, VdcActionType.ActivateStorageDomain));

        getMaintenanceCommand().setIsExecutionAllowed(items.size() == 1
                && VdcActionUtils.canExecute(items, StorageDomain.class, VdcActionType.DeactivateStorageDomainWithOvfUpdate));

        getAttachCommand().setIsExecutionAllowed(getEntity() != null
                && (getEntity().getStorageDomainSharedStatus() == StorageDomainSharedStatus.Unattached || getEntity().getStorageDomainType() == StorageDomainType.ISO));

        getDetachCommand().setIsExecutionAllowed(items.size() > 0
                && VdcActionUtils.canExecute(items, StorageDomain.class, VdcActionType.DetachStorageDomainFromPool));
    }

    @Override
    public void executeCommand(UICommand command) {
        super.executeCommand(command);

        if (command == getAttachCommand()) {
            attach();
        }
        else if (command == getDetachCommand()) {
            detach();
        }
        else if (command == getActivateCommand()) {
            activate();
        }
        else if (command == getMaintenanceCommand()) {
            maintenance();
        }
        else if ("OnAttach".equals(command.getName())) { //$NON-NLS-1$
            onAttach();
        }
        else if ("OnAttachApprove".equals(command.getName())) { //$NON-NLS-1$
            onAttachApprove();
        }
        else if ("OnDetach".equals(command.getName())) { //$NON-NLS-1$
            onDetach();
        }
        else if ("OnMaintenance".equals(command.getName())) { //$NON-NLS-1$
            onMaintenance();
        }
        else if ("Cancel".equals(command.getName())) { //$NON-NLS-1$
            cancel();
        }
    }

    @Override
    protected String getListName() {
        return "StorageDataCenterListModel"; //$NON-NLS-1$
    }
}

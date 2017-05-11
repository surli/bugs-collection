package org.ovirt.engine.ui.uicommonweb.models.storage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import org.ovirt.engine.core.common.action.StorageServerConnectionParametersBase;
import org.ovirt.engine.core.common.action.VdcActionParametersBase;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.action.VdcReturnValueBase;
import org.ovirt.engine.core.common.businessentities.StorageDomain;
import org.ovirt.engine.core.common.businessentities.StorageDomainType;
import org.ovirt.engine.core.common.businessentities.StorageServerConnections;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.storage.LUNs;
import org.ovirt.engine.core.common.businessentities.storage.LunStatus;
import org.ovirt.engine.core.common.businessentities.storage.StorageType;
import org.ovirt.engine.core.common.queries.DiscoverSendTargetsQueryParameters;
import org.ovirt.engine.core.common.queries.GetDeviceListQueryParameters;
import org.ovirt.engine.core.common.queries.VdcQueryReturnValue;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.ui.frontend.Frontend;
import org.ovirt.engine.ui.uicommonweb.Linq;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.uicommonweb.dataprovider.AsyncDataProvider;
import org.ovirt.engine.ui.uicommonweb.help.HelpTag;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicommonweb.models.Model;
import org.ovirt.engine.ui.uicommonweb.models.SearchableListModel;
import org.ovirt.engine.ui.uicommonweb.models.hosts.ValueEventArgs;
import org.ovirt.engine.ui.uicommonweb.validation.IValidation;
import org.ovirt.engine.ui.uicommonweb.validation.IntegerValidation;
import org.ovirt.engine.ui.uicommonweb.validation.NotEmptyValidation;
import org.ovirt.engine.ui.uicompat.ConstantsManager;
import org.ovirt.engine.ui.uicompat.Event;
import org.ovirt.engine.ui.uicompat.EventArgs;
import org.ovirt.engine.ui.uicompat.EventDefinition;
import org.ovirt.engine.ui.uicompat.IEventListener;
import org.ovirt.engine.ui.uicompat.IFrontendActionAsyncCallback;
import org.ovirt.engine.ui.uicompat.ObservableCollection;
import org.ovirt.engine.ui.uicompat.PropertyChangedEventArgs;
import org.ovirt.engine.ui.uicompat.UIConstants;
import org.ovirt.engine.ui.uicompat.UIMessages;

public abstract class SanStorageModelBase extends SearchableListModel implements IStorageModel {

    private static final UIConstants constants = ConstantsManager.getInstance().getConstants();

    private boolean isGrouppedByTarget;
    private VDS previousGetLunsByVGIdHost;

    private final List<LunModel> includedLUNs;
    private final ArrayList<SanTargetModel> lastDiscoveredTargets;
    private boolean isTargetModelList;

    private UICommand updateCommand;

    @Override
    public UICommand getUpdateCommand() {
        return updateCommand;
    }

    private void setUpdateCommand(UICommand value) {
        updateCommand = value;
    }

    private UICommand loginCommand;

    public UICommand getLoginCommand() {
        return loginCommand;
    }

    private void setLoginCommand(UICommand value) {
        loginCommand = value;
    }

    private UICommand discoverTargetsCommand;

    public UICommand getDiscoverTargetsCommand() {
        return discoverTargetsCommand;
    }

    private void setDiscoverTargetsCommand(UICommand value) {
        discoverTargetsCommand = value;
    }

    private StorageModel container;

    @Override
    public StorageModel getContainer() {
        return container;
    }

    @Override
    public void setContainer(StorageModel value) {
        container = value;
    }

    private StorageDomainType role = StorageDomainType.values()[0];

    @Override
    public StorageDomainType getRole() {
        return role;
    }

    @Override
    public void setRole(StorageDomainType value) {
        role = value;
    }

    @Override
    public abstract StorageType getType();

    private EntityModel<String> address;

    public EntityModel<String> getAddress() {
        return address;
    }

    private void setAddress(EntityModel<String> value) {
        address = value;
    }

    private EntityModel<String> port;

    public EntityModel<String> getPort() {
        return port;
    }

    private void setPort(EntityModel<String> value) {
        port = value;
    }

    private EntityModel<String> userName;

    public EntityModel<String> getUserName() {
        return userName;
    }

    private void setUserName(EntityModel<String> value) {
        userName = value;
    }

    private EntityModel<String> password;

    public EntityModel<String> getPassword() {
        return password;
    }

    private void setPassword(EntityModel<String> value) {
        password = value;
    }

    private EntityModel<Boolean> useUserAuth;

    public EntityModel<Boolean> getUseUserAuth() {
        return useUserAuth;
    }

    private void setUseUserAuth(EntityModel<Boolean> value) {
        useUserAuth = value;
    }

    private boolean proposeDiscoverTargets;

    public boolean getProposeDiscoverTargets() {
        return proposeDiscoverTargets;
    }

    public void setProposeDiscoverTargets(boolean value) {
        if (proposeDiscoverTargets != value) {
            proposeDiscoverTargets = value;
            onPropertyChanged(new PropertyChangedEventArgs("ProposeDiscoverTargets")); //$NON-NLS-1$
        }
    }

    private boolean isAllLunsSelected;

    public boolean getIsAllLunsSelected() {
        return isAllLunsSelected;
    }

    public void setIsAllLunsSelected(boolean value) {
        if (isAllLunsSelected != value) {
            isAllLunsSelected = value;
            isAllLunsSelectedChanged();
            onPropertyChanged(new PropertyChangedEventArgs("IsAllLunsSelected")); //$NON-NLS-1$
        }
    }

    private boolean ignoreGrayedOut;

    public boolean isIgnoreGrayedOut() {
        return ignoreGrayedOut;
    }

    public void setIgnoreGrayedOut(boolean value) {
        if (ignoreGrayedOut != value) {
            ignoreGrayedOut = value;
            onPropertyChanged(new PropertyChangedEventArgs("IgnoreGrayedOut")); //$NON-NLS-1$
        }
    }

    private boolean multiSelection;

    public boolean isMultiSelection() {
        return multiSelection;
    }

    public void setMultiSelection(boolean value) {
        if (multiSelection != value) {
            multiSelection = value;
            onPropertyChanged(new PropertyChangedEventArgs("MultiSelection")); //$NON-NLS-1$
        }
    }

    private String selectedLunWarning;

    public String getSelectedLunWarning() {
        return selectedLunWarning;
    }

    public void setSelectedLunWarning(String value) {
        if (!Objects.equals(selectedLunWarning, value)) {
            selectedLunWarning = value;
            onPropertyChanged(new PropertyChangedEventArgs("SelectedLunWarning")); //$NON-NLS-1$
        }
    }

    private ArrayList<SanTargetModel> targetsToConnect;

    protected SanStorageModelBase() {
        setHelpTag(HelpTag.SanStorageModelBase);
        setHashName("SanStorageModelBase"); //$NON-NLS-1$

        setUpdateCommand(new UICommand("Update", this)); //$NON-NLS-1$
        UICommand tempVar = new UICommand("Login", this); //$NON-NLS-1$
        tempVar.setIsExecutionAllowed(false);
        setLoginCommand(tempVar);
        setDiscoverTargetsCommand(new UICommand("DiscoverTargets", this)); //$NON-NLS-1$

        setAddress(new EntityModel<String>());
        EntityModel<String> tempVar2 = new EntityModel<>();
        tempVar2.setEntity("3260"); //$NON-NLS-1$
        setPort(tempVar2);
        setUserName(new EntityModel<String>());
        setPassword(new EntityModel<String>());
        EntityModel<Boolean> tempVar3 = new EntityModel<>();
        tempVar3.setEntity(false);
        setUseUserAuth(tempVar3);
        getUseUserAuth().getEntityChangedEvent().addListener(this);

        updateUserAuthFields();

        includedLUNs = new ArrayList<>();
        lastDiscoveredTargets = new ArrayList<>();

        initializeItems(null, null);
    }

    @Override
    public void eventRaised(Event ev, Object sender, EventArgs args) {
        super.eventRaised(ev, sender, args);

        if (ev.matchesDefinition(SanTargetModel.loggedInEventDefinition)) {
            sanTargetModel_LoggedIn(sender, args);
        }
        else if (ev.matchesDefinition(entityChangedEventDefinition)) {
            useUserAuth_EntityChanged(sender, args);
        }
    }

    public void updateLunWarningForDiscardAfterDelete() {
        if (getContainer().getDiscardAfterDelete().getEntity()) {
            for (LunModel lunModel : getSelectedLuns()) {
                if (!lunModel.getEntity().supportsDiscard()) {
                    setSelectedLunWarning(constants.discardIsNotSupportedByUnderlyingStorage());
                    return;
                }
            }
        }
        setSelectedLunWarning(constants.emptyString());
    }

    private void postLogin( VdcReturnValueBase returnValue, SanStorageModelBase sanStorageModel) {
        SanTargetModel sanTargetModel = sanStorageModel.targetsToConnect.remove(0);
        boolean success = returnValue != null && returnValue.getSucceeded();

        if (success) {
            sanTargetModel.setIsLoggedIn(true);
            sanTargetModel.getLoginCommand().setIsExecutionAllowed(false);
        }

        if (sanStorageModel.targetsToConnect.isEmpty()) {
            sanStorageModel.updateInternal();
        }
    }

    private void connectTargets() {

        VDS host = getContainer().getHost().getSelectedItem();
        if (host == null) {
            return;
        }

        ArrayList<VdcActionType> actionTypes = new ArrayList<>();
        ArrayList<VdcActionParametersBase> parameters = new ArrayList<>();
        ArrayList<IFrontendActionAsyncCallback> callbacks = new ArrayList<>();

        final SanStorageModelBase sanStorageModel = this;
        IFrontendActionAsyncCallback loginCallback =
                result -> sanStorageModel.postLogin(result.getReturnValue(), sanStorageModel);

        for (int i = 0; i < targetsToConnect.size(); i++) {
            SanTargetModel model = targetsToConnect.get(i);
            StorageServerConnections connection = new StorageServerConnections();
            connection.setStorageType(StorageType.ISCSI);
            connection.setUserName(getUseUserAuth().getEntity() ? getUserName().getEntity() : ""); //$NON-NLS-1$
            connection.setPassword(getUseUserAuth().getEntity() ? getPassword().getEntity() : ""); //$NON-NLS-1$
            connection.setIqn(model.getName());
            connection.setConnection(model.getAddress());
            connection.setPort(String.valueOf(model.getPort()));

            actionTypes.add(VdcActionType.ConnectStorageToVds);
            parameters.add(new StorageServerConnectionParametersBase(connection, host.getId(), false));
            callbacks.add(loginCallback);
        }

        Object target = getWidgetModel() != null ? getWidgetModel() : getContainer();
        Frontend.getInstance().runMultipleActions(actionTypes, parameters, callbacks, null, target);
    }

    private void sanTargetModel_LoggedIn(Object sender, EventArgs args) {
        SanTargetModel model = (SanTargetModel) sender;
        targetsToConnect = new ArrayList<>();
        targetsToConnect.add(model);
        connectTargets();
    }

    protected void login() {
        loginAll();
    }

    private void loginAll() {
        // Cast to list of SanTargetModel because we get call
        // to this method only from target/LUNs mode.
        List<SanTargetModel> items = (List<SanTargetModel>) getItems();
        targetsToConnect = new ArrayList<>();

        for (SanTargetModel item : items) {
            if (!item.getIsLoggedIn()) {
                targetsToConnect.add(item);
            }
        }

        connectTargets();
    }

    private void discoverTargets() {
        if (!validateDiscoverTargetFields()) {
            return;
        }

        VDS host = getContainer().getHost().getSelectedItem();

        StorageServerConnections tempVar = new StorageServerConnections();
        tempVar.setConnection(getAddress().getEntity().trim());
        tempVar.setPort(getPort().getEntity().trim());
        tempVar.setStorageType(StorageType.ISCSI);
        tempVar.setUserName(getUseUserAuth().getEntity() ? getUserName().getEntity() : ""); //$NON-NLS-1$
        tempVar.setPassword(getUseUserAuth().getEntity() ? getPassword().getEntity() : ""); //$NON-NLS-1$
        DiscoverSendTargetsQueryParameters parameters =
                new DiscoverSendTargetsQueryParameters(host.getId(), tempVar);

        setMessage(null);

        final SanStorageModelBase model = this;
        AsyncQuery<VdcQueryReturnValue> asyncQuery = new AsyncQuery<>(returnValue -> {
            Object result = returnValue.getReturnValue();
            model.postDiscoverTargetsInternal(result != null ? (ArrayList<StorageServerConnections>) result
                    : new ArrayList<StorageServerConnections>());
        }, true);
        Frontend.getInstance().runQuery(VdcQueryType.DiscoverSendTargets, parameters, asyncQuery);
    }

    protected void postDiscoverTargetsInternal(ArrayList<StorageServerConnections> items) {
        ArrayList<SanTargetModel> newItems = new ArrayList<>();

        for (StorageServerConnections a : items) {
            SanTargetModel model = new SanTargetModel();
            model.setAddress(a.getConnection());
            model.setPort(a.getPort());
            model.setName(a.getIqn());
            model.setLuns(new ObservableCollection<LunModel>());
            model.getLoggedInEvent().addListener(this);

            newItems.add(model);
        }

        if (items.isEmpty()) {
            setMessage(ConstantsManager.getInstance().getConstants().noNewDevicesWereFoundMsg());
        }

        postDiscoverTargets(newItems);
    }

    private boolean validateDiscoverTargetFields() {
        getContainer().getHost().validateSelectedItem(new NotEmptyValidation[] { new NotEmptyValidation() });

        getAddress().validateEntity(new IValidation[] { new NotEmptyValidation() });

        IntegerValidation tempVar = new IntegerValidation();
        tempVar.setMinimum(0);
        tempVar.setMaximum(65535);
        getPort().validateEntity(new IValidation[] { new NotEmptyValidation(), tempVar });

        if (getUseUserAuth().getEntity()) {
            getUserName().validateEntity(new IValidation[] { new NotEmptyValidation() });
            getPassword().validateEntity(new IValidation[] { new NotEmptyValidation() });
        }

        return getContainer().getHost().getIsValid() && getAddress().getIsValid() && getPort().getIsValid()
                && getUserName().getIsValid() && getPassword().getIsValid();
    }

    @Override
    public boolean validate() {
        boolean isValid = getAddedLuns().size() > 0 || includedLUNs.size() > 0;

        if (!isValid) {
            getInvalidityReasons().add(ConstantsManager.getInstance().getConstants().noLUNsSelectedInvalidReason());
        }

        setIsValid(isValid);

        return getIsValid();
    }

    private void useUserAuth_EntityChanged(Object sender, EventArgs args) {
        updateUserAuthFields();
    }

    private void updateUserAuthFields() {
        getUserName().setIsValid(true);
        getUserName().setIsChangeable(getUseUserAuth().getEntity());

        getPassword().setIsValid(true);
        getPassword().setIsChangeable(getUseUserAuth().getEntity());
    }

    @Override
    public void executeCommand(UICommand command) {
        super.executeCommand(command);

        if (command == getUpdateCommand()) {
            update();
        }
        else if (command == getLoginCommand()) {
            login();
        }
        else if (command == getDiscoverTargetsCommand()) {
            discoverTargets();
        }
    }

    protected void update() {
        lastDiscoveredTargets.clear();
        updateInternal();
        setIsValid(true);
    }

    protected void updateInternal() {
        if (!(getContainer().isNewStorage() || getContainer().isStorageActive())) {
            return;
        }

        VDS host = getContainer().getHost().getSelectedItem();
        if (host == null) {
            proposeDiscover();
            return;
        }

        final Collection<EntityModel<?>> prevSelected = Linq.findSelectedItems((Collection<EntityModel<?>>) getSelectedItem());
        clearItems();
        initializeItems(null, null);

        final SanStorageModelBase model = this;
        AsyncQuery<VdcQueryReturnValue> asyncQuery = new AsyncQuery<>(response -> {
            if (response.getSucceeded()) {
                model.applyData((ArrayList<LUNs>) response.getReturnValue(), false, prevSelected);
                model.setGetLUNsFailure(""); //$NON-NLS-1$
            }
            else {
                model.setGetLUNsFailure(
                        ConstantsManager.getInstance().getConstants().couldNotRetrieveLUNsLunsFailure());
            }
        }, true);
        Frontend.getInstance().runQuery(VdcQueryType.GetDeviceList,
                new GetDeviceListQueryParameters(host.getId(), getType(), false, null),
                asyncQuery);
    }

    protected void updateLoginAvailability() {
        List<SanTargetModel> items = (List<SanTargetModel>) getItems();

        // Allow login all command when there at least one target that may be logged in.
        boolean allow = false;

        for (SanTargetModel item : items) {
            if (!item.getIsLoggedIn()) {
                allow = true;
                break;
            }
        }

        getLoginCommand().setIsExecutionAllowed(allow);
    }

    public String getLoginButtonLabel() {
        return ""; //$NON-NLS-1$
    }

    /**
     * Gets or sets the value determining whether the items containing target/LUNs or LUN/targets.
     */
    public boolean getIsGrouppedByTarget() {
        return isGrouppedByTarget;
    }

    public void setIsGrouppedByTarget(boolean value) {
        if (isGrouppedByTarget != value) {
            isGrouppedByTarget = value;
            isGrouppedByTargetChanged();
            onPropertyChanged(new PropertyChangedEventArgs("IsGrouppedByTarget")); //$NON-NLS-1$
        }
    }

    private String getLUNsFailure;

    public String getGetLUNsFailure() {
        return getLUNsFailure;
    }

    public void setGetLUNsFailure(String value) {
        if (!Objects.equals(getLUNsFailure, value)) {
            getLUNsFailure = value;
            onPropertyChanged(new PropertyChangedEventArgs("GetLUNsFailure")); //$NON-NLS-1$
        }
    }

    private StorageDomain storageDomain;

    public StorageDomain getStorageDomain() {
        return storageDomain;
    }

    public void setStorageDomain(StorageDomain storageDomain) {
        this.storageDomain = storageDomain;
    }

    private boolean force;

    public boolean isForce() {
        return force;
    }

    public void setForce(boolean force) {
        this.force = force;
    }

    protected void postDiscoverTargets(ArrayList<SanTargetModel> newItems) {
        initializeItems(null, newItems);

        // Remember all discovered targets.
        lastDiscoveredTargets.clear();
        lastDiscoveredTargets.addAll(newItems);
    }

    private void clearItems() {
        if (getItems() == null) {
            return;
        }

        if (getIsGrouppedByTarget()) {
            List<SanTargetModel> items = (List<SanTargetModel>) getItems();
            items.removeIf(target -> {
                boolean found = false;

                // Ensure remove targets that are not in last discovered targets list.
                if (Linq.firstOrNull(lastDiscoveredTargets, new Linq.TargetPredicate(target)) != null) {
                    found = true;
                }
                else {
                    // Ensure remove targets that are not contain already included LUNs.
                    for (LunModel lun : target.getLuns()) {
                        LunModel foundItem = Linq.firstOrNull(includedLUNs, new Linq.LunPredicate(lun));
                        if (foundItem == null) {
                            found = true;
                            break;
                        }
                    }
                }

                return !found;
            });
        }
        else {
            List<LunModel> items = (List<LunModel>) getItems();
            items.removeIf(lun -> Linq.firstOrNull(includedLUNs, new Linq.LunPredicate(lun)) == null);
        }
    }

    /**
     * Creates model items from the provided list of business entities.
     */
    public void applyData(List<LUNs> source, boolean isIncluded, Collection<EntityModel<?>> selectedItems) {
        ArrayList<LunModel> newItems = new ArrayList<>();

        for (LUNs a : source) {
            if (a.getLunType() == getType() || a.getLunType() == StorageType.UNKNOWN) {
                ArrayList<SanTargetModel> targets = createTargetModelList(a);

                LunModel lunModel = new LunModel();
                lunModel.setLunId(a.getLUNId());
                lunModel.setVendorId(a.getVendorId());
                lunModel.setProductId(a.getProductId());
                lunModel.setSerial(a.getSerial());
                lunModel.setMultipathing(a.getPathCount());
                lunModel.setTargets(targets);
                lunModel.setSize(a.getDeviceSize());
                lunModel.setAdditionalAvailableSize(getAdditionalAvailableSize(a));
                lunModel.setAdditionalAvailableSizeSelected(false);
                lunModel.setIsAccessible(a.getAccessible());
                lunModel.setStatus(a.getStatus());
                lunModel.setIsIncluded(isIncluded);
                lunModel.setIsSelected(containsLun(lunModel, selectedItems, isIncluded));
                lunModel.setEntity(a);

                // Add LunModel
                newItems.add(lunModel);

                // Update isGrayedOut and grayedOutReason properties
                updateGrayedOut(lunModel);

                // Remember included LUNs to prevent their removal while updating items.
                if (isIncluded) {
                    includedLUNs.add(lunModel);
                }
            }
        }

        initializeItems(newItems, null);
        proposeDiscover();
    }

    private int getAdditionalAvailableSize(LUNs lun) {
        int pvSize = lun.getPvSize();
        if (pvSize == 0) {
            return 0;
        }
        // The PV size is always smaller by 1 GB from the device due to LVM metadata
        int additionalAvailableSize = lun.getDeviceSize() - pvSize - 1;
        if (additionalAvailableSize < 0) {
            additionalAvailableSize = 0;
        }
        return additionalAvailableSize;
    }

    private boolean containsLun(LunModel lunModel, Collection<EntityModel<?>> models, boolean isIncluded) {
        if (models == null) {
            return isIncluded;
        }

        for (EntityModel<?> model : models) {
            if (model instanceof LunModel) {
                if (((LunModel) model).getLunId().equals(lunModel.getLunId())) {
                    return true;
                }
            }
        }

        return false;
    }

    private ArrayList<SanTargetModel> createTargetModelList(LUNs a) {
        ArrayList<SanTargetModel> targetModelList = new ArrayList<>();
        if (a.getLunConnections() != null) {
            for (StorageServerConnections b : a.getLunConnections()) {
                SanTargetModel model = new SanTargetModel();
                model.setAddress(b.getConnection());
                model.setPort(b.getPort());
                model.setName(b.getIqn());
                model.setIsSelected(true);
                model.setIsLoggedIn(true);
                model.setLuns(new ObservableCollection<LunModel>());
                model.getLoginCommand().setIsExecutionAllowed(false);

                targetModelList.add(model);
            }
        }
        return targetModelList;
    }

    private void updateGrayedOut(LunModel lunModel) {
        UIConstants constants = ConstantsManager.getInstance().getConstants();
        UIMessages messages = ConstantsManager.getInstance().getMessages();

        LUNs lun = lunModel.getEntity();
        boolean nonEmpty = lun.getStorageDomainId() != null || lun.getDiskId() != null ||
                lun.getStatus() == LunStatus.Unusable;

        // Graying out LUNs
        lunModel.setIsGrayedOut(isIgnoreGrayedOut() ? lun.getDiskId() != null : nonEmpty);

        // Adding 'GrayedOutReasons'
        if (lun.getDiskId() != null) {
            lunModel.getGrayedOutReasons().add(
                    messages.lunUsedByDiskWarning(lun.getDiskAlias()));
        }
        else if (lun.getStorageDomainId() != null) {
            lunModel.getGrayedOutReasons().add(
                    messages.lunAlreadyPartOfStorageDomainWarning(lun.getStorageDomainName()));
        }
        else if (lun.getStatus() == LunStatus.Unusable) {
            lunModel.getGrayedOutReasons().add(
                    constants.lunUnusable());
        }
    }

    private void isGrouppedByTargetChanged() {
        initializeItems(null, null);
    }

    /**
     * Organizes items according to the current groupping flag. When new items provided takes them in account and add to
     * the Items collection.
     */
    protected void initializeItems(List<LunModel> newLuns, List<SanTargetModel> newTargets) {
        if (getIsGrouppedByTarget()) {
            if (getItems() == null) {
                setItems(new ObservableCollection<SanTargetModel>());
                isTargetModelList = true;
            }
            else {
                // Convert to list of another type as neccessary.
                if (!isTargetModelList) {
                    setItems(toTargetModelList((List<LunModel>) getItems()));
                }
            }

            ArrayList<SanTargetModel> items = new ArrayList<>();
            items.addAll((List<SanTargetModel>) getItems());

            // Add new targets.
            if (newTargets != null) {
                for (SanTargetModel newItem : newTargets) {
                    if (Linq.firstOrNull(items, new Linq.TargetPredicate(newItem)) == null) {
                        items.add(newItem);
                    }
                }
            }

            // Merge luns into targets.
            if (newLuns != null) {
                mergeLunsToTargets(newLuns, items);
            }

            setItems(items);

            updateLoginAvailability();
        }
        else {
            if (getItems() == null) {
                setItems(new ObservableCollection<LunModel>());
                isTargetModelList = false;
            }
            else {
                // Convert to list of another type as neccessary.
                if (isTargetModelList) {
                    setItems(toLunModelList((List<SanTargetModel>) getItems()));
                }
            }

            ArrayList<LunModel> items = new ArrayList<>();
            items.addAll((List<LunModel>) getItems());

            // Add new LUNs.
            if (newLuns != null) {
                for (LunModel newItem : newLuns) {
                    LunModel existingItem = Linq.firstOrNull(items, new Linq.LunPredicate(newItem));
                    if (existingItem == null) {
                        items.add(newItem);
                    }
                    else {
                        existingItem.setIsIncluded(existingItem.getIsIncluded() || newItem.getIsIncluded());
                    }
                }
            }

            setItems(items);
        }

        if (!isMultiSelection() && newLuns != null) {
            addLunModelSelectionEventListeners(newLuns);
        }
    }

    private void addLunModelSelectionEventListeners(List<LunModel> luns) {
        for (LunModel lun : luns) {
            // Adding PropertyEventListener to LunModel if needed
            if (!lun.getPropertyChangedEvent().getListeners().contains(lunModelEventListener)) {
                lun.getPropertyChangedEvent().addListener(lunModelEventListener);
            }
        }
    }

    private void mergeLunsToTargets(List<LunModel> newLuns, List<SanTargetModel> targets) {
        for (LunModel lun : newLuns) {
            for (SanTargetModel target : lun.getTargets()) {
                SanTargetModel item = Linq.firstOrNull(targets, new Linq.TargetPredicate(target));
                if (item == null) {
                    item = target;
                    targets.add(item);
                }

                LunModel currLun = Linq.firstOrNull(item.getLuns(), new Linq.LunPredicate(lun));
                if (currLun == null) {
                    item.getLuns().add(lun);
                } else {
                    currLun.setLunId(lun.getLunId());
                    currLun.setVendorId(lun.getVendorId());
                    currLun.setProductId(lun.getProductId());
                    currLun.setSerial(lun.getSerial());
                    currLun.setMultipathing(lun.getMultipathing());
                    currLun.setTargets(createTargetModelList(lun.getEntity()));
                    currLun.setSize(lun.getSize());
                    currLun.setAdditionalAvailableSize(lun.getAdditionalAvailableSize());
                    currLun.setAdditionalAvailableSizeSelected(lun.isAdditionalAvailableSizeSelected());
                    currLun.setIsAccessible(lun.getIsAccessible());
                    currLun.setStatus(lun.getStatus());
                    currLun.setIsIncluded(lun.getIsIncluded());
                    currLun.setIsSelected(lun.getIsSelected());
                    currLun.setEntity(lun.getEntity());
                }
            }
        }
    }

    private EventDefinition
            lunSelectionChangedEventDefinition = new EventDefinition("lunSelectionChanged", SanStorageModelBase.class); //$NON-NLS-1$
    private Event lunSelectionChangedEvent = new Event(lunSelectionChangedEventDefinition);

    public Event getLunSelectionChangedEvent() {
        return lunSelectionChangedEvent;
    }

    final IEventListener<PropertyChangedEventArgs> lunModelEventListener = new IEventListener<PropertyChangedEventArgs>() {
        @Override
        public void eventRaised(Event<? extends PropertyChangedEventArgs> ev, Object sender, PropertyChangedEventArgs args) {
            String propName = args.propertyName;
            if (propName.equals("IsSelected")) { //$NON-NLS-1$
                LunModel selectedLunModel = (LunModel) sender;

                if (!selectedLunModel.getIsSelected() || !getItems().iterator().hasNext()) {
                    return;
                }

                // Clear LUNs selection
                for (Model model : (List<Model>) getItems()) {
                    if (model instanceof LunModel) {
                        LunModel lunModel = (LunModel) model;
                        if (!lunModel.equals(selectedLunModel)) {
                            lunModel.setIsSelected(false);
                        }
                    }
                    else {
                        SanTargetModel sanTargetModel = (SanTargetModel) model;
                        boolean isIncludeSelected = false;

                        for (LunModel lunModel : sanTargetModel.getLuns()) {
                            if (!lunModel.equals(selectedLunModel)) {
                                lunModel.setIsSelected(false);
                            }
                            else {
                                isIncludeSelected = true;
                            }
                        }

                        if (!isIncludeSelected && sanTargetModel.getLunsList().getSelectedItem() != null) {
                            sanTargetModel.getLunsList().setSelectedItem(null);
                        }
                    }
                }
                lunSelectionChangedEvent.raise(this, new ValueEventArgs<>(selectedLunModel));
            }
        }
    };

    private List<SanTargetModel> toTargetModelList(List<LunModel> source) {
        ObservableCollection<SanTargetModel> list = new ObservableCollection<>();

        for (LunModel lun : source) {
            for (SanTargetModel target : lun.getTargets()) {
                SanTargetModel item = Linq.firstOrNull(list, new Linq.TargetPredicate(target));
                if (item == null) {
                    item = target;
                    list.add(item);
                }

                if (Linq.firstOrNull(item.getLuns(), new Linq.LunPredicate(lun)) == null) {
                    item.getLuns().add(lun);
                }
            }
        }

        // Merge with last discovered targets list.
        for (SanTargetModel target : lastDiscoveredTargets) {
            if (Linq.firstOrNull(list, new Linq.TargetPredicate(target)) == null) {
                list.add(target);
            }
        }

        isTargetModelList = true;

        return list;
    }

    private List<LunModel> toLunModelList(List<SanTargetModel> source) {
        ObservableCollection<LunModel> list = new ObservableCollection<>();

        for (SanTargetModel target : source) {
            for (LunModel lun : target.getLuns()) {
                LunModel item = Linq.firstOrNull(list, new Linq.LunPredicate(lun));
                if (item == null) {
                    item = lun;
                    list.add(item);
                }

                if (Linq.firstOrNull(item.getTargets(), new Linq.TargetPredicate(target)) == null) {
                    item.getTargets().add(target);
                }
            }
        }

        isTargetModelList = false;

        return list;
    }

    protected void proposeDiscover() {
        setProposeDiscoverTargets(getItems() == null || getItems().isEmpty());
    }

    protected void isAllLunsSelectedChanged() {
        if (!getIsGrouppedByTarget()) {
            List<LunModel> items = (List<LunModel>) getItems();
            for (LunModel lun : items) {
                if (!lun.getIsIncluded() && lun.getIsAccessible()) {
                    lun.setIsSelected(getIsAllLunsSelected());
                }
            }
        }
    }

    /**
     * @return the new selected and the preselected luns.
     */
    public ArrayList<LunModel> getSelectedLuns() {
        return getLuns(true, true);
    }

    /**
     * @return the new selected luns.
     */
    public ArrayList<LunModel> getAddedLuns() {
        return getLuns(true, false);
    }

    private ArrayList<LunModel> getLuns(boolean selectedLuns, boolean includedLuns) {
        ArrayList<LunModel> luns = new ArrayList<>();
        if (getIsGrouppedByTarget()) {
            List<SanTargetModel> items = (List<SanTargetModel>) getItems();
            for (SanTargetModel item : items) {
                luns.addAll(getAddedLuns(item.getLuns(), selectedLuns, includedLuns));
            }
        } else {
            List<LunModel> items = (List<LunModel>) getItems();
            luns.addAll(getAddedLuns(items, selectedLuns, includedLuns));
        }

        return luns;
    }

    private Collection<LunModel> getAddedLuns(List<LunModel> lunModels, boolean selectedLuns, boolean includedLuns) {
        Collection<LunModel> luns = new LinkedList<>();
        for (LunModel lun : lunModels) {
            if (((selectedLuns && lun.getIsSelected() && !lun.getIsIncluded()) ||
                    (includedLuns && lun.getIsIncluded() && !lun.getIsSelected())) &&
                    Linq.firstOrNull(luns, new Linq.LunPredicate(lun)) == null) {
                luns.add(lun);
            }
        }
        return luns;
    }

    public ArrayList<LunModel> getLunsToRefresh() {
        ArrayList<LunModel> luns = new ArrayList<>();
        if (!getIsGrouppedByTarget()) {
            List<LunModel> items = (List<LunModel>) getItems();
            for (LunModel lun : items) {
                if (lun.getIsIncluded()) {
                    if (lun.isAdditionalAvailableSizeSelected()
                            && Linq.firstOrNull(luns, new Linq.LunPredicate(lun)) == null) {
                        luns.add(lun);
                    }
                }
            }
        }
        return luns;
    }

    public ArrayList<String> getUsedLunsMessages(List<LUNs> luns) {
        ArrayList<String> usedLunsMessages = new ArrayList<>();
        UIMessages messages = ConstantsManager.getInstance().getMessages();

        for (LUNs lun : luns) {
            if (lun.getStatus() == LunStatus.Used) {
                String reason = null;

                if (lun.getVolumeGroupId() != null && !lun.getVolumeGroupId().isEmpty()) {
                    reason = messages.lunUsedByVG(lun.getVolumeGroupId());
                }

                usedLunsMessages.add(reason == null ? lun.getLUNId() :
                        messages.usedLunIdReason(lun.getLUNId(), reason));
            }
        }

        return usedLunsMessages;
    }

    public ArrayList<String> getPartOfSdLunsMessages() {
        ArrayList<String> partOfSdLunsMessages = new ArrayList<>();
        UIMessages messages = ConstantsManager.getInstance().getMessages();

        for (LunModel lunModel : getAddedLuns()) {
            LUNs lun = lunModel.getEntity();

            if (lun.getStorageDomainId() != null) {
                String reason = messages.lunAlreadyPartOfStorageDomainWarning(lun.getStorageDomainName());
                partOfSdLunsMessages.add(lunModel.getLunId() + " (" + reason + ")"); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }

        return partOfSdLunsMessages;
    }

    public boolean isEditable(StorageDomain storage) {
        return getContainer().isStorageActive() || getContainer().isNewStorage();
    }

    public void prepareForEdit(final StorageDomain storage) {
        if (isEditable(storage)) {
            final SanStorageModelBase thisModel = this;
            getContainer().getHost().getSelectedItemChangedEvent().addListener((ev, sender, args) -> postPrepareSanStorageForEdit(thisModel, true, storage));
        }
        else {
            postPrepareSanStorageForEdit(this, false, storage);
        }
    }

    private void postPrepareSanStorageForEdit(final SanStorageModelBase model, boolean isStorageActive, StorageDomain storage) {
        model.setStorageDomain(storage);

        VDS host = getContainer().getHost().getSelectedItem();
        if (Objects.equals(previousGetLunsByVGIdHost, host) && isStorageActive) {
            return;
        }
        previousGetLunsByVGIdHost = host;

        Guid hostId = host != null && isStorageActive ? host.getId() : null;

        AsyncDataProvider.getInstance().getLunsByVgId(new AsyncQuery<>(lunList -> model.applyData(lunList, true, Linq.findSelectedItems((Collection<EntityModel<?>>) getSelectedItem()))), storage.getStorage(), hostId);
    }
}

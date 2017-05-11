package org.ovirt.engine.core.bll.validator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.ovirt.engine.core.bll.validator.ValidationResultMatchers.failsWith;
import static org.ovirt.engine.core.bll.validator.ValidationResultMatchers.isValid;
import static org.ovirt.engine.core.utils.MockConfigRule.mockConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.ovirt.engine.core.bll.ValidationResult;
import org.ovirt.engine.core.bll.snapshots.SnapshotsValidator;
import org.ovirt.engine.core.bll.storage.disk.DiskHandler;
import org.ovirt.engine.core.bll.validator.storage.MultipleDiskVmElementValidator;
import org.ovirt.engine.core.common.businessentities.ArchitectureType;
import org.ovirt.engine.core.common.businessentities.BootSequence;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.VMStatus;
import org.ovirt.engine.core.common.businessentities.storage.Disk;
import org.ovirt.engine.core.common.businessentities.storage.DiskImage;
import org.ovirt.engine.core.common.config.ConfigValues;
import org.ovirt.engine.core.common.errors.EngineMessage;
import org.ovirt.engine.core.common.osinfo.OsRepository;
import org.ovirt.engine.core.common.utils.SimpleDependencyInjector;
import org.ovirt.engine.core.common.utils.customprop.VmPropertiesUtils;
import org.ovirt.engine.core.common.utils.exceptions.InitializationException;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.Version;
import org.ovirt.engine.core.dao.network.VmNicDao;
import org.ovirt.engine.core.di.InjectorRule;
import org.ovirt.engine.core.utils.MockConfigRule;

@RunWith(MockitoJUnitRunner.class)
public class RunVmValidatorTest {

    private static final int _64_BIT_OS = 13;

    public static final int MEMORY_LIMIT_32_BIT = 32000;
    public static final int MEMORY_LIMIT_64_BIT = 640000;
    @ClassRule
    public static MockConfigRule mcr = new MockConfigRule(
            mockConfig(ConfigValues.PredefinedVMProperties, Version.v3_6, "0"),
            mockConfig(ConfigValues.UserDefinedVMProperties, Version.v3_6, "0"),
            mockConfig(ConfigValues.VM32BitMaxMemorySizeInMB, "general", MEMORY_LIMIT_32_BIT),
            mockConfig(ConfigValues.VM64BitMaxMemorySizeInMB, Version.v4_0, MEMORY_LIMIT_64_BIT)
            );

    @ClassRule
    public static InjectorRule injectorRule = new InjectorRule();

    @Spy
    @InjectMocks
    private RunVmValidator runVmValidator = new RunVmValidator();
    @Mock
    private SnapshotsValidator snapshotValidator;
    @Mock
    private DiskHandler diskHandler;

    @Before
    public void setup() throws InitializationException {
        mockVmPropertiesUtils();
        mockOsRepository();
    }

    @After
    public void tearDown() {
        SimpleDependencyInjector.getInstance().bind(OsRepository.class);
    }

    @Test
    public void testValidEmptyCustomProerties() {
        VM vm = new VM();
        vm.setClusterCompatibilityVersion(Version.v4_0);
        vm.setCustomProperties("");
        List<String> messages = new ArrayList<>();
        assertTrue(runVmValidator.validateVmProperties(vm, messages));
        assertTrue(messages.isEmpty());
    }

    @Test
    public void testWrongFormatCustomProerties() {
        VM vm = new VM();
        vm.setClusterCompatibilityVersion(Version.v4_0);
        vm.setCustomProperties("sap_agent;"); // missing '= true'
        List<String> messages = new ArrayList<>();
        assertFalse(runVmValidator.validateVmProperties(vm, messages));
        assertFalse(messages.isEmpty());
    }

    @Test
    public void testNotValidCustomProerties() {
        VM vm = new VM();
        vm.setClusterCompatibilityVersion(Version.v4_0);
        vm.setCustomProperties("property=value;");
        List<String> messages = new ArrayList<>();
        assertFalse(runVmValidator.validateVmProperties(vm, messages));
        assertFalse(messages.isEmpty());
    }

    @Test
    public void testValidCustomProerties() {
        VM vm = new VM();
        vm.setClusterCompatibilityVersion(Version.v4_0);
        vm.setCustomProperties("sap_agent=true;");
        List<String> messages = new ArrayList<>();
        assertTrue(runVmValidator.validateVmProperties(vm, messages));
        assertTrue(messages.isEmpty());
    }

    @Test
    public void testVmFailNoDisks() {
        validateResult(runVmValidator.validateBootSequence(new VM(), new ArrayList<>(), null),
                       false,
                       EngineMessage.VM_CANNOT_RUN_FROM_DISK_WITHOUT_DISK);
    }

    @Test
    public void testVmWithDisks() {
        List<Disk> disks = new ArrayList<>();
        disks.add(new DiskImage());
        validateResult(runVmValidator.validateBootSequence(new VM(), disks, null),
                true,
                null);
    }

    @Test
    public void testNoIsoDomain() {
        VM vm = new VM();
        vm.setBootSequence(BootSequence.CD);
        validateResult(runVmValidator.validateBootSequence(vm, new ArrayList<>(), null),
                false,
                EngineMessage.VM_CANNOT_RUN_FROM_CD_WITHOUT_ACTIVE_STORAGE_DOMAIN_ISO);
    }

    @Test
    public void testNoDiskBootFromIsoDomain() {
        VM vm = new VM();
        vm.setBootSequence(BootSequence.CD);
        validateResult(runVmValidator.validateBootSequence(vm, new ArrayList<>(), Guid.newGuid()),
                true,
                null);
    }

    @Test
    public void testBootFromNetworkNoNetwork() {
        VmNicDao dao = mock(VmNicDao.class);
        doReturn(dao).when(runVmValidator).getVmNicDao();
        VM vm = new VM();
        vm.setBootSequence(BootSequence.N);
        validateResult(runVmValidator.validateBootSequence(vm, new ArrayList<>(), null),
                false,
                EngineMessage.VM_CANNOT_RUN_FROM_NETWORK_WITHOUT_NETWORK);
    }

    @Test
    public void canRunVmFailVmRunning() {
        final VM vm = new VM();
        vm.setStatus(VMStatus.Up);
        validateResult(runVmValidator.vmDuringInitialization(vm),
                false,
                EngineMessage.ACTION_TYPE_FAILED_VM_IS_RUNNING);
    }

    @Test
    public void canRunVmDuringInit() {
        final VM vm = new VM();
        doReturn(true).when(runVmValidator).isVmDuringInitiating(any(VM.class));
        validateResult(runVmValidator.vmDuringInitialization(vm),
                false,
                EngineMessage.ACTION_TYPE_FAILED_VM_IS_RUNNING);
    }

    @Test
    public void canRunVmNotResponding() {
        final VM vm = new VM();
        vm.setStatus(VMStatus.NotResponding);
        validateResult(runVmValidator.vmDuringInitialization(vm),
                false,
                EngineMessage.ACTION_TYPE_FAILED_VM_IS_RUNNING);
    }

    @Test
    public void testVmNotDuringInitialization() {
        final VM vm = new VM();
        vm.setStatus(VMStatus.Down);
        doReturn(false).when(runVmValidator).isVmDuringInitiating(any(VM.class));
        validateResult(runVmValidator.vmDuringInitialization(vm),
                true,
                null);
    }

    @Test
    public void passNotStatelessVM() {
        Random rand = new Random();
        canRunVmAsStateless(rand.nextBoolean(), rand.nextBoolean(), false, false, true, null);
        canRunVmAsStateless(rand.nextBoolean(), rand.nextBoolean(), false, null, true, null);
    }

    @Test
    public void failRunStatelessSnapshotInPreview() {
        Random rand = new Random();
        canRunVmAsStateless(rand.nextBoolean(),
                true,
                true,
                true,
                false,
                EngineMessage.ACTION_TYPE_FAILED_VM_IN_PREVIEW);
        canRunVmAsStateless(rand.nextBoolean(),
                true,
                true,
                null,
                false,
                EngineMessage.ACTION_TYPE_FAILED_VM_IN_PREVIEW);
        canRunVmAsStateless(rand.nextBoolean(),
                true,
                false,
                true,
                false,
                EngineMessage.ACTION_TYPE_FAILED_VM_IN_PREVIEW);
    }

    @Test
    public void failRunStatelessHaVm() {
        canRunVmAsStateless(true,
                false,
                true,
                true,
                false,
                EngineMessage.VM_CANNOT_RUN_STATELESS_HA);
        canRunVmAsStateless(true,
                false,
                true,
                null,
                false,
                EngineMessage.VM_CANNOT_RUN_STATELESS_HA);
        canRunVmAsStateless(true,
                false,
                false,
                true,
                false,
                EngineMessage.VM_CANNOT_RUN_STATELESS_HA);
    }

    private void mockOsRepository() {
        OsRepository osRepository = mock(OsRepository.class);
        when(osRepository.get64bitOss()).thenReturn(Collections.singletonList(_64_BIT_OS));
        final Map<Integer, ArchitectureType> osArchitectures =
                Collections.singletonMap(_64_BIT_OS, ArchitectureType.x86_64);
        when(osRepository.getOsArchitectures()).thenReturn(Collections.unmodifiableMap(osArchitectures));
        SimpleDependencyInjector.getInstance().bind(OsRepository.class, osRepository);
    }

    @Test
    public void test32BitMemoryExceedsLimit() {
        VM vm = new VM();
        vm.setClusterCompatibilityVersion(Version.v4_0);
        vm.setVmMemSizeMb(MEMORY_LIMIT_32_BIT + 1);
        validateResult(runVmValidator.validateMemorySize(vm), false, EngineMessage.ACTION_TYPE_FAILED_MEMORY_EXCEEDS_SUPPORTED_LIMIT);
    }

    @Test
    public void test64BitMemoryExceedsLimit() {
        VM vm = new VM();
        vm.setClusterCompatibilityVersion(Version.v4_0);
        vm.setVmMemSizeMb(MEMORY_LIMIT_64_BIT + 1);
        vm.setVmOs(_64_BIT_OS);
        validateResult(runVmValidator.validateMemorySize(vm), false, EngineMessage.ACTION_TYPE_FAILED_MEMORY_EXCEEDS_SUPPORTED_LIMIT);
    }

    @Test
    public void validateDisksPassDiscardSucceeds() {
        mockPassDiscardSupport(ValidationResult.VALID);
        VM vm = new VM();
        vm.setId(Guid.newGuid());
        assertThat(runVmValidator.validateDisksPassDiscard(vm), isValid());
    }

    @Test
    public void validateDisksPassDiscardFails() {
        EngineMessage failureEngineMessage =
                EngineMessage.ACTION_TYPE_FAILED_PASS_DISCARD_NOT_SUPPORTED_BY_DISK_INTERFACE;
        mockPassDiscardSupport(new ValidationResult(failureEngineMessage));
        VM vm = new VM();
        vm.setId(Guid.newGuid());
        assertThat(runVmValidator.validateDisksPassDiscard(vm), failsWith(failureEngineMessage));
    }

    private void mockPassDiscardSupport(ValidationResult validationResult) {
        doReturn(Collections.emptyList()).when(runVmValidator).getVmDisks();

        injectorRule.bind(DiskHandler.class, diskHandler);

        MultipleDiskVmElementValidator multipleDiskVmElementValidator = mock(MultipleDiskVmElementValidator.class);
        doReturn(multipleDiskVmElementValidator).when(runVmValidator).createMultipleDiskVmElementValidator(anyMap());
        when(multipleDiskVmElementValidator.isPassDiscardSupportedForDestSds(anyMap())).thenReturn(validationResult);
    }

    private void canRunVmAsStateless(boolean autoStartUp,
            final boolean vmInPreview,
            boolean isVmStateless,
            Boolean isStatelessParam,
            boolean shouldPass,
            EngineMessage message) {
        Guid vmId = Guid.newGuid();
        when(snapshotValidator.vmNotInPreview(vmId)).thenReturn(vmInPreview ?
                new ValidationResult(EngineMessage.ACTION_TYPE_FAILED_VM_IN_PREVIEW) : ValidationResult.VALID);
        VM vm = new VM();
        vm.setId(vmId);
        vm.setAutoStartup(autoStartUp);
        vm.setStateless(isVmStateless);
        validateResult(runVmValidator.validateStatelessVm(vm, isStatelessParam),
                shouldPass,
                message);
    }

    private VmPropertiesUtils mockVmPropertiesUtils() throws InitializationException {
        VmPropertiesUtils utils = spy(new VmPropertiesUtils());
        doReturn("sap_agent=^(true|false)$;sndbuf=^[0-9]+$;" +
                "vhost=^(([a-zA-Z0-9_]*):(true|false))(,(([a-zA-Z0-9_]*):(true|false)))*$;" +
                "viodiskcache=^(none|writeback|writethrough)$").
                when(utils)
                .getPredefinedVMProperties(any(Version.class));
        doReturn("").
                when(utils)
                .getUserdefinedVMProperties(any(Version.class));
        doReturn(new HashSet<>(Arrays.asList(Version.v3_6, Version.v4_0))).
                when(utils)
                .getSupportedClusterLevels();
        doReturn(utils).when(runVmValidator).getVmPropertiesUtils();
        utils.init();
        return utils;
    }

    private static void validateResult(ValidationResult validationResult, boolean isValid, EngineMessage message) {
        assertEquals(isValid, validationResult.isValid());
        if (!isValid) {
            assertThat(validationResult, failsWith(message));
        }
    }

}

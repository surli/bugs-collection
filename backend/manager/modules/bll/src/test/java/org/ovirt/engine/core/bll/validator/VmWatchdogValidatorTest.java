package org.ovirt.engine.core.bll.validator;

import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.ovirt.engine.core.bll.validator.ValidationResultMatchers.isValid;

import java.util.Collections;
import java.util.Set;

import org.hamcrest.Matcher;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.ovirt.engine.core.bll.ValidationResult;
import org.ovirt.engine.core.common.businessentities.VmWatchdog;
import org.ovirt.engine.core.common.businessentities.VmWatchdogType;
import org.ovirt.engine.core.common.osinfo.OsRepository;
import org.ovirt.engine.core.compat.Version;

@RunWith(MockitoJUnitRunner.class)
public class VmWatchdogValidatorTest {

    private static final Set<VmWatchdogType> WATCHDOG_MODELS = Collections.singleton(VmWatchdogType.i6300esb);

    @Test
    public void i6300esbVmWatchdogTypeWhenIsCompatibleWithOs() throws Exception {
        isModelCompatibleWithOsTest(isValid(), VmWatchdogType.i6300esb);
    }

    private void isModelCompatibleWithOsTest(Matcher<ValidationResult> matcher, VmWatchdogType watchDogModel) {
        Version version = new Version();
        VmWatchdog vmWatchdog = new VmWatchdog();
        vmWatchdog.setModel(watchDogModel);
        VmWatchdogValidator.VmWatchdogClusterDependentValidator validator = spy(new VmWatchdogValidator.VmWatchdogClusterDependentValidator(0, vmWatchdog, version));
        OsRepository osRepository = mock(OsRepository.class);

        when(validator.getOsRepository()).thenReturn(osRepository);
        when(osRepository.getVmWatchdogTypes(anyInt(), any(Version.class))).thenReturn(WATCHDOG_MODELS);

        assertThat(validator.isValid(), matcher);
    }

}

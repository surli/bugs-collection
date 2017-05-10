package org.ovirt.engine.ui.uicommonweb.validation;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.emptyCollectionOf;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner.Strict;
import org.ovirt.engine.ui.uicompat.ConstantsManager;
import org.ovirt.engine.ui.uicompat.UIConstants;

@RunWith(Strict.class)
public class IpAddressValidationTest {

    private static final String TEST_MESSAGE = "test message"; //$NON-NLS-1$
    private static final String VALID_IPV6 = "1::2"; //$NON-NLS-1$
    private static final String VALID_IPV4 = "1.2.3.4"; //$NON-NLS-1$
    private static final String EMPTY = ""; //$NON-NLS-1$
    private static final String INVALID_ADDRESS = "1.2.3.4:666"; //$NON-NLS-1$

    @Mock
    private ConstantsManager mockConstantsManager;

    @Mock
    private UIConstants mockUiConstants;

    @Before
    public void setUp() {
        when(mockConstantsManager.getConstants()).thenReturn(mockUiConstants);
        when(mockUiConstants.thisFieldMustContainIpAddressMsg()).thenReturn(TEST_MESSAGE);
    }

    @Test
    public void testValidateIpv6() {
        final IpAddressValidation underTest = new MyIpAddressValidation();

        final ValidationResult actual = underTest.validate(VALID_IPV6);

        assertTrue(actual.getSuccess());
        assertThat(actual.getReasons(), emptyCollectionOf(String.class));
    }

    @Test
    public void testValidateIpv4() {
        final IpAddressValidation underTest = new MyIpAddressValidation();

        final ValidationResult actual = underTest.validate(VALID_IPV4);

        assertTrue(actual.getSuccess());
        assertThat(actual.getReasons(), emptyCollectionOf(String.class));
    }

    @Test
    public void testValidateInvalid() {
        final IpAddressValidation underTest = new MyIpAddressValidation();

        final ValidationResult actual = underTest.validate(INVALID_ADDRESS);

        assertFalse(actual.getSuccess());
        assertThat(actual.getReasons(), contains(TEST_MESSAGE));
    }

    @Test
    public void testValidateEmpty() {
        final IpAddressValidation underTest = new MyIpAddressValidation();

        final ValidationResult actual = underTest.validate(EMPTY);

        assertFalse(actual.getSuccess());
        assertThat(actual.getReasons(), contains(TEST_MESSAGE));
    }

    @Test
    public void testValidateEmptyAllowed() {
        final IpAddressValidation underTest = new MyIpAddressValidation(true);

        final ValidationResult actual = underTest.validate(EMPTY);

        assertTrue(actual.getSuccess());
        assertThat(actual.getReasons(), emptyCollectionOf(String.class));
    }

    private class MyIpAddressValidation extends IpAddressValidation {
        private MyIpAddressValidation() {
        }

        private MyIpAddressValidation(boolean allowEmpty) {
            super(allowEmpty);
        }

        @Override
        ConstantsManager getConstantsManager() {
            return mockConstantsManager;
        }
    }
}

package org.ovirt.engine.core.common.utils;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.ovirt.engine.core.common.validation.annotation.ValidNetworkLabelFormat;

@RunWith(Parameterized.class)
public class NetworkLabelFormatValidatorTest {

    private Validator validator = ValidationUtils.getValidator();
    @Parameterized.Parameter(1)
    public boolean expectedResult;
    @Parameterized.Parameter(0)
    public Set<String> labels;

    @Test
    public void checkNetworkLabelFormat() {
        NetworkLabelContainer labelContainer = new NetworkLabelContainer(labels);
        Set<ConstraintViolation<NetworkLabelContainer>> validate = validator.validate(labelContainer);
        assertEquals("Failed to validate " + labelContainer.getLabels(), expectedResult, validate.isEmpty());
    }

    @Parameterized.Parameters
    public static Object[] ipAddressParams() {
        return new Object[][] {
                { Collections.emptySet(), true },
                { null, true },
                { Collections.singleton("abc"), true },
                { new HashSet<>(Arrays.asList("abc", "xyz")), true },
                { Collections.singleton("abc-_sc"), true },
                { Collections.singleton(""), false },
                { Collections.singleton(" "), false },
                { Collections.singleton("abc*"), false },
                { new HashSet<>(Arrays.asList("aaa", "abc*")), false }
        };
    }

    private static class NetworkLabelContainer {
        @ValidNetworkLabelFormat
        private Set<String> labels;

        public NetworkLabelContainer(Set<String> labels) {
            this.labels = labels;
        }

        public Set<String> getLabels() {
            return labels;
        }
    }
}

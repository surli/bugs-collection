package org.ovirt.engine.core.config.entity.helper;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.ovirt.engine.core.common.queries.ConfigurationValues;
import org.ovirt.engine.core.config.entity.ConfigKeyFactory;

@RunWith(Parameterized.class)
public class MtuValueHelperTest {

    private MtuValueHelper validator = new MtuValueHelper();
    @Parameterized.Parameter(0)
    public String values;
    @Parameterized.Parameter(1)
    public boolean expectedResult;

    @Test
    public void validateRanges() {
        assertEquals(expectedResult,
                validator.validate(ConfigKeyFactory.getInstance().generateBlankConfigKey(ConfigurationValues.DefaultMTU.name(), "Mtu"), values)
                        .isOk());
    }

    @Parameterized.Parameters
    public static Object[][] ipAddressParams() {
        return new Object[][] {
                { "0", true },
                { "68", true },
                { "1500", true },
                { "15000", true },
                { String.valueOf(Integer.MAX_VALUE), true },
                { "1", false },
                { "-1500", false },
                { "67", false },
                { "6aa", false },
                { "abc", false },
                { String.valueOf(Integer.MAX_VALUE) + "1", false },
                { "10-20", false },
                { "#", false },
                { null, false },
                { "", false },
                { " ", false },
        };
    }
}

package org.ovirt.engine.core.bll.storage;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;
import org.ovirt.engine.core.bll.utils.CommandsWeightsUtils;

@RunWith(MockitoJUnitRunner.class)
public class CommandsWeightsUtilsTest {
    @InjectMocks
    private CommandsWeightsUtils weightsUtils;

    @Test
    public void adjustWeightWithoutRound() {
        adjustWeights(Arrays.asList(0.2, 0.5, 0.3), Arrays.asList(2, 3, 5), 10);
    }

    @Test
    public void adjustWeightWithRound() {
        adjustWeights(Arrays.asList(0.33, 0.37, 0.3), Arrays.asList(3, 3, 4), 10);
    }

    public void adjustWeights(List<Double> weightParts, List<Integer> expectedWeightsSorted, int totalWeight) {
        Map<String, Double> map = new HashMap<>();
        IntStream.range(0, weightParts.size()).forEach(i -> map.put(String.valueOf(i), weightParts.get(i)));
        Map<String, Integer> res = weightsUtils.adjust(map, totalWeight);
        assertEquals("adjusted weights sum should be equal to the total weight",
                totalWeight,
                res.values().stream().mapToInt(x -> x).sum());

        List<Integer> values = new ArrayList<>(res.values());
        values.sort(Integer::compareTo);
        assertEquals("adjusted weights values aren't as expected", expectedWeightsSorted, values);
    }
}

package org.ovirt.engine.core.bll.scheduling.policyunits;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.ClassRule;
import org.junit.Test;
import org.ovirt.engine.core.common.businessentities.Cluster;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.utils.Pair;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.utils.MockConfigRule;

public class PreferredHostsWeightPolicyUnitTest {
    @ClassRule
    public static MockConfigRule configRule = new MockConfigRule();

    @Test
    public void testHostPreference() {
        PreferredHostsWeightPolicyUnit unit = new PreferredHostsWeightPolicyUnit(null, null);

        VDS host1 = new VDS();
        host1.setId(Guid.newGuid());

        VDS host2 = new VDS();
        host2.setId(Guid.newGuid());

        VDS host3 = new VDS();
        host3.setId(Guid.newGuid());

        Cluster cluster = new Cluster();
        cluster.setId(Guid.newGuid());

        VM vm = new VM();
        vm.setId(Guid.newGuid());
        vm.setDedicatedVmForVdsList(host2.getId());

        List<VDS> hosts = new ArrayList<>();
        hosts.add(host1);
        hosts.add(host2);
        hosts.add(host3);

        List<Pair<Guid, Integer>> weights = unit.score(cluster, hosts, vm, new HashMap<String, String>());

        Map<Guid, Integer> results = new HashMap<>();
        for (Pair<Guid, Integer> r: weights) {
            results.put(r.getFirst(), r.getSecond());
        }

        assertEquals(0, (long)results.get(host2.getId()));
        assertNotEquals(0, (long) results.get(host1.getId()));
        assertNotEquals(0, (long) results.get(host3.getId()));
    }
}

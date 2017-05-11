package org.ovirt.engine.core.bll.scheduling.policyunits;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.ovirt.engine.core.bll.scheduling.PolicyUnitImpl;
import org.ovirt.engine.core.bll.scheduling.SchedulingUnit;
import org.ovirt.engine.core.bll.scheduling.pending.PendingResourceManager;
import org.ovirt.engine.core.bll.scheduling.selector.SelectorInstance;
import org.ovirt.engine.core.common.scheduling.PolicyUnit;
import org.ovirt.engine.core.common.scheduling.PolicyUnitType;
import org.ovirt.engine.core.common.utils.Pair;
import org.ovirt.engine.core.compat.Guid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SchedulingUnit(
        guid = "b280e7de-5df8-401e-b004-a1414d79a687",
        name = "RankSelector",
        type = PolicyUnitType.SELECTOR,
        description = "The host with lowest accumulated rank wins."
)
public class RankSelectorPolicyUnit extends PolicyUnitImpl {
    public static final Logger log = LoggerFactory.getLogger(RankSelectorPolicyUnit.class);

    public RankSelectorPolicyUnit(PolicyUnit policyUnit, PendingResourceManager pendingResourceManager) {
        super(policyUnit, pendingResourceManager);
    }

    @Override
    public SelectorInstance selector(Map<String, String> parameters) {
        return new Selector();
    }

    public static class Selector implements SelectorInstance {
        // { PolicyUnit: [{ Host: weight }] }
        final Map<Guid, List<Pair<Guid, Integer>>> weightTable = new HashMap<>();
        // { PolicyUnit: factor }
        final Map<Guid, Integer> factorTable = new HashMap<>();
        List<Guid> hosts;

        @Override
        public void init(List<Pair<Guid, Integer>> policyUnits, List<Guid> hosts) {
            for (Pair<Guid, Integer> pair: policyUnits) {
                factorTable.put(pair.getFirst(), pair.getSecond());
            }

            this.hosts = Collections.unmodifiableList(hosts);
        }

        @Override
        public void record(Guid policyUnit, Guid host, Integer weight) {
            weightTable.putIfAbsent(policyUnit, new ArrayList<Pair<Guid, Integer>>());
            weightTable.get(policyUnit).add(new Pair<>(host, weight));
        }

        @Override
        public Optional<Guid> best() {
            Map<Guid, Integer> scores = new HashMap<>();

            StringBuffer debug = new StringBuffer();

            if (log.isDebugEnabled()) {
                // DEBUG header - columns are policy unit id, factor, host weight, host rank, ....
                debug.append("*;factor");
                hosts.forEach(h -> debug.append(String.format(";%s;", h.toString())));
                debug.append("\n");
            }

            for (Map.Entry<Guid, List<Pair<Guid, Integer>>> unit: weightTable.entrySet()) {
                // Retrieve the factor for this policy unit's results
                Integer factor = factorTable.getOrDefault(unit.getKey(), 1);

                // Prepare a copy of weights for local purposes
                Map<Guid, Integer> weights = new HashMap<>();
                for (Pair<Guid, Integer> record: unit.getValue()) {
                    weights.put(record.getFirst(), record.getSecond());
                }

                // Make sure all hosts are present in the list
                Set<Guid> visitedHosts = unit.getValue().stream()
                        .map(Pair::getFirst)
                        .collect(Collectors.toSet());

                // Add default weight for all hosts that were not part
                // of the result
                for (Guid host: hosts) {
                    if (!visitedHosts.contains(host)) {
                        weights.put(host, 0);
                    }
                }

                // Initialize rank for the best possible host
                // Single element arrays to avoid final limitation of lambdas
                final int[] currentRank = { weights.size() };
                final int[] realRank = { weights.size() };
                final int[] lastWeight = { Integer.MIN_VALUE };
                final Integer finalFactor = factor;

                Map<Guid, Integer> scoreDebugMap = new HashMap<>();

                // Sort according to the weight, lower weight (better) first
                // Assign rank, same weight has to have the same rank number
                // Rank = the number of hosts with the same or worse weight
                weights.entrySet().stream()
                        .sorted(Comparator.comparingInt(Entry::getValue))
                        .forEach(entry -> {
                            realRank[0]--;

                            if (entry.getValue() > lastWeight[0]) {
                                currentRank[0] = realRank[0];
                                lastWeight[0] = entry.getValue();
                            }

                            scores.putIfAbsent(entry.getKey(), 0);
                            scoreDebugMap.put(entry.getKey(), currentRank[0]);
                            scores.put(entry.getKey(),
                                    scores.get(entry.getKey()) + finalFactor * currentRank[0]);
                        });

                if (log.isDebugEnabled()) {
                    debug.append(Optional.ofNullable(unit.getKey()).map(Guid::toString).orElse("<unknown>"));
                    debug.append(";");
                    debug.append(factorTable.getOrDefault(unit.getKey(), 1));
                    hosts.forEach(h -> debug.append(String.format(";%d;%d", scoreDebugMap.get(h), weights.get(h))));
                    debug.append("\n");
                }
            }

            // Sort the scores - higher first
            List<Map.Entry<Guid, Integer>> sortedScores = new ArrayList<>(scores.entrySet());
            sortedScores.sort((o1, o2) -> Integer.compare(o2.getValue(), o1.getValue()));

            if (log.isDebugEnabled()) {
                log.debug("Ranking selector:\n{}", debug.toString());
            }

            // Return the best host
            if (sortedScores.isEmpty()) {
                return Optional.empty();
            } else {
                return Optional.of(sortedScores.get(0).getKey());
            }
        }
    }
}

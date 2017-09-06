/*
 * Copyright 1999-2015 dangdang.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * </p>
 */

package com.dangdang.ddframe.job.lite.api.strategy.impl;

import com.dangdang.ddframe.job.lite.api.strategy.JobShardingResult;
import com.dangdang.ddframe.job.lite.api.strategy.JobShardingStrategy;
import com.dangdang.ddframe.job.lite.api.strategy.JobShardingStrategyOption;
import com.dangdang.ddframe.job.lite.api.strategy.JobShardingUnit;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public final class AverageAllocationJobShardingStrategyTest {
    
    private final JobShardingStrategy jobShardingStrategy = new AverageAllocationJobShardingStrategy();
    
    @Test
    public void shardingForZeroServer() {
        assertThat(jobShardingStrategy.sharding(Collections.<JobShardingUnit>emptyList(), getJobShardingStrategyOption(3)),
                is((Collection<JobShardingResult>) Collections.<JobShardingResult>emptyList()));
    }
    
    @Test
    public void shardingForOneServer() {
        Collection<JobShardingResult> expected = Collections.singletonList(new JobShardingResult(new JobShardingUnit("host0", "test_job_instance_id"), Arrays.asList(0, 1, 2)));
        assertThat(jobShardingStrategy.sharding(Collections.singletonList(new JobShardingUnit("host0", "test_job_instance_id")), getJobShardingStrategyOption(3)), is(expected));
    }
    
    @Test
    public void shardingForServersMoreThanShardingCount() {
        Collection<JobShardingResult> expected = Arrays.asList(
                new JobShardingResult(new JobShardingUnit("host0", "test_job_instance_id"), Collections.singletonList(0)),
                new JobShardingResult(new JobShardingUnit("host1", "test_job_instance_id"), Collections.singletonList(1)),
                new JobShardingResult(new JobShardingUnit("host2", "test_job_instance_id"), Collections.<Integer>emptyList()));
        assertThat(jobShardingStrategy.sharding(Arrays.asList(
                new JobShardingUnit("host0", "test_job_instance_id"), new JobShardingUnit("host1", "test_job_instance_id"), new JobShardingUnit("host2", "test_job_instance_id")), 
                getJobShardingStrategyOption(2)), is(expected));
    }
    
    @Test
    public void shardingForServersLessThanShardingCountAliquot() {
        Collection<JobShardingResult> expected = Arrays.asList(
                new JobShardingResult(new JobShardingUnit("host0", "test_job_instance_id"), Arrays.asList(0, 1, 2)),
                new JobShardingResult(new JobShardingUnit("host1", "test_job_instance_id"), Arrays.asList(3, 4, 5)),
                new JobShardingResult(new JobShardingUnit("host2", "test_job_instance_id"), Arrays.asList(6, 7, 8)));
        assertThat(jobShardingStrategy.sharding(Arrays.asList(
                new JobShardingUnit("host0", "test_job_instance_id"), new JobShardingUnit("host1", "test_job_instance_id"), new JobShardingUnit("host2", "test_job_instance_id")),
                getJobShardingStrategyOption(9)), is(expected));
    }
    
    @Test
    public void shardingForServersLessThanShardingCountAliquantFor8ShardingCountAnd3Servers() {
        Collection<JobShardingResult> expected = Arrays.asList(
                new JobShardingResult(new JobShardingUnit("host0", "test_job_instance_id"), Arrays.asList(0, 1, 6)),
                new JobShardingResult(new JobShardingUnit("host1", "test_job_instance_id"), Arrays.asList(2, 3, 7)),
                new JobShardingResult(new JobShardingUnit("host2", "test_job_instance_id"), Arrays.asList(4, 5)));
        assertThat(jobShardingStrategy.sharding(Arrays.asList(
                new JobShardingUnit("host0", "test_job_instance_id"), new JobShardingUnit("host1", "test_job_instance_id"), new JobShardingUnit("host2", "test_job_instance_id")), 
                getJobShardingStrategyOption(8)), is(expected));
    }
    
    @Test
    public void shardingForServersLessThanShardingCountAliquantFor10ShardingCountAnd3Servers() {
        Collection<JobShardingResult> expected = Arrays.asList(
                new JobShardingResult(new JobShardingUnit("host0", "test_job_instance_id"), Arrays.asList(0, 1, 2, 9)),
                new JobShardingResult(new JobShardingUnit("host1", "test_job_instance_id"), Arrays.asList(3, 4, 5)),
                new JobShardingResult(new JobShardingUnit("host2", "test_job_instance_id"), Arrays.asList(6, 7, 8)));
        assertThat(jobShardingStrategy.sharding(Arrays.asList(
                new JobShardingUnit("host0", "test_job_instance_id"), new JobShardingUnit("host1", "test_job_instance_id"), new JobShardingUnit("host2", "test_job_instance_id")),
        getJobShardingStrategyOption(10)), is(expected));
    }
    
    private JobShardingStrategyOption getJobShardingStrategyOption(final int shardingTotalCount) {
        return new JobShardingStrategyOption("test_job", shardingTotalCount);
    }
}

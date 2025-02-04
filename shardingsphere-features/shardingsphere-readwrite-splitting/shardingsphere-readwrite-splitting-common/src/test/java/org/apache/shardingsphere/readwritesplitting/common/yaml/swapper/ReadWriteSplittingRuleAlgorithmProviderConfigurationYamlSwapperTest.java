/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.readwritesplitting.common.yaml.swapper;

import com.google.common.collect.ImmutableMap;
import org.apache.shardingsphere.readwritesplitting.api.rule.ReadWriteSplittingDataSourceRuleConfiguration;
import org.apache.shardingsphere.readwritesplitting.common.algorithm.RandomReplicaLoadBalanceAlgorithm;
import org.apache.shardingsphere.readwritesplitting.common.algorithm.config.AlgorithmProvidedReadWriteSplittingRuleConfiguration;
import org.apache.shardingsphere.readwritesplitting.common.constant.ReadWriteSplittingOrder;
import org.apache.shardingsphere.readwritesplitting.common.yaml.config.YamlReadWriteSplittingRuleConfiguration;
import org.junit.Test;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public final class ReadWriteSplittingRuleAlgorithmProviderConfigurationYamlSwapperTest {
    
    private final ReadWriteSplittingRuleAlgorithmProviderConfigurationYamlSwapper swapper = new ReadWriteSplittingRuleAlgorithmProviderConfigurationYamlSwapper();
    
    @Test
    public void assertSwapToYamlConfiguration() {
        YamlReadWriteSplittingRuleConfiguration actual = createYamlReadWriteSplittingRuleConfiguration();
        assertNotNull(actual);
        assertNotNull(actual.getDataSources());
        assertThat(actual.getDataSources().keySet(), is(Collections.singleton("name")));
        assertThat(actual.getDataSources().get("name").getName(), is("name"));
        assertThat(actual.getDataSources().get("name").getWriteDataSourceName(), is("writeDataSourceName"));
        assertThat(actual.getDataSources().get("name").getLoadBalancerName(), is("loadBalancerName"));
        assertThat(actual.getDataSources().get("name").getReadDataSourceNames(), is(Collections.singletonList("readDataSourceName")));
        assertNotNull(actual.getLoadBalancers());
        assertThat(actual.getLoadBalancers().keySet(), is(Collections.singleton("name")));
        assertNotNull(actual.getLoadBalancers().get("name"));
        assertThat(actual.getLoadBalancers().get("name").getType(), is("RANDOM"));
    }
    
    @Test
    public void assertSwapToObject() {
        AlgorithmProvidedReadWriteSplittingRuleConfiguration actual = swapper.swapToObject(createYamlReadWriteSplittingRuleConfiguration());
        assertNotNull(actual);
        assertNotNull(actual.getDataSources());
        assertTrue(actual.getDataSources().iterator().hasNext());
        ReadWriteSplittingDataSourceRuleConfiguration ruleConfig = actual.getDataSources().iterator().next();
        assertNotNull(ruleConfig);
        assertThat(ruleConfig.getName(), is("name"));
        assertThat(ruleConfig.getWriteDataSourceName(), is("writeDataSourceName"));
        assertThat(ruleConfig.getLoadBalancerName(), is("loadBalancerName"));
        assertThat(ruleConfig.getReadDataSourceNames(), is(Collections.singletonList("readDataSourceName")));
        assertThat(actual.getLoadBalanceAlgorithms(), is(Collections.emptyMap()));
    }
    
    @Test
    public void assertGetTypeClass() {
        assertThat(swapper.getTypeClass(), equalTo(AlgorithmProvidedReadWriteSplittingRuleConfiguration.class));
    }
    
    @Test
    public void assertGetRuleTagName() {
        assertThat(swapper.getRuleTagName(), is("READWRITE_SPLITTING"));
    }
    
    @Test
    public void assertGetOrder() {
        assertThat(swapper.getOrder(), is(ReadWriteSplittingOrder.ALGORITHM_PROVIDER_ORDER));
    }
    
    private YamlReadWriteSplittingRuleConfiguration createYamlReadWriteSplittingRuleConfiguration() {
        ReadWriteSplittingDataSourceRuleConfiguration ruleConfig = new ReadWriteSplittingDataSourceRuleConfiguration("name", "", "writeDataSourceName",
                Collections.singletonList("readDataSourceName"), "loadBalancerName");
        return swapper.swapToYamlConfiguration(
                new AlgorithmProvidedReadWriteSplittingRuleConfiguration(Collections.singletonList(ruleConfig), ImmutableMap.of("name", new RandomReplicaLoadBalanceAlgorithm())));
    }
}

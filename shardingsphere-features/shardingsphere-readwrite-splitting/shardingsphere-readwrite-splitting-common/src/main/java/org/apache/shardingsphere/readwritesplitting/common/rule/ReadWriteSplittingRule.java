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

package org.apache.shardingsphere.readwritesplitting.common.rule;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.apache.shardingsphere.infra.config.algorithm.ShardingSphereAlgorithmFactory;
import org.apache.shardingsphere.infra.rule.event.RuleChangedEvent;
import org.apache.shardingsphere.infra.rule.event.impl.DataSourceNameDisabledEvent;
import org.apache.shardingsphere.infra.rule.level.FeatureRule;
import org.apache.shardingsphere.infra.rule.scope.SchemaRule;
import org.apache.shardingsphere.infra.rule.type.DataSourceContainedRule;
import org.apache.shardingsphere.infra.rule.type.StatusContainedRule;
import org.apache.shardingsphere.infra.spi.ShardingSphereServiceLoader;
import org.apache.shardingsphere.infra.spi.typed.TypedSPIRegistry;
import org.apache.shardingsphere.readwritesplitting.api.ReadWriteSplittingRuleConfiguration;
import org.apache.shardingsphere.readwritesplitting.api.rule.ReadWriteSplittingDataSourceRuleConfiguration;
import org.apache.shardingsphere.readwritesplitting.common.algorithm.config.AlgorithmProvidedReadWriteSplittingRuleConfiguration;
import org.apache.shardingsphere.readwritesplitting.spi.ReplicaLoadBalanceAlgorithm;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

/**
 * Readwrite-splitting rule.
 */
public final class ReadWriteSplittingRule implements FeatureRule, SchemaRule, DataSourceContainedRule, StatusContainedRule {
    
    static {
        ShardingSphereServiceLoader.register(ReplicaLoadBalanceAlgorithm.class);
    }
    
    private final Map<String, ReplicaLoadBalanceAlgorithm> loadBalancers = new LinkedHashMap<>();
    
    private final Map<String, ReadWriteSplittingDataSourceRule> dataSourceRules;
    
    public ReadWriteSplittingRule(final ReadWriteSplittingRuleConfiguration config) {
        Preconditions.checkArgument(!config.getDataSources().isEmpty(), "Replica query data source rules can not be empty.");
        config.getLoadBalancers().forEach((key, value) -> loadBalancers.put(key, ShardingSphereAlgorithmFactory.createAlgorithm(value, ReplicaLoadBalanceAlgorithm.class)));
        dataSourceRules = new HashMap<>(config.getDataSources().size(), 1);
        for (ReadWriteSplittingDataSourceRuleConfiguration each : config.getDataSources()) {
            // TODO check if can not find load balancer should throw exception.
            ReplicaLoadBalanceAlgorithm loadBalanceAlgorithm = Strings.isNullOrEmpty(each.getLoadBalancerName()) || !loadBalancers.containsKey(each.getLoadBalancerName())
                    ? TypedSPIRegistry.getRegisteredService(ReplicaLoadBalanceAlgorithm.class) : loadBalancers.get(each.getLoadBalancerName());
            dataSourceRules.put(each.getName(), new ReadWriteSplittingDataSourceRule(each, loadBalanceAlgorithm));
        }
    }
    
    public ReadWriteSplittingRule(final AlgorithmProvidedReadWriteSplittingRuleConfiguration config) {
        Preconditions.checkArgument(!config.getDataSources().isEmpty(), "Replica query data source rules can not be empty.");
        loadBalancers.putAll(config.getLoadBalanceAlgorithms());
        dataSourceRules = new HashMap<>(config.getDataSources().size(), 1);
        for (ReadWriteSplittingDataSourceRuleConfiguration each : config.getDataSources()) {
            // TODO check if can not find load balancer should throw exception.
            ReplicaLoadBalanceAlgorithm loadBalanceAlgorithm = Strings.isNullOrEmpty(each.getLoadBalancerName()) || !loadBalancers.containsKey(each.getLoadBalancerName())
                    ? TypedSPIRegistry.getRegisteredService(ReplicaLoadBalanceAlgorithm.class) : loadBalancers.get(each.getLoadBalancerName());
            dataSourceRules.put(each.getName(), new ReadWriteSplittingDataSourceRule(each, loadBalanceAlgorithm));
        }
    }
    
    /**
     * Get all logic data source names.
     *
     * @return all logic data source names
     */
    public Collection<String> getAllLogicDataSourceNames() {
        return dataSourceRules.keySet();
    }
    
    /**
     * Get single data source rule.
     *
     * @return replica query data source rule
     */
    public ReadWriteSplittingDataSourceRule getSingleDataSourceRule() {
        return dataSourceRules.values().iterator().next();
    }
    
    /**
     * Find data source rule.
     * 
     * @param dataSourceName data source name
     * @return replica query data source rule
     */
    public Optional<ReadWriteSplittingDataSourceRule> findDataSourceRule(final String dataSourceName) {
        return Optional.ofNullable(dataSourceRules.get(dataSourceName));
    }
    
    @Override
    public Map<String, Collection<String>> getDataSourceMapper() {
        Map<String, Collection<String>> result = new HashMap<>();
        for (Entry<String, ReadWriteSplittingDataSourceRule> entry : dataSourceRules.entrySet()) {
            result.putAll(entry.getValue().getDataSourceMapper());
        }
        return result;
    }
    
    @Override
    public void updateRuleStatus(final RuleChangedEvent event) {
        if (event instanceof DataSourceNameDisabledEvent) {
            for (Entry<String, ReadWriteSplittingDataSourceRule> entry : dataSourceRules.entrySet()) {
                entry.getValue().updateDisabledDataSourceNames(((DataSourceNameDisabledEvent) event).getDataSourceName(), ((DataSourceNameDisabledEvent) event).isDisabled());
            }
        }
    }
}

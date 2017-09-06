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

package com.dangdang.ddframe.job.lite.internal.server;

import com.dangdang.ddframe.job.lite.api.strategy.JobShardingUnit;
import com.dangdang.ddframe.job.lite.internal.storage.JobNodeStorage;
import com.dangdang.ddframe.job.reg.base.CoordinatorRegistryCenter;
import com.dangdang.ddframe.job.util.env.LocalHostService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * 作业服务器节点服务.
 * 
 * @author zhangliang
 * @author caohao
 */
public class ServerService {
    
    private final JobNodeStorage jobNodeStorage;
    
    private final ServerNode serverNode;
    
    private final LocalHostService localHostService = new LocalHostService();
    
    public ServerService(final CoordinatorRegistryCenter regCenter, final String jobName) {
        jobNodeStorage = new JobNodeStorage(regCenter, jobName);
        serverNode = new ServerNode(jobName);
    }
    
    /**
     * 每次作业启动前清理上次运行状态.
     */
    public void clearPreviousServerStatus() {
        jobNodeStorage.removeJobNodeIfExisted(serverNode.getStatusNode(localHostService.getIp()));
        jobNodeStorage.removeJobNodeIfExisted(serverNode.getShutdownNode(localHostService.getIp()));
    }
    
    /**
     * 持久化作业服务器上线相关信息.
     * 
     * @param enabled 作业是否启用
     */
    public void persistServerOnline(final boolean enabled) {
        jobNodeStorage.fillJobNode(ServerNode.getHostNameNode(localHostService.getIp()), localHostService.getHostName());
        if (enabled) {
            jobNodeStorage.removeJobNodeIfExisted(serverNode.getDisabledNode(localHostService.getIp()));
        } else {
            jobNodeStorage.fillJobNode(serverNode.getDisabledNode(localHostService.getIp()), "");
        }
        jobNodeStorage.fillEphemeralJobNode(serverNode.getStatusNode(localHostService.getIp()), ServerStatus.READY);
        jobNodeStorage.removeJobNodeIfExisted(serverNode.getShutdownNode(localHostService.getIp()));
    }
    
    /**
     * 清除立刻执行作业的标记.
     */
    public void clearJobTriggerStatus() {
        jobNodeStorage.removeJobNodeIfExisted(serverNode.getTriggerNode(localHostService.getIp()));
    }
    
    /**
     * 清除暂停作业的标记.
     */
    public void clearJobPausedStatus() {
        jobNodeStorage.removeJobNodeIfExisted(serverNode.getPausedNode(localHostService.getIp()));
    }
    
    /**
     * 判断是否是手工暂停的作业.
     * 
     * @return 是否是手工暂停的作业
     */
    public boolean isJobPausedManually() {
        return jobNodeStorage.isJobNodeExisted(serverNode.getPausedNode(localHostService.getIp()));
    }
    
    /**
     * 处理服务器关机的相关信息.
     */
    public void processServerShutdown() {
        jobNodeStorage.removeJobNodeIfExisted(serverNode.getStatusNode(localHostService.getIp()));
    }
    
    /**
     * 在开始或结束执行作业时更新服务器状态.
     * 
     * @param status 服务器状态
     */
    public void updateServerStatus(final ServerStatus status) {
        jobNodeStorage.updateJobNode(serverNode.getStatusNode(localHostService.getIp()), status);
    }
    
    /**
     * 删除服务器状态.
     */
    public void removeServerStatus() {
        jobNodeStorage.removeJobNodeIfExisted(serverNode.getStatusNode(localHostService.getIp()));
    }
    
    /**
     * 获取所有的作业服务器列表.
     * 
     * @return 所有的作业服务器列表
     */
    public List<String> getAllServers() {
        List<String> result = jobNodeStorage.getJobNodeChildrenKeys(ServerNode.ROOT);
        Collections.sort(result);
        return result;
    }
    
    /**
     * 获取可分片的单元列表.
     *
     * @return 可分片的单元列表
     */
    public List<JobShardingUnit> getAvailableShardingUnits() {
        List<String> servers = getAllServers();
        List<JobShardingUnit> result = new LinkedList<>();
        for (String each : servers) {
            List<String> instances = getAvailableInstances(each);
            for (String instanceId : instances) {
                result.add(new JobShardingUnit(each, instanceId));
            }
        }
        return result;
    }
    
    private List<String> getAvailableInstances(final String ip) {
        List<String> result = new LinkedList<>();
        List<String> instances = jobNodeStorage.getJobNodeChildrenKeys(ServerNode.ROOT + "/" + ip);
        for (String each : instances) {
            if (jobNodeStorage.isJobNodeExisted(ServerNode.getStatusNode(ip, each))
                    && !jobNodeStorage.isJobNodeExisted(ServerNode.getDisabledNode(ip, each)) && !jobNodeStorage.isJobNodeExisted(ServerNode.getShutdownNode(ip, each))) {
                result.add(each);
            }
        }
        return result;
    }
    
    /**
     * 获取可用的作业服务器列表.
     * 
     * @return 可用的作业服务器列表
     */
    public List<String> getAvailableServers() {
        List<String> servers = getAllServers();
        List<String> result = new ArrayList<>(servers.size());
        for (String each : servers) {
            if (isAvailableServer(each)) {
                result.add(each);
            }
        }
        return result;
    }
    
    /**
     * 判断作业服务器是否可用.
     * 
     * @param ip 作业服务器IP地址.
     * @return 作业服务器是否可用
     */
    public boolean isAvailableServer(final String ip) {
        List<String> instances = jobNodeStorage.getJobNodeChildrenKeys(ServerNode.ROOT + "/" + ip);
        for (String each : instances) {
            if (jobNodeStorage.isJobNodeExisted(ServerNode.getStatusNode(ip, each)) && !jobNodeStorage.isJobNodeExisted(ServerNode.getPausedNode(ip, each))
                    && !jobNodeStorage.isJobNodeExisted(ServerNode.getDisabledNode(ip, each)) && !jobNodeStorage.isJobNodeExisted(ServerNode.getShutdownNode(ip, each))) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 判断当前服务器是否是等待执行的状态.
     * 
     * @return 当前服务器是否是等待执行的状态
     */
    public boolean isLocalhostServerReady() {
        String ip = localHostService.getIp();
        return isAvailableServer(ip) && ServerStatus.READY.name().equals(jobNodeStorage.getJobNodeData(serverNode.getStatusNode(ip)));
    }
    
    /**
     * 判断当前服务器是否是启用状态.
     *
     * @return 当前服务器是否是启用状态
     */
    public boolean isLocalhostServerEnabled() {
        return !jobNodeStorage.isJobNodeExisted(serverNode.getDisabledNode(localHostService.getIp()));
    }
    
    /**
     * 判断作业服务器是否存在status节点.
     * 
     * @param ip 作业服务器IP
     * @return 作业服务器是否存在status节点
     */
    public boolean hasStatusNode(final String ip) {
        return jobNodeStorage.isJobNodeExisted(serverNode.getStatusNode(ip));
    }
}

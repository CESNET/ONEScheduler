/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.muni.fi.scheduler.fairshare.calculators;

import cz.muni.fi.scheduler.elementpools.IClusterPool;
import cz.muni.fi.scheduler.elementpools.IDatastorePool;
import cz.muni.fi.scheduler.elementpools.IHostPool;
import cz.muni.fi.scheduler.filters.hosts.HostFilter;
import cz.muni.fi.scheduler.resources.ClusterElement;
import cz.muni.fi.scheduler.resources.DatastoreElement;
import cz.muni.fi.scheduler.resources.HostElement;
import cz.muni.fi.scheduler.resources.VmElement;
import cz.muni.fi.scheduler.resources.nodes.DatastoreNode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class calculates the penalty of a Virtual Machine by comparing the
 * desired resources to the resources of every deployable host. The penalty
 * is calculated using the host that gives the smallest possible penalty.
 * 
 * @author Andras Urge
 */
public abstract class MinimumPenaltyCalculator implements IVmPenaltyCalculator {
    
    private HostFilter hostFilter;
    private IHostPool hostPool;
    private IDatastorePool dsPool;
    private IClusterPool clusterPool;
    
    protected List<HostElement> hosts;
    protected Map<Integer, Integer> clusterHostNumber;
    protected Map<Integer, Float> clusterStorage;

    public MinimumPenaltyCalculator(IHostPool hostPool, IDatastorePool dsPool, IClusterPool clusterPool, HostFilter hostFilter) {
        this.hostFilter = hostFilter;
        this.hostPool = hostPool;     
        this.dsPool = dsPool;           
        this.clusterPool = clusterPool;           
        hosts = hostPool.getHosts();  
        clusterHostNumber = getClusterHostNumber();
        clusterStorage = getClusterStorage();
    }    
    
    @Override  
    public float getPenalty(VmElement vm) {
        List<HostElement> filteredHosts = hostFilter.getFilteredHosts(hosts, vm);
        HostElement firstHost = filteredHosts.get(0);
        float minPenalty = getHostPenalty(vm, firstHost);
        for (int i=1; i<filteredHosts.size(); i++) {
            float penalty = getHostPenalty(vm, filteredHosts.get(i));
            if (penalty < minPenalty) {
                minPenalty = penalty;
            }
        } 
        return minPenalty;  
    }    
    
    private Map<Integer, Integer> getClusterHostNumber() {
        Map<Integer, Integer> hostNumber = new HashMap<>();
        for (ClusterElement cluster : clusterPool.getClusters()) {
            hostNumber.put(cluster.getId(), cluster.getHosts().size());            
        }
        return hostNumber;
    } 
    
    private Map<Integer, Float> getClusterStorage() {
        Map<Integer, Float> dsStorage = new HashMap<>();
        for (DatastoreElement ds : dsPool.getSystemDs()) {
            if (ds.isShared() && ds.isMonitored()) {  
                int dsHosts = 0;
                for (int cluster : ds.getClusters()) {
                    dsHosts += clusterHostNumber.get(cluster);
                }    
                for (int cluster : ds.getClusters()) {
                    float dsClusterShare = ((float)ds.getTotal_mb() / dsHosts) * clusterHostNumber.get(cluster);
                    if (dsStorage.containsKey(cluster)) {
                        dsStorage.put(cluster, dsStorage.get(cluster) + dsClusterShare);
                    } else {
                        dsStorage.put(cluster, dsClusterShare );
                    }                    
                }
            }
        }
        return dsStorage;
    }    
    
    protected float getHostStorageShare(HostElement host) {
        float hostLocalStorage = 0;
        for (DatastoreNode ds : host.getDatastores()) {
            hostLocalStorage += ds.getTotal_mb();
        }
        float hostClusterStorageShare = clusterStorage.get(host.getClusterId()) / clusterHostNumber.get(host.getClusterId());
        return hostLocalStorage + hostClusterStorageShare;
    }
    
    protected abstract float getHostPenalty(VmElement vm, HostElement host);
}

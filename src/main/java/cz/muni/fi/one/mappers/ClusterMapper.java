/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.muni.fi.one.mappers;

import cz.muni.fi.one.XpathLoader;
import cz.muni.fi.scheduler.resources.ClusterElement;
import org.opennebula.client.cluster.Cluster;

/**
 *
 * @author Andras Urge
 */
public class ClusterMapper {
    
    public static ClusterElement map(Cluster cluster) {
        ClusterElement result = new ClusterElement();
        cluster.info();        
        
        result.setId(XpathLoader.getInt(cluster, "/CLUSTER/ID"));         
        result.setName(cluster.xpath("/CLUSTER/NAME"));
        result.setHosts(XpathLoader.getIntList(cluster, "/CLUSTER/HOSTS/ID"));
        result.setDatastores(XpathLoader.getIntList(cluster, "/CLUSTER/DATASTORES/ID"));
        
        return result;
    }
    
    
    
}
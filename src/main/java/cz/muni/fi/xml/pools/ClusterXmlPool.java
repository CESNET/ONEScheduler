/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.muni.fi.xml.pools;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import cz.muni.fi.scheduler.elementpools.IClusterPool;
import cz.muni.fi.scheduler.resources.ClusterElement;
import cz.muni.fi.xml.mappers.ClusterXmlMapper;
import cz.muni.fi.xml.resources.ClusterXml;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author Gabriela Podolnikova
 */
@JacksonXmlRootElement(localName = "CLUSTERPOOL")
public class ClusterXmlPool implements IClusterPool {
    
    @JacksonXmlProperty(localName = "CLUSTER")
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<ClusterXml> clusterXmls;
    
    private List<ClusterElement> clusters;

    public ClusterXmlPool() {
        clusters = ClusterXmlMapper.map(clusterXmls);
    }    
    
    @Override
    public List<ClusterElement> getClusters() {
        return Collections.unmodifiableList(clusters);
    }

    @Override
    public ClusterElement getCluster(int id) {
        for (ClusterElement cl : clusters) {
            if (cl.getId() == id) {
                return cl;
            }
        }
        return null;
    }
    
}

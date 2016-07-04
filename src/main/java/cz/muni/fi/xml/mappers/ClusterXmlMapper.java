/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.muni.fi.xml.mappers;

import cz.muni.fi.scheduler.resources.ClusterElement;
import cz.muni.fi.xml.resources.ClusterXml;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Gabriela Podolnikova
 */
public class ClusterXmlMapper {
    
    public static List<ClusterElement> map(List<ClusterXml> clusters) {
        List<ClusterElement> result = new ArrayList<>();
        for (ClusterXml xml : clusters) {
            result.add(map(xml));
        }
        return result;
    }

    public static ClusterElement map(ClusterXml cluster) {
        ClusterElement cl = new ClusterElement();
        cl.setId(cluster.getId());
        cl.setName(cluster.getName());
        cl.setHosts(cluster.getHosts());
        cl.setDatastores(cluster.getDatastores());
        return cl;
    }
}

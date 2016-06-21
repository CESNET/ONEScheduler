/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.muni.fi.scheduler.elementpools;

import cz.muni.fi.scheduler.resources.HostElement;
import java.util.List;

/**
 *
 * @author Gabriela Podolnikova
 */
public interface IHostPool {
    
    public List<HostElement> getHosts();
    
    public List<HostElement> getActiveHosts();
    
    public List<Integer> getHostsIds();
    
    public HostElement getHost(int id);
}
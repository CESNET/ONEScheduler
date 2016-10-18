/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cz.muni.fi.scheduler.filters.hosts;

import cz.muni.fi.scheduler.resources.HostElement;
import cz.muni.fi.scheduler.resources.VmElement;
import java.util.List;

/**
 *
 * @author Gabriela Podolnikova
 */
public interface IHostFilter {

     public List<HostElement> getFilteredHosts(List<Integer> authorizedHosts, VmElement vm);
    
}

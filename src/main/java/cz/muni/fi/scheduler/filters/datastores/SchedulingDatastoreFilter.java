package cz.muni.fi.scheduler.filters.datastores;

import cz.muni.fi.scheduler.filters.datastores.strategies.IDatastoreFilterStrategy;
import cz.muni.fi.scheduler.filters.datastores.strategies.ISchedulingDatastoreFilterStrategy;
import cz.muni.fi.scheduler.core.SchedulerData;
import cz.muni.fi.scheduler.elements.DatastoreElement;
import cz.muni.fi.scheduler.elements.HostElement;
import cz.muni.fi.scheduler.elements.VmElement;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is used in order to obtain the result from all configured scheduling filters.
 * @author Gabriela Podolnikova
 */
public class SchedulingDatastoreFilter {

    /**
     * The list of filters to be used for matching the datastore for a virtual machine.
     */
    private List<IDatastoreFilterStrategy> datastoreFilters;
    
    private List<ISchedulingDatastoreFilterStrategy> schedulingDatastoreFilters;

    public SchedulingDatastoreFilter(List<IDatastoreFilterStrategy> datastoreFilters, List<ISchedulingDatastoreFilterStrategy> schedulingDatastoreFilters) {
        this.datastoreFilters = datastoreFilters;
        this.schedulingDatastoreFilters = schedulingDatastoreFilters;
    }
    
    /**
     * Filter datastores that belongs to the host that can host the vm.
     * @param datastores all system datastores in the system
     * @param host the host for matching the datastore
     * @param vm the vm to be tested
     * @param schedulerData scheduler data to get the cached data
     * @return the list of filtered datastores
     */
    public List<DatastoreElement> filterDatastores(List<DatastoreElement> datastores, HostElement host, VmElement vm, SchedulerData schedulerData) {
        List<DatastoreElement> filteredDatastores = new ArrayList<>();
        for (DatastoreElement ds: datastores) {
            boolean matched = isSuitableDatastore(host, ds, vm, schedulerData);
            if (matched) {
                filteredDatastores.add(ds);
            }
        }
        return filteredDatastores;
    }
    
    /**
     * Goes through all filters and calls the test method.
     * If the vm and host matches (given by the specified criteria in the filter), true is returned.
     * @param host the host to be tested
     * @param ds the datasotre to be tested
     * @param vm the virtual machine to be tested
     * @param schedulerData scheduler data with cached data
     * @return true if the host and vm match, false otherwise
     */
    public boolean isSuitableDatastore(HostElement host, DatastoreElement ds, VmElement vm, SchedulerData schedulerData) {
         boolean result = true;
         for (IDatastoreFilterStrategy filter: datastoreFilters) {
             result = result && filter.test(vm, ds, host);
         }
         for (ISchedulingDatastoreFilterStrategy filter: schedulingDatastoreFilters) {
             result = result && filter.test(vm, ds, host, schedulerData);
         }
         return result;
     }
}

package cz.muni.fi.scheduler.filters.datastores.strategies;

import cz.muni.fi.scheduler.core.SchedulerData;
import cz.muni.fi.scheduler.resources.DatastoreElement;
import cz.muni.fi.scheduler.resources.HostElement;
import cz.muni.fi.scheduler.resources.VmElement;
import cz.muni.fi.scheduler.resources.nodes.DatastoreNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests whether current host has enough free space(mb) in datastores to host
 * the specified vm disks. Simplified version: firstly it adds up the sizes(mb)
 * of vm's disks. Then this value is checked on cluster's datastores.
 * TODO: It can divide the sizes of disks and try if it will fit somehow on the available
 * datastores.
 *
 * @author Gabriela Podolnikova
 */
public class FilterDatastoresByStorage implements ISchedulingDatastoreFilterStrategy {

    protected final Logger LOG = LoggerFactory.getLogger(getClass());
    
    @Override
    public boolean test(VmElement vm, DatastoreElement ds, HostElement host, SchedulerData schedulerData) {
        //check if ds and host have the same clusterId
        if (!(ds.getClusters().contains(host.getClusterId()))) {
            LOG.info("Filtering DS by storage. The DS and HOST ids don't match");
            return false;
        }
        //get the reserved storage for the datastore we are checking
        
        int sizeValue = vm.getDiskSizes();
        boolean matched = false;
        if (ds.isShared()) {
            Integer reservedStorage = schedulerData.getReservedStorage(ds);
            if (!ds.isMonitored()) {
                LOG.info("Datastore is not monitored. Cannot be matched.");
                return false;
            } else if (vm.isResched()) {
                LOG.info("VM is rescheduled. We do not need to check the datastore.");
                return true;
            } else {
                //test capacity on ds directly
                LOG.info("Datastore is shared and being checked.");
                matched = testOnDs(sizeValue, reservedStorage, ds.getFree_mb());
            }
        } else {
            Integer reservedStorage = schedulerData.getReservedStorage(host, ds);
            //test ds capacity on host datastores
            LOG.info("Datastore is not shared and is checked if it is on host: " + host.getId() + " then the capacity will be checked.");
            matched = testOnDsNode(sizeValue, reservedStorage, host, ds);
        }
        return matched;
    }
    
    private boolean testOnDs(Integer sizeValue, Integer reservedStorage, Integer freeSpace) {
        Integer actualStorage = freeSpace - reservedStorage;
        LOG.info("Filtering ds by free memory: " + actualStorage + " checking " + sizeValue);
        return (actualStorage > sizeValue);
    }

    private boolean testOnDsNode(Integer sizeValue, Integer reservedStorage, HostElement host, DatastoreElement ds) {
        if (host.getDatastores()== null || host.getDatastores().isEmpty()) {
            LOG.info("Datastores on hosts are empty or null. Returning false.");
            return false;
        }
        DatastoreNode dsNode = host.getDatastoreNode(ds.getId());
        if (dsNode != null) {
            return testOnDs(sizeValue, reservedStorage, dsNode.getFree_mb());
        } else {
            LOG.info("Ds: " + ds.getId() + " is not a datastore node of the host: " + host.getId());
            return false;
        }
    }

    private boolean testOnHost(Integer sizeValue, Integer reservedStorage, Integer freeSpace) {
        Integer actualStorage = freeSpace - reservedStorage;
        return (actualStorage > sizeValue);
    }
}
package cz.muni.fi.scheduler.filters.datastores.strategies;

import cz.muni.fi.scheduler.core.SchedulerData;
import cz.muni.fi.scheduler.elements.DatastoreElement;
import cz.muni.fi.scheduler.elements.HostElement;
import cz.muni.fi.scheduler.elements.VmElement;
import cz.muni.fi.scheduler.elements.nodes.DatastoreNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests whether current host has enough free space(mb) in datastores to host
 * the specified vm disks. Simplified version: firstly it adds up the sizes(mb)
 * of vm's disks. Then this value is checked on cluster's datastores.
 * datastores.
 *
 * @author Gabriela Podolnikova
 */
public class FilterDatastoresByStorage implements ISchedulingDatastoreFilterStrategy {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * Tests whether current host has enough free space(mb)
     *
     * @param vm with the disks sizes to be checked
     * @param ds to be checked
     * @param host to be checked
     * @param schedulerData with the current reservations on resources.
     * @return
     */
    @Override
    public boolean test(VmElement vm, DatastoreElement ds, HostElement host, SchedulerData schedulerData) {
        //check if ds and host have the same clusterId
        int sizeValue = vm.getCopyToSystemDiskSize();
        boolean matched;
        if (!(ds.getClusters().contains(host.getClusterId()))) {
            log.info("The DS " + ds.getId() + " and HOST " + host.getId() + " cluster ids don't match");
            return false;
        }
        if (ds.isShared()) {
            //get the reserved storage for the datastore we are checking
            Integer reservedStorage = schedulerData.getReservedStorage(ds);
            if (!ds.isMonitored()) {
                //Datastore " + ds.getId() + " is not monitored. Cannot be matched
                return false;
            } else if (vm.isResched()) {
                //VM is rescheduled. We do not need to check the datastore
                return true;
            } else {
                //test capacity on ds directly
                matched = testOnDs(sizeValue, reservedStorage, ds.getFree_mb());
            }
        } else {
            //get the reserved storage for the datastore we are checking
            Integer reservedStorage = schedulerData.getReservedStorage(host, ds);
            //test ds capacity on host datastores
            matched = testOnDsNode(sizeValue, reservedStorage, host, ds);
        }
        return matched;
    }

    private boolean testOnDs(Integer sizeValue, Integer reservedStorage, Integer freeSpace) {
        Integer actualStorage = freeSpace - reservedStorage;
        
        return (actualStorage > sizeValue);
    }

    /**
     * Tests the capacity directly on the datastore node on Host.
     *
     * @param sizeValue the size to be checked
     * @param reservedStorage the reserved storage in the datastore
     * @param host the host with the datastore node
     * @param ds the specified datastore
     * @return true if the size fits on the specified ds.
     */
    private boolean testOnDsNode(Integer sizeValue, Integer reservedStorage, HostElement host, DatastoreElement ds) {
        if (host.getDatastores() == null || host.getDatastores().isEmpty()) {
            return false;
        }
        DatastoreNode dsNode = host.getDatastoreNode(ds.getId());
        if (dsNode != null) {
            return testOnDs(sizeValue, reservedStorage, dsNode.getFree_mb());
        } else {
            log.info("Ds: " + ds.getId() + " is not a datastore node of the host: " + host.getId());
            return false;
        }
    }
}

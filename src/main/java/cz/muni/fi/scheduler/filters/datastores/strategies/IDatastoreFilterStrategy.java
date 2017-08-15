package cz.muni.fi.scheduler.filters.datastores.strategies;

import cz.muni.fi.scheduler.elements.DatastoreElement;
import cz.muni.fi.scheduler.elements.HostElement;
import cz.muni.fi.scheduler.elements.VmElement;

/**
 * Filters are used for filtering datastores in the system.
 * It matches the given Virtual machine and datastore, whether it meets the desired criteria.
 * For each criteria we create one Filter class by implementing this interface.
 * 
 * @author Gabriela Podolnikova
 */
public interface IDatastoreFilterStrategy {

    boolean test(VmElement vm, DatastoreElement ds, HostElement host);
}

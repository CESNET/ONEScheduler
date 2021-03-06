package cz.muni.fi.scheduler.policies.datastores;

import cz.muni.fi.scheduler.core.RankPair;
import cz.muni.fi.scheduler.core.SchedulerData;
import cz.muni.fi.scheduler.elements.DatastoreElement;
import cz.muni.fi.scheduler.elements.HostElement;
import java.util.List;

/**
 * This class represents an interface that all storage policies should be impelementing.
 * 
 * @author Gabriela Podolnikova
 */
public interface IStoragePolicy {
    
    RankPair selectDatastore(List<DatastoreElement> datastores, HostElement host, SchedulerData sd);
    
    DatastoreElement getBestRankedDatastore(List<RankPair> rps);
    
}

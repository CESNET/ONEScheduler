package cz.muni.fi.result;

import cz.muni.fi.scheduler.core.Match;
import cz.muni.fi.scheduler.elements.VmElement;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.LoggerFactory;

/**
 * This class is meant to store the results after each scheduling cycle.
 * The paths to the XML files is provided.
 * 
 * The plan should be parsed and the XML files should be updated.
 * 
 * This is the place were the simulation module can be attached.
 * 
 * @author Gabriela Podolnikova
 */
public class XmlResultManager implements IResultManager {
    
    protected final org.slf4j.Logger log = LoggerFactory.getLogger(getClass());
    
    private final String hostPoolPath;
    private final String clusterPoolPath;
    private final String userPoolPath;
    private final String vmPoolPath;
    private final String datastorePoolPath;
    
    public XmlResultManager(String hostPoolPath, String clusterPoolPath, String userPoolPath, String vmPoolPath, String datastorePoolPath) {
         this.hostPoolPath = hostPoolPath;
         this.clusterPoolPath = clusterPoolPath;
         this.userPoolPath = userPoolPath;
         this.vmPoolPath = vmPoolPath;
         this.datastorePoolPath = datastorePoolPath;
    }

    @Override
    public List<VmElement> deployPlan(List<Match> plan) {
        log.warn("Simulator is not available - cannot deploy any VM for real.");
        return new ArrayList<>();
    }

    @Override
    public List<VmElement> migrate(List<Match> migrations) {
        log.warn("Simulator not available - cannot migrate any VM for real.");
        return new ArrayList<>();
    }
}

package cz.muni.fi.scheduler.filters.hosts.strategies;

import cz.muni.fi.scheduler.resources.HostElement;
import cz.muni.fi.scheduler.resources.VmElement;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Checks whether the given Host satisfies the given VM's requirements.
 * For example: if the specified host is the host that the VM requires.
 * 
 * @author Gabriela Podolnikova
 */
public class FilterHostsBySchedulingRequirements implements IHostFilterStrategy {

    protected final Logger LOG = LoggerFactory.getLogger(getClass());
    
    @Override
    public boolean test(VmElement vm, HostElement host) {
        if (vm.getSchedRequirements() == null) {
            LOG.info("Vm does not have any host requirements");
            return true;
        }
        if (vm.getSchedRequirements().equals("")) {
            LOG.info("Vm does not have any host requirements");
            return true;
        }
        String[] reqs = vm.getSchedRequirements().split("\\|");
        boolean fits = false;
        for (String req: reqs) {
            req = req.trim();
            if (req.contains("ID")) {
                Integer id = Integer.parseInt(req.substring(req.indexOf("=")+2, req.length()-1));
                if (Objects.equals(host.getId(), id)) {
                    fits = true;
                }
            }
            if (req.contains("CLUSTER")) {
                Integer id = Integer.parseInt(req.substring(req.indexOf("=")+2, req.length()-1));
                if (Objects.equals(host.getClusterId(), id)) {
                    fits = true;
                }
            }
        }
        return fits;
    }
    
}
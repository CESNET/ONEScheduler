package cz.muni.fi.scheduler.core;

import cz.muni.fi.extensions.QueueListExtension;
import cz.muni.fi.scheduler.authorization.IAuthorizationManager;
import cz.muni.fi.scheduler.elementpools.IDatastorePool;
import cz.muni.fi.scheduler.elementpools.IHostPool;
import cz.muni.fi.scheduler.elementpools.IVmPool;
import cz.muni.fi.scheduler.filters.datastores.SchedulingDatastoreFilter;
import cz.muni.fi.scheduler.filters.hosts.SchedulingHostFilter;
import cz.muni.fi.scheduler.limits.ILimitChecker;
import cz.muni.fi.scheduler.elements.DatastoreElement;
import cz.muni.fi.scheduler.elements.HostElement;
import cz.muni.fi.scheduler.elements.VmElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import cz.muni.fi.scheduler.policies.datastores.IStoragePolicy;
import cz.muni.fi.scheduler.policies.hosts.IPlacementPolicy;
import cz.muni.fi.scheduler.queues.IQueueMapper;
import cz.muni.fi.scheduler.queues.Queue;
import cz.muni.fi.scheduler.elements.nodes.DiskNode;
import cz.muni.fi.scheduler.selectors.IVmSelector;
import java.util.HashMap;
import java.util.LinkedHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The class Scheduler is the core class responsible for all events during the
 * scheduling. Needs: - all pools - authorization manager - all hosts and
 * datastore filters - policy for host and datastore sorting/selection -
 * instance of: QueueMapper, VmSelector and LimitChecker - creates an instance
 * of SchedulerData to keep track of the current situation in the system.
 *
 * @author Gabriela Podolnikova
 */
public class Scheduler {

    private IAuthorizationManager authorizationManager;

    private IHostPool hostPool;

    private IVmPool vmPool;

    private IDatastorePool dsPool;

    /**
     * SchedulerData holds data that are updated everytime a vm is placed on a
     * host. Needs to be cleared every new cycle (after the plan is executed).
     */
    private SchedulerData schedulerData;

    /**
     * The hostFilter is used for filtering hosts. The filters that are used
     * depends on configuration.
     */
    private SchedulingHostFilter hostFilter;

    /**
     * The datastoreFilter is used for filtering datastores. The filters that
     * are used depends on configuration.
     */
    private SchedulingDatastoreFilter datastoreFilter;

    /**
     * The policy to be used for sorting the hosts available for a virtual
     * machine.
     */
    private IPlacementPolicy placementPolicy;

    /**
     * The policy to be used for choosing the best ranked datastore available
     * for a virtual machine and a host.
     */
    private IStoragePolicy storagePolicy;

    /**
     * Mapping VMs into queues.
     */
    private IQueueMapper queueMapper;

    /**
     * Selects VM to be scheduled.
     */
    private IVmSelector vmSelector;

    /**
     * Checks resources limits for a user.
     */
    private ILimitChecker limitChecker;

    /**
     * Queues with waiting VMs.
     */
    private List<Queue> queues;

    private int numberOfQueues;

    private boolean preferHostFit;

    private static final Integer PENDING_STATE = 1;
    private static final Integer HOLD_STATE = 2;

    protected final Logger log = LoggerFactory.getLogger(getClass());

    public Scheduler(IAuthorizationManager authorizationManager, IHostPool hostPool,
            IVmPool vmPool, IDatastorePool dsPool, SchedulingHostFilter hostFilter,
            SchedulingDatastoreFilter datastoreFilter, IPlacementPolicy placementPolicy,
            IStoragePolicy storagePolicy, int numberOfQueues,
            boolean preferHostFit, IQueueMapper queueMapper, IVmSelector vmSelector, ILimitChecker limitChecker) {
        this.authorizationManager = authorizationManager;
        this.hostPool = hostPool;
        this.vmPool = vmPool;
        this.dsPool = dsPool;
        this.hostFilter = hostFilter;
        this.datastoreFilter = datastoreFilter;
        this.placementPolicy = placementPolicy;
        this.storagePolicy = storagePolicy;
        this.numberOfQueues = numberOfQueues;
        this.preferHostFit = preferHostFit;
        this.queueMapper = queueMapper;
        this.vmSelector = vmSelector;
        this.limitChecker = limitChecker;
        //initialize scheduler data entity
        schedulerData = new SchedulerData();
    }

    /**
     * This method is called to start the scheduling. (in SetUp class) Gets
     * pending VMs and creates queues. Continues woth processing the queues.
     *
     * @return the list with all matched VMs.
     */
    public List<Match> schedule() {
        //get pendings, state = 1 is pending
        List<VmElement> pendingVms = vmPool.getVmsByState(PENDING_STATE);
        if (pendingVms.isEmpty()) {
            log.info("No pendings");
            return null;
        }
        authorizationManager.authorize(pendingVms);
        // VM queues construction
        //System.out.println("pending vms "+pendingVms.toString());
        queues = queueMapper.mapQueues(pendingVms);
        return processQueues(queues);
    }

    /**
     * Migrate is called before schedule. - Takes only those VMs with
     * rescheduling flag. - Finds suitable hosts. - Removes from suitable hosts
     * those where the VM currently is. - Creates match.
     *
     * @return all matched VMs
     */
    public List<Match> migrate() {
        List<Match> migrations = new ArrayList<>();
        List<VmElement> vmsToBeMigrated = vmPool.getReschedVms();
        for (VmElement vm : vmsToBeMigrated) {
            List<HostElement> suitableHosts = prepareHostsForVm(vm);
            suitableHosts.remove(hostPool.getHost(Integer.valueOf(vm.getDeploy_id())));
            log.info("Finding match for migrated VM: " + vm);
            Match match = processVm(vm, suitableHosts);
            if (match != null) {
                log.info("Migrating vm: " + vm.getVmId() + " on host: " + match.getHost().getId() + " and ds: " + match.getDatastore().getId());
                migrations.add(match);
            }
        }
        return migrations;
    }

    /**
     * Iterates through all the queues. How the VM is selected is dependent on
     * the vmSelector implementation. For each VM: - checks the image datastore
     * capacity - prepare hosts (get those that are authorized and filters
     * unsuitable hosts out) - process the VM --> gets match - checks limit
     * (dependent on the limitChecker implementation) - adds match to the plan
     *
     * @param queues the queues in the system to be processed
     * @return the created plan
     */
    private List<Match> processQueues(List<Queue> queues) {
        List<Match> plan = new ArrayList<>();
        while (!QueueListExtension.queuesEmpty(queues)) {
            VmElement vmSelected = vmSelector.selectVm(queues);
            System.out.println("VM selected: " + vmSelected);
            if (!hasImageDsStorageAvailable(vmSelected)) {
                log.warn("VM: " + vmSelected.getVmId() + " has no ImageDS available");
                continue;
            }
            List<HostElement> suitableHosts = prepareHostsForVm(vmSelected);
            Match match = processVm(vmSelected, suitableHosts);
            if (match != null) {
                if (limitChecker.checkLimit(vmSelected, match)) {
                    plan = match.addVm(plan, vmSelected);
                    log.warn("Scheduling vm: " + vmSelected.getVmId() + " on host: " + match.getHost().getId() + " and ds: " + match.getDatastore().getId());
                    limitChecker.getDataInstance().increaseData(vmSelected);
                }
            } else {
                log.warn("VM: " + vmSelected.getVmId() + " no match found");
            }
        }
        return plan;
    }

    /**
     * Checks the Image datastore. The image datastore needs to be checked only
     * for those disks in VM that has the CLONE_TARGET/LN_TARGET (depends
     * whether the VM is persistent) equals to SELF. It means that the image of
     * the disk is cloned in the image datastore.
     *
     * @param vm the VM to be checked
     * @return true if there is capacity for the image
     */
    private boolean hasImageDsStorageAvailable(VmElement vm) {
        Map<DatastoreElement, Integer> diskUsage = getDiskUsageWithCloneTargetSelf(vm);
        for (DatastoreElement ds : diskUsage.keySet()) {
            Integer freeMb = ds.getFree_mb();
            if (freeMb < (diskUsage.get(ds) + schedulerData.getReservedStorage(ds))) {
                return false;
            }
        }
        //update reservedStorage for Image DS
        for (DatastoreElement ds : diskUsage.keySet()) {
            schedulerData.reserveDatastoreStorage(ds, diskUsage.get(ds));
        }
        return true;
    }

    /**
     * Gets only disk and its capacity with cloning target SELF.
     *
     * @param vm to get the disks
     * @return the map with datastore location of the image and the capacity
     * needed by the disk.
     */
    private Map<DatastoreElement, Integer> getDiskUsageWithCloneTargetSelf(VmElement vm) {
        List<DiskNode> disks = vm.getDisksWithSelfTarget();
        Map<DatastoreElement, Integer> diskUsage = new HashMap<>();
        for (DiskNode disk : disks) {
            DatastoreElement ds = dsPool.getDatastore(disk.getDatastore_id());
            if (diskUsage.containsKey(ds)) {
                diskUsage.put(ds, diskUsage.get(ds) + disk.getSize());
            } else {
                diskUsage.put(ds, disk.getSize());
            }
        }
        return diskUsage;
    }

    /**
     * For each VM prepares suitable hosts. First, gets the authorized hosts for
     * the user. Then filters the unsuitable hosts out.
     *
     * @param vm the VM with the requirements
     * @return list of hosts that suits the requirements.
     */
    private List<HostElement> prepareHostsForVm(VmElement vm) {
        List<HostElement> authorizedHosts = authorizationManager.getAuthorizedHosts(vm.getUid());
        if (authorizedHosts.isEmpty()) {
            log.info("Empty authorized hosts.");
            return authorizedHosts;
        }
        //filter authorized hosts for vm 
        return hostFilter.getFilteredHosts(authorizedHosts, vm, schedulerData);
    }

    /**
     * This method process VM: - takes a subset of hosts that are suitable for
     * the VM - sorts the hosts by placement policy - filter and choose the best
     * ranked datastore for the VM. From the suitable hosts and datastores
     * creates candidate pairs. If candidates are not empty, then the match is
     * created.
     *
     * @param vm the VM to be processed
     * @param hosts the hosts suitable for VM
     * @return the match for the vm
     */
    private Match processVm(VmElement vm, List<HostElement> hosts) {
        Match match = null;
        if (hosts.isEmpty()) {
            log.info("No suitable hosts for VM: " + vm);
            return null;
        }
        //sort hosts
        List<HostElement> sortedHosts = placementPolicy.sortHosts(hosts, schedulerData);
        //filter and sort datastores for hosts
        LinkedHashMap<HostElement, RankPair> candidates = getCandidates(sortedHosts, vm);
        // deploy if candidates are not empty
        if (!candidates.isEmpty()) {
            //pick the host and datastore. We chose the best host, or the best datastore.
            match = createMatch(candidates);
            // update reservations
            updateCachedData(match, vm);
        }
        return match;
    }

    /**
     * Needs to be called after each created match, in order to update the
     * schedulerData: - host cpu - host memory - running vms - datastore storage
     *
     * @param match the match with the host, datastore
     * @param vm virtual machine's data to store
     */
    private void updateCachedData(Match match, VmElement vm) {
        schedulerData.reserveHostCpuCapacity(match.getHost(), vm);
        schedulerData.reserveHostMemoryCapacity(match.getHost(), vm);
        schedulerData.reserveHostRunningVm(match.getHost());
        if (match.getDatastore().isShared()) {
            schedulerData.reserveDatastoreStorage(match.getDatastore(), vm.getCopyToSystemDiskSize());
        } else {
            schedulerData.reserveDatastoreNodeStorage(match.getHost(), match.getDatastore(), vm.getCopyToSystemDiskSize());
        }
    }

    /**
     * This method creates a map of candidates. It goes through the sorted hosts
     * and for each host it filters its datastore to suit the VM requirements.
     * The map is build like this: The hosts are keys in this map and are sorted
     * by the placement policy. To each host he RankPair is assigned. - the
     * RankPair contains the suitable datastore and a number that represent its
     * rank. The rank is calculated based upon the storage policy. LinkedHashMap
     * preserves order in which the objects are entered.
     *
     * @param sortedHosts list of hosts sorted by the placement policy
     * @param vm the current virtual machine in the queue
     * @return the map containing the hosts and datastores (candidates).
     */
    private LinkedHashMap<HostElement, RankPair> getCandidates(List<HostElement> sortedHosts, VmElement vm) {
        LinkedHashMap<HostElement, RankPair> candidates = new LinkedHashMap<>();
        if (sortedHosts.size() == 0) {
            log.info("Sorted hosts empty for VM: " + vm);
        } else {
            log.info(sortedHosts.size() + " sorted hosts found for VM: " + vm);
        }
        for (HostElement host : sortedHosts) {
            List<DatastoreElement> filteredDatastores = datastoreFilter.filterDatastores(authorizationManager.getAuthorizedDs(vm.getUid()), host, vm, schedulerData);
            
            if (!filteredDatastores.isEmpty()) {
                RankPair ds = storagePolicy.selectDatastore(filteredDatastores, host, schedulerData);
                candidates.put(host, ds);
            }
        }
        return candidates;
    }

    /**
     * Create Match from sorted candidates Map. preferHostFit == true --> we
     * choose the best host from map (from keys) -- first key = best ranked host
     * preferHostFit == false --> we choose the best datastore from map (from
     * values) -- pick the best ranked datastore
     *
     * @param sortedCandidates contains hosts with datastores
     * @return chosen match for vm
     */
    private Match createMatch(LinkedHashMap<HostElement, RankPair> sortedCandidates) {
        HostElement chosenHost;
        DatastoreElement chosenDs;
        if (preferHostFit) {
            Map.Entry<HostElement, RankPair> entry = sortedCandidates.entrySet().iterator().next();
            chosenHost = entry.getKey();
            chosenDs = entry.getValue().getDs();
        } else {
            chosenDs = storagePolicy.getBestRankedDatastore(new ArrayList(sortedCandidates.values()));
            chosenHost = getFirstHostThatHasDs(sortedCandidates, chosenDs);
        }
        log.info("Created match host: " + chosenHost.getId() + " and datastore " + chosenDs.getId());
        return new Match(chosenHost, chosenDs);
    }

    /**
     * Finds the key (desired host) to corresponding value (known/chosen
     * datastore).
     *
     * @param candidates the map with sorted candidates
     * @param chosenDs the value
     * @return the key that corresponds to the value
     */
    private HostElement getFirstHostThatHasDs(Map<HostElement, RankPair> candidates, DatastoreElement chosenDs) {
        HostElement result = null;
        for (Map.Entry<HostElement, RankPair> entry : candidates.entrySet()) {
            if (entry.getValue().getDs().equals(chosenDs)) {
                result = entry.getKey();
            }
        }
        return result;
    }
}

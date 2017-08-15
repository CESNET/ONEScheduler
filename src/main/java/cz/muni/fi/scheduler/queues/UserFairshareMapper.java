package cz.muni.fi.scheduler.queues;

import cz.muni.fi.extensions.MapExtension;
import cz.muni.fi.extensions.VmListExtension;
import cz.muni.fi.scheduler.fairshare.UserPriorityCalculator;
import cz.muni.fi.scheduler.elements.VmElement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Class that maps virtual machines into queues based 
 * on the fairshare priority of their users.
 * 
 * @author Andras Urge
 */
public class UserFairshareMapper implements IQueueMapper {
    
    private UserPriorityCalculator calculator;
    private Map<Integer, Float> percentages;
    
    public UserFairshareMapper(UserPriorityCalculator calculator, Map<Integer, Float> percentages) {
        this.calculator = calculator;
        this.percentages = percentages;
    }

    /**
     * Creates queues for the users of the provided VMs. The queues are filled
     * with the the corresponding VMs and ordered by the fairshare priorities of
     * the users.
     * 
     * @param vms the virtual machines to be mapped
     * @return list of ordered queues
     */
    @Override
    public List<Queue> mapQueues(List<VmElement> vms) {
        //System.out.println("user priorities...");
        Map<Integer, Float> userPriorities = calculator.getUserPriorities(VmListExtension.getUserIds(vms));
        userPriorities = applyFairsharePercentages(userPriorities);
        List<Integer> sortedUsers = MapExtension.sortByValue(userPriorities);
        Map<Integer, List<VmElement>> userVms = VmListExtension.getUserVms(vms);
        
        List<Queue> queues = new ArrayList<>();
        for (Integer sortedUser : sortedUsers) {
            int userId = sortedUser;
            Queue queue = createUserQueue(userId, userPriorities.get(userId), userVms.get(userId));
            queues.add(queue);
        }
        return queues;
    }
    
    /**
     * Adjusts the user priorities by the specified  fairshare percentages.
     * 
     * @param userPriorities the user priorities
     * @return the adjusted user priorities
     */
    private Map<Integer, Float> applyFairsharePercentages(Map<Integer, Float> userPriorities) {
        for (int userId : userPriorities.keySet()) {
            float modifier = percentages.getOrDefault(userId, 100f);
            userPriorities.put(userId, userPriorities.get(userId) / modifier);
        }
        return userPriorities;
    }
    
    /**
     * Creates a Queue for a user. The provided virtual machines are ordered by 
     * their ID in the queue.
     * 
     * @param userId ID of the user
     * @param priority the fairshare priority of the user
     * @param userVms the user's VMs
     * @return the Queue for the user
     */
    private Queue createUserQueue(int userId, float priority, List<VmElement> userVms) {
        Collections.sort(userVms, (VmElement vm1, VmElement vm2) -> {
            return vm1.getVmId().compareTo(vm2.getVmId());
        });
        return new Queue("User" + userId, priority, userVms);
    }
}

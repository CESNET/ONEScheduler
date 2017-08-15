/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.muni.fi.scheduler.fairshare;

import cz.muni.fi.extensions.VmListExtension;
import cz.muni.fi.scheduler.elementpools.IVmPool;
import cz.muni.fi.scheduler.fairshare.penaltycalculators.IVmPenaltyCalculator;
import cz.muni.fi.scheduler.fairshare.historyrecords.IUserFairshareRecordManager;
import cz.muni.fi.scheduler.fairshare.historyrecords.IVmFairshareRecordManager;
import cz.muni.fi.scheduler.fairshare.historyrecords.VmFairshareRecord;
import cz.muni.fi.scheduler.elements.VmElement;
import cz.muni.fi.scheduler.elements.nodes.HistoryNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class is responsible to assign a  fairshare priority to the users based 
 * on the history of their virtual machines. The lower the value, the higher 
 * priority will the user get.
 * 
 * @author Andras Urge
 */
public class UserPriorityCalculator {
        
    private IVmPool vmPool; 
    private IVmPenaltyCalculator penaltyCalculator;
    private IUserFairshareRecordManager userRecordManager;
    private IVmFairshareRecordManager vmRecordManager;

    public UserPriorityCalculator(IVmPool vmPool, IVmPenaltyCalculator penaltyCalculator, IUserFairshareRecordManager userRecordManager, IVmFairshareRecordManager vmRecordManager) {
        this.vmPool = vmPool;
        this.penaltyCalculator = penaltyCalculator;
        this.userRecordManager = userRecordManager;
        this.vmRecordManager = vmRecordManager;
    }
    
    /**
     * Calculates the fairshare priority for each provided user.
     * 
     * @param userIds the user IDs
     * @return The user IDs paired with their priority
     */
    public Map<Integer, Float> getUserPriorities(Set<Integer> userIds) {
        Map<Integer, Float> userPriorities = new HashMap<>();
        //System.out.println("user priorities... of users num "+userIds.size());
        for (Integer userId : userIds) {                                    
            float currentUserPriority = calculateCurrentUserPriority(userId);
            float pastUserPriority = calculatePastUserPriority(userId);
            userRecordManager.storePriority(userId, pastUserPriority);
            
            userPriorities.put(userId, pastUserPriority + currentUserPriority);
        }
        return userPriorities;
    }  
    
    /**
     * Calculates the fairshare priority of the user for his virtual machines 
     * that are not finished yet. This is equal to the sum of the fairshare usages
     * of these virtual machines. 
     * 
     * @param userId the user's ID
     * @return the current fairshare priority of the user
     */
    private float calculateCurrentUserPriority(int userId) {
        List<VmElement> vms = vmPool.getVmsByUser(userId);
        
        float currentPriority = 0;
        for (VmElement vm : vms) {
            //System.out.println("priority of vm "+vm.getVmId());
            currentPriority += getVmUsage(vm);  
        }
        return currentPriority;
    } 
    
    /**
     * Calculates the fairshare priority of the user for his VMs that are already 
     * finished. This is equal to the sum of the fairshare usages of these virtual 
     * machines. 
     * 
     * @param userId the user's ID
     * @return the past fairshare priority of the user
     */
    private float calculatePastUserPriority(int userId) {
        float pastPriority = userRecordManager.getPriority(userId);
        List<VmElement> vms;
        if (pastPriority == 0) { 
            // get all vms in state DONE
            vms = vmPool.getVms(userId, 6);            
        } else {
            vms = getNewlyDoneVms(userId);              
        }  
        for (VmElement vm : vms) {
            pastPriority += getVmUsage(vm);
        }
        vmRecordManager.delete(VmListExtension.getVmIds(vms));
        return pastPriority;
    }
    
    /**
     * Returns the user's VMs that finished since the 
     * last time when priority was calculated.
     * 
     * @param userId the user's ID
     * @return the newly finished vms
     */
    private List<VmElement> getNewlyDoneVms(int userId) {
        List<VmFairshareRecord> lastVmRecords = vmRecordManager.getRecords(userId); 
        List<Integer> currentVmIds = VmListExtension.getVmIds(vmPool.getVmsByUser(userId));
        
        List<VmElement> newlyDoneVms = new ArrayList<>();
        for (VmFairshareRecord record : lastVmRecords) {
            if (!currentVmIds.contains(record.getVmId())) {
                VmElement newlyDoneVm = vmPool.getVm(record.getVmId());
                newlyDoneVms.add(newlyDoneVm);
            }
        }
        return newlyDoneVms;
    } 
    
    /**
     * Calculates the fairshare usage of a virtual machine.
     * 
     * @param vm the virtual machine
     * @return the fairshare usage of the virtual machine
     */
    private float getVmUsage(VmElement vm) {      
        //System.out.println("past usage of vm "+vm.getVmId());
        float pastVmUsage = calculatePastVmUsage(vm);  
        float activeVmUsage = calculateActiveVmUsage(vm);
        // unless vm is DONE
        if (vm.getState() != 6) {
            VmFairshareRecord newRecord = vmRecordManager.createRecord(vm, pastVmUsage);
            vmRecordManager.storeRecord(newRecord);
        }
        return activeVmUsage + pastVmUsage;
    } 
    
    /**
     * Calculates the fairshare usage of a virtual machine for his active history 
     * record if any.
     * 
     * @param vm the virtual machine
     * @return the fairshare usage for the active history record
     */
    private float calculateActiveVmUsage(VmElement vm) {
        float usage = 0;
        // if vm is currently running
        if (vm.getState() == 3) {
            HistoryNode lastHistory = vm.getHistories().get(vm.getHistories().size()-1);
            int historyRunTime = vm.getHistoryRuntime(lastHistory);            
            usage += historyRunTime * penaltyCalculator.getPenalty(vm);
        }
        return usage;
    }  
    
    /**
     * Calculates the fairshare usage of a virtual machine for his closed history 
     * records.
     * 
     * @param vm the virtual machine
     * @return the fairshare usage for the closed history records
     */
    private float calculatePastVmUsage(VmElement vm) {
        VmFairshareRecord vmRecord = vmRecordManager.getRecord(vm.getVmId());
        if (vmRecord == null) {
            vmRecord = new VmFairshareRecord(
                    vm.getVmId(), vm.getUid(), 0, -1, vm.getCpu(), vm.getMemory(), vm.getDiskSizes());
        }
        float priority = vmRecord.getUsage();        
        
        VmElement vmFromRecord = vmRecordManager.createVmFromRecord(vm, vmRecord); 
//        System.out.println(vm.getVmId()+" calculate past usage for num histories ="+vm.getClosedHistories().size());
        for (HistoryNode history : vm.getClosedHistories()) {
            if (history.getSequence() > vmRecord.getLastClosedHistory()) {                
                int historyRunTime = vm.getHistoryRuntime(history);            
                priority += historyRunTime * penaltyCalculator.getPenalty(vmFromRecord);
            }       
        }
        return priority;
    }   
}

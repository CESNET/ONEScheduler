package cz.muni.fi.xml.pools;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import cz.muni.fi.scheduler.elementpools.IVmPool;
import cz.muni.fi.scheduler.elements.VmElement;
import cz.muni.fi.xml.mappers.VmXmlMapper;
import cz.muni.fi.xml.resources.lists.VmXmlList;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.mapstruct.factory.Mappers;

import org.opennebula.client.Pool;
import org.opennebula.client.vm.VirtualMachinePool;

/**
 * Class responsible for reading virtual machines from an XML file.
 * 
 * @author Andras Urge
 */
public class VmXmlPool implements IVmPool {
    
    private List<VmElement> vms;

    VmXmlMapper vmXmlMapper = Mappers.getMapper(VmXmlMapper.class);
    
    /**
     * Loads the virtual machines from a file.
     * 
     * @param vmPoolPath path to the file
     * @throws IOException 
     */
    public VmXmlPool(String vmPoolPath) throws IOException {
        XmlMapper xmlMapper = new XmlMapper();
        String vmPoolMessage = new String(Files.readAllBytes(Paths.get(vmPoolPath)));
        VmXmlList xmlList = xmlMapper.readValue(vmPoolMessage, VmXmlList.class);
        //System.out.println("reading vms "+xmlList.getVms().size());
        vms = vmXmlMapper.map(xmlList.getVms());
        //System.out.println("reading finished of vms "+vms.size());
    }
    
    /**
     * Gets a virtual machine by the provided ID.
     * 
     * @param vmId the virtual machine ID
     * @return virtual machine
     */
    @Override
    public VmElement getVm(int vmId) {
        for (VmElement vm : vms) {                       
            if (vmId == vm.getVmId()) {
                return vm;
            }
        }
        return null;
    }
    
    /**
     * Gets all the virtual machines except the ones in state DONE.
     * 
     * @return all not done virtual machines
     */  
    @Override
    public List<VmElement> getVms() {
        return getVms(Pool.ALL, VirtualMachinePool.NOT_DONE);
    }

    /**
     * Gets all the virtual machines.
     * 
     * @return all the virtual machines
     */
    @Override
    public List<VmElement> getAllVms() {
        return getVms(Pool.ALL, VirtualMachinePool.ALL_VM);
    }

    /**
     * Gets the provided user's virtual machines except the ones in state DONE.
     * 
     * @param userId
     * @return user's not done virtual machines
     */
    @Override
    public List<VmElement> getVmsByUser(int userId) {
        return getVms(userId, VirtualMachinePool.NOT_DONE);
    }

    /**
     * Gets the provided user's virtual machines.
     * 
     * @param userId
     * @return user's virtual machines
     */
    @Override
    public List<VmElement> getAllVmsByUser(int userId) {
        return getVms(userId, VirtualMachinePool.ALL_VM); 
    }

    /**
     * Gets all the virtual machines with the provided state.
     * 
     * @param state 
     * @return virtual machines with state
     */
    @Override
    public List<VmElement> getVmsByState(int state) {
        return getVms(Pool.ALL, state);
    }

    /**
     * Gets virtual machines by user and state.
     * @param userId: >= 0            = UID User's Virtual Machines
     *             Pool.ALL        = All Virtual Machines
     *             
     * @param state: 1 = pending
     *             2 = hold
     *             3 = active
     *             4 = stopped
     *             5 = suspended
     *             6 = done
     *             8 = poweroff
     *             9 = undeployed
     *             VirtualMachinePool.ALL_VM   = Flag for Virtual Machines in any state.
     *             VirtualMachinePool.NOT_DONE = Flag for Virtual Machines in any state, except for DONE.
     * 
     * @return array of virtual machines
     */
    @Override
    public List<VmElement> getVms(int userId, int state) {
        List<VmElement> result = new ArrayList<>();
        for (VmElement vm : vms) {
            boolean hasUser = true;
            boolean hasState = true;
           
            if (userId != Pool.ALL) {
                hasUser = (vm.getUid() == userId);
            }
            
            if (state != VirtualMachinePool.ALL_VM) {
                if (state == VirtualMachinePool.NOT_DONE) {
                    hasState = (vm.getState() != 6);
                } else {
                    hasState = (vm.getState() == state);                 
                }
            }
            
            if (hasUser && hasState) {
                result.add(vm);
            }
        }
        
        return result;
    }

    
    /**
     * Goes through the pool and gets VMs with the rescheduling flag.
     * @return the list of VmElements to be rescheduled
     */
    @Override
    public List<VmElement> getReschedVms() {
        return vms.stream().filter(VmElement::isResched).collect(Collectors.toList());
    }
    
}

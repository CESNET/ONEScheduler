package cz.muni.fi.scheduler.elements;

import cz.muni.fi.scheduler.core.TimeManager;
import cz.muni.fi.scheduler.elements.nodes.DiskNode;
import cz.muni.fi.scheduler.elements.nodes.NicNode;
import cz.muni.fi.scheduler.elements.nodes.PciNode;
import cz.muni.fi.scheduler.elements.nodes.HistoryNode;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * This class represents a Virtual Machine.
 * A Virtual Machine within the OpenNebula system consists of:
 * - A capacity in terms memory and CPU
 * - A set of NICs attached to one or more virtual networks
 * - A set of disk (with images or file)
 * - A state file (optional) or recovery file, that contains the memory image of a running VM plus some hypervisor specific information.
 * For further information on creating Virtual machines please refer to: http://docs.opennebula.org/4.14/user/virtual_resource_management/vm_guide.html
 * 
 * @author Gabriela Podolnikova
 */
public class VmElement {
    
    private Integer vmId;
     
    private Integer uid;
    
    private Integer gid;    
    
    private String name;
        
    /**
    * STATE values,
        see http://opennebula.org/_media/documentation:rel3.6:states-complete.png
    */
    private Integer state;
    
    /**
    * LCM_STATE this sub-state is relevant only when STATE is ACTIVE (4)
    * for values see vm.xsd
    */
    private Integer lcm_state;
    
    /**
     * 1 if the VM is considered for rescheduling,
     * 0 otherwise.
     */
    private Integer resched;
        
    private String deploy_id;
    
    private Float cpu;
    
    private Integer memory;
    
    private List<DiskNode> disks;    
    
    private List<HistoryNode> histories;
    
    private List<NicNode> nics;        
    
    private Integer templateId;
    
    /**
     * Possible values:
     * RUNNING_VMS : packing
     * -RUNNING_VMS : stripping
     * FREE_CPU : load aware
     */
    private String schedRank;
    
    /**
     * Possible values:
     * -FREE_MB : packing
     * FREE_MB : striping
     */
    private String schedDsRank;
    
    private String schedRequirements;
    
    private String schedDsRequirements;
    
    private List<PciNode> pcis;
     
    /**
     * Calculates the time how long the virtual machine was really running.
     * 
     * @return The runtime
     */
    public int getRunTime() {        
        int runTime = 0;  
        for (HistoryNode history : histories) {
            runTime += getHistoryRuntime(history);
        }
        return runTime;
    }
    
    /**
     * Calculates the time how long the virtual machine was really running
     * during the given history.
     * 
     * @param history the history of the VM
     * @return The runtime
     */
    public int getHistoryRuntime(HistoryNode history) {
        int runTime = 0;
        boolean isActive = (state == 3);
        boolean isLast = (histories.indexOf(history) == histories.size() - 1);
        
        if (history.isClosed()) {
            runTime = (int) (history.getEndTime() - history.getStartTime());
        } 
        else if (isActive && isLast && history.getStartTime() > 0) {
            long timeStamp = TimeManager.getInstance().getSchedulingTimeStamp().getTime() / 1000; 
            runTime = (int) (timeStamp - history.getStartTime());
        }   
        return runTime;
    }
    
    /**
     * Returns the histories of the VM that are closed already.
     * 
     * @return the closed histories
     */
    public List<HistoryNode> getClosedHistories() {
        if(histories == null){
            return new LinkedList<HistoryNode>();
        }
        System.out.println(this.getVmId()+" closed histories = "+histories.size());
        return histories.stream().filter(HistoryNode::isClosed).collect(Collectors.toList());
    }
    
    /**
     * Returns the last closed history of the VM.
     * 
     * @return the last closed history
     */
    public HistoryNode getLastClosedHistory() {
        if(histories == null){
            return null;
        }
        for (int i=histories.size()-1; i>=0; i--) {
            HistoryNode history = histories.get(i);
            if (history.isClosed()) {
                return history;
            }
        }
        return null;
    }
    
    public int getDiskSizes() {
        int sizeValue = 0;
        for (DiskNode disk : disks) {
            sizeValue += disk.getSize();
        }
        return sizeValue;
    }
    
    public int getFileDiskSizes() {
        int sizeValue = 0;
        for (DiskNode disk : disks) {
            //If disk doesn not have datastore id,
            //it means that it is a volatile disk
            if (disk.getDatastore_id() == null) {
                 sizeValue += disk.getSize();
            }
        }
        return sizeValue;
    }
    
    public int getCopyToSystemDiskSize() {
        int sizeValue = 0;
        for (DiskNode disk : disks) {
            if (disk.copyToSystem()) {
                 sizeValue += disk.getSize();
            }
        }
        return sizeValue;
    }
    
    public List<DiskNode> getDisksWithSelfTarget() {
        return disks.stream().filter(DiskNode::copyToImage).collect(Collectors.toList());
    }
    
    public Boolean isResched() {
        return (Objects.equals(1, this.resched));
    }   
    
    /**
     * @return the id
     */
    public Integer getVmId() {
        return vmId;
    }    
    
    /**
     * @param id the id to set
     */
    public void setVmId(Integer id) {
        this.vmId = id;
    }
    
    /**
     * @return the uid
     */
    public Integer getUid() {
        return uid;
    }

    /**
     * @return the gid
     */
    public Integer getGid() {
        return gid;
    }    

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }
    
    /**
     * @return the state
     */
    public Integer getState() {
        return state;
    }

    /**
     * @param state the state to set
     */
    public void setState(Integer state) {
        this.state = state;
    }

    /**
     * @return the lcm_state
     */
    public Integer getLcm_state() {
        return lcm_state;
    }

    /**
     * @param lcm_state the lcm_state to set
     */
    public void setLcm_state(Integer lcm_state) {
        this.lcm_state = lcm_state;
    }

    /**
     * @return the resched
     */
    public Integer getResched() {
        return resched;
    }

    /**
     * @param resched the resched to set
     */
    public void setResched(Integer resched) {
        this.resched = resched;
    }

    /**
     * @return the deploy_id
     */
    public String getDeploy_id() {
        return deploy_id;
    }

    /**
     * @param deploy_id the deploy_id to set
     */
    public void setDeploy_id(String deploy_id) {
        this.deploy_id = deploy_id;
    }

    /**
     * @param uid the uid to set
     */
    public void setUid(Integer uid) {
        this.uid = uid;
    }

    /**
     * @param gid the gid to set
     */
    public void setGid(Integer gid) {
        this.gid = gid;
    }
    
    @Override
    public String toString() {
        return "VM{" +
                "id='" + getVmId() + '\'' +
                /*", name='" + name + '\'' +
                ", state=" + state +
                ", lcm_state='" + lcm_state + '\'' +
                ", cpu=" + getCpu() + '\'' +
                ", memory=" + getMemory() + '\'' +
                ", schedRank=" + schedRank + '\'' +
                ", schedDsRank=" + schedDsRank + '\'' +
                ", schedRequirements=" + schedRequirements + '\'' +
                ", schedDsRequirements=" + schedDsRequirements + '\'' +
                ", pcis=" + pcis + '\'' +
                ", disks=" + disks + '\'' +
                ", histories=" + histories + '\'' +
                ", nics=" + nics + '\'' +*/
                '}';
    }


    /**
     * @return the cpu
     */
    public Float getCpu() {
        return cpu;
    }

    /**
     * @param cpu the cpu to set
     */
    public void setCpu(Float cpu) {
        this.cpu = cpu;
    }

    /**
     * @return the memory
     */
    public Integer getMemory() {
        return memory;
    }

    /**
     * @param memory the memory to set
     */
    public void setMemory(Integer memory) {
        this.memory = memory;
    }

    /**
     * @return the network_id
     */
    public List<NicNode> getNetwork_ids() {
        return nics;
    }

    /**
     * @param nics the network_id to set
     */
    public void setNetwork_ids(List<NicNode> nics) {
        this.nics= nics;
    }

    /**
     * @return the schedRank
     */
    public String getSchedRank() {
        return schedRank;
    }

    /**
     * @param schedRank the schedRank to set
     */
    public void setSchedRank(String schedRank) {
        this.schedRank = schedRank;
    }

    /**
     * @return the schedDsRank
     */
    public String getSchedDsRank() {
        return schedDsRank;
    }

    /**
     * @param schedDsRank the schedDsRank to set
     */
    public void setSchedDsRank(String schedDsRank) {
        this.schedDsRank = schedDsRank;
    }

    /**
     * @return the schedRequirements
     */
    public String getSchedRequirements() {
        return schedRequirements;
    }

    /**
     * @param schedRequirements the schedRequirements to set
     */
    public void setSchedRequirements(String schedRequirements) {
        this.schedRequirements = schedRequirements;
    }

    /**
     * @return the schedDsRequirements
     */
    public String getSchedDsRequirements() {
        return schedDsRequirements;
    }

    /**
     * @param schedDsRequirements the schedDsRequirements to set
     */
    public void setSchedDsRequirements(String schedDsRequirements) {
        this.schedDsRequirements = schedDsRequirements;
    }

    /**
     * @return the templateId
     */
    public Integer getTemplateId() {
        return templateId;
    }

    /**
     * @param templateId the templateId to set
     */
    public void setTemplateId(Integer templateId) {
        this.templateId = templateId;
    }

    public List<DiskNode> getDisks() {
        return disks;
    }

    public void setDisks(List<DiskNode> disks) {
        this.disks = disks;
    }

    public List<HistoryNode> getHistories() {
        return histories;
    }

    public void setHistories(List<HistoryNode> histories) {
        this.histories = histories;
    }

    /**
     * @return the pcis
     */
    public List<PciNode> getPcis() {
        return pcis;
    }
    
    public void setPcis(List<PciNode> pcis) {
        this.pcis = pcis;
    }

    public List<NicNode> getNics() {
        return nics;
    }

    public void setNics(List<NicNode> nics) {
        this.nics = nics;
    }
    
    @Override
    public int hashCode() {
        int hash = 3;
        hash = 47 * hash + Objects.hashCode(this.getVmId());
        hash = 47 * hash + Objects.hashCode(this.getName());
        hash = 47 * hash + Objects.hashCode(this.getState());
        hash = 47 * hash + Objects.hashCode(this.getCpu());
        hash = 47 * hash + Objects.hashCode(this.getMemory());
        return hash;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof VmElement)) {
            return false;
        }
        final VmElement other = (VmElement) obj;
        if (!Objects.equals(this.getVmId(), other.getVmId())) {
            return false;
        }
        if (!Objects.equals(this.getName(), other.getName())) {
            return false;
        }
        if (!Objects.equals(this.getState(), other.getState())) {
            return false;
        }
        if (!Objects.equals(this.getCpu(), other.getCpu())) {
            return false;
        }
        return Objects.equals(this.getMemory(), other.getMemory());
    }
}

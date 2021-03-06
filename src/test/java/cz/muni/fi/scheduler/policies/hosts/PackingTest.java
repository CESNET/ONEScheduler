package cz.muni.fi.scheduler.policies.hosts;


import cz.muni.fi.scheduler.core.SchedulerData;
import cz.muni.fi.scheduler.elements.HostElement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.runners.MockitoJUnitRunner;

/**
 *
 * @author Gabriela Podolnikova
 */
@RunWith(MockitoJUnitRunner.class)
public class PackingTest {
    
    private List<HostElement> hosts;
    
    HostElement host1;
    HostElement host2;
    HostElement host3;
    HostElement host4;
    HostElement host5;
    HostElement host6;

    @Mock
    private SchedulerData schedulerData;
        
    private Packing packingPolicy;
    
    @Before
    public void init() {
        hosts = new ArrayList<>();
        host1 = new HostElement();
        host1.setId(1);
        host1.setRunningVms(10);
        
        host2 = new HostElement();
        host2.setId(2);
        host2.setRunningVms(0);
        
        host3 = new HostElement();
        host3.setId(3);
        host3.setRunningVms(0);
        
        host4 = new HostElement();
        host4.setId(4);
        host4.setRunningVms(4);
        
        host5 = new HostElement();
        host5.setId(5);       
        host5.setRunningVms(0);
        
        host6 = new HostElement();
        host6.setId(6);
        host6.setRunningVms(3);
        
        hosts.add(host1);
        hosts.add(host2);
        hosts.add(host3);
        hosts.add(host4);
        hosts.add(host5);
        hosts.add(host6);
        packingPolicy = new Packing();
    }
    
    @Test
    public void sortByValue() {
        Map<HostElement, Integer> actualRunningVms = new HashMap<>();
        actualRunningVms.put(host1, 13);
        actualRunningVms.put(host2, 1);
        actualRunningVms.put(host3, 0);
        actualRunningVms.put(host4, 4);
        actualRunningVms.put(host5, 0);
        actualRunningVms.put(host6, 3);
        when(schedulerData.getActualRunningVms(hosts)).thenReturn(actualRunningVms);
        List<HostElement> result = packingPolicy.sortHosts(hosts, schedulerData);
        
        List<HostElement> expectedResult = new ArrayList<>();
        expectedResult.add(host1);
        expectedResult.add(host4);
        expectedResult.add(host6);
        expectedResult.add(host2);
        expectedResult.add(host3);
        expectedResult.add(host5);
        
        assertThat(result, is(expectedResult));
    }
     
}

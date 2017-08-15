package cz.muni.fi.config;

import cz.muni.fi.exceptions.LoadingFailedException;
import cz.muni.fi.result.IResultManager;
import cz.muni.fi.result.OneResultManager;
import cz.muni.fi.result.XmlResultManager;
import cz.muni.fi.scheduler.setup.PropertiesConfig;
import java.io.IOException;
import org.opennebula.client.vm.VirtualMachinePool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Configures the result writers.
 * @author Gabriela Podolnikova
 */
@Configuration
@Import({PoolConfig.class})
public class ResultConfig {
    
    @Autowired PoolConfig poolConfig;
    
    private PropertiesConfig properties;
    
    protected final Logger log = LoggerFactory.getLogger(getClass());
    
    public ResultConfig() throws IOException {
        properties = new PropertiesConfig("configuration.properties");
    }
    
    /**
     * Returns the according result manager.
     * @return the according result manager.
     * @throws LoadingFailedException 
     */
    @Bean 
    public IResultManager resultManager() throws LoadingFailedException { 
        if (properties.getBoolean("testingMode")) {            
            return new XmlResultManager(properties.getString("hostpoolpath"),
                    properties.getString("clusterpoolpath"),
                    properties.getString("userpoolpath"),
                    properties.getString("vmpoolpath"),
                    properties.getString("datastorepoolpath"));
        }
        VirtualMachinePool vmp = new VirtualMachinePool(poolConfig.client());
        return new OneResultManager(vmp);               
    }
}

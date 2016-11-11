package cz.muni.fi.config;

import cz.muni.fi.exceptions.LoadingFailedException;
import cz.muni.fi.scheduler.limits.LimitChecker;
import cz.muni.fi.scheduler.limits.QuotasCheck;
import cz.muni.fi.scheduler.queues.FairshareMapper;
import cz.muni.fi.scheduler.queues.QueueMapper;
import cz.muni.fi.scheduler.select.QueueByQueue;
import cz.muni.fi.scheduler.select.RoundRobin;
import cz.muni.fi.scheduler.select.VmSelector;
import cz.muni.fi.scheduler.setup.PropertiesConfig;
import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 *  Configures scheduler's behaviour.
 * 
 * @author Gabriela Podolnikova
 */
@Configuration
public class SchedulingConfig {
    
    private PropertiesConfig properties;
    
    private static final String FAIRSHARE_MAPPER = "FairshareMapper";
    
    private static final String QUEUE_BY_QUEUE = "QueueByQueue";   
    private static final String ROUND_ROBIN = "RoundRobin";
    
    private static final String QUOTAS_CHECK = "QuotasCheck";
    
    public SchedulingConfig() throws IOException {
        properties = new PropertiesConfig("configuration.properties");
    }
    
    @Bean
    public QueueMapper queueMapper() throws LoadingFailedException {
        switch (properties.getString("queueMapper")) {
            case FAIRSHARE_MAPPER:
                return new FairshareMapper();
            default:
                throw new LoadingFailedException("Wrong queue mapper configuration.");
        }
    }
    
    @Bean
    public VmSelector vmSelector() throws LoadingFailedException {
        switch (properties.getString("vmSelector")) {
            case QUEUE_BY_QUEUE:
                return new QueueByQueue();
            case ROUND_ROBIN:
                return new RoundRobin();
            default:
                throw new LoadingFailedException("Wrong queue mapper configuration.");
        }
    }
    
    @Bean
    public LimitChecker limitChecker() throws LoadingFailedException {
        switch (properties.getString("limitChecker")) {
            case QUOTAS_CHECK:
                return new QuotasCheck();
            default:
                throw new LoadingFailedException("Wrong queue mapper configuration.");
        }
    }
}

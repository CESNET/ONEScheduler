package cz.muni.fi.scheduler.setup;

import cz.muni.fi.scheduler.core.TimeManager;
import cz.muni.fi.scheduler.core.Match;
import cz.muni.fi.scheduler.core.Scheduler;
import cz.muni.fi.config.RecordManagerConfig;
import cz.muni.fi.config.SchedulerConfig;
import cz.muni.fi.exceptions.LoadingFailedException;
import cz.muni.fi.result.IResultManager;
import cz.muni.fi.scheduler.fairshare.historyrecords.IUserFairshareRecordManager;
import cz.muni.fi.scheduler.fairshare.historyrecords.UserFairshareRecordManager;
import cz.muni.fi.scheduler.fairshare.historyrecords.VmFairshareRecordManager;
import cz.muni.fi.scheduler.elements.VmElement;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import org.opennebula.client.ClientConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * This class contains the main method. Loads the configuration file and
 * retreives its attributes. Creates instance of a manager depending on whether
 * we use OpenNebula or our own xml files. Then creates an instance of a
 * Scheduler and starts the scheduling.
 *
 * @author Gabriela Podolnikova
 */
public class SetUp {

    private static PropertiesConfig configuration;
    private static FairshareConfiguration fairshareConfig;

    private static int cycleinterval;
    private static boolean testingMode;

    private static final String DEFAULT_FILE_NAME = "configuration.properties";
    private static final String DEFAULT_FILE_NAME_FAIRSHARE = "fairshare.properties";
    private static Output out = new Output();

    protected static final Logger log = LoggerFactory.getLogger(SetUp.class);

    public static void main(String[] args) throws InterruptedException, ClientConfigurationException, IOException {
        try {
            configuration = new PropertiesConfig(DEFAULT_FILE_NAME);
            fairshareConfig = new FairshareConfiguration(DEFAULT_FILE_NAME_FAIRSHARE);
        } catch (LoadingFailedException | IOException e) {
            log.error("Could not load configuration file!" + e);
            return;
        }

        cycleinterval = configuration.getInt("cycleinterval");
        testingMode = configuration.getBoolean("testingMode");

        String data_set = "pools/experiments/ucc";
        String VMtype[] = {"tiny_VM", "tiny_VM", "small_VM", "medium_VM", "large_VM"};
        double vm_CPUs[] = {0.25, 0.5, 1, 4};
        int vm_RAM[] = {1 * 1024, 4 * 1024, 6 * 1024, 32 * 1024};

        String NodeType[] = {"tiny_Node", "small_Node", "medium_Node", "large_Node"};
        int node_CPUs[] = {8 * 100, 12 * 100, 16 * 100, 40 * 100};
        int node_RAM[] = {128 * 1048576, 90 * 1048576, 128 * 1048576, 256 * 1048576};

        int vm_count[] = {2, 8, 32, 128, 512, 1024};
        int node_count[] = {1, 4, 16, 64, 256, 512};

        String line = "";
        out.deleteResults("runtime.txt");

        for (int vm_type = 0; vm_type < VMtype.length; vm_type++) {
            for (int node_type = 0; node_type < NodeType.length; node_type++) {
                line = VMtype[vm_type] + " x " + NodeType[node_type];
                out.writeString("runtime.txt", line);
                for (int vm_c = 0; vm_c < vm_count.length; vm_c++) {
                    if (vm_c == 0) {
                        line = vm_count[vm_c] + " ";
                        for (int nd_c = 0; nd_c < node_count.length; nd_c++) {
                            line += node_count[nd_c] + " ";
                        }
                        out.writeString("runtime.txt", line);
                    }
                    line = vm_count[vm_c] + " ";
                    for (int nd_c = 0; nd_c < node_count.length; nd_c++) {
                        String filename = data_set + "-" + VMtype[vm_type] + "-" + NodeType[node_type] + "-" + vm_count[vm_c] + "VMs-over-" + node_count[nd_c] + "Nodes";

                        try {
                            changeConfigParams(filename);
                        } catch (IOException ex) {
                            java.util.logging.Logger.getLogger(SetUp.class.getName()).log(Level.SEVERE, null, ex);
                        }

                        if (testingMode) {
                            clearFairshareRecords();
                        }

                        while (true) {
                            long runtime = 0;
                            for (int it = 0; it < 10; it++) {
                                if (testingMode) {
                                    clearFairshareRecords();
                                }
                                log.warn("Starting scheduling cycle.");

                                //here we reload everything - config and data
                                //schedulerConfig calls for other configs - like ResultConfig
                                ApplicationContext context = new AnnotationConfigApplicationContext(SchedulerConfig.class);

                                saveSchedulingTime();
                                checkDecayTime(context.getBean(IUserFairshareRecordManager.class));
                                Scheduler scheduler = context.getBean(Scheduler.class);
                                IResultManager resultManager = context.getBean(IResultManager.class);

                                long start = System.currentTimeMillis();
                                //Plan migrations
                                List<Match> migrations = scheduler.migrate();
                                if (!migrations.isEmpty()) {
                                    printPlan(migrations);
                                    //migrate
                                    List<VmElement> failedMigrations = resultManager.migrate(migrations);
                                    printFailedVms(failedMigrations);
                                }

                                //Plan pendings
                                List<Match> plan = scheduler.schedule();
                                if (planExists(plan)) {
                                    printPlan(plan);
                                    //deploy
                                    List<VmElement> failedVms = resultManager.deployPlan(plan);
                                    printFailedVms(failedVms);
                                }

                                long end = System.currentTimeMillis();
                                log.warn("Scheduling took: " + (end - start) + " miliseconds.");
                                runtime += end - start;
                            }

                            line += (Math.round(runtime * 10 / 10.0) / 10.0 + " ").replace(".", ",");
                            // here we can change config/data to load something new
                            //log.warn("Another cycle will start in " + cycleinterval + "seconds.");
                            break;
                            //TimeUnit.SECONDS.sleep(cycleinterval);

                        }
                        log.warn("Scheduler stopped.");
                    }
                    out.writeString("runtime.txt", line);
                    line = "";
                }
            }
        }
    }

    private static boolean planExists(List<Match> plan) {
        return plan != null;
    }

    private static void clearFairshareRecords() {
        ApplicationContext context = new AnnotationConfigApplicationContext(RecordManagerConfig.class);
        context.getBean(VmFairshareRecordManager.class).clearContent();
        context.getBean(UserFairshareRecordManager.class).clearContent();
    }

    private static void saveSchedulingTime() {
        // TODO: when Dalibor's XML generator is ready add date it provides for testingMode
        TimeManager.getInstance().setSchedulingTimeStamp(new Date());
    }

    private static void checkDecayTime(IUserFairshareRecordManager userRecordManager) {
        long schedulingTime = TimeManager.getInstance().getSchedulingTimeStamp().getTime();
        long lastDecayTime = userRecordManager.getLastDecayTime();
        long decayMillisInterval = TimeUnit.HOURS.toMillis(fairshareConfig.getDecayInterval());

        if (schedulingTime - lastDecayTime > decayMillisInterval) {
            int decayValue = fairshareConfig.getDecayValue();
            userRecordManager.applyDecay(decayValue);
        }
    }

    private static void printPlan(List<Match> plan) {
        if (plan == null) {
            log.info("No schedule.");
            return;
        }
        System.out.println("Schedule:");
        for (Match match : plan) {
            System.out.println("Host: " + match.getHost().getId());
            System.out.println("Its vms: ");
            match.getVms().forEach(System.out::println);
            System.out.println();
        }
    }

    private static void printFailedVms(List<VmElement> failedVms) {
        if (!failedVms.isEmpty()) {
            System.out.println("Failed Vms: ");
            failedVms.forEach(System.out::println);
        }
    }

    private static void changeConfigParams(String path_to_xml) throws IOException {

        Input r = new Input();
        String CONFIG_DIRECTORY = "configFiles";
        String new_config = "";
        String line = null;
        BufferedReader br = r.openFile(new File(CONFIG_DIRECTORY + File.separator + DEFAULT_FILE_NAME));
        while (true) {

            line = br.readLine();

            if (line == null) {
                break;
            } else {
                if (line.contains("hostpoolpath=")) {
                    //replace value
                    line = line.substring(0, line.indexOf("=") + 1) + path_to_xml + "-nodes.xml";
                }
                if (line.contains("vmpoolpath=")) {
                    //replace value
                    line = line.substring(0, line.indexOf("=") + 1) + path_to_xml + "-VM.xml";
                }
                new_config += line + "\n";
            }
        }
        r.closeFile(br);
        br.close();
        out.deleteResults(CONFIG_DIRECTORY + File.separator + DEFAULT_FILE_NAME);
        out.writeString(CONFIG_DIRECTORY + File.separator + DEFAULT_FILE_NAME, new_config);        
    }
}

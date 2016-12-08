##ONEScheduler
ONEScheduler is a custom open source cloud scheduler for OpenNebula and is currently under development. First release is expected in 2017. 
ONEScheduler is a maven project, download or clone the project and run it in your IDE.
ONEScheduler is being developed as a replacement for current scheduler that OpenNebula provides.
Unlike the OpenNebula's scheduler, our scheduler has modular design and can be easily extended.

#####ONEScheduler offers:
- fair-sharing algorithms
- host and datastore criteria based filtration
- simple interfaces for policies
- switch on/off filters and policies
- ease of incorporating new scheduling and fair-sharing policies
- its own configuration file (configuration.properties)
- and cofigurable fairshare (fairshare.properties)

#####ONEScheduler is introducing:
- QueueMapper interface for creating Queues.
  Offering fair-share based queues or simple queue mapping implementations.
- VmSelector interface that chooses the VM that will be processed.
  Two implementation available: RoundRobin and QueueByQueue.

ONEScheduler can be used in two modes:
(You can switch between these two modes in the configuration file under "useXml" field)
- connecting to OpenNebula and obtaining the xml files from OpenNebula (useXml=false)
- providing the xml files in form of "hostpool.xml", "vmpool.xml" etc. (useXml=true) (This mode will be used for the simulations)

For connecting to OpenNebula you need to fill the "secret" and "endpoint" field in configuration file.
- "secret" - A string containing the ONE user:password tuple. Can be null.
- "endpoint" - Where the rpc server is listening, must be something like "http://localhost:2633/RPC2". Can be null.

For running it as a replacement of OpenNebula, you should shut down the OpenNebula scheduler daemon.

This project is a part of a master's thesis at Faculty of Informatics at Masaryk University.

package industrialiot;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import industrialiot.workers.MonitorMachinesWorker;
import industrialiot.workers.PredictiveAnalysisWorker;
import industrialiot.workers.ScheduleRepairWorker;
import java.util.List;
import java.util.Map;
public class IndustrialIotExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 537: Industrial IoT ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("iit_monitor_machines", "iit_predictive_analysis", "iit_schedule_repair"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new MonitorMachinesWorker(), new PredictiveAnalysisWorker(), new ScheduleRepairWorker());
        client.startWorkers(workers);
        if (args.length > 0 && "--workers".equals(args[0])) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("industrial_iot_workflow", 1, Map.of("plantId", "PLANT-EAST-01", "machineId", "CNC-537-A", "machineType", "cnc_mill"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        String s = wf.getStatus().name();
        System.out.println("  Status: " + s + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(s)?"\nResult: PASSED":"\nResult: FAILED");
        System.exit("COMPLETED".equals(s)?0:1);
    }
}

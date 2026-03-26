package smarthome;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import smarthome.workers.DetectEventWorker;
import smarthome.workers.EvaluateRulesWorker;
import smarthome.workers.ActuateLightsWorker;
import smarthome.workers.ActuateThermostatWorker;
import smarthome.workers.ActuateSecurityWorker;
import smarthome.workers.LogEventWorker;
import java.util.List;
import java.util.Map;
public class SmartHomeExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 536: Smart Home ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("smh_detect_event", "smh_evaluate_rules", "smh_actuate_lights", "smh_actuate_thermostat", "smh_actuate_security", "smh_log_event"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new DetectEventWorker(), new EvaluateRulesWorker(), new ActuateLightsWorker(), new ActuateThermostatWorker(), new ActuateSecurityWorker(), new LogEventWorker());
        client.startWorkers(workers);
        if (args.length > 0 && "--workers".equals(args[0])) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("smart_home_workflow", 1, Map.of("eventId", "EVT-536-001", "sensorId", "PIR-LIVING-01", "eventType", "motion_detected", "value", 1));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        String s = wf.getStatus().name();
        System.out.println("  Status: " + s + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(s)?"\nResult: PASSED":"\nResult: FAILED");
        System.exit("COMPLETED".equals(s)?0:1);
    }
}

package agricultureiot;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import agricultureiot.workers.SoilSensorsWorker;
import agricultureiot.workers.WeatherDataWorker;
import agricultureiot.workers.IrrigationDecisionWorker;
import agricultureiot.workers.ActuateWorker;
import java.util.List;
import java.util.Map;
public class AgricultureIotExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 546: Agriculture IoT ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("agr_soil_sensors", "agr_weather_data", "agr_irrigation_decision", "agr_actuate"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new SoilSensorsWorker(), new WeatherDataWorker(), new IrrigationDecisionWorker(), new ActuateWorker());
        client.startWorkers(workers);
        if (args.length > 0 && "--workers".equals(args[0])) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("agriculture_iot_demo", 1, Map.of("fieldId", "FIELD-7", "cropType", "corn"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        String s = wf.getStatus().name();
        System.out.println("  Status: " + s + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(s)?"\nResult: PASSED":"\nResult: FAILED");
        System.exit("COMPLETED".equals(s)?0:1);
    }
}

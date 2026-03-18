package devicemanagement;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import devicemanagement.workers.*;

import java.util.List;
import java.util.Map;

/**
 * Device Management Demo
 *
 * IoT device lifecycle: register device, provision credentials,
 * configure settings, monitor health, and push updates.
 *
 * Run:
 *   java -jar target/device-management-1.0.0.jar --workers
 */
public class DeviceManagementExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Device Management Demo ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "dev_register_device", "dev_provision", "dev_configure",
                "dev_monitor_health", "dev_push_update"));
        System.out.println("  Registered 5 task definitions.\n");

        System.out.println("Step 2: Registering workflow 'device_management_workflow'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new RegisterDeviceWorker(),
                new ProvisionWorker(),
                new ConfigureWorker(),
                new MonitorHealthWorker(),
                new PushUpdateWorker()
        );
        client.startWorkers(workers);
        System.out.println("  5 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("device_management_workflow", 1,
                Map.of("deviceId", "DEV-532-TEMP-001",
                        "deviceType", "sensor",
                        "fleetId", "FLEET-WAREHOUSE",
                        "firmwareVersion", "2.4.1"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Output: " + workflow.getOutput());

        client.stopWorkers();

        if ("COMPLETED".equals(status)) {
            System.out.println("\nResult: PASSED");
            System.exit(0);
        } else {
            System.out.println("\nResult: FAILED");
            System.exit(1);
        }
    }
}

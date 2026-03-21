package gpuorchestration;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import gpuorchestration.workers.GpuCheckAvailabilityWorker;
import gpuorchestration.workers.GpuAllocateWorker;
import gpuorchestration.workers.GpuSubmitJobWorker;
import gpuorchestration.workers.GpuCollectResultsWorker;
import gpuorchestration.workers.GpuReleaseWorker;

import java.util.List;
import java.util.Map;

/**
 * GPU Orchestration Demo
 *
 * Run:
 *   java -jar target/gpuorchestration-1.0.0.jar
 */
public class GpuOrchestrationExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== GPU Orchestration Demo ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "gpu_check_availability",
                "gpu_allocate",
                "gpu_submit_job",
                "gpu_collect_results",
                "gpu_release"));
        System.out.println("  Tasks registered.\n");

        System.out.println("Step 2: Registering workflow 'gpu_orchestration_demo'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new GpuCheckAvailabilityWorker(),
                new GpuAllocateWorker(),
                new GpuSubmitJobWorker(),
                new GpuCollectResultsWorker(),
                new GpuReleaseWorker());
        client.startWorkers(workers);
        System.out.println("  " + workers.size() + " workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("gpu_orchestration_demo", 1,
                Map.of("jobId", "TRAIN-001", "gpuType", "A100", "modelPath", "/models/resnet50"));
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
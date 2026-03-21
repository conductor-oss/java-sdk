package edgecomputing;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import edgecomputing.workers.OffloadTaskWorker;
import edgecomputing.workers.ProcessEdgeWorker;
import edgecomputing.workers.SyncResultsWorker;
import edgecomputing.workers.AggregateCloudWorker;
import java.util.List;
import java.util.Map;
public class EdgeComputingExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 534: Edge Computing ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("edg_offload_task", "edg_process_edge", "edg_sync_results", "edg_aggregate_cloud"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new OffloadTaskWorker(), new ProcessEdgeWorker(), new SyncResultsWorker(), new AggregateCloudWorker());
        client.startWorkers(workers);
        if (args.length > 0 && "--workers".equals(args[0])) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("edge_computing_workflow", 1, Map.of("jobId", "EDG-534-001", "edgeNodeId", "node-west-07", "taskType", "image_classification", "payload", Map.of("dataSize", 2048, "priority", "high")));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        String s = wf.getStatus().name();
        System.out.println("  Status: " + s + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(s)?"\nResult: PASSED":"\nResult: FAILED");
        System.exit("COMPLETED".equals(s)?0:1);
    }
}

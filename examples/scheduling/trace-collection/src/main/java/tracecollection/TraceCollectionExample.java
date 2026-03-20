package tracecollection;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import tracecollection.workers.*;
import java.util.List;
import java.util.Map;

public class TraceCollectionExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 416: Trace Collection ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("trc_instrument","trc_collect_spans","trc_assemble_trace","trc_store_trace"));
        client.registerWorkflow("workflow.json");
        client.startWorkers(List.of(new InstrumentWorker(), new CollectSpansWorker(), new AssembleTraceWorker(), new StoreTraceWorker()));
        if (args.length > 0 && "--workers".equals(args[0])) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("trace_collection_416", 1, Map.of("serviceName","checkout-flow","traceId","trace-8128aabb","samplingRate","100%"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        System.out.println("  Status: " + wf.getStatus().name() + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.exit("COMPLETED".equals(wf.getStatus().name()) ? 0 : 1);
    }
}

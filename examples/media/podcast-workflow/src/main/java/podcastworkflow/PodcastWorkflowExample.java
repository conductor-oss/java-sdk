package podcastworkflow;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import podcastworkflow.workers.RecordWorker;
import podcastworkflow.workers.EditWorker;
import podcastworkflow.workers.TranscribeWorker;
import podcastworkflow.workers.PublishWorker;
import podcastworkflow.workers.DistributeWorker;
import java.util.List;
import java.util.Map;
public class PodcastWorkflowExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 514: Podcast Workflow ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("pod_record", "pod_edit", "pod_transcribe", "pod_publish", "pod_distribute"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new RecordWorker(), new EditWorker(), new TranscribeWorker(), new PublishWorker(), new DistributeWorker());
        client.startWorkers(workers);
        if (args.length > 0 && "--workers".equals(args[0])) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("podcast_workflow", 1, Map.of("episodeId", "EP-514-001", "showName", "Tech Talks Weekly", "episodeTitle", "Workflow Orchestration Deep Dive", "hostId", "HOST-42"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        String s = wf.getStatus().name();
        System.out.println("  Status: " + s + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(s)?"\nResult: PASSED":"\nResult: FAILED");
        System.exit("COMPLETED".equals(s)?0:1);
    }
}

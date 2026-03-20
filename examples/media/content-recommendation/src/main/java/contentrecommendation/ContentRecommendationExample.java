package contentrecommendation;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import contentrecommendation.workers.AnalyzeHistoryWorker;
import contentrecommendation.workers.ComputeSimilarityWorker;
import contentrecommendation.workers.RankResultsWorker;
import contentrecommendation.workers.ApplyFiltersWorker;
import contentrecommendation.workers.ServeRecommendationsWorker;
import java.util.List;
import java.util.Map;
public class ContentRecommendationExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 520: Content Recommendation ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("crm_analyze_history", "crm_compute_similarity", "crm_rank_results", "crm_apply_filters", "crm_serve_recommendations"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new AnalyzeHistoryWorker(), new ComputeSimilarityWorker(), new RankResultsWorker(), new ApplyFiltersWorker(), new ServeRecommendationsWorker());
        client.startWorkers(workers);
        if (args.length > 0 && "--workers".equals(args[0])) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("content_recommendation_workflow", 1, Map.of("userId", "USER-520-001", "contentType", "all", "maxResults", 5));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        String s = wf.getStatus().name();
        System.out.println("  Status: " + s + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(s)?"\nResult: PASSED":"\nResult: FAILED");
        System.exit("COMPLETED".equals(s)?0:1);
    }
}

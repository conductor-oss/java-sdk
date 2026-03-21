package seoworkflow;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import seoworkflow.workers.AuditSiteWorker;
import seoworkflow.workers.ResearchKeywordsWorker;
import seoworkflow.workers.OptimizeContentWorker;
import seoworkflow.workers.SubmitSitemapWorker;
import seoworkflow.workers.MonitorRankingsWorker;
import java.util.List;
import java.util.Map;
public class SeoWorkflowExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 525: SEO Workflow ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("seo_audit_site", "seo_research_keywords", "seo_optimize_content", "seo_submit_sitemap", "seo_monitor_rankings"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new AuditSiteWorker(), new ResearchKeywordsWorker(), new OptimizeContentWorker(), new SubmitSitemapWorker(), new MonitorRankingsWorker());
        client.startWorkers(workers);
        if (args.length > 0 && "--workers".equals(args[0])) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("seo_workflow", 1, Map.of("siteUrl", "https://www.example.com", "targetKeywords", List.of("conductor workflow", "orchestration", "microservices"), "pageUrl", "https://www.example.com/blog/seo-guide"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        String s = wf.getStatus().name();
        System.out.println("  Status: " + s + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(s)?"\nResult: PASSED":"\nResult: FAILED");
        System.exit("COMPLETED".equals(s)?0:1);
    }
}

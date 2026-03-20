package translationpipeline;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import translationpipeline.workers.DetectLanguageWorker;
import translationpipeline.workers.TranslateWorker;
import translationpipeline.workers.ReviewTranslationWorker;
import translationpipeline.workers.PublishTranslationWorker;
import java.util.List;
import java.util.Map;
public class TranslationPipelineExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 517: Translation Pipeline ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("trn_detect_language", "trn_translate", "trn_review_translation", "trn_publish_translation"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new DetectLanguageWorker(), new TranslateWorker(), new ReviewTranslationWorker(), new PublishTranslationWorker());
        client.startWorkers(workers);
        if (args.length > 0 && "--workers".equals(args[0])) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("translation_pipeline_workflow", 1, Map.of("contentId", "CNT-517-001", "sourceText", "Automate your workflows with Conductor for efficient orchestration.", "targetLanguage", "fr"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        String s = wf.getStatus().name();
        System.out.println("  Status: " + s + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(s)?"\nResult: PASSED":"\nResult: FAILED");
        System.exit("COMPLETED".equals(s)?0:1);
    }
}

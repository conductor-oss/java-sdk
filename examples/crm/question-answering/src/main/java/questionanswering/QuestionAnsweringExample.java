package questionanswering;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import questionanswering.workers.*;
import java.util.List;
import java.util.Map;
public class QuestionAnsweringExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 637: Question Answering ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("qas_parse_question", "qas_retrieve_context", "qas_generate_answer"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new ParseQuestionWorker(), new RetrieveContextWorker(), new GenerateAnswerWorker());
        client.startWorkers(workers);
        if (args.length > 0 && "--workers".equals(args[0])) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("qas_question_answering", 1, Map.of("question", "How do I configure workflow timeouts?", "knowledgeBase", "conductor-docs"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        String status = wf.getStatus().name();
        System.out.println("  Status: " + status + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(status) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}

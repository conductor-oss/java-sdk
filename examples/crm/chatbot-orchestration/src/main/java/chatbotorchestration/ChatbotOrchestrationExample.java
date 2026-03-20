package chatbotorchestration;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import chatbotorchestration.workers.*;
import java.util.List;
import java.util.Map;

public class ChatbotOrchestrationExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 631: Chatbot Orchestration ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("cbo_receive", "cbo_understand_intent", "cbo_generate_response", "cbo_deliver"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new ReceiveWorker(), new UnderstandIntentWorker(), new GenerateResponseWorker(), new DeliverWorker());
        client.startWorkers(workers);
        if (workersOnly) { System.out.println("Worker-only mode."); Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("cbo_chatbot_orchestration", 1,
                Map.of("userId", "USR-CHAT01", "message", "I'd like to request a refund for my last charge", "sessionId", "SES-CHAT-001"));
        System.out.println("  Workflow ID: " + wfId);
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        String status = wf.getStatus().name();
        System.out.println("  Status: " + status + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(status) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}

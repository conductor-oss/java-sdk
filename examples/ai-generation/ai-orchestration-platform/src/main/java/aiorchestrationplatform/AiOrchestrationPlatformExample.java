package aiorchestrationplatform;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import aiorchestrationplatform.workers.ReceiveRequestWorker;
import aiorchestrationplatform.workers.RouteModelWorker;
import aiorchestrationplatform.workers.ExecuteWorker;
import aiorchestrationplatform.workers.ValidateWorker;
import aiorchestrationplatform.workers.RespondWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 800: AI Orchestration Platform — Receive Request, Route Model, Execute, Validate, Respond
 */
public class AiOrchestrationPlatformExample {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 800: AI Orchestration Platform — Receive Request, Route Model, Execute, Validate, Respond ===\n");
        ConductorClientHelper helper = new ConductorClientHelper();
        helper.registerTaskDefs(List.of("aop_receive_request", "aop_route_model", "aop_execute", "aop_validate", "aop_respond"));
        helper.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new ReceiveRequestWorker(), new RouteModelWorker(), new ExecuteWorker(), new ValidateWorker(), new RespondWorker());
        helper.startWorkers(workers);
        try {
            String workflowId = helper.startWorkflow("aop_ai_orchestration_platform", 1, Map.of("requestType", "summarization", "payload", "A long article about renewable energy...", "priority", "high"));
            Workflow workflow = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);
            System.out.printf("%nWorkflow status: %s%n", workflow.getStatus());
            System.out.printf("model: %s%n", workflow.getOutput().get("model"));
            System.out.println("\nResult: PASSED");
        } finally { helper.stopWorkers(); }
    }
}

package jiraintegration;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import jiraintegration.workers.CreateIssueWorker;
import jiraintegration.workers.TrackStatusWorker;
import jiraintegration.workers.TransitionWorker;
import jiraintegration.workers.NotifyWorker;

import java.util.List;
import java.util.Map;

/**
 * Jira Integration Demo
 *
 * Runs a Jira integration workflow:
 *   jra_create_issue -> jra_track_status -> jra_transition -> jra_notify
 *
 * Run:
 *   java -jar target/jira-integration-1.0.0.jar
 */
public class JiraIntegrationExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Jira Integration Demo ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "jra_create_issue", "jra_track_status",
                "jra_transition", "jra_notify"));
        System.out.println("  Registered: jra_create_issue, jra_track_status, jra_transition, jra_notify\n");

        System.out.println("Step 2: Registering workflow 'jira_integration'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new CreateIssueWorker(),
                new TrackStatusWorker(),
                new TransitionWorker(),
                new NotifyWorker()
        );
        client.startWorkers(workers);
        System.out.println("  4 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("jira_integration", 1,
                Map.of("project", "ENG",
                        "summary", "Fix login timeout issue",
                        "description", "Users experiencing timeout on login page",
                        "assignee", "jane.doe"));
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

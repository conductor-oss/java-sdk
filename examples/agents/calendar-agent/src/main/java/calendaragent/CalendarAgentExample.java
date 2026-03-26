package calendaragent;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import calendaragent.workers.ParseRequestWorker;
import calendaragent.workers.CheckCalendarWorker;
import calendaragent.workers.FindSlotsWorker;
import calendaragent.workers.BookMeetingWorker;

import java.util.List;
import java.util.Map;

/**
 * Calendar Agent Demo
 *
 * Demonstrates a sequential pipeline of four workers that schedule a meeting:
 * parse request, check calendars, find available slots, and book the meeting.
 *   cl_parse_request -> cl_check_calendar -> cl_find_slots -> cl_book_meeting
 *
 * Run:
 *   java -jar target/calendar-agent-1.0.0.jar
 */
public class CalendarAgentExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Calendar Agent Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "cl_parse_request", "cl_check_calendar",
                "cl_find_slots", "cl_book_meeting"));
        System.out.println("  Registered: cl_parse_request, cl_check_calendar, cl_find_slots, cl_book_meeting\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'calendar_agent'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new ParseRequestWorker(),
                new CheckCalendarWorker(),
                new FindSlotsWorker(),
                new BookMeetingWorker()
        );
        client.startWorkers(workers);
        System.out.println("  4 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 — Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("calendar_agent", 1,
                Map.of("request", "Schedule a Q1 architecture review meeting with the engineering leads next week",
                        "attendees", List.of("alice@company.com", "bob@company.com", "carol@company.com"),
                        "duration", "1 hour",
                        "preferredDate", "next week"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 — Wait for completion
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

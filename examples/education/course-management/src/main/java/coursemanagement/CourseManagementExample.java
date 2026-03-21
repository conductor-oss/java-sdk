package coursemanagement;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import coursemanagement.workers.CreateCourseWorker;
import coursemanagement.workers.ScheduleCourseWorker;
import coursemanagement.workers.AssignInstructorWorker;
import coursemanagement.workers.PublishCourseWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 672: Course Management — Create, Schedule & Publish
 *
 * Run:
 *   java -jar target/course-management-1.0.0.jar
 */
public class CourseManagementExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 672: Course Management ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of("crs_create", "crs_schedule", "crs_assign_instructor", "crs_publish"));

        System.out.println("Step 2: Registering workflow...");
        client.registerWorkflow("workflow.json");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new CreateCourseWorker(),
                new ScheduleCourseWorker(),
                new AssignInstructorWorker(),
                new PublishCourseWorker());
        client.startWorkers(workers);
        System.out.println("  4 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("crs_course_management", 1,
                Map.of("courseName", "Data Structures and Algorithms",
                       "department", "Computer Science",
                       "credits", 4,
                       "semester", "Fall 2024"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Course: " + workflow.getOutput().get("courseId"));
        System.out.println("  Schedule: " + workflow.getOutput().get("schedule"));
        System.out.println("  Instructor: " + workflow.getOutput().get("instructor"));
        System.out.println("  Published: " + workflow.getOutput().get("published"));

        client.stopWorkers();
        System.out.println("COMPLETED".equals(status) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}

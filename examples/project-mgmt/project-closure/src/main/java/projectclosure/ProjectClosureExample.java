package projectclosure;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import projectclosure.workers.*;

import java.util.List;
import java.util.Map;

public class ProjectClosureExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 909: Project Closure ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("pcl_review_deliverables", "pcl_sign_off", "pcl_archive", "pcl_lessons_learned"));
        client.registerWorkflow("workflow.json");
        client.startWorkers(List.of(new ReviewDeliverablesWorker(), new SignOffWorker(), new ArchiveWorker(), new LessonsLearnedWorker()));
        if (workersOnly) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("project_closure_project-closure", 1, Map.of("projectId", "PRJ-909", "projectName", "Cloud Migration", "manager", "Sarah Chen"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        String status = wf.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(status) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}

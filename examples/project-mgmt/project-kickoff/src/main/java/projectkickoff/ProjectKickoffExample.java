package projectkickoff;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import projectkickoff.workers.*;
import java.util.List; import java.util.Map;
public class ProjectKickoffExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example project-kickoff: Project Kickoff ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("pkf_define_scope","pkf_assign_team","pkf_create_plan","pkf_kick_off"));
        client.registerWorkflow("workflow.json");
        client.startWorkers(List.of(new DefineScopeWorker(),new AssignTeamWorker(),new CreatePlanWorker(),new KickOffWorker()));
        if (workersOnly) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("project_kickoff_project-kickoff", 1, Map.of("projectName","Project Alpha","sponsor","VP Engineering","budget",150000));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        String status = wf.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(status) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}

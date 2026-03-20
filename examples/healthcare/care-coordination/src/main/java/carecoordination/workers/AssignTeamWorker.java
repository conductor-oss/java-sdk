package carecoordination.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Assembles a care team based on the care plan and acuity.
 * Input: patientId, careplan, acuity
 * Output: team (list of role/name maps)
 */
public class AssignTeamWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ccr_assign_team";
    }

    @Override
    public TaskResult execute(Task task) {
        String acuity = (String) task.getInputData().get("acuity");
        if (acuity == null) acuity = "low";

        System.out.println("  [team] Assembling care team for acuity: " + acuity);

        List<Map<String, String>> team = List.of(
                Map.of("role", "PCP", "name", "Dr. Martinez"),
                Map.of("role", "Care Coordinator", "name", "Jane Smith, RN"),
                Map.of("role", "PT", "name", "Mike Johnson, DPT"),
                Map.of("role", "Dietitian", "name", "Lisa Park, RD")
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("team", team);
        return result;
    }
}

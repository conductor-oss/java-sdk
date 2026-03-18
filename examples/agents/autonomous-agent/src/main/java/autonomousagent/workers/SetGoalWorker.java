package autonomousagent.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;

/**
 * Translates a high-level mission into a concrete goal with constraints.
 * Input:  {mission}
 * Output: {goal, constraints}
 */
public class SetGoalWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "aa_set_goal";
    }

    @Override
    public TaskResult execute(Task task) {
        String mission = (String) task.getInputData().get("mission");
        if (mission == null || mission.isBlank()) {
            mission = "";
        }

        System.out.println("  [aa_set_goal] Mission: " + mission);

        String goal = "Build and deploy a monitoring dashboard with alerting capabilities";

        List<String> constraints = List.of(
                "Must complete in 3 steps",
                "Use existing infrastructure",
                "Zero-downtime deployment"
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("goal", goal);
        result.getOutputData().put("constraints", constraints);
        return result;
    }
}

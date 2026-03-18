package autonomousagent.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Produces the final report summarising the autonomous agent's work.
 * Input:  {mission, goal, totalSteps}
 * Output: {report, success, completionTime}
 */
public class FinalReportWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "aa_final_report";
    }

    @Override
    public TaskResult execute(Task task) {
        String mission = (String) task.getInputData().get("mission");
        if (mission == null || mission.isBlank()) {
            mission = "Unknown mission";
        }

        String goal = (String) task.getInputData().get("goal");
        if (goal == null || goal.isBlank()) {
            goal = "Unknown goal";
        }

        int totalSteps = toInt(task.getInputData().get("totalSteps"), 3);

        String report = "Mission '" + mission + "' completed. Goal '" + goal
                + "' achieved in " + totalSteps + " autonomous steps. All systems operational.";

        System.out.println("  [aa_final_report] " + report);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("report", report);
        result.getOutputData().put("success", true);
        result.getOutputData().put("completionTime", "2026-01-15T10:00:00Z");
        return result;
    }

    private int toInt(Object value, int defaultVal) {
        if (value == null) return defaultVal;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }
}

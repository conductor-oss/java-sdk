package dischargeplanning.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Schedules follow-up appointments after discharge.
 * Input: patientId, diagnosis, followUpNeeds
 * Output: appointmentDate, appointments
 */
public class ScheduleFollowupWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "dsc_schedule_followup";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        Object needsObj = task.getInputData().get("followUpNeeds");
        List<?> needs = needsObj instanceof List ? (List<?>) needsObj : List.of();

        System.out.println("  [follow-up] Scheduling " + needs.size() + " follow-up appointments");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("appointmentDate", "2024-03-22");
        result.getOutputData().put("appointments", List.of(
                Map.of("type", "PCP", "date", "2024-03-22"),
                Map.of("type", "Cardiology", "date", "2024-03-29")
        ));
        return result;
    }
}

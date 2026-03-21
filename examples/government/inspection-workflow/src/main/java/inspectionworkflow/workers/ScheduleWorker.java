package inspectionworkflow.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ScheduleWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "inw_schedule";
    }

    @Override
    public TaskResult execute(Task task) {
        String propertyId = (String) task.getInputData().get("propertyId");
        System.out.printf("  [schedule] Inspection scheduled for property %s%n", propertyId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("scheduledDate", "2024-03-15");
        result.getOutputData().put("inspector", "INS-42");
        return result;
    }
}

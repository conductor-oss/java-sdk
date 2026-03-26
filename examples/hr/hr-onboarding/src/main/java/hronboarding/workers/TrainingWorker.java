package hronboarding.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Creates a training plan for the new employee.
 */
public class TrainingWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "hro_training";
    }

    @Override
    public TaskResult execute(Task task) {
        String employeeId = (String) task.getInputData().get("employeeId");

        System.out.println("  [training] Training plan created for " + employeeId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("planId", "TRN-605");
        result.getOutputData().put("courses", 5);
        result.getOutputData().put("durationWeeks", 2);
        return result;
    }
}

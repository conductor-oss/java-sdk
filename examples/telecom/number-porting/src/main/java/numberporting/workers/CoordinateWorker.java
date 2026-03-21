package numberporting.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class CoordinateWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "npt_coordinate";
    }

    @Override
    public TaskResult execute(Task task) {

        String fromCarrier = (String) task.getInputData().get("fromCarrier");
        String toCarrier = (String) task.getInputData().get("toCarrier");
        System.out.printf("  [coordinate] Carriers %s and %s coordinated%n", fromCarrier, toCarrier);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("portDate", "2024-03-12");
        result.getOutputData().put("windowStart", "02:00");
        result.getOutputData().put("windowEnd", "04:00");
        return result;
    }
}

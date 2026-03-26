package dischargeplanning.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;

/**
 * Coordinates discharge services.
 * Input: patientId, services
 * Output: servicesArranged, confirmedServices
 */
public class CoordinateWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "dsc_coordinate";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        Object servicesObj = task.getInputData().get("services");
        List<?> services = servicesObj instanceof List ? (List<?>) servicesObj : List.of();

        System.out.println("  [coordinate] Arranging " + services.size() + " services");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("servicesArranged", services.size());
        result.getOutputData().put("confirmedServices", services);
        return result;
    }
}

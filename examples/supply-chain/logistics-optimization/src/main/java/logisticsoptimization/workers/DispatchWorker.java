package logisticsoptimization.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.*;

public class DispatchWorker implements Worker {
    @Override public String getTaskDefName() { return "lo_dispatch"; }

    @Override public TaskResult execute(Task task) {
        List<?> schedule = (List<?>) task.getInputData().get("schedule");
        int count = schedule != null ? schedule.size() : 0;
        System.out.println("  [dispatch] Dispatched " + count + " vehicles");
        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("dispatched", true);
        r.getOutputData().put("vehiclesDispatched", count);
        return r;
    }
}

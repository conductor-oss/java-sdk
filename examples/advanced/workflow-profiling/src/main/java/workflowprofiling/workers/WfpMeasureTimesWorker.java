package workflowprofiling.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class WfpMeasureTimesWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "wfp_measure_times";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [measure] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("timings", java.util.Map.of("validate", java.util.Map.of("avgMs", 55), "transform", java.util.Map.of("avgMs", 830), "persist", java.util.Map.of("avgMs", 1115)));
        result.getOutputData().put("totalAvgMs", 2033);
        return result;
    }
}
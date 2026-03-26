package workflowprofiling.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class WfpInstrumentWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "wfp_instrument";
    }

    @Override
    public TaskResult execute(Task task) {
        String wfName = (String) task.getInputData().getOrDefault("workflowName", "unknown");
        System.out.println("  [instrument] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("instrumentedWorkflow", wfName + "_profiled");
        result.getOutputData().put("profilingHooks", java.util.List.of("cpu_time", "wall_time", "memory_usage"));
        return result;
    }
}
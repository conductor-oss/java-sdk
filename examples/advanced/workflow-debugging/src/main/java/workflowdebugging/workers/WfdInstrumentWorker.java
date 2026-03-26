package workflowdebugging.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Instruments a workflow with debug trace points.
 * Input: workflowName, debugLevel
 * Output: instrumentedWorkflow, tracePoints
 */
public class WfdInstrumentWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "wfd_instrument";
    }

    @Override
    public TaskResult execute(Task task) {
        String workflowName = (String) task.getInputData().getOrDefault("workflowName", "unknown");
        String debugLevel = (String) task.getInputData().getOrDefault("debugLevel", "INFO");

        System.out.println("  [instrument] Instrumenting workflow \"" + workflowName + "\" at level " + debugLevel);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("instrumentedWorkflow", workflowName + "_instrumented");
        result.getOutputData().put("tracePoints", List.of(
                Map.of("id", "tp-1", "location", "task_start", "type", "timing"),
                Map.of("id", "tp-2", "location", "task_end", "type", "timing"),
                Map.of("id", "tp-3", "location", "decision_branch", "type", "data_capture")));
        return result;
    }
}

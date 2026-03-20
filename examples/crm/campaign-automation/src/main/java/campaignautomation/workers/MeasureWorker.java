package campaignautomation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;

public class MeasureWorker implements Worker {
    @Override public String getTaskDefName() { return "cpa_measure"; }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [measure] Campaign performance measured");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("roi", "245%");
        result.getOutputData().put("metrics", Map.of(
                "ctr", "2.5%", "conversionRate", "4.2%",
                "revenue", 24500, "costPerAcquisition", 12.30));
        return result;
    }
}

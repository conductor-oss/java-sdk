package usageanalytics.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class CollectCdrsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "uag_collect_cdrs";
    }

    @Override
    public TaskResult execute(Task task) {

        String region = (String) task.getInputData().get("region");
        System.out.printf("  [collect] Collected 1.2M CDRs from %s%n", region);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("cdrCount", 1200000);
        result.getOutputData().put("sources", 14);
        return result;
    }
}

package hybridcloud.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class HybClassifyDataWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "hyb_classify_data";
    }

    @Override
    public TaskResult execute(Task task) {
        String dataType = (String) task.getInputData().getOrDefault("dataType", "general");
        boolean isSensitive = java.util.List.of("pii", "phi", "financial").contains(dataType);
        String classification = isSensitive ? "confidential" : "public";
        String target = isSensitive ? "onprem" : "cloud";
        System.out.println("  [classify] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("classification", classification);
        result.getOutputData().put("target", target);
        return result;
    }
}
package networkmonitoring.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class DetectIssuesWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "nmn_detect_issues";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [detect] High latency and packet loss detected");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("issues", java.util.List.of("high-latency", "packet-loss"));
        result.getOutputData().put("issueCount", 2);
        return result;
    }
}

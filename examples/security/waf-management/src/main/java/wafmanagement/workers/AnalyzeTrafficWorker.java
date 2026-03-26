package wafmanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class AnalyzeTrafficWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "waf_analyze_traffic";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [analyze] web-portal: detected SQL injection attempts from 3 IPs");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("analyze_trafficId", "ANALYZE_TRAFFIC-1364");
        result.addOutputData("success", true);
        return result;
    }
}

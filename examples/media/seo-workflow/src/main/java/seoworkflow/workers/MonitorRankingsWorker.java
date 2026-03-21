package seoworkflow.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class MonitorRankingsWorker implements Worker {
    @Override public String getTaskDefName() { return "seo_monitor_rankings"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [monitor] Processing " + task.getInputData().getOrDefault("monitoringStatus", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("monitoringStatus", "active");
        r.getOutputData().put("trackedKeywordCount", 3);
        r.getOutputData().put("checkFrequency", "daily");
        r.getOutputData().put("alertThreshold", "rank_drop_5");
        return r;
    }
}

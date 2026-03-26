package analyticsreporting.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class CollectEventsWorker implements Worker {
    @Override public String getTaskDefName() { return "anr_collect_events"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [collect] Processing " + task.getInputData().getOrDefault("eventCount", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("eventCount", 1250000);
        r.getOutputData().put("rawDataPath", "s3://analytics/raw/526/events.parquet");
        r.getOutputData().put("sourcesProcessed", "sources.length");
        r.getOutputData().put("collectionTimeMs", 4500);
        return r;
    }
}

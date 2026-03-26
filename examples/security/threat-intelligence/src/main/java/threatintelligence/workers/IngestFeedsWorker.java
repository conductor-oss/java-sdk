package threatintelligence.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class IngestFeedsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ti_ingest_feeds";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [ingest] Ingested 2,400 indicators from 5 threat feeds");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("ingest_feedsId", "INGEST_FEEDS-1351");
        result.addOutputData("success", true);
        return result;
    }
}

package intrusiondetection.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class AnalyzeEventsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "id_analyze_events";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [analyze] 198.51.100.42: repeated-auth-failure — 47 suspicious events");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("analyze_eventsId", "ANALYZE_EVENTS-1359");
        result.addOutputData("success", true);
        return result;
    }
}

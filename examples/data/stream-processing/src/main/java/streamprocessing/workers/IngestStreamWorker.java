package streamprocessing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;

public class IngestStreamWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "st_ingest_stream";
    }

    @Override
    public TaskResult execute(Task task) {
        @SuppressWarnings("unchecked")
        List<Object> events = (List<Object>) task.getInputData().get("events");
        if (events == null) events = List.of();

        System.out.println("  [ingest] Received " + events.size() + " streaming events");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("events", events);
        result.getOutputData().put("eventCount", events.size());
        return result;
    }
}

package eventordering.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Buffers incoming events — accepts a list of events and outputs them
 * as a buffered collection along with the total count.
 */
public class BufferEventsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "oo_buffer_events";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> events =
                (List<Map<String, Object>>) task.getInputData().get("events");

        System.out.println("  [oo_buffer_events] Buffering " +
                (events != null ? events.size() : 0) + " events");

        int count = (events != null) ? events.size() : 0;

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("buffered", events != null ? events : List.of());
        result.getOutputData().put("count", count);
        return result;
    }
}

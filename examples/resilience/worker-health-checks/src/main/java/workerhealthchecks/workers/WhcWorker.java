package workerhealthchecks.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Worker for whc_task — processes tasks and tracks health metrics.
 *
 * Maintains thread-safe poll and completed counters that can be used
 * for health monitoring. Each successful execution increments both
 * the pollCount (task was polled) and completedCount (task was completed).
 *
 * Input:  { "data": "some-value" }
 * Output: { "result": "done-some-value", "pollCount": N, "completedCount": N }
 */
public class WhcWorker implements Worker {

    private final AtomicInteger pollCount = new AtomicInteger(0);
    private final AtomicInteger completedCount = new AtomicInteger(0);

    @Override
    public String getTaskDefName() {
        return "whc_task";
    }

    @Override
    public TaskResult execute(Task task) {
        int currentPoll = pollCount.incrementAndGet();

        String data = "";
        Object dataInput = task.getInputData().get("data");
        if (dataInput != null) {
            data = dataInput.toString();
        }

        System.out.println("  [whc_task] Processing task (poll #" + currentPoll + ", data=" + data + ")");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        int currentCompleted = completedCount.incrementAndGet();
        result.getOutputData().put("result", "done-" + data);
        result.getOutputData().put("pollCount", currentPoll);
        result.getOutputData().put("completedCount", currentCompleted);

        return result;
    }

    /**
     * Returns the total number of tasks polled by this worker.
     */
    public int getPollCount() {
        return pollCount.get();
    }

    /**
     * Returns the total number of tasks completed by this worker.
     */
    public int getCompletedCount() {
        return completedCount.get();
    }
}

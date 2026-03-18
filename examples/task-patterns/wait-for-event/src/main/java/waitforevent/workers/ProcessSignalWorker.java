package waitforevent.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Processes the signal data after the WAIT task completes.
 *
 * Takes requestId, signalData, and decision from the WAIT task's output.
 * Returns { processed: true, requestId, decision } to confirm processing.
 */
public class ProcessSignalWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "we_process_signal";
    }

    @Override
    public TaskResult execute(Task task) {
        String requestId = (String) task.getInputData().get("requestId");
        String signalData = (String) task.getInputData().get("signalData");
        String decision = (String) task.getInputData().get("decision");

        System.out.println("  [process_signal] Processing signal for request " + requestId
                + ": decision=" + decision + ", signalData=" + signalData);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("processed", true);
        result.getOutputData().put("requestId", requestId);
        result.getOutputData().put("decision", decision);
        return result;
    }
}

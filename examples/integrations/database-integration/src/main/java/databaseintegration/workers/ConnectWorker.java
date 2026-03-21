package databaseintegration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Connects to source and target databases.
 * Input: sourceDb, targetDb
 * Output: sourceConnectionId, targetConnectionId
 */
public class ConnectWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "dbi_connect";
    }

    @Override
    public TaskResult execute(Task task) {
        String sourceDb = (String) task.getInputData().get("sourceDb");
        String targetDb = (String) task.getInputData().get("targetDb");
        String srcId = "conn-src-" + System.currentTimeMillis();
        String tgtId = "conn-tgt-" + System.currentTimeMillis();
        System.out.println("  [connect] Source: " + sourceDb + " (" + srcId + "), Target: " + targetDb + " (" + tgtId + ")");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("sourceConnectionId", "" + srcId);
        result.getOutputData().put("targetConnectionId", "" + tgtId);
        return result;
    }
}

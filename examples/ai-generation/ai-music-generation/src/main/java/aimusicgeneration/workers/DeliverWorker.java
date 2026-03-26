package aimusicgeneration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class DeliverWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "amg_deliver";
    }

    @Override
    public TaskResult execute(Task task) {

        String trackId = (String) task.getInputData().get("trackId");
        System.out.printf("  [deliver] Track %s delivered as wav%n", trackId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("delivered", true);
        result.getOutputData().put("url", "/tracks/TRK-ai-music-generation-001-M.wav");
        result.getOutputData().put("sizeMB", 45);
        return result;
    }
}

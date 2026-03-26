package aimusicgeneration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class MasterWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "amg_master";
    }

    @Override
    public TaskResult execute(Task task) {

        String trackId = (String) task.getInputData().get("trackId");
        System.out.printf("  [master] Track %s mastered — loudness: -14 LUFS%n", trackId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("masteredTrackId", "TRK-ai-music-generation-001-M");
        result.getOutputData().put("loudness", -14);
        return result;
    }
}

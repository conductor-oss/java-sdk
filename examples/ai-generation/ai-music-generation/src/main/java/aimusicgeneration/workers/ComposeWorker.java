package aimusicgeneration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ComposeWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "amg_compose";
    }

    @Override
    public TaskResult execute(Task task) {

        String genre = (String) task.getInputData().get("genre");
        String mood = (String) task.getInputData().get("mood");
        System.out.printf("  [compose] Melody composed — genre: %s%n", genre, mood);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("composition", "C-major-120bpm");
        result.getOutputData().put("key", "C major");
        result.getOutputData().put("tempo", 120);
        result.getOutputData().put("bars", 32);
        return result;
    }
}

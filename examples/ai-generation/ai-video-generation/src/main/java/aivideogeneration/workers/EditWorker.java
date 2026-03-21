package aivideogeneration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class EditWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "avg_edit";
    }

    @Override
    public TaskResult execute(Task task) {

        String videoId = (String) task.getInputData().get("videoId");
        System.out.printf("  [edit] Video %s edited — transitions, audio sync%n", videoId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("editedVideoId", "VID-ai-video-generation-001-E");
        result.getOutputData().put("edits", 12);
        return result;
    }
}

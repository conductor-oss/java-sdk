package aivideogeneration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class RenderWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "avg_render";
    }

    @Override
    public TaskResult execute(Task task) {

        String videoId = (String) task.getInputData().get("videoId");
        System.out.printf("  [render] Final render of %s at 1080p%n", videoId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("finalVideoId", "VID-ai-video-generation-001-F");
        result.getOutputData().put("duration", 30);
        result.getOutputData().put("format", "mp4");
        result.getOutputData().put("complete", true);
        return result;
    }
}

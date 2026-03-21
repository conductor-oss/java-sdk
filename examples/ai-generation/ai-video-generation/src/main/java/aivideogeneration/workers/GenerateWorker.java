package aivideogeneration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class GenerateWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "avg_generate";
    }

    @Override
    public TaskResult execute(Task task) {

        String storyboard = (String) task.getInputData().get("storyboard");
        String style = (String) task.getInputData().get("style");
        System.out.printf("  [generate] Video clips generated in %s style%n", storyboard, style);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("videoId", "VID-ai-video-generation-001");
        result.getOutputData().put("clips", 5);
        result.getOutputData().put("rawDuration", 35);
        return result;
    }
}

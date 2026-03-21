package aivoicecloning.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class CollectSamplesWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "avc_collect_samples";
    }

    @Override
    public TaskResult execute(Task task) {

        String speakerId = (String) task.getInputData().get("speakerId");
        String language = (String) task.getInputData().get("language");
        System.out.printf("  [collect] 25 voice samples collected for speaker %s%n", speakerId, language);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("sampleCount", 25);
        result.getOutputData().put("totalDuration", "12 minutes");
        return result;
    }
}

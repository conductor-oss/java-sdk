package aivoicecloning.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class DeliverWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "avc_deliver";
    }

    @Override
    public TaskResult execute(Task task) {

        String audioId = (String) task.getInputData().get("audioId");
        System.out.printf("  [deliver] Audio %s delivered as mp3%n", audioId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("delivered", true);
        result.getOutputData().put("url", "/audio/AUD-ai-voice-cloning-001.mp3");
        return result;
    }
}

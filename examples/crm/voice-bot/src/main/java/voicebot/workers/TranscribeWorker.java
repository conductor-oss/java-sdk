package voicebot.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class TranscribeWorker implements Worker {
    @Override public String getTaskDefName() { return "vb_transcribe"; }
    @Override public TaskResult execute(Task task) {
        String transcript = "I need to check the status of my order number twelve thirty four";
        System.out.println("  [transcribe] Audio transcribed: \"" + transcript + "\"");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("transcript", transcript);
        result.getOutputData().put("confidence", 0.96);
        result.getOutputData().put("durationSec", 4.2);
        result.getOutputData().put("language", task.getInputData().get("language"));
        return result;
    }
}

package streamingllm.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Post-processes the assembled LLM response: counts words and marks
 * the result as processed.
 */
public class StreamPostProcessWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "stream_post_process";
    }

    @Override
    public TaskResult execute(Task task) {
        String fullResponse = (String) task.getInputData().get("fullResponse");
        Object chunkCountObj = task.getInputData().get("chunkCount");
        Object streamDurationMsObj = task.getInputData().get("streamDurationMs");

        int wordCount = fullResponse.split("\\s+").length;

        System.out.println("  [post] " + wordCount + " words, "
                + chunkCountObj + " chunks in " + streamDurationMsObj + "ms");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("wordCount", wordCount);
        result.getOutputData().put("processed", true);
        return result;
    }
}

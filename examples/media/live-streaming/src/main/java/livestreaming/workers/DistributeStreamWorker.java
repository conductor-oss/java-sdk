package livestreaming.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;

/**
 * Distributes the encoded stream to CDN edge nodes.
 * Input: streamId, encodedUrl, adaptiveBitrates
 * Output: playbackUrl, cdnNodes, viewerCount, regions
 */
public class DistributeStreamWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "lsm_distribute_stream";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        Object bitratesObj = task.getInputData().get("adaptiveBitrates");
        int bitrateCount = 0;
        if (bitratesObj instanceof List) {
            bitrateCount = ((List<?>) bitratesObj).size();
        }

        System.out.println("  [distribute] Distributing " + bitrateCount + " quality levels via CDN");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("playbackUrl", "https://live.example.com/watch/522");
        result.getOutputData().put("cdnNodes", 12);
        result.getOutputData().put("viewerCount", 2450);
        result.getOutputData().put("regions", List.of("us-east", "us-west", "eu-west", "ap-southeast"));
        return result;
    }
}

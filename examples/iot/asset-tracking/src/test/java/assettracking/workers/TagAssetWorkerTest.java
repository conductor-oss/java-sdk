package assettracking.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class TagAssetWorkerTest {
    @Test void taskDefName() { assertEquals("ast_tag_asset", new TagAssetWorker().getTaskDefName()); }

    @Test void generatesUniqueTagId() {
        TagAssetWorker w = new TagAssetWorker();
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("assetId", "A-001")));
        TaskResult r = w.execute(t);
        assertTrue(((String) r.getOutputData().get("tagId")).startsWith("TAG-"));
        assertTrue(((Number) r.getOutputData().get("batteryLevel")).intValue() >= 90);
    }
}

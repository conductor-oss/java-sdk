package leadscoring.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class CollectSignalsWorkerTest {
    @Test void collectsSignals() {
        CollectSignalsWorker w = new CollectSignalsWorker();
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("companySize", "enterprise", "industry", "tech", "pageViews", 30)));
        TaskResult r = w.execute(t);
        assertNotNull(r.getOutputData().get("signals"));
    }
}

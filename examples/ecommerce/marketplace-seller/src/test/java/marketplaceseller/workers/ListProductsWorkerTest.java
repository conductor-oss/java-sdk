package marketplaceseller.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ListProductsWorkerTest {
    private final ListProductsWorker w = new ListProductsWorker();
    @Test void taskDefName() { assertEquals("mkt_list_products", w.getTaskDefName()); }
    @Test void returnsProductCount() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(Map.of("sellerId", "S1", "category", "decor", "storeId", "STORE-S1")));
        TaskResult r = w.execute(t);
        assertEquals(12, ((Number) r.getOutputData().get("productCount")).intValue());
    }
}

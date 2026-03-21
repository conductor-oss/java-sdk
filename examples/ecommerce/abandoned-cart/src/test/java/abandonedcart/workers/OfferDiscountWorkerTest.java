package abandonedcart.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class OfferDiscountWorkerTest {
    private final OfferDiscountWorker w = new OfferDiscountWorker();
    @Test void taskDefName() { assertEquals("abc_offer_discount", w.getTaskDefName()); }
    @Test void highCartGets15Percent() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(Map.of("cartId", "C1", "cartTotal", 75.0)));
        TaskResult r = w.execute(t);
        assertEquals(15, ((Number) r.getOutputData().get("discountPercent")).intValue());
    }
    @Test void lowCartGets10Percent() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(Map.of("cartId", "C2", "cartTotal", 30.0)));
        TaskResult r = w.execute(t);
        assertEquals(10, ((Number) r.getOutputData().get("discountPercent")).intValue());
    }
}

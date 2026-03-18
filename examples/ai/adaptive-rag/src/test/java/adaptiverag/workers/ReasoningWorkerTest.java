package adaptiverag.workers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReasoningWorkerTest {

    @Test
    void requiresApiKey() {
        assertThrows(IllegalStateException.class, ReasoningWorker::new);
    }

    @Test
    void taskDefNameConstant() {
        assertEquals("ar_reason", "ar_reason");
    }
}

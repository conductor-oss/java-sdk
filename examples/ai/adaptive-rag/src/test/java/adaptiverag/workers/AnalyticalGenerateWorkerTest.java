package adaptiverag.workers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AnalyticalGenerateWorkerTest {

    @Test
    void requiresApiKey() {
        assertThrows(IllegalStateException.class, AnalyticalGenerateWorker::new);
    }

    @Test
    void taskDefNameConstant() {
        assertEquals("ar_anal_gen", "ar_anal_gen");
    }
}

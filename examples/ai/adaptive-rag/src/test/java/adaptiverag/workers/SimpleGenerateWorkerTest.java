package adaptiverag.workers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SimpleGenerateWorkerTest {

    @Test
    void requiresApiKey() {
        assertThrows(IllegalStateException.class, SimpleGenerateWorker::new);
    }

    @Test
    void taskDefNameConstant() {
        assertEquals("ar_simple_gen", "ar_simple_gen");
    }
}

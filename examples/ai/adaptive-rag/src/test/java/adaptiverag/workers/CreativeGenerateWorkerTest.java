package adaptiverag.workers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CreativeGenerateWorkerTest {

    @Test
    void requiresApiKey() {
        assertThrows(IllegalStateException.class, CreativeGenerateWorker::new);
    }

    @Test
    void taskDefNameConstant() {
        assertEquals("ar_creative_gen", "ar_creative_gen");
    }
}

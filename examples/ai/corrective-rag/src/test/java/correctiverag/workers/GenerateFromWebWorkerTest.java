package correctiverag.workers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GenerateFromWebWorkerTest {

    @Test
    void requiresApiKey() {
        assertThrows(IllegalStateException.class, GenerateFromWebWorker::new);
    }

    @Test
    void taskDefNameConstant() {
        assertEquals("cr_generate_from_web", "cr_generate_from_web");
    }
}

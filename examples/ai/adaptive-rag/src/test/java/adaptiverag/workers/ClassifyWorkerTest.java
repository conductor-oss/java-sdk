package adaptiverag.workers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ClassifyWorkerTest {

    @Test
    void requiresApiKey() {
        assertThrows(IllegalStateException.class, ClassifyWorker::new);
    }

    @Test
    void taskDefNameConstant() {
        assertEquals("ar_classify", "ar_classify");
    }
}

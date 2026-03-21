package dataencryption;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MainExampleTest {

    @Test
    void workflowJsonIsLoadable() {
        var is = getClass().getClassLoader().getResourceAsStream("workflow.json");
        assertNotNull(is, "workflow.json should be loadable from resources");
    }

    @Test
    void workerInstantiation() {
        assertEquals("de_classify_data", new dataencryption.workers.ClassifyDataWorker().getTaskDefName());
        assertEquals("de_generate_key", new dataencryption.workers.GenerateKeyWorker().getTaskDefName());
        assertEquals("de_encrypt_data", new dataencryption.workers.EncryptDataWorker().getTaskDefName());
        assertEquals("de_verify_encryption", new dataencryption.workers.VerifyEncryptionWorker().getTaskDefName());
    }
}

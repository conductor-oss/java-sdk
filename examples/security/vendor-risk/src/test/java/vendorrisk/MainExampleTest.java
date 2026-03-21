package vendorrisk;

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
        assertEquals("vr_collect_questionnaire", new vendorrisk.workers.CollectQuestionnaireWorker().getTaskDefName());
        assertEquals("vr_assess_risk", new vendorrisk.workers.AssessRiskWorker().getTaskDefName());
        assertEquals("vr_review_soc2", new vendorrisk.workers.ReviewSoc2Worker().getTaskDefName());
        assertEquals("vr_make_decision", new vendorrisk.workers.MakeDecisionWorker().getTaskDefName());
    }
}

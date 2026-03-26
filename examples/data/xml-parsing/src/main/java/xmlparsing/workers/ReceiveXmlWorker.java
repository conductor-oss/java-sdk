package xmlparsing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Receives raw XML content and passes it along with metadata.
 * Input: xmlContent, rootElement
 * Output: xml, rootElement, xmlSize (length of xml string)
 */
public class ReceiveXmlWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "xp_receive_xml";
    }

    @Override
    public TaskResult execute(Task task) {
        String xmlContent = (String) task.getInputData().get("xmlContent");
        if (xmlContent == null) {
            xmlContent = "";
        }

        String rootElement = (String) task.getInputData().get("rootElement");
        if (rootElement == null) {
            rootElement = "root";
        }

        System.out.println("  [xp_receive_xml] Received XML with root: " + rootElement);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("xml", xmlContent);
        result.getOutputData().put("rootElement", rootElement);
        result.getOutputData().put("xmlSize", xmlContent.length());
        return result;
    }
}

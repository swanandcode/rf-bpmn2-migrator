package org.locademiaz.jbpm.migrator;

import java.io.*;

import org.junit.Assert;
import org.junit.Test;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieModule;
import org.kie.api.io.Resource;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.process.WorkItemHandler;
import org.kie.api.runtime.process.WorkItemManager;
import org.xml.sax.SAXException;

public class MigrationTest {

    @Test
    public void simpleOutputTest() throws SAXException, IOException{
        RuleFlow2BPMN2Migrator migrator = new RuleFlow2BPMN2Migrator();
        InputStream ruleflowInputStream = MigrationTest.class.getResourceAsStream( "/ruleflow.rf" );
        migrator.convertToBpmn2( ruleflowInputStream, System.out );
    }

    @Test
    public void testWorkItemMigration() throws IOException, SAXException {
        final String processId = "com.sample.ruleflow";
        final String workItemName = "Some Work";
        final String processFileName = "/ruleflow.rf";
        final StringBuilder workItemValue = new StringBuilder();

        startProcessWithWorkItem(
            MigrationTest.class.getResourceAsStream(processFileName),
            ResourceType.DRF,
            processId,
            workItemName,
            new WorkItemHandler() {
                @Override
                public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
                    workItemValue.append(workItem.getParameter("workItemParam1"));
                    manager.completeWorkItem(workItem.getId(), null);
                }

                @Override
                public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
                }
            }
        );

        final ByteArrayOutputStream bpmn2OutputStream = new ByteArrayOutputStream();
        new RuleFlow2BPMN2Migrator().convertToBpmn2(MigrationTest.class.getResourceAsStream(processFileName), bpmn2OutputStream);

        startProcessWithWorkItem(
            new ByteArrayInputStream(bpmn2OutputStream.toByteArray()),
            ResourceType.BPMN2,
            processId,
            workItemName,
            new WorkItemHandler() {
                @Override
                public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
                    Assert.assertEquals(workItemValue.toString(), workItem.getParameter("workItemParam1"));
                    manager.completeWorkItem(workItem.getId(), null);
                }

                @Override
                public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
                }
            }
        );
    }

    private void startProcessWithWorkItem(InputStream process,
                                          ResourceType processType,
                                          String processId,
                                          String workItemName,
                                          WorkItemHandler handler) {
        final KieServices kieServices = KieServices.Factory.get();
        final KieFileSystem kfs = kieServices.newKieFileSystem();
        String extension;
        if (processType == ResourceType.BPMN2) {
            extension = ".bpmn2";
        } else if (processType == ResourceType.DRF) {
            extension = ".rf";
        } else {
            throw new IllegalArgumentException("Unsupported resource type: " + processType);
        }
        String path = "src/main/resources/process" + extension;
        Resource resource = kieServices.getResources().newInputStreamResource(process);
        kfs.write(path, resource);
        final KieBuilder kieBuilder = kieServices.newKieBuilder(kfs);
        kieBuilder.buildAll();
        final KieModule kieModule = kieBuilder.getKieModule();
        final KieContainer kieContainer = kieServices.newKieContainer(kieModule.getReleaseId());
        final KieSession ksession = kieContainer.newKieSession();
        ksession.getWorkItemManager().registerWorkItemHandler(workItemName, handler);
        ksession.startProcess(processId);
        ksession.dispose();
    }
}

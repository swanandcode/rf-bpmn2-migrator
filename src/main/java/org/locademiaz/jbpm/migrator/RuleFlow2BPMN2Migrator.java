package org.locademiaz.jbpm.migrator;

import org.drools.core.xml.SemanticModules;
import org.jbpm.compiler.xml.ProcessSemanticModule;
import org.jbpm.compiler.xml.XmlProcessReader;
import org.jbpm.ruleflow.core.RuleFlowProcess;
import org.jbpm.bpmn2.xml.XmlBPMNProcessDumper;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.xml.sax.SAXException;

public class RuleFlow2BPMN2Migrator {

    public void convertToBpmn2(InputStream ruleFlowInputStream, OutputStream bpmn2OutputStream) throws SAXException, IOException {
        SemanticModules semanticModules = new SemanticModules();
        semanticModules.addSemanticModule(new ProcessSemanticModule());
        XmlProcessReader processReader = new XmlProcessReader(semanticModules, Thread.currentThread().getContextClassLoader());
        RuleFlowProcess p = (RuleFlowProcess) processReader.read(ruleFlowInputStream).get(0);
        bpmn2OutputStream.write(XmlBPMNProcessDumper.INSTANCE.dump(p).getBytes());
    }
}
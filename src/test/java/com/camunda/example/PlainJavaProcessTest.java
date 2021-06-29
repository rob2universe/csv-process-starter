package com.camunda.example;

import com.camunda.example.service.CsvConverter;
import com.camunda.example.service.LoggerDelegate;
import connectjar.org.apache.commons.codec.binary.Base64;
import org.camunda.bpm.engine.ProcessEngineConfiguration;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.engine.test.ProcessEngineRule;
import org.camunda.bpm.engine.test.mock.Mocks;
import org.camunda.bpm.engine.variable.Variables;
import org.camunda.bpm.engine.variable.value.FileValue;
import org.camunda.bpm.engine.variable.value.ObjectValue;
import org.camunda.bpm.spring.boot.starter.test.helper.AbstractProcessEngineRuleTest;
import org.camunda.spin.plugin.impl.SpinProcessEnginePlugin;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.runtimeService;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.assertThat;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.withVariables;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Deployment(resources = {"CSVUploadProcess.bpmn", "processCsvItem.bpmn"})
public class PlainJavaProcessTest {

  @Rule
  public ProcessEngineRule engine = new ProcessEngineRule();

  private static final String PD_KEY = "CSVUploadProcess";
  public static final String CSV_FILE_DATA = "csv";

  @Before
  public void setUp() {
    Mocks.register("csvConverter", new CsvConverter());
  }

  @Test
  public void shouldExecuteHappyPath() throws IOException {

    File file = new File("src/test/resources/example.csv");
    byte[] data = Files.readAllBytes(Paths.get("src","test","resources","example.csv"));
    assertNotNull(data);

    FileValue fileValue = Variables.fileValue("example.csv")
        .file(file) // see FileValueTypeImpl.createValue
        .mimeType("text/plain")
        .encoding("UTF-8")
        .create();

    var pi = runtimeService().startProcessInstanceByKey(PD_KEY, withVariables(CSV_FILE_DATA,fileValue));
    assertThat(pi).hasPassed("ProcessCSVTask");
  }

}

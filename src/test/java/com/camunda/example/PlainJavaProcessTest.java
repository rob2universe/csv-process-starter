package com.camunda.example;

import com.camunda.example.service.LoggerDelegate;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.engine.test.mock.Mocks;
import org.camunda.bpm.spring.boot.starter.test.helper.AbstractProcessEngineRuleTest;
import org.junit.Before;
import org.junit.Test;

import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.runtimeService;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.assertThat;

@Deployment(resources = "process.bpmn")
public class PlainJavaProcessTest extends AbstractProcessEngineRuleTest {

  private static final String PD_KEY = "vanilla-camunda-template-process";

  @Before
  public void setUp() {
    Mocks.register("logger", new LoggerDelegate());
  }

  @Test
  public void shouldExecuteHappyPath() {

    var pi = runtimeService().startProcessInstanceByKey(PD_KEY);
    assertThat(pi).hasPassed("LogSomethingTask").isEnded();
  }

}

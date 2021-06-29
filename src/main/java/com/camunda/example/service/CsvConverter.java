/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.camunda.example.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.variable.Variables;
import org.camunda.bpm.engine.variable.value.ObjectValue;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaProperties;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.*;

@Component
public class CsvConverter implements JavaDelegate {

  private static final Logger log = LoggerFactory.getLogger(CsvConverter.class);
  public static final String RESULT_JSON = "resultJson";
  public static final String FILE_VARIABLE_NAME = "fileVariableName";
  public static final String RESULT_LIST = "resultList";
  public static final String PROCESS_TO_START = "processToStart";

  public void execute(DelegateExecution exec) throws RuntimeException {

    // check which extension properties are set to determine input and desired outputs
    Map<String, String> taskProperties = getTaskProperties(exec);
    log.debug("Read taskProperties: {}", taskProperties);

    // read csv from process data
    String fileVar = taskProperties.get(FILE_VARIABLE_NAME);
    ByteArrayInputStream csv = (ByteArrayInputStream) exec.getVariable(fileVar);
    if (csv == null) throw new RuntimeException("No CSV file found in process variable: " + fileVar);

    // convert file contents from engine to List
    List<Map<String, Object>> resultList;
    try {
      resultList = csvInputStreamToList(csv);
      log.debug("Result List from CSV: {}", resultList);
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException("Invalid CSV file", e.getCause());
    }

    // if 'resultJson' property is set create the JSON string and the process data for it
    String resultJsonVar = taskProperties.get(RESULT_JSON);
    if (resultJsonVar != null) {
      try {
        String json = new ObjectMapper().writeValueAsString(resultList);
        log.info("JSON: {}", json);
      } catch (Exception e) {
        e.printStackTrace();
      }
      // add output to process data
      ObjectValue resultJson = Variables
          .objectValue(resultList)
          .serializationDataFormat(Variables.SerializationDataFormats.JSON)
          .create();
      exec.setVariable(resultJsonVar, resultJson);
    } else log.debug("Task property {} not set. No JSON process data added", RESULT_JSON);

    // if 'resultList' property is set add the List to the process data
    String resultListVar = taskProperties.get(RESULT_LIST);
    if (resultListVar != null) {
      ObjectValue resultObj = Variables
          .objectValue(resultList)
          .serializationDataFormat(Variables.SerializationDataFormats.JAVA)
          .create();
      exec.setVariable(resultListVar, resultObj);
    } else log.debug("Task property {} not set. No List process data added.", RESULT_LIST);

    //start a process instance per csv submitting cells of csv row as variables
    String processToStart = taskProperties.get(PROCESS_TO_START);
    if (processToStart != null) {
      resultList.forEach(m -> {
        ProcessInstance pi = exec.getProcessEngineServices().getRuntimeService()
            .startProcessInstanceByKey(processToStart, exec.getProcessBusinessKey(), m);
        log.info("Process Instance {} of type {} started for {}", pi.getId(), processToStart, m);
      });
    } else log.debug("Task property {} not set. No process instances started.", PROCESS_TO_START);
  }

  public List<Map<String, Object>> csvInputStreamToList(ByteArrayInputStream csv) {

    BufferedReader reader = new BufferedReader(new InputStreamReader(csv));
    List<Map<String, Object>> list = new ArrayList<>();

    try (csv) {
      // header row for variable names
      String[] headers = reader.readLine().split(",");
      String line;

      // every data row
      while ((line = reader.readLine()) != null) {
        Map<String, Object> row = new HashMap<>();
        String[] cells = line.split(",");
        // every cell
        for (int j = 0; j < headers.length; j++)
          row.put(headers[j], cells[j]);
        list.add(row);
        log.trace("Line read: {} parsed to: {}", line, row);
      }
    } catch (Exception e) {
      throw new RuntimeException("CSV file is invalid: " + e);
    }
    return list;
  }

  public Map<String, String> getTaskProperties(DelegateExecution exec) {

    Map<String, String> properties = new HashMap<>();
    CamundaProperties camProps = exec.getBpmnModelElementInstance().getExtensionElements()
        .getElementsQuery().filterByType(CamundaProperties.class).singleResult();

    for (CamundaProperty prop : camProps.getCamundaProperties())
      properties.put(prop.getCamundaName(), prop.getCamundaValue());

    return properties;
  }
}
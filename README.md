# csv-process-starter
This is example illustrates how a process can be used to upload a CSV file in CAMUNDA Tasklist 
and start an instances of a given process definition for every row in the CSV. 
The cells of the row will be submitted as process variables.
The example does not depend on specific csv columns. You should be able to use any CSV, 
for instance test data from https://www.mockaroo.com/.  

## Usage
1. Start application using `mvn spring-boot:run`
2. Log into CAMUNDA tasklist under http://localhost:8080/camunda/app/tasklist/default/#/login    
   using credentials: *demo / demo*
3. Start new process *CSV Upload* 
   - add a variable named **csv** of type **File** and upload a CSV file, e.g. [example.csv](/src/test/resources/example.csv)

![Process Start dialog in CAMUNDA Tasklist](/doc/startProcessWithCsvFileAttachment.png)

4. Refresh Tasklist by clicking on 'All Tasks' and find the user task of the new created process instances in the list.

![Process Start dialog in CAMUNDA Tasklist](/doc/usertasksWithProcessDataFromCSV.png)

## Configuration
The extension properties configured in [CSVUploadProcess.bpmn](/src/main/resources/CSVUploadProcess.bpmn) are used to
configure the service task.

- fileVariableName: name of the process data which will contains the file (used at process start) - required
- resultJson: if set the CSV content will be stored as a process data with the specified name in JSON format - optional
- resultJson: if set the CSV content will be stored as a process data with the specified name as a Java List - optional
- processToStart: if set a new process instance for the specified process definition key will be started for each row in the csv. The content of teh row will be submitted as process data. - optional 

![Extension properties of Service Task in CAMUNDA Modeler](/doc/extensionProperties.png)

## Known issues
The CSV parsing logic applied here is very simple. Commas in the file may break the example,
special characters may not be handled correctly. This was done to not introduce additional dependencies.
A more robust implementation could replace the simple Java String `.split(",")` with an existing CSV parser library
such as [Open CSV](https://sourceforge.net/p/opencsv/wiki/Home/)  
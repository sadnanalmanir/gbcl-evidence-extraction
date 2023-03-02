# gbcl-evidence-extraction  
This repository contains the GATE NLP-based implementation of the project titled **A 
pipeline for extracting evidence of crop loss from scientific reports** whose aim
is to extract from preselected documents information that describes crop loss from 
insect infiltration. 

## Components
- GATE NLP installation
- `Main.java` contains command line options for I/O
- `Pipeline.java` contains GATE plugins, gazetteers, JAPE rules
- `CooccerenceMatcher.java` depends on the `CooccerenceMatcher.jape`
rule for selecting annotations simultaneously occurring in sentences 
of length 1/2/3
- `AnnotationExporter.java` takes array of annotations from the 
`main(...)` method to export annotated results as a CSV file
- `gazetteers` directory contains proprietary lists of pests 
(&copy;[CABI](https://www.cabi.org/)) including other lists
- `japerules` directory contains all necessary jape rules for 
producing the final annotations

## Set environments
### [GATE NLP vX.X](https://gate.ac.uk/) 
1. [Download](https://gate.ac.uk/download/) and install GATE
2. Set `GATE_HOME` path in `src/main/resources/project.properties`
3. Before using any plugin, create `plugins` as 
`[gate-installation-directory]/plugins` and load 
> Note: Use [nimbletext](https://nimbletext.com/Live/1498428636/) to format gazetteer with `@id=`
## How to run

### Using the `Main` method
- in Windows, set arguments as `--extract -i H:\html-corpus -o H:\extracted-text`
- in Ubuntu, set arguments as `--extract -i /home/user/html-corpus -o /home/user/extracted-text`
### Using the exec-maven-plugin
1. Set the argument values directly inside the plugin inside `<configuration>`

```xml

<plugin>
    <groupId>org.codehaus.mojo</groupId>
    <artifactId>exec-maven-plugin</artifactId>
    <version>3.1.0</version>
    <configuration>
        <mainClass>uk.ac.rothamsted.ide.gbcl.Main</mainClass>
        <arguments>
            <argument>--pipe</argument>
            <argument>-i H:\[SOURCE_FILES_DIR]</argument>
            <argument>-x H:\[ANNOTATED_FILES_DIR]</argument>
            <argument>--export</argument>
            <argument>-o [ANNOTATION_FILE_NAME].csv</argument>
        </arguments>
    </configuration>
</plugin>
```
or

2. Use `mvn exec:java` from terminal

For Windows
```shell
mvn exec:java -D'exec.mainClass'='uk.ac.rothamsted.ide.gbcl.Main' -D'exec.args'="--pipe -i H:\[SOURCE_FILES_DIR] -x H:\[ANNOTATED_FILES_DIR] --export -o [ANNOTATION_FILE_NAME].csv"
```

For Linux
```shell
 mvn exec:java -Dexec.mainClass=uk.ac.rothamsted.ide.gbcl.Main \
     -Dexec.args="--pipe -i [SOURCE_FILES_DIR] -x [ANNOTATED_FILES_DIR] --export -o [ANNOTATION_FILE_NAME].csv"
```
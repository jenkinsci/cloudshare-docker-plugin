# Building

## Prerequisites

- Java
- Maven

## HPI

This project is built as a HPI package using [the HPI Maven plugin](https://github.com/jenkinsci/maven-hpi-plugin).

To create a HPI file (ready to be intalled as a Jenkins plugin) run `mvn package`. Or just run `make build` (which calls maven).

`target/cloudshare-docker.hpi` should be created.



# Debugging

- Import the project's `pom.xml` in IntelliJ.
- Create a Maven launch configuration that runs `hpi:run -Djetty.port=8081`
![Using IntelliJ](https://i.imgur.com/TIMlr6Z.png)
    - This will launch a Jenkins instance at `http://localhost:8081/jenkins` with the plugin loaded.




# Building

## Prerequisites

- Java (tested with 1.8)
- Maven (tested with 3.3.9)

## Building HPI package

Docker plugins are distributed in `hpi` format. For instance, this plugin is installed as `cloudshare-docker.hpi`.

This project is built as a HPI package using [the HPI Maven plugin](https://github.com/jenkinsci/maven-hpi-plugin).

To create a HPI file (ready to be intalled as a Jenkins plugin) run `mvn package`. Or just run `make build` (which calls maven).

`target/cloudshare-docker.hpi` should be created.

# Debugging

- Import the project's `pom.xml` in IntelliJ.
- Create a Maven launch configuration that runs `hpi:run -Djetty.port=8081`
![Using IntelliJ](https://i.imgur.com/TIMlr6Z.png)
    - Make sure the launch config's PATH variable includes the location of `docker-machine-driver-cloudshare` binary.
    - This will launch a Jenkins instance at `http://localhost:8081/jenkins` with the plugin loaded.
    - To change the version of Jenkins that the debugger runs, edit `pom.xml`.




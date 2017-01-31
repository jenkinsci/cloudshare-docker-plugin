# CloudShare Docker-Machine Jenkins Plugin

Using CloudShare's Jenkins plugin you can have you Docker builds run on dedicated docker-machines on CloudShare, instead of running on the Jenkins host itself.

## Motivation

By using a dedicated docker-machine for each Jenkins project you enjoy the following benefits:

- Each project gets its own dedicated VM that does the actual building & running of docker images. In other words, you get easy parallelization without using Jenkins slaves.
- You can execute docker-compose based tests without worrying about conflicting published ports.
    - Since any docker-compose based test runs in isolation on its own VM, you can easily SSH into it and debug a failed test, if needed, without worrying about disturbing/pausing Jenkins itself.
- No need to worry about docker container/image/volume accumulation and cleanup. The VMs are disposable, and your Jenkins host won't get clogged up with obsolete docker files.
- CloudShare VMs automatically get suspended after there's no more docker activity. You don't need to worry about shutting down slaves when they're not needed to cut costs.
- If your Jenkins actually run in a container, you won't need to mount the docker daemon's socket as a volume and you won't need to run Jenkins as a priviliged container, which is a security concern.

# Installing

## Requirements

- Jenkins 2.0 and up (for pipeline support).
- This plugin depends on [Docker-Machine](https://docs.docker.com/machine/install-machine/).
- The [CloudShare docker-machine driver](https://github.com/cloudshare/docker-machine-driver-cloudshare) must be installed and in Jenkins' `PATH`.

Install this plugin through the Jenkins Plugin Manager.

# Usage

You can enable CloudShare docker-machines for your builds both in classic projects (under `Build Environment`) and as a pipeline step.

## Build Environment

In your Jenkins project's configuration, under `Build Environment`, check the `Run Docker commands on CloudShare VM` box.

![build environment screenshot](https://i.imgur.com/tLlBpDv.png)

You can leave the default machine name template as is.

Now every build step that invokes docker (build, run, docker-compose, etc.) will run against a remote CloudShare docker-machine automatically.

## Pipeline Step

Another way of achieving the same effect is with the `cloudshareDockerMachine` DSL step.

For example, in this pipeline script:

```
node {
    stage('build') {
        git 'https://github.com/cloudshare/express-ws-chat.git'

        cloudshareDockerMachine {
            sh 'docker-compose -p ${JOB_NAME} build'
        }
    }
}
```

The `docker-compose` command will run against a dedicated CloudShare docker-machine, and not on the Jenkins host itself.

If you want to modify the name of the CloudShare environment that's created for the project, you can specify:

```
cloudShareDockerMachine(name: 'my-environment') {
    // docker stuff
}
```

**Outside** the scope of the `cloudshareDockerMachine` step any docker command would run against the local Docker daemon.

package com.cloudshare.jenkins;

import hudson.AbortException;
import hudson.EnvVars;
import hudson.Launcher;
import hudson.Proc;
import hudson.model.TaskListener;
import org.apache.commons.lang.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by assaf on 30/01/2017.
 */
public class DockerMachineSetup {
    public static DockerMachineResult dockerMachineCommand(Launcher launcher, TaskListener listener, String... cmd) throws IOException, InterruptedException {
        Launcher.ProcStarter dmStarter = launcher.launch();
        ArrayList<String> command = new ArrayList<String>();
        command.add("docker-machine");
        command.addAll(Arrays.asList(cmd));
        EnvVars vars = new EnvVars();
        CloudShareConfiguration cfg = CloudShareConfiguration.get();
        String key = cfg.getApiKey();
        String id = cfg.getApiId();

        String credsErrorMessage = "CloudShare API key/ID not supplied.\nPlease supply it in Manage Jenkins -> Configure System -> CloudShare.\nYou can find your API keys at https://use.cloudshare.com/Ent/Vendor/UserDetails.aspx";
        if (StringUtils.isEmpty(key) || StringUtils.isEmpty(id)) {
            throw new AbortException(credsErrorMessage);
        }
        vars.put("CLOUDSHARE_API_ID", id);
        vars.put("CLOUDSHARE_API_KEY", key);
        dmStarter.cmds(command);
        dmStarter.envs(vars);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        if (listener != null) {
            dmStarter.stdout(listener);
        } else {
            dmStarter.stdout(buffer);
        }

        Proc dm = launcher.launch(dmStarter);
        int exitCode = dm.join();
        return new DockerMachineResult(buffer.toString("UTF-8"), exitCode);
    }

    public static void panic(String format, Object... args) throws AbortException {
        String msg = String.format(format, args);
        throw new AbortException(msg);
    }

    public static Map<String, String> startDockerMachine(String dmName, Launcher launcher, TaskListener listener) throws IOException, InterruptedException {
        PrintStream log = listener.getLogger();

        DockerMachineResult res  = dockerMachineCommand(launcher, null, "status", dmName);
        if (res.exitCode != 0) {
            if (res.output.startsWith("Host does not exist")) {
                log.println("CloudShare Docker-Machine for this project not found. Creating one...");
                res = dockerMachineCommand(launcher, listener, "create", "-d", "cloudshare", dmName);
                if (res.exitCode != 0) {
                    panic("Failed to create Docker machine.");
                }
                res  = dockerMachineCommand(launcher, null, "status", dmName);
            }
        } else if (res.output.contains("Driver \"cloudshare\" not found")){
            panic("Could not find CloudShare docker-machine driver in PATH. Make sure the executable from https://github.com/cloudshare/docker-machine-driver-cloudshare/releases/latest is installed, has execution permissions, and is in Jenkin's PATH.");
        } else if (res.output.startsWith("error")){
            panic("Error when testing the status of docker-machine.\n" + res.output);
        }

        if (!res.output.startsWith("Running")) {
            log.printf("Resuming Docker-Machine %s...%n", dmName);
            res = dockerMachineCommand(launcher, null, "start", dmName);
            if (res.exitCode != 0) {
                panic("Docker Machine failed to start.%n%s%n", res.output);
            }
        }


        res = dockerMachineCommand(launcher, null, "inspect", dmName, "--format", "{{.Driver.EnvID}}");
        if (res.exitCode == 0) {
            String envID = res.output.trim().substring(2);
            String URL = String.format("https://use.cloudshare.com/Ent/Environment.mvc/View/%s", envID);
            listener.getLogger().printf("Connected to Docker daemon on CloudShare environment ");
            listener.hyperlink(URL, "docker-machine-" + dmName);
            listener.getLogger().append(".\n");
        }

        res = dockerMachineCommand(launcher, null, "env", "--shell", "/bin/bash", dmName);
        if (res.exitCode != 0) {
            panic("Docker Machine env not available.%n%s%n", res.output);
        }

        Map<String, String> ret = new HashMap<String, String>();
        String []lines = res.output.split("\n");
        for (String line : lines) {
            if (line.startsWith("export")) {
                String keyval = line.substring(7);
                String [] parts = keyval.split("=");
                String key = parts[0];
                String val = parts[1];
                val = val.replaceAll("^\"|\"$", "");
                ret.put(key, val);
            }
        }

        return ret;
    }

    public static class DockerMachineResult {
        public String output;
        public int exitCode;
        public DockerMachineResult(String output, int exitCode) {
            this.output = output;
            this.exitCode = exitCode;
        }
    }
}

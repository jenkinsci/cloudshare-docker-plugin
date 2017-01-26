package com.cloudshare.cloudsharejenkins;

import com.google.common.io.ByteStreams;
import hudson.*;
import hudson.model.*;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.util.FormValidation;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import jenkins.tasks.SimpleBuildWrapper;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.QueryParameter;

import javax.management.RuntimeErrorException;
import javax.servlet.ServletException;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

public class DockerMachineWrapper extends SimpleBuildWrapper {

    private final String name;

    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public DockerMachineWrapper(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    private DockerMachineResult dockerMachineCommand(Launcher launcher, TaskListener listener, String... cmd) throws IOException, InterruptedException {
        Launcher.ProcStarter dmStarter = launcher.launch();
        ArrayList<String> command = new ArrayList<String>();
        command.add("docker-machine");
        command.addAll(Arrays.asList(cmd));
        EnvVars vars = new EnvVars();
        String key = getDescriptor().getApiKey();
        String id = getDescriptor().getApiId();

        String credsErrorMessage = "CloudShare API key/ID not supplied.\nPlease supply it in Manage Jenkins -> Configure System -> CloudShare.\nYou can find your API keys at https://use.cloudshare.com/Ent/Vendor/UserDetails.aspx";
        if (key == null || key == "" || id == null || id == "") {
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
        return new DockerMachineResult(buffer.toString(), exitCode);
    }

    private void panic(String format, String... args) throws AbortException {
        String msg = String.format(format, args);
        throw new AbortException(msg);
    }

    @Override
    public void setUp(Context context, Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener, EnvVars initialEnvironment) throws IOException, InterruptedException {

        PrintStream log = listener.getLogger();

        String dmName = initialEnvironment.expand(name);

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
            log.printf("Resuming Docker-Machine %s...\n", dmName);
            res = dockerMachineCommand(launcher, null, "start", dmName);
            if (res.exitCode != 0) {
                panic("Docker Machine failed to start.\n%s\n", res.output);
            }
        }

        res = dockerMachineCommand(launcher, null, "env", dmName);
        if (res.exitCode != 0) {
            panic("Docker Machine env not available.\n%s\n", res.output);
        }

        exportDockerMachineEnvars(context, res.output);

        res = dockerMachineCommand(launcher, null, "inspect", dmName, "--format", "{{.Driver.EnvID}}");
        if (res.exitCode == 0) {
            String envID = res.output.trim().substring(2);
            String URL = String.format("https://use.cloudshare.com/Ent/Environment.mvc/View/%s", envID);
            listener.getLogger().printf("Connected to Docker daemon on CloudShare environment ");
            listener.hyperlink(URL, "docker-machine-" + dmName);
            listener.getLogger().append(".\n");
        }

    }

    private void exportDockerMachineEnvars(Context ctx, String dmEnvOutput) {
        String []lines = dmEnvOutput.split("\n");
        for (String line : lines) {
            if (line.startsWith("export")) {
                String keyval = line.substring(7);
                String [] parts = keyval.split("=");
                String key = parts[0];
                String val = parts[1];
                val = val.replaceAll("^\"|\"$", "");
                ctx.env(key, val);
            }
        }
    }

    private static class DockerMachineResult {
        public String output;
        public int exitCode;
        public DockerMachineResult(String output, int exitCode) {
            this.output = output;
            this.exitCode = exitCode;
        }
    }

    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildWrapperDescriptor {
        private String apiKey;
        private String apiId;

        public DescriptorImpl() {
            load();
        }

        @Override
        public boolean isApplicable(AbstractProject<?, ?> item) {
            return true;
        }

        public String getDefaultMachineName() {
            return  "jenkins-$JOB_NAME";
        }

        public FormValidation doCheckName(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error("Machine name cannot be empty.");
            return FormValidation.ok();
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return "Run Docker commands on CloudShare VM";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            apiId = formData.getString("apiId");
            apiKey = formData.getString("apiKey");
            save();
            return super.configure(req, formData);
        }

        public String getApiKey() {
            return apiKey;
        }

        public String getApiId() {
            return apiId;
        }
    }
}

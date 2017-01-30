package com.cloudshare.jenkins;

import hudson.*;
import hudson.model.*;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.util.FormValidation;
import jenkins.tasks.SimpleBuildWrapper;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.servlet.ServletException;
import java.io.*;
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

    @Override
    public void setUp(Context context, Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener, EnvVars initialEnvironment) throws IOException, InterruptedException {

        String actualName = initialEnvironment.expand(name);
        Map<String, String> envars = DockerMachineSetup.startDockerMachine(actualName, launcher, listener);
        for (Map.Entry<String, String> e : envars.entrySet()) {
            context.env(e.getKey(), e.getValue());
        }

    }

    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildWrapperDescriptor {
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

    }
}

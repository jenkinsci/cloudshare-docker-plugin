package com.cloudshare.jenkins;

import com.google.common.base.Strings;
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

    private String name;
    private String expiryDays;

    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public DockerMachineWrapper(String name, String expiryDays) {
        this.name = name;
        this.expiryDays = expiryDays;
    }

    public String getName() {
        return name;
    }

    public String getExpiryDays() { return expiryDays; }

    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Override
    public void setUp(Context context, Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener, EnvVars initialEnvironment) throws IOException, InterruptedException {

        String actualName = initialEnvironment.expand(name);
        int days = 0;
        if (!Strings.isNullOrEmpty(expiryDays)) {
            days = Integer.parseInt(this.expiryDays);
        }
        Map<String, String> envars = DockerMachineSetup.startDockerMachine(actualName, launcher, listener, days);
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

        public FormValidation doCheckExpiryDays(@QueryParameter String value) throws IOException, ServletException {
            if (!Strings.isNullOrEmpty(value)) {
                try {
                    Integer.parseInt(value);
                } catch (java.lang.NumberFormatException e) {
                    return FormValidation.error("Please enter a valid number (integer).");
                }
            }
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

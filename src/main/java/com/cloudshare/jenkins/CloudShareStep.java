package com.cloudshare.jenkins;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.steps.*;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.IOException;
import java.util.Map;

public class CloudShareStep extends AbstractStepImpl {
    public static final String DEFAULT_MACHINE_NAME = "jenkins-$JOB_NAME";

    private String name = DEFAULT_MACHINE_NAME;

    @DataBoundConstructor
    public CloudShareStep(String name) {
        if (name != null) {
            setName(name);
        }

    }

    public String getName() {
        return this.name;
    }

    @DataBoundSetter
    public void setName(String name) {
        this.name = name;
    }

    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension
    public static class DescriptorImpl extends AbstractStepDescriptorImpl {

        public String getDefaultMachineName() {
            return DEFAULT_MACHINE_NAME;
        }

        public DescriptorImpl() {
            super(Execution.class);
        }

        @Override
        public String getFunctionName() {
            return "cloudshareDockerMachine";
        }

        @Override
        public String getDisplayName() {
            return "CloudShare Docker-Machine";
        }

        @Override
        public Step newInstance(Map<String, Object> arguments) throws Exception {
            return super.newInstance(arguments);
        }

        @Override
        public boolean takesImplicitBlockArgument() {
            return true;
        }

    }

    public static class Execution extends AbstractStepExecutionImpl {

        @Inject
        private transient CloudShareStep step;

        @StepContextParameter
        private transient TaskListener listener;

        @StepContextParameter
        private transient Launcher launcher;

        @Override
        public boolean start() throws Exception {

            final String actualName = getContext().get(EnvVars.class).expand(step.getName());
            final Map<String, String> dmEnvars = DockerMachineSetup.startDockerMachine(actualName, launcher, listener);

            EnvironmentExpander expander = new EnvironmentExpander() {
                @Override
                public void expand(@Nonnull EnvVars envVars) throws IOException, InterruptedException {
                    envVars.overrideAll(dmEnvars);
                }
            };


            this.getContext().newBodyInvoker()
                    .withContext(EnvironmentExpander.merge(this.getContext().get(EnvironmentExpander.class), expander))
                    .withContext(expander)
                    .withCallback(BodyExecutionCallback.wrap(this.getContext()))
                    .start();
            return false;
        }

        @Override
        public void stop(@Nonnull Throwable throwable) throws Exception {
            //
        }

    }

}

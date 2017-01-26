package com.cloudshare.cloudsharejenkins;

import hudson.*;
import hudson.model.Result;
import hudson.util.FormValidation;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.QueryParameter;

import javax.servlet.ServletException;
import java.io.IOException;
import java.io.PrintStream;

/**
 * Sample {@link Builder}.
 * <p>
 * <p>
 * When the user configures the project and enables this builder,
 * {@link DescriptorImpl#newInstance(StaplerRequest)} is invoked
 * and a new {@link HelloWorldBuilder} is created. The created
 * instance is persisted to the project configuration XML by using
 * XStream, so this allows you to use instance fields (like {@link #name})
 * to remember the configuration.
 * <p>
 * <p>
 * When a build is performed, the {@link #perform} method will be invoked.
 *
 * @author Kohsuke Kawaguchi
 */
public class HelloWorldBuilder extends Builder implements SimpleBuildStep {

    private final String name;

    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public HelloWorldBuilder(String name) {
        this.name = name;
    }

    /**
     * We'll use this from the {@code config.jelly}.
     */
    public String getName() {
        return name;
    }


    private String getMachineName(Run<?, ?> build) {
        return String.format("jenkins-%s", build.getParent().getName());
    }

    @Override
    public void perform(Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener) {
        // This is where you 'build' the project.
        // Since this is a dummy, we just say 'hello world' and call that a build.
        PrintStream log = listener.getLogger();

        // This also shows how you can consult the global configuration of the builder
        String key = getDescriptor().getApiKey();
        String id = getDescriptor().getApiId();
        String dmName = getMachineName(build);
        try {
            Launcher.ProcStarter dmStarter = launcher.launch();
            EnvVars envs = build.getEnvironment(listener);
            envs.put("CLOUDSHARE_API_ID", id);
            envs.put("CLOUDSHARE_API_KEY", key);
            dmStarter.envs(envs);
            dmStarter.cmds("docker-machine", "create", "-d", "cloudshare", dmName);
            dmStarter.stdout(listener);
            Proc dm = launcher.launch(dmStarter);
            try {

                int exitCode = dm.join();
                if (exitCode != 0) {
                    listener.error("Docker-Machine failed to activate.");
                    build.setResult(Result.FAILURE);
                }
            } catch (InterruptedException e) {
                listener.error("Docker-Machine not available");
            }
            listener.hyperlink("https://cloudshare.com/", "CloudShare Docker-Machine");
            listener.getLogger().append('\n');
        } catch (IOException | InterruptedException e) {
            listener.error(e.getMessage());
        }

    }

    // Overridden for better type safety.
    // If your plugin doesn't really define any property on Descriptor,
    // you don't have to do this.
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    /**
     * Descriptor for {@link HelloWorldBuilder}. Used as a singleton.
     * The class is marked as public so that it can be accessed from views.
     * <p>
     * <p>
     * See {@code src/main/resources/hudson/plugins/hello_world/HelloWorldBuilder/*.jelly}
     * for the actual HTML fragment for the configuration screen.
     */
    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        /**
         * To persist global configuration information,
         * simply store it in a field and call save().
         * <p>
         * <p>
         * If you don't want fields to be persisted, use {@code transient}.
         */
        private String apiKey;
        private String apiId;

        /**
         * In order to load the persisted global configuration, you have to
         * call load() in the constructor.
         */
        public DescriptorImpl() {
            load();
        }

        /**
         * Performs on-the-fly validation of the form field 'name'.
         *
         * @param value This parameter receives the value that the user has typed.
         * @return Indicates the outcome of the validation. This is sent to the browser.
         * <p>
         * Note that returning {@link FormValidation#error(String)} does not
         * prevent the form from being saved. It just means that a message
         * will be displayed to the user.
         */
        public FormValidation doCheckName(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error("Please set a name");
            if (value.length() < 4)
                return FormValidation.warning("Isn't the name too short?");
            return FormValidation.ok();
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types 
            return true;
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return "CloudShare Docker-Machine Builder";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            // To persist global configuration information,
            // set that to properties and call save().
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


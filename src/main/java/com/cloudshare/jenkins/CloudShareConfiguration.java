package com.cloudshare.jenkins;

import hudson.Extension;
import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;

import java.util.Collections;
import java.util.List;


@Extension
public class CloudShareConfiguration extends GlobalConfiguration {

    private String apiKey;
    private String apiId;

    public static CloudShareConfiguration get() {
        return GlobalConfiguration.all().get(CloudShareConfiguration.class);
    }

    public CloudShareConfiguration() {
        load();
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
        this.apiKey = json.getString("apiKey");
        this.apiId = json.getString("apiId");
        save();
        return true;
    }

    public final String getApiKey() {
        return apiKey;
    }

    public final void setApiKey(String key) {
        this.apiKey = key;
    }

    public final String getApiId() {
        return apiId;
    }

    public final void setApiId(String key) {
        this.apiId = key;
    }
}

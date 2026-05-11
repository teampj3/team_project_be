package com.teamproject.report.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pipeline")
public class PipelineProperties {

    private String runsRoot;
    private String runPath;
    private int timeoutSeconds = 15;

    public String getRunsRoot() {
        return runsRoot;
    }

    public void setRunsRoot(String runsRoot) {
        this.runsRoot = runsRoot;
    }

    public String getRunPath() {
        return runPath;
    }

    public void setRunPath(String runPath) {
        this.runPath = runPath;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }
}

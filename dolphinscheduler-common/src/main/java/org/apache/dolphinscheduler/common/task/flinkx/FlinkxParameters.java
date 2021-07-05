package org.apache.dolphinscheduler.common.task.flinkx;

import org.apache.dolphinscheduler.common.process.ResourceInfo;
import org.apache.dolphinscheduler.common.task.AbstractParameters;
import org.apache.dolphinscheduler.common.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class FlinkxParameters extends AbstractParameters {

    /**
     * deploy local  yarn-cluster yarn-local
     */
    private String deployMode;

    /**
     * if customConfig eq 1 ,then json is usable
     */
    private String json;

    private String jsonFilePath;

    private String queue;

    public String getDeployMode() {
        return deployMode;
    }

    public String getJsonFilePath() {
        return jsonFilePath;
    }

    public void setJsonFilePath(String jsonFilePath) {
        this.jsonFilePath = jsonFilePath;
    }

    public void setDeployMode(String deployMode) {
        this.deployMode = deployMode;
    }

    public String getJson() {
        return json;
    }

    public void setJson(String json) {
        this.json = json;
    }

    public String getQueue() {
        return queue;
    }

    public void setQueue(String queue) {
        this.queue = queue;
    }

    @Override
    public boolean checkParameters() {
        return StringUtils.isNotEmpty(deployMode) && StringUtils.isNotEmpty(json);
    }

    @Override
    public List<ResourceInfo> getResourceFilesList() {
        return new ArrayList<>();
    }
}

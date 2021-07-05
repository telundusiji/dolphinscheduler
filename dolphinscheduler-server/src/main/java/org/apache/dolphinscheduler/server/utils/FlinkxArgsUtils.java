package org.apache.dolphinscheduler.server.utils;

import org.apache.dolphinscheduler.common.task.flinkx.FlinkxParameters;

import java.util.ArrayList;
import java.util.List;

public class FlinkxArgsUtils {
    public static List<String> buildArgs(FlinkxParameters parameters) {
        List<String> args = new ArrayList<>();
        args.add("-job " + parameters.getJsonFilePath());
        args.add("-pluginRoot ${FLINKX_HOME}/syncplugins");
        if ("Local".equalsIgnoreCase(parameters.getDeployMode())) {
            args.add("-mode local");
            args.add("-flinkconf ${FLINK_CONF_DIR}");
        } else if ("Standalone".equalsIgnoreCase(parameters.getDeployMode())) {
            args.add("-mode standalone");
            args.add("-flinkconf ${FLINK_CONF_DIR}");
        } else if ("Yarn".equalsIgnoreCase(parameters.getDeployMode())) {
            args.add("-mode yarn");
            args.add("-flinkconf ${FLINK_CONF_DIR}");
            args.add("-yarnconf ${HADOOP_CONF_DIR}");
            args.add("-queue " + parameters.getQueue());
        } else if ("YarnPer".equalsIgnoreCase(parameters.getDeployMode())) {
            args.add("-mode yarnPer");
            args.add("-yarnconf ${HADOOP_CONF_DIR}");
            args.add("-flinkLibJar ${FLINK_HOME}/lib");
            args.add("-queue " + parameters.getQueue());
        } else {
            throw new RuntimeException("Flinkx deploy mode is invalid[" + parameters.getDeployMode() + "]");
        }
        return args;
    }
}

package org.apache.dolphinscheduler.server.worker.task.flinkx;

import org.apache.commons.io.FileUtils;
import org.apache.dolphinscheduler.common.enums.CommandType;
import org.apache.dolphinscheduler.common.process.Property;
import org.apache.dolphinscheduler.common.task.AbstractParameters;
import org.apache.dolphinscheduler.common.task.flinkx.FlinkxParameters;
import org.apache.dolphinscheduler.common.utils.JSONUtils;
import org.apache.dolphinscheduler.common.utils.ParameterUtils;
import org.apache.dolphinscheduler.server.entity.TaskExecutionContext;
import org.apache.dolphinscheduler.server.utils.FlinkxArgsUtils;
import org.apache.dolphinscheduler.server.utils.ParamUtils;
import org.apache.dolphinscheduler.server.worker.task.AbstractYarnTask;
import org.slf4j.Logger;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FlinkxTask extends AbstractYarnTask {

    private static final String FLINKX_COMMAND = "${FLINKX_HOME}/bin/flinkx";

    private FlinkxParameters parameters;

    private TaskExecutionContext context;

    /**
     * Abstract Yarn Task
     *
     * @param taskExecutionContext taskExecutionContext
     * @param logger               logger
     */
    public FlinkxTask(TaskExecutionContext taskExecutionContext, Logger logger) {
        super(taskExecutionContext, logger);
        this.context = taskExecutionContext;
    }

    @Override
    public void init() throws Exception {
        logger.info("Flinkx task parameter {}", context.getTaskParams());
        parameters = JSONUtils.parseObject(context.getTaskParams(), FlinkxParameters.class);
        if (parameters == null || !parameters.checkParameters()) {
            throw new RuntimeException("Flinkx task parameter is null or invalid");
        }
        parameters.setQueue(context.getQueue());
    }

    @Override
    public AbstractParameters getParameters() {
        return parameters;
    }

    @Override
    protected String buildCommand() throws Exception {
        // set the name of the current thread
        String threadLoggerInfoName = String.format("FlinkxTaskLogInfo-%s", context.getTaskAppId());
        Thread.currentThread().setName(threadLoggerInfoName);

        // combining local and global parameters
        Map<String, Property> paramsMap = ParamUtils.convert(ParamUtils.getUserDefParamsMap(context.getDefinedParams()),
                context.getDefinedParams(),
                parameters.getLocalParametersMap(),
                CommandType.of(context.getCmdTypeIfComplement()),
                context.getScheduleTime());
        String jsonFile = buildFlinkxJsonFile(paramsMap);

        List<String> args = new ArrayList<>();

        args.add(FLINKX_COMMAND);

        parameters.setJsonFilePath(jsonFile);
        args.addAll(FlinkxArgsUtils.buildArgs(parameters));

        String command = ParameterUtils
                .convertParameterPlaceholders(String.join(" ", args), context.getDefinedParams());

        logger.info("Flinkx task command : {}", command);

        return command;
    }

    @Override
    protected void setMainJarName() {

    }

    private String buildFlinkxJsonFile(Map<String, Property> paramsMap)
            throws Exception {
        // generate json
        String fileName = String.format("%s/%s_flinkx_job.json",
                context.getExecutePath(),
                context.getTaskAppId());

        Path path = new File(fileName).toPath();
        if (Files.exists(path)) {
            return fileName;
        }

        logger.info("get json {}",parameters.getJson());

        String json = parameters.getJson().replaceAll("\\r\\n", "\n");
        json = ParameterUtils.replaceScheduleTime(json, context.getScheduleTime());
        logger.debug("replace schedule time json {}",json);
        // replace placeholder
        json = ParameterUtils.convertParameterPlaceholders(json, ParamUtils.convert(paramsMap));



        logger.info("Flinkx job json {}", json);

        // create flinkx json file
        FileUtils.writeStringToFile(new File(fileName), json, StandardCharsets.UTF_8);
        return fileName;
    }
}

package org.apache.dolphinscheduler.server.worker.task.java;

import org.apache.dolphinscheduler.common.Constants;
import org.apache.dolphinscheduler.common.enums.CommandType;
import org.apache.dolphinscheduler.common.process.Property;
import org.apache.dolphinscheduler.common.process.ResourceInfo;
import org.apache.dolphinscheduler.common.task.AbstractParameters;
import org.apache.dolphinscheduler.common.task.java.JavaParameters;
import org.apache.dolphinscheduler.common.utils.JSONUtils;
import org.apache.dolphinscheduler.common.utils.OSUtils;
import org.apache.dolphinscheduler.common.utils.ParameterUtils;
import org.apache.dolphinscheduler.dao.entity.Resource;
import org.apache.dolphinscheduler.server.entity.TaskExecutionContext;
import org.apache.dolphinscheduler.server.utils.ParamUtils;
import org.apache.dolphinscheduler.server.worker.task.AbstractTask;
import org.apache.dolphinscheduler.server.worker.task.CommandExecuteResult;
import org.apache.dolphinscheduler.server.worker.task.ShellCommandExecutor;
import org.apache.dolphinscheduler.service.bean.SpringApplicationContext;
import org.apache.dolphinscheduler.service.process.ProcessService;
import org.slf4j.Logger;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class JavaTask extends AbstractTask {

    private JavaParameters parameters;

    private TaskExecutionContext context;

    private ShellCommandExecutor shellCommandExecutor;

    private static final String JAVA = "$JAVA_HOME/bin/java";

    private ProcessService processService;

    /**
     * constructor
     *
     * @param taskExecutionContext taskExecutionContext
     * @param logger               logger
     */
    public JavaTask(TaskExecutionContext taskExecutionContext, Logger logger) {
        super(taskExecutionContext, logger);
        this.context = taskExecutionContext;
        this.processService = SpringApplicationContext.getBean(ProcessService.class);
        this.shellCommandExecutor = new ShellCommandExecutor(this::logHandle,
                taskExecutionContext, logger);
    }

    @Override
    public void init() throws Exception {
        logger.info("Java task params {}", context.getTaskParams());

        this.parameters = JSONUtils.parseObject(context.getTaskParams(), JavaParameters.class);

        // check parameters
        if (parameters == null || !parameters.checkParameters()) {
            throw new RuntimeException("Java task params is not valid");
        }

        // replace placeholder
        Map<String, Property> paramsMap = ParamUtils.convert(ParamUtils.getUserDefParamsMap(context.getDefinedParams()),
                context.getDefinedParams(),
                parameters.getLocalParametersMap(),
                CommandType.of(context.getCmdTypeIfComplement()),
                context.getScheduleTime());

        if (paramsMap != null) {
            String args = ParameterUtils.convertParameterPlaceholders(parameters.getMainArgs(), ParamUtils.convert(paramsMap));
            parameters.setMainArgs(args);
            String jvmArgs = ParameterUtils.convertParameterPlaceholders(parameters.getJvmArgs(), ParamUtils.convert(paramsMap));
            parameters.setJvmArgs(jvmArgs);
        }
    }

    @Override
    public void cancelApplication(boolean status) throws Exception {
        shellCommandExecutor.cancelApplication();
        super.cancelApplication(status);
    }

    @Override
    public void handle() throws Exception {
        try {
            // set the name of the current thread
            String threadLoggerInfoName = String.format("TaskLogInfo-%s", context.getTaskAppId());
            Thread.currentThread().setName(threadLoggerInfoName);

            String shellCommandFilePath = buildShellCommandFile();
            CommandExecuteResult commandExecuteResult = shellCommandExecutor.run(shellCommandFilePath);

            setExitStatusCode(commandExecuteResult.getExitStatusCode());
            setAppIds(commandExecuteResult.getAppIds());
            setProcessId(commandExecuteResult.getProcessId());
        } catch (Exception e) {
            logger.error("Java task failure", e);
            setExitStatusCode(Constants.EXIT_CODE_FAILURE);
            throw e;
        }
    }

    /**
     * create command
     *
     * @return shell command file name
     * @throws Exception if error throws Exception
     */
    private String buildShellCommandFile()
            throws Exception {
        // generate scripts
        String fileName = String.format("%s/%s_node.%s",
                context.getExecutePath(),
                context.getTaskAppId(),
                OSUtils.isWindows() ? "bat" : "sh");

        Path path = new File(fileName).toPath();

        if (Files.exists(path)) {
            return fileName;
        }

        // java -classpath <jar> [mainClass] [jvmArgs] mainArgs...
        List<String> args = new ArrayList<>();
        args.add(JAVA);
        args.add(parameters.getJvmArgs());
        args.add("-classpath");
        args.add(getJar());
        args.add(parameters.getMainClass());
        args.add(parameters.getMainArgs());

        // replace placeholder
        String command = ParameterUtils.convertParameterPlaceholders(String.join(" ", args),
                context.getDefinedParams());

        logger.info("Java task script : {}", command);

        // create shell command file
        Set<PosixFilePermission> perms = PosixFilePermissions.fromString(Constants.RWXR_XR_X);
        FileAttribute<Set<PosixFilePermission>> attr = PosixFilePermissions.asFileAttribute(perms);

        if (OSUtils.isWindows()) {
            Files.createFile(path);
        } else {
            Files.createFile(path, attr);
        }

        Files.write(path, command.getBytes(), StandardOpenOption.APPEND);

        return fileName;
    }

    private String getJar() {
        List<ResourceInfo> resourceFilesList = parameters.getResourceFilesList();
        return resourceFilesList.stream()
                .filter(r -> r != null)
                .map(r -> {
                    int resourceId = r.getId();
                    String resourceName;
                    if (resourceId == 0) {
                        resourceName = r.getRes();
                    } else {
                        Resource resource = processService.getResourceById(r.getId());
                        if (resource == null) {
                            logger.error("resource id: {} not exist", resourceId);
                            throw new RuntimeException(String.format("resource id: %d not exist", resourceId));
                        }
                        resourceName = resource.getFullName().replaceFirst("/", "");
                    }
                    return resourceName;
                }).collect(Collectors.joining(OSUtils.isWindows() ? ";" : ":"));
    }

    @Override
    public AbstractParameters getParameters() {
        return parameters;
    }
}

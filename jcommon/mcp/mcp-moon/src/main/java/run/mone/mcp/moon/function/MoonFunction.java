package run.mone.mcp.moon.function;

import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import run.mone.hive.mcp.spec.McpSchema;
import run.mone.moon.api.bo.common.Result;
import run.mone.moon.api.bo.task.DubboParam;
import run.mone.moon.api.bo.task.FaasParam;
import run.mone.moon.api.bo.task.HttpParam;
import run.mone.moon.api.bo.task.TaskReq;
import run.mone.moon.api.bo.user.MoonMoneTpcContext;
import run.mone.moon.api.service.MoonTaskDubboService;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Moon系统创建任务执行器
 */
@Data
@Slf4j
@Component
public class MoonFunction implements Function<Map<String, Object>, McpSchema.CallToolResult> {


    private String name = "create_task_executor";

    private String desc = "Help user to create task";

    private String taskToolSchema = """
            {
                "type": "object",
                "properties": {
                    "tenant": {
                        "type": "integer",
                        "enum": [1, 2, 3, 4, 5],
                        "description": "租户: 中国区: 1, 国际： 2, 新加坡： 3， 欧洲： 4"
                    },
                    "name": {
                        "type": "string",
                        "description": "任务名"
                    },
                    "description": {
                        "type": "string",
                        "description": "任务描述"
                    },
                    "projectID": {
                        "type": "integer",
                        "description": "关联项目id"
                    },
                    "type": {
                        "type": "string",
                        "enum": ["httpTask", "dubboTask", "faasTask"],
                        "description": "任务类型"
                    },
                    "execMode": {
                        "type": "string",
                        "enum": ["single", "broadcast", "slice"],
                        "default": "broadcast",
                        "description": "执行模式"
                    },
                    "priority": {
                        "type": "string",
                        "description": "优先级"
                    },
                    "execParam": {
                        "type": "string",
                        "description": "执行参数"
                    },
                    "retryWait": {
                        "type": "integer",
                        "default": 1,
                        "description": "失败重试时间间隔，单位s"
                    },
                    "concurrency": {
                        "type": "integer",
                        "default": 1,
                        "description": "任务执行并发量"
                    },
                    "machine": {
                        "type": "string",
                        "description": "指定机器运行"
                    },
                    "scheduleMode": {
                        "type": "string",
                        "enum": ["cron", "fix rate", "second delay", "single shot", "api call"],
                        "default": "cron",
                        "description": "调度模式"
                    },
                    "scheduleParam": {
                        "type": "string",
                        "description": "scheduleMode对应的参数json，contab表达式，执行时间，间隔等参数"
                    },
                    "startTime": {
                        "type": "integer",
                        "description": "调度开始时间"
                    },
                    "concurrencyStrategy": {
                        "type": "string",
                        "enum": ["parallel", "cancel_new", "stop_old", "queue"],
                        "default": "cancel_new",
                        "description": "并行策略：并行，新任务取消，停止老任务，队列"
                    },
                    "alertTimeout": {
                        "type": "boolean",
                        "default": true,
                        "description": "是否开启超时报警"
                    },
                    "alertTimeoutLevel": {
                        "type": "string",
                        "default": "P1",
                        "description": "超时报警级别"
                    },
                    "timeout": {
                        "type": "integer",
                        "default": 7200,
                        "description": "超时报警的超时时间(s)"
                    },
                    "timeoutHalt": {
                        "type": "boolean",
                        "default": true,
                        "description": "超时终止"
                    },
                    "alertSuccess": {
                        "type": "boolean",
                        "default": false,
                        "description": "成功通知"
                    },
                    "alertSuccessLevel": {
                        "type": "string",
                        "default": "P2",
                        "description": "成功通知级别"
                    },
                    "alertFail": {
                        "type": "boolean",
                        "default": true,
                        "description": "失败报警"
                    },
                    "alertFailLevel": {
                        "type": "string",
                        "default": "P1",
                        "description": "失败报警级别"
                    },
                    "alertStop": {
                        "type": "boolean",
                        "default": true,
                        "description": "停止报警"
                    },
                    "alertStopLevel": {
                        "type": "string",
                        "default": "P0",
                        "description": "停止报警级别"
                    },
                    "alertSkip": {
                        "type": "boolean",
                        "default": false,
                        "description": "任务跳过报警"
                    },
                    "alertSkipLevel": {
                        "type": "string",
                        "default": "P0",
                        "description": "任务跳过报警级别"
                    },
                    "maxRetry": {
                        "type": "integer",
                        "default": 0,
                        "description": "最大失败重试次数"
                    },
                    "alertNoMachine": {
                        "type": "boolean",
                        "default": true,
                        "description": "没机器时报警"
                    },
                    "alertNoMachineLevel": {
                        "type": "string",
                        "default": "P0",
                        "description": "没机器时报警级别"
                    },
                    "alertConfig": {
                        "type": "string",
                        "description": "通知设置"
                    },
                    "historyKeep": {
                        "type": "integer",
                        "default": 7,
                        "description": "执行历史保存天数"
                    },
                    "faasParam": {
                        "type": "object",
                        "description": "faas相关配置"
                    },
                    "httpParam": {
                        "type": "object",
                        "description": "http相关配置"
                    },
                    "dubboParam": {
                        "type": "object",
                        "description": "dubbo相关配置"
                    }
                },
                "required": ["tenant", "name", "description", "projectID", "type", "historyKeep"]
            }
            """;

    private MoonTaskDubboService moonTaskDubboService;

    public MoonFunction(MoonTaskDubboService moonTaskDubboService) {
        this.moonTaskDubboService = moonTaskDubboService;
    }

    @SneakyThrows
    @Override
    public McpSchema.CallToolResult apply(Map<String, Object> args) {
        try {
            // 1. 参数验证和转换
            if (args == null || args.isEmpty()) {
                throw new IllegalArgumentException("Task parameters are required");
            }

            // 必填参数校验
            if (args.get("tenant") == null) {
                throw new IllegalArgumentException("tenant is required");
            }
            if (args.get("name") == null || ((String) args.get("name")).trim().isEmpty()) {
                throw new IllegalArgumentException("task name is required and cannot be empty");
            }
            if (args.get("projectID") == null) {
                throw new IllegalArgumentException("task projectID is required");
            }
            if (args.get("type") == null || ((String) args.get("type")).trim().isEmpty()) {
                throw new IllegalArgumentException("type is required and cannot be empty");
            }
            String type = (String) args.get("type");
            if (!type.equals("httpTask") && !type.equals("dubboTask") && !type.equals("faasTask")) {
                throw new IllegalArgumentException("type must be one of: httpTask, dubboTask, faasTask");
            }

            // 2. 构建任务参数
            TaskReq taskParam = new TaskReq();
            MoonMoneTpcContext context = new MoonMoneTpcContext();
            context.setTenant((String) args.get("tenant"));

            // 设置必填参数
            taskParam.setName((String) args.get("name"));
            taskParam.setProjectID(((Number) args.get("projectID")).longValue());
            taskParam.setType(type);

            // 设置默认值和可选参数
            taskParam.setDescription(args.containsKey("description") ? 
                (String) args.get("description") : "Task created by MCP");
            taskParam.setExecMode(args.containsKey("execMode") ? 
                (String) args.get("execMode") : "broadcast");
            taskParam.setPriority(args.containsKey("priority") ? 
                (String) args.get("priority") : "P2");
            taskParam.setExecParam(args.containsKey("execParam") ? 
                (String) args.get("execParam") : "{}");
            taskParam.setRetryWait(args.containsKey("retryWait") ? 
                ((Number) args.get("retryWait")).intValue() : 1);
            taskParam.setConcurrency(args.containsKey("concurrency") ? 
                ((Number) args.get("concurrency")).intValue() : 1);
            taskParam.setMachine(args.containsKey("machine") ? 
                (String) args.get("machine") : "");
            taskParam.setScheduleMode(args.containsKey("scheduleMode") ? 
                (String) args.get("scheduleMode") : "cron");
            taskParam.setScheduleParam(args.containsKey("scheduleParam") ? 
                (String) args.get("scheduleParam") : "0 0 * * * ?"); // 默认每小时执行
            taskParam.setStartTime(args.containsKey("startTime") ? 
                ((Number) args.get("startTime")).longValue() : System.currentTimeMillis());
            taskParam.setConcurrencyStrategy(args.containsKey("concurrencyStrategy") ? 
                (String) args.get("concurrencyStrategy") : "parallel");

            // 报警相关默认参数
            taskParam.setAlertTimeout(args.containsKey("alertTimeout") ? 
                (Boolean) args.get("alertTimeout") : false);
            taskParam.setAlertTimeoutLevel(args.containsKey("alertTimeoutLevel") ? 
                (String) args.get("alertTimeoutLevel") : "P1");
            taskParam.setTimeout(args.containsKey("timeout") ? 
                ((Number) args.get("timeout")).longValue() : 7200L);
            taskParam.setTimeoutHalt(args.containsKey("timeoutHalt") ? 
                (Boolean) args.get("timeoutHalt") : false);
            taskParam.setAlertSuccess(args.containsKey("alertSuccess") ? 
                (Boolean) args.get("alertSuccess") : false);
            taskParam.setAlertSuccessLevel(args.containsKey("alertSuccessLevel") ? 
                (String) args.get("alertSuccessLevel") : "P2");
            taskParam.setAlertFail(args.containsKey("alertFail") ? 
                (Boolean) args.get("alertFail") : false);
            taskParam.setAlertFailLevel(args.containsKey("alertFailLevel") ? 
                (String) args.get("alertFailLevel") : "P1");
            taskParam.setAlertStop(args.containsKey("alertStop") ? 
                (Boolean) args.get("alertStop") : false);
            taskParam.setAlertStopLevel(args.containsKey("alertStopLevel") ? 
                (String) args.get("alertStopLevel") : "P0");
            taskParam.setAlertSkip(args.containsKey("alertSkip") ? 
                (Boolean) args.get("alertSkip") : true);
            taskParam.setAlertSkipLevel(args.containsKey("alertSkipLevel") ? 
                (String) args.get("alertSkipLevel") : "P0");
            taskParam.setMaxRetry(args.containsKey("maxRetry") ? 
                ((Number) args.get("maxRetry")).intValue() : 0);
            taskParam.setAlertNoMachine(args.containsKey("alertNoMachine") ? 
                (Boolean) args.get("alertNoMachine") : false);
            taskParam.setAlertNoMachineLevel(args.containsKey("alertNoMachineLevel") ? 
                (String) args.get("alertNoMachineLevel") : "P0");
            taskParam.setAlertConfig(args.containsKey("alertConfig") ? 
                (String) args.get("alertConfig") : "{}");
            taskParam.setHistoryKeep(args.containsKey("historyKeep") ? 
                ((Number) args.get("historyKeep")).intValue() : 7);

            // 特殊参数处理
            if (args.containsKey("faasParam")) {
                taskParam.setFaasParam(convertToFaasParam(args.get("faasParam")));
            }
            if (args.containsKey("httpParam")) {
                taskParam.setHttpParam(convertToHttpParam(args.get("httpParam")));
            }
            if (args.containsKey("dubboParam")) {
                taskParam.setDubboParam(convertToDubboParam(args.get("dubboParam")));
            }

            // 3. 调用服务创建任务
            Result<Long> result = moonTaskDubboService.create(context, taskParam);

            // 4. 处理返回结果
            if (result.getCode() == 0) {
                return new McpSchema.CallToolResult(
                        List.of(new McpSchema.TextContent("Task created successfully. Task ID: " + result.getData())),
                        false
                );
            } else {
                throw new RuntimeException("Failed to create task: " + result.getMessage());
            }

        } catch (Exception ex) {
            log.error("Failed to create task", ex);
            throw new RuntimeException("Failed to create task: " + ex.getMessage());
        }
    }

    // 辅助方法：转换特殊参数
    private FaasParam convertToFaasParam(Object param) {
        if (param instanceof Map) {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.convertValue(param, FaasParam.class);
        }
        return null;
    }

    private HttpParam convertToHttpParam(Object param) {
        if (param instanceof Map) {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.convertValue(param, HttpParam.class);
        }
        return null;
    }

    private DubboParam convertToDubboParam(Object param) {
        if (param instanceof Map) {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.convertValue(param, DubboParam.class);
        }
        return null;
    }

}
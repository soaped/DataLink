package com.datalink.flink.job;

import com.datalink.flink.cluster.FlinkCluster;
import com.datalink.flink.constant.FlinkConstant;
import com.datalink.flink.constant.FlinkFunctionConstant;
import com.datalink.flink.constant.FlinkJobConstant;
import com.datalink.flink.constant.FlinkSQLConstant;
import com.datalink.flink.executor.EnvironmentSetting;
import com.datalink.flink.executor.Executor;
import com.datalink.flink.executor.ExecutorSetting;
import com.datalink.flink.result.IResult;
import com.datalink.flink.result.InsertResult;
import com.datalink.flink.result.JobSubmitResult;
import com.datalink.flink.result.ResultBuilder;
import com.datalink.flink.result.RunResult;
import com.datalink.flink.result.SubmitResult;
import com.datalink.flink.session.ExecutorEntity;
import com.datalink.flink.session.SessionPool;
import com.datalink.flink.trans.Operations;
import com.datalink.flink.ud.udf.GetKey;
import com.datalink.flink.ud.udtaf.RowsToMap;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.api.common.JobID;
import org.apache.flink.table.api.TableResult;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JobManager
 *
 * @author qiwenkai
 * @since 2021/5/25 15:27
 **/
public class JobManager {

    private String flinkHost;
    private Integer port;
    private String sessionId;
    private Integer maxRowNum = 100;

    public JobManager(String flinkHost, Integer port) {
        this.flinkHost = flinkHost;
        this.port = port;
    }

    public JobManager(String flinkHost, Integer port, String sessionId, Integer maxRowNum) {
        this.flinkHost = flinkHost;
        this.sessionId = sessionId;
        this.maxRowNum = maxRowNum;
        this.port = port;
    }

    public RunResult execute(String statement) {
        RunResult runResult = new RunResult(sessionId, statement, flinkHost);
        Executor executor = null;
        ExecutorEntity executorEntity = SessionPool.get(sessionId);
        if (executorEntity != null) {
            executor = executorEntity.getExecutor();
        } else {
            executor = Executor.build(new EnvironmentSetting(flinkHost, FlinkConstant.PORT), new ExecutorSetting(Executor.REMOTE));
            SessionPool.push(new ExecutorEntity(sessionId, executor));
        }
        String[] Statements = statement.split(";");
        int currentIndex = 0;
        //当前只支持对 show select的操作的结果的数据查询  后期需要可添加
        try {
            for (String item : Statements) {
                currentIndex++;
                if (item.trim().isEmpty()) {
                    continue;
                }
                String operationType = Operations.getOperationType(item);
                long start = System.currentTimeMillis();
                TableResult tableResult = executor.executeSql(item);
                long finish = System.currentTimeMillis();
                long timeElapsed = finish - start;
                IResult result = ResultBuilder.build(operationType, maxRowNum, "", false).getResult(tableResult);
                runResult.setResult(result);
                runResult.setTime(timeElapsed);
                runResult.setFinishDate(LocalDate.now());
            }
        } catch (Exception e) {
            e.printStackTrace();
            StackTraceElement[] trace = e.getStackTrace();
            StringBuffer resMsg = new StringBuffer("");
            for (StackTraceElement s : trace) {
                resMsg.append(" </br> " + s + "  ");
            }
            runResult.setError(LocalDate.now().toString() + ":" + "运行第" + currentIndex + "行sql时出现异常:" + e.getMessage() + "</br> >>>堆栈信息<<<" + resMsg.toString());
            return runResult;
        }
        return runResult;
    }

    public SubmitResult submit(List<String> sqlList, ExecutorSetting executerSetting) {
        SubmitResult result = new SubmitResult(sessionId,sqlList,flinkHost);
        Map<String, String> map = new HashMap<>();
        int currentIndex = 0;
        try {
            if (sqlList != null && sqlList.size() > 0) {
                Executor executor = Executor.build(new EnvironmentSetting(flinkHost, port), executerSetting);
                for (String sqlText : sqlList) {
                    currentIndex++;
                    String operationType = Operations.getOperationType(sqlText);
                    if (operationType.equalsIgnoreCase(FlinkSQLConstant.INSERT)) {

                        long start = System.currentTimeMillis();
                        TableResult tableResult = executor.executeSql(sqlText);
                        long finish = System.currentTimeMillis();
                        long timeElapsed = finish - start;
                        JobID jobID = tableResult.getJobClient().get().getJobID();
                        result.setSuccess(true);
                        result.setTime(timeElapsed);
                        result.setFinishDate(LocalDate.now());
                        InsertResult insertResult = new InsertResult(sqlText,(jobID == null ? "" : jobID.toHexString()),true,timeElapsed,LocalDate.now());
                        result.setResult(insertResult);
                    } else {
                        executor.executeSql(sqlText);
                    }
                }
            } else {
                result.setSuccess(false);
                result.setMsg(LocalDate.now().toString()+":执行sql语句为空。");
                return result;
            }
        } catch (Exception e) {
            e.printStackTrace();
            StackTraceElement[] trace = e.getStackTrace();
            StringBuilder resMsg = new StringBuilder();
            for (StackTraceElement s : trace) {
                resMsg.append(" </br> " + s + "  ");
            }
            result.setError(LocalDate.now().toString() + ":" + "运行第" + currentIndex + "行sql时出现异常:" + e.getMessage() + "</br> >>>堆栈信息<<<" + resMsg.toString());
            return result;

        }
        result.setSuccess(true);
        result.setMsg(LocalDate.now().toString() + ":任务提交成功！");
        return result;
    }
}

package com.timevale.drc.pd.service;

import com.ctrip.framework.apollo.ConfigService;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.timevale.drc.base.Task;
import com.timevale.drc.base.TaskStateEnum;
import com.timevale.drc.base.TaskTypeEnum;
import com.timevale.drc.base.Worker;
import com.timevale.drc.base.alarm.AlarmUtil;
import com.timevale.drc.base.dao.*;
import com.timevale.drc.base.distributed.TaskRegister;
import com.timevale.drc.base.eventbus.DrcThreadPool;
import com.timevale.drc.base.model.*;
import com.timevale.drc.base.model.bo.DrcSubTaskIncrBO;
import com.timevale.drc.base.model.ext.DrcSubTaskIncrExt;
import com.timevale.drc.base.mysql.MigrateTableStructureUtil;
import com.timevale.drc.base.redis.DrcLock;
import com.timevale.drc.base.redis.DrcLockFactory;
import com.timevale.drc.base.redis.DrcRedisson;
import com.timevale.drc.base.rocketmq.admin.MQAdminService;
import com.timevale.drc.base.rpc.RpcResult;
import com.timevale.drc.base.serialize.GenericJackson2JsonSerializer;
import com.timevale.drc.base.serialize.JackSonUtil;
import com.timevale.drc.base.sinkConfig.*;
import com.timevale.drc.base.util.*;
import com.timevale.drc.pd.service.cache.FullSliceDetailMapperCache;
import com.timevale.drc.pd.service.cache.IncrMapperCache;
import com.timevale.drc.pd.service.exp.NotFoundRouteException;
import com.timevale.drc.pd.service.full.resolve.SelectFullTaskScheduler;
import com.timevale.drc.pd.service.full.resolve.SplitDispatch;
import com.timevale.drc.pd.service.full.resolve.SplitSliceTaskFactory;
import com.timevale.drc.pd.service.param.OpenApiStartParams;
import com.timevale.drc.pd.service.param.QpsConfigParam;
import com.timevale.drc.pd.service.stat.ClusterSummary;
import com.timevale.drc.pd.service.user.UserContext;
import com.timevale.drc.pd.service.vo.*;
import com.timevale.drc.pd.service.vo.universal.MyPageQueryResult;
import com.timevale.drc.pd.service.worker.WorkerManager;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.skywalking.apm.toolkit.trace.TraceCrossThread;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.sql.Date;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.timevale.drc.base.TaskStateEnum.RUNNING;
import static com.timevale.drc.base.model.DrcSubTaskFullConfig.*;

/**
 * @author gwk_2
 * @date 2021/2/26 11:50
 */
@Service
@Slf4j
public class PDTaskService {

    private static final Set<Integer> type_set = Sets.newHashSet(TaskTypeEnum.MYSQL_MIX_TASK.code, TaskTypeEnum.MYSQL_DATABASE_MIX_TASK.code);
    private static final Set<Integer> split_set = Sets.newHashSet(TaskStateEnum.SPLIT_OVER.code, TaskStateEnum.SPLIT_ING.code);

    final static String ClusterQPS = "ClusterQPS";
    final static int WAN = 10000;

    private final JdbcTemplateManager jdbcTemplateManager = new JdbcTemplateManager();
    private final GenericJackson2JsonSerializer serializer = new GenericJackson2JsonSerializer();

    @Value("${env:test}")
    private String env;
    @Value("${showStateEnv:test}")
    private String showStateEnv;
    @Value("${default.range.size.inWan:500}")
    private Integer defaultRangeSize;
    @Value("${eureka.instance.appGroupName:DEFAULT}")
    private String appGroupName;

    @Autowired
    private DrcLockFactory lockFactory;
    @Autowired
    private DrcTaskMapper drcTaskMapper;
    @Autowired
    private DrcDbConfigMapper drcDbConfigMapper;
    @Autowired
    private DrcSubTaskFullConfigMapper fullConfigMapper;
    @Autowired
    private DrcSubTaskIncrMapper incrMapper;
    @Autowired
    private IncrMapperCache incrMapperCache;
    @Autowired
    private DrcSubTaskFullSliceDetailMapper fullSliceDetailMapper;
    @Autowired
    private FullSliceDetailMapperCache fullSliceDetailMapperCache;
    @Autowired
    private SplitSliceTaskFactory splitSliceTaskFactory;
    @Autowired
    private PDServer pdServer;
    @Autowired
    private TaskDbOperator taskDbOperator;
    @Autowired
    private DrcZkClient drcZkClient;
    @Autowired
    private AliYunKafkaOpenApiClient aliYunKafkaOpenApiClient;
    @Autowired
    private MQAdminService mqAdminService;
    @Autowired
    private QpsLogMapper qpsLogMapper;
    @Autowired
    private DrcSubTaskSchemaLogMapper drcSubTaskSchemaLogMapper;
    @Resource
    private TaskRegister taskRegister;

    private SplitDispatch splitDispatch;
    private SelectFullTaskScheduler selectFullTaskScheduler;
    private volatile boolean shutdown;
    ScheduledExecutorService service = DrcThreadPool.newScheduledThreadPool(3, getClass().getCanonicalName());

    @PostConstruct
    public void init() {
        shutdown = false;

        selectFullTaskScheduler = new SelectFullTaskScheduler(fullSliceDetailMapper, pdServer.getWorkerManager(), pdServer, taskDbOperator, lockFactory, drcTaskMapper);
        selectFullTaskScheduler.start();

        splitDispatch = new SplitDispatch(fullConfigMapper, splitSliceTaskFactory, lockFactory, selectFullTaskScheduler);
        splitDispatch.start();

        // 1 ??????????????????.
        service.scheduleAtFixedRate(new SaveQPSDataTask(), 1, 1, TimeUnit.SECONDS);
        service.scheduleWithFixedDelay(new DeleteQPSDataTask(), 0, 1, TimeUnit.HOURS);
        service.scheduleWithFixedDelay(new ReloadFailSplitTask(), 10, 10, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void shutdown() {
        shutdown = true;
        splitDispatch.stop();
        selectFullTaskScheduler.stop();
        service.shutdown();
    }

    public List<QpsLog> getQpsList(Long start, Long end) {
        List<QpsLog> qpsLogs = qpsLogMapper.selectByTimeRange(ClusterQPS, start, end);
        return qpsLogs;
    }


    public ClusterSummary clusterSummary() {
        WorkerClusterState workerClusterState = pdServer.clusterState();
        int totalQPS = pdServer.getTotalQPS();
        int size = pdServer.getWorkerManager().getWorkerList().size();
        ClusterSummary clusterSummary = new ClusterSummary();
        clusterSummary.setClusterTotalQPS(totalQPS);
        clusterSummary.setWorkerClusterState(workerClusterState.getDesc());
        clusterSummary.setWorkerCount(size);
        return clusterSummary;
    }

    public TaskSummaryVO runningTaskCount() {
        TaskSummaryVO v = new TaskSummaryVO();
        final List<DrcSubTaskIncr> incrList = incrMapper.selectRunningIncrTask();
        int incrAll = incrList.size();
        int fullAll = fullSliceDetailMapper.selectRunning();
        v.setAll(incrAll + fullAll);

        List<Task> taskList = pdServer.getTaskManager().getTaskList();


        v.setCount(taskList.size());

        int fullCount = 0;
        int incrCount = 0;
        for (Task task : taskList) {
            if (task == null) {
                continue;
            }
            boolean incr = task.getName().endsWith("Incr");
            if (incr) {
                ++incrCount;
            } else {
                ++fullCount;
            }
        }
        v.setDbIncrCount(incrAll);
        v.setFullCount(fullCount);
        v.setIncrCount(incrCount);
        return v;
    }



    public List<SimpleWorkerVO> workerList() {

        List<Worker> aliveWorker = pdServer.getWorkerManager().getWorkerList();
        List<SimpleWorkerVO> result = new ArrayList<>();

        for (Worker w : aliveWorker) {
            String ipPort = w.getEndpoint().getTcpUrl();
            final List<Task> listFromWorker = taskRegister.getListFromWorker(w);
            List<String> taskStringListByWorker = listFromWorker.stream().map(Task::getName).collect(Collectors.toList());
            List<SimpleWorkerVO.SimpleTask> objects = Lists.newArrayList();
            taskStringListByWorker.forEach(taskName -> objects.add(new SimpleWorkerVO.SimpleTask(taskName, pdServer.getQPS(taskName))));
            SimpleWorkerVO simpleWorkerVO = new SimpleWorkerVO(objects, ipPort);
            int workerQPS = 0;
            for (String task : taskStringListByWorker) {
                int qps = pdServer.getQPS(task);
                workerQPS += qps;
            }
            simpleWorkerVO.setWorkerQPS(workerQPS);
            result.add(simpleWorkerVO);
        }

        return result;
    }


    public MyPageQueryResult<ParentTaskVO> parentList(int pageNum, int pageSize, String taskName) {
        List<ParentTaskVO> tmpResult = new ArrayList<>();

        Page<ParentTaskVO> page = PageHelper.startPage(pageNum, pageSize);

        List<DrcTask> drcTasks;
        if (StringUtils.isBlank(taskName)) {
            drcTasks = drcTaskMapper.list();
        } else {
            drcTasks = drcTaskMapper.listByName(taskName);
        }
        for (DrcTask drcTask : drcTasks) {
            tmpResult.add(buildParentTaskVO(drcTask));
        }
        MyPageQueryResult<ParentTaskVO> result = new MyPageQueryResult<>();
        result.setResultList(tmpResult);
        result.setTotalItems((int) page.getTotal());
        result.setTotalPages((int) (page.getTotal() / pageSize + 1));
        result.setTotalItems((int) page.getTotal());
        result.setCurrentPage(pageSize);
        result.setItemsPerPage(pageSize);

        return result;
    }

    public List<BaseTaskVO> subList(int parentId) {
        //return subListCache.get(parentId);
        return subList0(parentId);
    }

    public List<BaseTaskVO> subList0(int parentId) {
        DrcTask drcTask = drcTaskMapper.selectByPrimaryKey(parentId);
        return buildTaskVO(drcTask);
    }

    /**
     * ????????????/???????????? parent task ???????????? task.
     *
     * @param parentId
     * @param start    true:?????? false: ??????.
     */
    @Transactional
    public void operateParentTask(int parentId, boolean start) {
        if (start) {
            operateParentTaskStart(parentId);
        } else {
            operateParentTaskStop(parentId);
        }
    }

    protected void operateParentTaskStart(int parentId) {

        DrcSubTaskIncr drcSubTaskIncr = incrMapper.selectByParentId(parentId);
        // ???????????????, ?????????, ??????????????????.
        drcSubTaskIncr.setState(RUNNING.code);
        pdServer.startTask(drcSubTaskIncr.getSubTaskName());

        taskDbOperator.updateByPrimaryKeySelective(drcSubTaskIncr);

        DrcSubTaskFullConfig drcSubTaskFullConfig = fullConfigMapper.selectOneByDrcTaskId(parentId);

        if (drcSubTaskFullConfig != null && !drcSubTaskFullConfig.splitOver()) {
            split(parentId, true);
        }
    }

    void operateParentTaskStop(int parentId) {
        DrcSubTaskFullConfig drcSubTaskFullConfig = fullConfigMapper.selectOneByDrcTaskId(parentId);
        if (drcSubTaskFullConfig != null && !drcSubTaskFullConfig.splitOver()) {
            throw new RuntimeException("???????????????????????????????????????????????????.");
        }

        DrcSubTaskIncr drcSubTaskIncr = incrMapper.selectByParentId(parentId);
        taskDbOperator.updateByPrimaryKeySelective(drcSubTaskIncr);

        if (drcSubTaskFullConfig == null) {
            return;
        }
        List<DrcSubTaskFullSliceDetail> sliceDetailList =
                fullSliceDetailMapper.selectFullConfigId(drcSubTaskFullConfig.getId());
        for (DrcSubTaskFullSliceDetail item : sliceDetailList) {
            pdServer.stopTask(item.getSubTaskName(), "onLine stop");
            item.setState(TaskStateEnum.HAND_STOP.code);
            taskDbOperator.updateByPrimaryKeySelective(item);
        }

        drcSubTaskIncr.setState(TaskStateEnum.HAND_STOP.code);
        pdServer.stopTask(drcSubTaskIncr.getSubTaskName(), "onLine stop");
    }


    /**
     * ??????????????? task ????????? ?????? task
     * ??????????????????????????? full sub task???
     */
    @Transactional(rollbackFor = Exception.class)
    public void split(Integer parentTaskId, boolean start) {
        DrcTask drcTask = drcTaskMapper.selectByPrimaryKey(parentTaskId);
        if (drcTask == null || drcTask.getTaskType() == TaskTypeEnum.MYSQL_INCR_TASK.code) {
            throw new RuntimeException("????????????, ????????????, ????????????.");
        }
        // ????????? mix Type, ????????????????????????, ??????????????????.????????????????????????.
        if (type_set.contains(drcTask.getTaskType())) {
            DrcSubTaskIncr drcSubTaskIncr = incrMapper.selectByParentId(parentTaskId);
            if (drcSubTaskIncr.getState() != RUNNING.code) {
                throw new RuntimeException("????????????????????????, ??????????????????????????????????????? running ??????.");
            }
        }

        if (split_set.contains(drcTask.getState())) {
            throw new RuntimeException("??????????????????. " + TaskStateEnum.conv(drcTask.getState()).getDesc());
        }
        // ???????????????????????????.
        List<DrcSubTaskFullConfig> list = fullConfigMapper.selectListByDrcTaskId(parentTaskId);

        // ????????????.
        drcTask.setState(TaskStateEnum.SPLIT_ING.code);
        drcTaskMapper.updateByPrimaryKeySelective(drcTask);

        // ?????????, ?????????, ?????????????????????, JVM ??????, ?????? ReloadFailSpTask ??????.
        list.stream().filter((i) -> i.getSplitState() == SPLIT_STATE_NO).forEach(this::submitSplitTask);
    }

    private void submitSplitTask(DrcSubTaskFullConfig item) {
        // ??????????????????, ?????????????????????????????????, ??????????????? drcWoker ??? ?????????????????????.
        splitDispatch.putTask(item);
    }

    /**
     * ???????????????????????????.
     * ??????????????????, ???????????????????????????.
     *
     * @param taskName
     */
    @Transactional
    public void startSubTask(String taskName) {
        BaseTaskModel lookup = taskDbOperator.lookup(taskName);
        if (lookup.getState() == TaskStateEnum.OVER.code) {
            throw new RuntimeException("???????????????,?????????????????????.");
        }
        pdServer.startTask(taskName);
        lookup.setState(RUNNING.code);
        taskDbOperator.updateByPrimaryKeySelective(lookup);
    }

    @Transactional
    public void stopSubTask(String taskName) {
        BaseTaskModel lookup = taskDbOperator.lookup(taskName);
        if (lookup.getState() == TaskStateEnum.HAND_STOP.code) {
            return;
        }
        if (lookup.getState() == TaskStateEnum.OVER.code) {
            return;
        }
        lookup.setState(TaskStateEnum.HAND_STOP.code);
        taskDbOperator.updateByPrimaryKeySelective(lookup);
        try {
            pdServer.stopTask(taskName, "onLine stop");
        } catch (Exception e) {
            if (e.getCause() instanceof NotFoundRouteException) {
                log.warn(e.getMessage(), e);
                return;
            }
            throw e;
        }
    }

    public void failover(String taskName, String workerTcpUrl) {
        pdServer.failover(taskName, workerTcpUrl);
    }

    /**
     * ???????????? ip ??????????????????????????????????????????.
     *
     * @param ip
     */
    public boolean failover(String ip) {
        // 8081 ??? worker ???????????????.
        String workerName = ip + ":8081";
        WorkerManager workerManager = pdServer.getWorkerManager();

        Worker originWorker = workerManager.getWorkerByWorkerName(workerName);
        if (originWorker == null) {
            return false;
        }

        List<String> taskList = workerManager.getTaskStringListByWorker(originWorker);

        List<Worker> collect = workerManager.getWorkerList().stream().filter(i -> !i.getEndpoint().equals(originWorker.getEndpoint())).collect(Collectors.toList());
        if (collect.size() < 1) {
            log.info("targetWorkerList size < 1");
            return false;
        }
        int size = taskList.size();
        int count = 0;
        for (String task : taskList) {
            Collections.shuffle(collect);
            Worker targetWorker = collect.get(0);
            if (count >= size) {
                break;
            }
            pdServer.failover(task, targetWorker.getEndpoint().getTcpUrl());
            ++count;
        }
        return count >= size;
    }


    public void autoReBalance() {
        pdServer.autoReBalance("onLine autoReBalance");
    }

    @Transactional
    public void addIncrTask(IncrTaskInput task) {
        DbConfigVO dbConfigVO = task.getDbConfigVO();
        if (!task.getDrcSubTaskIncrVO().getTableExpression().startsWith(dbConfigVO.getDatabase())) {
            throw new RuntimeException("binlog ???????????????, ????????? dataBase ????????????.");
        }
        DrcTaskVO drcTaskVO = task.getDrcTaskVO();
        DrcSubTaskIncrVO drcSubTaskIncrVO = task.getDrcSubTaskIncrVO();

        DrcDbConfig drcDbConfig = insertDbConfig(dbConfigVO);

        if (drcTaskVO.getSinkType() == SinkConfig.Type.MYSQL.getCode()) {
            // ??????????????????, ??? SINK ??? MySQL. ????????????.
            String tableExpression = drcSubTaskIncrVO.getTableExpression();

            Map<String, String> allTables = new HashMap<>();

            if (tableExpression.contains(",")) {
                // ???????????????, ?????????????????????.
                String[] split = tableExpression.split(",");
                for (String expression : split) {
                    String tableName = expression.substring(expression.indexOf(".") + 1);
                    allTables.put(tableName, tableName);
                }
            } else {
                // ????????????, ???????????????, ????????? * ???.
                String tableName = tableExpression.substring(tableExpression.indexOf(".") + 1);
                if ("*".equals(tableName)) {
                    getAllTables(task.getDbConfigVO(), "");
                } else {
                    allTables = ImmutableMap.<String, String>builder().put(tableName, tableName).build();
                }
            }

            // ???????????????.
            migrateAllTableStructure(drcTaskVO, drcDbConfig, allTables);
        }
        int drcTaskId = insertDrcTask(drcTaskVO, TaskTypeEnum.MYSQL_INCR_TASK.code);
        insertDrcSubTaskIncr(drcTaskVO, drcSubTaskIncrVO, drcDbConfig.getId(), drcTaskId, drcTaskVO.getTaskName(), drcTaskVO.getQpsLimitConfig());

        if (task.getDrcTaskVO().getSinkType() == 0) {
            aliYunKafkaOpenApiClient.createTopic(task.getDrcTaskVO().getTaskName());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void addDataBaseMixTask(MixTaskInput mixInput) {

        DrcTaskVO drcTaskVO = mixInput.getDrcTaskVO();
        DbConfigVO fullDbConfigVO = mixInput.getFullDbConfig();
        DbConfigVO incrDbConfigVO = mixInput.getIncrDbConfig();
        DrcSubTaskIncrVO incrVO = mixInput.getDrcSubTaskIncrVO();
        if (incrVO == null) {
            incrVO = new DrcSubTaskIncrVO();
        }
        if (drcTaskVO.getQpsLimitConfig() == null) {
            drcTaskVO.setQpsLimitConfig(100);
        }


        DrcDbConfig incrDbConfig = insertDbConfig(incrDbConfigVO);
        DrcDbConfig fullDbConfig = insertDbConfig(fullDbConfigVO);

        int sinkType = drcTaskVO.getSinkType();
        if (sinkType == SinkConfig.Type.MYSQL.getCode()) {
            // ??????????????????.????????????????????????.??????????????????.
            drcTaskVO.getMySQLSinkConfig().setTableName(null);
        }
        // ?????????????????? sink ??????.
        int parentTaskId = insertDrcTask(drcTaskVO, TaskTypeEnum.MYSQL_DATABASE_MIX_TASK.code);

        // ?????????.
        FullTaskConfigVO vo = mixInput.getFullTaskConfigVO();
        Map<String, String> allTables = getAllTables(fullDbConfigVO, vo.getTableName().trim());
        if (CollectionUtils.isEmpty(allTables)) {
            throw new RuntimeException("???????????????.");
        }

        // ???????????? database ??????.
        insertSchemaLog(parentTaskId, allTables.keySet(), vo.getTableName().trim());

        // ????????????????????????.
        if (mixInput.getDrcTaskVO().getSinkType() == SinkConfig.Type.MYSQL.getCode()) {
            migrateAllTableStructure(drcTaskVO, incrDbConfig, allTables);
        }

        // ??????????????????.
        String database = fullDbConfigVO.getDatabase();
        if ("*".equals(vo.getTableName().trim())) {
            // ???????????? * ???.
            incrVO.setTableExpression(database + ".*");
        } else {
            // ???????????????. ?????????.
            StringBuilder tableExpression = new StringBuilder();
            for (String tableName : allTables.keySet()) {
                tableExpression.append(database.trim()).append(".").append(tableName.trim());
                tableExpression.append(",");
            }
            incrVO.setTableExpression(tableExpression.substring(0, tableExpression.length() - 1));
        }

        insertDrcSubTaskIncr(drcTaskVO, incrVO, incrDbConfig.getId(), parentTaskId, drcTaskVO.getTaskName(), drcTaskVO.getQpsLimitConfig());

        // ??????, ??????????????????.
        for (String table : allTables.keySet()) {
            FullTaskConfigVO fullTaskConfigVO = mixInput.getFullTaskConfigVO();
            if (fullTaskConfigVO.getSelectFieldList() == null) {
                fullTaskConfigVO.setSelectFieldList("*");
            }
            fullTaskConfigVO.setTableName(table);
            fullTaskConfigVO.setRangeSizeConfig(mixInput.getFullTaskConfigVO().getRangeSizeConfig());
            insertFullTaskConfig(fullTaskConfigVO, parentTaskId, fullDbConfig.getId(), drcTaskVO);
        }

        if (mixInput.getDrcTaskVO().getSinkType() == 0 || mixInput.getDrcTaskVO().getSinkType() == 1) {
            aliYunKafkaOpenApiClient.createTopic(mixInput.getDrcTaskVO().getTaskName());
        }
    }

    private void migrateAllTableStructure(DrcTaskVO drcTaskVO, DrcDbConfig oldDbConfig, Map<String/*oldTalbe*/, String/*sinkTable*/> allTables) {
        for (String oldTable : allTables.keySet()) {
            MySQLSinkConfig sink = new MySQLSinkConfig();
            BeanUtils.copyProperties(drcTaskVO.getMySQLSinkConfig(), sink);

            String sinkTableName = allTables.get(oldTable);
            sink.setTableName(sinkTableName == null ? oldTable : sinkTableName);
            // ???????????????.
            migrateTableStructure(oldDbConfig, oldTable, sink);
        }
    }

    private Map<String, String> getAllTables(DbConfigVO dbConfigVO, String tableName) {
        // ????????????????????????, ?????????????????????. ??????, ??????????????????.
        if ("*".equals(tableName.trim())) {
            Set<String> allTables = jdbcTemplateManager.showTables(dbConfigVO.getUrl(),
                    dbConfigVO.getUsername(),
                    dbConfigVO.getPwd(),
                    dbConfigVO.getDatabase());
            return Maps.uniqueIndex(allTables, input -> input);
        }

        Map<String, String> allTables = new HashMap<>();
        String[] split = tableName.split("\n");
        for (String table : split) {
            allTables.put(table.trim(), null);
        }
        return allTables;
    }

    private void insertSchemaLog(int parentTaskId, Set<String> allTables, String tableExpression) {

        StringBuilder sb = new StringBuilder();
        allTables.forEach(e -> sb.append(e).append(","));
        String substring = sb.substring(0, sb.length() - 1);

        DrcSubTaskSchemaLog log = new DrcSubTaskSchemaLog();
        log.setParentId(parentTaskId);
        log.setTableTotal(allTables.size());
        log.setTableList(substring);
        log.setTableExpression(tableExpression);
        log.setTableSplitFinish(0);
        drcSubTaskSchemaLogMapper.insertSelective(log);
    }

    @Transactional
    public void addFullTask(FullTaskInput input) {
        String whereStatement = input.getFullTaskConfigVO().getWhereStatement();
        if (!StringUtils.isBlank(whereStatement)) {
            if (!whereStatement.contains("where")) {
                throw new RuntimeException("??????????????? where ??????, ?????? where ???????????? ??? ???????????? where");
            }
        }
        DbConfigVO originDbConfigVO = input.getDbConfigVO();
        DrcTaskVO drcTaskVO = input.getDrcTaskVO();
        FullTaskConfigVO fullTaskConfigVO = input.getFullTaskConfigVO();
        if (fullTaskConfigVO.getSelectFieldList() == null) {
            fullTaskConfigVO.setSelectFieldList("*");
        }

        String tableName = input.getFullTaskConfigVO().getTableName();

        checkDbConfigValid(originDbConfigVO, tableName);

        DrcDbConfig originDbConfig = insertDbConfig(originDbConfigVO);
        int drcTaskId = insertDrcTask(drcTaskVO, TaskTypeEnum.MYSQL_FULL_TASK.code);
        insertFullTaskConfig(fullTaskConfigVO, drcTaskId, originDbConfig.getId(), drcTaskVO);

        if (input.getDrcTaskVO().getSinkType() == SinkConfig.Type.MYSQL.getCode()) {
            HashMap<String, String> map = Maps.newHashMap();
            map.put(tableName, drcTaskVO.getMySQLSinkConfig().getTableName());

            migrateAllTableStructure(drcTaskVO, originDbConfig, map);
        }

        if (input.getDrcTaskVO().getSinkType() == SinkConfig.Type.UN_KNOW.getCode()) {
            aliYunKafkaOpenApiClient.createTopic(input.getDrcTaskVO().getTaskName());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void addMixTask(MixTaskInput task) {

        DrcTaskVO drcTaskVO = task.getDrcTaskVO();
        DbConfigVO fullDbConfigVO = task.getFullDbConfig();
        DbConfigVO incrDbConfigVO = task.getIncrDbConfig();
        DrcSubTaskIncrVO incrVO = task.getDrcSubTaskIncrVO();
        if (incrVO == null) {
            incrVO = new DrcSubTaskIncrVO();
        }
        FullTaskConfigVO fullTaskConfigVO = task.getFullTaskConfigVO();
        if (fullTaskConfigVO.getSelectFieldList() == null) {
            fullTaskConfigVO.setSelectFieldList("*");
        }

        String originTableName = task.getFullTaskConfigVO().getTableName().trim();

        checkDbConfigValid(fullDbConfigVO, originTableName);

        incrVO.setTableExpression(fullDbConfigVO.getDatabase() + "." + originTableName);

        DrcDbConfig incrDbConfig = insertDbConfig(incrDbConfigVO);
        DrcDbConfig fullDbConfig = insertDbConfig(fullDbConfigVO);
        // incrDbConfig or fullDbConfig
        int parentTaskId = insertDrcTask(drcTaskVO, TaskTypeEnum.MYSQL_MIX_TASK.code);
        insertDrcSubTaskIncr(drcTaskVO, incrVO, incrDbConfig.getId(), parentTaskId, drcTaskVO.getTaskName(), drcTaskVO.getQpsLimitConfig());

        insertFullTaskConfig(fullTaskConfigVO, parentTaskId, fullDbConfig.getId(), drcTaskVO);

        if (drcTaskVO.getSinkType() == SinkConfig.Type.MYSQL.getCode()) {
            String sinkTable = drcTaskVO.getMySQLSinkConfig().getTableName();
            sinkTable = sinkTable.trim();
            HashMap<String, String> map = Maps.newHashMap();
            map.put(originTableName, sinkTable);
            migrateAllTableStructure(drcTaskVO, incrDbConfig, map);
        }

        if (task.getDrcTaskVO().getSinkType() == 0 || task.getDrcTaskVO().getSinkType() == 1) {
            aliYunKafkaOpenApiClient.createTopic(task.getDrcTaskVO().getTaskName());
        }
    }

    private void checkDbConfigValid(DbConfigVO fullDbConfig, String tableName) {
        jdbcTemplateManager.checkDbConfig(
                fullDbConfig.getUrl(),
                fullDbConfig.getUsername(),
                fullDbConfig.getPwd(),
                fullDbConfig.getDatabase(),
                tableName.trim());
    }

    private int insertDrcTask(DrcTaskVO drcTaskVO, int type) {
        DrcTask drcTask = new DrcTask();
        drcTask.setTaskName(drcTaskVO.getTaskName());
        drcTask.setTaskDesc(drcTaskVO.getDesc());
        drcTask.setTaskType(type);
        drcTask.setUserAlias(UserContext.getAlias());
        drcTask.setQpsLimitConfig(drcTaskVO.getQpsLimitConfig());
        drcTask.setSinkJson(getSinkJson(drcTaskVO));
        drcTaskMapper.insertSelective(drcTask);
        return drcTask.getId();
    }

    private String getSinkJson(DrcTaskVO drcTaskVO) {
        int sinkType = drcTaskVO.getSinkType();
        String json = "";
        if (sinkType != SinkConfig.Type.UN_KNOW.getCode()) {
            if (sinkType == SinkConfig.Type.KAFKA.getCode()) {
                json = (serializer.serializer(drcTaskVO.getKafkaSinkConfig()));
            }
            if (sinkType == SinkConfig.Type.MYSQL.getCode()) {
                json = (serializer.serializer(drcTaskVO.getMySQLSinkConfig()));
            }
            if (sinkType == SinkConfig.Type.ROCKETMQ.getCode()) {
                json = (serializer.serializer(drcTaskVO.getRocketSinkConfig()));
            }
            if (sinkType == SinkConfig.Type.CANAL_KAFKA.getCode()) {
                json = serializer.serializer(drcTaskVO.getCanalKafkaSinkConfig());
            }
        }

        return json;
    }


    /**
     * ???????????????.
     *
     * @param oldDbConfig
     * @param sink
     */
    public void migrateTableStructure(DrcDbConfig oldDbConfig, String originTableName, MySQLSinkConfig sink) {
        DrcDbConfig newConfig = new DrcDbConfig(sink.getUrl(), sink.getUsername(), sink.getPwd(), sink.getDatabase());
        MigrateTableStructureUtil.migrateTableStructure(oldDbConfig, newConfig, originTableName, sink.getTableName());
    }

    private DrcDbConfig insertDbConfig(DbConfigVO dbConfigVO) {

        CheckDataSourceValidResult checkDataSourceValidResult = jdbcTemplateManager.
                checkDataSourceValid(dbConfigVO.getUrl(), dbConfigVO.getUsername(), dbConfigVO.getPwd(), dbConfigVO.getDatabase());
        if (!checkDataSourceValidResult.isValid()) {
            throw new RuntimeException("url ?????????, select 1 ??????, ??????:" + checkDataSourceValidResult.getMsg());
        }

        DrcDbConfig drcDbConfig = new DrcDbConfig();
        drcDbConfig.setUrl(dbConfigVO.getUrl());
        drcDbConfig.setUsername(dbConfigVO.getUsername());
        drcDbConfig.setPassword(dbConfigVO.getPwd());
        drcDbConfig.setDatabaseName(dbConfigVO.getDatabase());
        drcDbConfigMapper.insertSelective(drcDbConfig);
        return drcDbConfigMapper.selectByPrimaryKey(drcDbConfig.getId());
    }

    private DrcSubTaskIncr insertDrcSubTaskIncr(DrcTaskVO drcTaskVO, DrcSubTaskIncrVO drcSubTaskIncrVO, int dbConfigId, int drcTaskId, String taskName, int qps) {
        DrcSubTaskIncr drcSubTaskIncr = new DrcSubTaskIncr();
        drcSubTaskIncr.setDbConfigId(dbConfigId);
        drcSubTaskIncr.setParentId(drcTaskId);
        drcSubTaskIncr.setSubTaskName(TaskNameBuilder.buildIncrName(taskName));
        drcSubTaskIncr.setState(TaskStateEnum.INIT.code);
        drcSubTaskIncr.setTableExpression(drcSubTaskIncrVO.getTableExpression());
        drcSubTaskIncr.setSinkJson(getSinkJson(drcTaskVO));

        DrcSubTaskIncrExt drcSubTaskIncrExt = new DrcSubTaskIncrExt();
        drcSubTaskIncrExt.setSupportDDLSync(drcSubTaskIncrVO.getSupportDDLSync());
        drcSubTaskIncrExt.setDDLSyncFilterDML(drcSubTaskIncrVO.getDDLSyncFilterDML());

        drcSubTaskIncr.setExt(JackSonUtil.obj2String(drcSubTaskIncrExt));
        incrMapper.insertSelective(drcSubTaskIncr);

        return drcSubTaskIncr;
    }

    private DrcSubTaskFullConfig insertFullTaskConfig(FullTaskConfigVO fullTaskConfigVO, Integer drcTaskId, Integer dbConfigId, DrcTaskVO drcTaskVO) {
        // ?????? 500w.
        DrcSubTaskFullConfig drcSubTaskFullConfig = new DrcSubTaskFullConfig();
        drcSubTaskFullConfig.setDbConfigId(dbConfigId);
        drcSubTaskFullConfig.setDrcTaskId(drcTaskId);

        int size;
        if (fullTaskConfigVO.getRangeSizeConfig() <= 0) {
            size = (defaultRangeSize * WAN);
        } else {
            size = (fullTaskConfigVO.getRangeSizeConfig() * WAN);
        }
        drcSubTaskFullConfig.setTableName(fullTaskConfigVO.getTableName());
        drcSubTaskFullConfig.setSelectFieldList(fullTaskConfigVO.getSelectFieldList());
        drcSubTaskFullConfig.setWhereStatement(fullTaskConfigVO.getWhereStatement());
        drcSubTaskFullConfig.setSinkJson(getSinkJson(drcTaskVO));
        drcSubTaskFullConfig.setRangeSizeConfig(size);

        fullConfigMapper.insertSelective(drcSubTaskFullConfig);
        return drcSubTaskFullConfig;

    }

    public CheckDataSourceValidResult checkDataSourceValid(DbConfigVO dbConfigVO) {

        CheckDataSourceValidResult checkDataSourceValidResult = jdbcTemplateManager.
                checkDataSourceValid(dbConfigVO.getUrl(), dbConfigVO.getUsername(), dbConfigVO.getPwd(), dbConfigVO.getDatabase());
        if (!checkDataSourceValidResult.isValid()) {
            throw new RuntimeException("?????????????????????????????????????????????????????????, select 1 ??????, ??????:" + checkDataSourceValidResult.getMsg());
        }

        return checkDataSourceValidResult;
    }

    private ParentTaskVO buildParentTaskVO(DrcTask drcTask) {
        ParentTaskVO vo = new ParentTaskVO();
        vo.setId(drcTask.getId());
        vo.setName(drcTask.getTaskName());
        vo.setType(drcTask.getTaskType());
        vo.setDesc(drcTask.getTaskDesc());
        vo.setCreateTime(DateUtils.format(drcTask.getCreateTime(), DateUtils.newFormat));

        String sinkJson = drcTask.getSinkJson();
        if (StringUtils.isBlank(sinkJson)) {
            vo.setDestName("????????? Kafka");
        } else {
            SinkConfig o = serializer.deSerializer(sinkJson);
            if (o.getClass().getName().equals(MySQLSinkConfig.class.getName())) {
                vo.setDestName("MySQL");
            } else if (o.getClass().getName().equals(RocketSinkConfig.class.getName())) {
                vo.setDestName("RocketMQ");
            } else if (o.getClass().getName().equals(KafkaSinkConfig.class.getName())) {
                vo.setDestName("????????? Kafka");
            } else if (o.getClass().getName().equals(CanalKafkaSinkConfig.class.getName())) {
                vo.setDestName("CanalKafka");
            }
        }
        buildState(drcTask, vo);
        buildException(drcTask, vo);
        buildQPS(drcTask, vo);
        return vo;
    }

    private void buildQPS(DrcTask drcTask, ParentTaskVO vo) {
        int qps = 0;
        DrcSubTaskIncr drcSubTaskIncr = incrMapperCache.get(drcTask.getId());
        if (drcSubTaskIncr != null) {
            qps = pdServer.getQPS(drcSubTaskIncr.getSubTaskName());
        }
        List<DrcSubTaskFullSliceDetail> list = fullSliceDetailMapper.selectByParentIdAndRunning(drcTask.getId());
        if (list != null) {
            for (DrcSubTaskFullSliceDetail item : list) {
                qps += pdServer.getQPS(item.getSubTaskName());
            }
        }

        vo.setQps(qps);
    }

    private void buildException(DrcTask drcTask, ParentTaskVO vo) {
        vo.setHasNormal(true);
        vo.setHasException(false);
        if (env.equalsIgnoreCase(showStateEnv)) {
            return;
        }

        Integer taskType = drcTask.getTaskType();
        if (taskType == TaskTypeEnum.MYSQL_INCR_TASK.code) {
            buildIncr(drcTask, vo);
        }
        if (taskType == TaskTypeEnum.MYSQL_FULL_TASK.code) {
            buildFull(drcTask, vo);
        }
        if (taskType == TaskTypeEnum.MYSQL_MIX_TASK.code) {
            buildFull(drcTask, vo);
            buildIncr(drcTask, vo);
        }
    }

    private void buildIncr(DrcTask drcTask, ParentTaskVO vo) {

        DrcSubTaskIncr incrDbResult = incrMapper.selectByParentId(drcTask.getId());
        if (incrDbResult != null) {
            if (incrDbResult.getState() != RUNNING.code) {
                vo.setHasNormal(true);
                vo.setHasException(false);
                return;
            }
            Task task = pdServer.getTaskManager().getTaskWithOutCreate(incrDbResult.getSubTaskName());
            if (task != null) {
                try {
                    if (task.getState() == TaskStateEnum.EXCEPTION) {
                        vo.setHasException(true);
                        vo.setHasNormal(false);
                    } else {
                        vo.setHasException(false);
                        vo.setHasNormal(true);
                    }
                } catch (Exception e) {
                    vo.setHasException(true);
                }
            } else {
                vo.setState(TaskStateEnum.EXCEPTION.code);
            }
        }
    }

    private void buildFull(DrcTask drcTask, ParentTaskVO vo) {
        List<DrcSubTaskFullSliceDetail> list = fullSliceDetailMapper.selectByParentId(drcTask.getId());
        for (DrcSubTaskFullSliceDetail item : list) {
            if (item.getState() != RUNNING.code) {
                vo.setHasNormal(true);
                vo.setHasException(false);
                return;
            }
            Task task = pdServer.getTaskManager().getTaskWithOutCreate(item.getSubTaskName());
            if (task != null) {
                try {
                    if (task.getState() == TaskStateEnum.EXCEPTION) {
                        vo.setHasException(true);
                        vo.setHasNormal(false);
                    } else {
                        vo.setHasException(false);
                        vo.setHasNormal(true);
                    }
                } catch (Exception e) {
                    vo.setHasException(true);
                }
            } else {
                vo.setState(TaskStateEnum.EXCEPTION.code);
            }
        }
    }

    private void buildState(DrcTask drcTask, ParentTaskVO vo) {
        if (vo.getType() == TaskTypeEnum.MYSQL_DATABASE_MIX_TASK.code) {
//            List<DrcSubTaskFullConfig> list = fullConfigMapper.selectListByDrcTaskId(drcTask.getId());
            // todo ????????????????????????.
            /** @see TaskStateEnum */
            vo.setState(drcTask.getState());
            vo.setCanSplit(vo.getState() == TaskStateEnum.INIT.code);
            return;
        }

        if (vo.getType() == TaskTypeEnum.MYSQL_FULL_TASK.code || vo.getType() == TaskTypeEnum.MYSQL_MIX_TASK.code) {
            DrcSubTaskFullConfig config = fullConfigMapper.selectOneByDrcTaskId(drcTask.getId());
            if (config != null) {
                Integer splitState = config.getSplitState();
                vo.setCanSplit(splitState == SPLIT_STATE_NO);
                if (config.getSplitState() == SET_SPLIT_STATE_ING) {
                    vo.setState(TaskStateEnum.SPLIT_ING.code);
                }
                if (config.getSplitState() == SPLIT_STATE_NO) {
                    vo.setState(TaskStateEnum.INIT.code);
                }
                if (config.getSplitState() == SPLIT_STATE_OVER) {
                    vo.setState(TaskStateEnum.INIT.code);
                }
            } else {
                vo.setState(TaskStateEnum.SPLIT_ING.code);
            }
            return;
        }
        if (vo.getType() == TaskTypeEnum.MYSQL_INCR_TASK.code) {
            DrcSubTaskIncr drcSubTaskIncr = incrMapper.selectByParentId(drcTask.getId());
            if (drcSubTaskIncr != null) {
                vo.setState(drcSubTaskIncr.getState());
            }
        }
    }


    /**
     * ????????????????????? guava ??????, ???????????????????????????.
     *
     * @param drcTask
     * @return
     */
    private List<BaseTaskVO> buildTaskVO(DrcTask drcTask) {

        LinkedList<BaseTaskVO> taskVOS = new LinkedList<>();

        List<DrcSubTaskFullSliceDetail> sliceDetailList = fullSliceDetailMapperCache.get(drcTask.getId());
        if (!CollectionUtils.isEmpty(sliceDetailList)) {
            sliceDetailList = fullSliceDetailMapper.selectByParentId(drcTask.getId());
            for (DrcSubTaskFullSliceDetail item : sliceDetailList) {
                TaskVO.FullTaskVO fullTaskVO = new TaskVO.FullTaskVO();
                BeanUtils.copyProperties(item, fullTaskVO);
                if (item.getState() == RUNNING.code) {
                    Task task = pdServer.getTaskManager().getTaskWithOutCreate(item.getSubTaskName());
                    if (task == null) {
                        fullTaskVO.setState(TaskStateEnum.DB_RUNNING_RPC_EXCEPTION.code);
                    }
                }

                //DrcSubTaskFullConfig drcSubTaskFullConfig = fullConfigMapperCache.get(item.getDrcSubTaskFullConfigId());

                fullTaskVO.setType(TaskTypeEnum.MYSQL_FULL_TASK.code);
                fullTaskVO.setTaskName(item.getSubTaskName());
                fullTaskVO.setCursor(item.getSliceCursor());
//                fullTaskVO.setSplitState(drcSubTaskFullConfig.getSplitState());
//                fullTaskVO.setTableName(drcSubTaskFullConfig.getTableName());
//                fullTaskVO.setSliceCount(drcSubTaskFullConfig.getSliceCount());
                fullTaskVO.setQpsConfig(drcTask.getQpsLimitConfig());
                //fullTaskVO.setFinishRowCount(DrcRedissonCache.get(item.getSubTaskName()));

                buildSwitchState(fullTaskVO);
                taskVOS.add(fullTaskVO);
            }
        }

        taskVOS.sort(Comparator.comparingInt(BaseTaskVO::getState));

        List<BaseTaskVO> runningList = new ArrayList<>();

        // ??????????????????????????????.
        Iterator<BaseTaskVO> iterator = taskVOS.iterator();
        while (iterator.hasNext()) {
            BaseTaskVO i = iterator.next();
            if (i.getState() == RUNNING.code) {
                iterator.remove();
                runningList.add(i);
            }
        }

        runningList.forEach(taskVOS::addFirst);

        // ???????????? VO ??????.
        DrcSubTaskIncr incrDbResult = incrMapper.selectByParentId(drcTask.getId());
        if (incrDbResult != null) {
            TaskVO.IncrTaskVO incrTaskVO = new TaskVO.IncrTaskVO();
            BeanUtils.copyProperties(incrDbResult, incrTaskVO);
            incrTaskVO.setTaskName(incrDbResult.getSubTaskName());
            incrTaskVO.setExt(incrDbResult.toBO().getDrcSubTaskIncrExt());
            if (incrDbResult.getState() == RUNNING.code) {
                Task task = null;
                try {
                    task = pdServer.getTaskManager().getTaskWithOutCreate(incrDbResult.getSubTaskName());
                } catch (Exception e) {
                    log.warn(e.getMessage(), e);
                }
                if (task != null) {
                    try {
                        incrTaskVO.setState(task.getState().code);
                    } catch (Exception e) {
                        incrTaskVO.setState(TaskStateEnum.EXCEPTION.code);
                    }
                } else {
                    incrTaskVO.setState(TaskStateEnum.DB_RUNNING_RPC_EXCEPTION.code);
                }
            }
            incrTaskVO.setTableExpression(incrTaskVO.getTableExpression());
            incrTaskVO.setType(TaskTypeEnum.MYSQL_INCR_TASK.code);
            buildSwitchState(incrTaskVO);
            // ??????????????????.
            taskVOS.addFirst(incrTaskVO);
        }

        return taskVOS;
    }

    private void buildSwitchState(BaseTaskVO vo) {
        Integer state = vo.getState();
        if (state == RUNNING.code ||
                state == TaskStateEnum.STAGING.code ||
                state == TaskStateEnum.PLAYBACK_ING.code
        ) {
            vo.setSwitchState(BaseTaskVO.switchState_ON);
        } else {
            vo.setSwitchState(BaseTaskVO.switchState_OFF);
        }
    }

    public QPSvo getQpsConfig(String taskName) {
        DrcTask drcTask = drcTaskMapper.selectByName(taskName);
        Integer qpsLimitConfig = drcTask.getQpsLimitConfig();
        return new QPSvo(qpsLimitConfig);
    }

    public void updateQpsConfig(QpsConfigParam qps) {
        String taskName = qps.getTaskName();
        DrcTask drcTask = drcTaskMapper.selectByName(taskName);
        drcTask.setQpsLimitConfig(qps.getQps());
        drcTaskMapper.updateByPrimaryKeySelective(drcTask);
    }

    /**
     * ?????????????????????????????????.
     * ???????????????????????????.
     * ?????????????????????.
     *
     * @param parentId ??? id.
     */
    @Transactional
    public void deleteParentTask(Integer parentId) {
        DrcTask drcTask = drcTaskMapper.selectByPrimaryKey(parentId);
        if (drcTask == null) {
            throw new RuntimeException("??? task ?????????.");
        }
        Integer taskType = drcTask.getTaskType();
        // ????????????
        deleteFull(parentId, taskType);
        // ????????????
        deleteIncrTask(parentId, taskType);
        // 3 ???????????????.
        drcTaskMapper.deleteByPrimaryKey(drcTask.getId());
        // 3 ????????????????????????.
        drcSubTaskSchemaLogMapper.deleteByParentId(drcTask.getId());


        // ??????????????? kafka topic
        if (drcTask.getSinkJson() == null) {
            aliYunKafkaOpenApiClient.deleteTopic(drcTask.getTaskName());
        } else {
            SinkConfig sinkConfig = serializer.deSerializer(drcTask.getSinkJson());
            if (sinkConfig instanceof KafkaSinkConfig) {
                aliYunKafkaOpenApiClient.deleteTopic(drcTask.getTaskName());
            }
        }
    }

    private void deleteFull(Integer parentId, Integer taskType) {
        if (taskType == TaskTypeEnum.MYSQL_FULL_TASK.code
                || taskType == TaskTypeEnum.MYSQL_MIX_TASK.code
                || taskType == TaskTypeEnum.MYSQL_DATABASE_MIX_TASK.code) {
            // ?????????????????????, ??????????????????.
            List<DrcSubTaskFullSliceDetail> fullList = fullSliceDetailMapper.selectByParentId(parentId);
            for (DrcSubTaskFullSliceDetail item : fullList) {
                if (item.getState() == RUNNING.code) {
                    // ??????????????????.
                    pdServer.stopTask(item.getSubTaskName(), "deleteFull");
                }
                // ???????????????.
                fullSliceDetailMapper.deleteByPrimaryKey(item.getId());
            }
            DrcSubTaskFullConfig drcSubTaskFullConfig = fullConfigMapper.selectOneByDrcTaskId(parentId);
            // ???????????????.
            if (drcSubTaskFullConfig != null) {
                fullConfigMapper.deleteByPrimaryKey(drcSubTaskFullConfig.getId());
            }
        }
    }

    private void deleteIncrTask(Integer parentId, Integer taskType) {
        if (taskType == TaskTypeEnum.MYSQL_INCR_TASK.code || taskType == TaskTypeEnum.MYSQL_MIX_TASK.code || taskType == TaskTypeEnum.MYSQL_DATABASE_MIX_TASK.code) {
            DrcSubTaskIncr drcSubTaskIncr = incrMapper.selectByParentId(parentId);
            // ???????????????.
            if (drcSubTaskIncr != null) {
                if (drcSubTaskIncr.getState() == RUNNING.code) {
                    // ??????????????????
                    pdServer.stopTask(drcSubTaskIncr.getSubTaskName(), "deleteIncrTask");
                }
                // ?????????????????????
                incrMapper.deleteByPrimaryKey(drcSubTaskIncr.getId());
                //  ?????? topic, ?????? zk ??????..
                try {
                    deleteMQTopicZkNode(drcSubTaskIncr);
                    deleteRedisCache(drcSubTaskIncr.getSubTaskName());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public void deleteRedisCache(String taskName) {
        DrcRedisson.delete(MysqlIncrTaskConstants.getRunningStatus(taskName));
        DrcRedisson.delete(MysqlIncrTaskConstants.getPauseTime(taskName));
        DrcRedisson.delete(MysqlIncrTaskConstants.getStashMsgNum(taskName));
        DrcRedisson.delete(MysqlIncrTaskConstants.getLastReceivedTime(taskName));
        DrcRedisson.delete(MysqlIncrTaskConstants.getFirstData(taskName));
    }

    private void deleteMQTopicZkNode(DrcSubTaskIncr drcSubTaskIncr) throws Exception {
        String path = "/middleware/myCanal/destinations/" + drcSubTaskIncr.getSubTaskName();
        // ?????? canal server
        long waitTime = System.currentTimeMillis();
        boolean deleteSuccess = false;
        // ?????? 20s, ?????????. ???????????????.
        while (drcZkClient.exists(path) && System.currentTimeMillis() - waitTime < TimeUnit.SECONDS.toMillis(20)) {
            try {
                // ?????? zk ?????? canal ??????. ???????????????, ????????? canal server ????????????????????????, ????????????????????????????????????.
                // ?????????????????????.
                drcZkClient.deleteRecursive(path);
                deleteSuccess = true;
            } catch (Exception e) {
                //ignore
            }
        }
        if (!deleteSuccess) {
            AlarmUtil.pushAlarm2Admin("?????? zk ????????????, ???????????????, ??????????????????, ??????:" + path);
        }
        mqAdminService.deleteMQTopicAndConsumerGroup(drcSubTaskIncr.getSubTaskName());
    }

    public String getLog(String taskName, Integer line) throws IOException {
        Task task = pdServer.getTaskManager().getTaskWithOutCreate(taskName);
        if (task != null) {
            try {
                return task.getLogText(line);
            } catch (UndeclaredThrowableException e) {
                // ????????????
                return e.getCause().getMessage();
            }
        }
        String ip = DrcRedisson.get("DRC_TASK_RECENT_START_IP_" + taskName);
        if (ip == null) {
            return "?????????.";
        }

        String result = null;
        try {
            result = DrcHttpClientUtil.postAndReturnString("http://" + ip + ":8081/api/task/" + taskName + "/getLogText", "");
        } catch (Exception e) {
            log.warn("getLog fail, msg={}", e.getMessage());
            return "RPC ????????????, ?????????";
        }

        Object o = serializer.deSerializer(result);
        if (o instanceof String) {
            return o.toString();
        }
        RpcResult<?> rpcResult = (RpcResult<?>) o;
        Object log = rpcResult.getT();
        return log + "\n  " + ip;
    }


    public TaskDetail getTaskDetail(String parentTaskName) {
        TaskDetail detail = new TaskDetail();

        DrcTask drcTask = drcTaskMapper.selectByName(parentTaskName);
        Integer id = drcTask.getId();

        DrcSubTaskFullConfig drcSubTaskFullConfig = fullConfigMapper.selectOneByDrcTaskId(id);
        DrcSubTaskIncr drcSubTaskIncr = incrMapper.selectByParentId(id);

        DrcDbConfig masterDb = drcDbConfigMapper.selectByPrimaryKey(drcSubTaskIncr.getDbConfigId());
        DrcDbConfig slaveDb = drcDbConfigMapper.selectByPrimaryKey(drcSubTaskFullConfig.getDbConfigId());
        DbConfigVO master = new DbConfigVO();
        BeanUtils.copyProperties(masterDb, master);
        master.setPwd(masterDb.getPassword());
        master.setDatabase(masterDb.getDatabaseName());
        DbConfigVO slave = new DbConfigVO();
        BeanUtils.copyProperties(slaveDb, slave);
        slave.setPwd(slaveDb.getPassword());
        slave.setDatabase(slaveDb.getDatabaseName());

        detail.setMaster(master);
        detail.setSlave(slave);


        String sinkJson = drcTask.getSinkJson();
        SinkConfig sink = serializer.deSerializer(sinkJson);
        if (sink instanceof KafkaSinkConfig) {
            KafkaSinkConfig kafkaSinkConfig = (KafkaSinkConfig) sink;
            String kafkaBootstrapServers = kafkaSinkConfig.getKafkaBootstrapServers();
            String topic = kafkaSinkConfig.getTopic();

            OpenApiStartParams.MessageQueueConfig messageQueueConfig = new OpenApiStartParams.MessageQueueConfig();
            messageQueueConfig.setServers(kafkaBootstrapServers);
            messageQueueConfig.setTopic(topic);
            messageQueueConfig.setType("kafka");
            detail.setTopic(messageQueueConfig);
        }
        if (sink instanceof RocketSinkConfig) {
            RocketSinkConfig sink2 = (RocketSinkConfig) sink;

            OpenApiStartParams.MessageQueueConfig messageQueueConfig = new OpenApiStartParams.MessageQueueConfig();
            messageQueueConfig.setServers(sink2.getNameServer());
            messageQueueConfig.setTopic(sink2.getTopic());
            messageQueueConfig.setType("rocketmq");
            detail.setTopic(messageQueueConfig);
        }

        Task task = pdServer.getTaskManager().getTaskWithOutCreate(drcSubTaskIncr.getSubTaskName());
        TaskStateEnum state = task.getState();
        detail.setTaskStatus(state);

        return detail;
    }

    static Set<Integer> mixTaskCodeSet = Sets.newHashSet(TaskTypeEnum.MYSQL_FULL_TASK.code, TaskTypeEnum.MYSQL_MIX_TASK.code, TaskTypeEnum.MYSQL_DATABASE_MIX_TASK.code);

    public MixTaskInput copyTask(String taskName) {

        DrcTaskVO drcTaskVO = new DrcTaskVO();
        DbConfigVO fullDbConfigVO = new DbConfigVO();
        DbConfigVO incrDbConfigVO = new DbConfigVO();
        FullTaskConfigVO fullTaskConfigVO = new FullTaskConfigVO();
        DrcSubTaskIncrVO drcSubTaskIncrVO = new DrcSubTaskIncrVO();
        MixTaskInput input = new MixTaskInput();

        DrcTask drcTask = drcTaskMapper.selectByName(taskName);
        input.setDrcTaskVO(drcTaskVO);
        if (drcTask == null) {
            drcTaskVO.setTaskName("??????????????? Task.");
            return input;
        }
        buildDrcTaskVO(drcTaskVO, drcTask);

        // ????????????.
        if (mixTaskCodeSet.contains(drcTask.getTaskType())) {
            DrcSubTaskFullConfig fullConfig = fullConfigMapper.selectOneByDrcTaskId(drcTask.getId());
            if (fullConfig != null) {
                buildDbConfigVO(fullDbConfigVO, fullConfig.getDbConfigId());
                buildFullConfigVO(fullTaskConfigVO, fullConfig);
            }
            if (drcTask.getTaskType() == TaskTypeEnum.MYSQL_DATABASE_MIX_TASK.code) {
                DrcSubTaskSchemaLog log = drcSubTaskSchemaLogMapper.selectByParentTaskId(drcTask.getId());
                if (log != null) {
                    if (StringUtils.isNotEmpty(log.getTableExpression())) {
                        fullTaskConfigVO.setTableName(log.getTableExpression());
                    } else {
                        String[] split = log.getTableList().split(",");
                        StringBuilder sb = new StringBuilder();
                        for (String tableName : split) {
                            sb.append(tableName).append("\r\n");
                        }
                        fullTaskConfigVO.setTableName(sb.toString());
                    }
                }
            }

            input.setFullTaskConfigVO(fullTaskConfigVO);
            input.setFullDbConfig(fullDbConfigVO);
        }


        DrcSubTaskIncr drcSubTaskIncr = incrMapper.selectByParentId(drcTask.getId());
        if (drcSubTaskIncr != null) {
            DrcSubTaskIncrBO drcSubTaskIncrBO = drcSubTaskIncr.toBO();
            drcSubTaskIncrVO.setSupportDDLSync(
                    BooleanUtils.isTrue(drcSubTaskIncrBO.getDrcSubTaskIncrExt().getSupportDDLSync()));
            drcSubTaskIncrVO.setDDLSyncFilterDML(
                    BooleanUtils.isTrue(drcSubTaskIncrBO.getDrcSubTaskIncrExt().getDDLSyncFilterDML()));
            drcSubTaskIncrVO.setTableExpression(drcSubTaskIncr.getTableExpression());
            buildDbConfigVO(incrDbConfigVO, drcSubTaskIncr.getDbConfigId());

            input.setIncrDbConfig(incrDbConfigVO);
            input.setDrcSubTaskIncrVO(drcSubTaskIncrVO);
        }

        return input;
    }

    private void buildFullConfigVO(FullTaskConfigVO fullTaskConfigVO, DrcSubTaskFullConfig fullConfig) {
        fullTaskConfigVO.setRangeSizeConfig(fullConfig.getRangeSizeConfig() / 10000);
        fullTaskConfigVO.setTableName(fullConfig.getTableName());
        fullTaskConfigVO.setSelectFieldList(fullConfig.getSelectFieldList());
        fullTaskConfigVO.setWhereStatement(fullConfig.getWhereStatement());
    }

    private void buildDbConfigVO(DbConfigVO fullDbConfigVO, Integer dbConfigId) {
        DrcDbConfig fullDrcDbConfig = drcDbConfigMapper.selectByPrimaryKey(dbConfigId);
        fullDbConfigVO.setUrl(fullDrcDbConfig.getUrl());
        fullDbConfigVO.setUsername(fullDrcDbConfig.getUsername());
        fullDbConfigVO.setPwd(fullDrcDbConfig.getPassword());
        fullDbConfigVO.setDatabase(fullDrcDbConfig.getDatabaseName());
    }

    private void buildDrcTaskVO(DrcTaskVO drcTaskVO, DrcTask drcTask) {
        String sinkJson = drcTask.getSinkJson();
        if (!StringUtils.isBlank(sinkJson)) {
            SinkConfig sinkConfig = serializer.deSerializer(sinkJson);
            if (sinkConfig instanceof KafkaSinkConfig) {
                drcTaskVO.setKafkaSinkConfig(new Gson().fromJson(sinkJson, KafkaSinkConfig.class));
                drcTaskVO.setSinkType(1);
            } else if (sinkConfig instanceof MySQLSinkConfig) {
                drcTaskVO.setMySQLSinkConfig(new Gson().fromJson(sinkJson, MySQLSinkConfig.class));
                drcTaskVO.setSinkType(2);
            } else if (sinkConfig instanceof RocketSinkConfig) {
                drcTaskVO.setRocketSinkConfig(new Gson().fromJson(sinkJson, RocketSinkConfig.class));
                drcTaskVO.setSinkType(3);
            } else if (sinkConfig instanceof CanalKafkaSinkConfig) {
                drcTaskVO.setCanalKafkaSinkConfig(new Gson().fromJson(sinkJson, CanalKafkaSinkConfig.class));
                drcTaskVO.setSinkType(4);
            }
        }
        drcTaskVO.setQpsLimitConfig(drcTask.getQpsLimitConfig());
        drcTaskVO.setTaskName(drcTask.getTaskName());
        drcTaskVO.setDesc(drcTask.getTaskDesc());
        drcTaskVO.setType(drcTask.getTaskType());
    }

    @TraceCrossThread
    class SaveQPSDataTask implements Runnable {

        public SaveQPSDataTask() {
        }

        @SneakyThrows
        @Override
        public void run() {
            // ????????????.
            lockFactory.getLock("SaveQPSDataTask" + appGroupName).lockAndProtect(60, () -> {
                while (!shutdown) {
                    try {
                        QpsLog qpsLog = new QpsLog();
                        int totalQPS = pdServer.getTotalQPS();
                        qpsLog.setQps(totalQPS);
                        qpsLog.setTimeInSeconds(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()));
                        qpsLog.setName(ClusterQPS);
                        qpsLogMapper.insertSelective(qpsLog);
                        TimeUnit.MILLISECONDS.sleep(1000);
                    } catch (InterruptedException e2) {
                        log.warn("???????????????.");
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    }
                }
            });
        }
    }

    @TraceCrossThread
    class DeleteQPSDataTask implements Runnable {
        @Override
        public void run() {
            // ?????? 1 ???????????? QPS ????????????, ????????????, ???????????? 8w ?????????.
            qpsLogMapper.deleteWhenLessThanTime(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()) - TimeUnit.DAYS.toSeconds(1));
        }
    }


    @TraceCrossThread
    class ReloadFailSplitTask implements Runnable {

        @Override
        public void run() {
            DrcLock reloadFailSpTask = lockFactory.getLock("ReloadFailSpTask");
            reloadFailSpTask.lockAndProtect(0, () -> {
                List<DrcSubTaskFullConfig> list = fullConfigMapper.selectFailSplitTask();
                for (DrcSubTaskFullConfig config : list) {
                    Date splitTime = config.getSplitTime();
                    if (System.currentTimeMillis() - splitTime.getTime() > TimeUnit.MINUTES.toMillis(
                            ConfigService.getAppConfig().getIntProperty("ReloadFailSpTask.difference.value", 5))) {
                        PDTaskService.this.submitSplitTask(config);
                    }
                }
            });

        }
    }


}

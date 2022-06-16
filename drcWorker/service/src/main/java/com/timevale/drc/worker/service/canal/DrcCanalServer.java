package com.timevale.drc.worker.service.canal;

import com.alibaba.otter.canal.protocol.ClientIdentity;
import com.alibaba.otter.canal.protocol.Message;
import com.timevale.drc.base.log.TaskLog;
import com.timevale.drc.base.util.DrcZkClient;
import com.timevale.drc.worker.service.canal.support.DrcCanalInstanceGenerator;
import com.timevale.drc.worker.service.canal.support.DrcCanalServerWithEmbedded;
import com.timevale.drc.worker.service.canal.support.DrcLogAlarmHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author gwk_2
 * @date 2021/4/12 20:46
 */
@Slf4j
public class DrcCanalServer {
    private static final Map<String, DrcCanalServer> map = new HashMap<>();
    private static final short CLIENT_ID = 1002;


    private final DrcCanalServerWithEmbedded canalServer;
    private final DrcCanalInstanceGenerator drcCanalInstanceGenerator;
    private final DrcLogAlarmHandler drcLogAlarmHandler;
    private final DrcZkClient drcZkClient;


    public static DrcCanalServer getInstanceWithStart(String zkAddr, DrcZkClient drcZkClient, DrcLogAlarmHandler drcLogAlarmHandler) {

        synchronized (map) {
            if (map.get(zkAddr) == null) {
                DrcCanalServer drcCanalServer = new DrcCanalServer(zkAddr, drcZkClient, drcLogAlarmHandler);
                drcCanalServer.tryStartServer();
                Runtime.getRuntime().addShutdownHook(new Thread(drcCanalServer::stopServer));
                log.info("启动 canal server 成功......");
                map.put(zkAddr, drcCanalServer);
                return drcCanalServer;
            }
            return map.get(zkAddr);
        }
    }

    public DrcCanalServer(String zkAddr, DrcZkClient drcZkClient, DrcLogAlarmHandler drcLogAlarmHandler) {
        this.canalServer = new DrcCanalServerWithEmbedded();
        this.drcZkClient = drcZkClient;
        this.drcCanalInstanceGenerator = new DrcCanalInstanceGenerator(zkAddr);
        this.canalServer.setCanalInstanceGenerator(drcCanalInstanceGenerator);
        this.drcLogAlarmHandler = drcLogAlarmHandler;
    }

    /**
     * 尝试启动 server. 可能启动过了, 但是不要紧.
     */
    public void tryStartServer() {
        canalServer.start();
    }

    /**
     * 停止 server.
     */
    public void stopServer() {
        canalServer.stop();
    }

    /**
     * 启动并订阅实例.
     *
     * @param destination
     */
    public void startInstance(String destination, Properties properties, TaskLog logger) {

        Boolean remove = DrcLogAlarmHandler.NEED_DELETE_MAP.get(destination);
        if (remove != null && remove) {
            String path = String.format("/middleware/myCanal/destinations/%s/1002/cursor", destination);
            boolean delete = drcZkClient.delete(String.format(path, destination));
            logger.warn("删除是否成功={" + delete + "}, DrcLogAlarmHandler 表示需要删除位点记录,重新开始, TaskName = " + destination);
            DrcLogAlarmHandler.NEED_DELETE_MAP.remove(destination);
        }

        drcCanalInstanceGenerator.addInstanceProperties(destination, properties);

        drcLogAlarmHandler.saveLog(destination, logger);

        boolean startSuccess = canalServer.startInstance(destination);

        ClientIdentity clientIdentity = new ClientIdentity(destination, CLIENT_ID);
        canalServer.subscribe(clientIdentity);
        log.info("启动 canal 实例是否成功={}......{}", startSuccess, destination);
    }

    /**
     * 停止实例.
     *
     * @param destination
     */
    public void stopInstance(final String destination) {
        canalServer.stopInstance(destination);

        drcLogAlarmHandler.remove(destination);

        log.info("关闭 canal 实例 成功......");
    }

    /**
     * 获取数据.
     *
     * @param batchSize
     * @param destination
     * @return
     */
    public Message getWithoutAck(int batchSize, String destination) {
        ClientIdentity clientIdentity = new ClientIdentity(destination, CLIENT_ID);
        return canalServer.getWithoutAck(clientIdentity, batchSize);
    }

    /**
     * ack 数据. 更新 cursor.
     *
     * @param id          不能是 -1
     * @param destination
     */
    public void ack(long id, String destination) {
        ClientIdentity clientIdentity = new ClientIdentity(destination, CLIENT_ID);
        canalServer.ack(clientIdentity, id);
    }

    /**
     * 查看实例是否启动.
     *
     * @param destination
     * @return
     */
    public boolean isStart(String destination) {
        return canalServer.isStart(destination);
    }

}

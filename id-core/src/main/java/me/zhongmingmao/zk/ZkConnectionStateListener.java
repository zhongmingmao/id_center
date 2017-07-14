package me.zhongmingmao.zk;

import lombok.extern.apachecommons.CommonsLog;
import me.zhongmingmao.Id.generator.ZKSnowflakeIDGenerator;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;

/**
 * Zk连接状态监听器
 *
 * @author zhongmingmao zhongmingmao0625@gmail.com
 */
@CommonsLog
public class ZkConnectionStateListener implements ConnectionStateListener {
    
    /**
     * ID生成器
     */
    private ZKSnowflakeIDGenerator idGenerator;
    /**
     * ID生成器是否进入休眠状态
     */
    private volatile boolean suspend = false;
    
    public ZkConnectionStateListener(ZKSnowflakeIDGenerator idGenerator) {
        this.idGenerator = idGenerator;
    }
    
    @Override
    public synchronized void stateChanged(CuratorFramework client, ConnectionState newState) {
        switch (newState) {
            case LOST:
            case SUSPENDED:
                // 连接丢失或会话超时，原本持有的集群机器ID可能已经被其他机器申请，ID生成器应该进入休眠状态
                log.error("zookeeper connection lost , idGenerator will suspend!");
                suspend = true;
                idGenerator.suspend();
                // 尝试重新建立连接
                idGenerator.reconnect();
                break;
            case RECONNECTED:
                if (suspend) {
                    // 重新建立连接后，重新生成集群机器ID
                    try {
                        idGenerator.rebuildDatacenterId();
                        suspend = false;
                        idGenerator.recover();
                        log.info("idGenerator recover success!");
                    } catch (Exception e) {
                        log.error("idGenerator recover fail!", e);
                    }
                }
                break;
            default:
                break;
        }
    }
}

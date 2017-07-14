package me.zhongmingmao.zk;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.ACLProvider;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.utils.CloseableUtils;
import org.apache.curator.utils.PathUtils;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Id;
import org.apache.zookeeper.server.auth.DigestAuthenticationProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.Closeable;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Zookeeper管理器，管理Zookeeper的常规操作
 */
@Component
@CommonsLog
public class ZkManager implements Closeable {
    
    private static final String DEFAULT_NAMESPACE = "id-center";
    private static final String ROOT_PATH = "/";
    private static final String DEFAULT_CHARSET = "UTF-8";
    private static final String DEFAULT_ZNODE_VALUE = "zhongmingmao";
    private static final String DEFAULT_ACL_SCHEME = "digest";
    private static final String DEFAULT_ACL_AUTH = "zhongmingmao:test1234";
    private static final String DEFAULT_CONNECT_STRING = "localhost:2181";
    private static final List<ACL> DEFAULT_ACL_LIST;
    private static final int DEFAULT_BASE_SLEEP_TIME_MS = 1000;
    private static final int DEFAULT_MAX_RETRIES = 3;
    private static final int MAX_WAIT_SECONDS = 5;
    
    static {
        List<ACL> customAclList = ZooDefs.Ids.OPEN_ACL_UNSAFE;
        try {
            customAclList = Arrays.asList(new ACL(ZooDefs.Perms.ALL,
                    new Id(DEFAULT_ACL_SCHEME, DigestAuthenticationProvider.generateDigest(DEFAULT_ACL_AUTH))));
        } catch (NoSuchAlgorithmException e) {
            log.error(String.format("tryCreate acls fail , use acls : %s", customAclList), e);
        }
        DEFAULT_ACL_LIST = customAclList;
        log.info(String.format("init acls : %s", customAclList));
    }
    
    /**
     * ZK连接字符串
     */
    @Getter
    @Setter
    @Value("${zookeeper.connect.string}")
    private String connectString = DEFAULT_CONNECT_STRING;
    
    /**
     * Curator客户端
     */
    private CuratorFramework client;
    
    /**
     * 创建Zookeeper连接
     *
     * @throws InterruptedException
     * @throws UnsupportedEncodingException
     */
    @PostConstruct
    public void connect() throws InterruptedException, UnsupportedEncodingException {
        client = CuratorFrameworkFactory.builder()
                .connectString(connectString)
                .retryPolicy(new ExponentialBackoffRetry(DEFAULT_BASE_SLEEP_TIME_MS, DEFAULT_MAX_RETRIES))
                .namespace(DEFAULT_NAMESPACE)
                .authorization(DEFAULT_ACL_SCHEME, DEFAULT_ACL_AUTH.getBytes(DEFAULT_CHARSET)) // 设置权限
                .aclProvider(new ACLProvider() { // 设置ACL规则
                    @Override
                    public List<ACL> getDefaultAcl() {
                        return DEFAULT_ACL_LIST;
                    }
                    
                    @Override
                    public List<ACL> getAclForPath(String path) {
                        return DEFAULT_ACL_LIST;
                    }
                })
                .build();
        
        if (CuratorFrameworkState.STARTED != client.getState()) {
            client.start();
        }
        
        while (!client.blockUntilConnected(MAX_WAIT_SECONDS, TimeUnit.SECONDS)) {
            log.info("can not connect to zookeeper , retry again!!");
        }
    }
    
    /**
     * Spring容器关闭前先释放Zookeeper连接
     */
    @Override
    @PreDestroy
    public void close() throws IOException {
        log.info(String.format("releasing zookeeper connection before shutdown..."));
        CloseableUtils.closeQuietly(client);
    }
    
    /**
     * 检查数据数据节点是否存在
     *
     * @param path 数据节点路径
     * @return 存在返回true，否则返回false
     */
    private boolean pathExist(final String path) {
        try {
            PathUtils.validatePath(path);
            return null != client.checkExists().forPath(path);
        } catch (Exception e) {
            log.error(String.format("zKManager.pathExist error , path:%s", path), e);
            return false;
        }
    }
    
    public boolean tryCreate(final String path, final boolean isEphemeral) {
        return tryCreate(path, DEFAULT_ZNODE_VALUE, isEphemeral);
    }
    
    /**
     * 尝试递归创建数据节点
     *
     * @param path        数据节点路径
     * @param value       数据节点内容
     * @param isEphemeral 是否创建临时节点
     * @return 创建成功返回true，否则返回false
     */
    public boolean tryCreate(final String path, final String value, boolean isEphemeral) {
        log.info(String.format("zKManager.tryCreate , path:%s , value:%s , isEphemeral:%s",
                path, value, isEphemeral));
        try {
            if (pathExist(path)) {
                return false;
            }
            client.create().creatingParentsIfNeeded()
                    .withMode(isEphemeral ? CreateMode.EPHEMERAL : CreateMode.PERSISTENT)
                    .withACL(DEFAULT_ACL_LIST)
                    .forPath(path,
                            null == value ? DEFAULT_ZNODE_VALUE.getBytes(DEFAULT_CHARSET) :
                                    value.getBytes(DEFAULT_CHARSET));
            return true;
        } catch (Exception e) {
            log.error(String.format("zKManager.tryCreate error , path:%s , value:%s", path, value), e);
            return false;
        }
    }
    
    
    public List<String> getChildren() throws Exception {
        return getChildren(ROOT_PATH);
    }
    
    /**
     * 获取数据节点的子节点列表
     *
     * @param path 数据节点路径
     * @return
     * @throws Exception
     */
    public List<String> getChildren(final String path) throws Exception {
        return client.getChildren().forPath(path);
    }
    
    /**
     * 添加ZK连接状态监听器
     *
     * @param listener
     */
    public void addConnectionStateListener(ConnectionStateListener listener) {
        client.getConnectionStateListenable().addListener(listener);
    }
}

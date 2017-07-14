package me.zhongmingmao.Id.generator1;

import me.zhongmingmao.Id.generator.ZKSnowflakeIDGenerator;
import me.zhongmingmao.zk.ZkManager;
import org.apache.curator.test.TestingServer;
import org.junit.BeforeClass;
import org.junit.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

public class ZKSnowflakeIDGeneratorTests {
    
    public static final int Zk_CLIENT_PORT = 2182;
    public static final String CONNECT_STRING = String.format("localhost:%s", Zk_CLIENT_PORT);
    
    public volatile boolean beSet = false;
    public volatile LocalDateTime startTime = null;
    
    @BeforeClass
    public static void setUp() throws Exception {
        new TestingServer(Zk_CLIENT_PORT);
    }
    
    @Test
    public void generateIdTest() throws InterruptedException {
        int threadCount = 10;
        int idCount = 50000000;
        CountDownLatch endLatch = new CountDownLatch(idCount);
        CyclicBarrier startBarrier = new CyclicBarrier(threadCount);
        
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        IntStream.range(0, threadCount).forEach(value -> {
            pool.submit(() -> {
                try {
                    ZkManager zkManager = new ZkManager();
                    zkManager.setConnectString(CONNECT_STRING);
                    zkManager.connect();
                    ZKSnowflakeIDGenerator idGenerator = new ZKSnowflakeIDGenerator(zkManager);
                    idGenerator.init();
                    startBarrier.await(); // 等待所有idGenerator都已经连接到ZK后才开始并发请求ID
                    if (!beSet) {
                        startTime = LocalDateTime.now();
                        beSet = true;
                    }
                    
                    while (endLatch.getCount() > 0) {
                        endLatch.countDown(); // 到达多个线程并发请求ID的总数，就退出测试
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        });
        pool.shutdown();
        endLatch.await();
        long seconds = Duration.between(startTime, LocalDateTime.now()).getSeconds();
        System.out.println(String.format("%s thread get %s ids from zk ,take %ss , speed:%s[max:4095]",
                threadCount, idCount, seconds, (int) ((idCount + 0.0) / threadCount / seconds / 1000)));
        // 真实Zookeeper测试结果：10 thread get 300000000 ids from zk ,take 23s , speed:1304[max:4095]
        // TestServer测试结果：10 thread get 300000000 ids from zk ,take 18s , speed:1666[max:4095]
        // 性能已经能满足大部分应用场景了
    }
}
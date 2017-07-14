package me.zhongmingmao;

import lombok.extern.apachecommons.CommonsLog;
import me.zhongmingmao.Id.generator.ZKSnowflakeIDGenerator;
import org.apache.curator.test.TestingServer;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@CommonsLog
public class IdGeneratorTests {
    
    private static final int Zk_CLIENT_PORT = 2182;
    
    @Autowired
    private ZKSnowflakeIDGenerator idGenerator;
    
    @BeforeClass
    public static void setUp() throws Exception {
        new TestingServer(Zk_CLIENT_PORT);
    }
    
    @Test
    public void getIdTest() {
        log.info(String.format("id:%s", idGenerator.getId()));
    }
    
}

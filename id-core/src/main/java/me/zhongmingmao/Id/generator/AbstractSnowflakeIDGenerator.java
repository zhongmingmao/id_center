package me.zhongmingmao.Id.generator;

import java.util.Random;

/**
 * Twitter-Snowflake算法的抽象类，保存基本参数
 */
public abstract class AbstractSnowflakeIDGenerator implements IDGenerator {
    
    /**
     * 集群机器ID占用位数
     */
    protected static final long DATACENTER_ID_BITS = 10L;
    /**
     * 最多支持集群机器的数量，1023
     */
    protected static final long MAX_DATACENTER_ID = -1L ^ (-1L << DATACENTER_ID_BITS);
    /**
     * 序列号占用位数
     */
    protected static final long SEQUENCE_BITS = 12L;
    /**
     * 序列号最大值，表示一台机器在一毫秒内最多产生多少个ID，4095
     */
    protected static final long SEQUENCE_MASK = -1L ^ (-1L << SEQUENCE_BITS);
    /**
     * 集群机器ID位移位数，10
     */
    protected static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS;
    /**
     * 时间戳ID位移位数，22
     */
    protected static final long TIMESTAMP_LEFT_SHIFT = SEQUENCE_BITS + DATACENTER_ID_BITS;
    /**
     * ID时间戳起点：2017-6-25 0:0:0.000<br/>
     * (1<<41 ) / 1000 / 3600 / 24 / 365 ≈ 69.73<br/>
     * 可支持到2087-3-1 15:47:35
     */
    protected static final long ID_EPOCH = 1498320000000L;
    /**
     * 随机数生成器
     */
    protected static final Random RANDOM = new Random();
    
}

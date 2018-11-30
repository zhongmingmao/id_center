# 1. 简介

基于`Twitter-Snowflake`算法和`Zookeeper临时节点`特性实现的《ID生成器》（`64bit自增ID`）

# 2. Twitter-Snowflake

![snowflake.png](https://id-center-1253868755.cos.ap-guangzhou.myqcloud.com/snowflake.png)

第1个bit固定为0，时间戳、工作机器ID、序列号占用的bit位数是可变的，依据具体的业务而定

## 2.1 时间戳
单位为`毫秒`，`41bit ≈ 69.73年`，可以修改`时间戳起点`（代码中为`ID_EPOCH`）来延长使用年限

## 2.2 工作机器ID
在代码中，`工作机器ID`实际上对应的是`JVM进程`，采用`Zookeeper临时节点`来控制集群机器数量

## 2.3 序列号
表示一台工作机器（JVM进程）在一毫秒内最多产生多少个ID，如果达到上限则等待至下一毫秒

# 3. 容错处理
1. ID生成器依赖于Zookeeper，启动时无法连接到Zookeeper，则不断尝试重连
2. 运行过程中发生网络异常，会尝试重连Zookeeper
3. 会话过期或连接丢失，ID生成器进入休眠（suspend）状态，不继续工作，直至与Zookeeper的连接恢复
4. 与Zookeeper的连接重新建立后，会重新获取新的工作机器ID，因为在连接断开期间，原本的工作机器ID会被其他客户端申请并使用

# 4. 简单性能测试
```
$ mvn clean test

# 真实Zookeeper测试结果：10 thread get 300000000 ids from zk ,take 23s , speed:1304[max:4095]
# TestServer测试结果：10 thread get 300000000 ids from zk ,take 18s , speed:1666[max:4095]
# 性能已经能满足大部分应用场景了
```

# 5. 实际业务预估
1. 系统运行时长：10年，`10*365*24*3600*1000 ~ 39 bit`
2. 业务线：30，`30 ~ 5 bit`
3. 机房数量：4，`4 ~ 2 bit`
4. 单机房机器数量：100，`100 ~ 7 bit`
5. QPS：20W，`20W/1000=200 ~ 8 bit`

![id_center.png](https://id-center-1253868755.cos.ap-guangzhou.myqcloud.com/id_center.png)

# 6. 存在的问题
1. 强依赖于机器时钟，优化措施：周期性记录机器最新时钟，开始工作前先通过最相关判断（`current>=least && (current-sum(least)/(n-1)) < M`）
2. 强依赖于Zookeeper，而Zookeeper是`CP`类型（放弃了部分可用性）
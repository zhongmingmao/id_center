package me.zhongmingmao.Id.generator;

import java.io.Closeable;

/**
 * ID生成器接口
 */
public interface IDGenerator extends Closeable {
    
    
    /**
     * 返回下一个唯一ID
     *
     * @return
     */
    long getId();
}

package com.suwako.northstarplanetaddon.venus.blockentity;

/**
 * 掉方块时需要把内部物品吐出来的机器。
 *
 * <p>{@link com.suwako.northstarplanetaddon.venus.block.AbstractMachineBlock#onRemove} 统一处理，
 * 避免每加一台机器都要在方块里加一个 instanceof 分支。</p>
 */
public interface ContentDropping {

    void dropContents();
}

package com.suwako.northstarplanetaddon.venus.machine;

/**
 * 压缩空气储量（设计草案第 8 章的气动侧）。
 *
 * <p>统一换算：**1 bar = 1000 mB**。所以压力罐 8000 mB = 8 bar，
 * 与草案里「压力罐容量 8000 mB」「高压锅需 8 bar」两处数值自洽。</p>
 *
 * <p>这一类刻意不依赖 Create：应力（SU/RPM）由后续的
 * {@code compat/create} 适配层另外提供，气动与旋转解耦，
 * 这样机器逻辑在只有本模组时也能完整运行。</p>
 */
public final class PneumaticStorage {

    public static final int MILLIBUCKET_PER_BAR = 1000;

    private final int capacity;
    private int air;

    public PneumaticStorage(int capacity) {
        this.capacity = Math.max(0, capacity);
    }

    public int getAir() {
        return this.air;
    }

    public int getCapacity() {
        return this.capacity;
    }

    public float getBar() {
        return this.air / (float) MILLIBUCKET_PER_BAR;
    }

    public boolean isEmpty() {
        return this.air <= 0;
    }

    public boolean isFull() {
        return this.air >= this.capacity;
    }

    /**
     * 注入压缩空气。
     *
     * @return 实际接受量
     */
    public int receive(int amount, boolean simulate) {
        if (amount <= 0 || this.capacity <= 0) {
            return 0;
        }
        int accepted = Math.min(amount, this.capacity - this.air);
        if (accepted <= 0) {
            return 0;
        }
        if (!simulate) {
            this.air += accepted;
        }
        return accepted;
    }

    /**
     * 抽出压缩空气。
     *
     * @return 实际抽出量
     */
    public int extract(int amount, boolean simulate) {
        if (amount <= 0) {
            return 0;
        }
        int drained = Math.min(amount, this.air);
        if (drained <= 0) {
            return 0;
        }
        if (!simulate) {
            this.air -= drained;
        }
        return drained;
    }

    public boolean hasBar(float bar) {
        return this.air >= bar * MILLIBUCKET_PER_BAR;
    }

    public void setAir(int air) {
        this.air = Math.max(0, Math.min(this.capacity, air));
    }

    /** 供配置热调：容量变化时裁剪当前储量。 */
    public int clampToCapacity(int newCapacity) {
        if (this.air > newCapacity) {
            this.air = newCapacity;
        }
        return this.air;
    }
}

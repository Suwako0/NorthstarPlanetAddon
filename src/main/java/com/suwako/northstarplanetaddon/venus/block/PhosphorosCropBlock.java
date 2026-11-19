package com.suwako.northstarplanetaddon.venus.block;

import com.suwako.northstarplanetaddon.venus.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.function.Supplier;

/**
 * 三种作物的共享实现（设计草案 7.3 节）。
 *
 * <p>共用一套 0–3 的 age 属性与十字模型，只是收获物与生长时长不同：</p>
 * <ul>
 *   <li>巨菌 {@code giant_fungus_crop}：8 分钟，收获菌丝纤维 ×2–4；</li>
 *   <li>酸渍作物 {@code acid_vegetable_crop}：12 分钟，收获酸渍脆蔬 ×2；</li>
 *   <li>云藻 {@code cloud_algae_crop}：6 分钟，收获云藻冻 ×3。</li>
 * </ul>
 *
 * <p>生长时长按「随机刻」折算：三项都只在 {@link #randomTick} 里推进，
 * 数值来自草案的分钟数 / 平均随机刻间隔。</p>
 */
public class PhosphorosCropBlock extends CropBlock {

    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 3);
    private static final VoxelShape[] SHAPES = {
            Block.box(0.0D, 0.0D, 0.0D, 16.0D, 4.0D, 16.0D),
            Block.box(0.0D, 0.0D, 0.0D, 16.0D, 8.0D, 16.0D),
            Block.box(0.0D, 0.0D, 0.0D, 16.0D, 12.0D, 16.0D),
            Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D)
    };

    private final Supplier<? extends Item> seed;
    private final Supplier<? extends Item> harvest;
    private final int harvestMin;
    private final int harvestMax;
    /** 每次随机刻推进一级的概率分子/分母（越大越慢）。 */
    private final int growthChance;

    public PhosphorosCropBlock(Properties properties, Supplier<? extends Item> seed,
                               Supplier<? extends Item> harvest, int harvestMin, int harvestMax,
                               int growthChance) {
        super(properties);
        this.seed = seed;
        this.harvest = harvest;
        this.harvestMin = harvestMin;
        this.harvestMax = harvestMax;
        this.growthChance = growthChance;
        this.registerDefaultState(this.stateDefinition.any().setValue(this.getAgeProperty(), 0));
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return this.seed.get();
    }

    @Override
    public IntegerProperty getAgeProperty() {
        return AGE;
    }

    @Override
    public int getMaxAge() {
        return 3;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[Math.min(state.getValue(AGE), SHAPES.length - 1)];
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getRawBrightness(pos, 0) < 8 && !this.isFungus()) {
            return;
        }
        if (random.nextInt(this.growthChance) != 0) {
            return;
        }
        int age = this.getAge(state);
        if (age < this.getMaxAge()) {
            level.setBlock(pos, state.setValue(AGE, age + 1), 2);
        }
    }

    /** 只有巨菌允许低光生长（草案：光照任意）。 */
    private boolean isFungus() {
        return this.harvest.get() == ModItems.FUNGUS_FIBER.get();
    }

    @Override
    public void growCrops(net.minecraft.world.level.Level level, BlockPos pos, BlockState state) {
        int age = this.getAge(state) + this.getBonemealAgeIncrease(level);
        level.setBlock(pos, state.setValue(AGE, Math.min(age, this.getMaxAge())), 2);
    }

    /** 收获数量（成熟时）。 */
    public ItemStack rollHarvest(RandomSource random) {
        int count = this.harvestMin + random.nextInt(Math.max(1, this.harvestMax - this.harvestMin + 1));
        return new ItemStack(this.harvest.get(), count);
    }

    @Override
    public boolean isValidBonemealTarget(net.minecraft.world.level.LevelReader level, BlockPos pos,
                                         BlockState state) {
        return this.getAge(state) < this.getMaxAge();
    }
}

package com.suwako.northstarplanetaddon.mars.client;

import com.suwako.northstarplanetaddon.NorthstarPlanetAddon;

import com.suwako.northstarplanetaddon.mars.MarsModule;
import com.suwako.northstarplanetaddon.mars.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/**
 * 沙尘暴的客户端表现：飞舞的沙尘粒子、风声、屏幕沙尘罩。
 *
 * <p>v1.12 新增。起因是用户反馈「沙尘暴仍然没有视野缩小，也没有特效显示」——</p>
 * <ul>
 *   <li><b>视野缩小</b>：真因是 {@code RenderFog} 必须 cancel 才生效，已修（见
 *       {@link SandstormFogHandler} 的类注释）；</li>
 *   <li><b>特效</b>：v1.11 只做了雾与 HUD，**一个粒子都没放**。这里补齐 ——
 *       不管有没有戴护目镜都能看见沙尘在飞（护目镜只降低密度），
 *       再叠一层屏幕沙尘罩与风声，保证「沙尘暴来了」这件事一定看得见。</li>
 * </ul>
 *
 * <p>所有表现都是纯客户端，不参与任何判定；服务端状态来自
 * {@link ClientSandstormState}（服务端每秒广播一次）。</p>
 */
@EventBusSubscriber(modid = NorthstarPlanetAddon.MODID, value = Dist.CLIENT)
public final class SandstormClientEffects {

    /** 每 {@value #PARTICLE_INTERVAL} tick 撒一次粒子；数量按强度分档，戴护目镜降到 1/4。 */
    private static final int PARTICLE_INTERVAL = 2;
    private static final int CALM_PARTICLES = 6;
    private static final int INTENSE_PARTICLES = 14;
    /** 粒子生成范围（格，以玩家为中心的方盒边长的一半）。 */
    private static final double SPAWN_RADIUS = 14.0D;
    private static final double SPAWN_HEIGHT = 5.0D;
    /** 风速（格/tick）——只影响粒子漂移，不影响玩家。 */
    private static final double MIN_WIND_SPEED = 0.35D;
    private static final double MAX_WIND_SPEED = 0.75D;
    /** 风声间隔（tick）。 */
    private static final int SOUND_INTERVAL = 70;

    /** 屏幕沙尘罩（ARGB）：普通 / 强化。 */
    private static final int TINT_CALM = 0x2E7A5228;
    private static final int TINT_INTENSE = 0x557A4A20;

    private static int tick;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        LocalPlayer player = minecraft.player;
        if (level == null || player == null || minecraft.isPaused()) {
            return;
        }
        if (!ClientSandstormState.active()) {
            tick = 0;
            return;
        }
        tick++;

        boolean goggles = player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.SANDSTORM_GOGGLES.get());
        int count = ClientSandstormState.intense() ? INTENSE_PARTICLES : CALM_PARTICLES;
        if (goggles) {
            count = Math.max(1, count / 4);
        }

        if (tick % PARTICLE_INTERVAL == 0) {
            // 风向：0° = 正北（-Z），顺时针；粒子朝风吹的方向漂
            double radians = Math.toRadians(ClientSandstormState.windDegrees());
            double windX = Math.sin(radians);
            double windZ = -Math.cos(radians);
            RandomSource random = level.random;
            for (int i = 0; i < count; i++) {
                double x = player.getX() + (random.nextDouble() - 0.5D) * 2.0D * SPAWN_RADIUS;
                double y = player.getY() + random.nextDouble() * SPAWN_HEIGHT - 1.0D;
                double z = player.getZ() + (random.nextDouble() - 0.5D) * 2.0D * SPAWN_RADIUS;
                double speed = MIN_WIND_SPEED + random.nextDouble() * (MAX_WIND_SPEED - MIN_WIND_SPEED);
                level.addParticle(ParticleTypes.WHITE_ASH, x, y, z, windX * speed, 0.01D, windZ * speed);
                if (i % 3 == 0) {
                    level.addParticle(ParticleTypes.SMOKE, x, y, z,
                            windX * speed * 0.5D, 0.02D, windZ * speed * 0.5D);
                }
            }
        }

        if (tick % SOUND_INTERVAL == 0) {
            level.playLocalSound(player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ELYTRA_FLYING, SoundSource.WEATHER, 0.4F, 0.55F, false);
        }
    }

    /**
     * 屏幕沙尘罩：在 HUD 之前铺一层半透明沙色。
     *
     * <p>为什么不只靠雾：雾只影响地形，抬头看天或站在基地窗口时几乎看不出来；
     * 罩一层屏幕色是「视野受阻」最直观的提示。戴护目镜时完全不铺（护目镜的价值就在于视野）。</p>
     */
    @SubscribeEvent
    public static void onRenderGuiPre(RenderGuiEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.options.hideGui || !ClientSandstormState.active()) {
            return;
        }
        if (player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.SANDSTORM_GOGGLES.get())) {
            return;
        }
        GuiGraphics graphics = event.getGuiGraphics();
        graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(),
                ClientSandstormState.intense() ? TINT_INTENSE : TINT_CALM);
    }

    private SandstormClientEffects() {
    }
}

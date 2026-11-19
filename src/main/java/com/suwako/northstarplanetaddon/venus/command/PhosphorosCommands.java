package com.suwako.northstarplanetaddon.venus.command;

import com.suwako.northstarplanetaddon.venus.combat.DamageKind;
import com.suwako.northstarplanetaddon.venus.combat.DamageTrace;
import com.suwako.northstarplanetaddon.venus.entity.boss.AbstractBossEntity;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /phosphoros debug dps|boss|classify}（设计草案第 14 章调试命令，仅 OP）。
 */
public final class PhosphorosCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("northstar_planet_addon")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("debug")
                        .then(Commands.literal("boss").executes(context -> debugBoss(context.getSource().getPlayerOrException())))
                        .then(Commands.literal("dps").executes(context -> debugDps(context.getSource().getPlayerOrException())))
                        .then(Commands.literal("classify").executes(context -> debugClassify(context.getSource().getPlayerOrException())))));
    }

    private static AbstractBossEntity findBoss(ServerPlayer player) {
        return player.level().getEntitiesOfClass(AbstractBossEntity.class,
                        player.getBoundingBox().inflate(64.0D)).stream()
                .findFirst().orElse(null);
    }

    private static int debugBoss(ServerPlayer player) {
        AbstractBossEntity boss = findBoss(player);
        if (boss == null) {
            player.sendSystemMessage(Component.literal("§c[VenusModule] 64 格内没有 Boss"));
            return 0;
        }
        player.sendSystemMessage(Component.literal("§6[VenusModule] " + boss.getDisplayName().getString()));
        player.sendSystemMessage(Component.literal("§7" + boss.debugLines()));
        return 1;
    }

    private static int debugDps(ServerPlayer player) {
        AbstractBossEntity boss = findBoss(player);
        if (boss == null) {
            player.sendSystemMessage(Component.literal("§c[VenusModule] 64 格内没有 Boss"));
            return 0;
        }
        for (DamageKind kind : new DamageKind[]{DamageKind.BALLISTIC, DamageKind.MELEE}) {
            float[] data = boss.debugDps(player.getUUID(), kind);
            player.sendSystemMessage(Component.literal("§7" + kind + ": 本窗口已用 §f"
                    + String.format("%.1f", data[0]) + "§7 / 上限 §f" + String.format("%.1f", data[1])));
        }
        return 1;
    }

    private static int debugClassify(ServerPlayer player) {
        AbstractBossEntity boss = findBoss(player);
        if (boss == null) {
            player.sendSystemMessage(Component.literal("§c[VenusModule] 64 格内没有 Boss"));
            return 0;
        }
        DamageTrace trace = boss.getLastTrace();
        player.sendSystemMessage(Component.literal("§7最近一次结算：" + trace.kind()
                + " raw=" + String.format("%.2f", trace.raw())
                + " plate=" + String.format("%.2f", trace.plate())
                + " core=" + String.format("%.2f", trace.core())
                + " [" + trace.note() + "]"));
        return 1;
    }

    private PhosphorosCommands() {
    }
}

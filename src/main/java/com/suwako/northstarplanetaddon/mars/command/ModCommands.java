package com.suwako.northstarplanetaddon.mars.command;

import com.suwako.northstarplanetaddon.NorthstarPlanetAddon;

import com.suwako.northstarplanetaddon.mars.MarsModule;
import com.suwako.northstarplanetaddon.mars.world.Sandstorm;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /northstarmars sandstorm ...}（别名 {@code /sandstorm}）。
 *
 * <p>自然沙尘暴平均十几分钟才来一场，验收时不该靠等 —— 这条命令可以直接开一场、
 * 开一场强化的、立刻停、或者查当前状态。需要权限等级 2（单人存档的作弊权限 / 管理员）。</p>
 */
@EventBusSubscriber(modid = NorthstarPlanetAddon.MODID)
public final class ModCommands {

    private static final String ARG_SECONDS = "seconds";

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> root = sandstorm();
        event.getDispatcher().register(Commands.literal("northstarmars").then(root));
        event.getDispatcher().register(Commands.literal("sandstorm").then(sandstorm()));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> sandstorm() {
        return Commands.literal("sandstorm")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("start")
                        .executes(ctx -> start(ctx, false, Sandstorm.DEFAULT_DURATION))
                        .then(Commands.argument(ARG_SECONDS, IntegerArgumentType.integer(10, 3600))
                                .executes(ctx -> start(ctx, false, seconds(ctx) * 20))))
                .then(Commands.literal("intense")
                        .executes(ctx -> start(ctx, true, Sandstorm.DEFAULT_DURATION))
                        .then(Commands.argument(ARG_SECONDS, IntegerArgumentType.integer(10, 3600))
                                .executes(ctx -> start(ctx, true, seconds(ctx) * 20))))
                .then(Commands.literal("stop").executes(ModCommands::stop))
                .then(Commands.literal("status").executes(ModCommands::status));
    }

    private static int seconds(CommandContext<CommandSourceStack> ctx) {
        return IntegerArgumentType.getInteger(ctx, ARG_SECONDS);
    }

    private static int start(CommandContext<CommandSourceStack> ctx, boolean strong, int ticks) {
        ServerLevel mars = Sandstorm.marsLevel(ctx.getSource().getServer());
        if (mars == null) {
            ctx.getSource().sendFailure(Component.translatable(
                    "command.northstar_planet_addon.sandstorm.no_mars"));
            return 0;
        }
        Sandstorm.trigger(mars, ticks, strong, Sandstorm.Origin.COMMAND);
        int secs = ticks / 20;
        // 沙尘暴是 northstar:mars 的维度级状态 —— 执行者若不在火星，得明确告诉他看不到效果
        boolean executorOnMars = ctx.getSource().getEntity() instanceof ServerPlayer serverPlayer
                && Sandstorm.isMars(serverPlayer.level());
        ctx.getSource().sendSuccess(() -> Component.translatable(
                executorOnMars
                        ? "command.northstar_planet_addon.sandstorm.started"
                        : "command.northstar_planet_addon.sandstorm.started_elsewhere",
                Component.translatable(strong
                        ? "command.northstar_planet_addon.sandstorm.label.intense"
                        : "command.northstar_planet_addon.sandstorm.label.normal"),
                secs), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int stop(CommandContext<CommandSourceStack> ctx) {
        ServerLevel mars = Sandstorm.marsLevel(ctx.getSource().getServer());
        if (mars == null) {
            ctx.getSource().sendFailure(Component.translatable(
                    "command.northstar_planet_addon.sandstorm.no_mars"));
            return 0;
        }
        Sandstorm.stop(mars);
        ctx.getSource().sendSuccess(() -> Component.translatable(
                "command.northstar_planet_addon.sandstorm.stopped"), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int status(CommandContext<CommandSourceStack> ctx) {
        ServerLevel mars = Sandstorm.marsLevel(ctx.getSource().getServer());
        if (mars == null) {
            ctx.getSource().sendFailure(Component.translatable(
                    "command.northstar_planet_addon.sandstorm.no_mars"));
            return 0;
        }
        if (!Sandstorm.isActive(mars)) {
            ctx.getSource().sendSuccess(() -> Component.translatable(
                    "command.northstar_planet_addon.sandstorm.status.idle"), false);
            return Command.SINGLE_SUCCESS;
        }
        ctx.getSource().sendSuccess(() -> Component.translatable(
                "command.northstar_planet_addon.sandstorm.status.active",
                Sandstorm.remainingTicks(mars) / 20,
                Component.translatable(Sandstorm.isIntense(mars)
                        ? "command.northstar_planet_addon.sandstorm.label.intense"
                        : "command.northstar_planet_addon.sandstorm.label.normal"),
                Component.translatable("hud.northstar_planet_addon.direction."
                        + Sandstorm.windKey(Sandstorm.windDegrees())),
                Sandstorm.windDegrees()), false);
        return Command.SINGLE_SUCCESS;
    }

    private ModCommands() {
    }
}

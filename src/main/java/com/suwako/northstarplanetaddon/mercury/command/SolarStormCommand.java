package com.suwako.northstarplanetaddon.mercury.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.suwako.northstarplanetaddon.mercury.MercuryModule;
import com.suwako.northstarplanetaddon.mercury.world.SolarStormData;
import com.suwako.northstarplanetaddon.mercury.world.SolarStormManager;
import com.suwako.northstarplanetaddon.mercury.world.SolarStormSettings;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /solarstorm start|stop|query} — the manual trigger for Mercury's weather.
 * <p>
 * The command can be run from anywhere (the console included); the storm itself only ever exists in
 * {@code northstar:mercury}, which is why the feedback says so explicitly.
 */
public final class SolarStormCommand {

    private static final String SECONDS = "seconds";

    private SolarStormCommand() {
    }

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("solarstorm")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("start")
                        .executes(context -> start(context, SolarStormSettings.DEFAULT_COMMAND_DURATION))
                        .then(Commands.literal("infinite")
                                .executes(context -> start(context, 0)))
                        .then(Commands.argument(SECONDS, IntegerArgumentType.integer(1, 3600))
                                .executes(context -> start(context,
                                        IntegerArgumentType.getInteger(context, SECONDS) * 20))))
                .then(Commands.literal("stop")
                        .executes(SolarStormCommand::stop))
                .then(Commands.literal("query")
                        .executes(SolarStormCommand::query))
                .then(Commands.literal("impact")
                        .executes(SolarStormCommand::impact)));
    }

    private static int start(CommandContext<CommandSourceStack> context, int durationTicks) {
        CommandSourceStack source = context.getSource();
        SolarStormManager.start(source.getServer(), durationTicks);

        Component when = durationTicks <= 0
                ? Component.translatable("commands." + MercuryModule.NAMESPACE + ".solar_storm.forever")
                : Component.translatable("commands." + MercuryModule.NAMESPACE + ".solar_storm.duration",
                        durationTicks / 20);
        source.sendSuccess(() -> Component.translatable(
                "commands." + MercuryModule.NAMESPACE + ".solar_storm.started", when), true);
        return 1;
    }

    private static int stop(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        boolean wasActive = SolarStormManager.stop(source.getServer());

        if (wasActive) {
            source.sendSuccess(() -> Component.translatable(
                    "commands." + MercuryModule.NAMESPACE + ".solar_storm.stopped"), true);
        } else {
            source.sendFailure(Component.translatable(
                    "commands." + MercuryModule.NAMESPACE + ".solar_storm.not_running"));
        }
        return wasActive ? 1 : 0;
    }

    private static int query(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        SolarStormData data = SolarStormManager.state(source.getServer());

        if (!data.isActive()) {
            source.sendSuccess(() -> Component.translatable(
                    "commands." + MercuryModule.NAMESPACE + ".solar_storm.query_idle"), false);
            return 0;
        }

        long remaining = data.remaining(source.getServer().overworld().getGameTime());
        Component left = remaining < 0L
                ? Component.translatable("commands." + MercuryModule.NAMESPACE + ".solar_storm.forever")
                : Component.translatable("commands." + MercuryModule.NAMESPACE + ".solar_storm.remaining",
                        remaining / 20);
        source.sendSuccess(() -> Component.translatable(
                "commands." + MercuryModule.NAMESPACE + ".solar_storm.query_active", left), false);
        return 1;
    }

    /**
     * Calls one meteorite down over the source's position. Purely a convenience for builders and
     * server admins (and for reproducing an impact without waiting for a storm) — it obeys the same
     * "Mercury only" rule as the weather itself.
     */
    private static int impact(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();

        if (!level.dimension().equals(SolarStormManager.MERCURY)) {
            source.sendFailure(Component.translatable(
                    "commands." + MercuryModule.NAMESPACE + ".solar_storm.impact_wrong_dimension"));
            return 0;
        }

        BlockPos target = BlockPos.containing(source.getPosition());
        SolarStormManager.dropMeteor(level, target.getX() + 0.5D, target.getZ() + 0.5D);
        source.sendSuccess(() -> Component.translatable(
                "commands." + MercuryModule.NAMESPACE + ".solar_storm.impact_called",
                target.getX(), target.getZ()), true);
        return 1;
    }
}

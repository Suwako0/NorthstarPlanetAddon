package com.possible_triangle.create_jetpack.config

import com.possible_triangle.create_jetpack.Constants
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.server.level.ServerPlayer
import net.neoforged.neoforge.common.ModConfigSpec
import net.neoforged.neoforge.common.ModConfigSpec.Builder
import net.neoforged.neoforge.network.PacketDistributor
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent
import net.neoforged.neoforge.network.handling.IPayloadContext
import org.apache.commons.lang3.tuple.Pair

public object Configs {
   public final var SERVER_SPEC: ModConfigSpec
      private set

   private final var LOCAL_SERVER: ServerConfig
   internal final var SYNCED_SERVER: IServerConfig?

   public final val SERVER: IServerConfig
      public final get() {
         var var10000: IServerConfig = SYNCED_SERVER;
         if (SYNCED_SERVER == null) {
            var10000 = LOCAL_SERVER;
         }

         return var10000;
      }


   public final var CLIENT_SPEC: ModConfigSpec
      private set

   public final var CLIENT: ClientConfig
      private set

   public fun syncConfig() {
      Constants.LOGGER.debug("Sending server config all players");
      PacketDistributor.sendToAllPlayers(new SyncConfigMessage(LOCAL_SERVER), new CustomPacketPayload[0]);
   }

   public fun syncConfig(player: ServerPlayer) {
      Constants.LOGGER.debug("Sending server config to ${player.getScoreboardName()}");
      PacketDistributor.sendToPlayer(player, new SyncConfigMessage(LOCAL_SERVER), new CustomPacketPayload[0]);
   }

   @JvmStatic
   fun `_init_$lambda$0`(it: Builder): ServerConfig {
      return new ServerConfig(it);
   }

   @JvmStatic
   fun `_init_$lambda$2`(it: Builder): ClientConfig {
      return new ClientConfig(it);
   }

   @JvmStatic
   fun {
      var `$this$_init__u24lambda_u243`: Pair = new Builder().configure(Configs::_init_$lambda$0);
      var var10000: Any = `$this$_init__u24lambda_u243`.getLeft();
      LOCAL_SERVER = var10000 as ServerConfig;
      var10000 = `$this$_init__u24lambda_u243`.getRight();
      SERVER_SPEC = var10000 as ModConfigSpec;
      `$this$_init__u24lambda_u243` = new Builder().configure(Configs::_init_$lambda$2);
      var10000 = `$this$_init__u24lambda_u243`.getLeft();
      CLIENT = var10000 as ClientConfig;
      var10000 = `$this$_init__u24lambda_u243`.getRight();
      CLIENT_SPEC = var10000 as ModConfigSpec;
   }

   public object Network {
      public fun register(event: RegisterPayloadHandlersEvent) {
         event.registrar("2.0")
            .playToClient(SyncConfigMessage.Companion.getTYPE().type(), SyncConfigMessage.Companion.getTYPE().codec(), Configs.Network::register$lambda$0);
      }

      @JvmStatic
      fun `register$lambda$0`(message: SyncConfigMessage, var1: IPayloadContext) {
         message.handle();
      }
   }
}

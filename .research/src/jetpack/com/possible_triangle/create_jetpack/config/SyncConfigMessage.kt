package com.possible_triangle.create_jetpack.config

import com.possible_triangle.create_jetpack.Constants
import kotlin.jvm.functions.Function1
import kotlin.jvm.functions.Function2
import kotlin.jvm.internal.SourceDebugExtension
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.TypeAndCodec

public class SyncConfigMessage(config: IServerConfig) : CustomPacketPayload {
   private final val config: IServerConfig

   init {
      this.config = config;
   }

   public open fun type(): Type<SyncConfigMessage> {
      val var10000: Type = TYPE.type();
      return var10000;
   }

   public fun handle() {
      Constants.LOGGER.debug("Hover speed: ${this.config.getHoverSpeed()}");
      Configs.INSTANCE.setSYNCED_SERVER$create_jetpack_forge_5_2_1(this.config);
   }

   @SourceDebugExtension(["SMAP\nSyncConfigMessage.kt\nKotlin\n*S Kotlin\n*F\n+ 1 SyncConfigMessage.kt\ncom/possible_triangle/create_jetpack/config/SyncConfigMessage$Companion\n+ 2 fake.kt\nkotlin/jvm/internal/FakeKt\n*L\n1#1,68:1\n1#2:69\n*E\n"])
   public companion object {
      public final val TYPE: TypeAndCodec<FriendlyByteBuf, SyncConfigMessage>

      private fun decode(buf: FriendlyByteBuf): SyncConfigMessage {
         val var10002: Int = buf.readInt();
         val var10003: Int = buf.readInt();
         val var10004: Double = buf.readDouble();
         val var10005: Double = buf.readDouble();
         val var10006: Double = buf.readDouble();
         val var10007: Double = buf.readDouble();
         val var10008: Double = buf.readDouble();
         val var10009: Double = buf.readDouble();
         val var10010: Int = if (buf.readBoolean()) buf.readVarInt() else null;
         val var10013: java.util.List = buf.readList(SyncConfigMessage.Companion::decode$lambda$0);
         return new SyncConfigMessage(
            new SyncedConfig(
               var10002, var10003, var10004, var10005, var10006, var10007, var10008, var10009, var10010, new EnchantmentConfig(var10013, buf.readBoolean())
            )
         );
      }

      private fun encode(buf: FriendlyByteBuf, message: SyncConfigMessage): FriendlyByteBuf {
         buf.writeInt(SyncConfigMessage.access$getConfig$p(message).getSecondsPerTank());
         buf.writeInt(SyncConfigMessage.access$getConfig$p(message).getSecondsPerTankHover());
         buf.writeDouble(SyncConfigMessage.access$getConfig$p(message).getHorizontalSpeed());
         buf.writeDouble(SyncConfigMessage.access$getConfig$p(message).getVerticalSpeed());
         buf.writeDouble(SyncConfigMessage.access$getConfig$p(message).getAcceleration());
         buf.writeDouble(SyncConfigMessage.access$getConfig$p(message).getHoverSpeed());
         buf.writeDouble(SyncConfigMessage.access$getConfig$p(message).getSwimModifier());
         buf.writeDouble(SyncConfigMessage.access$getConfig$p(message).getElytraBoost());
         buf.writeBoolean(SyncConfigMessage.access$getConfig$p(message).getHeightAboveGroundLimit() != null);
         val var10000: Int = SyncConfigMessage.access$getConfig$p(message).getHeightAboveGroundLimit();
         if (var10000 != null) {
            buf.writeVarInt(var10000.intValue());
         }

         val it: EnchantmentConfig = SyncConfigMessage.access$getConfig$p(message).getEnchantments();
         buf.writeCollection(it.getIds(), SyncConfigMessage.Companion::encode$lambda$0$0$0);
         return buf.writeBoolean(it.isBlacklist());
      }

      @JvmStatic
      fun `decode$lambda$0`(`$tmp0`: Function1, p0: Any): java.lang.String {
         return `$tmp0`.invoke(p0) as java.lang.String;
      }

      @JvmStatic
      fun `encode$lambda$0$0$0`(`$tmp0`: Function2, p0: Any, p1: java.lang.String) {
         `$tmp0`.invoke(p0, p1);
      }
   }
}

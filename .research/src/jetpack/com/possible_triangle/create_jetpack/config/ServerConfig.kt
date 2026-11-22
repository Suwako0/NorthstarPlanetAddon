package com.possible_triangle.create_jetpack.config

import kotlin.jvm.internal.SourceDebugExtension
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue
import net.neoforged.neoforge.common.ModConfigSpec.Builder
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue
import net.neoforged.neoforge.common.ModConfigSpec.DoubleValue
import net.neoforged.neoforge.common.ModConfigSpec.IntValue

@SourceDebugExtension(["SMAP\nServerConfig.kt\nKotlin\n*S Kotlin\n*F\n+ 1 ServerConfig.kt\ncom/possible_triangle/create_jetpack/config/ServerConfig\n+ 2 fake.kt\nkotlin/jvm/internal/FakeKt\n*L\n1#1,90:1\n1#2:91\n*E\n"])
public class ServerConfig(builder: Builder) : IServerConfig {
   private final val secondsPerTankValue: IntValue

   public open val secondsPerTank: Int
      public open get() {
         return this.secondsPerTankValue.get() as Int;
      }


   private final val secondsPerTankHoverValue: IntValue

   public open val secondsPerTankHover: Int
      public open get() {
         return this.secondsPerTankHoverValue.get() as Int;
      }


   private final val horizontalSpeedValue: DoubleValue

   public open val horizontalSpeed: Double
      public open get() {
         return this.horizontalSpeedValue.get() as java.lang.Double;
      }


   private final val verticalSpeedValue: DoubleValue

   public open val verticalSpeed: Double
      public open get() {
         return this.verticalSpeedValue.get() as java.lang.Double;
      }


   private final val accelerationValue: DoubleValue

   public open val acceleration: Double
      public open get() {
         return this.accelerationValue.get() as java.lang.Double;
      }


   private final val hoverSpeedValue: DoubleValue

   public open val hoverSpeed: Double
      public open get() {
         return this.hoverSpeedValue.get() as java.lang.Double;
      }


   private final val swimModifierValue: DoubleValue

   public open val swimModifier: Double
      public open get() {
         return this.swimModifierValue.get() as java.lang.Double;
      }


   private final val elytraBoostValue: DoubleValue

   public open val elytraBoost: Double
      public open get() {
         return this.elytraBoostValue.get() as java.lang.Double;
      }


   private final val enchantmentsList: ConfigValue<MutableList<out String>>
   private final val enchantmentsIsBlacklist: BooleanValue
   private final val heightAboveGroundLimitValue: ConfigValue<Int>

   public open val heightAboveGroundLimit: Int?
      public open get() {
         val var1: Any = this.heightAboveGroundLimitValue.get();
         return (if (var1 as Int >= 0) var1 else null) as Int;
      }


   public open val enchantments: EnchantmentConfig
      public open get() {
         var var10002: Any = this.enchantmentsList.get();
         var10002 = var10002 as java.util.List;
         val var10003: Any = this.enchantmentsIsBlacklist.get();
         return new EnchantmentConfig((java.util.List<java.lang.String>)var10002, var10003 as java.lang.Boolean);
      }


   init {
      this.secondsPerTankValue = builder.defineInRange("air.seconds_per_tank", 450, 1, Integer.MAX_VALUE);
      this.secondsPerTankHoverValue = builder.defineInRange("air.seconds_per_tank_hover", 900, 1, Integer.MAX_VALUE);
      this.horizontalSpeedValue = builder.defineInRange("speed.horizontal", 0.02, 0.01, 100.0);
      this.verticalSpeedValue = builder.defineInRange("speed.vertical", 0.4, 0.01, 100.0);
      this.accelerationValue = builder.defineInRange("speed.acceleration", 0.6, 0.01, 100.0);
      this.hoverSpeedValue = builder.defineInRange("speed.hover_descend", -0.03, -100.0, 0.0);
      this.swimModifierValue = builder.defineInRange("speed.swim_modifier", 1.8, 0.0, 100.0);
      this.elytraBoostValue = builder.defineInRange("features.elytra_boost", 1.25, 1.0, 100.0);
      this.enchantmentsList = builder.defineListAllowEmpty(
         "enchantments.list", CollectionsKt.emptyList(), ServerConfig::enchantmentsList$lambda$0, ServerConfig::enchantmentsList$lambda$1
      );
      this.enchantmentsIsBlacklist = builder.define("enchantments.is_blacklist", true);
      this.heightAboveGroundLimitValue = builder.define("heightAboveGroundLimit", -1);
   }

   @JvmStatic
   fun `enchantmentsList$lambda$0`(): java.lang.String {
      return "";
   }

   @JvmStatic
   fun `enchantmentsList$lambda$1`(it: Any): Boolean {
      return true;
   }
}

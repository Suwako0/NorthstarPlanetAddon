package com.possible_triangle.create_jetpack.config

import java.util.Calendar
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue
import net.neoforged.neoforge.common.ModConfigSpec.Builder
import net.neoforged.neoforge.common.ModConfigSpec.DoubleValue
import net.neoforged.neoforge.common.ModConfigSpec.IntValue

public class ClientConfig(builder: Builder) {
   public final val showOverlay: BooleanValue
   public final val overlayX: IntValue
   public final val overlayY: IntValue
   public final val overlayScale: DoubleValue
   private final val seasonalEffects: BooleanValue

   public final val spawnSnowParticles: Boolean
      public final get() {
         return isChristmas && this.seasonalEffects.get() as java.lang.Boolean;
      }


   init {
      this.showOverlay = builder.define("overlay.enabled", true);
      builder.comment("Use negative values to position relative to the right/bottom of the screen");
      this.overlayX = builder.defineInRange("overlay.position.x", 6, Integer.MIN_VALUE, Integer.MAX_VALUE);
      this.overlayY = builder.defineInRange("overlay.position.y", 6, Integer.MIN_VALUE, Integer.MAX_VALUE);
      this.overlayScale = builder.defineInRange("overlay.scale", 1.0, 0.0, java.lang.Double.MAX_VALUE);
      this.seasonalEffects = builder.define("effects.seasonal", true);
   }

   public companion object {
      private final val calendar: Calendar
      private final val isChristmas: Boolean
   }
}

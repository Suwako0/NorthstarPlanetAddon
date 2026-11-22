package com.possible_triangle.create_jetpack.client

import com.mojang.blaze3d.systems.RenderSystem
import com.possible_triangle.create_jetpack.config.Configs
import com.possible_triangle.flightlib.api.ControlType
import com.possible_triangle.flightlib.api.FlightKey
import com.possible_triangle.flightlib.api.IFlightApi
import com.possible_triangle.flightlib.api.IJetpack.Context
import com.simibubi.create.content.equipment.armor.BacktankUtil
import java.util.ArrayList
import java.util.LinkedHashMap
import java.util.Locale
import java.util.Map.Entry
import kotlin.jvm.functions.Function1
import kotlin.jvm.internal.SourceDebugExtension
import net.minecraft.client.DeltaTracker
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.LayeredDraw.Layer
import net.minecraft.client.player.LocalPlayer
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.Vec2
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent
import net.neoforged.neoforge.client.gui.VanillaGuiLayers

@SourceDebugExtension(["SMAP\nControlsDisplay.kt\nKotlin\n*S Kotlin\n*F\n+ 1 ControlsDisplay.kt\ncom/possible_triangle/create_jetpack/client/ControlsDisplay\n+ 2 fake.kt\nkotlin/jvm/internal/FakeKt\n+ 3 Maps.kt\nkotlin/collections/MapsKt__MapsKt\n+ 4 _Collections.kt\nkotlin/collections/CollectionsKt___CollectionsKt\n*L\n1#1,165:1\n1#2:166\n491#3,7:167\n507#3,7:174\n1596#4:181\n1629#4,4:182\n*S KotlinDebug\n*F\n+ 1 ControlsDisplay.kt\ncom/possible_triangle/create_jetpack/client/ControlsDisplay\n*L\n102#1:167,7\n103#1:174,7\n105#1:181\n105#1:182,4\n*E\n"])
public object ControlsDisplay : Layer {
   private final val controls: ResourceLocation = ResourceLocation.fromNamespaceAndPath("create_jetpack", "textures/gui/controls.png")
   private final val airIndicator: ResourceLocation = ResourceLocation.fromNamespaceAndPath("create_jetpack", "textures/gui/air_indicator.png")
   private final val ICONS: Map<FlightKey, (Context) -> ControlType> =
      MapsKt.mapOf(
         new Pair[]{
            TuplesKt.to(FlightKey.TOGGLE_ACTIVE, ControlsDisplay::ICONS$lambda$0), TuplesKt.to(FlightKey.TOGGLE_HOVER, ControlsDisplay::ICONS$lambda$1)
         }
      )

   private fun spritePos(index: Int): Vec2 {
      return new Vec2(index % 2 * 16.0F, index / 2 * 16.0F);
   }

   @JvmStatic
   public fun register(event: RegisterGuiLayersEvent) {
      event.registerAbove(VanillaGuiLayers.HOTBAR, ResourceLocation.fromNamespaceAndPath("create_jetpack", "jetpack_controls"), INSTANCE);
   }

   public open fun render(graphics: GuiGraphics, tracker: DeltaTracker) {
      val mc: Minecraft = Minecraft.getInstance();
      if (Configs.INSTANCE.getCLIENT().getShowOverlay().get() as java.lang.Boolean) {
         if (!mc.options.hideGui) {
            if (mc.player != null) {
               val player: LocalPlayer = mc.player;
               val var10000: Context = IFlightApi.Companion.getINSTANCE().findJetpack(player as LivingEntity);
               if (var10000 != null) {
                  val context: Context = var10000;
                  val scale: Float = (float)(Configs.INSTANCE.getCLIENT().getOverlayScale().get() as java.lang.Number).doubleValue();
                  val spriteWidth: Int = 16 + 6;
                  val engineActive: Int = Configs.INSTANCE.getCLIENT().getOverlayX().get() as Int;
                  val var59: Int;
                  if (engineActive >= 0) {
                     var59 = engineActive;
                  } else {
                     val var60: Int = graphics.guiWidth();
                     var59 = var60 + engineActive - 50;
                  }

                  val startX: Int = (int)(var59.intValue() / scale);
                  val itx: Int = Configs.INSTANCE.getCLIENT().getOverlayY().get() as Int;
                  val var61: Int;
                  if (itx >= 0) {
                     var61 = itx;
                  } else {
                     val var62: Int = graphics.guiHeight();
                     var61 = var62 + itx - 24;
                  }

                  val startY: Int = (int)(var61.intValue() / scale);
                  val var32: Boolean = FlightKey.TOGGLE_ACTIVE.isPressed(player as LivingEntity);
                  var var38: java.util.Map = ICONS;
                  var airSource: LinkedHashMap = new LinkedHashMap();

                  for (Entry entry$iv : var38.entrySet()) {
                     if (air.getKey() as FlightKey === FlightKey.TOGGLE_ACTIVE || var32) {
                        airSource.put(air.getKey(), air.getValue());
                     }
                  }

                  var38 = airSource;
                  airSource = new LinkedHashMap();

                  for (Entry entry$ivx : var38.entrySet()) {
                     if ((`entry$ivx`.getValue() as Function1).invoke(context) === ControlType.TOGGLE) {
                        airSource.put(`entry$ivx`.getKey(), `entry$ivx`.getValue());
                     }
                  }

                  val var40: java.lang.Iterable = airSource.keySet();
                  val var48: java.util.Collection = new ArrayList(CollectionsKt.collectionSizeOrDefault(var40, 10));
                  var var54: Int = 0;

                  for (Object item$iv$iv : var40) {
                     val var21: Int = var54++;
                     if (var21 < 0) {
                        CollectionsKt.throwIndexOverflow();
                     }

                     val key: FlightKey = `item$iv$iv` as FlightKey;
                     graphics.pose().pushPose();
                     val active: Boolean = key.isPressed(player as LivingEntity);
                     render$renderSprite(graphics, scale, startX, startY, var21 + (if (active) 0 else 2), spriteWidth * var21);
                     graphics.pose().scale(0.5F, 0.5F, 0.5F);
                     val textMargin: Float = (startX + 8 + spriteWidth * var21) * (1 / 0.5F);
                     val var63: java.lang.String = key.name().toLowerCase(Locale.ROOT);
                     graphics.drawCenteredString(
                        mc.font,
                        Component.translatable("overlay.flightlib.control.$var63") as Component,
                        (int)textMargin,
                        startY * 2 + 36,
                        if (active) 16777215 else 12303291
                     );
                     graphics.pose().popPose();
                     var48.add(Unit.INSTANCE);
                  }

                  val var36: Int = (var48 as java.util.List).size();
                  if (var32) {
                     graphics.pose().pushPose();
                     graphics.pose().scale(scale, scale, scale);
                     RenderSystem.enableBlend();
                     val var44: Boolean = player.level().getGameTime() % 20 < 5L;
                     val var64: java.util.List = BacktankUtil.getAllWithAir(player as LivingEntity);
                     var var65: ItemStack = CollectionsKt.firstOrNull(var64) as ItemStack;
                     if (var65 == null) {
                        var65 = ItemStack.EMPTY;
                     }

                     var54 = (int)Math.ceil((double)BacktankUtil.getAir(var65) * 14.0 / (double)BacktankUtil.maxAir(var65));
                     val var58: Boolean = IFlightApi.Companion.getINSTANCE().currentAction(context) != null;
                     render$renderBar$default(graphics, startX, spriteWidth, var36, startY, 5, 1, 0, 0, 384, null);
                     if (var58 && var54 > 0 && var44) {
                        render$renderBar(graphics, startX, spriteWidth, var36, startY, 5, 0, var54 - 1, 1);
                     } else {
                        render$renderBar(graphics, startX, spriteWidth, var36, startY, 5, 0, var54, 1);
                     }

                     RenderSystem.disableBlend();
                     graphics.pose().popPose();
                  }
               }
            }
         }
      }
   }

   @JvmStatic
   fun `ICONS$lambda$0`(it: Context): ControlType {
      return it.getJetpack().activeType(it);
   }

   @JvmStatic
   fun `ICONS$lambda$1`(it: Context): ControlType {
      return it.getJetpack().hoverType(it);
   }

   @JvmStatic
   fun `render$renderSprite`(`$graphics`: GuiGraphics, scale: Float, startX: Int, startY: Int, index: Int, x: Int) {
      val sprite: Vec2 = INSTANCE.spritePos(index);
      `$graphics`.pose().scale(scale, scale, scale);
      `$graphics`.blit(controls, startX + x, startY, 0, sprite.x, sprite.y, 16, 16, 32, 32);
   }

   @JvmStatic
   fun `render$renderBar`(
      `$graphics`: GuiGraphics, startX: Int, spriteWidth: Int, renderedIcons: Int, startY: Int, barWidth: Int, index: Int, barHeight: Int, spriteOffset: Int
   ) {
      `$graphics`.blit(
         airIndicator,
         startX + spriteWidth * renderedIcons,
         startY + (19 - barHeight) - spriteOffset,
         (float)(barWidth * index),
         16.0F - (float)barHeight - (float)spriteOffset,
         barWidth,
         barHeight,
         16,
         16
      );
   }
}

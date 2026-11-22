package com.possible_triangle.create_jetpack.item

import com.possible_triangle.create_jetpack.config.Configs
import com.possible_triangle.flightlib.api.ControlType
import com.possible_triangle.flightlib.api.FlightAction
import com.possible_triangle.flightlib.api.IJetpack
import com.possible_triangle.flightlib.api.ISource
import com.possible_triangle.flightlib.api.IJetpack.Context
import com.possible_triangle.flightlib.api.sources.CuriosSource
import com.possible_triangle.flightlib.api.sources.EquipmentSource
import com.simibubi.create.content.equipment.armor.BacktankItem
import com.simibubi.create.content.equipment.armor.BacktankUtil
import com.simibubi.create.content.equipment.armor.BacktankItem.BacktankBlockItem
import com.simibubi.create.foundation.item.LayeredArmorItem
import com.simibubi.create.foundation.particle.AirParticleData
import com.tterrag.registrate.util.entry.ItemEntry
import java.util.ArrayList
import java.util.Arrays
import java.util.Locale
import java.util.function.Supplier
import kotlin.jvm.internal.SourceDebugExtension
import net.minecraft.core.Holder
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.core.particles.SimpleParticleType
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ArmorMaterial
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Item.Properties
import net.minecraft.world.item.enchantment.Enchantment
import net.minecraft.world.phys.Vec3

@SourceDebugExtension(["SMAP\nJetpackItem.kt\nKotlin\n*S Kotlin\n*F\n+ 1 JetpackItem.kt\ncom/possible_triangle/create_jetpack/item/JetpackItem\n+ 2 _Collections.kt\nkotlin/collections/CollectionsKt___CollectionsKt\n*L\n1#1,127:1\n1586#2:128\n1661#2,3:129\n*S KotlinDebug\n*F\n+ 1 JetpackItem.kt\ncom/possible_triangle/create_jetpack/item/JetpackItem\n*L\n56#1:128\n56#1:129,3\n*E\n"])
public open class JetpackItem(properties: Properties, material: Holder<ArmorMaterial>, texture: ResourceLocation, blockItem: ItemEntry<BacktankBlockItem>) : BacktankItem(
         material, properties, texture, blockItem as Supplier
      ),
   IJetpack {
   private final val thrusters: List<Vec3>

   init {
      val var17: java.lang.Iterable = CollectionsKt.listOf(new java.lang.Double[]{-0.35, 0.35});
      val `destination$iv$iv`: java.util.Collection = new ArrayList(CollectionsKt.collectionSizeOrDefault(var17, 10));

      for (Object item$iv$iv : var17) {
         `destination$iv$iv`.add(new Vec3((`item$iv$iv` as java.lang.Number).doubleValue(), 0.7, -0.5));
      }

      this.thrusters = `destination$iv$iv` as MutableList<Vec3>;
   }

   public open fun hoverSpeed(context: Context): Double {
      return Configs.INSTANCE.getSERVER().getHoverSpeed();
   }

   public open fun verticalSpeed(context: Context): Double {
      return Configs.INSTANCE.getSERVER().getVerticalSpeed();
   }

   public open fun activeType(context: Context): ControlType {
      return ControlType.TOGGLE;
   }

   public open fun hoverType(context: Context): ControlType {
      return ControlType.TOGGLE;
   }

   public open fun horizontalSpeed(context: Context): Double {
      return Configs.INSTANCE.getSERVER().getHorizontalSpeed();
   }

   public open fun acceleration(context: Context): Double {
      return Configs.INSTANCE.getSERVER().getAcceleration();
   }

   public open fun swimModifier(context: Context): Double {
      return Configs.INSTANCE.getSERVER().getSwimModifier();
   }

   public open fun elytraBoost(): Double {
      return Configs.INSTANCE.getSERVER().getElytraBoost();
   }

   public open fun getThrusters(context: Context): List<Vec3> {
      return this.thrusters;
   }

   public open fun onUse(context: Context, action: FlightAction) {
      if (context.getLevel().getGameTime() % 20 == 0L) {
         BacktankUtil.canAbsorbDamage(context.getEntity(), this.secondsPerTank(action));
      }
   }

   private fun secondsPerTank(action: FlightAction): Int {
      return if (action != FlightAction.HOVER && action != FlightAction.DOWN)
         Configs.INSTANCE.getSERVER().getSecondsPerTank()
         else
         Configs.INSTANCE.getSERVER().getSecondsPerTankHover();
   }

   public open fun isValid(context: Context): Boolean {
      val source: ISource = context.getSource();
      return if (source is EquipmentSource) (source as EquipmentSource).getSlot() === EquipmentSlot.CHEST else source is CuriosSource;
   }

   public open fun isUsable(context: Context): Boolean {
      val var10000: java.util.List = BacktankUtil.getAllWithAir(context.getEntity());
      val var5: ItemStack = CollectionsKt.firstOrNull(var10000) as ItemStack;
      if (var5 == null) {
         return false;
      } else {
         return BacktankUtil.getAir(var5) >= (float)BacktankUtil.maxAirWithoutEnchants() / this.secondsPerTank(FlightAction.UP);
      }
   }

   public open fun supportsEnchantment(stack: ItemStack, enchantment: Holder<Enchantment>): Boolean {
      return super.supportsEnchantment(stack, enchantment) && Configs.INSTANCE.getSERVER().getEnchantments().isAllowed(enchantment);
   }

   public open fun createParticles(): ParticleOptions {
      val var10000: ParticleOptions;
      if (Configs.INSTANCE.getCLIENT().getSpawnSnowParticles()) {
         val var1: SimpleParticleType = ParticleTypes.SNOWFLAKE;
         var10000 = var1 as ParticleOptions;
      } else {
         var10000 = (new AirParticleData(0.0F, 0.01F)) as ParticleOptions;
      }

      return var10000;
   }

   public open fun heightAboveGroundLimit(context: Context): Int? {
      return Configs.INSTANCE.getSERVER().getHeightAboveGroundLimit();
   }

   public class Layered(properties: Properties, material: Holder<ArmorMaterial>, texture: ResourceLocation, blockItem: ItemEntry<BacktankBlockItem>) : JetpackItem(
            properties, material, texture, blockItem
         ),
      LayeredArmorItem {
      public open fun getArmorTextureLocation(entity: LivingEntity?, slot: EquipmentSlot?, stack: ItemStack?, layer: Int): String {
         val var5: Locale = Locale.ROOT;
         val var7: Array<Any> = new Object[]{this.textureLoc.getNamespace(), this.textureLoc.getPath(), layer};
         val var10000: java.lang.String = java.lang.String.format(var5, "%s:textures/models/armor/%s_layer_%d.png", Arrays.copyOf(var7, var7.length));
         return var10000;
      }
   }
}

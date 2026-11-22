package com.possible_triangle.create_jetpack.config

import java.util.ArrayList
import kotlin.jvm.internal.SourceDebugExtension
import net.minecraft.core.Holder
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.enchantment.Enchantment

@SourceDebugExtension(["SMAP\nServerConfig.kt\nKotlin\n*S Kotlin\n*F\n+ 1 ServerConfig.kt\ncom/possible_triangle/create_jetpack/config/EnchantmentConfig\n+ 2 _Collections.kt\nkotlin/collections/CollectionsKt___CollectionsKt\n+ 3 fake.kt\nkotlin/jvm/internal/FakeKt\n*L\n1#1,90:1\n1642#2,10:91\n1915#2:101\n1916#2:103\n1652#2:104\n1#3:102\n*S KotlinDebug\n*F\n+ 1 ServerConfig.kt\ncom/possible_triangle/create_jetpack/config/EnchantmentConfig\n*L\n86#1:91,10\n86#1:101\n86#1:103\n86#1:104\n86#1:102\n*E\n"])
public data class EnchantmentConfig(ids: List<String>, isBlacklist: Boolean) {
   public final val ids: List<String>
   public final val isBlacklist: Boolean

   init {
      this.ids = ids;
      this.isBlacklist = isBlacklist;
   }

   public fun isAllowed(enchantment: Holder<Enchantment>): Boolean {
      val var10000: ResourceKey = enchantment.getKey();
      if (var10000 != null) {
         val var19: ResourceLocation = var10000.location();
         if (var19 != null) {
            val `$this$mapNotNull$iv`: java.lang.Iterable = this.ids;
            val `destination$iv$iv`: java.util.Collection = new ArrayList();

            for (Object element$iv$iv$iv : $this$mapNotNull$iv) {
               val var20: ResourceLocation = ResourceLocation.tryParse(`element$iv$iv$iv` as java.lang.String);
               if (var20 != null) {
                  `destination$iv$iv`.add(var20);
               }
            }

            return (`destination$iv$iv` as java.util.List).contains(var19) != this.isBlacklist;
         }
      }

      return false;
   }

   public operator fun component1(): List<String> {
      return this.ids;
   }

   public operator fun component2(): Boolean {
      return this.isBlacklist;
   }

   public fun copy(ids: List<String> = this.ids, isBlacklist: Boolean = this.isBlacklist): EnchantmentConfig {
      return new EnchantmentConfig(ids, isBlacklist);
   }

   public override fun toString(): String {
      return "EnchantmentConfig(ids=${this.ids}, isBlacklist=${this.isBlacklist})";
   }

   public override fun hashCode(): Int {
      return this.ids.hashCode() * 31 + java.lang.Boolean.hashCode(this.isBlacklist);
   }

   public override operator fun equals(other: Any?): Boolean {
      if (this === other) {
         return true;
      } else if (other !is EnchantmentConfig) {
         return false;
      } else {
         val var2: EnchantmentConfig = other as EnchantmentConfig;
         if (!(this.ids == (other as EnchantmentConfig).ids)) {
            return false;
         } else {
            return this.isBlacklist == var2.isBlacklist;
         }
      }
   }
}

package com.possible_triangle.create_jetpack

import com.possible_triangle.create_jetpack.CopyComponentsMechanicalCraftingRecipe.Serializer
import com.possible_triangle.create_jetpack.block.JetpackBlock
import com.possible_triangle.create_jetpack.config.Configs
import com.possible_triangle.create_jetpack.item.JetpackItem
import com.possible_triangle.flightlib.api.IJetpack
import com.possible_triangle.flightlib.neoforge.api.NeoForgeFlightLib
import com.simibubi.create.AllBlocks
import com.simibubi.create.AllCreativeModeTabs
import com.simibubi.create.AllDataComponents
import com.simibubi.create.AllPartialModels
import com.simibubi.create.Create
import com.simibubi.create.AllTags.AllItemTags
import com.simibubi.create.api.stress.BlockStressValues
import com.simibubi.create.content.equipment.armor.AllArmorMaterials
import com.simibubi.create.content.equipment.armor.BacktankBlockEntity
import com.simibubi.create.content.equipment.armor.BacktankRenderer
import com.simibubi.create.content.equipment.armor.BacktankUtil
import com.simibubi.create.content.equipment.armor.BacktankItem.BacktankBlockItem
import com.simibubi.create.content.kinetics.base.KineticBlockEntity
import com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual
import com.simibubi.create.foundation.data.CreateRegistrate
import com.simibubi.create.foundation.data.SharedProperties
import com.simibubi.create.foundation.data.TagGen
import com.simibubi.create.foundation.item.KineticStats
import com.simibubi.create.foundation.item.TooltipModifier
import com.simibubi.create.foundation.item.ItemDescription.Modifier
import com.tterrag.registrate.builders.BlockBuilder
import com.tterrag.registrate.builders.ItemBuilder
import com.tterrag.registrate.providers.DataGenContext
import com.tterrag.registrate.providers.RegistrateBlockstateProvider
import com.tterrag.registrate.providers.RegistrateItemModelProvider
import com.tterrag.registrate.providers.loot.RegistrateBlockLootTables
import com.tterrag.registrate.util.CreativeModeTabModifier
import com.tterrag.registrate.util.entry.BlockEntityEntry
import com.tterrag.registrate.util.entry.BlockEntry
import com.tterrag.registrate.util.entry.ItemEntry
import com.tterrag.registrate.util.entry.RegistryEntry
import com.tterrag.registrate.util.nullness.NonNullFunction
import com.tterrag.registrate.util.nullness.NonNullSupplier
import dev.engine_room.flywheel.api.visual.BlockEntityVisual
import dev.engine_room.flywheel.api.visualization.VisualizationContext
import dev.engine_room.flywheel.lib.model.Models
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer.Factory
import java.util.function.Supplier
import kotlin.jvm.functions.Function0
import kotlin.jvm.functions.Function1
import kotlin.jvm.internal.SourceDebugExtension
import net.createmod.catnip.lang.FontHelper.Palette
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context
import net.minecraft.core.Holder
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.tags.ItemTags
import net.minecraft.tags.TagKey
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ArmorMaterials
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Rarity
import net.minecraft.world.item.Item.Properties
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.level.ItemLike
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.storage.loot.LootPool
import net.minecraft.world.level.storage.loot.LootTable
import net.minecraft.world.level.storage.loot.entries.LootItem
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction.Source
import net.minecraft.world.level.storage.loot.functions.LootItemFunction.Builder
import net.minecraft.world.level.storage.loot.predicates.ExplosionCondition
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.ModContainer
import net.neoforged.fml.config.IConfigSpec
import net.neoforged.fml.config.ModConfig.Type
import net.neoforged.fml.event.config.ModConfigEvent.Reloading
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder
import net.neoforged.neoforge.client.model.generators.ModelFile
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent

@SourceDebugExtension(["SMAP\nContent.kt\nKotlin\n*S Kotlin\n*F\n+ 1 Content.kt\ncom/possible_triangle/create_jetpack/Content\n+ 2 Forge.kt\nthedarkcolour/kotlinforforge/neoforge/forge/ForgeKt\n*L\n1#1,262:1\n17#2:263\n*S KotlinDebug\n*F\n+ 1 Content.kt\ncom/possible_triangle/create_jetpack/Content\n*L\n244#1:263\n*E\n"])
public object Content {
   public final val COPY_NBT_MECHANICAL_CRAFTING_SERIALIZER: RegistryEntry<RecipeSerializer<*>, Serializer> =
      Constants.REGISTRATE
         .generic("copy_components_mechanical_crafting", Registries.RECIPE_SERIALIZER, Content::COPY_NBT_MECHANICAL_CRAFTING_SERIALIZER$lambda$0)
         .register()
         public final val JETPACK_ITEM: ItemEntry<JetpackItem>
   public final val JETPACK_PLACEABLE: ItemEntry<BacktankBlockItem>
   public final val JETPACK_BLOCK: BlockEntry<JetpackBlock>
   public final val NETHERITE_JETPACK_ITEM: ItemEntry<JetpackItem>
   public final val NETHERITE_JETPACK_PLACEABLE: ItemEntry<BacktankBlockItem>
   public final val NETHERITE_JETPACK_BLOCK: BlockEntry<JetpackBlock>
   public final val JETPACK_BLOCK_ENTITY: BlockEntityEntry<BacktankBlockEntity>

   public fun <T : Block, P> BlockBuilder<T, P>.jetpackTransforms(getItem: () -> Item): BlockBuilder<T, P> {
      `$this$jetpackTransforms`.blockstate(Content::jetpackTransforms$lambda$0$0);
      `$this$jetpackTransforms`.transform(TagGen.pickaxeOnly());
      `$this$jetpackTransforms`.onRegister(Content::jetpackTransforms$lambda$0$2);
      `$this$jetpackTransforms`.addLayer(Content::jetpackTransforms$lambda$0$3);
      `$this$jetpackTransforms`.loot(Content::jetpackTransforms$lambda$0$4);
      return `$this$jetpackTransforms`;
   }

   public fun <T : Item, P> ItemBuilder<T, P>.jetpackProperties(): ItemBuilder<T, P> {
      `$this$jetpackProperties`.model(Content::jetpackProperties$lambda$0$0);
      `$this$jetpackProperties`.tag(new TagKey[]{ItemTags.CHEST_ARMOR});
      val var10000: ItemBuilder = `$this$jetpackProperties`.tag(new TagKey[]{AllItemTags.PRESSURIZED_AIR_SOURCES.tag});
      val var10001: ResourceKey = AllCreativeModeTabs.BASE_CREATIVE_TAB.getKey();
      var10000.tab(var10001, Content::jetpackProperties$lambda$0$1);
      `$this$jetpackProperties`.getOwner().addRawLang("item.${Constants.REGISTRATE.getModid()}.${`$this$jetpackProperties`.getName()}.tooltip", "");
      `$this$jetpackProperties`.getOwner()
         .addRawLang(
            "item.${Constants.REGISTRATE.getModid()}.${`$this$jetpackProperties`.getName()}.tooltip.summary", "Allows levitation using pressurized air"
         );
      `$this$jetpackProperties`.getOwner()
         .addRawLang("item.${Constants.REGISTRATE.getModid()}.${`$this$jetpackProperties`.getName()}.tooltip.control1", "Press [JUMP]");
      `$this$jetpackProperties`.getOwner()
         .addRawLang("item.${Constants.REGISTRATE.getModid()}.${`$this$jetpackProperties`.getName()}.tooltip.action1", "Fly upwards");
      `$this$jetpackProperties`.getOwner()
         .addRawLang("item.${Constants.REGISTRATE.getModid()}.${`$this$jetpackProperties`.getName()}.tooltip.control2", "Press [SHIFT]");
      `$this$jetpackProperties`.getOwner()
         .addRawLang("item.${Constants.REGISTRATE.getModid()}.${`$this$jetpackProperties`.getName()}.tooltip.action2", "Fly downwards");
      `$this$jetpackProperties`.getOwner()
         .addRawLang("item.${Constants.REGISTRATE.getModid()}.${`$this$jetpackProperties`.getName()}.tooltip.control3", "Press [G]");
      `$this$jetpackProperties`.getOwner()
         .addRawLang("item.${Constants.REGISTRATE.getModid()}.${`$this$jetpackProperties`.getName()}.tooltip.action3", "Turn engine on/off");
      `$this$jetpackProperties`.getOwner()
         .addRawLang("item.${Constants.REGISTRATE.getModid()}.${`$this$jetpackProperties`.getName()}.tooltip.control4", "Press [H]");
      `$this$jetpackProperties`.getOwner()
         .addRawLang("item.${Constants.REGISTRATE.getModid()}.${`$this$jetpackProperties`.getName()}.tooltip.action4", "Turn hover mode on/off");
      return `$this$jetpackProperties`;
   }

   public fun <T : Item, P> ItemBuilder<T, P>.jetpackPlaceableProperties(): ItemBuilder<T, P> {
      return `$this$jetpackPlaceableProperties`.model(Content::jetpackPlaceableProperties$lambda$0);
   }

   public fun register(container: ModContainer, modBus: IEventBus) {
      Constants.REGISTRATE.addRawLang("key.categories.movement.jetpack", "Create Jetpack");
      container.registerConfig(Type.COMMON, Configs.INSTANCE.getSERVER_SPEC() as IConfigSpec);
      container.registerConfig(Type.CLIENT, Configs.INSTANCE.getCLIENT_SPEC() as IConfigSpec);
      modBus.addListener(Configs.Network.INSTANCE::register);
      val var10000: IEventBus = NeoForge.EVENT_BUS;
      var10000.addListener(Content::register$lambda$0);
      modBus.addListener(Content::register$lambda$1);
      modBus.addListener(Content::register$lambda$2);
   }

   @JvmStatic
   fun `_init_$lambda$0`(it: Item): TooltipModifier {
      return new Modifier(it, Palette.STANDARD_CREATE).andThen(TooltipModifier.mapNull(KineticStats.create(it) as TooltipModifier));
   }

   @JvmStatic
   fun `COPY_NBT_MECHANICAL_CRAFTING_SERIALIZER$lambda$0`(): CopyComponentsMechanicalCraftingRecipe.Serializer {
      return CopyComponentsMechanicalCraftingRecipe.Serializer.INSTANCE;
   }

   @JvmStatic
   fun `JETPACK_ITEM$lambda$0`(it: Properties): JetpackItem {
      val var10003: Holder = AllArmorMaterials.COPPER;
      val var10004: ResourceLocation = Create.asResource("copper_diving");
      return new JetpackItem(it, var10003, var10004, JETPACK_PLACEABLE);
   }

   @JvmStatic
   fun `JETPACK_ITEM$lambda$1`(it: Properties): Properties {
      return it.rarity(Rarity.RARE);
   }

   @JvmStatic
   fun `JETPACK_PLACEABLE$lambda$0$0`(): Item {
      return JETPACK_ITEM.get() as Item;
   }

   @JvmStatic
   fun `JETPACK_PLACEABLE$lambda$0`(it: Properties): BacktankBlockItem {
      return new BacktankBlockItem(JETPACK_BLOCK.get() as Block, Content::JETPACK_PLACEABLE$lambda$0$0, it);
   }

   @JvmStatic
   fun `JETPACK_BLOCK$lambda$0`(it: net.minecraft.world.level.block.state.BlockBehaviour.Properties): JetpackBlock {
      return new JetpackBlock(it);
   }

   @JvmStatic
   fun `JETPACK_BLOCK$lambda$1`(): Block {
      return SharedProperties.copperMetal();
   }

   @JvmStatic
   fun `JETPACK_BLOCK$lambda$2`(): Item {
      val var10000: Any = JETPACK_ITEM.get();
      return var10000 as Item;
   }

   @JvmStatic
   fun `NETHERITE_JETPACK_ITEM$lambda$0`(it: Properties): JetpackItem {
      val var10003: Holder = ArmorMaterials.NETHERITE;
      val var10004: ResourceLocation = Create.asResource("netherite_diving");
      return new JetpackItem.Layered(it, var10003, var10004, NETHERITE_JETPACK_PLACEABLE);
   }

   @JvmStatic
   fun `NETHERITE_JETPACK_ITEM$lambda$1`(it: Properties): Properties {
      return it.rarity(Rarity.EPIC);
   }

   @JvmStatic
   fun `NETHERITE_JETPACK_ITEM$lambda$2`(it: Properties): Properties {
      return it.fireResistant();
   }

   @JvmStatic
   fun `NETHERITE_JETPACK_PLACEABLE$lambda$0$0`(): Item {
      return NETHERITE_JETPACK_ITEM.get() as Item;
   }

   @JvmStatic
   fun `NETHERITE_JETPACK_PLACEABLE$lambda$0`(it: Properties): BacktankBlockItem {
      return new BacktankBlockItem(NETHERITE_JETPACK_BLOCK.get() as Block, Content::NETHERITE_JETPACK_PLACEABLE$lambda$0$0, it);
   }

   @JvmStatic
   fun `NETHERITE_JETPACK_BLOCK$lambda$0`(it: net.minecraft.world.level.block.state.BlockBehaviour.Properties): JetpackBlock {
      return new JetpackBlock(it);
   }

   @JvmStatic
   fun `NETHERITE_JETPACK_BLOCK$lambda$1`(): Block {
      return SharedProperties.netheriteMetal();
   }

   @JvmStatic
   fun `NETHERITE_JETPACK_BLOCK$lambda$2`(): Item {
      val var10000: Any = NETHERITE_JETPACK_ITEM.get();
      return var10000 as Item;
   }

   @JvmStatic
   fun `jetpackTransforms$lambda$0$1$0`(): Double {
      return BlockStressValues.getImpact(AllBlocks.COPPER_BACKTANK.get() as Block);
   }

   @JvmStatic
   fun `jetpackTransforms$lambda$0$3$0`(): RenderType {
      return RenderType.cutoutMipped();
   }

   @JvmStatic
   fun `jetpackTransforms$lambda$0$0`(c: DataGenContext, p: RegistrateBlockstateProvider) {
      p.horizontalBlock(
         c.getEntry() as Block,
         (
            (p.models().withExistingParent("block/${c.getName()}", p.modLoc("block/jetpack/block")) as BlockModelBuilder).texture("0", "block/${c.getName()}") as BlockModelBuilder
         ) as ModelFile
      );
   }

   @JvmStatic
   fun `jetpackTransforms$lambda$0$1`(it: Block): Unit {
      BlockStressValues.IMPACTS.register(it, Content::jetpackTransforms$lambda$0$1$0);
      return Unit.INSTANCE;
   }

   @JvmStatic
   fun `jetpackTransforms$lambda$0$2`(`$tmp0`: Function1, p0: Any) {
      `$tmp0`.invoke(p0);
   }

   @JvmStatic
   fun `jetpackTransforms$lambda$0$3`(): Supplier {
      return Content::jetpackTransforms$lambda$0$3$0;
   }

   @JvmStatic
   fun `jetpackTransforms$lambda$0$4`(`$getItem`: Function0, lt: RegistrateBlockLootTables, block: Block) {
      lt.add(
         block,
         LootTable.lootTable()
            .withPool(
               LootPool.lootPool()
                  .when(ExplosionCondition.survivesExplosion())
                  .setRolls(ConstantValue.exactly(1.0F) as NumberProvider)
                  .add(
                     LootItem.lootTableItem(`$getItem`.invoke() as ItemLike)
                        .apply(CopyComponentsFunction.copyComponents(Source.BLOCK_ENTITY).include(AllDataComponents.BACKTANK_AIR) as Builder) as net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer.Builder
                  )
            )
      );
   }

   @JvmStatic
   fun `jetpackProperties$lambda$0$0`(c: DataGenContext, p: RegistrateItemModelProvider) {
      (p.withExistingParent("item/${c.getName()}", p.modLoc("block/jetpack/item")) as ItemModelBuilder).texture("0", "block/${c.getName()}");
   }

   @JvmStatic
   fun `jetpackProperties$lambda$0$1`(context: DataGenContext, mod: CreativeModeTabModifier) {
      val var2: ItemStack = new ItemStack(context.get() as ItemLike);
      var2.set(AllDataComponents.BACKTANK_AIR, BacktankUtil.maxAirWithoutEnchants());
      mod.accept(var2);
   }

   @JvmStatic
   fun `jetpackPlaceableProperties$lambda$0`(context: DataGenContext, provider: RegistrateItemModelProvider) {
      provider.withExistingParent(context.getName(), provider.mcLoc("item/barrier"));
   }

   @JvmStatic
   fun `JETPACK_BLOCK_ENTITY$lambda$0$0`(ctx: VisualizationContext, be: BacktankBlockEntity, f: Float): BlockEntityVisual {
      return (
         new SingleAxisRotatingVisual(
            ctx,
            be as KineticBlockEntity,
            f,
            Models.partial(
               if (be.getBlockState().is(NETHERITE_JETPACK_BLOCK as Holder))
                  AllPartialModels.NETHERITE_BACKTANK_SHAFT
                  else
                  AllPartialModels.COPPER_BACKTANK_SHAFT
            )
         )
      ) as BlockEntityVisual;
   }

   @JvmStatic
   fun `JETPACK_BLOCK_ENTITY$lambda$1$0`(it: Context): BlockEntityRenderer {
      return (new BacktankRenderer(it)) as BlockEntityRenderer;
   }

   @JvmStatic
   fun `JETPACK_BLOCK_ENTITY$lambda$0`(): Factory {
      return Content::JETPACK_BLOCK_ENTITY$lambda$0$0;
   }

   @JvmStatic
   fun `JETPACK_BLOCK_ENTITY$lambda$1`(): NonNullFunction {
      return Content::JETPACK_BLOCK_ENTITY$lambda$1$0;
   }

   @JvmStatic
   fun `register$lambda$2$0`(stack: ItemStack, var1: Void): IJetpack {
      val var10000: Item = stack.getItem();
      return var10000 as IJetpack;
   }

   @JvmStatic
   fun `register$lambda$0`(event: PlayerLoggedInEvent) {
      val player: Player = event.getEntity();
      if (player is ServerPlayer) {
         Configs.INSTANCE.syncConfig(player as ServerPlayer);
      }
   }

   @JvmStatic
   fun `register$lambda$1`(event: Reloading) {
      if (event.getConfig().getType() === Type.COMMON) {
         Configs.INSTANCE.syncConfig();
      }
   }

   @JvmStatic
   fun `register$lambda$2`(event: RegisterCapabilitiesEvent) {
      event.registerItem(NeoForgeFlightLib.ITEM_CAPABILITY, Content::register$lambda$2$0, new ItemLike[]{JETPACK_ITEM, NETHERITE_JETPACK_ITEM});
   }

   @JvmStatic
   fun {
      (Constants.REGISTRATE.defaultCreativeTab(null) as CreateRegistrate).setTooltipModifierFactory(Content::_init_$lambda$0);
      var var10000: Content = INSTANCE;
      var var10001: ItemBuilder = Constants.REGISTRATE.item("jetpack", Content::JETPACK_ITEM$lambda$0).properties(Content::JETPACK_ITEM$lambda$1);
      var var0: ItemEntry = var10000.jetpackProperties(var10001).register();
      JETPACK_ITEM = var0;
      var10000 = INSTANCE;
      var10001 = Constants.REGISTRATE.item("jetpack_placeable", Content::JETPACK_PLACEABLE$lambda$0);
      var0 = var10000.jetpackPlaceableProperties(var10001).register();
      JETPACK_PLACEABLE = var0;
      var10000 = INSTANCE;
      val var14: BlockBuilder = Constants.REGISTRATE.block("jetpack", Content::JETPACK_BLOCK$lambda$0).initialProperties(Content::JETPACK_BLOCK$lambda$1);
      val var2: BlockEntry = var10000.jetpackTransforms(var14, Content::JETPACK_BLOCK$lambda$2).register();
      JETPACK_BLOCK = var2;
      var10000 = INSTANCE;
      var10001 = Constants.REGISTRATE
         .item("netherite_jetpack", Content::NETHERITE_JETPACK_ITEM$lambda$0)
         .properties(Content::NETHERITE_JETPACK_ITEM$lambda$1)
         .properties(Content::NETHERITE_JETPACK_ITEM$lambda$2);
      var0 = var10000.jetpackProperties(var10001).register();
      NETHERITE_JETPACK_ITEM = var0;
      var10000 = INSTANCE;
      var10001 = Constants.REGISTRATE.item("netherite_jetpack_placeable", Content::NETHERITE_JETPACK_PLACEABLE$lambda$0);
      var0 = var10000.jetpackPlaceableProperties(var10001).register();
      NETHERITE_JETPACK_PLACEABLE = var0;
      var10000 = INSTANCE;
      val var17: BlockBuilder = Constants.REGISTRATE
         .block("netherite_jetpack", Content::NETHERITE_JETPACK_BLOCK$lambda$0)
         .initialProperties(Content::NETHERITE_JETPACK_BLOCK$lambda$1);
      val var5: BlockEntry = var10000.jetpackTransforms(var17, Content::NETHERITE_JETPACK_BLOCK$lambda$2).register();
      NETHERITE_JETPACK_BLOCK = var5;
      JETPACK_BLOCK_ENTITY = Constants.REGISTRATE
         .blockEntity("jetpack", BacktankBlockEntity::new)
         .visual(Content::JETPACK_BLOCK_ENTITY$lambda$0)
         .validBlocks(new NonNullSupplier[]{JETPACK_BLOCK, NETHERITE_JETPACK_BLOCK})
         .renderer(Content::JETPACK_BLOCK_ENTITY$lambda$1)
         .register();
   }
}

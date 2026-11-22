package com.possible_triangle.create_jetpack.block

import com.possible_triangle.create_jetpack.Content
import com.simibubi.create.content.equipment.armor.BacktankBlock
import com.simibubi.create.content.equipment.armor.BacktankBlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockBehaviour.Properties

public class JetpackBlock(properties: Properties) : BacktankBlock(properties) {
   public open fun getBlockEntityType(): BlockEntityType<out BacktankBlockEntity> {
      val var10000: Any = Content.INSTANCE.getJETPACK_BLOCK_ENTITY().get();
      return var10000 as BlockEntityType<? extends BacktankBlockEntity>;
   }
}

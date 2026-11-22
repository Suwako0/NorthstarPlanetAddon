package com.possible_triangle.create_jetpack;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod("create_jetpack")
public class NeoForgeEntrypoint {
   public NeoForgeEntrypoint(ModContainer container, IEventBus modBus) {
      Constants.REGISTRATE.registerEventListeners(modBus);
      Content.INSTANCE.register(container, modBus);
   }
}

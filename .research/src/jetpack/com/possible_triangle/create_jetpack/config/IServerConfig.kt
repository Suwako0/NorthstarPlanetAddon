package com.possible_triangle.create_jetpack.config

public interface IServerConfig {
   public val secondsPerTank: Int
   public val secondsPerTankHover: Int
   public val horizontalSpeed: Double
   public val verticalSpeed: Double
   public val acceleration: Double
   public val hoverSpeed: Double
   public val swimModifier: Double
   public val elytraBoost: Double
   public val heightAboveGroundLimit: Int?
   public val enchantments: EnchantmentConfig
}

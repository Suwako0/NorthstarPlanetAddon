package com.possible_triangle.create_jetpack.config

public data class SyncedConfig(secondsPerTank: Int,
      secondsPerTankHover: Int,
      horizontalSpeed: Double,
      verticalSpeed: Double,
      acceleration: Double,
      hoverSpeed: Double,
      swimModifier: Double,
      elytraBoost: Double,
      heightAboveGroundLimit: Int?,
      enchantments: EnchantmentConfig
   ) :
   IServerConfig {
   public open val secondsPerTank: Int
   public open val secondsPerTankHover: Int
   public open val horizontalSpeed: Double
   public open val verticalSpeed: Double
   public open val acceleration: Double
   public open val hoverSpeed: Double
   public open val swimModifier: Double
   public open val elytraBoost: Double
   public open val heightAboveGroundLimit: Int?
   public open val enchantments: EnchantmentConfig

   init {
      this.secondsPerTank = secondsPerTank;
      this.secondsPerTankHover = secondsPerTankHover;
      this.horizontalSpeed = horizontalSpeed;
      this.verticalSpeed = verticalSpeed;
      this.acceleration = acceleration;
      this.hoverSpeed = hoverSpeed;
      this.swimModifier = swimModifier;
      this.elytraBoost = elytraBoost;
      this.heightAboveGroundLimit = heightAboveGroundLimit;
      this.enchantments = enchantments;
   }

   public operator fun component1(): Int {
      return this.secondsPerTank;
   }

   public operator fun component2(): Int {
      return this.secondsPerTankHover;
   }

   public operator fun component3(): Double {
      return this.horizontalSpeed;
   }

   public operator fun component4(): Double {
      return this.verticalSpeed;
   }

   public operator fun component5(): Double {
      return this.acceleration;
   }

   public operator fun component6(): Double {
      return this.hoverSpeed;
   }

   public operator fun component7(): Double {
      return this.swimModifier;
   }

   public operator fun component8(): Double {
      return this.elytraBoost;
   }

   public operator fun component9(): Int? {
      return this.heightAboveGroundLimit;
   }

   public operator fun component10(): EnchantmentConfig {
      return this.enchantments;
   }

   public fun copy(
      secondsPerTank: Int = this.secondsPerTank,
      secondsPerTankHover: Int = this.secondsPerTankHover,
      horizontalSpeed: Double = this.horizontalSpeed,
      verticalSpeed: Double = this.verticalSpeed,
      acceleration: Double = this.acceleration,
      hoverSpeed: Double = this.hoverSpeed,
      swimModifier: Double = this.swimModifier,
      elytraBoost: Double = this.elytraBoost,
      heightAboveGroundLimit: Int? = this.heightAboveGroundLimit,
      enchantments: EnchantmentConfig = this.enchantments
   ): SyncedConfig {
      return new SyncedConfig(
         secondsPerTank,
         secondsPerTankHover,
         horizontalSpeed,
         verticalSpeed,
         acceleration,
         hoverSpeed,
         swimModifier,
         elytraBoost,
         heightAboveGroundLimit,
         enchantments
      );
   }

   public override fun toString(): String {
      return "SyncedConfig(secondsPerTank=${this.secondsPerTank}, secondsPerTankHover=${this.secondsPerTankHover}, horizontalSpeed=${this.horizontalSpeed}, verticalSpeed=${this.verticalSpeed}, acceleration=${this.acceleration}, hoverSpeed=${this.hoverSpeed}, swimModifier=${this.swimModifier}, elytraBoost=${this.elytraBoost}, heightAboveGroundLimit=${this.heightAboveGroundLimit}, enchantments=${this.enchantments})";
   }

   public override fun hashCode(): Int {
      return (
               (
                        (
                                 (
                                          (
                                                   (
                                                            (
                                                                     (Integer.hashCode(this.secondsPerTank) * 31 + Integer.hashCode(this.secondsPerTankHover))
                                                                           * 31
                                                                        + java.lang.Double.hashCode(this.horizontalSpeed)
                                                                  )
                                                                  * 31
                                                               + java.lang.Double.hashCode(this.verticalSpeed)
                                                         )
                                                         * 31
                                                      + java.lang.Double.hashCode(this.acceleration)
                                                )
                                                * 31
                                             + java.lang.Double.hashCode(this.hoverSpeed)
                                       )
                                       * 31
                                    + java.lang.Double.hashCode(this.swimModifier)
                              )
                              * 31
                           + java.lang.Double.hashCode(this.elytraBoost)
                     )
                     * 31
                  + (if (this.heightAboveGroundLimit == null) 0 else this.heightAboveGroundLimit.hashCode())
            )
            * 31
         + this.enchantments.hashCode();
   }

   public override operator fun equals(other: Any?): Boolean {
      if (this === other) {
         return true;
      } else if (other !is SyncedConfig) {
         return false;
      } else {
         val var2: SyncedConfig = other as SyncedConfig;
         if (this.secondsPerTank != (other as SyncedConfig).secondsPerTank) {
            return false;
         } else if (this.secondsPerTankHover != var2.secondsPerTankHover) {
            return false;
         } else if (java.lang.Double.compare(this.horizontalSpeed, var2.horizontalSpeed) != 0) {
            return false;
         } else if (java.lang.Double.compare(this.verticalSpeed, var2.verticalSpeed) != 0) {
            return false;
         } else if (java.lang.Double.compare(this.acceleration, var2.acceleration) != 0) {
            return false;
         } else if (java.lang.Double.compare(this.hoverSpeed, var2.hoverSpeed) != 0) {
            return false;
         } else if (java.lang.Double.compare(this.swimModifier, var2.swimModifier) != 0) {
            return false;
         } else if (java.lang.Double.compare(this.elytraBoost, var2.elytraBoost) != 0) {
            return false;
         } else if (!(this.heightAboveGroundLimit == var2.heightAboveGroundLimit)) {
            return false;
         } else {
            return this.enchantments == var2.enchantments;
         }
      }
   }
}

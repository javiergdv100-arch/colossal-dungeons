package com.colossaldungeons.enhanced.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Abstract base class for all CDE animated entities using GeckoLib.
 * Provides standard AnimatableInstanceCache setup and template for animation controllers.
 *
 * Subclasses must implement registerControllers() to define their animations.
 */
public abstract class CDEGeoEntity extends PathfinderMob implements GeoEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    protected CDEGeoEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    /**
     * Creates a base attribute supplier with sensible defaults for CDE mobs.
     * Subclasses should call this and customize via their own createAttributes().
     *
     * @return a basic attribute builder with health, speed, and knockback resistance
     */
    public static AttributeSupplier.Builder createBaseAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 20.0)
            .add(Attributes.MOVEMENT_SPEED, 0.25)
            .add(Attributes.ATTACK_DAMAGE, 3.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.0);
    }
}

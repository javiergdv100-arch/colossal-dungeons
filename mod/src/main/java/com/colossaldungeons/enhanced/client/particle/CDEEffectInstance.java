package com.colossaldungeons.enhanced.client.particle;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Wraps a visual effect instance with lifecycle tracking and optional entity/bone attachment.
 * Each instance represents one active effect in the world managed by CDEEffectManager.
 */
@OnlyIn(Dist.CLIENT)
public class CDEEffectInstance {

    private final String effectId;
    private Vec3 position;
    private float scale;
    private boolean finished;
    private int lifetime;
    private int ticksAlive;

    // Entity attachment (optional)
    private Entity attachedEntity;
    private String attachedBone;

    /**
     * Creates a new effect instance at a position.
     *
     * @param effectId the effect identifier
     * @param position the world position
     * @param scale the effect scale
     * @param lifetime maximum lifetime in ticks (0 for infinite until stopped)
     */
    public CDEEffectInstance(String effectId, Vec3 position, float scale, int lifetime) {
        this.effectId = effectId;
        this.position = position;
        this.scale = scale;
        this.finished = false;
        this.lifetime = lifetime;
        this.ticksAlive = 0;
    }

    /**
     * Attaches this effect to an entity bone for position tracking.
     *
     * @param entity the entity to attach to
     * @param bone the bone name (used for model attachment points)
     */
    public void attachToBone(Entity entity, String bone) {
        this.attachedEntity = entity;
        this.attachedBone = bone;
    }

    /**
     * Updates the position of this effect.
     * If attached to an entity, follows the entity position.
     */
    public void updatePosition() {
        if (attachedEntity != null) {
            if (attachedEntity.isRemoved()) {
                finished = true;
                return;
            }
            // Follow entity position (bone offset would be applied here with model data)
            position = attachedEntity.position().add(0, attachedEntity.getBbHeight() * 0.75, 0);
        }
    }

    /**
     * Ticks this effect instance.
     */
    public void tick() {
        ticksAlive++;
        updatePosition();

        if (lifetime > 0 && ticksAlive >= lifetime) {
            finished = true;
        }
    }

    /**
     * Stops this effect, marking it as finished.
     */
    public void stop() {
        finished = true;
    }

    /**
     * Whether this effect has finished playing and should be removed.
     */
    public boolean isFinished() {
        return finished;
    }

    // ========== Getters ==========

    public String getEffectId() {
        return effectId;
    }

    public Vec3 getPosition() {
        return position;
    }

    public float getScale() {
        return scale;
    }

    public int getTicksAlive() {
        return ticksAlive;
    }

    public Entity getAttachedEntity() {
        return attachedEntity;
    }

    public String getAttachedBone() {
        return attachedBone;
    }
}

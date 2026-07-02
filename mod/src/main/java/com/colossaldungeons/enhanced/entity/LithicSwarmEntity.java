package com.colossaldungeons.enhanced.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.List;

/**
 * Lithic Swarm - Worldbearer dungeon creature.
 *
 * A cloud of living pebbles that swarms around players. Each individual pebble
 * has very low HP but the swarm as a whole is dangerous through sustained contact.
 * While in contact with a player, degrades their armor durability over time.
 *
 * Stats: 3 HP per entity (swarm representation), 2 damage per tick on contact.
 * Vulnerability: AoE attacks destroy the entire swarm. Water disperses instantly.
 * Behavior: Continuous contact damage + armor degradation.
 */
public class LithicSwarmEntity extends CDEGeoEntity implements GeoEntity {

    private static final EntityDataAccessor<Integer> SWARM_SIZE =
        SynchedEntityData.defineId(LithicSwarmEntity.class, EntityDataSerializers.INT);

    private static final int MAX_SWARM_SIZE = 20;
    private static final int ARMOR_DEGRADE_INTERVAL = 40; // Every 2 seconds
    private static final int ARMOR_DEGRADE_AMOUNT = 2;
    private static final double CONTACT_RANGE = 1.5;
    private static final float CONTACT_DAMAGE = 2.0f;
    private static final int CONTACT_DAMAGE_INTERVAL = 20; // Every 1 second

    private int contactDamageTimer = 0;
    private int armorDegradeTimer = 0;

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.lithic_swarm.idle");
    private static final RawAnimation SWIRL = RawAnimation.begin().thenLoop("animation.lithic_swarm.swirl");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenLoop("animation.lithic_swarm.attack");
    private static final RawAnimation DISPERSE = RawAnimation.begin().thenPlay("animation.lithic_swarm.disperse");

    public LithicSwarmEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * Creates the attribute supplier for Lithic Swarm.
     * 3 HP, 0.3 speed, 2 damage, 0 armor, 0 knockback resistance.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 3.0)
            .add(Attributes.MOVEMENT_SPEED, 0.3)
            .add(Attributes.ATTACK_DAMAGE, 2.0)
            .add(Attributes.ARMOR, 0.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.0)
            .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SWARM_SIZE, MAX_SWARM_SIZE);
    }

    public int getSwarmSize() {
        return this.entityData.get(SWARM_SIZE);
    }

    private void setSwarmSize(int size) {
        this.entityData.set(SWARM_SIZE, Math.max(0, Math.min(MAX_SWARM_SIZE, size)));
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2, true));
        this.goalSelector.addGoal(2, new RandomStrollGoal(this, 0.8));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            // Water check - disperses entire swarm instantly
            if (this.isInWaterOrBubble() || this.isInWaterRainOrBubble()) {
                disperseSwarm();
                return;
            }

            // Contact damage and armor degradation
            AABB contactBox = this.getBoundingBox().inflate(CONTACT_RANGE);
            List<Player> nearbyPlayers = this.level().getEntitiesOfClass(
                Player.class, contactBox, p -> !p.isSpectator() && !p.isCreative());

            if (!nearbyPlayers.isEmpty()) {
                contactDamageTimer++;
                armorDegradeTimer++;

                for (Player player : nearbyPlayers) {
                    // Deal contact damage periodically
                    if (contactDamageTimer >= CONTACT_DAMAGE_INTERVAL) {
                        player.hurt(this.damageSources().mobAttack(this), CONTACT_DAMAGE);
                    }

                    // Degrade armor durability
                    if (armorDegradeTimer >= ARMOR_DEGRADE_INTERVAL) {
                        degradeArmor(player);
                    }
                }

                if (contactDamageTimer >= CONTACT_DAMAGE_INTERVAL) {
                    contactDamageTimer = 0;
                }
                if (armorDegradeTimer >= ARMOR_DEGRADE_INTERVAL) {
                    armorDegradeTimer = 0;
                }
            } else {
                contactDamageTimer = 0;
                armorDegradeTimer = 0;
            }
        }
    }

    /**
     * Degrades a random piece of equipped armor on the player.
     */
    private void degradeArmor(Player player) {
        EquipmentSlot[] armorSlots = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
        };

        for (EquipmentSlot slot : armorSlots) {
            ItemStack armor = player.getItemBySlot(slot);
            if (!armor.isEmpty() && armor.isDamageableItem()) {
                armor.hurtAndBreak(ARMOR_DEGRADE_AMOUNT, player, slot);
                break; // Only degrade one piece per interval
            }
        }
    }

    /**
     * Disperses the entire swarm (killed by water contact).
     */
    private void disperseSwarm() {
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CLOUD,
                this.getX(), this.getY() + 0.5, this.getZ(),
                20, 0.5, 0.5, 0.5, 0.05);
        }
        this.level().playSound(null, this.blockPosition(),
            SoundEvents.GRAVEL_BREAK, SoundSource.HOSTILE, 1.0f, 1.5f);
        this.discard();
    }

    /**
     * AoE attacks are extra effective - kills entire swarm entity.
     * Explosions and sweep attacks deal double damage.
     */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        // AoE attacks (explosions, sweep) are extremely effective
        if (source.isIndirect() || amount > 5.0f) {
            amount *= 3.0f; // Guaranteed kill from AoE
        }
        return super.hurt(source, amount);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 3, state -> {
            LivingEntity target = this.getTarget();
            if (target != null && this.distanceTo(target) <= CONTACT_RANGE + 1.0) {
                return state.setAndContinue(ATTACK);
            }
            if (state.isMoving()) {
                return state.setAndContinue(SWIRL);
            }
            return state.setAndContinue(IDLE);
        }));
    }
}

package com.colossaldungeons.enhanced.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

/**
 * The Whisperer - An invisible entity from the Descent into Madness dungeon.
 *
 * Invisible until speaking. Voice comes from a false direction (guides to traps).
 * Torch light reveals its silhouette. Sculk sensors find its real position.
 *
 * Stats: 20 HP, 0.3 speed, 6 attack damage.
 */
public class TheWhispererEntity extends CDEGeoEntity implements GeoEntity {

    private static final EntityDataAccessor<Boolean> IS_SPEAKING =
        SynchedEntityData.defineId(TheWhispererEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Boolean> IS_REVEALED =
        SynchedEntityData.defineId(TheWhispererEntity.class, EntityDataSerializers.BOOLEAN);

    private static final int WHISPER_INTERVAL = 100; // 5 seconds
    private static final int WHISPER_DURATION = 30;
    private static final int TORCH_REVEAL_LIGHT_LEVEL = 8;
    private static final double FALSE_VOICE_OFFSET = 8.0;

    private int whisperCooldown = 0;
    private int whisperTimer = 0;

    private static final RawAnimation IDLE_INVISIBLE = RawAnimation.begin().thenLoop("animation.the_whisperer.idle_invisible");
    private static final RawAnimation IDLE_VISIBLE = RawAnimation.begin().thenLoop("animation.the_whisperer.idle_visible");
    private static final RawAnimation SPEAK = RawAnimation.begin().thenPlay("animation.the_whisperer.speak");
    private static final RawAnimation MOVE = RawAnimation.begin().thenLoop("animation.the_whisperer.move");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("animation.the_whisperer.attack");

    public TheWhispererEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * Creates the attribute supplier for The Whisperer.
     * 20 HP, 0.3 speed, 6 damage.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 20.0)
            .add(Attributes.MOVEMENT_SPEED, 0.3)
            .add(Attributes.ATTACK_DAMAGE, 6.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.0)
            .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(IS_SPEAKING, false);
        builder.define(IS_REVEALED, false);
    }

    public boolean isSpeaking() {
        return this.entityData.get(IS_SPEAKING);
    }

    public boolean isRevealed() {
        return this.entityData.get(IS_REVEALED);
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
            if (whisperCooldown > 0) whisperCooldown--;

            // Check torch reveal
            int lightLevel = this.level().getBrightness(LightLayer.BLOCK, this.blockPosition());
            this.entityData.set(IS_REVEALED, lightLevel >= TORCH_REVEAL_LIGHT_LEVEL);

            // Whisper from false direction
            if (isSpeaking()) {
                whisperTimer--;
                if (whisperTimer <= 0) {
                    this.entityData.set(IS_SPEAKING, false);
                }
            } else if (whisperCooldown <= 0) {
                Player target = this.level().getNearestPlayer(this, 20.0);
                if (target != null) {
                    performWhisper(target);
                    whisperCooldown = WHISPER_INTERVAL;
                    whisperTimer = WHISPER_DURATION;
                    this.entityData.set(IS_SPEAKING, true);
                }
            }
        }
    }

    /**
     * Plays a whisper sound from a false direction to mislead the player.
     * The sound is played at an offset position from the entity's real location.
     */
    private void performWhisper(Player target) {
        // Calculate a false position (opposite or perpendicular to real position)
        Vec3 toTarget = target.position().subtract(this.position()).normalize();
        Vec3 falseOffset = toTarget.yRot((float) (Math.PI * (0.5 + this.random.nextDouble())));
        Vec3 falsePos = target.position().add(falseOffset.scale(FALSE_VOICE_OFFSET));

        // Play whisper sound at false position
        BlockPos soundPos = BlockPos.containing(falsePos);
        this.level().playSound(null, soundPos,
            SoundEvents.AMBIENT_CAVE.value(), SoundSource.HOSTILE, 0.8f, 1.5f);
    }

    @Override
    public boolean isInvisible() {
        if (!isRevealed() && !isSpeaking()) {
            return true;
        }
        return super.isInvisible();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Can still be hurt even when invisible (sculk sensors can find it)
        return super.hurt(source, amount);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, state -> {
            if (isSpeaking()) {
                return state.setAndContinue(SPEAK);
            }
            if (!isRevealed()) {
                return state.setAndContinue(IDLE_INVISIBLE);
            }
            if (state.isMoving()) {
                return state.setAndContinue(MOVE);
            }
            return state.setAndContinue(IDLE_VISIBLE);
        }));

        controllers.add(new AnimationController<>(this, "attack", 3, state -> {
            if (this.swinging) {
                return state.setAndContinue(ATTACK);
            }
            state.getController().forceAnimationReset();
            return state.setAndContinue(IDLE_VISIBLE);
        }));
    }
}

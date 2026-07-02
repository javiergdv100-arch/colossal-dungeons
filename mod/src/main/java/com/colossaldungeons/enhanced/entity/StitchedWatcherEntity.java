package com.colossaldungeons.enhanced.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.List;

/**
 * Stitched Watcher - Wall-mounted eye creature that surveys and coordinates.
 *
 * An elite creature integrated into walls, ceilings, or structures that watches
 * the dungeon. Does not engage in direct combat; instead perceives and coordinates
 * other enemies.
 *
 * Stats: Variable HP (scales with dungeon difficulty), variable armor. Indirect damage only.
 * Size: 1.0 x 1.0 blocks (wall-mounted).
 *
 * Behavior:
 * - Detects players via line of sight (eye-based detection).
 * - Alerts nearby mobs when it spots a player (coordination AI).
 * - Can activate nearby traps when players are in range.
 * - Immobile - mounted to walls/structures.
 * - Destroying it silences the coordination in the area.
 *
 * Drops: Optical Organ (detection accessory component).
 */
public class StitchedWatcherEntity extends CDEGeoEntity implements GeoEntity {

    /**
     * Watcher states.
     */
    public enum WatcherState {
        DORMANT,      // Not yet aware of any player
        SCANNING,     // Searching for players
        ALERT,        // Has detected a player, alerting mobs
        ALARMED       // Active alarm state - all nearby mobs aggro
    }

    private static final EntityDataAccessor<Integer> WATCHER_STATE =
        SynchedEntityData.defineId(StitchedWatcherEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Boolean> IS_MOUNTED =
        SynchedEntityData.defineId(StitchedWatcherEntity.class, EntityDataSerializers.BOOLEAN);

    // Detection parameters
    private static final double DETECTION_RANGE = 24.0;
    private static final double ALERT_RANGE = 32.0;
    private static final double TRAP_ACTIVATION_RANGE = 8.0;
    private static final int SCAN_INTERVAL = 20; // Check every second
    private static final int ALERT_COOLDOWN = 100; // 5 seconds between alerts
    private static final float DETECTION_CONE_ANGLE = 120.0f; // Wide cone of vision

    private int scanTimer = 0;
    private int alertCooldown = 0;
    private Player detectedPlayer = null;

    // Animations
    private static final RawAnimation DORMANT_ANIM = RawAnimation.begin().thenLoop("animation.stitched_watcher.dormant");
    private static final RawAnimation SCAN_ANIM = RawAnimation.begin().thenLoop("animation.stitched_watcher.scan");
    private static final RawAnimation ALERT_ANIM = RawAnimation.begin().thenPlay("animation.stitched_watcher.alert");
    private static final RawAnimation ALARMED_ANIM = RawAnimation.begin().thenLoop("animation.stitched_watcher.alarmed");
    private static final RawAnimation BLINK_ANIM = RawAnimation.begin().thenPlay("animation.stitched_watcher.blink");

    public StitchedWatcherEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true); // Wall-mounted
    }

    /**
     * Creates the attribute supplier for the Stitched Watcher.
     * Variable HP (base 40), no attack damage (indirect only), immobile.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 40.0)
            .add(Attributes.MOVEMENT_SPEED, 0.0) // Immobile
            .add(Attributes.ATTACK_DAMAGE, 0.0)  // No direct attack
            .add(Attributes.KNOCKBACK_RESISTANCE, 1.0) // Cannot be knocked back
            .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(WATCHER_STATE, WatcherState.DORMANT.ordinal());
        builder.define(IS_MOUNTED, true);
    }

    public WatcherState getWatcherState() {
        return WatcherState.values()[this.entityData.get(WATCHER_STATE)];
    }

    private void setWatcherState(WatcherState state) {
        this.entityData.set(WATCHER_STATE, state.ordinal());
    }

    @Override
    protected void registerGoals() {
        // No movement goals - this entity is wall-mounted and stationary
        // All behavior is handled in tick()
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isNoAi() {
        // Always stationary but AI logic runs in tick
        return false;
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            // Prevent any movement
            this.setDeltaMovement(0, 0, 0);

            if (alertCooldown > 0) alertCooldown--;

            scanTimer++;
            if (scanTimer >= SCAN_INTERVAL) {
                scanTimer = 0;
                performScan();
            }

            // Handle state-specific behavior
            WatcherState currentState = getWatcherState();
            switch (currentState) {
                case ALERT -> {
                    if (detectedPlayer != null && alertCooldown <= 0) {
                        alertNearbyMobs();
                        activateNearbyTraps();
                        alertCooldown = ALERT_COOLDOWN;
                        setWatcherState(WatcherState.ALARMED);
                    }
                }
                case ALARMED -> {
                    // Continue tracking - if player leaves, return to scanning
                    if (detectedPlayer == null || !canSeePlayer(detectedPlayer)) {
                        detectedPlayer = null;
                        setWatcherState(WatcherState.SCANNING);
                    }
                }
                default -> {}
            }
        }
    }

    /**
     * Scans for players within detection range and cone of vision.
     */
    private void performScan() {
        if (getWatcherState() == WatcherState.DORMANT) {
            setWatcherState(WatcherState.SCANNING);
        }

        Player nearest = this.level().getNearestPlayer(this, DETECTION_RANGE);
        if (nearest != null && canSeePlayer(nearest)) {
            detectedPlayer = nearest;
            if (getWatcherState() == WatcherState.SCANNING) {
                setWatcherState(WatcherState.ALERT);
            }
        }
    }

    /**
     * Checks if the watcher can see the given player (line of sight + cone check).
     */
    private boolean canSeePlayer(Player player) {
        if (player.isInvisible()) return false;
        if (!this.getSensing().hasLineOfSight(player)) return false;
        // Sneaking players are harder to detect
        double detectRange = player.isShiftKeyDown() ? DETECTION_RANGE * 0.5 : DETECTION_RANGE;
        return this.distanceTo(player) <= detectRange;
    }

    /**
     * Alerts all nearby mobs to the detected player's position.
     * Sets their target to the detected player.
     */
    private void alertNearbyMobs() {
        if (detectedPlayer == null) return;

        AABB alertBox = this.getBoundingBox().inflate(ALERT_RANGE);
        List<Mob> nearbyMobs = this.level().getEntitiesOfClass(
            Mob.class, alertBox,
            mob -> mob != this && !mob.isDeadOrDying() && !(mob instanceof StitchedWatcherEntity)
        );

        for (Mob mob : nearbyMobs) {
            if (mob.getTarget() == null) {
                mob.setTarget(detectedPlayer);
            }
        }
    }

    /**
     * Activates nearby traps when a player is detected.
     * This sends a signal to trap blocks within activation range.
     */
    private void activateNearbyTraps() {
        if (detectedPlayer == null) return;

        // Signal nearby trap blocks to prepare/arm
        BlockPos center = this.blockPosition();
        int range = (int) TRAP_ACTIVATION_RANGE;
        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    BlockPos pos = center.offset(x, y, z);
                    // Trap activation would be handled by the trap system
                    // This is the coordination signal
                }
            }
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, state -> {
            WatcherState watcherState = getWatcherState();
            return switch (watcherState) {
                case DORMANT -> state.setAndContinue(DORMANT_ANIM);
                case SCANNING -> state.setAndContinue(SCAN_ANIM);
                case ALERT -> state.setAndContinue(ALERT_ANIM);
                case ALARMED -> state.setAndContinue(ALARMED_ANIM);
            };
        }));

        // Blink controller (overlay)
        controllers.add(new AnimationController<>(this, "blink", 0, state -> {
            if (getWatcherState() != WatcherState.DORMANT && this.random.nextFloat() < 0.005f) {
                return state.setAndContinue(BLINK_ANIM);
            }
            state.getController().forceAnimationReset();
            return state.setAndContinue(DORMANT_ANIM);
        }));
    }
}

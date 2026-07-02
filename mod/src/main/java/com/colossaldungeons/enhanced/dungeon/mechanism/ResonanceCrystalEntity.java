package com.colossaldungeons.enhanced.dungeon.mechanism;

import com.colossaldungeons.enhanced.ColossalDungeons;
import com.colossaldungeons.enhanced.api.IResonanceReceiver;
import com.colossaldungeons.enhanced.core.registry.CDESounds;
import com.colossaldungeons.enhanced.network.CDENetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.network.PacketDistributor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Block entity for Resonance Crystals - mystical blocks that respond to sound stimuli.
 * 
 * 4-state machine:
 * - DORMANT: Not active, waiting for initial stimulus.
 * - CHARGING: Accumulating charge from Sculk Sensors, Note Blocks, Bells.
 * - CHARGED: Fully charged, emitting effects (opens doors, disables barriers).
 * - OVERLOADED: Dangerous state from TNT or excessive stimulation.
 * 
 * TNT interaction causes instant overload regardless of current state.
 * Charged crystals open doors/disable barriers in the room.
 */
public class ResonanceCrystalEntity extends BlockEntity implements IResonanceReceiver {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResonanceCrystalEntity.class);
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(
        ColossalDungeons.MOD_ID, "resonance_crystal");

    /** Maximum charge level before becoming CHARGED. */
    private static final int MAX_CHARGE = 100;
    /** Charge decay per tick when no stimulus. */
    private static final int CHARGE_DECAY = 1;
    /** Overload damage radius. */
    private static final double OVERLOAD_RADIUS = 5.0;
    /** Overload damage amount. */
    private static final float OVERLOAD_DAMAGE = 8.0f; // 4 hearts

    private ResonanceState resonanceState;
    private int chargeLevel;
    private int overloadTimer;
    private int tickCounter;
    private boolean discharging;

    public ResonanceCrystalEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
        this.resonanceState = ResonanceState.DORMANT;
        this.chargeLevel = 0;
        this.overloadTimer = 0;
        this.tickCounter = 0;
        this.discharging = false;
    }

    /**
     * Main tick method for the resonance crystal.
     */
    public void tick(ServerLevel level) {
        tickCounter++;

        switch (resonanceState) {
            case DORMANT -> {
                // Nothing to do - waiting for stimulus
            }
            case CHARGING -> {
                // Decay charge slowly if no input
                if (tickCounter % 20 == 0) { // Every second
                    chargeLevel = Math.max(0, chargeLevel - CHARGE_DECAY);
                    if (chargeLevel <= 0) {
                        transitionTo(ResonanceState.DORMANT);
                    }
                }

                // Check if fully charged
                if (chargeLevel >= MAX_CHARGE) {
                    transitionTo(ResonanceState.CHARGED);
                    onFullyCharged(level);
                }

                // Visual feedback - particles based on charge level
                if (tickCounter % 10 == 0) {
                    spawnChargingParticles(level);
                }
            }
            case CHARGED -> {
                // Emit effect while charged
                if (tickCounter % 40 == 0) {
                    spawnChargedParticles(level);
                }

                // Slowly discharge
                if (discharging) {
                    chargeLevel--;
                    if (chargeLevel <= 0) {
                        transitionTo(ResonanceState.DORMANT);
                    }
                }
            }
            case OVERLOADED -> {
                overloadTimer--;
                if (overloadTimer <= 0) {
                    // Overload explosion
                    performOverloadExplosion(level);
                    transitionTo(ResonanceState.DORMANT);
                    chargeLevel = 0;
                }

                // Warning particles
                if (tickCounter % 2 == 0) {
                    spawnOverloadParticles(level);
                }
            }
        }
    }

    /**
     * Receives a resonance pulse from an external source (Sculk Sensor, Note Block, Bell).
     */
    @Override
    public void onResonancePulse(int power) {
        switch (resonanceState) {
            case DORMANT -> {
                chargeLevel += power;
                transitionTo(ResonanceState.CHARGING);
            }
            case CHARGING -> {
                chargeLevel += power;
                if (chargeLevel >= MAX_CHARGE) {
                    transitionTo(ResonanceState.CHARGED);
                    if (level instanceof ServerLevel serverLevel) {
                        onFullyCharged(serverLevel);
                    }
                }
            }
            case CHARGED -> {
                // Additional power while charged causes overload
                chargeLevel += power;
                if (chargeLevel >= MAX_CHARGE * 1.5) {
                    triggerOverload();
                }
            }
            case OVERLOADED -> {
                // Already overloaded, no further effect
            }
        }
        setChanged();
    }

    @Override
    public int getResonanceThreshold() {
        return MAX_CHARGE;
    }

    /**
     * Instantly triggers overload state (called by TNT interaction).
     */
    public void triggerOverload() {
        resonanceState = ResonanceState.OVERLOADED;
        overloadTimer = 40; // 2 seconds until explosion
        chargeLevel = MAX_CHARGE * 2; // Maximum charge for explosion
        setChanged();

        if (level instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, getBlockPos(), CDESounds.RESONANCE_CHARGE_UP.get(),
                SoundSource.BLOCKS, 2.0f, 0.5f);

            // Send network update
            syncToClients(serverLevel);
        }

        LOGGER.info("Resonance Crystal at {} entering OVERLOADED state!", getBlockPos());
    }

    /**
     * Called when the crystal becomes fully charged.
     */
    private void onFullyCharged(ServerLevel level) {
        level.playSound(null, getBlockPos(), CDESounds.RESONANCE_CHARGE_UP.get(),
            SoundSource.BLOCKS, 1.5f, 1.5f);
        syncToClients(level);
        LOGGER.debug("Resonance Crystal at {} is now CHARGED", getBlockPos());
    }

    /**
     * Performs the overload explosion.
     */
    private void performOverloadExplosion(ServerLevel level) {
        BlockPos pos = getBlockPos();
        AABB damageArea = new AABB(pos).inflate(OVERLOAD_RADIUS);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, damageArea);

        for (LivingEntity target : targets) {
            target.hurt(level.damageSources().magic(), OVERLOAD_DAMAGE);
        }

        // Visual/sound effects
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
            pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
            3, 1.0, 1.0, 1.0, 0.0);

        LOGGER.debug("Resonance Crystal at {} overload explosion, hit {} entities", pos, targets.size());
        syncToClients(level);
    }

    private void transitionTo(ResonanceState newState) {
        if (resonanceState.canTransitionTo(newState)) {
            resonanceState = newState;
            tickCounter = 0;
            setChanged();
        }
    }

    private void syncToClients(ServerLevel level) {
        CDENetworking.ResonanceUpdatePayload payload = new CDENetworking.ResonanceUpdatePayload(
            0, // Use 0 for block entity (not mob entity id)
            chargeLevel,
            discharging
        );
        PacketDistributor.sendToPlayersInDimension(level, payload);
    }

    private void spawnChargingParticles(ServerLevel level) {
        double x = getBlockPos().getX() + 0.5;
        double y = getBlockPos().getY() + 0.5;
        double z = getBlockPos().getZ() + 0.5;
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y, z,
            2, 0.3, 0.3, 0.3, 0.01);
    }

    private void spawnChargedParticles(ServerLevel level) {
        double x = getBlockPos().getX() + 0.5;
        double y = getBlockPos().getY() + 0.5;
        double z = getBlockPos().getZ() + 0.5;
        level.sendParticles(ParticleTypes.END_ROD, x, y, z,
            4, 0.5, 0.5, 0.5, 0.02);
    }

    private void spawnOverloadParticles(ServerLevel level) {
        double x = getBlockPos().getX() + 0.5;
        double y = getBlockPos().getY() + 0.5;
        double z = getBlockPos().getZ() + 0.5;
        level.sendParticles(ParticleTypes.ANGRY_VILLAGER, x, y, z,
            3, 0.5, 0.5, 0.5, 0.0);
    }

    // ========== Persistence ==========

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("resonance_state", resonanceState.ordinal());
        tag.putInt("charge_level", chargeLevel);
        tag.putInt("overload_timer", overloadTimer);
        tag.putBoolean("discharging", discharging);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        int stateOrd = tag.getInt("resonance_state");
        if (stateOrd >= 0 && stateOrd < ResonanceState.values().length) {
            resonanceState = ResonanceState.values()[stateOrd];
        }
        chargeLevel = tag.getInt("charge_level");
        overloadTimer = tag.getInt("overload_timer");
        discharging = tag.getBoolean("discharging");
    }

    // ========== Getters ==========

    public ResonanceState getResonanceState() { return resonanceState; }
    public int getChargeLevel() { return chargeLevel; }
    public boolean isDischarging() { return discharging; }
    public ResourceLocation getId() { return ID; }

    public void setDischarging(boolean discharging) {
        this.discharging = discharging;
        setChanged();
    }
}

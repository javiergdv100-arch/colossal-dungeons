package com.colossaldungeons.enhanced.dungeon.mechanism;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import com.colossaldungeons.enhanced.ColossalDungeons;

/**
 * Brazier mechanism. Has lit/unlit state. Can be ignited by fire-related items
 * (flint and steel, fire charge, torches). When lit, provides light and activates
 * fire-based triggers in the room.
 */
public class BrazierBlockEntity extends MechanismBlockEntity {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(
        ColossalDungeons.MOD_ID, "brazier");

    private boolean lit;
    private int burnDuration; // Ticks remaining; 0 = infinite
    private int particleTimer;

    public BrazierBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
        this.lit = false;
        this.burnDuration = 0;
        this.particleTimer = 0;
    }

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public boolean canActivate(ServerPlayer player) {
        if (lit) return false;
        if (!super.canActivate(player)) return false;
        // Requires fire-starting item
        return player.getMainHandItem().is(Items.FLINT_AND_STEEL)
            || player.getMainHandItem().is(Items.FIRE_CHARGE)
            || player.getMainHandItem().is(Items.TORCH);
    }

    @Override
    protected int getActivationDelay() {
        return 10; // Quick ignition
    }

    @Override
    protected int getDeactivationDelay() {
        return 20; // Slower extinguish
    }

    @Override
    protected void onActivate(ServerPlayer player, Level level) {
        lit = true;

        // Damage flint and steel, consume fire charge
        if (player.getMainHandItem().is(Items.FLINT_AND_STEEL)) {
            player.getMainHandItem().hurtAndBreak(1, player, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
        } else if (player.getMainHandItem().is(Items.FIRE_CHARGE)) {
            player.getMainHandItem().shrink(1);
        }

        level.playSound(null, getBlockPos(), SoundEvents.FLINTANDSTEEL_USE,
            SoundSource.BLOCKS, 1.0f, 1.0f);
    }

    @Override
    protected void onDeactivate() {
        lit = false;
    }

    @Override
    protected void onActivationComplete(ServerLevel level) {
        level.playSound(null, getBlockPos(), SoundEvents.CAMPFIRE_CRACKLE,
            SoundSource.BLOCKS, 0.8f, 1.0f);
    }

    @Override
    protected void onDeactivationComplete(ServerLevel level) {
        level.playSound(null, getBlockPos(), SoundEvents.FIRE_EXTINGUISH,
            SoundSource.BLOCKS, 0.8f, 1.0f);
    }

    @Override
    protected void tickActive(ServerLevel level) {
        particleTimer++;

        // Spawn fire particles
        if (particleTimer % 5 == 0) {
            double x = getBlockPos().getX() + 0.5;
            double y = getBlockPos().getY() + 1.0;
            double z = getBlockPos().getZ() + 0.5;
            level.sendParticles(ParticleTypes.FLAME, x, y, z, 2,
                0.1, 0.1, 0.1, 0.01);
        }

        // Check burn duration
        if (burnDuration > 0) {
            burnDuration--;
            if (burnDuration <= 0) {
                deactivate();
            }
        }
    }

    /**
     * Extinguishes the brazier (e.g., from water interaction).
     */
    public void extinguish(ServerLevel level) {
        if (lit) {
            lit = false;
            deactivate();
            level.playSound(null, getBlockPos(), SoundEvents.FIRE_EXTINGUISH,
                SoundSource.BLOCKS, 1.0f, 1.0f);
        }
    }

    /**
     * Sets the burn duration in ticks. 0 means infinite (until manually extinguished).
     */
    public void setBurnDuration(int ticks) {
        this.burnDuration = ticks;
    }

    @Override
    protected void saveExtraData(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putBoolean("lit", lit);
        tag.putInt("burn_duration", burnDuration);
    }

    @Override
    protected void loadExtraData(CompoundTag tag, HolderLookup.Provider registries) {
        lit = tag.getBoolean("lit");
        burnDuration = tag.getInt("burn_duration");
    }

    public boolean isLit() { return lit; }
}

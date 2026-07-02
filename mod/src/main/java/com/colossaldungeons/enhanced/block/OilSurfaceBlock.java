package com.colossaldungeons.enhanced.block;

import com.colossaldungeons.enhanced.core.registry.CDEEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

/**
 * Oil Surface block - a thin layer of oil on the ground (like carpet).
 * 
 * Behavior:
 * - No collision (entities walk through it)
 * - When stepped on: applies OiledEffect unless entity has OIL_PROTECTION
 * - Ignited by lava (ignitedByLava property set in CDEBlocks)
 * - Bone meal interaction: removes block + particles (handled in BoneMealOilInteraction)
 * - Water bucket: creates steam explosion AoE
 */
public class OilSurfaceBlock extends Block {

    /** Visual shape (thin layer, like carpet). */
    private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 1.0, 16.0);

    /** Duration of oiled effect applied on step (10 seconds). */
    private static final int OILED_DURATION = 200;

    /** Radius for steam explosion AoE. */
    private static final double STEAM_EXPLOSION_RADIUS = 3.0;

    /** Damage dealt by steam explosion. */
    private static final float STEAM_DAMAGE = 4.0f;

    public OilSurfaceBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!level.isClientSide() && entity instanceof LivingEntity livingEntity) {
            // Check if entity has Oil Protection - if so, do not apply Oiled
            if (!livingEntity.hasEffect(CDEEffects.OIL_PROTECTION)) {
                livingEntity.addEffect(new MobEffectInstance(
                    CDEEffects.OILED, OILED_DURATION, 0,
                    false, true, true
                ));
            }
        }
        super.stepOn(level, pos, state, entity);
    }

    /**
     * Handles the water bucket interaction creating a steam explosion AoE.
     * Called from an event handler when a water bucket is used on this block.
     *
     * @param level The server level
     * @param pos The block position of the oil surface
     */
    public void createSteamExplosion(ServerLevel level, BlockPos pos) {
        // Remove the oil block
        level.removeBlock(pos, false);

        // Spawn steam particles
        for (int i = 0; i < 20; i++) {
            double x = pos.getX() + 0.5 + (level.random.nextDouble() - 0.5) * 2.0;
            double y = pos.getY() + 0.5 + level.random.nextDouble() * 2.0;
            double z = pos.getZ() + 0.5 + (level.random.nextDouble() - 0.5) * 2.0;
            level.sendParticles(ParticleTypes.CLOUD, x, y, z, 1, 0, 0.1, 0, 0.05);
        }

        // Play steam sound
        level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0f, 0.8f);

        // Deal damage to entities in radius
        AABB aoe = new AABB(pos).inflate(STEAM_EXPLOSION_RADIUS);
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, aoe);
        for (LivingEntity entity : entities) {
            entity.hurt(entity.damageSources().hotFloor(), STEAM_DAMAGE);
        }
    }
}

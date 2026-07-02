package com.colossaldungeons.enhanced.block;

import com.colossaldungeons.enhanced.core.registry.CDEEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Membrane Block - semi-transparent, selective permeability.
 * 
 * Behavior:
 * - Certain mob types can pass through (Monsters)
 * - Players cannot pass unless they have Resonance Charge effect active
 * - Custom collision shape: allows select entities through
 * - Semi-transparent rendering
 */
public class MembraneBlock extends Block {

    private static final VoxelShape VISUAL_SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0);

    public MembraneBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return VISUAL_SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // Check if the entity can pass through
        if (context instanceof EntityCollisionContext entityContext) {
            Entity entity = entityContext.getEntity();
            if (entity != null && canPassThrough(entity)) {
                return Shapes.empty();
            }
        }
        return VISUAL_SHAPE;
    }

    /**
     * Determines whether an entity can pass through this membrane.
     *
     * @param entity The entity attempting to pass
     * @return true if the entity can pass through
     */
    private boolean canPassThrough(Entity entity) {
        // Monsters can always pass through
        if (entity instanceof Monster) {
            return true;
        }

        // Players with Resonance Charge effect can pass through
        if (entity instanceof LivingEntity living) {
            return living.hasEffect(CDEEffects.RESONANCE_CHARGE);
        }

        return false;
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    public float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        // Semi-transparent appearance
        return 0.7f;
    }
}

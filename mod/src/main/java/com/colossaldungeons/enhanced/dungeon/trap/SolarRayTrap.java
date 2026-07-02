package com.colossaldungeons.enhanced.dungeon.trap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Solar Ray Trap - Deadly redirected sunlight beam across room.
 * 
 * Mechanics:
 * - Concentrated sunlight beam sweeps across the room via mirrors
 * - Shield REFLECTS the beam (redirects it)
 * - Fishing rod rotates mirrors, redirecting beam path
 * - Water creates dispersing vapor (safe for 5 seconds)
 * - Powder snow creates blocking fog
 * - Beam deals massive fire + radiant damage
 * 
 * Config values used:
 * - damage.amount: beam damage per tick of contact
 * - damage.radius: beam width and room scan range
 * - timing.warningTicks: beam warmup (light intensifies)
 * - timing.activeTicks: active sweep duration
 */
public class SolarRayTrap extends AbstractTrap {

    private static final int WATER_VAPOR_SAFE_TICKS = 100; // 5 seconds
    private static final int POWDER_SNOW_FOG_TICKS = 60;
    private static final float BEAM_SWEEP_SPEED = 0.05f;
    private static final float REFLECT_DAMAGE_MULTIPLIER = 0.5f;

    private float beamAngle;
    private Direction beamDirection;
    private boolean obscuredByVapor;
    private int vaporTimer;
    private boolean obscuredByFog;
    private int fogTimer;
    private boolean beamReflected;

    public SolarRayTrap(BlockPos position, TrapConfig config) {
        super(position, config);
        this.beamAngle = 0;
        this.beamDirection = Direction.EAST;
        this.obscuredByVapor = false;
        this.vaporTimer = 0;
        this.obscuredByFog = false;
        this.fogTimer = 0;
        this.beamReflected = false;
    }

    @Override
    protected boolean shouldPrepare(ServerLevel level) {
        List<LivingEntity> nearby = getTargetsInRange(level, config.activator().range());
        return !nearby.isEmpty();
    }

    @Override
    protected boolean shouldArm(ServerLevel level) {
        return tickCounter >= config.timing().warningTicks();
    }

    @Override
    protected void prepare(ServerLevel level) {
        // Light intensifies in the room
        // Mirrors begin to glow and align
        beamAngle = 0;
    }

    @Override
    protected void arm(ServerLevel level) {
        // Beam is focused - mirrors locked in position
        // Intense heat radiates from focal point
    }

    @Override
    protected void trigger(ServerLevel level) {
        // Beam fires - sweeping begins
    }

    @Override
    protected void damage(ServerLevel level) {
        // Handle water vapor obscuring
        if (obscuredByVapor) {
            vaporTimer--;
            if (vaporTimer <= 0) {
                obscuredByVapor = false;
            }
            return; // Safe during vapor
        }

        // Handle powder snow fog
        if (obscuredByFog) {
            fogTimer--;
            if (fogTimer <= 0) {
                obscuredByFog = false;
            }
            return; // Safe during fog
        }

        // Sweep beam across room
        beamAngle += BEAM_SWEEP_SPEED;
        if (beamAngle > Math.PI * 2) {
            beamAngle = 0;
        }

        // Calculate beam path
        double beamReach = config.damage().radius();
        for (int i = 1; i <= (int) beamReach; i++) {
            double beamX = position.getX() + Math.cos(beamAngle) * i;
            double beamZ = position.getZ() + Math.sin(beamAngle) * i;
            BlockPos beamPos = BlockPos.containing(beamX, position.getY(), beamZ);
            AABB beamArea = new AABB(beamPos).inflate(0.5);

            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, beamArea);
            DamageSource beamSource = level.damageSources().onFire();

            for (LivingEntity target : targets) {
                // Shield REFLECTS the beam
                if (target.isBlocking()) {
                    beamReflected = true;
                    // Reflected beam does reduced damage to nearby enemies
                    List<LivingEntity> reflectTargets = level.getEntitiesOfClass(
                        LivingEntity.class,
                        new AABB(target.blockPosition()).inflate(3.0));
                    for (LivingEntity reflectTarget : reflectTargets) {
                        if (reflectTarget != target) {
                            reflectTarget.hurt(beamSource,
                                config.damage().amount() * REFLECT_DAMAGE_MULTIPLIER);
                        }
                    }
                    continue;
                }

                // Direct beam hit - massive damage + fire
                target.hurt(beamSource, config.damage().amount());
                target.setRemainingFireTicks(100);
            }
        }
    }

    @Override
    protected void reset(ServerLevel level) {
        beamAngle = 0;
        obscuredByVapor = false;
        obscuredByFog = false;
        beamReflected = false;
        vaporTimer = 0;
        fogTimer = 0;
    }

    /**
     * Called when water creates dispersing vapor.
     * Beam is safe for 5 seconds.
     */
    public void createWaterVapor() {
        this.obscuredByVapor = true;
        this.vaporTimer = WATER_VAPOR_SAFE_TICKS;
    }

    /**
     * Called when powder snow creates blocking fog.
     */
    public void createPowderSnowFog() {
        this.obscuredByFog = true;
        this.fogTimer = POWDER_SNOW_FOG_TICKS;
    }

    /**
     * Called when fishing rod rotates a mirror, changing beam direction.
     */
    public void rotateMirror(float angleOffset) {
        this.beamAngle += angleOffset;
    }

    public float getBeamAngle() {
        return beamAngle;
    }

    public boolean isBeamReflected() {
        return beamReflected;
    }
}

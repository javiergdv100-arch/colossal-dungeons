package com.colossaldungeons.enhanced.dungeon.trap;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * False Voice Trap - Reproduces help voices from wrong direction to lure into traps.
 * 
 * Mechanics:
 * - Mimics voices (help, warnings, laughter) from misleading directions
 * - Lures players toward other trap zones
 * - Sculk sensors can find the real source direction
 * - Note blocks saturate and silence the false voices
 * - Milk cleans disorientation debuff
 * - Applies confusion + wrong direction indicators
 * 
 * Config values used:
 * - damage.amount: disorientation damage buildup
 * - damage.radius: voice hearing range
 * - timing.warningTicks: initial delay before voices start
 * - timing.activeTicks: voice duration per cycle
 */
public class FalseVoiceTrap extends AbstractTrap {

    private static final int VOICE_INTERVAL_TICKS = 80;
    private static final int DISORIENTATION_DURATION = 200;
    private static final int NOTE_BLOCK_SILENCE_RADIUS = 5;

    private boolean silencedByNoteBlock;
    private BlockPos lureTargetPos;
    private Vec3 falseDirection;

    public FalseVoiceTrap(BlockPos position, TrapConfig config) {
        super(position, config);
        this.silencedByNoteBlock = false;
        this.lureTargetPos = position.offset(10, 0, 0); // Default lure position
        this.falseDirection = new Vec3(1, 0, 0);
    }

    @Override
    protected boolean shouldPrepare(ServerLevel level) {
        if (silencedByNoteBlock) return false;
        List<Player> players = level.getEntitiesOfClass(Player.class,
            new AABB(position).inflate(config.activator().range()));
        return !players.isEmpty();
    }

    @Override
    protected boolean shouldArm(ServerLevel level) {
        return tickCounter >= config.timing().warningTicks();
    }

    @Override
    protected void prepare(ServerLevel level) {
        // Faint whispers begin - barely audible
        // Source direction is misleading

        // Calculate false direction (away from real source)
        float angle = level.random.nextFloat() * (float) Math.PI * 2;
        falseDirection = new Vec3(Math.cos(angle), 0, Math.sin(angle));
    }

    @Override
    protected void arm(ServerLevel level) {
        // Voices become clearer - seem to be calling for help
    }

    @Override
    protected void trigger(ServerLevel level) {
        // Full voice reproduction - sounds like real person in distress
    }

    @Override
    protected void damage(ServerLevel level) {
        if (silencedByNoteBlock) {
            state = TrapState.COOLDOWN;
            tickCounter = 0;
            return;
        }

        // Play false voice at intervals
        if (tickCounter % VOICE_INTERVAL_TICKS != 0) return;

        List<Player> targets = level.getEntitiesOfClass(Player.class,
            new AABB(position).inflate(config.damage().radius()));

        for (Player target : targets) {
            // Apply disorientation effect
            target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, DISORIENTATION_DURATION, 0));

            // Minor psychic damage from disorientation
            DamageSource psychicSource = level.damageSources().magic();
            target.hurt(psychicSource, config.damage().amount());

            // Attempt to lure player toward trap zone
            Vec3 lureDir = falseDirection.scale(0.05);
            target.push(lureDir.x, 0, lureDir.z);
        }
    }

    @Override
    protected void reset(ServerLevel level) {
        silencedByNoteBlock = false;
    }

    /**
     * Called when note blocks saturate the area.
     * Silences the false voices.
     */
    public void silenceWithNoteBlock() {
        this.silencedByNoteBlock = true;
    }

    /**
     * Sets the target position that players are lured toward.
     */
    public void setLureTarget(BlockPos target) {
        this.lureTargetPos = target;
    }

    /**
     * Gets the real source position (for sculk sensor detection).
     */
    public BlockPos getRealSourcePosition() {
        return position;
    }

    public boolean isSilenced() {
        return silencedByNoteBlock;
    }
}

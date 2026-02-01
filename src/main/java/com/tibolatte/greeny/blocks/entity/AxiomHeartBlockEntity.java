package com.tibolatte.greeny.blocks.entity;

import com.tibolatte.greeny.mobs.*;

import com.tibolatte.greeny.registry.BlockEntityRegistry;
import com.tibolatte.greeny.registry.EntityRegistry;
import com.tibolatte.greeny.registry.MobEffectRegistry;
import com.tibolatte.greeny.registry.ParticleRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import team.lodestar.lodestone.handlers.ScreenshakeHandler;
import team.lodestar.lodestone.systems.easing.Easing;
import team.lodestar.lodestone.systems.particle.builder.WorldParticleBuilder;
import team.lodestar.lodestone.systems.particle.data.GenericParticleData;
import team.lodestar.lodestone.systems.particle.data.color.ColorParticleData;
import team.lodestar.lodestone.systems.screenshake.ScreenshakeInstance;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tibolatte.greeny.registry.EntityRegistry.AXIOM_GUARDIAN;

public class AxiomHeartBlockEntity extends BlockEntity {

    // =============================================================
    //               GAMEPLAY CONFIGURATION (TWEAK HERE)
    // =============================================================

    // 1. TIMINGS (In seconds)
    private static final int PULSE_LOOP_SPEED = 5 * 20;      // How often the Green Pulse happens (5s)
    private static final int ANGER_BUILDUP_TIME = 4 * 20;     // Time between Kill -> Explosion (4s)
    private static final int ANGER_REFRACTORY_TIME = 20 * 20; // How long it stays Angry/Silent AFTER the explosion (20s)

    // 2. RESONANCE (The "Attunement" Bar 0.0 to 1.0)
    private static final float SCORE_GAIN_PER_TICK = 0.015f; // Speed to get attuned (0.005 = ~10 seconds of standing still)
    private static final float SCORE_DECAY_PER_TICK = 0.05f; // Speed you lose it if moving
    private static final float SCORE_PUNISHMENT = 0.1f;      // Instant loss if you hold a weapon

    // 3. RANGES & PHYSICS
    private static final double DETECTION_RADIUS = 20.0;     // How far it looks for players
    private static final double KNOCKBACK_HORIZONTAL = 1.8;  // How hard you get yeeted sideways
    private static final double KNOCKBACK_VERTICAL = 1.3;    // How hard you get yeeted up

    private boolean hasExplodedThisCycle = false;


    // =============================================================

    private int pulseTicks = 0;
    private int angerTicks = 0;

    private final Map<UUID, Float> serverResonance = new HashMap<>();
    private final Map<UUID, Float> clientResonance = new HashMap<>();

    public AxiomHeartBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.AXIOM_HEART.get(), pos, state);
    }

    public void triggerAnger() {
        // Only trigger if we aren't already mid-explosion
        boolean isBusyExploding = (this.pulseTicks >= PULSE_LOOP_SPEED) &&
                (this.pulseTicks < PULSE_LOOP_SPEED + ANGER_BUILDUP_TIME);

        // Reset the "Have I exploded?" flag because we are starting fresh
        if (!isBusyExploding) {
            this.pulseTicks = PULSE_LOOP_SPEED;
            this.hasExplodedThisCycle = false;

            this.angerTicks = ANGER_BUILDUP_TIME + ANGER_REFRACTORY_TIME;

            serverResonance.clear();
            setChanged();
            if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        } else {
            // Just extend the anger, don't restart the boom
            this.angerTicks = Math.max(this.angerTicks, ANGER_REFRACTORY_TIME);
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, AxiomHeartBlockEntity blockEntity) {

        // --- TICK LOGIC FIX ---
        // If we have already exploded, we LOCK pulseTicks to 0.
        // This prevents the loop from restarting while we are in the "Silence" phase.
        if (blockEntity.hasExplodedThisCycle && blockEntity.angerTicks > 0) {
            blockEntity.pulseTicks = 0;
        } else {
            blockEntity.pulseTicks++;
        }

        if (blockEntity.angerTicks > 0) blockEntity.angerTicks--;
        boolean isAngry = blockEntity.angerTicks > 0;

        // If anger runs out, reset the exploded flag so it can happen again next time
        if (!isAngry) {
            blockEntity.hasExplodedThisCycle = false;
        }

        if (isAngry) {
            // 1. SHAKE
            if (level.isClientSide && blockEntity.pulseTicks == PULSE_LOOP_SPEED + 1) {
                ScreenshakeHandler.addScreenshake(new ScreenshakeInstance(ANGER_BUILDUP_TIME)
                        .setIntensity(0.2f, 1.5f).setEasing(Easing.QUAD_IN));
            }

            // 2. PARTICLES
            if (level.isClientSide && blockEntity.pulseTicks >= PULSE_LOOP_SPEED && blockEntity.pulseTicks < PULSE_LOOP_SPEED + ANGER_BUILDUP_TIME) {
                blockEntity.spawnAngerBuildupParticles(pos, blockEntity.pulseTicks);
            }

            // 3. EXPLOSION (HAPPENS ONCE)
            if (blockEntity.pulseTicks >= PULSE_LOOP_SPEED + ANGER_BUILDUP_TIME) {
                blockEntity.pulseTicks = 0;
                blockEntity.hasExplodedThisCycle = true; // LOCK THE LOOP

                if (level.isClientSide) {
                    blockEntity.spawnAngerShockwave(pos);
                } else {
                    blockEntity.applyShockwaveKnockback(level, pos);
                    blockEntity.spawnGuardianDefender(level, pos);
                }
            }
        }
        else {
            // GREEN PULSE
            if (blockEntity.pulseTicks >= PULSE_LOOP_SPEED) {
                blockEntity.pulseTicks = 0;
                if (level.isClientSide) blockEntity.spawnGreenShockwave(pos);
                else blockEntity.triggerPulseRewards(level, pos);
            }
        }

        // Visuals
        if (level.isClientSide) {
            blockEntity.animateHeart(level, pos, isAngry);
            if (!isAngry) blockEntity.animatePlayerTethers(level, pos);
        } else {
            if (!isAngry) blockEntity.updateServerLogic(level, pos);
        }
    }

    // =============================================================
    //                  PHYSICS
    // =============================================================

    private void applyShockwaveKnockback(Level level, BlockPos pos) {
        double pushRadius = 8.0;
        AABB area = new AABB(pos).inflate(pushRadius);
        List<Player> players = level.getEntitiesOfClass(Player.class, area);

        for (Player player : players) {
            Vec3 center = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            Vec3 direction = player.position().subtract(center).normalize();

            if (direction.lengthSqr() < 0.0001) direction = new Vec3(0, 1, 0);

            //
            player.push(
                    direction.x * KNOCKBACK_HORIZONTAL,
                    KNOCKBACK_VERTICAL,
                    direction.z * KNOCKBACK_HORIZONTAL
            );
            player.hurtMarked = true;
        }
    }

    // =============================================================
    //                  SYNCING
    // =============================================================
    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.putInt("Anger", angerTicks);
        tag.putInt("Pulse", pulseTicks);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        super.handleUpdateTag(tag);
        this.angerTicks = tag.getInt("Anger");
        this.pulseTicks = tag.getInt("Pulse");
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net, ClientboundBlockEntityDataPacket pkt) {
        handleUpdateTag(pkt.getTag());
    }

    // =============================================================
    //                  CLIENT ART
    // =============================================================

    private void spawnAngerBuildupParticles(BlockPos pos, int currentTick) {
        int targetTick = PULSE_LOOP_SPEED + ANGER_BUILDUP_TIME;
        int ticksUntilBoom = targetTick - currentTick;

        if (ticksUntilBoom <= 0) return;

        double cx = pos.getX() + 0.5;
        double cy = pos.getY() + 0.5;
        double cz = pos.getZ() + 0.5;

        double radius = 3.0 + (ticksUntilBoom * 0.05);

        double theta = level.random.nextDouble() * Math.PI * 2;
        double phi = level.random.nextDouble() * Math.PI;
        double ox = Math.sin(phi) * Math.cos(theta) * radius;
        double oy = Math.cos(phi) * radius;
        double oz = Math.sin(phi) * Math.sin(theta) * radius;

        double speedX = -ox / ticksUntilBoom;
        double speedY = -oy / ticksUntilBoom;
        double speedZ = -oz / ticksUntilBoom;

        WorldParticleBuilder.create(ParticleRegistry.STAR_PARTICLE.get())
                .setColorData(ColorParticleData.create(new Color(100, 0, 0), new Color(0, 0, 0)).build())
                .setScaleData(GenericParticleData.create(0.2f, 0.0f).build())
                .setLifetime(ticksUntilBoom)
                .setMotion(speedX, speedY, speedZ)
                .enableNoClip()
                .spawn(level, cx + ox, cy + oy, cz + oz);

        if (ticksUntilBoom < 20 && level.random.nextBoolean()) {
            WorldParticleBuilder.create(ParticleRegistry.MANA_WISP.get())
                    .setColorData(ColorParticleData.create(new Color(255, 0, 0), new Color(50, 0, 0)).build())
                    .setScaleData(GenericParticleData.create(0.5f, 0.8f).build())
                    .setLifetime(2)
                    .spawn(level, cx, cy, cz);
        }
    }

    private void spawnAngerShockwave(BlockPos pos) {
        double cx = pos.getX() + 0.5;
        double cy = pos.getY() + 0.5;
        double cz = pos.getZ() + 0.5;

        int particleCount = 100;
        for (int i = 0; i < particleCount; i++) {
            double angle = (Math.PI * 2 * i) / particleCount;
            double speed = 0.8;

            double vx = Math.cos(angle) * speed;
            double vz = Math.sin(angle) * speed;

            WorldParticleBuilder.create(ParticleRegistry.STAR_PARTICLE.get())
                    .setColorData(ColorParticleData.create(new Color(255, 0, 0), new Color(50, 0, 0)).build())
                    .setScaleData(GenericParticleData.create(0.6f, 0.0f).build())
                    .setLifetime(50)
                    .setMotion(vx, 0, vz)
                    .enableNoClip()
                    .spawn(level, cx, cy, cz);
        }
    }

    private void spawnGreenShockwave(BlockPos pos) {
        double cx = pos.getX() + 0.5;
        double cy = pos.getY() + 0.5;
        double cz = pos.getZ() + 0.5;

        int particleCount = 40;
        for (int i = 0; i < particleCount; i++) {
            double angle = (Math.PI * 2 * i) / particleCount;
            double speed = 0.2;

            double vx = Math.cos(angle) * speed;
            double vz = Math.sin(angle) * speed;

            WorldParticleBuilder.create(ParticleRegistry.MANA_WISP.get())
                    .setColorData(ColorParticleData.create(new Color(100, 255, 100), new Color(0, 100, 50)).build())
                    .setScaleData(GenericParticleData.create(0.2f, 0.0f).build())
                    .setLifetime(35)
                    .setMotion(vx, 0, vz)
                    .enableNoClip()
                    .spawn(level, cx, cy, cz);
        }
    }

    private void animatePlayerTethers(Level level, BlockPos pos) {
        AABB area = new AABB(pos).inflate(DETECTION_RADIUS);
        List<Player> players = level.getEntitiesOfClass(Player.class, area);

        for (Player player : players) {
            UUID id = player.getUUID();
            float visualScore = clientResonance.getOrDefault(id, 0.0f);

            boolean isStill = isStationary(player);
            boolean isArmed = isHoldingTool(player);

            if (isArmed) {
                visualScore = 0.0f;
                spawnRejectionParticles(level, player);
            }
            else if (isStill) {
                visualScore += SCORE_GAIN_PER_TICK; // Variable
            } else {
                visualScore -= SCORE_DECAY_PER_TICK; // Variable
            }

            visualScore = Math.max(0.0f, Math.min(1.0f, visualScore));
            clientResonance.put(id, visualScore);

            if (visualScore > 0.1f) {
                spawnTetherParticles(level, pos, player, visualScore);
            }
        }
    }

    private void spawnRejectionParticles(Level level, Player player) {
        double x = player.getX() + (level.random.nextDouble() - 0.5) * 0.5;
        double y = player.getY() + 1.0;
        double z = player.getZ() + (level.random.nextDouble() - 0.5) * 0.5;

        WorldParticleBuilder.create(ParticleRegistry.STAR_PARTICLE.get())
                .setColorData(ColorParticleData.create(new Color(200, 0, 0), new Color(0, 0, 0)).build())
                .setScaleData(GenericParticleData.create(0.15f, 0.0f).build())
                .setLifetime(10)
                .setSpinData(team.lodestar.lodestone.systems.particle.data.spin.SpinParticleData.create(0.5f).build())
                .setMotion((level.random.nextDouble() - 0.5) * 0.1, 0.05, (level.random.nextDouble() - 0.5) * 0.1)
                .spawn(level, x, y, z);
    }

    private void spawnTetherParticles(Level level, BlockPos pos, Player player, float score) {
        float spawnChance = 0.1f + (score * 0.4f);
        if (level.random.nextFloat() > spawnChance) return;

        double startX = player.getX();
        double startY = player.getY() + 0.7;
        double startZ = player.getZ();

        double targetX = pos.getX() + 0.5;
        double targetY = pos.getY() + 0.5;
        double targetZ = pos.getZ() + 0.5;

        Vec3 vectorToTarget = new Vec3(targetX - startX, targetY - startY, targetZ - startZ);
        double distance = vectorToTarget.length();
        Vec3 direction = vectorToTarget.normalize();

        double angle = level.getGameTime() * 0.1;
        double radius = 0.5;
        double offsetX = Math.cos(angle) * radius + (level.random.nextDouble() - 0.5) * 0.2;
        double offsetZ = Math.sin(angle) * radius + (level.random.nextDouble() - 0.5) * 0.2;
        double offsetY = (level.random.nextDouble() - 0.5) * 0.5;

        Color c1 = score > 0.8f ? new Color(50, 255, 100) : new Color(200, 255, 150);
        Color c2 = new Color(0, 50, 20, 0);

        WorldParticleBuilder.create(ParticleRegistry.MANA_WISP.get())
                .setColorData(ColorParticleData.create(c1, c2).build())
                .setTransparencyData(GenericParticleData.create(0.6f, 0.0f).build())
                .setScaleData(GenericParticleData.create(0.08f, 0.0f).build())
                .setLifetime((int) (distance * 10) + 10)
                .setMotion(direction.x * 0.1, direction.y * 0.1, direction.z * 0.1)
                .enableNoClip()
                .spawn(level, startX + offsetX, startY + offsetY, startZ + offsetZ);
    }

    private void animateHeart(Level level, BlockPos pos, boolean isAngry) {
        if (level.random.nextFloat() < 0.2f) {
            double x = pos.getX() + 0.5 + (level.random.nextDouble() - 0.5) * 1.5;
            double y = pos.getY() + 0.5 + (level.random.nextDouble() - 0.5) * 0.5;
            double z = pos.getZ() + 0.5 + (level.random.nextDouble() - 0.5) * 1.5;

            Color c1 = isAngry ? new Color(100, 0, 0) : new Color(20, 100, 50);
            Color c2 = new Color(0, 0, 0, 0);

            WorldParticleBuilder.create(ParticleRegistry.SMOKE_PARTICLE.get())
                    .setColorData(ColorParticleData.create(c1, c2).build())
                    .setTransparencyData(GenericParticleData.create(0.4f, 0.0f).build())
                    .setScaleData(GenericParticleData.create(0.3f, 0.5f).build())
                    .setLifetime(80)
                    .setMotion(0, 0.005, 0)
                    .enableNoClip()
                    .spawn(level, x, y, z);
        }
    }

    private void updateServerLogic(Level level, BlockPos pos) {
        AABB area = new AABB(pos).inflate(DETECTION_RADIUS);
        List<Player> players = level.getEntitiesOfClass(Player.class, area);

        for (Player player : players) {
            UUID id = player.getUUID();
            float score = serverResonance.getOrDefault(id, 0.0f);

            boolean isSprinting = player.isSprinting();
            boolean isStill = isStationary(player);
            boolean isArmed = isHoldingTool(player);

            if (isSprinting || isArmed) {
                score -= SCORE_PUNISHMENT; // Variable
                if (player.hasEffect(MobEffectRegistry.FOREST_MARK.get())) {
                    player.removeEffect(MobEffectRegistry.FOREST_MARK.get());
                }
            } else if (isStill) {
                score += SCORE_GAIN_PER_TICK; // Variable
            } else {
                score -= SCORE_DECAY_PER_TICK; // Variable
            }

            score = Math.max(0.0f, Math.min(1.0f, score));
            serverResonance.put(id, score);
        }
    }

    private void triggerPulseRewards(Level level, BlockPos pos) {
        AABB area = new AABB(pos).inflate(DETECTION_RADIUS);
        List<Player> players = level.getEntitiesOfClass(Player.class, area);

        for (Player player : players) {
            float score = serverResonance.getOrDefault(player.getUUID(), 0.0f);
            if (score > 0.8f) {
                player.addEffect(new MobEffectInstance(
                        MobEffectRegistry.FOREST_MARK.get(),
                        420, 0, false, false, false
                ));
            }
        }
    }

    private void spawnGuardianDefender(Level level, BlockPos pos) {
        // 1. Find the Nearest Player (The Target)
        // We look within 20 blocks. If no one is close, we default to the Heart's position.
        Player targetPlayer = level.getNearestPlayer(pos.getX(), pos.getY(), pos.getZ(), 20.0, true);
        BlockPos searchCenter = (targetPlayer != null) ? targetPlayer.blockPosition() : pos;

        // 2. Find a valid spot NEAR THE TARGET
        // We look for a spot around the player (or heart if no player)
        BlockPos spawnPos = findValidSpawnSpot(level, searchCenter);

        // Fallback: If no valid ground found near player, try near the Heart itself
        if (spawnPos == null) spawnPos = findValidSpawnSpot(level, pos);
        // Final Fallback: Just spawn on top of the Heart
        if (spawnPos == null) spawnPos = pos.above();

        // 3. Create Entity
        AxiomGuardianEntity guardian = EntityRegistry.AXIOM_GUARDIAN.get().create(level);
        if (guardian != null) {
            guardian.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, 0.0f, 0.0f);

            guardian.setSummoned(true);

            // Aggro on the found player immediately
            if (targetPlayer != null) {
                guardian.setTarget(targetPlayer);
            }

            level.addFreshEntity(guardian);
        }
    }

    private BlockPos findValidSpawnSpot(Level level, BlockPos center) {
        // SCAN STRATEGY:
        // Start from 1 block above the heart (in case it's buried slightly)
        // and scan downwards up to 7 blocks to find the "floor".
        for (int y = 2; y >= -7; y--) {

            // Search expanding radius at this height
            for (int r = 1; r <= 5; r++) {
                for (int x = -r; x <= r; x++) {
                    for (int z = -r; z <= r; z++) {

                        // Optimization: Skip the inner rings we already checked
                        if (Math.abs(x) < r && Math.abs(z) < r) continue;

                        BlockPos target = center.offset(x, y, z);

                        // THE VALIDITY CHECK:
                        // 1. Feet space is Air
                        // 2. Head space is Air (Guardian is ~1.5 blocks tall)
                        // 3. Ground below is Solid (So it doesn't fall)
                        if (level.getBlockState(target).isAir() &&
                                level.getBlockState(target.above()).isAir() &&
                                level.getBlockState(target.below()).isSolidRender(level, target.below())) {

                            return target;
                        }
                    }
                }
            }
        }
        return null; // No valid floor found within 7 blocks down
    }

    private boolean isStationary(Player player) {
        double dist = player.position().distanceToSqr(player.xo, player.yo, player.zo);
        return dist < 0.01;
    }
    private boolean isHoldingTool(Player player) {
        Item item = player.getMainHandItem().getItem();
        return item instanceof DiggerItem || item instanceof SwordItem || item instanceof AxeItem;
    }
}
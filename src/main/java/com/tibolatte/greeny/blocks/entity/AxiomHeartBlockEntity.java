package com.tibolatte.greeny.blocks.entity;

import com.tibolatte.greeny.blocks.AxiomHeartBlock;
import com.tibolatte.greeny.blocks.HeartState;
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

public class AxiomHeartBlockEntity extends BlockEntity {

    // =============================================================
    //               GAMEPLAY CONFIGURATION
    // =============================================================

    // TIMINGS
    private static final int PULSE_LOOP_SPEED = 5 * 20;       // Green Pulse frequency (5s)
    private static final int ANGER_BUILDUP_TIME = 4 * 20;      // Warning time before boom (4s)
    private static final int ANGER_REFRACTORY_TIME = 20 * 20;  // Cooldown after boom (20s)
    private static final int SHUTDOWN_COOLDOWN_TIME = 600 * 20; // Sleep time if dead (10 mins)

    // MECHANICS
    private static final int MAX_ANGER_STRIKES = 3;           // How many times before it dies?
    private static final float SCORE_GAIN_PER_TICK = 0.015f;  // Attunement gain speed
    private static final float SCORE_DECAY_PER_TICK = 0.05f;  // Attunement loss speed
    private static final float SCORE_PUNISHMENT = 0.1f;       // Instant loss for bad behavior

    // PHYSICS
    private static final double DETECTION_RADIUS = 20.0;
    private static final double KNOCKBACK_HORIZONTAL = 1.8;
    private static final double KNOCKBACK_VERTICAL = 1.3;

    // =============================================================
    //               STATE VARIABLES
    // =============================================================

    // Standard Logic
    private int pulseTicks = 0;
    private int angerTicks = 0;
    private boolean hasExplodedThisCycle = false;

    // Shutdown Logic
    private int angerStrikes = 0;  // Counts strikes (0 to 3)
    private int shutdownTimer = 0; // Counts down if dead

    // Resonance (Attunement)
    private final Map<UUID, Float> serverResonance = new HashMap<>();
    private final Map<UUID, Float> clientResonance = new HashMap<>();

    public AxiomHeartBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.AXIOM_HEART.get(), pos, state);
    }

    // =============================================================
    //                  CORE LOGIC
    // =============================================================

    public static void tick(Level level, BlockPos pos, BlockState state, AxiomHeartBlockEntity blockEntity) {

        // 1. SHUTDOWN STATE (Priority)
        if (blockEntity.shutdownTimer > 0) {
            blockEntity.shutdownTimer--;

            // Wake up if timer hits 0
            if (blockEntity.shutdownTimer == 0) {
                blockEntity.resetToActive();
            }
            return; // Stop all other logic while dormant
        }

        // 2. TICKERS
        if (blockEntity.hasExplodedThisCycle && blockEntity.angerTicks > 0) {
            // Wait for silence
            blockEntity.pulseTicks = 0;
        } else {
            // Pulse normally
            blockEntity.pulseTicks++;
        }

        if (blockEntity.angerTicks > 0) blockEntity.angerTicks--;
        boolean isAngry = blockEntity.angerTicks > 0;

        // 3. STATE MANAGEMENT (Visuals)
        // If anger just ended, reset to normal
        if (!isAngry && blockEntity.hasExplodedThisCycle) {
            blockEntity.hasExplodedThisCycle = false;
            if (state.getValue(AxiomHeartBlock.STATE) == HeartState.ANGRY) {
                blockEntity.updateBlockState(HeartState.ACTIVE);
            }
        }

        // 4. ANGER SEQUENCE
        if (isAngry) {
            // A. Shake Screen (Warning)
            if (level.isClientSide && blockEntity.pulseTicks == PULSE_LOOP_SPEED + 1) {
                ScreenshakeHandler.addScreenshake(new ScreenshakeInstance(ANGER_BUILDUP_TIME)
                        .setIntensity(0.2f, 1.5f).setEasing(Easing.QUAD_IN));
            }

            // B. Particles (Buildup)
            if (level.isClientSide && blockEntity.pulseTicks >= PULSE_LOOP_SPEED && blockEntity.pulseTicks < PULSE_LOOP_SPEED + ANGER_BUILDUP_TIME) {
                blockEntity.spawnAngerBuildupParticles(pos, blockEntity.pulseTicks);
            }

            // C. EXPLOSION
            if (blockEntity.pulseTicks >= PULSE_LOOP_SPEED + ANGER_BUILDUP_TIME) {
                blockEntity.pulseTicks = 0;
                blockEntity.hasExplodedThisCycle = true; // Lock loop

                if (level.isClientSide) {
                    blockEntity.spawnAngerShockwave(pos);
                } else {
                    blockEntity.applyShockwaveKnockback(level, pos);
                    blockEntity.spawnGuardianDefender(level, pos);
                }
            }
        }
        // 5. PASSIVE PULSE
        else {
            if (blockEntity.pulseTicks >= PULSE_LOOP_SPEED) {
                blockEntity.pulseTicks = 0;
                if (level.isClientSide) blockEntity.spawnGreenShockwave(pos);
                else blockEntity.triggerPulseRewards(level, pos);
            }
        }

        // 6. VISUAL ANIMATIONS
        if (level.isClientSide) {
            blockEntity.animateHeart(level, pos, isAngry);
            if (!isAngry) blockEntity.animatePlayerTethers(level, pos);
        } else {
            if (!isAngry) blockEntity.updateServerLogic(level, pos);
        }
    }

    // =============================================================
    //                  TRIGGER METHODS
    // =============================================================

    public void triggerAnger() {
        if (shutdownTimer > 0) return; // Can't anger a dead heart

        // If we are currently "Passive" or "Cooling Down", add a strike
        // We do NOT add a strike if we are already in the middle of exploding (spam prevention)
        if (angerTicks <= 0) {
            this.angerStrikes++;

            // CHECK SHUTDOWN CONDITION
            if (this.angerStrikes >= MAX_ANGER_STRIKES) {
                initiateShutdown();
                return;
            }
        }

        // Start the Anger Cycle
        boolean isBusyExploding = (this.pulseTicks >= PULSE_LOOP_SPEED) &&
                (this.pulseTicks < PULSE_LOOP_SPEED + ANGER_BUILDUP_TIME);

        if (!isBusyExploding) {
            this.pulseTicks = PULSE_LOOP_SPEED; // Skip to start of buildup
            this.hasExplodedThisCycle = false;
            this.angerTicks = ANGER_BUILDUP_TIME + ANGER_REFRACTORY_TIME;

            // Wipe Attunement progress as punishment
            serverResonance.clear();
            setChanged();

            updateBlockState(HeartState.ANGRY); // Turn RED

            // Sync
            if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        } else {
            // Just extend the duration if already angry
            this.angerTicks = Math.max(this.angerTicks, ANGER_REFRACTORY_TIME);
        }
    }

    private void initiateShutdown() {
        this.shutdownTimer = SHUTDOWN_COOLDOWN_TIME;
        this.angerTicks = 0;
        this.pulseTicks = 0;
        this.angerStrikes = 0; // Reset strikes for when it wakes up

        serverResonance.clear();
        clientResonance.clear();

        updateBlockState(HeartState.DORMANT); // Turn GRAY

        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    private void resetToActive() {
        this.shutdownTimer = 0;
        updateBlockState(HeartState.ACTIVE); // Turn BLUE
    }

    private void updateBlockState(HeartState newState) {
        if (level != null && !level.isClientSide) {
            BlockState currentState = getBlockState();
            if (currentState.getValue(AxiomHeartBlock.STATE) != newState) {
                level.setBlock(worldPosition, currentState.setValue(AxiomHeartBlock.STATE, newState), 3);
            }
        }
    }

    // =============================================================
    //                  PHYSICS & LOGIC
    // =============================================================

    private void applyShockwaveKnockback(Level level, BlockPos pos) {
        double pushRadius = 8.0;
        AABB area = new AABB(pos).inflate(pushRadius);
        List<Player> players = level.getEntitiesOfClass(Player.class, area);

        for (Player player : players) {
            Vec3 center = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            Vec3 direction = player.position().subtract(center).normalize();

            if (direction.lengthSqr() < 0.0001) direction = new Vec3(0, 1, 0);

            player.push(direction.x * KNOCKBACK_HORIZONTAL, KNOCKBACK_VERTICAL, direction.z * KNOCKBACK_HORIZONTAL);
            player.hurtMarked = true;
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
                score -= SCORE_PUNISHMENT;
                if (player.hasEffect(MobEffectRegistry.FOREST_MARK.get())) {
                    player.removeEffect(MobEffectRegistry.FOREST_MARK.get());
                }
            } else if (isStill) {
                score += SCORE_GAIN_PER_TICK;
            } else {
                score -= SCORE_DECAY_PER_TICK;
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
                player.addEffect(new MobEffectInstance(MobEffectRegistry.FOREST_MARK.get(), 420, 0, false, false, false));
            }
        }
    }

    private void spawnGuardianDefender(Level level, BlockPos pos) {
        Player targetPlayer = level.getNearestPlayer(pos.getX(), pos.getY(), pos.getZ(), 20.0, true);
        BlockPos searchCenter = (targetPlayer != null) ? targetPlayer.blockPosition() : pos;
        BlockPos spawnPos = findValidSpawnSpot(level, searchCenter);
        if (spawnPos == null) spawnPos = findValidSpawnSpot(level, pos);
        if (spawnPos == null) spawnPos = pos.above();

        // Use EntityRegistry.AXIOM_GUARDIAN
        var guardian = EntityRegistry.AXIOM_GUARDIAN.get().create(level);
        if (guardian != null) {
            guardian.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, 0.0f, 0.0f);
            guardian.setSummoned(true);
            if (targetPlayer != null) guardian.setTarget(targetPlayer);
            level.addFreshEntity(guardian);
        }
    }

    private BlockPos findValidSpawnSpot(Level level, BlockPos center) {
        for (int y = 2; y >= -7; y--) {
            for (int r = 1; r <= 5; r++) {
                for (int x = -r; x <= r; x++) {
                    for (int z = -r; z <= r; z++) {
                        if (Math.abs(x) < r && Math.abs(z) < r) continue;
                        BlockPos target = center.offset(x, y, z);
                        if (level.getBlockState(target).isAir() &&
                                level.getBlockState(target.above()).isAir() &&
                                level.getBlockState(target.below()).isSolidRender(level, target.below())) {
                            return target;
                        }
                    }
                }
            }
        }
        return null;
    }

    // =============================================================
    //                  SYNCING & HELPERS
    // =============================================================

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.putInt("Anger", angerTicks);
        tag.putInt("Pulse", pulseTicks);
        tag.putInt("Strikes", angerStrikes);
        tag.putInt("Shutdown", shutdownTimer);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        super.handleUpdateTag(tag);
        this.angerTicks = tag.getInt("Anger");
        this.pulseTicks = tag.getInt("Pulse");
        this.angerStrikes = tag.getInt("Strikes");
        this.shutdownTimer = tag.getInt("Shutdown");
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net, ClientboundBlockEntityDataPacket pkt) {
        handleUpdateTag(pkt.getTag());
    }

    private boolean isStationary(Player player) {
        return player.position().distanceToSqr(player.xo, player.yo, player.zo) < 0.01;
    }

    private boolean isHoldingTool(Player player) {
        Item item = player.getMainHandItem().getItem();
        return item instanceof DiggerItem || item instanceof SwordItem || item instanceof AxeItem;
    }

    // =============================================================
    //                  CLIENT VISUALS
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

        WorldParticleBuilder.create(ParticleRegistry.STAR_PARTICLE.get())
                .setColorData(ColorParticleData.create(new Color(100, 0, 0), new Color(0, 0, 0)).build())
                .setScaleData(GenericParticleData.create(0.2f, 0.0f).build())
                .setLifetime(ticksUntilBoom)
                .setMotion(-ox / ticksUntilBoom, -oy / ticksUntilBoom, -oz / ticksUntilBoom)
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
        int count = 100;
        for (int i = 0; i < count; i++) {
            double angle = (Math.PI * 2 * i) / count;
            WorldParticleBuilder.create(ParticleRegistry.STAR_PARTICLE.get())
                    .setColorData(ColorParticleData.create(new Color(255, 0, 0), new Color(50, 0, 0)).build())
                    .setScaleData(GenericParticleData.create(0.6f, 0.0f).build())
                    .setLifetime(50)
                    .setMotion(Math.cos(angle) * 0.8, 0, Math.sin(angle) * 0.8)
                    .enableNoClip()
                    .spawn(level, cx, cy, cz);
        }
    }

    private void spawnGreenShockwave(BlockPos pos) {
        double cx = pos.getX() + 0.5;
        double cy = pos.getY() + 0.5;
        double cz = pos.getZ() + 0.5;
        int count = 40;
        for (int i = 0; i < count; i++) {
            double angle = (Math.PI * 2 * i) / count;
            WorldParticleBuilder.create(ParticleRegistry.MANA_WISP.get())
                    .setColorData(ColorParticleData.create(new Color(100, 255, 100), new Color(0, 100, 50)).build())
                    .setScaleData(GenericParticleData.create(0.2f, 0.0f).build())
                    .setLifetime(35)
                    .setMotion(Math.cos(angle) * 0.2, 0, Math.sin(angle) * 0.2)
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

            if (isHoldingTool(player)) {
                visualScore = 0.0f;
                spawnRejectionParticles(level, player);
            } else if (isStationary(player)) {
                visualScore += SCORE_GAIN_PER_TICK;
            } else {
                visualScore -= SCORE_DECAY_PER_TICK;
            }

            visualScore = Math.max(0.0f, Math.min(1.0f, visualScore));
            clientResonance.put(id, visualScore);

            if (visualScore > 0.1f) {
                spawnTetherParticles(level, pos, player, visualScore);
            }
        }
    }

    private void spawnRejectionParticles(Level level, Player player) {
        WorldParticleBuilder.create(ParticleRegistry.STAR_PARTICLE.get())
                .setColorData(ColorParticleData.create(new Color(200, 0, 0), new Color(0, 0, 0)).build())
                .setScaleData(GenericParticleData.create(0.15f, 0.0f).build())
                .setLifetime(10)
                .setMotion((level.random.nextDouble() - 0.5) * 0.1, 0.05, (level.random.nextDouble() - 0.5) * 0.1)
                .spawn(level, player.getX(), player.getY() + 1.0, player.getZ());
    }

    private void spawnTetherParticles(Level level, BlockPos pos, Player player, float score) {
        float spawnChance = 0.1f + (score * 0.4f);
        if (level.random.nextFloat() > spawnChance) return;

        double startX = player.getX();
        double startY = player.getY() + 0.7;
        double startZ = player.getZ();
        Vec3 target = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        Vec3 direction = target.subtract(startX, startY, startZ).normalize();

        Color c1 = score > 0.8f ? new Color(50, 255, 100) : new Color(200, 255, 150);
        WorldParticleBuilder.create(ParticleRegistry.MANA_WISP.get())
                .setColorData(ColorParticleData.create(c1, new Color(0, 50, 20, 0)).build())
                .setTransparencyData(GenericParticleData.create(0.6f, 0.0f).build())
                .setScaleData(GenericParticleData.create(0.08f, 0.0f).build())
                .setLifetime(20)
                .setMotion(direction.x * 0.1, direction.y * 0.1, direction.z * 0.1)
                .enableNoClip()
                .spawn(level, startX + (level.random.nextDouble()-0.5)*0.5, startY, startZ + (level.random.nextDouble()-0.5)*0.5);
    }

    private void animateHeart(Level level, BlockPos pos, boolean isAngry) {
        if (level.random.nextFloat() < 0.2f) {
            Color c1 = isAngry ? new Color(100, 0, 0) : new Color(20, 100, 50);
            WorldParticleBuilder.create(ParticleRegistry.SMOKE_PARTICLE.get())
                    .setColorData(ColorParticleData.create(c1, new Color(0, 0, 0, 0)).build())
                    .setTransparencyData(GenericParticleData.create(0.4f, 0.0f).build())
                    .setScaleData(GenericParticleData.create(0.3f, 0.5f).build())
                    .setLifetime(80)
                    .setMotion(0, 0.005, 0)
                    .enableNoClip()
                    .spawn(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        }
    }
}
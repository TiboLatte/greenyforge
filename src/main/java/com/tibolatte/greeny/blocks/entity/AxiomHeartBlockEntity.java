package com.tibolatte.greeny.blocks.entity;

import com.tibolatte.greeny.blocks.AxiomHeartBlock;
import com.tibolatte.greeny.blocks.HeartState;
import com.tibolatte.greeny.mobs.AxiomGuardianEntity;
import com.tibolatte.greeny.registry.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
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

import java.awt.Color; // FIX: Specific import only
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
public class AxiomHeartBlockEntity extends BlockEntity {

    // =============================================================
    //               GAMEPLAY CONFIGURATION
    // =============================================================

    private static final int PULSE_LOOP_SPEED = 5 * 20;       // 5s Pulse
    private static final int ANGER_BUILDUP_TIME = 4 * 20;     // 4s Warning
    private static final int ANGER_REFRACTORY_TIME = 20 * 20; // 20s Angry state
    private static final int SHUTDOWN_COOLDOWN_TIME = 600 * 20; // 10 mins Dead

    private static final int MAX_ANGER_STRIKES = 3;
    private static final float SCORE_GAIN_PER_TICK = 0.015f;
    private static final float SCORE_DECAY_PER_TICK = 0.05f;
    private static final float SCORE_PUNISHMENT = 0.1f;
    private static final int MAX_CHARGES = 10;


    private static final double DETECTION_RADIUS = 20.0;
    private static final double KNOCKBACK_HORIZONTAL = 1.8;
    private static final double KNOCKBACK_VERTICAL = 1.3;

    // =============================================================
    //               STATE VARIABLES
    // =============================================================

    private int pulseTicks = 0;
    private int angerTicks = 0;
    private boolean hasExplodedThisCycle = false;
    private int repairCharges = 0;

    // Logic Tracking
    private int angerStrikes = 0;
    private int shutdownTimer = 0;
    private long lastStrikeTime = 0; // Anti-spam timer

    private final Map<UUID, Float> serverResonance = new HashMap<>();
    private final Map<UUID, Float> clientResonance = new HashMap<>();

    public AxiomHeartBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.AXIOM_HEART.get(), pos, state);
    }

    // =============================================================
    //                  CORE TICK LOGIC
    // =============================================================

    public static void tick(Level level, BlockPos pos, BlockState state, AxiomHeartBlockEntity blockEntity) {

        // 1. DORMANT STATE (Sleep Mode)
        if (state.getValue(AxiomHeartBlock.STATE) == HeartState.DORMANT || blockEntity.shutdownTimer > 0) {
            if (blockEntity.shutdownTimer > 0) {
                blockEntity.shutdownTimer--;
            } else {
                blockEntity.resetToActive(); // Wake up
            }
            return; // Don't run animation logic
        }

        // 2. TIMERS
        if (blockEntity.hasExplodedThisCycle && blockEntity.angerTicks > 0) {
            blockEntity.pulseTicks = 0;
        } else {
            blockEntity.pulseTicks++;
        }

        if (blockEntity.angerTicks > 0) blockEntity.angerTicks--;
        boolean isAngry = blockEntity.angerTicks > 0;

        // 3. VISUAL RESET (If anger expired naturally)
        if (!isAngry && blockEntity.hasExplodedThisCycle) {
            blockEntity.hasExplodedThisCycle = false;
            if (blockEntity.shutdownTimer <= 0 && state.getValue(AxiomHeartBlock.STATE) == HeartState.ANGRY) {
                blockEntity.updateBlockState(HeartState.ACTIVE);
            }
        }

        // 4. ANGER SEQUENCE
        if (isAngry) {
            // Force Red Texture
            if (state.getValue(AxiomHeartBlock.STATE) != HeartState.ANGRY) {
                blockEntity.updateBlockState(HeartState.ANGRY);
            }

            // --- CLIENT VISUALS (SHAKE FIX) ---
            if (level.isClientSide) {
                // 1. BUILDUP PHASE (Rumble)
                if (blockEntity.pulseTicks >= PULSE_LOOP_SPEED && blockEntity.pulseTicks < PULSE_LOOP_SPEED + ANGER_BUILDUP_TIME) {

                    // FIX: Shake every 0.25s (5 ticks) so it feels like it's charging up
                    if (blockEntity.pulseTicks % 5 == 0) {
                        ScreenshakeHandler.addScreenshake(new ScreenshakeInstance(10)
                                .setIntensity(0.1f, 0.3f).setEasing(Easing.SINE_IN));
                    }

                    blockEntity.spawnAngerBuildupParticles(pos, blockEntity.pulseTicks);
                }
            }

            // 2. EXPLOSION PHASE (Boom)
            if (blockEntity.pulseTicks >= PULSE_LOOP_SPEED + ANGER_BUILDUP_TIME) {
                blockEntity.pulseTicks = 0;
                blockEntity.hasExplodedThisCycle = true; // Lock animation

                if (level.isClientSide) {
                    // FIX: Add BIG Shake on explosion
                    ScreenshakeHandler.addScreenshake(new ScreenshakeInstance(30)
                            .setIntensity(0.6f, 1.5f).setEasing(Easing.EXPO_OUT));

                    blockEntity.spawnAngerShockwave(pos);
                } else {
                    blockEntity.applyShockwaveKnockback(level, pos);
                    blockEntity.spawnGuardianDefender(level, pos);

                    // CHECK DEATH
                    if (blockEntity.angerStrikes >= MAX_ANGER_STRIKES) {
                        blockEntity.initiateShutdown();
                    }
                }
            }
        }
        // 5. PASSIVE PULSE
        else {
            if (state.getValue(AxiomHeartBlock.STATE) != HeartState.ACTIVE) {
                blockEntity.updateBlockState(HeartState.ACTIVE);
            }

            if (blockEntity.pulseTicks >= PULSE_LOOP_SPEED) {
                blockEntity.pulseTicks = 0;
                if (level.isClientSide) blockEntity.spawnGreenShockwave(pos);
                else blockEntity.triggerPulseRewards(level, pos);
            }

            if (blockEntity.pulseTicks % 20 == 0){
                blockEntity.scanForFood(level, pos);
            }

            if (blockEntity.pulseTicks % 10 == 0){
                blockEntity.scanForIntruders(level, pos);
            }
            if (!level.isClientSide) blockEntity.updateServerLogic(level, pos);
        }

        // 6. CONTINUOUS VISUALS
        if (level.isClientSide) {
            blockEntity.animateHeart(level, pos, isAngry);
            if (!isAngry) blockEntity.animatePlayerTethers(level, pos);
        } else {
            // Server Logic handled above
        }
    }

    // =============================================================
    //                  TRIGGER METHODS
    // =============================================================

    public void triggerAnger() {
        if (level == null || level.isClientSide) return;
        if (getBlockState().getValue(AxiomHeartBlock.STATE) == HeartState.DORMANT) return;

        // Anti-Spam
        long time = level.getGameTime();
        if (time - lastStrikeTime < 10) return;
        lastStrikeTime = time;

        // 1. ADD STRIKE
        this.angerStrikes++;
        setChanged();

        // 2. START ANIMATION
        boolean isBusyExploding = (this.pulseTicks >= PULSE_LOOP_SPEED) &&
                (this.pulseTicks < PULSE_LOOP_SPEED + ANGER_BUILDUP_TIME);

        if (!isBusyExploding) {
            // Start Warning Phase
            this.pulseTicks = PULSE_LOOP_SPEED;
            this.hasExplodedThisCycle = false;
            this.angerTicks = ANGER_BUILDUP_TIME + ANGER_REFRACTORY_TIME;

            updateBlockState(HeartState.ANGRY);
            serverResonance.clear();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        } else {
            // Already counting down to boom? Just extend the red state.
            this.angerTicks = Math.max(this.angerTicks, ANGER_REFRACTORY_TIME);
        }
    }

    private void initiateShutdown() {
        this.shutdownTimer = SHUTDOWN_COOLDOWN_TIME;
        this.angerTicks = 0;
        this.pulseTicks = 0;
        this.angerStrikes = 0;
        this.hasExplodedThisCycle = false;

        serverResonance.clear();
        clientResonance.clear();

        updateBlockState(HeartState.DORMANT);

        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    private void resetToActive() {
        this.shutdownTimer = 0;
        updateBlockState(HeartState.ACTIVE);
    }

    private void updateBlockState(HeartState newState) {
        if (level != null && !level.isClientSide) {
            BlockState currentState = getBlockState();
            if (currentState.getValue(AxiomHeartBlock.STATE) != newState) {
                level.setBlock(worldPosition, currentState.setValue(AxiomHeartBlock.STATE, newState), 3);
            }
        }
    }

    private void scanForFood(Level level, BlockPos pos){
        AABB searchBox = new AABB(pos).inflate(1.5);
        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, searchBox);
        for (ItemEntity itemEntity : items){
            ItemStack stack = itemEntity.getItem();
            // CHECK: Is it Axiom Dust?
            if (stack.is(ItemRegistry.AXIOM_DUST.get())) {
                if (this.repairCharges < MAX_CHARGES){
                    stack.shrink(1);
                    this.repairCharges++;
                    this.setChanged();

                    level.playSound(null, pos, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.BLOCKS, 1.0f, 1.5f);
                    level.globalLevelEvent(2005,pos,0);
                }
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

        // Track if anything changed this tick
        boolean anyScoreChanged = false;

        for (Player player : players) {
            UUID id = player.getUUID();
            float score = serverResonance.getOrDefault(id, 0.0f);
            float oldScore = score; // Snapshot for comparison

            boolean isSprinting = player.isSprinting();
            boolean isStill = isStationary(player);
            boolean isArmed = isHoldingTool(player);

            if (isSprinting || isArmed) {
                score -= SCORE_PUNISHMENT;
                if (player.hasEffect(MobEffectRegistry.FOREST_MARK.get())) {
                    player.removeEffect(MobEffectRegistry.FOREST_MARK.get());
                }
            } else if (isStill) {
                // CAP at 50% for passive standing
                if(score <= 0.5f){
                    score += SCORE_GAIN_PER_TICK;
                }
            } else {
                score -= SCORE_DECAY_PER_TICK;
            }

            score = Math.max(0.0f, Math.min(1.0f, score));
            serverResonance.put(id, score);

            // Check if value actually changed
            if (score != oldScore) {
                anyScoreChanged = true;
            }
        }

        // --- THE FIX ---
        // 1. If scores changed...
        // 2. AND it has been 10 ticks (0.5 seconds)...
        // 3. THEN send the packet.
        if (anyScoreChanged) {
            setChanged(); // Mark chunk as dirty (Save to disk)

            if (level.getGameTime() % 10 == 0) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
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

    // 2. REPAIRING (Called by Block Right-Click)
    public void tryRepairItem(Player player) {
        if (level == null || level.isClientSide) return;

        UUID id = player.getUUID();
        float resonance = serverResonance.getOrDefault(id, 0.0f);

        // CHECK 1: Is the player a "Gardener" (Tier 2)?
        if (resonance <= 0.5f) { // FIX: Changed < to <= to be strict
            // Fail: Not trusted enough
            level.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5f, 0.5f);
            return;
        }
        // CHECK 2: Does the heart have energy?
        if (this.repairCharges <= 0) {
            // Fail: Empty
            level.playSound(null, worldPosition, SoundEvents.DISPENSER_FAIL, SoundSource.BLOCKS, 1.0f, 1.2f);
            return;
        }

        // CHECK 3: Is item damaged?
        ItemStack held = player.getMainHandItem();
        if (held.isDamageableItem() && held.isDamaged()) {
            // SUCCESS!
            int repairAmount = held.getMaxDamage() / 4; // Repair 25%
            held.setDamageValue(Math.max(0, held.getDamageValue() - repairAmount));

            this.repairCharges--;
            this.setChanged();
            // Force sync so client sees charges drop
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);

            level.playSound(null, worldPosition, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.5f, 1.5f);
            level.playSound(null, worldPosition, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0f, 1.0f);
        }
    }

    // 3. TREMORSENSE (Detecting Mobs on Roots)
    private void scanForIntruders(Level level, BlockPos pos) {
        AABB area = new AABB(pos).inflate(DETECTION_RADIUS); // 20 blocks
        List<Monster> monsters = level.getEntitiesOfClass(Monster.class, area);

        for (Monster mob : monsters) {
            BlockPos mobPos = mob.blockPosition();
            BlockState below = level.getBlockState(mobPos.below());

            // Check if standing on Roots or Axiom Soil
            if (below.is(BlockRegistry.ANCIENT_ROOT.get()) || below.is(BlockRegistry.AXIOM_SOIL.get())) {
                // HIGHLIGHT THEM
                if (!mob.hasEffect(MobEffects.GLOWING)) {
                    mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0, false, false));
                }
            }
        }
    }

    private void spawnGuardianDefender(Level level, BlockPos pos) {
        Player targetPlayer = level.getNearestPlayer(pos.getX(), pos.getY(), pos.getZ(), 20.0, true);
        BlockPos searchCenter = (targetPlayer != null) ? targetPlayer.blockPosition() : pos;
        BlockPos spawnPos = findValidSpawnSpot(level, searchCenter);
        if (spawnPos == null) spawnPos = findValidSpawnSpot(level, pos);
        if (spawnPos == null) spawnPos = pos.above();

        var guardian = EntityRegistry.AXIOM_GUARDIAN.get().create(level);
        if (guardian != null) {
            guardian.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, 0.0f, 0.0f);
            if (guardian instanceof AxiomGuardianEntity) {
                ((AxiomGuardianEntity) guardian).setSummoned(true);
            }
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
        saveAdditional(tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        super.handleUpdateTag(tag);
        load(tag);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net, ClientboundBlockEntityDataPacket pkt) {
        handleUpdateTag(pkt.getTag());
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Anger", angerTicks);
        tag.putInt("Pulse", pulseTicks);
        tag.putInt("Strikes", angerStrikes);
        tag.putInt("Shutdown", shutdownTimer);
        tag.putBoolean("Exploded", hasExplodedThisCycle);
        tag.putInt("RepairCharges", repairCharges);

        // SYNC FIX: Save the Trust Map
        net.minecraft.nbt.ListTag resonanceList = new net.minecraft.nbt.ListTag();
        for (Map.Entry<UUID, Float> entry : serverResonance.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putUUID("UUID", entry.getKey());
            entryTag.putFloat("Score", entry.getValue());
            resonanceList.add(entryTag);
        }
        tag.put("ResonanceMap", resonanceList);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.angerTicks = tag.getInt("Anger");
        this.pulseTicks = tag.getInt("Pulse");
        this.angerStrikes = tag.getInt("Strikes");
        this.shutdownTimer = tag.getInt("Shutdown");
        this.hasExplodedThisCycle = tag.getBoolean("Exploded");
        this.repairCharges = tag.getInt("RepairCharges");

        // SYNC FIX: Load the Trust Map
        if (tag.contains("ResonanceMap")) {
            net.minecraft.nbt.ListTag resonanceList = tag.getList("ResonanceMap", 10);
            serverResonance.clear();
            clientResonance.clear();

            for (int i = 0; i < resonanceList.size(); i++) {
                CompoundTag entryTag = resonanceList.getCompound(i);
                UUID id = entryTag.getUUID("UUID");
                float score = entryTag.getFloat("Score");
                serverResonance.put(id, score);
                clientResonance.put(id, score); // Update client immediately
            }
        }
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

        WorldParticleBuilder.create(ParticleRegistry.STAR_PARTICLE.get())
                .setColorData(ColorParticleData.create(new Color(100, 0, 0), new Color(0, 0, 0)).build())
                .setScaleData(GenericParticleData.create(0.2f, 0.0f).build())
                .setLifetime(ticksUntilBoom)
                .spawn(level, cx, cy, cz);

        if (ticksUntilBoom < 20 && level.random.nextBoolean()) {
            WorldParticleBuilder.create(ParticleRegistry.MANA_WISP.get())
                    .setColorData(ColorParticleData.create(new Color(255, 0, 0), new Color(50, 0, 0)).build())
                    .setScaleData(GenericParticleData.create(0.5f, 0.8f).build())
                    .setLifetime(10)
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
            // FIX: Just read the map. Don't simulate calculations.
            float visualScore = clientResonance.getOrDefault(id, 0.0f);

            // Cut beam if holding weapon visually
            if (isHoldingTool(player)) {
                spawnRejectionParticles(level, player);
                continue;
            }

            // Only show beam if score > 10%
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

    // =============================================================
    //                  PUBLIC API (For Events)
    // =============================================================

    /**
     * Called by external events (Planting, Feeding) to instantly boost trust.
     */
    public void boostResonance(UUID playerId, float amount) {
        float current = serverResonance.getOrDefault(playerId, 0.0f);
        float newScore = Math.min(1.0f, current + amount);
        serverResonance.put(playerId, newScore);

        // Mark block for update so logic (like Tethers) refreshes immediately
        this.setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
}
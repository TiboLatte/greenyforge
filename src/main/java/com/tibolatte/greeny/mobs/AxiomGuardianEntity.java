package com.tibolatte.greeny.mobs;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class AxiomGuardianEntity extends Animal implements GeoEntity {

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    // SYNCHED DATA (Visible to both Client and Server)
    private static final EntityDataAccessor<Boolean> IS_SUMMONED = SynchedEntityData.defineId(AxiomGuardianEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IS_ATTACKING = SynchedEntityData.defineId(AxiomGuardianEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IS_WAVING = SynchedEntityData.defineId(AxiomGuardianEntity.class, EntityDataSerializers.BOOLEAN);

    private static final int MAX_LIFE = 400;
    private int lifeTicks = 0;

    // Server-side timers
    private int waveTimer = 0;
    private int attackTimer = 0; // Counts down the attack duration

    public AxiomGuardianEntity(EntityType<? extends Animal> type, Level level) {
        super(type, level);
        this.xpReward = 0;
    }

    public void setSummoned(boolean summoned) {
        this.entityData.set(IS_SUMMONED, summoned);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25) // Slightly faster so it can actually reach players
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.ATTACK_KNOCKBACK, 1.5);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));

        // CUSTOM ATTACK GOAL
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2, true) {
            @Override
            protected void checkAndPerformAttack(LivingEntity enemy, double distToEnemySqr) {
                if (isTimeToAttack() && distToEnemySqr <= getAttackReachSqr(enemy)) {
                    // 1. Perform Damage
                    this.mob.doHurtTarget(enemy);
                    // 2. Start Animation Timer
                    ((AxiomGuardianEntity)mob).startAttackAnimation();
                    // 3. Reset Cooldown
                    this.resetAttackCooldown();
                } else {
                    // Standard cooldown management
                    super.checkAndPerformAttack(enemy, distToEnemySqr);
                }
            }
        });

        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0f));

        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true, (e) -> this.entityData.get(IS_SUMMONED)));
    }

    public void startAttackAnimation() {
        // Set the timer on the server
        this.attackTimer = 20; // 1 Second (Full animation length)
        this.entityData.set(IS_ATTACKING, true); // SYNC TO CLIENT
    }

    @Override
    public void tick() {
        super.tick();

        // DECREMENT TIMERS
        if (waveTimer > 0) {
            waveTimer--;
            if (waveTimer == 0) this.entityData.set(IS_WAVING, false);
        }

        if (attackTimer > 0) {
            attackTimer--;
            if (attackTimer == 0) this.entityData.set(IS_ATTACKING, false); // Animation Over
        }

        if (!this.level().isClientSide) {
            // Despawn Logic
            if (this.entityData.get(IS_SUMMONED)) {
                lifeTicks++;
                if (lifeTicks > MAX_LIFE) this.discard();
            }
            // Hello Logic
            else {
                if (this.random.nextInt(120) == 0 && this.getTarget() == null && waveTimer == 0 && attackTimer == 0) {
                    Player p = this.level().getNearestPlayer(this, 8.0);
                    if (p != null && this.hasLineOfSight(p)) {
                        this.entityData.set(IS_WAVING, true);
                        this.waveTimer = 40;
                    }
                }
            }
        }
    }

    // --- ANIMATION CONTROLLER ---
    // This runs on the CLIENT and decides what to show based on the data we synced.
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 5, state -> {

            // 1. SPAWN (Highest Priority - Visual only based on age)
            if (this.tickCount < 30) {
                return state.setAndContinue(RawAnimation.begin().thenPlay("animation.axiom_guardian.spawn"));
            }

            // 2. ATTACK (Synced via IS_ATTACKING)
            // If the server says we are attacking, we PLAY ATTACK.
            // This overrides walking because it is higher in the list.
            if (this.entityData.get(IS_ATTACKING)) {
                return state.setAndContinue(RawAnimation.begin().thenPlay("animation.axiom_guardian.attack"));
            }

            // 3. WAVE
            if (this.entityData.get(IS_WAVING)) {
                return state.setAndContinue(RawAnimation.begin().thenPlay("animation.axiom_guardian.hello"));
            }

            // 4. WALK
            double velocity = this.getDeltaMovement().horizontalDistanceSqr();
            if (velocity > 0.0001) {
                return state.setAndContinue(RawAnimation.begin().thenLoop("animation.axiom_guardian.walk"));
            }

            // 5. IDLE
            return state.setAndContinue(RawAnimation.begin().thenLoop("animation.axiom_guardian.idle"));
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return this.geoCache; }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(IS_SUMMONED, false);
        this.entityData.define(IS_ATTACKING, false);
        this.entityData.define(IS_WAVING, false);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Summoned", this.entityData.get(IS_SUMMONED));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setSummoned(tag.getBoolean("Summoned"));
    }

    @Nullable @Override public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob otherParent) { return null; }
}
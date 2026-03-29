package com.dimensionmod.entity.boss;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class InfernoAncient extends Monster {
    private static final int PHASE_TRANSITION_HEALTH = (int) (400 * 0.5);
    private static final int DEBUFF_COUNT = 2;
    private static final int MELEE_COOLDOWN = 30;
    private static final int RANGED_COOLDOWN = 60;
    private static final int SHIELD_DURATION = 200;
    private static final int PHASE2_DURATION = 300;

    private static final EntityDataAccessor<Integer> DATA_PHASE = SynchedEntityData.defineId(InfernoAncient.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_SHIELD = SynchedEntityData.defineId(InfernoAncient.class, EntityDataSerializers.BOOLEAN);

    private static final List<net.minecraft.world.effect.MobEffect> DEBUFF_POOL = Arrays.asList(
            MobEffects.WITHER, MobEffects.WEAKNESS, MobEffects.MOVEMENT_SLOWDOWN,
            MobEffects.HUNGER, MobEffects.DIG_SLOWDOWN, MobEffects.BLINDNESS
    );

    private final ServerBossEvent bossInfo = new ServerBossEvent(
            this.getDisplayName(), ServerBossEvent.BossBarColor.RED, ServerBossEvent.BossBarOverlay.PROGRESS);

    private int phase = 1;
    private int meleeCooldown = 0;
    private int rangedCooldown = 0;
    private boolean isEnraged = false;
    private int enragedTime = 0;
    private int shieldTicks = 0;
    private boolean hasShield = false;

    public InfernoAncient(EntityType<? extends InfernoAncient> type, Level level) {
        super(type, level);
        this.xpReward = 100;
        this.setCustomNameVisible(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 400)
                .add(Attributes.ARMOR, 8)
                .add(Attributes.ARMOR_TOUGHNESS, 4)
                .add(Attributes.ATTACK_DAMAGE, 18)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
                .add(Attributes.FOLLOW_RANGE, 30)
                .add(Attributes.MOVEMENT_SPEED, 0.25);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_PHASE, 1);
        builder.define(DATA_SHIELD, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, false));
        this.goalSelector.addGoal(3, new RandomStrollGoal(this, 0.8, 30));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 10f));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide) {
            if (meleeCooldown > 0) meleeCooldown--;
            if (rangedCooldown > 0) rangedCooldown--;

            if (this.getHealth() <= PHASE_TRANSITION_HEALTH && phase == 1) {
                transitionToPhase2();
            }

            if (phase == 2 && !hasShield && shieldTicks <= 0) {
                activateShield();
            }
            if (hasShield) {
                shieldTicks--;
                if (shieldTicks <= 0) {
                    deactivateShield();
                }
            }

            if (isEnraged) {
                enragedTime++;
                if (enragedTime >= PHASE2_DURATION) {
                    isEnraged = false;
                    enragedTime = 0;
                }
            }

            if (phase == 2 && rangedCooldown <= 0) {
                rangedAttack();
            }

            if (this.level().getGameTime() % 5 == 0) {
                spawnFlameParticles();
            }
        }
    }

    private void transitionToPhase2() {
        phase = 2;
        this.entityData.set(DATA_PHASE, 2);
        this.level().broadcastEntityEvent(this, (byte) 2);
        this.playSound(SoundEvents.WITHER_SPAWN, 1.5f, 0.8f);
        this.shieldTicks = SHIELD_DURATION;
        activateShield();
    }

    private void activateShield() {
        hasShield = true;
        this.entityData.set(DATA_SHIELD, true);
    }

    private void deactivateShield() {
        hasShield = false;
        this.entityData.set(DATA_SHIELD, false);
        this.playSound(SoundEvents.GENERIC_BREAK, 1.5f, 1.0f);
        isEnraged = true;
        enragedTime = 0;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (meleeCooldown > 0) return false;
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof Player player) {
            applyRandomDebuffs(player);
            meleeCooldown = MELEE_COOLDOWN;
        }
        return hit;
    }

    private void applyRandomDebuffs(Player player) {
        List<net.minecraft.world.effect.MobEffect> shuffled = new ArrayList<>(DEBUFF_POOL);
        java.util.Collections.shuffle(shuffled, new Random());
        int count = Math.min(DEBUFF_COUNT, shuffled.size());
        for (int i = 0; i < count; i++) {
            net.minecraft.world.effect.MobEffect effect = shuffled.get(i);
            int duration = 100 + this.random.nextInt(100);
            int amplifier = this.random.nextInt(2);
            player.addEffect(new MobEffectInstance(effect, duration, amplifier));
        }
    }

    private void rangedAttack() {
        List<Player> players = this.level().getEntitiesOfClass(Player.class,
                new net.minecraft.world.phys.AABB(
                        this.getX() - 20, this.getY() - 10, this.getZ() - 20,
                        this.getX() + 20, this.getY() + 10, this.getZ() + 20));
        if (!players.isEmpty()) {
            Player target = players.get(this.random.nextInt(players.size()));
            double dx = target.getX() - this.getX();
            double dy = target.getY() - this.getY();
            double dz = target.getZ() - this.getZ();
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

            net.minecraft.world.entity.projectile.SmallFireball fireball =
                    new net.minecraft.world.entity.projectile.SmallFireball(
                            net.minecraft.world.entity.projectile.EntityType.SMALL_FIREBALL,
                            this.level()
            );
            fireball.setPos(this.getX(), this.getY() + 1.5, this.getZ());
            fireball.shoot(dx / dist, dy / dist, dz / dist, 1.5f, 0);
            this.level().addFreshEntity(fireball);
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.BLAZE_SHOOT, this.getSoundSource(), 0.8f, 0.8f);
            rangedCooldown = RANGED_COOLDOWN;
        }
    }

    private void spawnFlameParticles() {
        for (int i = 0; i < 3; i++) {
            this.level().addParticle(ParticleTypes.FLAME,
                    this.getX() + (this.random.nextDouble() - 0.5) * 2,
                    this.getY() + this.random.nextDouble() * 2,
                    this.getZ() + (this.random.nextDouble() - 0.5) * 2,
                    0, 0.05, 0);
        }
        if (phase == 2 || isEnraged) {
            this.level().addParticle(ParticleTypes.LAVA,
                    this.getX() + (this.random.nextDouble() - 0.5) * 2,
                    this.getY(),
                    this.getZ() + (this.random.nextDouble() - 0.5) * 2,
                    0, 0.1, 0);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (hasShield && source != this.level().damageSources().outOfWorld()) {
            amount *= 0.2f;
        }
        if (isEnraged) {
            amount *= 1.3f;
        }
        return super.hurt(source, amount);
    }

    @Override
    public void customServerAiStep() {
        super.customServerAiStep();
        this.bossInfo.setProgress(this.getHealth() / this.getMaxHealth());
        this.bossInfo.setName(
                isEnraged ? net.minecraft.network.chat.Component.literal("§c熔岩古王 §4【狂怒】") :
                phase == 2 ? net.minecraft.network.chat.Component.literal("§6熔岩古王 §e【二阶段】") :
                             net.minecraft.network.chat.Component.literal("§6熔岩古王"));
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossInfo.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossInfo.removePlayer(player);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        phase = tag.getInt("Phase");
        isEnraged = tag.getBoolean("Enraged");
        enragedTime = tag.getInt("EnragedTime");
        shieldTicks = tag.getInt("ShieldTicks");
        hasShield = tag.getBoolean("HasShield");
        meleeCooldown = tag.getInt("MeleeCooldown");
        rangedCooldown = tag.getInt("RangedCooldown");
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Phase", phase);
        tag.putBoolean("Enraged", isEnraged);
        tag.putInt("EnragedTime", enragedTime);
        tag.putInt("ShieldTicks", shieldTicks);
        tag.putBoolean("HasShield", hasShield);
        tag.putInt("MeleeCooldown", meleeCooldown);
        tag.putInt("RangedCooldown", rangedCooldown);
    }

    @Override
    public boolean isFireImmune() {
        return true;
    }
}

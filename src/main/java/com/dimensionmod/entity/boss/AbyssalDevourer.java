package com.dimensionmod.entity.boss;

import net.minecraft.core.BlockPos;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class AbyssalDevourer extends Monster {
    private static final int PHASE2_HEALTH = (int) (600 * 0.4); // 40% HP
    private static final int PHASE1_SPIKE_COOLDOWN = 80;
    private static final int PHASE2_SUMMON_COOLDOWN = 200;
    private static final int SPIKE_DURATION = 40;

    private final ServerBossEvent bossInfo = (ServerBossEvent) new ServerBossEvent(
            this.getDisplayName(), ServerBossEvent.BossBarColor.PURPLE, ServerBossEvent.BossBarOverlay.PROGRESS)
            .setDarkenScreen(true);

    private int phase = 1;
    private int spikeCooldown = 0;
    private int summonCooldown = 0;
    private int spikeActiveTime = 0;
    private boolean isSpiking = false;
    private List<BlockPos> spikePositions = new ArrayList<>();
    private int attackCycle = 0; // alternates between melee and spike

    private static final EntityDataAccessor<Integer> DATA_PHASE = SynchedEntityData.defineId(AbyssalDevourer.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_SPIKING = SynchedEntityData.defineId(AbyssalDevourer.class, EntityDataSerializers.BOOLEAN);

    public AbyssalDevourer(EntityType<? extends AbyssalDevourer> type, Level level) {
        super(type, level);
        this.xpReward = 150;
        this.setCustomNameVisible(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 600)
                .add(Attributes.ARMOR, 12)
                .add(Attributes.ARMOR_TOUGHNESS, 6)
                .add(Attributes.ATTACK_DAMAGE, 22)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
                .add(Attributes.FOLLOW_RANGE, 35)
                .add(Attributes.MOVEMENT_SPEED, 0.22);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_PHASE, 1);
        builder.define(DATA_SPIKING, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new AbyssalMeleeAttackGoal(this, 1.1, false));
        this.goalSelector.addGoal(2, new RandomSwimmingGoal(this, 0.6, 40));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 12f));

        this.targetSelector.addGoal(0, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide) {
            if (spikeCooldown > 0) spikeCooldown--;
            if (summonCooldown > 0) summonCooldown--;

            // Phase transition
            if (this.getHealth() <= PHASE2_HEALTH && phase == 1) {
                transitionToPhase2();
            }

            // Phase 1: spike attack
            if (phase == 1 && spikeCooldown <= 0 && attackCycle % 2 == 1) {
                startSpikeAttack();
            }

            // Phase 2: summon minions
            if (phase == 2 && summonCooldown <= 0) {
                summonMinions();
            }

            // Active spike mechanic
            if (isSpiking) {
                spikeActiveTime++;
                // Deal damage to players standing on spikes
                dealSpikeDamage();
                if (spikeActiveTime >= SPIKE_DURATION) {
                    endSpikeAttack();
                }
            }

            // Ambient particles
            if (this.level().getGameTime() % 8 == 0) {
                spawnAbyssParticles();
            }
        }
    }

    private void transitionToPhase2() {
        phase = 2;
        this.entityData.set(DATA_PHASE, 2);
        this.level().broadcastEntityEvent(this, (byte) 2);
        this.playSound(SoundEvents.ENDER_DRAGON_GROWL, 1.5f, 0.7f);
        // Spawn portal-like effects
        for (int i = 0; i < 8; i++) {
            Vec3 offset = new Vec3(
                    (this.random.nextDouble() - 0.5) * 6,
                    this.random.nextDouble() * 3,
                    (this.random.nextDouble() - 0.5) * 6
            );
            this.level().addParticle(ParticleTypes.PORTAL,
                    this.getX() + offset.x, this.getY() + offset.y, this.getZ() + offset.z,
                    offset.x * 0.1, offset.y * 0.1, offset.z * 0.1
            );
        }
    }

    private void startSpikeAttack() {
        isSpiking = true;
        spikeActiveTime = 0;
        this.entityData.set(DATA_SPIKING, true);
        // Raise spikes around the boss
        spikePositions.clear();
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                if (Math.abs(x) + Math.abs(z) <= 5 && (x != 0 || z != 0)) {
                    BlockPos pos = this.blockPosition().offset(x, 0, z);
                    if (this.level().getBlockState(pos).isAir()) {
                        spikePositions.add(pos.immutable());
                    }
                }
            }
        }
        // Visual warning
        for (BlockPos p : spikePositions) {
            this.level().addParticle(ParticleTypes.SCULK_CHARGE,
                    p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 0, 0, 0
            );
        }
    }

    private void dealSpikeDamage() {
        if (spikeActiveTime < 15) return; // first 15 ticks = warning
        for (BlockPos p : spikePositions) {
            AABB box = new AABB(p);
            List<Player> players = this.level().getEntitiesOfClass(Player.class, box.inflate(0.5, 1, 0.5));
            for (Player player : players) {
                if (player.hurts(this.level().damageSources().cactus(), 2.0f)) {
                    player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0));
                }
            }
        }
    }

    private void endSpikeAttack() {
        isSpiking = false;
        spikeActiveTime = 0;
        this.entityData.set(DATA_SPIKING, false);
        spikePositions.clear();
        spikeCooldown = PHASE1_SPIKE_COOLDOWN;
        attackCycle++;
    }

    private void summonMinions() {
        for (int i = 0; i < 3; i++) {
            double angle = this.random.nextDouble() * Math.PI * 2;
            double dist = 3 + this.random.nextDouble() * 3;
            BlockPos spawnPos = this.blockPosition().offset(
                    (int) (Math.cos(angle) * dist),
                    0,
                    (int) (Math.sin(angle) * dist)
            );
            if (this.level().getBlockState(spawnPos).isAir()) {
                // Summon a "Abyssal Shambler" - a brute version of zombie
                EntityType<?> minionType = EntityType.ZOMBIE;
                LivingEntity minion = (LivingEntity) minionType.create(this.level());
                if (minion != null) {
                    minion.setPos(spawnPos.getX() + 0.5, spawnPos.getY() + 0.5, spawnPos.getZ() + 0.5);
                    minion.setCustomName(net.minecraft.network.chat.Component.literal("§5深渊侍从"));
                    minion.setCustomNameVisible(true);
                    // Boost stats
                    minion.getAttribute(Attributes.MAX_HEALTH).setBaseValue(40);
                    minion.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(8);
                    minion.heal(40);
                    this.level().addFreshEntity(minion);
                }
            }
        }
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.ENDERMAN_TELEPORT, this.getSoundSource(), 1.0f, 0.8f);
        summonCooldown = PHASE2_SUMMON_COOLDOWN;
    }

    private void spawnAbyssParticles() {
        for (int i = 0; i < 4; i++) {
            this.level().addParticle(ParticleTypes.AMBIENT_ENTITY_AT,
                    this.getX() + (this.random.nextDouble() - 0.5) * 3,
                    this.getY() + this.random.nextDouble() * 3,
                    this.getZ() + (this.random.nextDouble() - 0.5) * 3,
                    0, 0.02, 0
            );
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (target instanceof Player player) {
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1));
        }
        return super.doHurtTarget(target);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        this.bossInfo.setProgress(this.getHealth() / this.getMaxHealth());
        this.bossInfo.setName(phase == 2 ? "§5深渊吞噬者 §d【觉醒】" : "§5深渊吞噬者");
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
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        phase = tag.getInt("Phase");
        spikeCooldown = tag.getInt("SpikeCooldown");
        summonCooldown = tag.getInt("SummonCooldown");
        attackCycle = tag.getInt("AttackCycle");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Phase", phase);
        tag.putInt("SpikeCooldown", spikeCooldown);
        tag.putInt("SummonCooldown", summonCooldown);
        tag.putInt("AttackCycle", attackCycle);
    }

    static class AbyssalMeleeAttackGoal extends MeleeAttackGoal {
        public AbyssalMeleeAttackGoal(PathfinderMob entity, double speed, boolean followingTargetEvenIfNotSeen) {
            super(entity, speed, followingTargetEvenIfNotSeen);
        }
        @Override
        public boolean canUse() {
            // Don't melee attack while spiking
            if (((AbyssalDevourer) this.mob).isSpiking) return false;
            return super.canUse();
        }
    }
}

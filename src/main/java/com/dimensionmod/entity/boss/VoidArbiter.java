package com.dimensionmod.entity.boss;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class VoidArbiter extends Monster {
    private static final int PHASE2_HEALTH = (int) (800 * 0.35);
    private static final int TELEPORT_COOLDOWN = 100;
    private static final int PULL_COOLDOWN = 120;
    private static final int SPACE_SHRINK_INTERVAL = 400;
    private static final float SPACE_SHRINK_RADIUS_START = 30;
    private static final float SPACE_SHRINK_RADIUS_END = 8;

    private static final EntityDataAccessor<Integer> DATA_PHASE = SynchedEntityData.defineId(VoidArbiter.class, EntityDataSerializers.INT);

    private final ServerBossEvent bossInfo = new ServerBossEvent(
            this.getDisplayName(), ServerBossEvent.BossBarColor.DARK_BLUE, ServerBossEvent.BossBarOverlay.PROGRESS);

    private int phase = 1;
    private int teleportCooldown = 0;
    private int pullCooldown = 0;
    private int shrinkTimer = 0;
    private float currentArenaRadius = SPACE_SHRINK_RADIUS_START;
    private boolean isPulling = false;
    private int attackCycle = 0;

    public VoidArbiter(EntityType<? extends VoidArbiter> type, Level level) {
        super(type, level);
        this.xpReward = 200;
        this.setCustomNameVisible(true);
        this.noPhysics = true;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 800)
                .add(Attributes.ARMOR, 15)
                .add(Attributes.ARMOR_TOUGHNESS, 8)
                .add(Attributes.ATTACK_DAMAGE, 25)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 50)
                .add(Attributes.MOVEMENT_SPEED, 0.35);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_PHASE, 1);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.3, false));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.8, 30));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 15f));

        this.targetSelector.addGoal(0, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        this.setNoGravity(true);
        if (!this.level().isClientSide) {
            if (teleportCooldown > 0) teleportCooldown--;
            if (pullCooldown > 0) pullCooldown--;
            shrinkTimer++;

            if (this.getHealth() <= PHASE2_HEALTH && phase == 1) {
                transitionToPhase2();
            }

            if (phase == 1) {
                if (teleportCooldown <= 0) teleportToPlayer();
                if (pullCooldown <= 0) activateVoidPull();
            }

            if (phase == 2) {
                if (shrinkTimer >= SPACE_SHRINK_INTERVAL / 2) {
                    shrinkArena();
                }
                if (teleportCooldown <= 0 && attackCycle % 3 == 0) {
                    teleportAndSpawnSpikes();
                }
            }

            if (isPulling && pullCooldown <= 0) {
                applyVoidPull();
            }

            if (this.level().getGameTime() % 4 == 0) {
                spawnVoidParticles();
            }
        }
    }

    private void transitionToPhase2() {
        phase = 2;
        shrinkTimer = 0;
        currentArenaRadius = SPACE_SHRINK_RADIUS_START;
        this.level().broadcastEntityEvent(this, (byte) 2);
        this.playSound(SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE, 2.0f, 0.6f);
        for (int i = 0; i < 30; i++) {
            this.level().addParticle(ParticleTypes.REVERSE_PORTAL,
                    this.getX() + (this.random.nextDouble() - 0.5) * 5,
                    this.getY() + (this.random.nextDouble() - 0.5) * 5,
                    this.getZ() + (this.random.nextDouble() - 0.5) * 5, 0, 0, 0);
        }
    }

    private void teleportToPlayer() {
        List<Player> players = this.level().getEntitiesOfClass(Player.class,
                new AABB(this.getX() - 30, this.getY() - 15, this.getZ() - 30,
                        this.getX() + 30, this.getY() + 15, this.getZ() + 30));
        if (!players.isEmpty()) {
            Player target = players.get(this.random.nextInt(players.size()));
            Vec3 behind = target.getViewVector(1.0f).scale(-3);
            Vec3 newPos = target.position().add(behind).add(0, 2, 0);
            this.level().addParticle(ParticleTypes.PORTAL, this.getX(), this.getY(), this.getZ(), 0, 0, 0);
            this.teleportTo(newPos.x, newPos.y, newPos.z);
            this.level().addParticle(ParticleTypes.PORTAL, this.getX(), this.getY(), this.getZ(), 0, 0, 0);
            this.playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0f, 0.8f);
            teleportCooldown = TELEPORT_COOLDOWN;
        }
    }

    private void teleportAndSpawnSpikes() {
        List<Player> players = this.level().getEntitiesOfClass(Player.class,
                new AABB(this.getX() - 30, this.getY() - 15, this.getZ() - 30,
                        this.getX() + 30, this.getY() + 15, this.getZ() + 30));
        if (!players.isEmpty()) {
            Player target = players.get(this.random.nextInt(players.size()));
            Vec3 offset = new Vec3(
                    (this.random.nextDouble() - 0.5) * 8,
                    this.random.nextDouble() * 4,
                    (this.random.nextDouble() - 0.5) * 8);
            Vec3 newPos = target.position().add(offset);
            this.level().addParticle(ParticleTypes.PORTAL, this.getX(), this.getY(), this.getZ(), 0, 0, 0);
            this.teleportTo(newPos.x, newPos.y, newPos.z);
            this.level().addParticle(ParticleTypes.PORTAL, this.getX(), this.getY(), this.getZ(), 0, 0, 0);
            spawnVoidSpikesAt(target.blockPosition());
            teleportCooldown = TELEPORT_COOLDOWN - 20;
            attackCycle++;
        }
    }

    private void spawnVoidSpikesAt(net.minecraft.core.BlockPos targetPos) {
        for (int i = 0; i < 15; i++) {
            this.level().addParticle(ParticleTypes.DRAGON_BREATH,
                    targetPos.getX() + 0.5 + (this.random.nextDouble() - 0.5),
                    targetPos.getY() + 0.5,
                    targetPos.getZ() + 0.5 + (this.random.nextDouble() - 0.5),
                    0, 0.1, 0);
        }
        AABB damageBox = new AABB(targetPos).inflate(1.5, 2, 1.5);
        List<Player> players = this.level().getEntitiesOfClass(Player.class, damageBox);
        for (Player p : players) {
            p.hurt(this.level().damageSources().magic(), 12f);
            p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
        }
    }

    private void activateVoidPull() {
        isPulling = true;
        pullCooldown = PULL_COOLDOWN;
    }

    private void applyVoidPull() {
        Vec3 center = this.position();
        AABB searchBox = new AABB(
                center.x - currentArenaRadius, center.y - 10, center.z - currentArenaRadius,
                center.x + currentArenaRadius, center.y + 10, center.z + currentArenaRadius);
        List<Player> players = this.level().getEntitiesOfClass(Player.class, searchBox);
        for (Player p : players) {
            double dist = p.distanceTo(this);
            if (dist > currentArenaRadius * 0.7) {
                Vec3 pull = center.subtract(p.position()).normalize().scale(0.3);
                p.setDeltaMovement(p.getDeltaMovement().add(pull));
                p.hurtMarked = true;
            }
        }
    }

    private void shrinkArena() {
        if (currentArenaRadius > SPACE_SHRINK_RADIUS_END) {
            currentArenaRadius -= 2;
            for (float angle = 0; angle < Math.PI * 2; angle += 0.3f) {
                double x = this.getX() + Math.cos(angle) * currentArenaRadius;
                double z = this.getZ() + Math.sin(angle) * currentArenaRadius;
                this.level().addParticle(ParticleTypes.REVERSE_PORTAL, x, this.getY(), z, 0, 0, 0);
            }
            shrinkTimer = 0;
        }
    }

    private void spawnVoidParticles() {
        for (int i = 0; i < 3; i++) {
            this.level().addParticle(ParticleTypes.REVERSE_PORTAL,
                    this.getX() + (this.random.nextDouble() - 0.5) * 3,
                    this.getY() + (this.random.nextDouble() - 0.5) * 3,
                    this.getZ() + (this.random.nextDouble() - 0.5) * 3, 0, 0, 0);
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (target instanceof Player player) {
            player.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 1));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2));
        }
        return super.doHurtTarget(target);
    }

    @Override
    public void customServerAiStep() {
        super.customServerAiStep();
        this.bossInfo.setProgress(this.getHealth() / this.getMaxHealth());
        this.bossInfo.setName(
                phase == 2 ? Component.literal("§3虚空裁决者 §b【空间折叠】") : Component.literal("§3虚空裁决者"));
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
        teleportCooldown = tag.getInt("TeleportCooldown");
        pullCooldown = tag.getInt("PullCooldown");
        shrinkTimer = tag.getInt("ShrinkTimer");
        currentArenaRadius = tag.getFloat("ArenaRadius");
        attackCycle = tag.getInt("AttackCycle");
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Phase", phase);
        tag.putInt("TeleportCooldown", teleportCooldown);
        tag.putInt("PullCooldown", pullCooldown);
        tag.putInt("ShrinkTimer", shrinkTimer);
        tag.putFloat("ArenaRadius", currentArenaRadius);
        tag.putInt("AttackCycle", attackCycle);
    }
}

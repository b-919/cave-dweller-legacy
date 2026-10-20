package com.b919.cave_dweller_legacy.entity;

import com.b919.cave_dweller_legacy.common.ModSounds;
import com.b919.cave_dweller_legacy.common.config.ModConfig;
import com.b919.cave_dweller_legacy.entity.ai.EntityAICaveDwellerChase;
import com.b919.cave_dweller_legacy.entity.ai.EntityAICaveDwellerFlee;
import com.b919.cave_dweller_legacy.entity.ai.EntityAICaveDwellerStare;
import com.b919.cave_dweller_legacy.entity.ai.EntityAICaveDwellerStroll;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.pathfinding.PathNavigate;
import net.minecraft.pathfinding.PathNavigateClimber;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

public class CaveDwellerEntity extends EntityCreature {
    private static final DataParameter<Byte> STATE = EntityDataManager.createKey(CaveDwellerEntity.class, DataSerializers.BYTE);

    public Roll currentRoll = Roll.STROLL;
    private int lifeTicks = 0;
    private int chaseTicks = 0;
    public boolean pleaseStopMoving = false;
    private boolean hasSpawned = false;

    public CaveDwellerEntity(World worldIn) {
        super(worldIn);
        this.setSize(0.6F, 2.7F);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataManager.register(STATE, (byte) 0);
    }

    @Override
    protected void initEntityAI() {
        this.tasks.addTask(1, new EntityAICaveDwellerChase(this));
        this.tasks.addTask(2, new EntityAICaveDwellerStare(this));
        this.tasks.addTask(3, new EntityAICaveDwellerFlee(this, ModConfig.behavior.fleeSpeed));
        this.tasks.addTask(4, new EntityAICaveDwellerStroll(this));
        this.targetTasks.addTask(1, new EntityAIHurtByTarget(this, true));
        this.targetTasks.addTask(2, new EntityAINearestAttackableTarget<>(this, EntityPlayer.class, true, false));
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        this.getAttributeMap().registerAttribute(SharedMonsterAttributes.ATTACK_DAMAGE);
        this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(ModConfig.behavior.maxHealth);
        this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(ModConfig.behavior.movementSpeed);
        this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(ModConfig.behavior.attackDamage);
        this.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(ModConfig.behavior.followRange);
    }

    @Override
    protected PathNavigate createNavigator(@Nonnull World worldIn) {
        if (ModConfig.behavior.canClimb) {
            return new PathNavigateClimber(this, worldIn);
        }
        return super.createNavigator(worldIn);
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (world.isRemote) {
            return;
        }
        lifeTicks++;
        if (currentRoll == Roll.CHASE) {
            chaseTicks++;
        } else {
            chaseTicks = 0;
        }
        if (!hasSpawned && this.isInvisible()) {
            hasSpawned = true;
        }
        if (this.isInvisible() && getAttackTarget() != null && this.ticksExisted > 20) {
            this.setInvisible(false);
            playSpottedSound();
            if (ModConfig.general.debugMode) {
                System.out.println("[CAVE_DWELLER_DEBUG] CaveDweller revealed to target");
            }
        }
        int leaveTicks = ModConfig.timers.timeUntilLeave * 20;
        if (lifeTicks > leaveTicks) {
            if (ModConfig.general.debugMode) {
                System.out.println("[CAVE_DWELLER_DEBUG] CaveDweller leaving due to time limit");
            }
            disappear();
        }
        if (chaseTicks > ModConfig.behavior.chaseTimeUntilLeave * 20 && currentRoll == Roll.CHASE) {
            if (ModConfig.general.debugMode) {
                System.out.println("[CAVE_DWELLER_DEBUG] CaveDweller leaving during chase");
            }
            currentRoll = Roll.FLEE;
        }
    }

    public void disappear() {
        playDisappearSound();
        if (ModConfig.general.debugMode) {
            System.out.println("[CAVE_DWELLER_DEBUG] CaveDweller disappearing");
        }
        setDead();
    }

    public void playSpottedSound() {
        playSound(ModSounds.SPOTTED, 1.0F, 1.0F);
    }

    public void playDisappearSound() {
        playSound(ModSounds.DISAPPEAR, 1.0F, 1.0F);
    }

    public void playFleeSound() {
        if (ticksExisted % 20 == 0) {
            SoundEvent sound = ModSounds.FLEE_1;
            int rand = this.rand.nextInt(2);
            if (rand == 1) sound = ModSounds.FLEE_2;
            playSound(sound, 1.0F, 1.0F);
        }
    }

    @Override
    protected SoundEvent getHurtSound(@Nonnull DamageSource damageSourceIn) {
        int rand = this.rand.nextInt(4);
        switch (rand) {
            case 0: return ModSounds.HURT_1;
            case 1: return ModSounds.HURT_2;
            case 2: return ModSounds.HURT_3;
            default: return ModSounds.HURT_4;
        }
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.DEATH;
    }

    @Override
    protected void playStepSound(@Nonnull BlockPos pos, @Nonnull Block blockIn) {
        int rand = this.rand.nextInt(4);
        SoundEvent sound = ModSounds.STEPS_1;
        switch (rand) {
            case 1: sound = ModSounds.STEPS_2; break;
            case 2: sound = ModSounds.STEPS_3; break;
            case 3: sound = ModSounds.STEPS_4; break;
        }
        this.playSound(sound, 0.15F, 1.0F);
    }

    public void pickRoll(java.util.List<Roll> rolls) {
        if (rolls == null || rolls.isEmpty()) {
            currentRoll = Roll.STROLL;
            return;
        }
        currentRoll = rolls.get(this.rand.nextInt(rolls.size()));
    }

    public void reRoll() {
        Roll[] rolls = new Roll[]{Roll.CHASE, Roll.STARE, Roll.FLEE};
        currentRoll = rolls[this.rand.nextInt(rolls.length)];
    }

    @Override
    public void writeEntityToNBT(@Nonnull NBTTagCompound compound) {
        super.writeEntityToNBT(compound);
        compound.setInteger("CurrentRoll", currentRoll.rollValue);
        compound.setInteger("LifeTicks", lifeTicks);
    }

    @Override
    public void readEntityFromNBT(@Nonnull NBTTagCompound compound) {
        super.readEntityFromNBT(compound);
        if (compound.hasKey("CurrentRoll")) {
            currentRoll = Roll.fromValue(compound.getInteger("CurrentRoll"));
        }
        if (compound.hasKey("LifeTicks")) {
            lifeTicks = compound.getInteger("LifeTicks");
        }
    }
}

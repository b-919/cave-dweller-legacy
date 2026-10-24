package com.b919.cave_dweller_legacy.entity;

import com.b919.cave_dweller_legacy.common.ModSounds;
import com.b919.cave_dweller_legacy.common.config.ModConfig;
import com.b919.cave_dweller_legacy.entity.ai.EntityAICaveDwellerChase;
import com.b919.cave_dweller_legacy.entity.ai.EntityAICaveDwellerFlee;
import com.b919.cave_dweller_legacy.entity.ai.EntityAICaveDwellerGrief;
import com.b919.cave_dweller_legacy.entity.ai.EntityAICaveDwellerStare;
import com.b919.cave_dweller_legacy.entity.ai.EntityAICaveDwellerStroll;
import com.b919.cave_dweller_legacy.entity.ai.path.DwellerNodeType;
import com.b919.cave_dweller_legacy.entity.ai.path.PathNavigateDweller;
import mcp.MethodsReturnNonnullByDefault;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
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
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;

public class CaveDwellerEntity extends EntityCreature implements IAnimatable {
    private static final DataParameter<Byte> STATE = EntityDataManager.createKey(CaveDwellerEntity.class, DataSerializers.BYTE);
    private static final DataParameter<Byte> CRAWLING = EntityDataManager.createKey(CaveDwellerEntity.class, DataSerializers.BYTE);
    private static final DataParameter<Byte> CROUCHING = EntityDataManager.createKey(CaveDwellerEntity.class, DataSerializers.BYTE);
    private static final DataParameter<Byte> CLIMBING = EntityDataManager.createKey(CaveDwellerEntity.class, DataSerializers.BYTE);
    private static final DataParameter<Byte> SPOTTED = EntityDataManager.createKey(CaveDwellerEntity.class, DataSerializers.BYTE);
    private static final DataParameter<Byte> ATTACKING = EntityDataManager.createKey(CaveDwellerEntity.class, DataSerializers.BYTE);
    private static final Logger LOGGER = LogManager.getLogger("cave_dweller_legacy");
    private static final java.util.UUID POSTURE_SPEED_MODIFIER = java.util.UUID.fromString("71a4c925-a554-4a2d-b017-4651eb2f88e1");

    // Hitbox dimensions
    private static final float NORMAL_WIDTH = 0.6F;
    private static final float NORMAL_HEIGHT = 2.7F;
    private static final float CRAWL_HEIGHT = 0.5F;

    public Roll currentRoll = Roll.STROLL;
    private final AnimationFactory animationFactory = new AnimationFactory(this);
    private DwellerNodeType appliedPosture;
    private int attackAnimationTicks;
    public int lifeTicks = 0;
    public int chaseTicks = 0;

    public CaveDwellerEntity(World worldIn) {
        super(worldIn);
        this.setSize(NORMAL_WIDTH, NORMAL_HEIGHT);
        setRoll(Roll.STARE);
        this.stepHeight = 1.0F; // Can step up 1 block
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataManager.register(STATE, (byte) 0);
        this.dataManager.register(CRAWLING, (byte) 0);
        this.dataManager.register(CROUCHING, (byte) 0);
        this.dataManager.register(CLIMBING, (byte) 0);
        this.dataManager.register(SPOTTED, (byte) 0);
        this.dataManager.register(ATTACKING, (byte) 0);
    }

    public boolean isCrawling() {
        return this.dataManager.get(CRAWLING) == 1;
    }

    public void setCrawling(boolean crawling) {
        this.dataManager.set(CRAWLING, (byte) (crawling ? 1 : 0));
    }

    public boolean isCrouching() {
        return this.dataManager.get(CROUCHING) == 1;
    }

    private void setCrouching(boolean crouching) {
        this.dataManager.set(CROUCHING, (byte) (crouching ? 1 : 0));
    }

    public boolean isClimbing() {
        return this.dataManager.get(CLIMBING) == 1;
    }

    private void setClimbing(boolean climbing) {
        this.dataManager.set(CLIMBING, (byte) (climbing ? 1 : 0));
    }

    public boolean isSpotted() {
        return this.dataManager.get(SPOTTED) == 1;
    }

    private void setSpotted(boolean spotted) {
        this.dataManager.set(SPOTTED, (byte) (spotted ? 1 : 0));
    }

    private boolean isAttacking() {
        return this.dataManager.get(ATTACKING) == 1;
    }

    private void setAttacking(boolean attacking) {
        this.dataManager.set(ATTACKING, (byte) (attacking ? 1 : 0));
    }

    private Roll getSyncedRoll() {
        return Roll.fromValue(this.dataManager.get(STATE));
    }

    @Override
    public void registerControllers(AnimationData data) {
        data.addAnimationController(new AnimationController<>(this, "dweller", 2.0F, this::animationPredicate));
    }

    private PlayState animationPredicate(AnimationEvent<CaveDwellerEntity> event) {
        AnimationController<?> controller = event.getController();
        if (isAttacking()) {
            controller.setAnimation(new AnimationBuilder().playOnce("attack.swipe"));
        } else if (isClimbing()) {
            controller.setAnimation(new AnimationBuilder().loop("move.climb"));
        } else if (isCrawling()) {
            controller.setAnimation(new AnimationBuilder().loop("move.crawl"));
        } else if (isCrouching()) {
            controller.setAnimation(new AnimationBuilder().loop("move.crouch"));
        } else if (event.isMoving()) {
            controller.setAnimation(new AnimationBuilder().loop(getSyncedRoll() == Roll.CHASE ? "move.run" : "move.walk"));
        } else if (isSpotted()) {
            controller.setAnimation(new AnimationBuilder().loop("idle.alert"));
        } else {
            controller.setAnimation(new AnimationBuilder().loop("idle.stand"));
        }
        return PlayState.CONTINUE;
    }

    @Override
    public AnimationFactory getFactory() {
        return this.animationFactory;
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        this.getAttributeMap().registerAttribute(SharedMonsterAttributes.ATTACK_DAMAGE);
        this.getAttributeMap().registerAttribute(SharedMonsterAttributes.ATTACK_SPEED);
        this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(ModConfig.behavior.maxHealth);
        this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(ModConfig.behavior.movementSpeed);
        this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(ModConfig.behavior.attackDamage);
        this.getEntityAttribute(SharedMonsterAttributes.ATTACK_SPEED).setBaseValue(0.35D);
        this.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(ModConfig.behavior.followRange);
    }

    @Override @MethodsReturnNonnullByDefault
    protected PathNavigate createNavigator(@Nonnull World worldIn) {
        return new PathNavigateDweller(this, worldIn);
    }

    @Override
    protected void initEntityAI() {
        this.tasks.addTask(0, new EntityAICaveDwellerGrief(this));
        this.tasks.addTask(1, new EntityAICaveDwellerChase(this));
        this.tasks.addTask(2, new EntityAICaveDwellerStare(this));
        this.tasks.addTask(3, new EntityAICaveDwellerFlee(this, ModConfig.behavior.fleeSpeed));
        this.tasks.addTask(4, new EntityAICaveDwellerStroll(this));
        this.targetTasks.addTask(1, new EntityAIHurtByTarget(this, true));
        this.targetTasks.addTask(2, new EntityAINearestAttackableTarget<>(this, EntityPlayer.class, true, false));
    }

    private void updateCrawlState() {
        if (this.getNavigator() instanceof PathNavigateDweller) {
            DwellerNodeType nodeType = ((PathNavigateDweller) this.getNavigator()).getDesiredNodeType();
            if (nodeType != null) {
                applyNavigationPosture(nodeType);
                return;
            }
        }

        if (!ModConfig.behavior.canCrawl) {
            setCrawling(false);
            setCrouching(false);
            applyPostureSpeed(DwellerNodeType.WALK);
            if (this.height != NORMAL_HEIGHT) this.setSize(NORMAL_WIDTH, NORMAL_HEIGHT);
            return;
        }

        boolean shouldCrawl = false;
        if (this.onGround) {
            BlockPos headPos = new BlockPos(this.posX, this.posY + 1.0, this.posZ);
            BlockPos headPos2 = new BlockPos(this.posX, this.posY + 1.5, this.posZ);
            IBlockState state1 = this.world.getBlockState(headPos);
            IBlockState state2 = this.world.getBlockState(headPos2);
            
            boolean blocked1 = !state1.getBlock().isAir(state1, this.world, headPos) && 
                              state1.getCollisionBoundingBox(this.world, headPos) != Block.NULL_AABB;
            boolean blocked2 = !state2.getBlock().isAir(state2, this.world, headPos2) && 
                              state2.getCollisionBoundingBox(this.world, headPos2) != Block.NULL_AABB;
            
            shouldCrawl = blocked1 || blocked2;
        }

        boolean wasCrawling = isCrawling();
        setCrawling(shouldCrawl);
        setCrouching(false);
        applyPostureSpeed(shouldCrawl ? DwellerNodeType.CRAWL : DwellerNodeType.WALK);
        float desiredHeight = shouldCrawl ? CRAWL_HEIGHT : NORMAL_HEIGHT;
        if (this.height != desiredHeight) this.setSize(NORMAL_WIDTH, desiredHeight);
        if (shouldCrawl != wasCrawling) {
            if (shouldCrawl) {
                if (ModConfig.general.debugMode) {
                    System.out.println("[CAVE_DWELLER_DEBUG] Entered crawl mode at " + getPosition());
                }
            } else {
                if (ModConfig.general.debugMode) {
                    System.out.println("[CAVE_DWELLER_DEBUG] Exited crawl mode at " + getPosition());
                }
            }
        }
    }

    @Override
    public boolean isOnLadder() {
        if (this.getNavigator() instanceof PathNavigateDweller
                && ((PathNavigateDweller) this.getNavigator()).isClimbingUp()) {
            return true;
        }
        return isClimbing() || super.isOnLadder();
    }

    public void applyNavigationPosture(@Nonnull DwellerNodeType nodeType) {
        float desiredHeight = nodeType.getHeight();
        boolean crawling = nodeType == DwellerNodeType.CRAWL;
        boolean crouching = nodeType == DwellerNodeType.CROUCH;
        if (isCrawling() != crawling) {
            setCrawling(crawling);
        }
        if (isCrouching() != crouching) {
            setCrouching(crouching);
        }
        applyPostureSpeed(nodeType);
        if (this.height != desiredHeight) {
            this.setSize(NORMAL_WIDTH, desiredHeight);
        }
    }

    private void applyPostureSpeed(DwellerNodeType nodeType) {
        if (this.appliedPosture == nodeType) {
            return;
        }
        this.appliedPosture = nodeType;
        double speedMultiplier = nodeType == DwellerNodeType.CRAWL ? 0.35D
                : nodeType == DwellerNodeType.CROUCH ? 0.6D : 0.85D;
        IAttributeInstance movementSpeed = this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);
        movementSpeed.removeModifier(POSTURE_SPEED_MODIFIER);
        movementSpeed.applyModifier(new AttributeModifier(POSTURE_SPEED_MODIFIER,
                "Cave Dweller posture speed", speedMultiplier - 1.0D, 2));
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (world.isRemote) {
            return;
        }
        
        updateCrawlState();

        if (this.getNavigator() instanceof PathNavigateDweller) {
            setClimbing(((PathNavigateDweller) this.getNavigator()).isClimbingUp());
        } else {
            setClimbing(false);
        }
        setSpotted(getAttackTarget() != null && !isInvisible());
        if (attackAnimationTicks > 0 && --attackAnimationTicks == 0) {
            setAttacking(false);
        }

        
        if (this.getAttackTarget() == null && this.ticksExisted > 40) {
            if (this.rand.nextInt(100) == 0) {
                net.minecraft.entity.player.EntityPlayer nearest = this.world.getClosestPlayerToEntity(this, 32.0D);
                if (nearest != null) {
                    this.setAttackTarget(nearest);
                    setRoll(Roll.STARE);
                    this.setInvisible(false);
                    this.playSpottedSound();
                }
            }
        }
        if (getAttackTarget() != null && this.isInvisible()) {
            this.setInvisible(false);
            playSpottedSound();
            if (ModConfig.general.debugMode) {
                System.out.println("[CAVE_DWELLER_DEBUG] CaveDweller revealed (has target): " + getPosition());
            }
        }
        lifeTicks++;
        if (currentRoll == Roll.CHASE) {
            chaseTicks++;
        } else {
            chaseTicks = 0;
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
            setRoll(Roll.FLEE);
        }
    }

    public void disappear() {
        playDisappearSound();
        if (ModConfig.general.debugMode) {
            System.out.println("[CAVE_DWELLER_DEBUG] CaveDweller disappearing");
        }
        setDead();
    }

    @Override
    public boolean attackEntityAsMob(@Nonnull Entity entityIn) {
        if (ModConfig.general.debugMode) {
            System.out.println("[CAVE_DWELLER_DEBUG] attackEntityAsMob called on " + entityIn.getName());
        }
        
        if (!(entityIn instanceof EntityPlayer)) {
            return false;
        }
        EntityPlayer player = (EntityPlayer) entityIn;
        if (player.isCreative() || player.isSpectator()) {
            if (ModConfig.general.debugMode) {
                System.out.println("[CAVE_DWELLER_DEBUG] Target is invulnerable, skipping attack");
            }
            return false;
        }
        double dist = this.getDistanceSq(player);
        double reach = ModConfig.behavior.attackReach;
        if (dist > reach * reach) {
            if (ModConfig.general.debugMode) {
                System.out.println("[CAVE_DWELLER_DEBUG] Target out of reach: " + Math.sqrt(dist) + " > " + reach);
            }
            return false;
        }
        float damage = (float) this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue();
        if (ModConfig.general.debugMode) {
            System.out.println("[CAVE_DWELLER_DEBUG] Direct attack on " + player.getName() + " for " + damage + " damage at distance " + Math.sqrt(dist));
        }
        boolean attacked = player.attackEntityFrom(DamageSource.causeMobDamage(this), damage);
        if (attacked) {
            attackAnimationTicks = 8;
            setAttacking(true);
        }
        this.swingArm(net.minecraft.util.EnumHand.MAIN_HAND);
        return attacked;
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

    public void setRoll(Roll roll) {
        if (roll == null) return;
        if (this.currentRoll != roll) {
            if (ModConfig.general.debugMode) {
                LOGGER.info("[DEBUG] CaveDweller roll changed from {} to {} at {}", this.currentRoll, roll, getPosition());
            }
            this.currentRoll = roll;
        }
        this.dataManager.set(STATE, (byte) roll.rollValue);
    }

    public void pickRoll(java.util.List<Roll> rolls) {
        if (rolls == null || rolls.isEmpty()) {
            setRoll(Roll.STROLL);
            return;
        }
        setRoll(rolls.get(this.rand.nextInt(rolls.size())));
    }

    public void reRoll() {
        Roll[] rolls = new Roll[]{Roll.CHASE, Roll.STARE, Roll.FLEE};
        setRoll(rolls[this.rand.nextInt(rolls.length)]);
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
            setRoll(Roll.fromValue(compound.getInteger("CurrentRoll")));
        }
        if (compound.hasKey("LifeTicks")) {
            lifeTicks = compound.getInteger("LifeTicks");
        }
    }
}

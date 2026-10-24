package com.b919.cave_dweller_legacy.event;

import com.b919.cave_dweller_legacy.entity.CaveDwellerEntity;
import com.b919.cave_dweller_legacy.entity.ai.path.PathNavigateDweller;
import com.b919.cave_dweller_legacy.util.SpawnDirector;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class WorldEvents {
    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.world.isRemote) {
            SpawnDirector.tick(event.world);
        }
    }

    @SubscribeEvent
    public void onLivingAttack(LivingAttackEvent event) {
        EntityLivingBase entity = event.getEntityLiving();
        if (!(entity instanceof CaveDwellerEntity)) {
            return;
        }
        DamageSource source = event.getSource();
        if (source == DamageSource.DROWN || source == DamageSource.OUT_OF_WORLD ||
                source == DamageSource.IN_WALL || source == DamageSource.FALL) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        invalidateNearbyDwellerPaths(event.getWorld(), event.getPos());
    }

    @SubscribeEvent
    public void onBlockBroken(BlockEvent.BreakEvent event) {
        invalidateNearbyDwellerPaths(event.getWorld(), event.getPos());
    }

    private void invalidateNearbyDwellerPaths(net.minecraft.world.World world, BlockPos pos) {
        if (world.isRemote) {
            return;
        }

        for (Entity entity : world.loadedEntityList) {
            if (entity instanceof CaveDwellerEntity) {
                CaveDwellerEntity dweller = (CaveDwellerEntity) entity;
                if (dweller.getNavigator() instanceof PathNavigateDweller) {
                    ((PathNavigateDweller) dweller.getNavigator()).onBlockChanged(pos);
                }
            }
        }
    }
}

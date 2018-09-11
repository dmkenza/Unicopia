package com.minelittlepony.unicopia.power;

import org.lwjgl.input.Keyboard;

import com.minelittlepony.unicopia.Race;
import com.minelittlepony.unicopia.Unicopia;
import com.minelittlepony.unicopia.player.IPlayer;
import com.minelittlepony.unicopia.power.data.Location;
import com.minelittlepony.util.vector.VecHelper;

import net.minecraft.block.Block;
import net.minecraft.block.BlockFence;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockWall;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

public class PowerTeleport implements IPower<Location> {

    @Override
    public String getKeyName() {
        return "unicopia.power.teleport";
    }

    @Override
    public int getKeyCode() {
        return Keyboard.KEY_O;
    }

    @Override
    public int getWarmupTime(IPlayer player) {
        return 20;
    }

    @Override
    public int getCooldownTime(IPlayer player) {
        return 50;
    }

    @Override
    public boolean canUse(Race playerSpecies) {
        return playerSpecies.canCast();
    }

    @Override
    public Location tryActivate(EntityPlayer player, World w) {
        RayTraceResult ray = VecHelper.getObjectMouseOver(player, 100, 1);

        if (ray != null && ray.typeOfHit != RayTraceResult.Type.MISS) {
            BlockPos pos;

            if (ray.typeOfHit == RayTraceResult.Type.ENTITY) {
                pos = new BlockPos(ray.entityHit);
            } else {
                pos = ray.getBlockPos();
            }

            boolean airAbove = enterable(w, pos.up()) && enterable(w, pos.up(2));
            if (exception(w, pos)) {
                EnumFacing sideHit = ray.sideHit;

                if (player.isSneaking()) {
                    sideHit = sideHit.getOpposite();
                }

                pos = pos.offset(sideHit);
            }

            if (enterable(w, pos.down())) {
                pos = pos.down();

                if (enterable(w, pos.down())) {
                    if (airAbove) {
                        pos = new BlockPos(
                                ray.getBlockPos().getX(),
                                pos.getY() + 2,
                                ray.getBlockPos().getZ());
                    } else {
                        return null;
                    }
                }
            }

            if ((!enterable(w, pos) && exception(w, pos))
             || (!enterable(w, pos.up()) && exception(w, pos.up()))) {
                return null;
            }

            return new Location(pos.getX(), pos.getY(), pos.getZ());
        }

        return null;
    }



    @Override
    public Class<Location> getPackageType() {
        return Location.class;
    }

    @Override
    public void apply(EntityPlayer player, Location data) {
        player.world.playSound(player.posX, player.posY, player.posZ, SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, 1, 1, true);

        double distance = player.getDistance(data.x, data.y, data.z) / 10;
        player.dismountRidingEntity();

        player.setPositionAndUpdate(data.x + (player.posX - Math.floor(player.posX)), data.y, data.z + (player.posZ - Math.floor(player.posZ)));
        IPower.takeFromPlayer(player, distance);
        player.fallDistance /= distance;
        player.world.playSound(data.x, data.y, data.z, SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, 1, 1, true);
    }

    private boolean enterable(World w, BlockPos pos) {
        IBlockState state = w.getBlockState(pos);

        Block block = state.getBlock();

        return w.isAirBlock(pos)
                || block.isReplaceable(w, pos)
                || (block instanceof BlockLeaves);
    }

    private boolean exception(World w, BlockPos pos) {
        IBlockState state = w.getBlockState(pos);

        Block c = state.getBlock();
        return state.isSideSolid(w, pos, EnumFacing.UP)
                || state.getMaterial().isLiquid()
                || (c instanceof BlockWall)
                || (c instanceof BlockFence)
                || (c instanceof BlockLeaves);
    }

    @Override
    public void preApply(EntityPlayer player) {
        postApply(player);
    }

    @Override
    public void postApply(EntityPlayer player) {
        IPower.spawnParticles(Unicopia.MAGIC_PARTICLE, player, 1);
    }

}
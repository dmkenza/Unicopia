package com.minelittlepony.unicopia.item;

import java.util.function.Predicate;

import net.minecraft.block.Block;
import net.minecraft.block.BlockSlab;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemSlab;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class UItemSlab extends ItemSlab {

    private final Predicate<EntityPlayer> abilityTest;

    public UItemSlab(Block block, BlockSlab singleSlab, BlockSlab doubleSlab, Predicate<EntityPlayer> abilityTest) {
        super(block, singleSlab, doubleSlab);

        this.abilityTest = abilityTest;
    }

    @Override
    public boolean canPlaceBlockOnSide(World worldIn, BlockPos pos, EnumFacing side, EntityPlayer player, ItemStack stack) {
        if (!abilityTest.test(player)) {
            return player.capabilities.isCreativeMode;
        }

        return super.canPlaceBlockOnSide(worldIn, pos, side, player, stack);
    }

}
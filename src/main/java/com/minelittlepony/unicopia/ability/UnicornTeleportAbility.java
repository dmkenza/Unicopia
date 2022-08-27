package com.minelittlepony.unicopia.ability;

import com.minelittlepony.unicopia.Race;
import com.minelittlepony.unicopia.USounds;
import com.minelittlepony.unicopia.ability.data.Hit;
import com.minelittlepony.unicopia.ability.data.Pos;
import com.minelittlepony.unicopia.ability.magic.Caster;
import com.minelittlepony.unicopia.entity.player.Pony;
import com.minelittlepony.unicopia.particle.MagicParticleEffect;
import com.minelittlepony.unicopia.util.RayTraceHelper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.FenceBlock;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.WallBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityPassengersSetS2CPacket;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Unicorn teleport ability
 */
public class UnicornTeleportAbility implements Ability<Pos> {

    @Override
    public Identifier getIcon(Pony player, boolean swap) {
        Identifier id = Abilities.REGISTRY.getId(this);
        return new Identifier(id.getNamespace(), "textures/gui/ability/" + id.getPath() + (swap ? "_far" : "_near") + ".png");
    }

    @Override
    public int getWarmupTime(Pony player) {
        return 5;
    }

    @Override
    public int getCooldownTime(Pony player) {
        return 20;
    }

    @Override
    public boolean canUse(Race race) {
        return race.canCast();
    }

    @Override
    public double getCostEstimate(Pony player) {
        Pos pos = tryActivate(player);

        if (pos == null) {
            return 0;
        }
        return pos.distanceTo(player) / 10;
    }

    @Override
    public Pos tryActivate(Pony player) {
        int maxDistance = player.getMaster().isCreative() ? 1000 : 30;
        HitResult ray = RayTraceHelper.doTrace(player.getMaster(), maxDistance, 1, EntityPredicates.CAN_COLLIDE).getResult();

        World w = player.getReferenceWorld();

        if (ray.getType() == HitResult.Type.MISS) {
            return null;
        }

        BlockPos pos;

        if (ray.getType() == HitResult.Type.ENTITY) {
            pos = new BlockPos(ray.getPos());
        } else {
            pos = ((BlockHitResult)ray).getBlockPos();
        }

        boolean airAbove = enterable(w, pos.up()) && enterable(w, pos.up(2));

        if (exception(w, pos, player.getMaster()) && ray.getType() == HitResult.Type.BLOCK) {
            Direction sideHit = ((BlockHitResult)ray).getSide();

            if (player.getMaster().isSneaking()) {
                sideHit = sideHit.getOpposite();
            }

            pos = pos.offset(sideHit);
        }

        if (enterable(w, pos.down())) {
            pos = pos.down();

            if (enterable(w, pos.down())) {
                if (!airAbove) {
                    return null;
                }

                pos = new BlockPos(
                        ray.getPos().getX(),
                        pos.getY() + 2,
                        ray.getPos().getZ());
            }
        }

        if ((!enterable(w, pos) && exception(w, pos, player.getMaster()))
         || (!enterable(w, pos.up()) && exception(w, pos.up(), player.getMaster()))) {
            return null;
        }

        return new Pos(pos.getX(), pos.getY(), pos.getZ());
    }

    @Override
    public Hit.Serializer<Pos> getSerializer() {
        return Pos.SERIALIZER;
    }

    @Override
    public void apply(Pony iplayer, Pos data) {
        teleport(iplayer, iplayer, data);
    }

    protected void teleport(Pony teleporter, Caster<?> teleportee, Pos destination) {

        LivingEntity player = teleportee.getMaster();

        if (player == null) {
            return;
        }

        teleportee.getReferenceWorld().playSound(null, teleportee.getOrigin(), USounds.ENTITY_PLAYER_UNICORN_TELEPORT, SoundCategory.PLAYERS, 1, 1);

        double distance = destination.distanceTo(teleportee) / 10;

        if (player.hasVehicle()) {
            Entity mount = player.getVehicle();

            player.stopRiding();

            if (mount instanceof ServerPlayerEntity) {
                ((ServerPlayerEntity)player).networkHandler.sendPacket(new EntityPassengersSetS2CPacket(player));
            }
        }

        Vec3d offset = teleportee.getOriginVector().subtract(teleporter.getOriginVector());

        player.teleport(
                destination.x + offset.x + (player.getX() - Math.floor(player.getX())),
                destination.y + offset.y,
                destination.z + offset.z + (player.getZ() - Math.floor(player.getZ())));
        teleporter.subtractEnergyCost(distance);

        player.fallDistance /= distance;

        player.world.playSound(null, destination.pos(), USounds.ENTITY_PLAYER_UNICORN_TELEPORT, SoundCategory.PLAYERS, 1, 1);
    }

    private boolean enterable(World w, BlockPos pos) {
        BlockState state = w.getBlockState(pos);

        Block block = state.getBlock();

        return w.isAir(pos)
                || !state.isOpaque()
                || (block instanceof LeavesBlock);
    }

    private boolean exception(World w, BlockPos pos, PlayerEntity player) {
        BlockState state = w.getBlockState(pos);

        Block c = state.getBlock();
        return state.hasSolidTopSurface(w, pos, player)
                || state.getMaterial().isLiquid()
                || (c instanceof WallBlock)
                || (c instanceof FenceBlock)
                || (c instanceof LeavesBlock);
    }

    @Override
    public void preApply(Pony player, AbilitySlot slot) {
        player.getMagicalReserves().getExertion().add(30);
        player.spawnParticles(MagicParticleEffect.UNICORN, 5);
    }

    @Override
    public void postApply(Pony player, AbilitySlot slot) {
        player.spawnParticles(MagicParticleEffect.UNICORN, 5);
    }
}

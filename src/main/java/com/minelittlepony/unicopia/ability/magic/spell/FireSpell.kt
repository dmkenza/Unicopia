package com.minelittlepony.unicopia.ability.magic.spell

import com.minelittlepony.unicopia.ability.magic.Thrown
import com.minelittlepony.unicopia.ability.magic.Attached
import com.minelittlepony.unicopia.projectile.MagicProjectileEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.block.BlockState
import net.minecraft.world.explosion.Explosion.DestructionType
import com.minelittlepony.unicopia.util.PosHelper
import net.minecraft.util.math.Vec3d
import net.minecraft.particle.ParticleTypes
import net.minecraft.world.World
import net.minecraft.block.Blocks
import net.minecraft.tag.BlockTags
import com.minelittlepony.unicopia.block.state.StateMaps
import com.minelittlepony.unicopia.util.VecHelper
import net.minecraft.entity.player.PlayerEntity
import com.minelittlepony.unicopia.EquinePredicates
import com.minelittlepony.unicopia.ability.magic.Caster
import net.minecraft.entity.ItemEntity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.damage.DamageSource
import com.minelittlepony.unicopia.util.MagicalDamageSource
import net.minecraft.block.RedstoneWireBlock
import net.minecraft.sound.SoundEvents
import net.minecraft.sound.SoundCategory
import com.minelittlepony.unicopia.particle.ParticleUtils
import com.minelittlepony.unicopia.util.shape.Shape
import com.minelittlepony.unicopia.util.shape.Sphere
import net.minecraft.entity.Entity

/**
 * Simple fire spell that triggers an effect when used on a block.
 */
open class FireSpell(type: SpellType<*>?) : AbstractSpell(type), Thrown, Attached {

    internal var EFFECT_RADIUS = 1.0
    internal val EFFECT_RANGE: Shape = Sphere(false, EFFECT_RADIUS)

    override fun onImpact(projectile: MagicProjectileEntity, pos: BlockPos, state: BlockState) {
        if (!projectile.isClient) {
            projectile.getWorld().createExplosion(
                projectile.master,
                pos.x.toDouble(),
                pos.y.toDouble(),
                pos.z.toDouble(),
                2f,
                DestructionType.DESTROY
            )
        }
    }

    override fun onThrownTick(projectile: MagicProjectileEntity): Boolean {
        return onBodyTick(projectile)
    }

    override fun onBodyTick(source: Caster<*>): Boolean {
        if (source.isClient) {
            generateParticles(source)
        }
        return (PosHelper.getAllInRegionMutable(source.origin, EFFECT_RANGE).reduce(false, { r: Boolean?, i: BlockPos ->
            applyBlocks(source.world, i)
        }) { a: Boolean, b: Boolean -> a || b }
                || applyEntities(null, source.world, source.originVector))
    }

    protected open fun generateParticles(source: Caster<*>) {
        source.spawnParticles(EFFECT_RANGE, (1 + source.level.get()) * 6) { pos: Vec3d? ->
            source.addParticle(
                ParticleTypes.LARGE_SMOKE,
                pos,
                Vec3d.ZERO
            )
        }
    }

    protected open fun applyBlocks(world: World, pos: BlockPos): Boolean {
        val state = world.getBlockState(pos)
        if (!state.isAir) {
            if (state.isOf(Blocks.NETHERRACK)) {
                if (world.isAir(pos.up())) {
                    if (world.random.nextInt(300) == 0) {
                        world.setBlockState(pos.up(), Blocks.FIRE.defaultState)
                    }
                    return true
                }
            } else if (state.isOf(Blocks.REDSTONE_WIRE)) {
                val power = if (world.random.nextInt(5) == 3) 15 else 3
                sendPower(world, pos, power, 3, 0)
                return true
            } else if (state.isIn(BlockTags.SAND) && world.random.nextInt(10) == 0) {
                if (isSurroundedBySand(world, pos)) {
                    world.setBlockState(pos, Blocks.GLASS.defaultState)
                    playEffect(world, pos)
                    return true
                }
            } else if (state.isIn(BlockTags.LEAVES)) {
                if (world.isAir(pos.up())) {
                    world.setBlockState(pos.up(), Blocks.FIRE.defaultState)
                    playEffect(world, pos)
                    return true
                }
            } else if (StateMaps.FIRE_AFFECTED.convert(world, pos)) {
                playEffect(world, pos)
                return true
            }
        }
        return false
    }

    protected open fun applyEntities(owner: Entity?, world: World, pos: Vec3d?): Boolean {
        return !VecHelper.findInRange(owner, world, pos, EFFECT_RADIUS) { i: Entity ->
            applyEntitySingle(
                owner,
                world,
                i
            )
        }
            .isEmpty()
    }

    protected fun applyEntitySingle(owner: Entity?, world: World, e: Entity): Boolean {
        if ((e != owner ||
                    owner is PlayerEntity && !EquinePredicates.PLAYER_UNICORN.test(owner)) && e !is ItemEntity
            && e !is Caster<*>
        ) {
            e.setOnFireFor(60)
            e.damage(getDamageCause(e, owner as LivingEntity?), 0.1f)
            playEffect(world, e.blockPos)
            return true
        }
        return false
    }

    protected open fun getDamageCause(target: Entity?, attacker: LivingEntity?): DamageSource? {
        return MagicalDamageSource.create("fire", attacker)
    }

    /**
     * Transmits power to a piece of redstone
     */
    internal fun sendPower(w: World, pos: BlockPos, power: Int, max: Int, i: Int) {
        var i = i
        val state = w.getBlockState(pos)
        val id = state.block
        if (i < max && id === Blocks.REDSTONE_WIRE) {
            i++
            w.setBlockState(pos, state.with(RedstoneWireBlock.POWER, power))
            sendPower(w, pos.up(), power, max, i)
            sendPower(w, pos.down(), power, max, i)
            sendPower(w, pos.north(), power, max, i)
            sendPower(w, pos.south(), power, max, i)
            sendPower(w, pos.east(), power, max, i)
            sendPower(w, pos.west(), power, max, i)
        }
    }

    protected fun playEffect(world: World, pos: BlockPos) {
        val x = pos.x
        val y = pos.y
        val z = pos.z
        world.playSound(
            null,
            pos,
            SoundEvents.BLOCK_FURNACE_FIRE_CRACKLE,
            SoundCategory.AMBIENT,
            0.5f,
            2.6f + (world.random.nextFloat() - world.random.nextFloat()) * 0.8f
        )
        for (i in 0..7) {
            ParticleUtils.spawnParticle(
                ParticleTypes.LARGE_SMOKE, world, Vec3d(
                    x + Math.random(),
                    y + Math.random(),
                    z + Math.random()
                ), Vec3d.ZERO
            )
        }
    }

    companion object {
        fun isSurroundedBySand(w: World, pos: BlockPos): Boolean {
            return isSand(w, pos.up()) && isSand(w, pos.down()) &&
                    isSand(w, pos.north()) && isSand(w, pos.south()) &&
                    isSand(w, pos.east()) && isSand(w, pos.west())
        }

        fun isSand(world: World, pos: BlockPos?): Boolean {
            val id = world.getBlockState(pos).block
            return id === Blocks.SAND || id === Blocks.GLASS
        }
    }
}
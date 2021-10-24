package kenza

import com.minelittlepony.unicopia.Affinity
import com.minelittlepony.unicopia.ability.magic.Caster
import com.minelittlepony.unicopia.ability.magic.spell.FireSpell
import com.minelittlepony.unicopia.ability.magic.spell.IceSpell
import com.minelittlepony.unicopia.ability.magic.spell.SpellType
import com.minelittlepony.unicopia.block.state.StateMaps
import com.minelittlepony.unicopia.particle.MagicParticleEffect
import com.minelittlepony.unicopia.projectile.MagicProjectileEntity
import com.minelittlepony.unicopia.util.PosHelper
import com.minelittlepony.unicopia.util.VecHelper
import kenza.Colors.FLAME_COLOR
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.entity.Entity
import net.minecraft.particle.ParticleTypes
import net.minecraft.tag.BlockTags
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World

class KFireSpell(type: SpellType<*>?) : FireSpell(type) {


    override fun onImpact(projectile: MagicProjectileEntity, pos: BlockPos, state: BlockState) {
//        super.onImpact(projectile, pos, state)
    }

    override fun onBodyTick(source: Caster<*>): Boolean {

        if (source.isClient) {
            generateParticles(source)
        }
        (PosHelper.getAllInRegionMutable(source.origin, EFFECT_RANGE).reduce(false, { r: Boolean?, i: BlockPos ->
            applyBlocks(source.world, i)
        }) { a: Boolean, b: Boolean -> a || b }
                || applyEntities(source.master, source.world, source.originVector))

        return true
    }


    override fun applyEntities(owner: Entity?, world: World, pos: Vec3d?): Boolean {
        return !VecHelper.findInRange(owner, world, pos, EFFECT_RADIUS) { i: Entity -> applyEntitySingle(owner, world, i) }
            .isEmpty()
    }

    override fun generateParticles(source: Caster<*>) {
        source.spawnParticles(EFFECT_RANGE, (1 + source.level.get()) * 2 ) { pos: Vec3d? ->
            source.addParticle(
                MagicParticleEffect.UNICORN,
                pos,
                Vec3d.ZERO
            )
        }
    }

    override fun applyBlocks(world: World, pos: BlockPos): Boolean {
        val state = world.getBlockState(pos)
//        if (!state.isAir) {
//            if (state.isOf(Blocks.NETHERRACK)) {
//                if (world.isAir(pos.up())) {
//                    if (world.random.nextInt(300) == 0) {
//                        world.setBlockState(pos.up(), Blocks.FIRE.defaultState)
//                    }
//                    return true
//                }
//            } else if (state.isOf(Blocks.REDSTONE_WIRE)) {
//                val power = if (world.random.nextInt(5) == 3) 15 else 3
//                sendPower(world, pos, power, 3, 0)
//                return true
//            } else if (state.isIn(BlockTags.SAND) && world.random.nextInt(10) == 0) {
//                if (isSurroundedBySand(world, pos)) {
//                    world.setBlockState(pos, Blocks.GLASS.defaultState)
//                    playEffect(world, pos)
//                    return true
//                }
//            } else if (state.isIn(BlockTags.LEAVES)) {
//                if (world.isAir(pos.up())) {
//                    world.setBlockState(pos.up(), Blocks.FIRE.defaultState)
//                    playEffect(world, pos)
//                    return true
//                }
//            } else if (StateMaps.FIRE_AFFECTED.convert(world, pos)) {
//                playEffect(world, pos)
//                return true
//            }
//        }
        return false
    }



    companion object {
        val KFire = SpellType.register("k_fire", Affinity.GOOD, FLAME_COLOR, true) { type: SpellType<KFireSpell?>? ->
            KFireSpell(type)
        }
    }
}
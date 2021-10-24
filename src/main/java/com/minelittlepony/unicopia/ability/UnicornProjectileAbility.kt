package com.minelittlepony.unicopia.ability

import com.minelittlepony.unicopia.Race
import com.minelittlepony.unicopia.ability.data.Hit
import com.minelittlepony.unicopia.ability.magic.Spell
import com.minelittlepony.unicopia.ability.magic.Thrown
import com.minelittlepony.unicopia.ability.magic.spell.SpellType
import com.minelittlepony.unicopia.entity.player.Pony
import com.minelittlepony.unicopia.item.GemstoneItem
import com.minelittlepony.unicopia.particle.MagicParticleEffect
import kenza.KFireSpell
import kenza.KFireSpell.Companion.KFire
import net.minecraft.item.ItemStack
import net.minecraft.util.ActionResult
import net.minecraft.util.Identifier
import net.minecraft.util.TypedActionResult

/**
 * A magic casting ability for unicorns.
 * (only shields for now)
 */
class UnicornProjectileAbility : Ability<Hit?> {
    /**
     * The icon representing this ability on the UI and HUD.
     */
    override fun getIcon(player: Pony, swap: Boolean): Identifier {
        val id = Abilities.REGISTRY.getId(this)
        return Identifier(
            id!!.namespace,
            "textures/gui/ability/" + id.path + (if (swap) "_focused" else "_unfocused") + ".png"
        )
    }

    override fun getWarmupTime(player: Pony): Int {
        return 4
    }

    override fun getCooldownTime(player: Pony): Int {
        return 0
    }

    override fun canUse(race: Race): Boolean {
        return race.canCast()
    }

    override fun tryActivate(player: Pony): Hit? {
        return Hit.of(getNewSpell(player).result != ActionResult.FAIL)
    }

    override fun getSerializer(): Hit.Serializer<Hit?>? {
        return Hit.SERIALIZER
    }

    override fun getCostEstimate(player: Pony): Double {
        return 7.0
    }

    override fun apply(player: Pony, data: Hit?) {
        val thrown = getNewSpell(player)
        if (thrown.result != ActionResult.FAIL) {
            var spell = thrown.value
            if (spell == null) {
                spell = SpellType.VORTEX
            }
            player.subtractEnergyCost(getCostEstimate(player))
            val createdSpell = spell!!.create()
            (createdSpell as Thrown?)!!.toss(player)
        }
    }

    private fun getNewSpell(player: Pony): TypedActionResult<SpellType<*>?> {
        val list = player.master.itemsHand.filter { stack ->
            GemstoneItem.isEnchanted(stack)
        }.map { stack: ItemStack? ->
            GemstoneItem.consumeSpell(stack, player.master, null) { obj: SpellType<*> ->
                obj.mayThrow()
            }
        }

        return list.firstOrNull() ?: TypedActionResult.pass(KFire)
    }

    override fun preApply(player: Pony, slot: AbilitySlot) {
        player.magicalReserves.exhaustion.multiply(3.3f)
        player.spawnParticles(MagicParticleEffect.UNICORN, 10)
    }

    override fun postApply(player: Pony, slot: AbilitySlot) {
        player.spawnParticles(MagicParticleEffect.UNICORN, 5)
    }
}
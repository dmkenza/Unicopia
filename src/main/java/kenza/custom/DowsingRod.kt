package kenza.custom

import com.mojang.brigadier.LiteralMessage
import net.minecraft.block.Block
import net.minecraft.block.Blocks
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemUsageContext
import net.minecraft.text.LiteralText
import net.minecraft.util.ActionResult
import net.minecraft.util.math.BlockPos

class DowsingRod(settings: Settings?) : Item(settings) {


    override fun useOnBlock(context: ItemUsageContext): ActionResult {
        context.apply {

            if (world.isClient) {
                blockPos
                player

                var foundBlock = false

                for (x in 0..blockPos.y) {

                    val blockBelow = world.getBlockState(blockPos.down(x)).block

                    if (blockBelow.isValuableBlock()) {
                        sendMessageAboutOreToPlayer(blockBelow, blockPos.add(0, -x, 0), player)
                        foundBlock = true
                        break
                    }
                }

                if (!foundBlock) {
                    player?.sendMessage(LiteralText("Didn't find any blocks"), false)
                }

                context.stack.damage(1, context.player) { player ->
                    player?.sendToolBreakStatus(player.activeHand)
                }

            }

            return super.useOnBlock(context)
        }
    }


    fun Block.isValuableBlock(): Boolean {
        return this in listOf(
            Blocks.COAL_ORE,
            Blocks.COPPER_ORE,
            Blocks.DIAMOND_ORE,
        )
    }

    private fun sendMessageAboutOreToPlayer(blockFound: Block, pos: BlockPos, player: PlayerEntity?) {
        player?.sendMessage(
            LiteralText("Found " + blockFound.asItem().name + " at x = " + pos.x + " y = " + pos.y + "  Z = " + pos.z),
            false
        )
    }
}
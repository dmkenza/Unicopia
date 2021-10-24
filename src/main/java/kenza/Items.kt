package kenza

import com.minelittlepony.unicopia.Unicopia
import kenza.custom.DowsingRod
import net.fabricmc.fabric.api.item.v1.FabricItemSettings
import net.minecraft.item.FoodComponent
import net.minecraft.item.Item
import net.minecraft.item.ItemGroup
import net.minecraft.util.Identifier
import net.minecraft.util.registry.Registry

object Items {

    //    val MAGIC_BOOK: Item = registerItem("magic_book", Item(FabricItemSettings().group(ItemGroup.MISC)))
    val MAGIC_BOOK: Item = registerItem(
        "magic_book", Item(
            FabricItemSettings().food(
                FoodComponent.Builder().hunger(2).saturationModifier(2.2f).build()
            ).group(ItemGroup.MISC)
        )
    )

    val DOWSING_ROD = registerItem("dowsing_rod", DowsingRod(
        FabricItemSettings().group(ItemGroup.MISC).maxDamage(10)
    ))

    fun registerItem(name: String, item: Item): Item {
        println("asdssssx")
        return Registry.register(Registry.ITEM, Identifier("unicopia", name), item)
    }

    fun registerModItems() {
        println("asdx")
    }
}
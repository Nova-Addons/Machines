package xyz.xenondevs.nova.machines.recipe.group.hardcoded

import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.data.recipe.NovaRecipe
import xyz.xenondevs.nova.data.recipe.RecipeRegistry
import xyz.xenondevs.nova.data.recipe.SingleResultRecipe
import xyz.xenondevs.nova.initialize.Init
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.machines.Machines
import xyz.xenondevs.nova.machines.registry.Items
import xyz.xenondevs.nova.machines.registry.RecipeTypes
import xyz.xenondevs.nova.machines.tileentity.processing.CobblestoneGenerator
import xyz.xenondevs.nova.machines.tileentity.processing.Freezer
import xyz.xenondevs.nova.util.data.NamespacedKey

@Init
object HardcodedRecipes {
    
    private val recipes: List<NovaRecipe> = listOf(
        StarCollectorRecipe,
        CobblestoneGeneratorRecipe(NamespacedKey(Machines, "cobblestone_generator.cobblestone"), CobblestoneGenerator.Mode.COBBLESTONE),
        CobblestoneGeneratorRecipe(NamespacedKey(Machines, "cobblestone_generator.stone"), CobblestoneGenerator.Mode.STONE),
        CobblestoneGeneratorRecipe(NamespacedKey(Machines, "cobblestone_generator.obsidian"), CobblestoneGenerator.Mode.OBSIDIAN),
        FreezerRecipe(NamespacedKey(Machines, "freezer.ice"), Freezer.Mode.ICE),
        FreezerRecipe(NamespacedKey(Machines, "freezer.packed_ice"), Freezer.Mode.PACKED_ICE),
        FreezerRecipe(NamespacedKey(Machines, "freezer.blue_ice"), Freezer.Mode.BLUE_ICE),
    )
    
    @InitFun
    fun register() {
        RecipeRegistry.addFakeRecipes(recipes)
        RecipeRegistry.addCreationInfo(mapOf(
            "machines:star_shards" to "item_info.machines.star_shards",
            "machines:infinite_water_source" to "item_info.machines.infinite_water_source"
        ))
    }
    
}

object StarCollectorRecipe : NovaRecipe, SingleResultRecipe {
    override val key = NamespacedKey(Machines, "star_collector.star_dust")
    override val type = RecipeTypes.STAR_COLLECTOR
    override val result = Items.STAR_DUST.createItemStack()
}

class CobblestoneGeneratorRecipe(
    override val key: NamespacedKey,
    val mode: CobblestoneGenerator.Mode,
    override val result: ItemStack = mode.product
) : NovaRecipe, SingleResultRecipe {
    override val type = RecipeTypes.COBBLESTONE_GENERATOR
}

class FreezerRecipe(
    override val key: NamespacedKey,
    val mode: Freezer.Mode,
    override val result: ItemStack = mode.product
) : NovaRecipe, SingleResultRecipe {
    override val type = RecipeTypes.FREEZER
}
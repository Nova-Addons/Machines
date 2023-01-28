package xyz.xenondevs.nova.machines.recipe.group.hardcoded

import de.studiocode.invui.gui.GUI
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.guitype.GUIType
import xyz.xenondevs.nova.machines.registry.Blocks
import xyz.xenondevs.nova.machines.registry.GUITextures
import xyz.xenondevs.nova.tileentity.network.fluid.FluidType
import xyz.xenondevs.nova.ui.StaticFluidBar
import xyz.xenondevs.nova.ui.menu.item.recipes.createRecipeChoiceItem
import xyz.xenondevs.nova.ui.menu.item.recipes.group.RecipeGroup

object FreezerRecipeGroup : RecipeGroup<FreezerRecipe>() {
    
    override val priority = 8
    override val texture = GUITextures.RECIPE_FREEZER
    override val icon = Blocks.FREEZER.basicClientsideProvider
    
    override fun createGUI(recipe: FreezerRecipe): GUI {
        return GUIBuilder(GUIType.NORMAL)
            .setStructure(
                ". w . . . . . . .",
                ". w . . . . r . .",
                ". w . . . . . . .")
            .addIngredient('r', createRecipeChoiceItem(listOf(recipe.result)))
            .addIngredient('w', StaticFluidBar(FluidType.WATER, 1000L * recipe.mode.maxCostMultiplier, 100_000, 3))
            .build()
    }
    
}
package xyz.xenondevs.nova.machines.recipe.group.hardcoded

import xyz.xenondevs.invui.gui.Gui
import xyz.xenondevs.nova.machines.registry.GuiTextures
import xyz.xenondevs.nova.machines.registry.Items
import xyz.xenondevs.nova.tileentity.network.fluid.FluidType
import xyz.xenondevs.nova.ui.StaticFluidBar
import xyz.xenondevs.nova.ui.menu.item.recipes.createRecipeChoiceItem
import xyz.xenondevs.nova.ui.menu.item.recipes.group.RecipeGroup

object FreezerRecipeGroup : RecipeGroup<FreezerRecipe>() {
    
    override val priority = 8
    override val texture = GuiTextures.RECIPE_FREEZER
    override val icon = Items.FREEZER.basicClientsideProvider
    
    override fun createGui(recipe: FreezerRecipe): Gui {
        return Gui.normal()
            .setStructure(
                ". w . . . . . . .",
                ". w . . . . r . .",
                ". w . . . . . . .")
            .addIngredient('r', createRecipeChoiceItem(listOf(recipe.result)))
            .addIngredient('w', StaticFluidBar(FluidType.WATER, 1000L * recipe.mode.maxCostMultiplier, 100_000, 3))
            .build()
    }
    
}
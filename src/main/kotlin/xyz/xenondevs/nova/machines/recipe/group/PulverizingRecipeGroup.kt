package xyz.xenondevs.nova.machines.recipe.group

import xyz.xenondevs.nova.machines.recipe.PulverizerRecipe
import xyz.xenondevs.nova.machines.registry.Blocks
import xyz.xenondevs.nova.machines.registry.GuiTextures
import xyz.xenondevs.nova.ui.menu.item.recipes.group.ConversionRecipeGroup

object PulverizingRecipeGroup : ConversionRecipeGroup<PulverizerRecipe>() {
    override val priority = 4
    override val icon = Blocks.PULVERIZER.basicClientsideProvider
    override val texture = GuiTextures.RECIPE_PULVERIZER
}
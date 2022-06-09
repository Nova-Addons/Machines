package xyz.xenondevs.nova.machines.recipe.group

import xyz.xenondevs.nova.machines.registry.Blocks
import xyz.xenondevs.nova.machines.registry.GUITextures
import xyz.xenondevs.nova.ui.menu.item.recipes.group.ConversionRecipeGroup

object PulverizingRecipeGroup : ConversionRecipeGroup() {
    override val priority = 4
    override val icon = Blocks.PULVERIZER.basicClientsideProvider
    override val texture = GUITextures.RECIPE_PULVERIZER
}
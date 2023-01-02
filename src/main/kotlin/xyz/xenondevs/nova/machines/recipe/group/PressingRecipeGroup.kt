package xyz.xenondevs.nova.machines.recipe.group

import xyz.xenondevs.nova.data.recipe.ConversionNovaRecipe
import xyz.xenondevs.nova.machines.registry.Blocks
import xyz.xenondevs.nova.machines.registry.GUITextures
import xyz.xenondevs.nova.ui.menu.item.recipes.group.ConversionRecipeGroup

object PressingRecipeGroup : ConversionRecipeGroup<ConversionNovaRecipe>() {
    override val priority = 5
    override val icon = Blocks.MECHANICAL_PRESS.basicClientsideProvider
    override val texture = GUITextures.RECIPE_PRESS
}
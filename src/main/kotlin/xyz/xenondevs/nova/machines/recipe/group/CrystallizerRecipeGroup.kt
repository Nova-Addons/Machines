package xyz.xenondevs.nova.machines.recipe.group

import xyz.xenondevs.nova.machines.recipe.CrystallizerRecipe
import xyz.xenondevs.nova.machines.registry.Blocks
import xyz.xenondevs.nova.ui.menu.item.recipes.group.ConversionRecipeGroup
import xyz.xenondevs.nova.ui.overlay.character.gui.CoreGuiTexture

object CrystallizerRecipeGroup : ConversionRecipeGroup<CrystallizerRecipe>() {
    override val icon = Blocks.CRYSTALLIZER.clientsideProvider
    override val priority = 10
    override val texture = CoreGuiTexture.RECIPE_CONVERSION
}
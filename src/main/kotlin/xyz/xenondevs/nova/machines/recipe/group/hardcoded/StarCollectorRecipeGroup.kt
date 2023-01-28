package xyz.xenondevs.nova.machines.recipe.group.hardcoded

import de.studiocode.invui.gui.GUI
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.guitype.GUIType
import xyz.xenondevs.nova.machines.registry.Blocks
import xyz.xenondevs.nova.machines.registry.GUITextures
import xyz.xenondevs.nova.ui.menu.item.recipes.createRecipeChoiceItem
import xyz.xenondevs.nova.ui.menu.item.recipes.group.RecipeGroup

object StarCollectorRecipeGroup : RecipeGroup<StarCollectorRecipe>() {
    
    override val priority = 9
    override val texture = GUITextures.RECIPE_STAR_COLLECTOR
    override val icon = Blocks.STAR_COLLECTOR.basicClientsideProvider
    
    override fun createGUI(recipe: StarCollectorRecipe): GUI {
        return GUIBuilder(GUIType.NORMAL)
            .setStructure(
                ". . . . . . . . .",
                ". . . . . . . r .",
                ". . . . . . . . .")
            .addIngredient('r', createRecipeChoiceItem(listOf(recipe.result)))
            .build()
    }
    
}
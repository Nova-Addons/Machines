package xyz.xenondevs.nova.machines.recipe.group.hardcoded

import de.studiocode.invui.gui.GUI
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.guitype.GUIType
import xyz.xenondevs.nova.data.recipe.RecipeContainer
import xyz.xenondevs.nova.machines.registry.Blocks
import xyz.xenondevs.nova.machines.registry.GUITextures
import xyz.xenondevs.nova.ui.menu.item.recipes.createRecipeChoiceItem
import xyz.xenondevs.nova.ui.menu.item.recipes.group.RecipeGroup

object StarCollectorRecipeGroup : RecipeGroup() {
    
    override val priority = 9
    override val texture = GUITextures.RECIPE_STAR_COLLECTOR
    override val icon = Blocks.STAR_COLLECTOR.basicItemProvider
    
    override fun createGUI(container: RecipeContainer): GUI {
        return GUIBuilder(GUIType.NORMAL, 9, 3)
            .setStructure("" +
                ". . . . . . . . ." +
                ". . . . . . . r ." +
                ". . . . . . . . .")
            .addIngredient('r', createRecipeChoiceItem(listOf(container.result!!)))
            .build()
    }
    
}
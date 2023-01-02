package xyz.xenondevs.nova.machines.recipe.group.hardcoded

import de.studiocode.invui.gui.GUI
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.guitype.GUIType
import net.md_5.bungee.api.chat.TranslatableComponent
import xyz.xenondevs.nova.machines.registry.Blocks
import xyz.xenondevs.nova.machines.registry.GUIMaterials
import xyz.xenondevs.nova.machines.registry.GUITextures
import xyz.xenondevs.nova.tileentity.network.fluid.FluidType
import xyz.xenondevs.nova.ui.StaticFluidBar
import xyz.xenondevs.nova.ui.menu.item.recipes.createRecipeChoiceItem
import xyz.xenondevs.nova.ui.menu.item.recipes.group.RecipeGroup

object CobblestoneGeneratorRecipeGroup : RecipeGroup<CobblestoneGeneratorRecipe>() {
    
    override val priority = 7
    override val texture = GUITextures.RECIPE_COBBLESTONE_GENERATOR
    override val icon = Blocks.COBBLESTONE_GENERATOR.basicClientsideProvider
    
    override fun createGUI(recipe: CobblestoneGeneratorRecipe): GUI {
        val progressItem = GUIMaterials.TP_FLUID_PROGRESS_LEFT_RIGHT
            .createClientsideItemBuilder()
            .setDisplayName(TranslatableComponent("menu.machines.recipe.cobblestone_generator.${recipe.mode.name.lowercase()}"))
        
        return GUIBuilder(GUIType.NORMAL)
            .setStructure(
                ". w l . . . . . .",
                ". w l . > . r . .",
                ". w l . m . . . .")
            .addIngredient('r', createRecipeChoiceItem(listOf(recipe.result)))
            .addIngredient('m', recipe.mode.uiItem.clientsideProvider)
            .addIngredient('>', progressItem)
            .addIngredient('w', StaticFluidBar(FluidType.WATER, 1000, 1000, 3))
            .addIngredient('l', StaticFluidBar(FluidType.LAVA, 1000, 1000, 3))
            .build()
        
    }
    
}
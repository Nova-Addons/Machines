package xyz.xenondevs.nova.machines.recipe.group

import org.bukkit.Material
import org.bukkit.potion.PotionEffect
import xyz.xenondevs.invui.gui.Gui
import xyz.xenondevs.invui.gui.ScrollGui
import xyz.xenondevs.invui.item.builder.ItemBuilder
import xyz.xenondevs.invui.item.builder.PotionBuilder
import xyz.xenondevs.nova.item.DefaultGuiItems
import xyz.xenondevs.nova.machines.recipe.ElectricBrewingStandRecipe
import xyz.xenondevs.nova.machines.registry.GuiTextures
import xyz.xenondevs.nova.machines.registry.Items
import xyz.xenondevs.nova.ui.item.ScrollLeftItem
import xyz.xenondevs.nova.ui.item.ScrollRightItem
import xyz.xenondevs.nova.ui.menu.item.recipes.createRecipeChoiceItem
import xyz.xenondevs.nova.ui.menu.item.recipes.group.RecipeGroup

object ElectricBrewingStandRecipeGroup : RecipeGroup<ElectricBrewingStandRecipe>() {
    
    override val icon = Items.ELECTRIC_BREWING_STAND.clientsideProvider
    override val priority = 0
    override val texture = GuiTextures.RECIPE_ELECTRIC_BREWING_STAND
    
    override fun createGui(recipe: ElectricBrewingStandRecipe): Gui {
        val result = PotionBuilder(PotionBuilder.PotionType.NORMAL)
            .addEffect(PotionEffect(recipe.result, -1, -1))
            .get()
        
        val timeItem = DefaultGuiItems.INVISIBLE_ITEM.createClientsideItemBuilder()
            .setDisplayName("Time: ${recipe.defaultTime} ticks")
        val durationItem = ItemBuilder(Material.REDSTONE)
            .setDisplayName("Max duration level: ${recipe.maxDurationLevel}\nDuration multiplier: ${recipe.redstoneMultiplier}")
        val amplifierItem = ItemBuilder(Material.GLOWSTONE_DUST)
            .setDisplayName("Max amplifier level: ${recipe.maxAmplifierLevel}\nAmplifier multiplier: ${recipe.glowstoneMultiplier}")
        
        return ScrollGui.items()
            .setStructure(
                "< x x x x x x x >",
                ". . . . t . . . .",
                ". . d . r . a . .")
            .addIngredient('r', createRecipeChoiceItem(listOf(result)))
            .addIngredient('<', ::ScrollLeftItem)
            .addIngredient('>', ::ScrollRightItem)
            .addIngredient('t', timeItem)
            .addIngredient('d', durationItem)
            .addIngredient('a', amplifierItem)
            .setContent(recipe.inputs.map(::createRecipeChoiceItem))
            .build()
    }
    
}
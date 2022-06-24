package xyz.xenondevs.nova.machines

import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.machines.advancement.Advancements
import xyz.xenondevs.nova.machines.recipe.group.hardcoded.HardcodedRecipes
import xyz.xenondevs.nova.machines.registry.Blocks
import xyz.xenondevs.nova.machines.registry.Items
import xyz.xenondevs.nova.machines.registry.RecipeTypes
import java.util.logging.Logger

lateinit var LOGGER: Logger

object Machines : Addon() {
    
    override fun init() {
        LOGGER = logger
        
        Blocks.init()
        Items.init()
        RecipeTypes.init()
        HardcodedRecipes.register()
        Advancements.register()
    }
    
    override fun onEnable() = Unit
    override fun onDisable() = Unit
    
}
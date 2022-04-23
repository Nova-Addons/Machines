package xyz.xenondevs.nova.machines

import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.machines.recipe.group.hardcoded.HardcodedRecipes
import xyz.xenondevs.nova.machines.registry.Blocks
import xyz.xenondevs.nova.machines.registry.Items
import xyz.xenondevs.nova.machines.registry.RecipeTypes
import java.util.logging.Logger

lateinit var LOGGER: Logger
lateinit var MACHINES: Machines

class Machines : Addon() {
    
    override fun init() {
        MACHINES = this
        LOGGER = logger
        
        Blocks.init()
        Items.init()
        RecipeTypes.init()
        HardcodedRecipes.register()
    }
    
    override fun onEnable() = Unit
    override fun onDisable() = Unit
    
}
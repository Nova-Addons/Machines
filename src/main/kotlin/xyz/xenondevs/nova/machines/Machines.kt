package xyz.xenondevs.nova.machines

import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.update.ProjectDistributor
import java.util.logging.Logger

lateinit var LOGGER: Logger

object Machines : Addon() {
    
    override val projectDistributors = listOf(ProjectDistributor.hangar("xenondevs/Machines"), ProjectDistributor.spigotmc(102712))
    
    override fun init() {
        LOGGER = logger
    }
    
}
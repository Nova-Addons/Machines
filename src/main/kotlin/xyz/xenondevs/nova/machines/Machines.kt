package xyz.xenondevs.nova.machines

import xyz.xenondevs.nova.addon.Addon
import java.util.logging.Logger

lateinit var LOGGER: Logger

object Machines : Addon() {
    
    override fun init() {
        LOGGER = logger
    }
    
}
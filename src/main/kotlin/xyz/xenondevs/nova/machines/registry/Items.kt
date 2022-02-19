package xyz.xenondevs.nova.machines.registry

import xyz.xenondevs.nova.machines.MACHINES
import xyz.xenondevs.nova.machines.item.MobCatcherItem
import xyz.xenondevs.nova.material.NovaMaterialRegistry.registerDefaultItem

object Items {
    
    val MOB_CATCHER = registerDefaultItem(MACHINES, "mob_catcher", MobCatcherItem)
    
    // Plates
    val IRON_PLATE = registerDefaultItem(MACHINES, "iron_plate")
    val GOLD_PLATE = registerDefaultItem(MACHINES, "gold_plate")
    val DIAMOND_PLATE = registerDefaultItem(MACHINES, "diamond_plate")
    val NETHERITE_PLATE = registerDefaultItem(MACHINES, "netherite_plate")
    val EMERALD_PLATE = registerDefaultItem(MACHINES, "emerald_plate")
    val REDSTONE_PLATE = registerDefaultItem(MACHINES, "redstone_plate")
    val LAPIS_PLATE = registerDefaultItem(MACHINES, "lapis_plate")
    val COPPER_PLATE = registerDefaultItem(MACHINES, "copper_plate")
    
    // Gears
    val IRON_GEAR = registerDefaultItem(MACHINES, "iron_gear")
    val GOLD_GEAR = registerDefaultItem(MACHINES, "gold_gear")
    val DIAMOND_GEAR = registerDefaultItem(MACHINES, "diamond_gear")
    val NETHERITE_GEAR = registerDefaultItem(MACHINES, "netherite_gear")
    val EMERALD_GEAR = registerDefaultItem(MACHINES, "emerald_gear")
    val REDSTONE_GEAR = registerDefaultItem(MACHINES, "redstone_gear")
    val LAPIS_GEAR = registerDefaultItem(MACHINES, "lapis_gear")
    val COPPER_GEAR = registerDefaultItem(MACHINES, "copper_gear")
    
    // Dusts
    val IRON_DUST = registerDefaultItem(MACHINES, "iron_dust")
    val GOLD_DUST = registerDefaultItem(MACHINES, "gold_dust")
    val DIAMOND_DUST = registerDefaultItem(MACHINES, "diamond_dust")
    val NETHERITE_DUST = registerDefaultItem(MACHINES, "netherite_dust")
    val EMERALD_DUST = registerDefaultItem(MACHINES, "emerald_dust")
    val LAPIS_DUST = registerDefaultItem(MACHINES, "lapis_dust")
    val COAL_DUST = registerDefaultItem(MACHINES, "coal_dust")
    val COPPER_DUST = registerDefaultItem(MACHINES, "copper_dust")
    val STAR_DUST = registerDefaultItem(MACHINES, "star_dust")
    
    // Crafting components
    val NETHERITE_DRILL = registerDefaultItem(MACHINES, "netherite_drill")
    val STAR_SHARDS = registerDefaultItem(MACHINES, "star_shards")
    val BASIC_MACHINE_FRAME = registerDefaultItem(MACHINES, "basic_machine_frame")
    val ADVANCED_MACHINE_FRAME = registerDefaultItem(MACHINES, "advanced_machine_frame")
    val ELITE_MACHINE_FRAME = registerDefaultItem(MACHINES, "elite_machine_frame")
    val ULTIMATE_MACHINE_FRAME = registerDefaultItem(MACHINES, "ultimate_machine_frame")
    val CREATIVE_MACHINE_FRAME = registerDefaultItem(MACHINES, "creative_machine_frame")
    val SCAFFOLDING = registerDefaultItem(MACHINES, "scaffolding")
    
    fun init() = Unit
    
}
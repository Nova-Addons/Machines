package xyz.xenondevs.nova.machines.registry

import xyz.xenondevs.nova.item.behavior.Damageable
import xyz.xenondevs.nova.item.behavior.Enchantable
import xyz.xenondevs.nova.item.behavior.Extinguishing
import xyz.xenondevs.nova.item.behavior.Flattening
import xyz.xenondevs.nova.item.behavior.Stripping
import xyz.xenondevs.nova.item.behavior.Tilling
import xyz.xenondevs.nova.item.behavior.Tool
import xyz.xenondevs.nova.item.behavior.Wearable
import xyz.xenondevs.nova.machines.Machines
import xyz.xenondevs.nova.machines.item.MobCatcherBehavior
import xyz.xenondevs.nova.material.NovaMaterialRegistry.registerItem
import xyz.xenondevs.nova.player.equipment.ArmorType

@Suppress("unused")
object Items {
    
    val MOB_CATCHER = registerItem(Machines, "mob_catcher", MobCatcherBehavior)
    
    // Tools
    val STAR_SWORD = registerItem(Machines, "star_sword", Tool, Damageable, Enchantable)
    val STAR_SHOVEL = registerItem(Machines, "star_shovel", Tool, Damageable, Enchantable, Flattening, Extinguishing)
    val STAR_PICKAXE = registerItem(Machines, "star_pickaxe", Tool, Damageable, Enchantable)
    val STAR_AXE = registerItem(Machines, "star_axe", Tool, Damageable, Enchantable, Stripping)
    val STAR_HOE = registerItem(Machines, "star_hoe", Tool, Damageable, Enchantable, Tilling)
    
    // Armor
    val STAR_HELMET = registerItem(Machines, "star_helmet", Wearable(ArmorType.HELMET))
    val STAR_CHESTPLATE = registerItem(Machines, "star_chestplate", Wearable(ArmorType.CHESTPLATE))
    val STAR_LEGGINGS = registerItem(Machines, "star_leggings", Wearable(ArmorType.LEGGINGS))
    val STAR_BOOTS = registerItem(Machines, "star_boots", Wearable(ArmorType.BOOTS))
    
    // Plates
    val IRON_PLATE = registerItem(Machines, "iron_plate")
    val GOLD_PLATE = registerItem(Machines, "gold_plate")
    val DIAMOND_PLATE = registerItem(Machines, "diamond_plate")
    val NETHERITE_PLATE = registerItem(Machines, "netherite_plate")
    val EMERALD_PLATE = registerItem(Machines, "emerald_plate")
    val REDSTONE_PLATE = registerItem(Machines, "redstone_plate")
    val LAPIS_PLATE = registerItem(Machines, "lapis_plate")
    val COPPER_PLATE = registerItem(Machines, "copper_plate")
    
    // Gears
    val IRON_GEAR = registerItem(Machines, "iron_gear")
    val GOLD_GEAR = registerItem(Machines, "gold_gear")
    val DIAMOND_GEAR = registerItem(Machines, "diamond_gear")
    val NETHERITE_GEAR = registerItem(Machines, "netherite_gear")
    val EMERALD_GEAR = registerItem(Machines, "emerald_gear")
    val REDSTONE_GEAR = registerItem(Machines, "redstone_gear")
    val LAPIS_GEAR = registerItem(Machines, "lapis_gear")
    val COPPER_GEAR = registerItem(Machines, "copper_gear")
    
    // Dusts
    val IRON_DUST = registerItem(Machines, "iron_dust")
    val GOLD_DUST = registerItem(Machines, "gold_dust")
    val DIAMOND_DUST = registerItem(Machines, "diamond_dust")
    val NETHERITE_DUST = registerItem(Machines, "netherite_dust")
    val EMERALD_DUST = registerItem(Machines, "emerald_dust")
    val LAPIS_DUST = registerItem(Machines, "lapis_dust")
    val COAL_DUST = registerItem(Machines, "coal_dust")
    val COPPER_DUST = registerItem(Machines, "copper_dust")
    val STAR_DUST = registerItem(Machines, "star_dust")
    
    // Crafting components
    val STAR_SHARDS = registerItem(Machines, "star_shards")
    val STAR_CRYSTAL = registerItem(Machines, "star_crystal")
    val NETHERITE_DRILL = registerItem(Machines, "netherite_drill")
    val SCAFFOLDING = registerItem(Machines, "scaffolding")
    val SOLAR_CELL = registerItem(Machines, "solar_cell")
    
    fun init() = Unit
    
}
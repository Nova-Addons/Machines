package xyz.xenondevs.nova.machines.registry

import org.bukkit.Material
import org.bukkit.Sound
import xyz.xenondevs.nova.machines.MACHINES
import xyz.xenondevs.nova.machines.tileentity.agriculture.*
import xyz.xenondevs.nova.machines.tileentity.energy.*
import xyz.xenondevs.nova.machines.tileentity.mob.Breeder
import xyz.xenondevs.nova.machines.tileentity.mob.MobDuplicator
import xyz.xenondevs.nova.machines.tileentity.mob.MobKiller
import xyz.xenondevs.nova.machines.tileentity.processing.*
import xyz.xenondevs.nova.machines.tileentity.processing.brewing.ElectricBrewingStand
import xyz.xenondevs.nova.machines.tileentity.world.*
import xyz.xenondevs.nova.material.BlockOptions
import xyz.xenondevs.nova.material.NovaMaterialRegistry.registerEnergyTileEntity
import xyz.xenondevs.nova.material.NovaMaterialRegistry.registerItem
import xyz.xenondevs.nova.material.NovaMaterialRegistry.registerTileEntity
import xyz.xenondevs.nova.util.SoundEffect
import xyz.xenondevs.nova.util.item.ToolCategory
import xyz.xenondevs.nova.util.item.ToolLevel

object Blocks {
    
    private val SANDSTONE = BlockOptions(0.8, ToolCategory.PICKAXE, ToolLevel.WOODEN, true, Material.BARRIER, SoundEffect(Sound.BLOCK_STONE_PLACE), SoundEffect(Sound.BLOCK_STONE_BREAK), Material.SANDSTONE)
    private val STONE = BlockOptions(3.0, ToolCategory.PICKAXE, ToolLevel.STONE, true, Material.BARRIER, SoundEffect(Sound.BLOCK_STONE_PLACE), SoundEffect(Sound.BLOCK_STONE_BREAK), Material.NETHERITE_BLOCK)
    private val LIGHT_METAL = BlockOptions(0.5, ToolCategory.PICKAXE, ToolLevel.WOODEN, false, Material.BARRIER, SoundEffect(Sound.BLOCK_METAL_PLACE), SoundEffect(Sound.BLOCK_METAL_BREAK), Material.IRON_BLOCK)
    private val METAL = BlockOptions(5.0, ToolCategory.PICKAXE, ToolLevel.STONE, true, Material.BARRIER, SoundEffect(Sound.BLOCK_METAL_PLACE), SoundEffect(Sound.BLOCK_METAL_BREAK), Material.IRON_BLOCK)
    
    val AUTO_FISHER = registerEnergyTileEntity(MACHINES, "auto_fisher", STONE, ::AutoFisher)
    val FERTILIZER = registerEnergyTileEntity(MACHINES, "fertilizer", STONE, ::Fertilizer)
    val HARVESTER = registerEnergyTileEntity(MACHINES, "harvester", STONE, ::Harvester)
    val PLANTER = registerEnergyTileEntity(MACHINES, "planter", STONE, ::Planter)
    val TREE_FACTORY = registerEnergyTileEntity(MACHINES, "tree_factory", STONE, ::TreeFactory)
    val CHARGER = registerEnergyTileEntity(MACHINES, "charger", STONE, ::Charger)
    val WIRELESS_CHARGER = registerEnergyTileEntity(MACHINES, "wireless_charger", STONE, ::WirelessCharger)
    val BREEDER = registerEnergyTileEntity(MACHINES, "breeder", STONE, ::Breeder)
    val MOB_DUPLICATOR = registerEnergyTileEntity(MACHINES, "mob_duplicator", STONE, ::MobDuplicator)
    val MOB_KILLER = registerEnergyTileEntity(MACHINES, "mob_killer", STONE, ::MobKiller)
    val COBBLESTONE_GENERATOR = registerEnergyTileEntity(MACHINES, "cobblestone_generator", STONE, ::CobblestoneGenerator)
    val ELECTRIC_FURNACE = registerEnergyTileEntity(MACHINES, "electric_furnace", STONE, ::ElectricFurnace)
    val MECHANICAL_PRESS = registerEnergyTileEntity(MACHINES, "mechanical_press", STONE, ::MechanicalPress)
    val PULVERIZER = registerEnergyTileEntity(MACHINES, "pulverizer", STONE, ::Pulverizer)
    val BLOCK_BREAKER = registerEnergyTileEntity(MACHINES, "block_breaker", STONE, ::BlockBreaker)
    val BLOCK_PLACER = registerEnergyTileEntity(MACHINES, "block_placer", STONE, ::BlockPlacer)
    val STAR_COLLECTOR = registerEnergyTileEntity(MACHINES, "star_collector", STONE, ::StarCollector)
    val CHUNK_LOADER = registerEnergyTileEntity(MACHINES, "chunk_loader", STONE, ::ChunkLoader)
    val QUARRY = registerEnergyTileEntity(MACHINES, "quarry", STONE, ::Quarry, Quarry::canPlace)
    val ELECTRIC_BREWING_STAND = registerTileEntity(MACHINES, "electric_brewing_stand", STONE, ::ElectricBrewingStand)
    val PUMP = registerTileEntity(MACHINES, "pump", STONE, ::Pump)
    val FREEZER = registerTileEntity(MACHINES, "freezer", STONE, ::Freezer)
    val FLUID_INFUSER = registerTileEntity(MACHINES, "fluid_infuser", STONE, ::FluidInfuser)
    val SPRINKLER = registerTileEntity(MACHINES, "sprinkler", LIGHT_METAL, ::Sprinkler)
    val SOLAR_PANEL = registerEnergyTileEntity(MACHINES, "solar_panel", STONE, ::SolarPanel)
    val LIGHTNING_EXCHANGER = registerEnergyTileEntity(MACHINES, "lightning_exchanger", STONE, ::LightningExchanger)
    val WIND_TURBINE = registerEnergyTileEntity(MACHINES, "wind_turbine", METAL, ::WindTurbine, WindTurbine::canPlace, multiBlockLoader = WindTurbine::loadMultiBlock)
    val FURNACE_GENERATOR = registerEnergyTileEntity(MACHINES, "furnace_generator", STONE, ::FurnaceGenerator)
    val LAVA_GENERATOR = registerTileEntity(MACHINES, "lava_generator", STONE, ::LavaGenerator)
    val INFINITE_WATER_SOURCE = registerTileEntity(MACHINES, "infinite_water_source", SANDSTONE, ::InfiniteWaterSource)
    
    // Tree Miniatures
    val OAK_TREE_MINIATURE = registerItem(MACHINES, "oak_tree_miniature")
    val SPRUCE_TREE_MINIATURE = registerItem(MACHINES, "spruce_tree_miniature")
    val BIRCH_TREE_MINIATURE = registerItem(MACHINES, "birch_tree_miniature")
    val JUNGLE_TREE_MINIATURE = registerItem(MACHINES, "jungle_tree_miniature")
    val ACACIA_TREE_MINIATURE = registerItem(MACHINES, "acacia_tree_miniature")
    val DARK_OAK_TREE_MINIATURE = registerItem(MACHINES, "dark_oak_tree_miniature")
    val MANGROVE_TREE_MINIATURE = registerItem(MACHINES, "mangrove_tree_miniature")
    val CRIMSON_TREE_MINIATURE = registerItem(MACHINES, "crimson_tree_miniature")
    val WARPED_TREE_MINIATURE = registerItem(MACHINES, "warped_tree_miniature")
    val GIANT_RED_MUSHROOM_MINIATURE = registerItem(MACHINES, "giant_red_mushroom_miniature")
    val GIANT_BROWN_MUSHROOM_MINIATURE = registerItem(MACHINES, "giant_brown_mushroom_miniature")
    
    // Move these somewhere else?
    val COBBLESTONE_GENERATOR_WATER_LEVELS = registerItem(MACHINES, "cobblestone_generator_water_levels")
    val COBBLESTONE_GENERATOR_LAVA_LEVELS = registerItem(MACHINES, "cobblestone_generator_lava_levels")
    
    fun init() = Unit
    
}
package xyz.xenondevs.nova.machines.registry

import org.bukkit.Material
import org.bukkit.Sound
import xyz.xenondevs.nova.machines.Machines
import xyz.xenondevs.nova.machines.tileentity.agriculture.*
import xyz.xenondevs.nova.machines.tileentity.energy.*
import xyz.xenondevs.nova.machines.tileentity.mob.Breeder
import xyz.xenondevs.nova.machines.tileentity.mob.MobDuplicator
import xyz.xenondevs.nova.machines.tileentity.mob.MobKiller
import xyz.xenondevs.nova.machines.tileentity.processing.*
import xyz.xenondevs.nova.machines.tileentity.processing.brewing.ElectricBrewingStand
import xyz.xenondevs.nova.machines.tileentity.world.*
import xyz.xenondevs.nova.material.BlockOptions
import xyz.xenondevs.nova.material.NovaMaterialRegistry.registerTileEntity
import xyz.xenondevs.nova.material.NovaMaterialRegistry.registerItem
import xyz.xenondevs.nova.util.SoundEffect
import xyz.xenondevs.nova.util.item.ToolCategory
import xyz.xenondevs.nova.util.item.ToolLevel

object Blocks {
    
    private val SANDSTONE = BlockOptions(0.8, ToolCategory.PICKAXE, ToolLevel.WOODEN, true, Material.BARRIER, SoundEffect(Sound.BLOCK_STONE_PLACE), SoundEffect(Sound.BLOCK_STONE_BREAK), Material.SANDSTONE)
    private val STONE = BlockOptions(3.0, ToolCategory.PICKAXE, ToolLevel.STONE, true, Material.BARRIER, SoundEffect(Sound.BLOCK_STONE_PLACE), SoundEffect(Sound.BLOCK_STONE_BREAK), Material.NETHERITE_BLOCK)
    private val LIGHT_METAL = BlockOptions(0.5, ToolCategory.PICKAXE, ToolLevel.WOODEN, false, Material.BARRIER, SoundEffect(Sound.BLOCK_METAL_PLACE), SoundEffect(Sound.BLOCK_METAL_BREAK), Material.IRON_BLOCK)
    private val METAL = BlockOptions(5.0, ToolCategory.PICKAXE, ToolLevel.STONE, true, Material.BARRIER, SoundEffect(Sound.BLOCK_METAL_PLACE), SoundEffect(Sound.BLOCK_METAL_BREAK), Material.IRON_BLOCK)
    
    val AUTO_FISHER = registerTileEntity(Machines, "auto_fisher", STONE, ::AutoFisher)
    val FERTILIZER = registerTileEntity(Machines, "fertilizer", STONE, ::Fertilizer)
    val HARVESTER = registerTileEntity(Machines, "harvester", STONE, ::Harvester)
    val PLANTER = registerTileEntity(Machines, "planter", STONE, ::Planter)
    val TREE_FACTORY = registerTileEntity(Machines, "tree_factory", STONE, ::TreeFactory)
    val CHARGER = registerTileEntity(Machines, "charger", STONE, ::Charger)
    val WIRELESS_CHARGER = registerTileEntity(Machines, "wireless_charger", STONE, ::WirelessCharger)
    val BREEDER = registerTileEntity(Machines, "breeder", STONE, ::Breeder)
    val MOB_DUPLICATOR = registerTileEntity(Machines, "mob_duplicator", STONE, ::MobDuplicator)
    val MOB_KILLER = registerTileEntity(Machines, "mob_killer", STONE, ::MobKiller)
    val COBBLESTONE_GENERATOR = registerTileEntity(Machines, "cobblestone_generator", STONE, ::CobblestoneGenerator)
    val ELECTRIC_FURNACE = registerTileEntity(Machines, "electric_furnace", STONE, ::ElectricFurnace)
    val MECHANICAL_PRESS = registerTileEntity(Machines, "mechanical_press", STONE, ::MechanicalPress)
    val PULVERIZER = registerTileEntity(Machines, "pulverizer", STONE, ::Pulverizer)
    val BLOCK_BREAKER = registerTileEntity(Machines, "block_breaker", STONE, ::BlockBreaker)
    val BLOCK_PLACER = registerTileEntity(Machines, "block_placer", STONE, ::BlockPlacer)
    val STAR_COLLECTOR = registerTileEntity(Machines, "star_collector", STONE, ::StarCollector)
    val CHUNK_LOADER = registerTileEntity(Machines, "chunk_loader", STONE, ::ChunkLoader)
    val QUARRY = registerTileEntity(Machines, "quarry", STONE, ::Quarry, Quarry::canPlace)
    val ELECTRIC_BREWING_STAND = registerTileEntity(Machines, "electric_brewing_stand", STONE, ::ElectricBrewingStand)
    val PUMP = registerTileEntity(Machines, "pump", STONE, ::Pump)
    val FREEZER = registerTileEntity(Machines, "freezer", STONE, ::Freezer)
    val FLUID_INFUSER = registerTileEntity(Machines, "fluid_infuser", STONE, ::FluidInfuser)
    val SPRINKLER = registerTileEntity(Machines, "sprinkler", LIGHT_METAL, ::Sprinkler)
    val SOLAR_PANEL = registerTileEntity(Machines, "solar_panel", STONE, ::SolarPanel)
    val LIGHTNING_EXCHANGER = registerTileEntity(Machines, "lightning_exchanger", STONE, ::LightningExchanger)
    val WIND_TURBINE = registerTileEntity(Machines, "wind_turbine", METAL, ::WindTurbine, WindTurbine::canPlace, multiBlockLoader = WindTurbine::loadMultiBlock)
    val FURNACE_GENERATOR = registerTileEntity(Machines, "furnace_generator", STONE, ::FurnaceGenerator)
    val LAVA_GENERATOR = registerTileEntity(Machines, "lava_generator", STONE, ::LavaGenerator)
    val INFINITE_WATER_SOURCE = registerTileEntity(Machines, "infinite_water_source", SANDSTONE, ::InfiniteWaterSource)
    
    // Tree Miniatures
    val OAK_TREE_MINIATURE = registerItem(Machines, "oak_tree_miniature")
    val SPRUCE_TREE_MINIATURE = registerItem(Machines, "spruce_tree_miniature")
    val BIRCH_TREE_MINIATURE = registerItem(Machines, "birch_tree_miniature")
    val JUNGLE_TREE_MINIATURE = registerItem(Machines, "jungle_tree_miniature")
    val ACACIA_TREE_MINIATURE = registerItem(Machines, "acacia_tree_miniature")
    val DARK_OAK_TREE_MINIATURE = registerItem(Machines, "dark_oak_tree_miniature")
    val MANGROVE_TREE_MINIATURE = registerItem(Machines, "mangrove_tree_miniature")
    val CRIMSON_TREE_MINIATURE = registerItem(Machines, "crimson_tree_miniature")
    val WARPED_TREE_MINIATURE = registerItem(Machines, "warped_tree_miniature")
    val GIANT_RED_MUSHROOM_MINIATURE = registerItem(Machines, "giant_red_mushroom_miniature")
    val GIANT_BROWN_MUSHROOM_MINIATURE = registerItem(Machines, "giant_brown_mushroom_miniature")
    
    // Move these somewhere else?
    val COBBLESTONE_GENERATOR_WATER_LEVELS = registerItem(Machines, "cobblestone_generator_water_levels")
    val COBBLESTONE_GENERATOR_LAVA_LEVELS = registerItem(Machines, "cobblestone_generator_lava_levels")
    
    fun init() = Unit
    
}
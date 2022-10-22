package xyz.xenondevs.nova.machines.registry

import org.bukkit.Material
import xyz.xenondevs.nova.data.world.block.property.Directional
import xyz.xenondevs.nova.data.world.block.property.LegacyDirectional
import xyz.xenondevs.nova.item.tool.ToolCategory
import xyz.xenondevs.nova.item.tool.ToolLevel
import xyz.xenondevs.nova.machines.Machines
import xyz.xenondevs.nova.machines.tileentity.agriculture.AutoFisher
import xyz.xenondevs.nova.machines.tileentity.agriculture.Fertilizer
import xyz.xenondevs.nova.machines.tileentity.agriculture.Harvester
import xyz.xenondevs.nova.machines.tileentity.agriculture.Planter
import xyz.xenondevs.nova.machines.tileentity.agriculture.TreeFactory
import xyz.xenondevs.nova.machines.tileentity.energy.Charger
import xyz.xenondevs.nova.machines.tileentity.energy.FurnaceGenerator
import xyz.xenondevs.nova.machines.tileentity.energy.LavaGenerator
import xyz.xenondevs.nova.machines.tileentity.energy.LightningExchanger
import xyz.xenondevs.nova.machines.tileentity.energy.SolarPanel
import xyz.xenondevs.nova.machines.tileentity.energy.WindTurbine
import xyz.xenondevs.nova.machines.tileentity.energy.WirelessCharger
import xyz.xenondevs.nova.machines.tileentity.mob.Breeder
import xyz.xenondevs.nova.machines.tileentity.mob.MobDuplicator
import xyz.xenondevs.nova.machines.tileentity.mob.MobKiller
import xyz.xenondevs.nova.machines.tileentity.processing.CobblestoneGenerator
import xyz.xenondevs.nova.machines.tileentity.processing.ElectricFurnace
import xyz.xenondevs.nova.machines.tileentity.processing.FluidInfuser
import xyz.xenondevs.nova.machines.tileentity.processing.Freezer
import xyz.xenondevs.nova.machines.tileentity.processing.MechanicalPress
import xyz.xenondevs.nova.machines.tileentity.processing.Pulverizer
import xyz.xenondevs.nova.machines.tileentity.processing.brewing.ElectricBrewingStand
import xyz.xenondevs.nova.machines.tileentity.world.BlockBreaker
import xyz.xenondevs.nova.machines.tileentity.world.BlockPlacer
import xyz.xenondevs.nova.machines.tileentity.world.ChunkLoader
import xyz.xenondevs.nova.machines.tileentity.world.InfiniteWaterSource
import xyz.xenondevs.nova.machines.tileentity.world.Pump
import xyz.xenondevs.nova.machines.tileentity.world.Quarry
import xyz.xenondevs.nova.machines.tileentity.world.Sprinkler
import xyz.xenondevs.nova.machines.tileentity.world.StarCollector
import xyz.xenondevs.nova.material.BlockOptions
import xyz.xenondevs.nova.material.NovaMaterialRegistry.registerItem
import xyz.xenondevs.nova.material.NovaMaterialRegistry.registerTileEntity
import xyz.xenondevs.nova.world.block.sound.SoundGroup

object Blocks {
    
    private val SANDSTONE = BlockOptions(0.8, ToolCategory.PICKAXE, null, true, SoundGroup.STONE, Material.SANDSTONE)
    private val STONE = BlockOptions(3.0, ToolCategory.PICKAXE, ToolLevel.STONE, true, SoundGroup.STONE, Material.NETHERITE_BLOCK)
    private val LIGHT_METAL = BlockOptions(0.5, ToolCategory.PICKAXE, null, false, SoundGroup.METAL, Material.IRON_BLOCK)
    private val METAL = BlockOptions(5.0, ToolCategory.PICKAXE, ToolLevel.STONE, true, SoundGroup.METAL, Material.IRON_BLOCK)
    
    val AUTO_FISHER = registerTileEntity(Machines, "auto_fisher", STONE, ::AutoFisher, properties = listOf(Directional.NORMAL))
    val FERTILIZER = registerTileEntity(Machines, "fertilizer", STONE, ::Fertilizer, properties = listOf(Directional.NORMAL))
    val HARVESTER = registerTileEntity(Machines, "harvester", STONE, ::Harvester, properties = listOf(Directional.NORMAL))
    val PLANTER = registerTileEntity(Machines, "planter", STONE, ::Planter, properties = listOf(Directional.NORMAL))
    val TREE_FACTORY = registerTileEntity(Machines, "tree_factory", STONE, ::TreeFactory, properties = listOf(Directional.NORMAL))
    val CHARGER = registerTileEntity(Machines, "charger", STONE, ::Charger, properties = listOf(Directional.NORMAL))
    val WIRELESS_CHARGER = registerTileEntity(Machines, "wireless_charger", STONE, ::WirelessCharger, properties = listOf(Directional.NORMAL))
    val BREEDER = registerTileEntity(Machines, "breeder", STONE, ::Breeder, properties = listOf(Directional.NORMAL))
    val MOB_DUPLICATOR = registerTileEntity(Machines, "mob_duplicator", STONE, ::MobDuplicator, properties = listOf(Directional.NORMAL))
    val MOB_KILLER = registerTileEntity(Machines, "mob_killer", STONE, ::MobKiller, properties = listOf(Directional.NORMAL))
    val COBBLESTONE_GENERATOR = registerTileEntity(Machines, "cobblestone_generator", STONE, ::CobblestoneGenerator, properties = listOf(Directional.NORMAL))
    val ELECTRIC_FURNACE = registerTileEntity(Machines, "electric_furnace", STONE, ::ElectricFurnace, properties = listOf(Directional.NORMAL))
    val MECHANICAL_PRESS = registerTileEntity(Machines, "mechanical_press", STONE, ::MechanicalPress, properties = listOf(Directional.NORMAL))
    val PULVERIZER = registerTileEntity(Machines, "pulverizer", STONE, ::Pulverizer, properties = listOf(Directional.NORMAL))
    val BLOCK_BREAKER = registerTileEntity(Machines, "block_breaker", STONE, ::BlockBreaker, properties = listOf(Directional.NORMAL))
    val BLOCK_PLACER = registerTileEntity(Machines, "block_placer", STONE, ::BlockPlacer, properties = listOf(Directional.NORMAL))
    val STAR_COLLECTOR = registerTileEntity(Machines, "star_collector", STONE, ::StarCollector, properties = listOf(LegacyDirectional))
    val CHUNK_LOADER = registerTileEntity(Machines, "chunk_loader", STONE, ::ChunkLoader, properties = listOf(Directional.NORMAL))
    val QUARRY = registerTileEntity(Machines, "quarry", STONE, ::Quarry, Quarry::canPlace, properties = listOf(Directional.NORMAL))
    val ELECTRIC_BREWING_STAND = registerTileEntity(Machines, "electric_brewing_stand", STONE, ::ElectricBrewingStand, properties = listOf(Directional.NORMAL))
    val PUMP = registerTileEntity(Machines, "pump", STONE, ::Pump, properties = listOf(LegacyDirectional))
    val FREEZER = registerTileEntity(Machines, "freezer", STONE, ::Freezer, properties = listOf(Directional.NORMAL))
    val FLUID_INFUSER = registerTileEntity(Machines, "fluid_infuser", STONE, ::FluidInfuser, properties = listOf(Directional.NORMAL))
    val SPRINKLER = registerTileEntity(Machines, "sprinkler", LIGHT_METAL, ::Sprinkler, properties = listOf(LegacyDirectional))
    val SOLAR_PANEL = registerTileEntity(Machines, "solar_panel", STONE, ::SolarPanel, properties = listOf(Directional.NORMAL))
    val LIGHTNING_EXCHANGER = registerTileEntity(Machines, "lightning_exchanger", STONE, ::LightningExchanger, properties = listOf(LegacyDirectional))
    val WIND_TURBINE = registerTileEntity(Machines, "wind_turbine", METAL, ::WindTurbine, WindTurbine::canPlace, WindTurbine::loadMultiBlock, properties = listOf(Directional.NORMAL))
    val FURNACE_GENERATOR = registerTileEntity(Machines, "furnace_generator", STONE, ::FurnaceGenerator, properties = listOf(Directional.NORMAL))
    val LAVA_GENERATOR = registerTileEntity(Machines, "lava_generator", STONE, ::LavaGenerator, properties = listOf(Directional.NORMAL))
    val INFINITE_WATER_SOURCE = registerTileEntity(Machines, "infinite_water_source", SANDSTONE, ::InfiniteWaterSource, properties = listOf(LegacyDirectional))
    
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
    
    // Water levels
    val COBBLESTONE_GENERATOR_WATER_LEVELS = registerItem(Machines, "cobblestone_generator_water_levels")
    val COBBLESTONE_GENERATOR_LAVA_LEVELS = registerItem(Machines, "cobblestone_generator_lava_levels")
    
    fun init() = Unit
    
}
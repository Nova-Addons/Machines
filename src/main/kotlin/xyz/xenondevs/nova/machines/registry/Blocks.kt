package xyz.xenondevs.nova.machines.registry

import org.bukkit.Material
import xyz.xenondevs.nova.data.world.block.property.Directional
import xyz.xenondevs.nova.data.world.block.property.LegacyDirectional
import xyz.xenondevs.nova.item.tool.ToolCategory
import xyz.xenondevs.nova.item.tool.ToolTier
import xyz.xenondevs.nova.machines.Machines
import xyz.xenondevs.nova.machines.block.StarShardsOre
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
import xyz.xenondevs.nova.machines.tileentity.processing.Crystallizer
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
import xyz.xenondevs.nova.material.NovaMaterialRegistry.block
import xyz.xenondevs.nova.material.NovaMaterialRegistry.registerUnnamedItem
import xyz.xenondevs.nova.material.NovaMaterialRegistry.tileEntity
import xyz.xenondevs.nova.material.options.BlockOptions
import xyz.xenondevs.nova.world.block.sound.SoundGroup

object Blocks {
    
    private val SAND = BlockOptions(0.5, ToolCategory.SHOVEL, ToolTier.WOOD, false, SoundGroup.SAND, Material.PURPLE_CONCRETE_POWDER)
    private val SANDSTONE = BlockOptions(0.8, ToolCategory.PICKAXE, ToolTier.WOOD, true, SoundGroup.STONE, Material.SANDSTONE)
    private val STONE = BlockOptions(3.0, ToolCategory.PICKAXE, ToolTier.WOOD, true, SoundGroup.STONE, Material.NETHERITE_BLOCK)
    private val LIGHT_METAL = BlockOptions(0.5, ToolCategory.PICKAXE, ToolTier.WOOD, false, SoundGroup.METAL, Material.IRON_BLOCK)
    private val STONE_ORE = BlockOptions(3.0, ToolCategory.PICKAXE, ToolTier.STONE, true, SoundGroup.STONE, Material.STONE)
    private val DEEPSLATE_ORE = BlockOptions(3.0, ToolCategory.PICKAXE, ToolTier.STONE, true, SoundGroup.DEEPSLATE, Material.DEEPSLATE)
    private val METAL = BlockOptions(5.0, ToolCategory.PICKAXE, ToolTier.WOOD, true, SoundGroup.METAL, Material.IRON_BLOCK)
    private val MACHINE_FRAME = BlockOptions(2.0, ToolCategory.PICKAXE, ToolTier.WOOD, true, SoundGroup.METAL, Material.STONE)
    
    // TileEntities
    val AUTO_FISHER = tileEntity(Machines, "auto_fisher", ::AutoFisher).blockOptions(STONE).properties(Directional.NORMAL).register()
    val FERTILIZER = tileEntity(Machines, "fertilizer", ::Fertilizer).blockOptions(STONE).properties(Directional.NORMAL).register()
    val HARVESTER = tileEntity(Machines, "harvester", ::Harvester).blockOptions(STONE).properties(Directional.NORMAL).register()
    val PLANTER = tileEntity(Machines, "planter", ::Planter).blockOptions(STONE).properties(Directional.NORMAL).register()
    val TREE_FACTORY = tileEntity(Machines, "tree_factory", ::TreeFactory).blockOptions(STONE).properties(Directional.NORMAL).register()
    val CHARGER = tileEntity(Machines, "charger", ::Charger).blockOptions(STONE).properties(Directional.NORMAL).register()
    val WIRELESS_CHARGER = tileEntity(Machines, "wireless_charger", ::WirelessCharger).blockOptions(STONE).properties(Directional.NORMAL).register()
    val BREEDER = tileEntity(Machines, "breeder", ::Breeder).blockOptions(STONE).properties(Directional.NORMAL).register()
    val MOB_DUPLICATOR = tileEntity(Machines, "mob_duplicator", ::MobDuplicator).blockOptions(STONE).properties(Directional.NORMAL).register()
    val MOB_KILLER = tileEntity(Machines, "mob_killer", ::MobKiller).blockOptions(STONE).properties(Directional.NORMAL).register()
    val COBBLESTONE_GENERATOR = tileEntity(Machines, "cobblestone_generator", ::CobblestoneGenerator).blockOptions(STONE).properties(Directional.NORMAL).register()
    val ELECTRIC_FURNACE = tileEntity(Machines, "electric_furnace", ::ElectricFurnace).blockOptions(STONE).properties(Directional.NORMAL).register()
    val MECHANICAL_PRESS = tileEntity(Machines, "mechanical_press", ::MechanicalPress).blockOptions(STONE).properties(Directional.NORMAL).register()
    val PULVERIZER = tileEntity(Machines, "pulverizer", ::Pulverizer).blockOptions(STONE).properties(Directional.NORMAL).register()
    val BLOCK_BREAKER = tileEntity(Machines, "block_breaker", ::BlockBreaker).blockOptions(STONE).properties(Directional.NORMAL).register()
    val BLOCK_PLACER = tileEntity(Machines, "block_placer", ::BlockPlacer).blockOptions(STONE).properties(Directional.NORMAL).register()
    val STAR_COLLECTOR = tileEntity(Machines, "star_collector", ::StarCollector).blockOptions(STONE).properties(LegacyDirectional).register()
    val CHUNK_LOADER = tileEntity(Machines, "chunk_loader", ::ChunkLoader).blockOptions(STONE).properties(Directional.NORMAL).register()
    val QUARRY = tileEntity(Machines, "quarry", ::Quarry).blockOptions(STONE).properties(Directional.NORMAL).placeCheck(Quarry::canPlace).register()
    val ELECTRIC_BREWING_STAND = tileEntity(Machines, "electric_brewing_stand", ::ElectricBrewingStand).blockOptions(STONE).properties(Directional.NORMAL).register()
    val PUMP = tileEntity(Machines, "pump", ::Pump).blockOptions(STONE).properties(LegacyDirectional).register()
    val FREEZER = tileEntity(Machines, "freezer", ::Freezer).blockOptions(STONE).properties(Directional.NORMAL).register()
    val FLUID_INFUSER = tileEntity(Machines, "fluid_infuser", ::FluidInfuser).blockOptions(STONE).properties(Directional.NORMAL).register()
    val SPRINKLER = tileEntity(Machines, "sprinkler", ::Sprinkler).blockOptions(LIGHT_METAL).properties(LegacyDirectional).register()
    val SOLAR_PANEL = tileEntity(Machines, "solar_panel", ::SolarPanel).blockOptions(STONE).properties(Directional.NORMAL).register()
    val LIGHTNING_EXCHANGER = tileEntity(Machines, "lightning_exchanger", ::LightningExchanger).blockOptions(STONE).properties(LegacyDirectional).register()
    val WIND_TURBINE = tileEntity(Machines, "wind_turbine", ::WindTurbine).blockOptions(METAL).properties(Directional.NORMAL).placeCheck(WindTurbine::canPlace).multiBlockLoader(WindTurbine::loadMultiBlock).register()
    val FURNACE_GENERATOR = tileEntity(Machines, "furnace_generator", ::FurnaceGenerator).blockOptions(STONE).properties(Directional.NORMAL).register()
    val LAVA_GENERATOR = tileEntity(Machines, "lava_generator", ::LavaGenerator).blockOptions(STONE).properties(Directional.NORMAL).register()
    val INFINITE_WATER_SOURCE = tileEntity(Machines, "infinite_water_source", ::InfiniteWaterSource).blockOptions(SANDSTONE).properties(LegacyDirectional).register()
    val CRYSTALLIZER = tileEntity(Machines, "crystallizer", ::Crystallizer).blockOptions(STONE).register()
    
    // Normal blocks
    val STAR_DUST_BLOCK = block(Machines, "star_dust_block").blockOptions(SAND).register()
    val BASIC_MACHINE_FRAME = block(Machines, "basic_machine_frame").blockOptions(MACHINE_FRAME).register()
    val ADVANCED_MACHINE_FRAME = block(Machines, "advanced_machine_frame").blockOptions(MACHINE_FRAME).register()
    val ELITE_MACHINE_FRAME = block(Machines, "elite_machine_frame").blockOptions(MACHINE_FRAME).register()
    val ULTIMATE_MACHINE_FRAME = block(Machines, "ultimate_machine_frame").blockOptions(MACHINE_FRAME).register()
    val CREATIVE_MACHINE_FRAME = block(Machines, "creative_machine_frame").blockOptions(MACHINE_FRAME).register()
    
    // Ores
    val STAR_SHARDS_ORE = block(Machines, "star_shards_ore").blockOptions(STONE_ORE).block(StarShardsOre).register()
    val DEEPSLATE_STAR_SHARDS_ORE = block(Machines, "deepslate_star_shards_ore").blockOptions(DEEPSLATE_ORE).block(StarShardsOre).register()
    
    // Tree Miniatures
    val OAK_TREE_MINIATURE = registerUnnamedItem(Machines, "oak_tree_miniature")
    val SPRUCE_TREE_MINIATURE = registerUnnamedItem(Machines, "spruce_tree_miniature")
    val BIRCH_TREE_MINIATURE = registerUnnamedItem(Machines, "birch_tree_miniature")
    val JUNGLE_TREE_MINIATURE = registerUnnamedItem(Machines, "jungle_tree_miniature")
    val ACACIA_TREE_MINIATURE = registerUnnamedItem(Machines, "acacia_tree_miniature")
    val DARK_OAK_TREE_MINIATURE = registerUnnamedItem(Machines, "dark_oak_tree_miniature")
    val MANGROVE_TREE_MINIATURE = registerUnnamedItem(Machines, "mangrove_tree_miniature")
    val CRIMSON_TREE_MINIATURE = registerUnnamedItem(Machines, "crimson_tree_miniature")
    val WARPED_TREE_MINIATURE = registerUnnamedItem(Machines, "warped_tree_miniature")
    val GIANT_RED_MUSHROOM_MINIATURE = registerUnnamedItem(Machines, "giant_red_mushroom_miniature")
    val GIANT_BROWN_MUSHROOM_MINIATURE = registerUnnamedItem(Machines, "giant_brown_mushroom_miniature")
    
    // Water levels
    val COBBLESTONE_GENERATOR_WATER_LEVELS = registerUnnamedItem(Machines, "cobblestone_generator_water_levels")
    val COBBLESTONE_GENERATOR_LAVA_LEVELS = registerUnnamedItem(Machines, "cobblestone_generator_lava_levels")
    
    fun init() = Unit
    
}
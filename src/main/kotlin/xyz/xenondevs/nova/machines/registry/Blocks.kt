package xyz.xenondevs.nova.machines.registry

import org.bukkit.Material.*
import xyz.xenondevs.nova.machines.MACHINES
import xyz.xenondevs.nova.machines.tileentity.agriculture.*
import xyz.xenondevs.nova.machines.tileentity.energy.*
import xyz.xenondevs.nova.machines.tileentity.mob.Breeder
import xyz.xenondevs.nova.machines.tileentity.mob.MobDuplicator
import xyz.xenondevs.nova.machines.tileentity.mob.MobKiller
import xyz.xenondevs.nova.machines.tileentity.processing.*
import xyz.xenondevs.nova.machines.tileentity.processing.brewing.ElectricBrewingStand
import xyz.xenondevs.nova.machines.tileentity.world.*
import xyz.xenondevs.nova.material.NovaMaterialRegistry.registerEnergyTileEntity
import xyz.xenondevs.nova.material.NovaMaterialRegistry.registerItem
import xyz.xenondevs.nova.material.NovaMaterialRegistry.registerTileEntity
import xyz.xenondevs.nova.tileentity.network.energy.holder.EnergyHolder
import xyz.xenondevs.nova.tileentity.network.fluid.holder.NovaFluidHolder

object Blocks {
    
    val AUTO_FISHER = registerEnergyTileEntity(MACHINES, "auto_fisher", ::AutoFisher, COBBLESTONE)
    val FERTILIZER = registerEnergyTileEntity(MACHINES, "fertilizer", ::Fertilizer, COBBLESTONE)
    val HARVESTER = registerEnergyTileEntity(MACHINES, "harvester", ::Harvester, COBBLESTONE)
    val PLANTER = registerEnergyTileEntity(MACHINES, "planter", ::Planter, COBBLESTONE)
    val TREE_FACTORY = registerEnergyTileEntity(MACHINES, "tree_factory", ::TreeFactory, BARRIER)
    val CHARGER = registerEnergyTileEntity(MACHINES, "charger", ::Charger, COBBLESTONE)
    val WIRELESS_CHARGER = registerEnergyTileEntity(MACHINES, "wireless_charger", ::WirelessCharger, COBBLESTONE)
    val BREEDER = registerEnergyTileEntity(MACHINES, "breeder", ::Breeder, COBBLESTONE)
    val MOB_DUPLICATOR = registerEnergyTileEntity(MACHINES, "mob_duplicator", ::MobDuplicator, COBBLESTONE)
    val MOB_KILLER = registerEnergyTileEntity(MACHINES, "mob_killer", ::MobKiller, COBBLESTONE)
    val COBBLESTONE_GENERATOR = registerEnergyTileEntity(MACHINES, "cobblestone_generator", ::CobblestoneGenerator, BARRIER)
    val ELECTRIC_FURNACE = registerEnergyTileEntity(MACHINES, "electric_furnace", ::ElectricFurnace, COBBLESTONE)
    val MECHANICAL_PRESS = registerEnergyTileEntity(MACHINES, "mechanical_press", ::MechanicalPress, COBBLESTONE)
    val PULVERIZER = registerEnergyTileEntity(MACHINES, "pulverizer", ::Pulverizer, COBBLESTONE)
    val BLOCK_BREAKER = registerEnergyTileEntity(MACHINES, "block_breaker", ::BlockBreaker, COBBLESTONE)
    val BLOCK_PLACER = registerEnergyTileEntity(MACHINES, "block_placer", ::BlockPlacer, COBBLESTONE)
    val STAR_COLLECTOR = registerEnergyTileEntity(MACHINES, "star_collector", ::StarCollector, BARRIER)
    val CHUNK_LOADER = registerEnergyTileEntity(MACHINES, "chunk_loader", ::ChunkLoader, COBBLESTONE)
    val QUARRY = registerEnergyTileEntity(MACHINES, "quarry", ::Quarry, COBBLESTONE, Quarry::canPlace)
    val ELECTRIC_BREWING_STAND = registerTileEntity(MACHINES, "electric_brewing_stand", ::ElectricBrewingStand, BARRIER, listOf(EnergyHolder::modifyItemBuilder, NovaFluidHolder::modifyItemBuilder))
    val PUMP = registerTileEntity(MACHINES, "pump", ::Pump, BARRIER, listOf(EnergyHolder::modifyItemBuilder, NovaFluidHolder::modifyItemBuilder))
    val FREEZER = registerTileEntity(MACHINES, "freezer", ::Freezer, COBBLESTONE, listOf(EnergyHolder::modifyItemBuilder, NovaFluidHolder::modifyItemBuilder))
    val FLUID_INFUSER = registerTileEntity(MACHINES, "fluid_infuser", ::FluidInfuser, COBBLESTONE, listOf(EnergyHolder::modifyItemBuilder, NovaFluidHolder::modifyItemBuilder))
    val SPRINKLER = registerTileEntity(MACHINES, "sprinkler", ::Sprinkler, BARRIER, listOf(NovaFluidHolder::modifyItemBuilder))
    val SOLAR_PANEL = registerEnergyTileEntity(MACHINES, "solar_panel", ::SolarPanel, BARRIER)
    val LIGHTNING_EXCHANGER = registerEnergyTileEntity(MACHINES, "lightning_exchanger", ::LightningExchanger, BARRIER)
    val WIND_TURBINE = registerEnergyTileEntity(MACHINES, "wind_turbine", ::WindTurbine, BARRIER, WindTurbine::canPlace)
    val FURNACE_GENERATOR = registerEnergyTileEntity(MACHINES, "furnace_generator", ::FurnaceGenerator, COBBLESTONE)
    val LAVA_GENERATOR = registerTileEntity(MACHINES, "lava_generator", ::LavaGenerator, COBBLESTONE, listOf(EnergyHolder::modifyItemBuilder, NovaFluidHolder::modifyItemBuilder))
    val INFINITE_WATER_SOURCE = registerTileEntity(MACHINES, "infinite_water_source", ::InfiniteWaterSource, SANDSTONE)
    
    // Tree Miniatures
    val OAK_TREE_MINIATURE = registerItem(MACHINES, "oak_tree_miniature")
    val SPRUCE_TREE_MINIATURE = registerItem(MACHINES, "spruce_tree_miniature")
    val BIRCH_TREE_MINIATURE = registerItem(MACHINES, "birch_tree_miniature")
    val JUNGLE_TREE_MINIATURE = registerItem(MACHINES, "jungle_tree_miniature")
    val ACACIA_TREE_MINIATURE = registerItem(MACHINES, "acacia_tree_miniature")
    val DARK_OAK_TREE_MINIATURE = registerItem(MACHINES, "dark_oak_tree_miniature")
    val CRIMSON_TREE_MINIATURE = registerItem(MACHINES, "crimson_tree_miniature")
    val WARPED_TREE_MINIATURE = registerItem(MACHINES, "warped_tree_miniature")
    val GIANT_RED_MUSHROOM_MINIATURE = registerItem(MACHINES, "giant_red_mushroom_miniature")
    val GIANT_BROWN_MUSHROOM_MINIATURE = registerItem(MACHINES, "giant_brown_mushroom_miniature")
    
    // Move these somewhere else?
    val COBBLESTONE_GENERATOR_WATER_LEVELS = registerItem(MACHINES, "cobblestone_generator_water_levels")
    val COBBLESTONE_GENERATOR_LAVA_LEVELS = registerItem(MACHINES, "cobblestone_generator_lava_levels")
    
    fun init() = Unit
    
}
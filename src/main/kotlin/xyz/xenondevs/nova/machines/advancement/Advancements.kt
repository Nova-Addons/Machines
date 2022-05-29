package xyz.xenondevs.nova.machines.advancement

import net.md_5.bungee.api.chat.TranslatableComponent
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import xyz.xenondevs.kadvancements.adapter.version.AdvancementLoader
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.machines.MACHINES
import xyz.xenondevs.nova.machines.registry.Blocks
import xyz.xenondevs.nova.machines.registry.Items
import xyz.xenondevs.nova.util.*

private val ROOT = advancement(MACHINES, "root") {
    display {
        icon(Blocks.QUARRY)
        title(TranslatableComponent("advancement.machines.root.title"))
        description("")
        background("minecraft:textures/block/tuff.png")
        
        announceToChat(false)
        showToast(false)
    }
    
    criteria { impossible() }
}

//<editor-fold desc="Power Generation" defaultstate="collapsed">
private val FURNACE_GENERATOR = obtainNovaItemAdvancement(MACHINES, ROOT, Blocks.FURNACE_GENERATOR)
private val LAVA_GENERATOR = obtainNovaItemAdvancement(MACHINES, FURNACE_GENERATOR, Blocks.LAVA_GENERATOR)
private val SOLAR_PANEL = obtainNovaItemAdvancement(MACHINES, LAVA_GENERATOR, Blocks.SOLAR_PANEL)
private val WIND_TURBINE = obtainNovaItemAdvancement(MACHINES, SOLAR_PANEL, Blocks.WIND_TURBINE)
private val LIGHTNING_EXCHANGER = obtainNovaItemAdvancement(MACHINES, WIND_TURBINE, Blocks.LIGHTNING_EXCHANGER)
//</editor-fold>

//<editor-fold desc="Farming" defaultstate="collapsed">
private val PLANTER = obtainNovaItemAdvancement(MACHINES, ROOT, Blocks.PLANTER)
private val SPRINKLER = obtainNovaItemAdvancement(MACHINES, PLANTER, Blocks.SPRINKLER)
private val FERTILIZER = obtainNovaItemAdvancement(MACHINES, SPRINKLER, Blocks.FERTILIZER)
private val HARVESTER = obtainNovaItemAdvancement(MACHINES, FERTILIZER, Blocks.HARVESTER)
private val TREE_FACTORY = obtainNovaItemAdvancement(MACHINES, HARVESTER, Blocks.TREE_FACTORY)
//</editor-fold>

//<editor-fold desc="Mobs" defaultstate="collapsed">
private val MOB_CATCHER = obtainNovaItemAdvancement(MACHINES, ROOT, Items.MOB_CATCHER)
private val BREEDER = obtainNovaItemAdvancement(MACHINES, MOB_CATCHER, Blocks.BREEDER)
private val MOB_KILLER = obtainNovaItemAdvancement(MACHINES, BREEDER, Blocks.MOB_KILLER)
private val MOB_DUPLICATOR = obtainNovaItemAdvancement(MACHINES, MOB_KILLER, Blocks.MOB_DUPLICATOR)
//</editor-fold>

//<editor-fold desc="Blocks" defaultstate="collapsed">
private val BLOCK_PLACER = obtainNovaItemAdvancement(MACHINES, ROOT, Blocks.BLOCK_PLACER)
private val BLOCK_BREAKER = obtainNovaItemAdvancement(MACHINES, BLOCK_PLACER, Blocks.BLOCK_BREAKER)
private val QUARRY = obtainNovaItemAdvancement(MACHINES, BLOCK_BREAKER, Blocks.QUARRY)
//</editor-fold>

//<editor-fold desc="Star Shards" defaultstate="collapsed">
private val STAR_SHARDS = obtainNovaItemAdvancement(MACHINES, ROOT, Items.STAR_SHARDS)
private val STAR_COLLECTOR = obtainNovaItemAdvancement(MACHINES, STAR_SHARDS, Blocks.STAR_COLLECTOR)
//</editor-fold>

//<editor-fold desc="Fluids" defaultstate="collapsed">
private val PUMP = obtainNovaItemAdvancement(MACHINES, ROOT, Blocks.PUMP)
private val COBBLESTONE_GENERATOR = obtainNovaItemAdvancement(MACHINES, PUMP, Blocks.COBBLESTONE_GENERATOR)
private val FREEZER = obtainNovaItemAdvancement(MACHINES, COBBLESTONE_GENERATOR, Blocks.FREEZER)
private val FLUID_INFUSER = obtainNovaItemAdvancement(MACHINES, PUMP, Blocks.FLUID_INFUSER)
private val ELECTRIC_BREWING_STAND = obtainNovaItemAdvancement(MACHINES, FLUID_INFUSER, Blocks.ELECTRIC_BREWING_STAND)
//</editor-fold>

//<editor-fold desc="Pulverizing" defaultstate="collapsed">
private val PULVERIZER = obtainNovaItemAdvancement(MACHINES, ROOT, Blocks.PULVERIZER)
private val DUST = obtainNovaItemsAdvancement(MACHINES, "dust", PULVERIZER, listOf(
    Items.IRON_DUST, Items.GOLD_DUST, Items.DIAMOND_DUST,
    Items.NETHERITE_DUST, Items.EMERALD_DUST, Items.LAPIS_DUST,
    Items.COAL_DUST, Items.COPPER_DUST, Items.STAR_DUST
), false)
private val ALL_DUSTS = obtainNovaItemsAdvancement(MACHINES, "all_dusts", DUST, listOf(
    Items.DIAMOND_DUST, Items.IRON_DUST, Items.GOLD_DUST,
    Items.NETHERITE_DUST, Items.EMERALD_DUST, Items.LAPIS_DUST,
    Items.COAL_DUST, Items.COPPER_DUST, Items.STAR_DUST
), true)
//</editor-fold>

//<editor-fold desc="Mechanical Press" defaultstate="collapsed">
private val MECHANICAL_PRESS = obtainNovaItemAdvancement(MACHINES, ROOT, Blocks.MECHANICAL_PRESS)
private val GEAR = obtainNovaItemsAdvancement(MACHINES, "gears", MECHANICAL_PRESS, listOf(
    Items.IRON_GEAR, Items.GOLD_GEAR, Items.DIAMOND_GEAR,
    Items.NETHERITE_GEAR, Items.EMERALD_GEAR, Items.LAPIS_GEAR,
    Items.REDSTONE_GEAR, Items.COPPER_GEAR
), false)
private val ALL_GEARS = obtainNovaItemsAdvancement(MACHINES, "all_gears", GEAR, listOf(
    Items.DIAMOND_GEAR, Items.IRON_GEAR, Items.GOLD_GEAR,
    Items.NETHERITE_GEAR, Items.EMERALD_GEAR, Items.LAPIS_GEAR,
    Items.REDSTONE_GEAR, Items.COPPER_GEAR
), true)
private val PLATE = obtainNovaItemsAdvancement(MACHINES, "plates", MECHANICAL_PRESS, listOf(
    Items.IRON_PLATE, Items.GOLD_PLATE, Items.DIAMOND_PLATE,
    Items.NETHERITE_PLATE, Items.EMERALD_PLATE, Items.LAPIS_PLATE,
    Items.REDSTONE_PLATE, Items.COPPER_PLATE
), false)
private val ALL_PLATES = obtainNovaItemsAdvancement(MACHINES, "all_plates", PLATE, listOf(
    Items.DIAMOND_PLATE, Items.IRON_PLATE, Items.GOLD_PLATE,
    Items.NETHERITE_PLATE, Items.EMERALD_PLATE, Items.LAPIS_PLATE,
    Items.REDSTONE_PLATE, Items.COPPER_PLATE
), true)
//</editor-fold>

//<editor-fold desc="Charger" defaultstate="collapsed">
private val CHARGER = obtainNovaItemAdvancement(MACHINES, ROOT, Blocks.CHARGER)
private val WIRELESS_CHARGER = obtainNovaItemAdvancement(MACHINES, CHARGER, Blocks.WIRELESS_CHARGER)
//</editor-fold>

//<editor-fold desc="Miscellaneous" defaultstate="collapsed">
private val AUTO_FISHER = obtainNovaItemAdvancement(MACHINES, ROOT, Blocks.AUTO_FISHER)
//</editor-fold>

object Advancements : Listener {
    
    init {
        Bukkit.getPluginManager().registerEvents(this, NOVA)
    }
    
    @EventHandler
    private fun handleJoin(event: PlayerJoinEvent) {
        event.player.awardAdvancement(NamespacedKey.fromString(ROOT.id)!!)
    }
    
    fun register() {
        AdvancementLoader.registerAdvancements(
            ROOT, FURNACE_GENERATOR, LAVA_GENERATOR, SOLAR_PANEL, WIND_TURBINE, LIGHTNING_EXCHANGER, PLANTER,
            SPRINKLER, FERTILIZER, HARVESTER, TREE_FACTORY, MOB_CATCHER, BREEDER, MOB_KILLER, MOB_DUPLICATOR,
            BLOCK_PLACER, BLOCK_BREAKER, QUARRY, STAR_SHARDS, STAR_COLLECTOR, PUMP, COBBLESTONE_GENERATOR,
            FLUID_INFUSER, ELECTRIC_BREWING_STAND, PULVERIZER, DUST, ALL_DUSTS, GEAR, ALL_GEARS, PLATE, ALL_PLATES,
            MECHANICAL_PRESS, GEAR, ALL_GEARS, PLATE, ALL_PLATES, CHARGER, WIRELESS_CHARGER, AUTO_FISHER
        )
    }
    
}
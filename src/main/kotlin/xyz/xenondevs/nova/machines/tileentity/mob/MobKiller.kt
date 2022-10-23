package xyz.xenondevs.nova.machines.tileentity.mob

import de.studiocode.invui.gui.GUI
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.guitype.GUIType
import de.studiocode.invui.item.Item
import de.studiocode.invui.item.builder.ItemBuilder
import net.md_5.bungee.api.ChatColor
import org.bukkit.entity.Mob
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.data.config.configReloadable
import xyz.xenondevs.nova.data.world.block.state.NovaTileEntityState
import xyz.xenondevs.nova.integration.protection.ProtectionManager
import xyz.xenondevs.nova.machines.registry.Blocks.MOB_KILLER
import xyz.xenondevs.nova.material.CoreGUIMaterial
import xyz.xenondevs.nova.tileentity.NetworkedTileEntity
import xyz.xenondevs.nova.tileentity.network.NetworkConnectionType
import xyz.xenondevs.nova.tileentity.network.energy.holder.ConsumerEnergyHolder
import xyz.xenondevs.nova.tileentity.upgrade.Upgradable
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeType
import xyz.xenondevs.nova.ui.EnergyBar
import xyz.xenondevs.nova.ui.OpenUpgradesItem
import xyz.xenondevs.nova.ui.VerticalBar
import xyz.xenondevs.nova.ui.config.side.OpenSideConfigItem
import xyz.xenondevs.nova.ui.config.side.SideConfigGUI
import xyz.xenondevs.nova.ui.item.AddNumberItem
import xyz.xenondevs.nova.ui.item.DisplayNumberItem
import xyz.xenondevs.nova.ui.item.RemoveNumberItem
import xyz.xenondevs.nova.ui.item.VisualizeRegionItem
import xyz.xenondevs.nova.util.EntityUtils
import xyz.xenondevs.nova.util.data.localized
import xyz.xenondevs.nova.world.region.Region
import xyz.xenondevs.nova.world.region.VisualRegion
import java.util.*
import kotlin.math.min

private val MAX_ENERGY = configReloadable { NovaConfig[MOB_KILLER].getLong("capacity") }
private val ENERGY_PER_TICK = configReloadable { NovaConfig[MOB_KILLER].getLong("energy_per_tick") }
private val ENERGY_PER_DAMAGE = configReloadable { NovaConfig[MOB_KILLER].getLong("energy_per_damage") }
private val IDLE_TIME by configReloadable { NovaConfig[MOB_KILLER].getInt("idle_time") }
private val KILL_LIMIT by configReloadable { NovaConfig[MOB_KILLER].getInt("kill_limit") }
private val DAMAGE by configReloadable { NovaConfig[MOB_KILLER].getDouble("damage") }
private val MIN_RANGE by configReloadable { NovaConfig[MOB_KILLER].getInt("range.min") }
private val MAX_RANGE by configReloadable { NovaConfig[MOB_KILLER].getInt("range.max") }
private val DEFAULT_RANGE by configReloadable { NovaConfig[MOB_KILLER].getInt("range.default") }

class MobKiller(blockState: NovaTileEntityState) : NetworkedTileEntity(blockState), Upgradable {
    
    override val gui = lazy { MobCrusherGUI() }
    override val upgradeHolder = getUpgradeHolder(UpgradeType.SPEED, UpgradeType.EFFICIENCY, UpgradeType.ENERGY, UpgradeType.RANGE)
    override val energyHolder = ConsumerEnergyHolder(this, MAX_ENERGY, ENERGY_PER_TICK, ENERGY_PER_DAMAGE, upgradeHolder) { createSideConfig(NetworkConnectionType.INSERT) }
    private val fakePlayer = EntityUtils.createFakePlayer(location, ownerUUID ?: UUID.randomUUID(), "Mob Killer").bukkitEntity
    
    private var timePassed = 0
    private var maxIdleTime = 0
    private var maxRange = 0
    private var range = retrieveData("range") { DEFAULT_RANGE }
        set(value) {
            field = value
            updateRegion()
            if (gui.isInitialized()) gui.value.updateRangeItems()
        }
    private lateinit var region: Region
    
    init {
        reload()
        updateRegion()
    }
    
    override fun reload() {
        super.reload()
        
        maxIdleTime = (IDLE_TIME / upgradeHolder.getValue(UpgradeType.SPEED)).toInt()
        if (timePassed > maxIdleTime) timePassed = maxIdleTime
        
        maxRange = MAX_RANGE + upgradeHolder.getValue(UpgradeType.RANGE)
        if (range > maxRange) range = maxRange
    }
    
    override fun saveData() {
        super.saveData()
        storeData("range", range)
    }
    
    private fun updateRegion() {
        region = getBlockFrontRegion(range, range, 4, -1)
        VisualRegion.updateRegion(uuid, region)
    }
    
    override fun handleTick() {
        if (energyHolder.energy >= energyHolder.energyConsumption) {
            energyHolder.energy -= energyHolder.energyConsumption
            
            if (timePassed++ >= maxIdleTime) {
                timePassed = 0
                
                val killLimit = min((energyHolder.energy / energyHolder.specialEnergyConsumption).toInt(), KILL_LIMIT)
                
                location.world!!.entities
                    .asSequence()
                    .filterIsInstance<Mob>()
                    .filter { it.location in region && ProtectionManager.canHurtEntity(this, it, null).get() }
                    .take(killLimit)
                    .forEach { entity ->
                        energyHolder.energy -= energyHolder.specialEnergyConsumption
                        entity.damage(DAMAGE, fakePlayer)
                    }
            }
        }
        
        if (gui.isInitialized())
            gui.value.idleBar.percentage = timePassed / maxIdleTime.toDouble()
    }
    
    override fun handleRemoved(unload: Boolean) {
        super.handleRemoved(unload)
        VisualRegion.removeRegion(uuid)
    }
    
    inner class MobCrusherGUI : TileEntityGUI() {
        
        private val sideConfigGUI = SideConfigGUI(
            this@MobKiller,
            ::openWindow
        )
        
        private val rangeItems = ArrayList<Item>()
        
        val idleBar = object : VerticalBar(3) {
            override val barMaterial = CoreGUIMaterial.BAR_GREEN
            override fun modifyItemBuilder(itemBuilder: ItemBuilder) =
                itemBuilder.setDisplayName(localized(ChatColor.GRAY, "menu.machines.mob_killer.idle", maxIdleTime - timePassed))
        }
        
        override val gui: GUI = GUIBuilder(GUIType.NORMAL)
            .setStructure(
                "1 - - - - - - - 2",
                "| s # i # e # p |",
                "| r # i # e # n |",
                "| u # i # e # m |",
                "3 - - - - - - - 4")
            .addIngredient('s', OpenSideConfigItem(sideConfigGUI))
            .addIngredient('r', VisualizeRegionItem(uuid) { region })
            .addIngredient('u', OpenUpgradesItem(upgradeHolder))
            .addIngredient('p', AddNumberItem({ MIN_RANGE..maxRange }, { range }, { range = it }).also(rangeItems::add))
            .addIngredient('m', RemoveNumberItem({ MIN_RANGE..maxRange }, { range }, { range = it }).also(rangeItems::add))
            .addIngredient('n', DisplayNumberItem { range }.also(rangeItems::add))
            .addIngredient('e', EnergyBar(3, energyHolder))
            .addIngredient('i', idleBar)
            .build()
        
        fun updateRangeItems() = rangeItems.forEach(Item::notifyWindows)
        
    }
    
}
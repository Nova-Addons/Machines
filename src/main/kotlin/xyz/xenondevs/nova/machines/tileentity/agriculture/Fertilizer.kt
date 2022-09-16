package xyz.xenondevs.nova.machines.tileentity.agriculture

import de.studiocode.invui.gui.GUI
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.guitype.GUIType
import de.studiocode.invui.item.Item
import de.studiocode.invui.virtualinventory.event.ItemUpdateEvent
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.data.Ageable
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.data.config.configReloadable
import xyz.xenondevs.nova.data.world.block.state.NovaTileEntityState
import xyz.xenondevs.nova.integration.protection.ProtectionManager
import xyz.xenondevs.nova.machines.registry.Blocks.FERTILIZER
import xyz.xenondevs.nova.tileentity.NetworkedTileEntity
import xyz.xenondevs.nova.tileentity.network.NetworkConnectionType
import xyz.xenondevs.nova.tileentity.network.energy.holder.ConsumerEnergyHolder
import xyz.xenondevs.nova.tileentity.network.item.holder.NovaItemHolder
import xyz.xenondevs.nova.tileentity.upgrade.Upgradable
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeType
import xyz.xenondevs.nova.ui.EnergyBar
import xyz.xenondevs.nova.ui.OpenUpgradesItem
import xyz.xenondevs.nova.ui.config.side.OpenSideConfigItem
import xyz.xenondevs.nova.ui.config.side.SideConfigGUI
import xyz.xenondevs.nova.ui.item.AddNumberItem
import xyz.xenondevs.nova.ui.item.DisplayNumberItem
import xyz.xenondevs.nova.ui.item.RemoveNumberItem
import xyz.xenondevs.nova.ui.item.VisualizeRegionItem
import xyz.xenondevs.nova.util.BlockSide
import xyz.xenondevs.nova.util.item.PlantUtils
import xyz.xenondevs.nova.util.item.isFullyAged
import xyz.xenondevs.nova.world.region.Region
import xyz.xenondevs.nova.world.region.VisualRegion

private val MAX_ENERGY = configReloadable { NovaConfig[FERTILIZER].getLong("capacity") }
private val ENERGY_PER_TICK = configReloadable { NovaConfig[FERTILIZER].getLong("energy_per_tick") }
private val ENERGY_PER_FERTILIZE = configReloadable { NovaConfig[FERTILIZER].getLong("energy_per_fertilize") }
private val IDLE_TIME by configReloadable { NovaConfig[FERTILIZER].getInt("idle_time") }
private val MIN_RANGE by configReloadable { NovaConfig[FERTILIZER].getInt("range.min") }
private val MAX_RANGE by configReloadable { NovaConfig[FERTILIZER].getInt("range.max") }
private val DEFAULT_RANGE by configReloadable { NovaConfig[FERTILIZER].getInt("range.default") }

class Fertilizer(blockState: NovaTileEntityState) : NetworkedTileEntity(blockState), Upgradable {
    
    private val fertilizerInventory = getInventory("fertilizer", 12, ::handleFertilizerUpdate)
    override val gui = lazy(::FertilizerGUI)
    override val upgradeHolder = getUpgradeHolder(UpgradeType.SPEED, UpgradeType.EFFICIENCY, UpgradeType.ENERGY, UpgradeType.RANGE)
    override val energyHolder = ConsumerEnergyHolder(this, MAX_ENERGY, ENERGY_PER_TICK, ENERGY_PER_FERTILIZE, upgradeHolder) { createSideConfig(NetworkConnectionType.INSERT, BlockSide.FRONT) }
    override val itemHolder = NovaItemHolder(this, fertilizerInventory to NetworkConnectionType.BUFFER) { createSideConfig(NetworkConnectionType.INSERT, BlockSide.FRONT) }
    
    private var maxIdleTime = 0
    private var timePassed = 0
    private var maxRange = 0
    private var range = retrieveData("range") { DEFAULT_RANGE }
        set(value) {
            field = value
            updateRegion()
            if (gui.isInitialized()) gui.value.updateRangeItems()
        }
    private lateinit var fertilizeRegion: Region
    
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
    
    private fun updateRegion() {
        fertilizeRegion = getBlockFrontRegion(range, range, 1, 0)
        VisualRegion.updateRegion(uuid, fertilizeRegion)
    }
    
    override fun saveData() {
        super.saveData()
        storeData("range", range)
    }
    
    override fun handleTick() {
        if (energyHolder.energy >= energyHolder.energyConsumption) {
            energyHolder.energy -= energyHolder.energyConsumption
            if (energyHolder.energy >= energyHolder.specialEnergyConsumption) {
                if (timePassed++ >= maxIdleTime) {
                    timePassed = 0
                    if (!fertilizerInventory.isEmpty)
                        fertilizeNextPlant()
                }
            }
        }
    }
    
    private fun fertilizeNextPlant() {
        for ((index, item) in fertilizerInventory.items.withIndex()) {
            if (item == null) continue
            val plant = getNextPlant() ?: return
            PlantUtils.fertilize(plant)
            
            energyHolder.energy -= energyHolder.specialEnergyConsumption
            fertilizerInventory.addItemAmount(SELF_UPDATE_REASON, index, -1)
            break
        }
    }
    
    private fun getNextPlant(): Block? =
        fertilizeRegion.blocks
            .firstOrNull {
                (it.blockData is Ageable && !it.isFullyAged())
                    && ProtectionManager.canUseBlock(this, ItemStack(Material.BONE_MEAL), it.location).get()
            }
    
    private fun handleFertilizerUpdate(event: ItemUpdateEvent) {
        if ((event.isAdd || event.isSwap) && event.newItemStack.type != Material.BONE_MEAL)
            event.isCancelled = true
    }
    
    override fun handleRemoved(unload: Boolean) {
        super.handleRemoved(unload)
        VisualRegion.removeRegion(uuid)
    }
    
    inner class FertilizerGUI : TileEntityGUI() {
        
        private val sideConfigGUI = SideConfigGUI(
            this@Fertilizer,
            listOf(itemHolder.getNetworkedInventory(fertilizerInventory) to "inventory.machines.fertilizer"),
            ::openWindow
        )
        
        private val rangeItems = ArrayList<Item>()
        
        override val gui: GUI = GUIBuilder(GUIType.NORMAL)
            .setStructure(
                "1 - - - - - - - 2",
                "| s p i i i i e |",
                "| v n i i i i e |",
                "| u m i i i i e |",
                "3 - - - - - - - 4")
            .addIngredient('i', fertilizerInventory)
            .addIngredient('v', VisualizeRegionItem(uuid) { fertilizeRegion })
            .addIngredient('s', OpenSideConfigItem(sideConfigGUI))
            .addIngredient('u', OpenUpgradesItem(upgradeHolder))
            .addIngredient('p', AddNumberItem({ MIN_RANGE..maxRange }, { range }, { range = it }).also(rangeItems::add))
            .addIngredient('m', RemoveNumberItem({ MIN_RANGE..maxRange }, { range }, { range = it }).also(rangeItems::add))
            .addIngredient('n', DisplayNumberItem { range }.also(rangeItems::add))
            .addIngredient('e', EnergyBar(3, energyHolder))
            .build()
        
        fun updateRangeItems() = rangeItems.forEach(Item::notifyWindows)
        
    }
    
}

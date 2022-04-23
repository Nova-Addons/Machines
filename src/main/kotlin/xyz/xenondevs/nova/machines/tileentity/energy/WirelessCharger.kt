package xyz.xenondevs.nova.machines.tileentity.energy

import de.studiocode.invui.gui.GUI
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.guitype.GUIType
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.data.world.block.state.NovaTileEntityState
import xyz.xenondevs.nova.item.behavior.Chargeable
import xyz.xenondevs.nova.machines.registry.Blocks.WIRELESS_CHARGER
import xyz.xenondevs.nova.tileentity.NetworkedTileEntity
import xyz.xenondevs.nova.tileentity.network.NetworkConnectionType
import xyz.xenondevs.nova.tileentity.network.energy.holder.ConsumerEnergyHolder
import xyz.xenondevs.nova.tileentity.upgrade.Upgradable
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeHolder
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeType
import xyz.xenondevs.nova.ui.EnergyBar
import xyz.xenondevs.nova.ui.OpenUpgradesItem
import xyz.xenondevs.nova.ui.config.side.OpenSideConfigItem
import xyz.xenondevs.nova.ui.config.side.SideConfigGUI
import xyz.xenondevs.nova.ui.item.AddNumberItem
import xyz.xenondevs.nova.ui.item.DisplayNumberItem
import xyz.xenondevs.nova.ui.item.RemoveNumberItem
import xyz.xenondevs.nova.ui.item.VisualizeRegionItem
import xyz.xenondevs.nova.util.item.novaMaterial
import xyz.xenondevs.nova.world.region.Region
import xyz.xenondevs.nova.world.region.VisualRegion
import de.studiocode.invui.item.Item as UIItem

private val MAX_ENERGY = NovaConfig[WIRELESS_CHARGER].getLong("capacity")!!
private val CHARGE_SPEED = NovaConfig[WIRELESS_CHARGER].getLong("charge_speed")!!
private val MIN_RANGE = NovaConfig[WIRELESS_CHARGER].getInt("range.min")!!
private val MAX_RANGE = NovaConfig[WIRELESS_CHARGER].getInt("range.max")!!
private val DEFAULT_RANGE = NovaConfig[WIRELESS_CHARGER].getInt("range.default")!!

class WirelessCharger(blockState: NovaTileEntityState) : NetworkedTileEntity(blockState), Upgradable {
    
    override val gui = lazy(::WirelessChargerGUI)
    override val upgradeHolder = UpgradeHolder(this, gui, ::handleUpgradeUpdates, UpgradeType.SPEED, UpgradeType.ENERGY, UpgradeType.RANGE)
    override val energyHolder = ConsumerEnergyHolder(this, MAX_ENERGY, CHARGE_SPEED, 0, upgradeHolder) { createSideConfig(NetworkConnectionType.INSERT) }
    
    private var maxRange = 0
    private var range = retrieveData("range") { DEFAULT_RANGE }
        set(value) {
            field = value
            updateRegion()
            if (gui.isInitialized()) gui.value.updateRangeItems()
        }
    
    private lateinit var region: Region
    
    init {
        handleUpgradeUpdates()
        updateRegion()
    }
    
    private fun handleUpgradeUpdates() {
        maxRange = MAX_RANGE + upgradeHolder.getValue(UpgradeType.RANGE)
        if (maxRange < range) range = maxRange
    }
    
    private fun updateRegion() {
        region = getSurroundingRegion(range)
        VisualRegion.updateRegion(uuid, region)
    }
    
    override fun saveData() {
        super.saveData()
        storeData("range", range)
    }
    
    private var players: List<Player> = emptyList()
    private var findPlayersCooldown = 0
    
    override fun handleTick() {
        var energyTransferred: Long
        
        if (--findPlayersCooldown <= 0) {
            findPlayersCooldown = 100
            players = world.players.filter { it.location in region }
        }
        
        if (energyHolder.energy != 0L && players.isNotEmpty()) {
            playerLoop@ for (player in players) {
                energyTransferred = 0L
                if (energyHolder.energy == 0L) break
                for (itemStack in player.inventory) {
                    energyTransferred += chargeItemStack(energyTransferred, itemStack)
                    if (energyHolder.energy == 0L) break@playerLoop
                    if (energyTransferred == energyHolder.energyConsumption) break
                }
            }
        }
    }
    
    private fun chargeItemStack(alreadyTransferred: Long, itemStack: ItemStack?): Long {
        val chargeable = itemStack?.novaMaterial?.novaItem?.getBehavior(Chargeable::class)
        
        if (chargeable != null) {
            val maxEnergy = chargeable.maxEnergy
            val currentEnergy = chargeable.getEnergy(itemStack)
            
            val energyToTransfer = minOf(energyHolder.energyConsumption - alreadyTransferred, maxEnergy - currentEnergy, energyHolder.energy)
            energyHolder.energy -= energyToTransfer
            chargeable.addEnergy(itemStack, energyToTransfer)
            
            return energyToTransfer
        }
        
        return 0
    }
    
    override fun handleRemoved(unload: Boolean) {
        super.handleRemoved(unload)
        VisualRegion.removeRegion(uuid)
    }
    
    inner class WirelessChargerGUI : TileEntityGUI() {
        
        private val sideConfigGUI = SideConfigGUI(
            this@WirelessCharger,
            ::openWindow
        )
        
        private val rangeItems = ArrayList<UIItem>()
        
        override val gui: GUI = GUIBuilder(GUIType.NORMAL)
            .setStructure(
                "1 - - - - - - - 2",
                "| s # # e # # p |",
                "| v # # e # # n |",
                "| u # # e # # m |",
                "3 - - - - - - - 4")
            .addIngredient('s', OpenSideConfigItem(sideConfigGUI))
            .addIngredient('v', VisualizeRegionItem(uuid) { region })
            .addIngredient('p', AddNumberItem({ MIN_RANGE..maxRange }, { range }, { range = it }).also(rangeItems::add))
            .addIngredient('m', RemoveNumberItem({ MIN_RANGE..maxRange }, { range }, { range = it }).also(rangeItems::add))
            .addIngredient('n', DisplayNumberItem { range }.also(rangeItems::add))
            .addIngredient('u', OpenUpgradesItem(upgradeHolder))
            .addIngredient('e', EnergyBar(3, energyHolder))
            .build()
        
        fun updateRangeItems() = rangeItems.forEach(UIItem::notifyWindows)
        
    }
    
}
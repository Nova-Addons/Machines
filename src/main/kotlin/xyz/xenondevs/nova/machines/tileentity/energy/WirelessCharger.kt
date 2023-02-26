package xyz.xenondevs.nova.machines.tileentity.energy

import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.invui.gui.Gui
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.data.config.configReloadable
import xyz.xenondevs.nova.data.world.block.state.NovaTileEntityState
import xyz.xenondevs.nova.item.behavior.Chargeable
import xyz.xenondevs.nova.machines.registry.Blocks.WIRELESS_CHARGER
import xyz.xenondevs.nova.tileentity.NetworkedTileEntity
import xyz.xenondevs.nova.tileentity.network.NetworkConnectionType
import xyz.xenondevs.nova.tileentity.upgrade.Upgradable
import xyz.xenondevs.nova.ui.EnergyBar
import xyz.xenondevs.nova.ui.OpenUpgradesItem
import xyz.xenondevs.nova.ui.config.side.OpenSideConfigItem
import xyz.xenondevs.nova.ui.config.side.SideConfigGui
import xyz.xenondevs.nova.ui.item.AddNumberItem
import xyz.xenondevs.nova.ui.item.DisplayNumberItem
import xyz.xenondevs.nova.ui.item.RemoveNumberItem
import xyz.xenondevs.nova.ui.item.VisualizeRegionItem
import xyz.xenondevs.nova.util.item.novaMaterial
import xyz.xenondevs.nova.world.region.Region
import xyz.xenondevs.nova.world.region.VisualRegion
import xyz.xenondevs.simpleupgrades.ConsumerEnergyHolder
import xyz.xenondevs.simpleupgrades.registry.UpgradeTypes
import xyz.xenondevs.invui.item.Item as UIItem

private val MAX_ENERGY = configReloadable { NovaConfig[WIRELESS_CHARGER].getLong("capacity") }
private val CHARGE_SPEED = configReloadable { NovaConfig[WIRELESS_CHARGER].getLong("charge_speed") }
private val MIN_RANGE by configReloadable { NovaConfig[WIRELESS_CHARGER].getInt("range.min") }
private val MAX_RANGE by configReloadable { NovaConfig[WIRELESS_CHARGER].getInt("range.max") }
private val DEFAULT_RANGE by configReloadable { NovaConfig[WIRELESS_CHARGER].getInt("range.default") }

class WirelessCharger(blockState: NovaTileEntityState) : NetworkedTileEntity(blockState), Upgradable {
    
    override val gui = lazy(::WirelessChargerGui)
    override val upgradeHolder = getUpgradeHolder(UpgradeTypes.SPEED, UpgradeTypes.ENERGY, UpgradeTypes.RANGE)
    override val energyHolder = ConsumerEnergyHolder(this, MAX_ENERGY, CHARGE_SPEED, null, upgradeHolder) { createSideConfig(NetworkConnectionType.INSERT) }
    
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
        maxRange = MAX_RANGE + upgradeHolder.getValue(UpgradeTypes.RANGE)
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
            val maxEnergy = chargeable.options.maxEnergy
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
    
    inner class WirelessChargerGui : TileEntityGui() {
        
        private val sideConfigGui = SideConfigGui(
            this@WirelessCharger,
            ::openWindow
        )
        
        private val rangeItems = ArrayList<UIItem>()
        
        override val gui = Gui.normal()
            .setStructure(
                "1 - - - - - - - 2",
                "| s # # e # # p |",
                "| v # # e # # n |",
                "| u # # e # # m |",
                "3 - - - - - - - 4")
            .addIngredient('s', OpenSideConfigItem(sideConfigGui))
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
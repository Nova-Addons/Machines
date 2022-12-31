package xyz.xenondevs.nova.machines.tileentity.energy

import de.studiocode.invui.gui.GUI
import de.studiocode.invui.gui.SlotElement.VISlotElement
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.guitype.GUIType
import de.studiocode.invui.virtualinventory.event.ItemUpdateEvent
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.data.config.configReloadable
import xyz.xenondevs.nova.data.world.block.state.NovaTileEntityState
import xyz.xenondevs.nova.item.behavior.Chargeable
import xyz.xenondevs.nova.machines.registry.Blocks.CHARGER
import xyz.xenondevs.nova.tileentity.NetworkedTileEntity
import xyz.xenondevs.nova.tileentity.network.NetworkConnectionType
import xyz.xenondevs.nova.tileentity.network.energy.holder.ConsumerEnergyHolder
import xyz.xenondevs.nova.tileentity.network.item.holder.NovaItemHolder
import xyz.xenondevs.nova.tileentity.network.item.inventory.NetworkedVirtualInventory
import xyz.xenondevs.nova.tileentity.upgrade.Upgradable
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeType
import xyz.xenondevs.nova.ui.EnergyBar
import xyz.xenondevs.nova.ui.OpenUpgradesItem
import xyz.xenondevs.nova.ui.config.side.OpenSideConfigItem
import xyz.xenondevs.nova.ui.config.side.SideConfigGUI
import xyz.xenondevs.nova.util.item.novaMaterial

private val MAX_ENERGY = configReloadable { NovaConfig[CHARGER].getLong("capacity") }
private val ENERGY_PER_TICK = configReloadable { NovaConfig[CHARGER].getLong("charge_speed") }

class Charger(blockState: NovaTileEntityState) : NetworkedTileEntity(blockState), Upgradable {
    
    private val inventory = getInventory("inventory", 1, ::handleInventoryUpdate)
    override val gui = lazy { ChargerGUI() }
    override val upgradeHolder = getUpgradeHolder(UpgradeType.ENERGY, UpgradeType.SPEED)
    override val energyHolder = ConsumerEnergyHolder(this, MAX_ENERGY, ENERGY_PER_TICK, null, upgradeHolder) { createSideConfig(NetworkConnectionType.INSERT) }
    override val itemHolder = NovaItemHolder(this, inventory to NetworkConnectionType.BUFFER) { createSideConfig(NetworkConnectionType.BUFFER) }
    
    private fun handleInventoryUpdate(event: ItemUpdateEvent) {
        if (event.isAdd || event.isSwap) {
            // cancel adding non-chargeable or fully charged items
            val newStack = event.newItemStack
            val chargeable = newStack.novaMaterial?.novaItem?.getBehavior(Chargeable::class)
            event.isCancelled = chargeable == null || chargeable.getEnergy(newStack) >= chargeable.options.maxEnergy
        } else if (event.updateReason == NetworkedVirtualInventory.UPDATE_REASON) {
            // prevent item networks from removing not fully charged items
            val previousStack = event.previousItemStack
            val chargeable = previousStack?.novaMaterial?.novaItem?.getBehavior(Chargeable::class) ?: return
            event.isCancelled = chargeable.getEnergy(previousStack) < chargeable.options.maxEnergy
        }
    }
    
    override fun handleTick() {
        val currentItem = inventory.getUnsafeItemStack(0)
        val chargeable = currentItem?.novaMaterial?.novaItem?.getBehavior(Chargeable::class)
        if (chargeable != null) {
            val itemCharge = chargeable.getEnergy(currentItem)
            if (itemCharge < chargeable.options.maxEnergy) {
                val chargeEnergy = minOf(energyHolder.energyConsumption, energyHolder.energy, chargeable.options.maxEnergy - itemCharge)
                chargeable.addEnergy(currentItem, chargeEnergy)
                energyHolder.energy -= chargeEnergy
                
                inventory.notifyWindows()
            }
        }
    }
    
    inner class ChargerGUI : TileEntityGUI() {
        
        private val sideConfigGUI = SideConfigGUI(
            this@Charger,
            listOf(itemHolder.getNetworkedInventory(inventory) to "inventory.nova.default"),
            ::openWindow
        )
        
        override val gui: GUI = GUIBuilder(GUIType.NORMAL)
            .setStructure(
                "1 - - - - - - - 2",
                "| s # # # # # e |",
                "| u # # i # # e |",
                "| # # # # # # e |",
                "3 - - - - - - - 4")
            .addIngredient('s', OpenSideConfigItem(sideConfigGUI))
            .addIngredient('i', VISlotElement(inventory, 0))
            .addIngredient('u', OpenUpgradesItem(upgradeHolder))
            .addIngredient('e', EnergyBar(3, energyHolder))
            .build()
        
    }
    
}
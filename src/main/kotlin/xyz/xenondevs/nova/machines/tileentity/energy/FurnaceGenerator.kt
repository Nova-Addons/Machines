package xyz.xenondevs.nova.machines.tileentity.energy

import net.minecraft.core.particles.ParticleTypes
import xyz.xenondevs.invui.gui.Gui
import xyz.xenondevs.invui.gui.SlotElement
import xyz.xenondevs.invui.virtualinventory.event.ItemUpdateEvent
import xyz.xenondevs.nmsutils.particle.particle
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.data.config.configReloadable
import xyz.xenondevs.nova.data.world.block.state.NovaTileEntityState
import xyz.xenondevs.nova.machines.gui.EnergyProgressItem
import xyz.xenondevs.nova.machines.registry.Blocks.FURNACE_GENERATOR
import xyz.xenondevs.nova.tileentity.NetworkedTileEntity
import xyz.xenondevs.nova.tileentity.network.NetworkConnectionType
import xyz.xenondevs.nova.tileentity.network.item.holder.NovaItemHolder
import xyz.xenondevs.nova.tileentity.upgrade.Upgradable
import xyz.xenondevs.nova.ui.EnergyBar
import xyz.xenondevs.nova.ui.OpenUpgradesItem
import xyz.xenondevs.nova.ui.config.side.OpenSideConfigItem
import xyz.xenondevs.nova.ui.config.side.SideConfigGui
import xyz.xenondevs.nova.util.BlockSide
import xyz.xenondevs.nova.util.BlockSide.FRONT
import xyz.xenondevs.nova.util.advance
import xyz.xenondevs.nova.util.axis
import xyz.xenondevs.nova.util.intValue
import xyz.xenondevs.nova.util.item.burnTime
import xyz.xenondevs.nova.util.item.craftingRemainingItem
import xyz.xenondevs.nova.util.item.isFuel
import xyz.xenondevs.simpleupgrades.ProviderEnergyHolder
import xyz.xenondevs.simpleupgrades.registry.UpgradeTypes
import kotlin.math.min
import kotlin.math.roundToInt

private val MAX_ENERGY = configReloadable { NovaConfig[FURNACE_GENERATOR].getLong("capacity") }
private val ENERGY_PER_TICK = configReloadable { NovaConfig[FURNACE_GENERATOR].getLong("energy_per_tick") }
private val BURN_TIME_MULTIPLIER by configReloadable { NovaConfig[FURNACE_GENERATOR].getDouble("burn_time_multiplier") }

class FurnaceGenerator(blockState: NovaTileEntityState) : NetworkedTileEntity(blockState), Upgradable {
    
    override val gui = lazy { FurnaceGeneratorGui() }
    private val inventory = getInventory("fuel", 1, ::handleInventoryUpdate)
    override val upgradeHolder = getUpgradeHolder(UpgradeTypes.SPEED, UpgradeTypes.EFFICIENCY, UpgradeTypes.ENERGY)
    override val energyHolder = ProviderEnergyHolder(this, MAX_ENERGY, ENERGY_PER_TICK, upgradeHolder, UpgradeTypes.SPEED) { createSideConfig(NetworkConnectionType.EXTRACT, FRONT) }
    override val itemHolder = NovaItemHolder(this, inventory to NetworkConnectionType.INSERT) { createSideConfig(NetworkConnectionType.INSERT, FRONT) }
    
    private var burnTimeMultiplier = BURN_TIME_MULTIPLIER
    private var burnTime: Int = retrieveData("burnTime") { 0 }
    private var totalBurnTime: Int = retrieveData("totalBurnTime") { 0 }
    private var active = burnTime != 0
        set(active) {
            if (field != active) {
                field = active
                if (active) particleTask.start()
                else particleTask.stop()
                
                blockState.modelProvider.update(active.intValue)
            }
        }
    
    private val particleTask = createPacketTask(listOf(
        particle(ParticleTypes.SMOKE) {
            location(centerLocation.advance(getFace(FRONT), 0.6).apply { y += 0.8 })
            offset(getFace(BlockSide.RIGHT).axis, 0.15f)
            offsetY(0.1f)
            speed(0f)
            amount(5)
        }
    ), 1)
    
    init {
        if (active) particleTask.start()
        reload()
    }
    
    override fun reload() {
        super.reload()
        
        // percent of the burn time left
        val burnPercentage = burnTime.toDouble() / totalBurnTime.toDouble()
        // previous burn time without the burnTimeMultiplier
        val previousBurnTime = totalBurnTime.toDouble() / burnTimeMultiplier
        // calculate the new burn time multiplier based on upgrades
        burnTimeMultiplier = BURN_TIME_MULTIPLIER / upgradeHolder.getValue(UpgradeTypes.SPEED) * upgradeHolder.getValue(UpgradeTypes.EFFICIENCY)
        // set the new total burn time based on the fuel burn time and the new multiplier
        totalBurnTime = (previousBurnTime * burnTimeMultiplier).toInt()
        // set the burn time based on the calculated total burn time and the percentage of burn time that was left previously
        burnTime = (totalBurnTime * burnPercentage).toInt()
    }
    
    override fun handleTick() {
        if (burnTime == 0) burnItem()
        
        if (burnTime != 0) {
            burnTime--
            energyHolder.energy = min(energyHolder.maxEnergy, energyHolder.energy + energyHolder.energyGeneration)
            
            if (gui.isInitialized())
                gui.value.progressItem.percentage = burnTime.toDouble() / totalBurnTime.toDouble()
            
            if (!active) active = true
        } else if (active) active = false
    }
    
    private fun burnItem() {
        val fuelStack = inventory.getItemStack(0)
        if (energyHolder.energy < energyHolder.maxEnergy && fuelStack != null) {
            val itemBurnTime = fuelStack.burnTime
            if (itemBurnTime != null) {
                burnTime += (itemBurnTime * burnTimeMultiplier).roundToInt()
                totalBurnTime = burnTime
                val remains = fuelStack.craftingRemainingItem
                if (remains != null) {
                    inventory.setItemStack(null, 0, remains)
                } else inventory.addItemAmount(null, 0, -1)
            }
        }
    }
    
    private fun handleInventoryUpdate(event: ItemUpdateEvent) {
        if (event.updateReason != null) { // not done by the tileEntity itself
            if (event.newItemStack != null && event.newItemStack.isFuel) {
                // illegal item
                event.isCancelled = true
            }
        }
    }
    
    override fun saveData() {
        super.saveData()
        storeData("burnTime", burnTime)
        storeData("totalBurnTime", totalBurnTime)
    }
    
    inner class FurnaceGeneratorGui : TileEntityGui() {
        
        val progressItem = EnergyProgressItem()
        
        private val sideConfigGui = SideConfigGui(
            this@FurnaceGenerator,
            listOf(itemHolder.getNetworkedInventory(inventory) to "inventory.machines.fuel"),
            ::openWindow
        )
        
        override val gui = Gui.normal()
            .setStructure(
                "1 - - - - - - - 2",
                "| s # # # # # e |",
                "| u # # i # # e |",
                "| # # # ! # # e |",
                "| # # # # # # e |",
                "3 - - - - - - - 4")
            .addIngredient('i', SlotElement.VISlotElement(inventory, 0))
            .addIngredient('!', progressItem)
            .addIngredient('s', OpenSideConfigItem(sideConfigGui))
            .addIngredient('u', OpenUpgradesItem(upgradeHolder))
            .addIngredient('e', EnergyBar(4, energyHolder))
            .build()
        
    }
    
}

package xyz.xenondevs.nova.machines.tileentity.agriculture

import de.studiocode.invui.gui.GUI
import de.studiocode.invui.gui.SlotElement.VISlotElement
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.guitype.GUIType
import de.studiocode.invui.item.Item
import de.studiocode.invui.item.impl.BaseItem
import de.studiocode.invui.virtualinventory.event.ItemUpdateEvent
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.data.world.block.state.NovaTileEntityState
import xyz.xenondevs.nova.integration.protection.ProtectionManager
import xyz.xenondevs.nova.machines.registry.Blocks.PLANTER
import xyz.xenondevs.nova.machines.registry.GUIMaterials
import xyz.xenondevs.nova.tileentity.NetworkedTileEntity
import xyz.xenondevs.nova.tileentity.SELF_UPDATE_REASON
import xyz.xenondevs.nova.tileentity.network.NetworkConnectionType
import xyz.xenondevs.nova.tileentity.network.energy.holder.ConsumerEnergyHolder
import xyz.xenondevs.nova.tileentity.network.item.holder.NovaItemHolder
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
import xyz.xenondevs.nova.util.BlockSide
import xyz.xenondevs.nova.util.advance
import xyz.xenondevs.nova.util.item.PlantUtils
import xyz.xenondevs.nova.util.item.ToolUtils
import xyz.xenondevs.nova.util.item.isHoe
import xyz.xenondevs.nova.util.item.isTillable
import xyz.xenondevs.nova.world.region.Region
import xyz.xenondevs.nova.world.region.VisualRegion

private val MAX_ENERGY = NovaConfig[PLANTER].getLong("capacity")
private val ENERGY_PER_TICK = NovaConfig[PLANTER].getLong("energy_per_tick")
private val ENERGY_PER_PLANT = NovaConfig[PLANTER].getLong("energy_per_plant")
private val IDLE_TIME = NovaConfig[PLANTER].getInt("idle_time")
private val MIN_RANGE = NovaConfig[PLANTER].getInt("range.min")
private val MAX_RANGE = NovaConfig[PLANTER].getInt("range.max")
private val DEFAULT_RANGE = NovaConfig[PLANTER].getInt("range.default")

class Planter(blockState: NovaTileEntityState) : NetworkedTileEntity(blockState), Upgradable {
    
    private val inputInventory = getInventory("input", 6, ::handleSeedUpdate)
    private val hoesInventory = getInventory("hoes", 1, ::handleHoeUpdate)
    override val gui = lazy(::PlanterGUI)
    override val upgradeHolder = UpgradeHolder(this, gui, ::handleUpgradeUpdates, UpgradeType.SPEED, UpgradeType.EFFICIENCY, UpgradeType.ENERGY, UpgradeType.RANGE)
    override val energyHolder = ConsumerEnergyHolder(this, MAX_ENERGY, ENERGY_PER_TICK, ENERGY_PER_PLANT, upgradeHolder) { createSideConfig(NetworkConnectionType.INSERT, BlockSide.FRONT) }
    override val itemHolder = NovaItemHolder(
        this,
        inputInventory to NetworkConnectionType.BUFFER,
        hoesInventory to NetworkConnectionType.BUFFER
    ) { createSideConfig(NetworkConnectionType.INSERT, BlockSide.FRONT) }
    
    private var autoTill = retrieveData("autoTill") { true }
    private var maxIdleTime = 0
    private var timePassed = 0
    
    private var maxRange = 0
    private var range = retrieveData("range") { DEFAULT_RANGE }
        set(value) {
            field = value
            updateRegion()
            if (gui.isInitialized()) gui.value.updateRangeItems()
        }
    
    private lateinit var plantRegion: Region
    private lateinit var soilRegion: Region
    
    init {
        handleUpgradeUpdates()
        updateRegion()
    }
    
    private fun handleUpgradeUpdates() {
        maxIdleTime = (IDLE_TIME / upgradeHolder.getValue(UpgradeType.SPEED)).toInt()
        if (timePassed > maxIdleTime) timePassed = maxIdleTime
        
        maxRange = MAX_RANGE + upgradeHolder.getValue(UpgradeType.RANGE)
        if (maxRange < range) range = maxRange
    }
    
    private fun updateRegion() {
        plantRegion = getBlockFrontRegion(range, range, 1, 0)
        soilRegion = Region(plantRegion.min.clone().advance(BlockFace.DOWN), plantRegion.max.clone().advance(BlockFace.DOWN))
        
        VisualRegion.updateRegion(uuid, plantRegion)
    }
    
    override fun handleTick() {
        if (energyHolder.energy >= energyHolder.energyConsumption) {
            energyHolder.energy -= energyHolder.energyConsumption // idle energy consumption
            
            if (energyHolder.energy >= energyHolder.specialEnergyConsumption && timePassed++ >= maxIdleTime) {
                timePassed = 0
                placeNextSeed()
            }
        }
    }
    
    private fun placeNextSeed() {
        if (!inputInventory.isEmpty) {
            // loop over items until a placeable seed has been found
            for ((index, item) in inputInventory.items.withIndex()) {
                if (item == null) continue
                
                // find a location to place this seed or skip to the next one if there isn't one
                val (plant, soil) = getNextPlantBlock(item) ?: continue
                energyHolder.energy -= energyHolder.specialEnergyConsumption
                
                // till dirt if possible
                if (soil.type.isTillable() && autoTill && !hoesInventory.isEmpty) tillDirt(soil)
                
                // plant the seed
                PlantUtils.placeSeed(item, plant, true)
                
                // remove one from the seed stack
                inputInventory.addItemAmount(SELF_UPDATE_REASON, index, -1)
                
                // break the loop as a seed has been placed
                break
            }
        } else if (autoTill && !hoesInventory.isEmpty) {
            val block = getNextTillableBlock()
            if (block != null) {
                energyHolder.energy -= energyHolder.specialEnergyConsumption
                tillDirt(block)
            }
        }
    }
    
    private fun getNextPlantBlock(seedStack: ItemStack): Pair<Block, Block>? {
        val emptyHoes = hoesInventory.isEmpty
        val index = plantRegion.withIndex().indexOfFirst { (index, block) ->
            val soilBlock = soilRegion[index]
            val soilType = soilBlock.type
            
            // if the plant block is already occupied return false
            if (!block.type.isAir)
                return@indexOfFirst false
            
            val soilTypeApplicable = PlantUtils.canBePlaced(seedStack, soilBlock)
            if (soilTypeApplicable) {
                // if the seed can be placed on the soil block, only the permission needs to be checked
                return@indexOfFirst ProtectionManager.canPlace(this, seedStack, block.location).get()
            } else {
                // if the seed can not be placed on the soil block, check if this seed requires farmland and if it does
                // check if the soil block can be tilled
                val requiresFarmland = PlantUtils.requiresFarmland(seedStack)
                val isOrCanBeFarmland = soilType == Material.FARMLAND || (soilType.isTillable() && autoTill && !emptyHoes)
                if (requiresFarmland && !isOrCanBeFarmland)
                    return@indexOfFirst false
                
                // the block can be tilled, now check for both planting and tilling permissions
                return@indexOfFirst ProtectionManager.canPlace(this, seedStack, block.location).get() &&
                    ProtectionManager.canUseBlock(this, hoesInventory.getItemStack(0), soilBlock.location).get()
            }
        }
        
        if (index == -1)
            return null
        return plantRegion[index] to soilRegion[index]
    }
    
    private fun getNextTillableBlock(): Block? {
        return plantRegion.firstOrNull {
            it.type.isTillable()
                && ProtectionManager.canUseBlock(this, hoesInventory.getItemStack(0), it.location).get()
        }
    }
    
    private fun tillDirt(block: Block) {
        block.type = Material.FARMLAND
        world.playSound(block.location, Sound.ITEM_HOE_TILL, 1f, 1f)
        useHoe()
    }
    
    private fun handleHoeUpdate(event: ItemUpdateEvent) {
        if ((event.isAdd || event.isSwap) && !event.newItemStack.type.isHoe())
            event.isCancelled = true
    }
    
    private fun handleSeedUpdate(event: ItemUpdateEvent) {
        if (!event.isRemove && !PlantUtils.isSeed(event.newItemStack))
            event.isCancelled = true
    }
    
    private fun useHoe() {
        hoesInventory.setItemStack(null, 0, ToolUtils.damageTool(hoesInventory.items[0]))
    }
    
    override fun handleRemoved(unload: Boolean) {
        super.handleRemoved(unload)
        VisualRegion.removeRegion(uuid)
    }
    
    override fun saveData() {
        super.saveData()
        storeData("autoTill", autoTill)
        storeData("range", range)
    }
    
    inner class PlanterGUI : TileEntityGUI() {
        
        private val sideConfigGUI = SideConfigGUI(
            this@Planter,
            listOf(
                itemHolder.getNetworkedInventory(inputInventory) to "inventory.nova.input",
                itemHolder.getNetworkedInventory(hoesInventory) to "inventory.nova.hoes",
            ),
            ::openWindow
        )
        
        private val rangeItems = ArrayList<Item>()
        
        override val gui: GUI = GUIBuilder(GUIType.NORMAL)
            .setStructure(
                "1 - - - - - - - 2",
                "| s u v # # p e |",
                "| i i i # h n e |",
                "| i i i # f m e |",
                "3 - - - - - - - 4")
            .addIngredient('i', inputInventory)
            .addIngredient('h', VISlotElement(hoesInventory, 0, GUIMaterials.HOE_PLACEHOLDER.createBasicItemBuilder()))
            .addIngredient('v', VisualizeRegionItem(uuid) { plantRegion })
            .addIngredient('s', OpenSideConfigItem(sideConfigGUI))
            .addIngredient('f', AutoTillingItem())
            .addIngredient('u', OpenUpgradesItem(upgradeHolder))
            .addIngredient('p', AddNumberItem({ MIN_RANGE..maxRange }, { range }, { range = it }).also(rangeItems::add))
            .addIngredient('m', RemoveNumberItem({ MIN_RANGE..maxRange }, { range }, { range = it }).also(rangeItems::add))
            .addIngredient('n', DisplayNumberItem { range }.also(rangeItems::add))
            .addIngredient('e', EnergyBar(3, energyHolder))
            .build()
        
        fun updateRangeItems() = rangeItems.forEach(Item::notifyWindows)
        
        private inner class AutoTillingItem : BaseItem() {
            
            override fun getItemProvider() =
                (if (autoTill) GUIMaterials.HOE_BTN_ON else GUIMaterials.HOE_BTN_OFF).itemProvider
            
            override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
                autoTill = !autoTill
                notifyWindows()
                
                player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 1f)
            }
            
        }
        
    }
    
}
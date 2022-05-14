package xyz.xenondevs.nova.machines.tileentity.agriculture

import de.studiocode.invui.gui.GUI
import de.studiocode.invui.gui.SlotElement.VISlotElement
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.guitype.GUIType
import de.studiocode.invui.item.Item
import de.studiocode.invui.virtualinventory.VirtualInventory
import de.studiocode.invui.virtualinventory.event.ItemUpdateEvent
import org.bukkit.Material
import org.bukkit.Tag
import org.bukkit.block.Block
import xyz.xenondevs.nova.api.event.tileentity.TileEntityBreakBlockEvent
import xyz.xenondevs.nova.data.config.GlobalValues
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.data.config.Reloadable
import xyz.xenondevs.nova.data.config.configReloadable
import xyz.xenondevs.nova.data.world.block.state.NovaTileEntityState
import xyz.xenondevs.nova.integration.protection.ProtectionManager
import xyz.xenondevs.nova.machines.registry.Blocks.HARVESTER
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
import xyz.xenondevs.nova.util.*
import xyz.xenondevs.nova.util.item.*
import xyz.xenondevs.nova.world.block.context.BlockBreakContext
import xyz.xenondevs.nova.world.pos
import xyz.xenondevs.nova.world.region.Region
import xyz.xenondevs.nova.world.region.VisualRegion
import java.util.*

private val MAX_ENERGY by configReloadable { NovaConfig[HARVESTER].getLong("capacity") }
private val ENERGY_PER_TICK by configReloadable { NovaConfig[HARVESTER].getLong("energy_per_tick") }
private val ENERGY_PER_BREAK by configReloadable { NovaConfig[HARVESTER].getLong("energy_per_break") }
private val IDLE_TIME by configReloadable { NovaConfig[HARVESTER].getInt("idle_time") }
private val MIN_RANGE by configReloadable { NovaConfig[HARVESTER].getInt("range.min") }
private val MAX_RANGE by configReloadable { NovaConfig[HARVESTER].getInt("range.max") }
private val DEFAULT_RANGE by configReloadable { NovaConfig[HARVESTER].getInt("range.default") }

class Harvester(blockState: NovaTileEntityState) : NetworkedTileEntity(blockState), Upgradable, Reloadable {
    
    private val inventory = getInventory("harvest", 12, ::handleInventoryUpdate)
    private val shearInventory = getInventory("shears", 1, ::handleShearInventoryUpdate)
    private val axeInventory = getInventory("axe", 1, ::handleAxeInventoryUpdate)
    private val hoeInventory = getInventory("hoe", 1, ::handleHoeInventoryUpdate)
    override val gui = lazy(::HarvesterGUI)
    override val upgradeHolder = UpgradeHolder(this, gui, ::handleUpgradeUpdates, UpgradeType.SPEED, UpgradeType.EFFICIENCY, UpgradeType.ENERGY, UpgradeType.RANGE)
    override val energyHolder = ConsumerEnergyHolder(this, MAX_ENERGY, ENERGY_PER_TICK, ENERGY_PER_BREAK, upgradeHolder) { createSideConfig(NetworkConnectionType.INSERT, BlockSide.FRONT) }
    override val itemHolder = NovaItemHolder(
        this,
        inventory to NetworkConnectionType.EXTRACT,
        shearInventory to NetworkConnectionType.BUFFER, axeInventory to NetworkConnectionType.BUFFER, hoeInventory to NetworkConnectionType.BUFFER
    ) { createSideConfig(NetworkConnectionType.EXTRACT, BlockSide.FRONT) }
    
    private var maxIdleTime = 0
    private var maxRange = 0
    private var range = retrieveData("range") { DEFAULT_RANGE }
        set(value) {
            field = value
            updateRegion()
            if (gui.isInitialized()) gui.value.updateRangeItems()
        }
    private lateinit var harvestRegion: Region
    
    private val queuedBlocks = LinkedList<Pair<Block, Material>>()
    private var timePassed = 0
    private var loadCooldown = 0
    
    init {
        NovaConfig.reloadables.add(this)
        handleUpgradeUpdates()
        updateRegion()
    }
    
    override fun reload() {
        energyHolder.defaultMaxEnergy = MAX_ENERGY
        energyHolder.defaultEnergyConsumption = ENERGY_PER_TICK
        energyHolder.defaultSpecialEnergyConsumption = ENERGY_PER_BREAK
        
        handleUpgradeUpdates()
        updateRegion()
    }
    
    private fun handleUpgradeUpdates() {
        maxIdleTime = (IDLE_TIME / upgradeHolder.getValue(UpgradeType.SPEED)).toInt()
        if (timePassed > maxIdleTime) timePassed = maxIdleTime
        
        maxRange = MAX_RANGE + upgradeHolder.getValue(UpgradeType.RANGE)
        if (range > maxRange) range = maxRange
    }
    
    private fun updateRegion() {
        harvestRegion = getBlockFrontRegion(range, range, range * 2, 0)
        VisualRegion.updateRegion(uuid, harvestRegion)
    }
    
    override fun saveData() {
        super.saveData()
        storeData("range", range)
    }
    
    override fun handleTick() {
        if (energyHolder.energy >= energyHolder.energyConsumption) {
            energyHolder.energy -= energyHolder.energyConsumption
            
            if (energyHolder.energy >= energyHolder.specialEnergyConsumption) {
                loadCooldown--
                
                if (timePassed++ >= maxIdleTime) {
                    timePassed = 0
                    
                    if (!GlobalValues.DROP_EXCESS_ON_GROUND && inventory.isFull()) return
                    if (queuedBlocks.isEmpty()) loadBlocks()
                    harvestNextBlock()
                }
            }
        }
    }
    
    private fun loadBlocks() {
        if (loadCooldown <= 0) {
            loadCooldown = 100
            
            queuedBlocks += harvestRegion
                .blocks
                .filter(PlantUtils::isHarvestable)
                .sortedWith(HarvestPriorityComparator)
                .map { it to it.type }
        }
    }
    
    private fun harvestNextBlock() {
        do {
            var tryAgain = false
            
            if (queuedBlocks.isNotEmpty()) {
                // get next block
                val (block, expectedType) = queuedBlocks.first
                queuedBlocks.removeFirst()
                
                // check that the type hasn't changed
                if (block.type == expectedType) {
                    
                    val toolInventory: VirtualInventory? = when {
                        Tag.LEAVES.isTagged(expectedType) -> if (shearInventory.isEmpty) hoeInventory else shearInventory
                        Tag.MINEABLE_AXE.isTagged(expectedType) -> axeInventory
                        Tag.MINEABLE_HOE.isTagged(expectedType) -> hoeInventory
                        else -> null
                    }
                    
                    val tool = toolInventory?.getItemStack(0)
                    if (!ProtectionManager.canBreak(this, tool, block.location).get()) {
                        // skip block if it is protected
                        tryAgain = true
                        continue
                    }
                    
                    // get drops
                    val ctx = BlockBreakContext(block.pos, this, location, null, tool)
                    var drops = PlantUtils.getHarvestDrops(ctx)!!.toMutableList()
                    drops = TileEntityBreakBlockEvent(this, block, drops).also(::callEvent).drops
                    
                    // check that the drops will fit in the inventory or can be dropped on the ground
                    if (!GlobalValues.DROP_EXCESS_ON_GROUND && !inventory.canHold(drops)) {
                        tryAgain = true
                        continue
                    }
                    
                    // check for tool and damage if present
                    if (toolInventory != null) {
                        if (tool == null) {
                            tryAgain = true
                            continue
                        }
                        
                        toolInventory.setItemStack(SELF_UPDATE_REASON, 0, ToolUtils.damageTool(tool))
                    }
                    
                    // harvest the plant
                    PlantUtils.harvest(ctx, true)
                    
                    // add the drops to the inventory or drop them in the world if they don't fit
                    if (inventory.canHold(drops))
                        inventory.addAll(SELF_UPDATE_REASON, drops)
                    else if (GlobalValues.DROP_EXCESS_ON_GROUND)
                        world.dropItemsNaturally(block.location, drops)
                    
                    // take energy
                    energyHolder.energy -= energyHolder.specialEnergyConsumption
                } else tryAgain = true
            }
            
        } while (tryAgain)
    }
    
    private fun handleInventoryUpdate(event: ItemUpdateEvent) {
        event.isCancelled = event.updateReason != SELF_UPDATE_REASON && event.isAdd
    }
    
    private fun handleShearInventoryUpdate(event: ItemUpdateEvent) {
        event.isCancelled = event.newItemStack != null && event.newItemStack.type != Material.SHEARS
    }
    
    private fun handleAxeInventoryUpdate(event: ItemUpdateEvent) {
        event.isCancelled = event.newItemStack != null && !event.newItemStack.type.isAxe()
    }
    
    private fun handleHoeInventoryUpdate(event: ItemUpdateEvent) {
        event.isCancelled = event.newItemStack != null && !event.newItemStack.type.isHoe()
    }
    
    override fun handleRemoved(unload: Boolean) {
        super.handleRemoved(unload)
        VisualRegion.removeRegion(uuid)
    }
    
    inner class HarvesterGUI : TileEntityGUI() {
        
        private val sideConfigGUI = SideConfigGUI(
            this@Harvester,
            listOf(
                itemHolder.getNetworkedInventory(inventory) to "inventory.nova.output",
                itemHolder.getNetworkedInventory(shearInventory) to "inventory.nova.shears",
                itemHolder.getNetworkedInventory(axeInventory) to "inventory.nova.axes",
                itemHolder.getNetworkedInventory(hoeInventory) to "inventory.nova.hoes",
            ),
            ::openWindow
        )
        
        private val rangeItems = ArrayList<Item>()
        
        override val gui: GUI = GUIBuilder(GUIType.NORMAL)
            .setStructure(
                "1 - - - - - - - 2",
                "| c v u s a h e |",
                "| m n p # # # e |",
                "| i i i i i i e |",
                "| i i i i i i e |",
                "3 - - - - - - - 4")
            .addIngredient('i', inventory)
            .addIngredient('c', OpenSideConfigItem(sideConfigGUI))
            .addIngredient('v', VisualizeRegionItem(uuid) { harvestRegion })
            .addIngredient('s', VISlotElement(shearInventory, 0, GUIMaterials.SHEARS_PLACEHOLDER.createBasicItemBuilder()))
            .addIngredient('a', VISlotElement(axeInventory, 0, GUIMaterials.AXE_PLACEHOLDER.createBasicItemBuilder()))
            .addIngredient('h', VISlotElement(hoeInventory, 0, GUIMaterials.HOE_PLACEHOLDER.createBasicItemBuilder()))
            .addIngredient('p', AddNumberItem({ MIN_RANGE..maxRange }, { range }, { range = it }).also(rangeItems::add))
            .addIngredient('m', RemoveNumberItem({ MIN_RANGE..maxRange }, { range }, { range = it }).also(rangeItems::add))
            .addIngredient('n', DisplayNumberItem { range }.also(rangeItems::add))
            .addIngredient('u', OpenUpgradesItem(upgradeHolder))
            .addIngredient('e', EnergyBar(4, energyHolder))
            .build()
        
        fun updateRangeItems() = rangeItems.forEach(Item::notifyWindows)
        
    }
    
}

private object HarvestPriorityComparator : Comparator<Block> {
    
    @Suppress("LiftReturnOrAssignment")
    override fun compare(o1: Block, o2: Block): Int {
        val type1 = o1.type
        val type2 = o2.type
        
        fun compareLocation() = o2.location.y.compareTo(o1.location.y)
        
        if (type1 == type2) compareLocation()
        
        if (PlantUtils.isTreeAttachment(type1)) {
            if (PlantUtils.isTreeAttachment(type2)) {
                return compareLocation()
            } else {
                return -1
            }
        } else if (PlantUtils.isTreeAttachment(type2)) {
            return 1
        }
        
        if (type1.isLeaveLike()) {
            if (type2.isLeaveLike()) {
                return compareLocation()
            } else {
                return -1
            }
        } else if (type2.isLeaveLike()) {
            return 1
        }
        
        if (Tag.LOGS.isTagged(type1)) {
            if (Tag.LOGS.isTagged(type2)) {
                return compareLocation()
            } else {
                return -1
            }
        } else if (Tag.LOGS.isTagged(type2)) {
            return 1
        }
        
        return compareLocation()
    }
    
}
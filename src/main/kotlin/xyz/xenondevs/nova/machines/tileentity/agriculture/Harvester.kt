package xyz.xenondevs.nova.machines.tileentity.agriculture

import org.bukkit.Material
import org.bukkit.Tag
import org.bukkit.block.Block
import org.bukkit.entity.Player
import xyz.xenondevs.invui.gui.Gui
import xyz.xenondevs.invui.gui.SlotElement.VISlotElement
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.virtualinventory.VirtualInventory
import xyz.xenondevs.invui.virtualinventory.event.ItemUpdateEvent
import xyz.xenondevs.nova.api.NovaEventFactory
import xyz.xenondevs.nova.data.config.GlobalValues
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.data.config.configReloadable
import xyz.xenondevs.nova.data.world.block.state.NovaTileEntityState
import xyz.xenondevs.nova.integration.protection.ProtectionManager
import xyz.xenondevs.nova.item.tool.ToolCategory
import xyz.xenondevs.nova.item.tool.VanillaToolCategories
import xyz.xenondevs.nova.machines.registry.Blocks.HARVESTER
import xyz.xenondevs.nova.machines.registry.GuiMaterials
import xyz.xenondevs.nova.tileentity.NetworkedTileEntity
import xyz.xenondevs.nova.tileentity.menu.TileEntityMenuClass
import xyz.xenondevs.nova.tileentity.network.NetworkConnectionType
import xyz.xenondevs.nova.tileentity.network.item.holder.NovaItemHolder
import xyz.xenondevs.nova.tileentity.upgrade.Upgradable
import xyz.xenondevs.nova.ui.EnergyBar
import xyz.xenondevs.nova.ui.OpenUpgradesItem
import xyz.xenondevs.nova.ui.config.side.OpenSideConfigItem
import xyz.xenondevs.nova.ui.config.side.SideConfigMenu
import xyz.xenondevs.nova.ui.item.AddNumberItem
import xyz.xenondevs.nova.ui.item.DisplayNumberItem
import xyz.xenondevs.nova.ui.item.RemoveNumberItem
import xyz.xenondevs.nova.ui.item.VisualizeRegionItem
import xyz.xenondevs.nova.util.BlockSide
import xyz.xenondevs.nova.util.addAll
import xyz.xenondevs.nova.util.dropItemsNaturally
import xyz.xenondevs.nova.util.item.DamageableUtils
import xyz.xenondevs.nova.util.item.PlantUtils
import xyz.xenondevs.nova.util.item.isLeaveLike
import xyz.xenondevs.nova.world.block.context.BlockBreakContext
import xyz.xenondevs.nova.world.pos
import xyz.xenondevs.nova.world.region.Region
import xyz.xenondevs.nova.world.region.VisualRegion
import xyz.xenondevs.simpleupgrades.ConsumerEnergyHolder
import xyz.xenondevs.simpleupgrades.registry.UpgradeTypes
import java.util.*

private val MAX_ENERGY = configReloadable { NovaConfig[HARVESTER].getLong("capacity") }
private val ENERGY_PER_TICK = configReloadable { NovaConfig[HARVESTER].getLong("energy_per_tick") }
private val ENERGY_PER_BREAK = configReloadable { NovaConfig[HARVESTER].getLong("energy_per_break") }
private val IDLE_TIME by configReloadable { NovaConfig[HARVESTER].getInt("idle_time") }
private val MIN_RANGE by configReloadable { NovaConfig[HARVESTER].getInt("range.min") }
private val MAX_RANGE by configReloadable { NovaConfig[HARVESTER].getInt("range.max") }
private val DEFAULT_RANGE by configReloadable { NovaConfig[HARVESTER].getInt("range.default") }

class Harvester(blockState: NovaTileEntityState) : NetworkedTileEntity(blockState), Upgradable {
    
    private val inventory = getInventory("harvest", 12, ::handleInventoryUpdate)
    private val shearInventory = getInventory("shears", 1, ::handleShearInventoryUpdate)
    private val axeInventory = getInventory("axe", 1, ::handleAxeInventoryUpdate)
    private val hoeInventory = getInventory("hoe", 1, ::handleHoeInventoryUpdate)
    override val upgradeHolder = getUpgradeHolder(UpgradeTypes.SPEED, UpgradeTypes.EFFICIENCY, UpgradeTypes.ENERGY, UpgradeTypes.RANGE)
    override val energyHolder = ConsumerEnergyHolder(this, MAX_ENERGY, ENERGY_PER_TICK, ENERGY_PER_BREAK, upgradeHolder) { createSideConfig(NetworkConnectionType.INSERT, BlockSide.FRONT) }
    override val itemHolder = NovaItemHolder(
        this,
        inventory to NetworkConnectionType.EXTRACT,
        shearInventory to NetworkConnectionType.INSERT, axeInventory to NetworkConnectionType.INSERT, hoeInventory to NetworkConnectionType.INSERT
    ) { createSideConfig(NetworkConnectionType.BUFFER, BlockSide.FRONT) }
    
    private var maxIdleTime = 0
    private var maxRange = 0
    private var range = retrieveData("range") { DEFAULT_RANGE }
        set(value) {
            field = value
            updateRegion()
            menuContainer.forEachMenu(HarvesterMenu::updateRangeItems)
        }
    private lateinit var harvestRegion: Region
    
    private val queuedBlocks = LinkedList<Pair<Block, Material>>()
    private var timePassed = 0
    private var loadCooldown = 0
    
    init {
        reload()
        updateRegion()
    }
    
    override fun reload() {
        super.reload()
        
        maxIdleTime = (IDLE_TIME / upgradeHolder.getValue(UpgradeTypes.SPEED)).toInt()
        if (timePassed > maxIdleTime) timePassed = maxIdleTime
        
        maxRange = MAX_RANGE + upgradeHolder.getValue(UpgradeTypes.RANGE)
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
                    
                    if (!GlobalValues.DROP_EXCESS_ON_GROUND && inventory.isFull) return
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
                    val drops = PlantUtils.getHarvestDrops(ctx)!!.toMutableList()
                    NovaEventFactory.callTileEntityBlockBreakEvent(this, block, drops)
                    
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
                        
                        toolInventory.setItemStack(SELF_UPDATE_REASON, 0, DamageableUtils.damageItem(tool))
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
        event.isCancelled = event.newItemStack != null && ToolCategory.ofItem(event.newItemStack) != VanillaToolCategories.AXE
    }
    
    private fun handleHoeInventoryUpdate(event: ItemUpdateEvent) {
        event.isCancelled = event.newItemStack != null && ToolCategory.ofItem(event.newItemStack) != VanillaToolCategories.HOE
    }
    
    override fun handleRemoved(unload: Boolean) {
        super.handleRemoved(unload)
        VisualRegion.removeRegion(uuid)
    }
    
    @TileEntityMenuClass
    inner class HarvesterMenu(player: Player) : IndividualTileEntityMenu(player) {
        
        private val sideConfigGui = SideConfigMenu(
            this@Harvester,
            listOf(
                itemHolder.getNetworkedInventory(inventory) to "inventory.nova.output",
                itemHolder.getNetworkedInventory(shearInventory) to "inventory.machines.shears",
                itemHolder.getNetworkedInventory(axeInventory) to "inventory.machines.axes",
                itemHolder.getNetworkedInventory(hoeInventory) to "inventory.machines.hoes",
            ),
            ::openWindow
        )
        
        private val rangeItems = ArrayList<Item>()
        
        override val gui = Gui.normal()
            .setStructure(
                "1 - - - - - - - 2",
                "| c v u s a h e |",
                "| m n p # # # e |",
                "| i i i i i i e |",
                "| i i i i i i e |",
                "3 - - - - - - - 4")
            .addIngredient('i', inventory)
            .addIngredient('c', OpenSideConfigItem(sideConfigGui))
            .addIngredient('v', VisualizeRegionItem(player, uuid) { harvestRegion })
            .addIngredient('s', VISlotElement(shearInventory, 0, GuiMaterials.SHEARS_PLACEHOLDER.clientsideProvider))
            .addIngredient('a', VISlotElement(axeInventory, 0, GuiMaterials.AXE_PLACEHOLDER.clientsideProvider))
            .addIngredient('h', VISlotElement(hoeInventory, 0, GuiMaterials.HOE_PLACEHOLDER.clientsideProvider))
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
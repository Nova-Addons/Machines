package xyz.xenondevs.nova.machines.tileentity.world

import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import xyz.xenondevs.commons.collections.rotateRight
import xyz.xenondevs.invui.gui.builder.GuiType
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.impl.BaseItem
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.data.config.configReloadable
import xyz.xenondevs.nova.data.world.block.state.NovaTileEntityState
import xyz.xenondevs.nova.integration.protection.ProtectionManager
import xyz.xenondevs.nova.machines.registry.Blocks.PUMP
import xyz.xenondevs.nova.machines.registry.GuiMaterials
import xyz.xenondevs.nova.tileentity.NetworkedTileEntity
import xyz.xenondevs.nova.tileentity.TileEntity
import xyz.xenondevs.nova.tileentity.network.NetworkConnectionType
import xyz.xenondevs.nova.tileentity.network.fluid.FluidType
import xyz.xenondevs.nova.tileentity.network.fluid.holder.NovaFluidHolder
import xyz.xenondevs.nova.tileentity.upgrade.Upgradable
import xyz.xenondevs.nova.ui.EnergyBar
import xyz.xenondevs.nova.ui.FluidBar
import xyz.xenondevs.nova.ui.OpenUpgradesItem
import xyz.xenondevs.nova.ui.config.side.OpenSideConfigItem
import xyz.xenondevs.nova.ui.config.side.SideConfigGui
import xyz.xenondevs.nova.ui.item.AddNumberItem
import xyz.xenondevs.nova.ui.item.DisplayNumberItem
import xyz.xenondevs.nova.ui.item.RemoveNumberItem
import xyz.xenondevs.nova.ui.item.VisualizeRegionItem
import xyz.xenondevs.nova.util.BlockSide
import xyz.xenondevs.nova.util.HORIZONTAL_FACES
import xyz.xenondevs.nova.util.VERTICAL_FACES
import xyz.xenondevs.nova.util.advance
import xyz.xenondevs.nova.util.isSourceFluid
import xyz.xenondevs.nova.util.item.playPlaceSoundEffect
import xyz.xenondevs.nova.util.sourceFluidType
import xyz.xenondevs.nova.world.region.Region
import xyz.xenondevs.nova.world.region.VisualRegion
import xyz.xenondevs.simpleupgrades.ConsumerEnergyHolder
import xyz.xenondevs.simpleupgrades.getFluidContainer
import xyz.xenondevs.simpleupgrades.registry.UpgradeTypes
import java.util.*

private val ENERGY_CAPACITY = configReloadable { NovaConfig[PUMP].getLong("energy_capacity") }
private val ENERGY_PER_TICK = configReloadable { NovaConfig[PUMP].getLong("energy_per_tick") }
private val FLUID_CAPACITY = configReloadable { NovaConfig[PUMP].getLong("fluid_capacity") }
private val REPLACEMENT_BLOCK by configReloadable { Material.valueOf(NovaConfig[PUMP].getString("replacement_block")!!) }
private val IDLE_TIME by configReloadable { NovaConfig[PUMP].getLong("idle_time") }

private val MIN_RANGE by configReloadable { NovaConfig[PUMP].getInt("range.min") }
private val MAX_RANGE by configReloadable { NovaConfig[PUMP].getInt("range.max") }
private val DEFAULT_RANGE by configReloadable { NovaConfig[PUMP].getInt("range.default") }

class Pump(blockState: NovaTileEntityState) : NetworkedTileEntity(blockState), Upgradable {
    
    override val gui = lazy(::PumpGui)
    override val upgradeHolder = getUpgradeHolder(UpgradeTypes.SPEED, UpgradeTypes.EFFICIENCY, UpgradeTypes.ENERGY, UpgradeTypes.RANGE, UpgradeTypes.FLUID)
    
    private val fluidTank = getFluidContainer("tank", hashSetOf(FluidType.WATER, FluidType.LAVA), FLUID_CAPACITY, upgradeHolder = upgradeHolder)
    
    override val energyHolder = ConsumerEnergyHolder(this, ENERGY_CAPACITY, ENERGY_PER_TICK, upgradeHolder = upgradeHolder) { createExclusiveSideConfig(NetworkConnectionType.INSERT, BlockSide.TOP) }
    override val fluidHolder = NovaFluidHolder(this, fluidTank to NetworkConnectionType.EXTRACT) { createExclusiveSideConfig(NetworkConnectionType.EXTRACT, BlockSide.TOP) }
    
    private var maxIdleTime = 0
    private var idleTime = 0
    
    private var mode = retrieveData("mode") { PumpMode.REPLACE }
    
    private var maxRange = 0
    private var range = retrieveData("range") { DEFAULT_RANGE }
        set(value) {
            field = value
            updateRegion()
            if (gui.isInitialized()) gui.value.updateRangeItems()
        }
    private lateinit var region: Region
    
    private var lastBlock: Block? = null
    private var sortedFaces = LinkedList(HORIZONTAL_FACES)
    
    init {
        reload()
        updateRegion()
    }
    
    override fun reload() {
        super.reload()
        
        maxIdleTime = (IDLE_TIME / upgradeHolder.getValue(UpgradeTypes.SPEED)).toInt()
        if (idleTime > maxIdleTime) idleTime = maxIdleTime
        
        maxRange = MAX_RANGE + upgradeHolder.getValue(UpgradeTypes.RANGE)
        if (maxRange < range) range = maxRange
    }
    
    private fun updateRegion() {
        val rangeDouble = range.toDouble()
        val min = location.clone().subtract(rangeDouble - 1, rangeDouble, rangeDouble - 1)
        val max = location.clone().add(rangeDouble, 0.0, rangeDouble)
        region = Region(min, max)
        VisualRegion.updateRegion(uuid, region)
        idleTime = maxIdleTime
    }
    
    override fun handleTick() {
        if (energyHolder.energy >= energyHolder.energyConsumption && fluidTank.accepts(FluidType.WATER, 1000)) {
            if (--idleTime <= 0)
                pumpNextBlock()
        }
    }
    
    private fun pumpNextBlock() {
        val (block, type) = getNextBlock()
        if (block != null && type != null) {
            if (mode == PumpMode.REPLACE) {
                block.type = REPLACEMENT_BLOCK
                REPLACEMENT_BLOCK.playPlaceSoundEffect(block.location)
            } else if (!block.isInfiniteWaterSource()) {
                block.type = Material.AIR
            }
            fluidTank.addFluid(type, 1000)
            lastBlock = block
            energyHolder.energy -= energyHolder.energyConsumption
            idleTime = maxIdleTime
        } else {
            lastBlock = null
            idleTime = 60 * 20 // ByteZ' Idee
        }
    }
    
    private fun getNextBlock(): Pair<Block?, FluidType?> {
        var block: Block? = null
        var type: FluidType? = null
        if (lastBlock != null) {
            val pair = getRelativeBlock()
            block = pair.first
            type = pair.second
        }
        if (block == null) {
            val pair = searchBlock()
            block = pair.first
            type = pair.second
        }
        return block to type
    }
    
    private fun getRelativeBlock(): Pair<Block?, FluidType?> {
        val location = lastBlock!!.location
        val faces = VERTICAL_FACES + sortedFaces
        var block: Block? = null
        var type: FluidType? = null
        for (face in faces) {
            val newBlock = location.clone().advance(face, 1.0).block
            
            val fluidType = newBlock.sourceFluidType ?: continue
            if (fluidTank.accepts(fluidType) && newBlock in region && ProtectionManager.canBreak(this, null, newBlock.location).get()) {
                if (face !in VERTICAL_FACES)
                    sortedFaces.rotateRight()
                block = newBlock
                type = fluidType
                break
            }
        }
        return block to type
    }
    
    private fun searchBlock(): Pair<Block?, FluidType?> {
        repeat(range) { r ->
            if (r == 0) {
                val block = location.clone().advance(BlockFace.DOWN).block
                val fluidType = block.sourceFluidType ?: return@repeat
                if (fluidTank.accepts(fluidType) && ProtectionManager.canBreak(this, null, block.location).get())
                    return block to fluidType
                return@repeat
            }
            for (x in -r..r) {
                for (y in -r - 1 until 0) {
                    for (z in -r..r) {
                        if ((x != -r && x != r) && (y != -r - 1 && y != -1) && (z != -r && z != r))
                            continue
                        val block = location.clone().add(x.toDouble(), y.toDouble(), z.toDouble()).block
                        val fluidType = block.sourceFluidType ?: continue
                        if (fluidTank.accepts(fluidType) && ProtectionManager.canBreak(this, null, block.location).get())
                            return block to fluidType
                    }
                }
            }
        }
        return null to null
    }
    
    private fun Block.isInfiniteWaterSource(): Boolean {
        var waterCount = 0
        for (it in HORIZONTAL_FACES) {
            val newBlock = location.clone().advance(it, 1.0).block
            if ((newBlock.type == Material.WATER || newBlock.type == Material.BUBBLE_COLUMN) && newBlock.isSourceFluid())
                if (++waterCount > 1)
                    return true
        }
        return false
    }
    
    override fun handleRemoved(unload: Boolean) {
        super.handleRemoved(unload)
        VisualRegion.removeRegion(uuid)
    }
    
    override fun saveData() {
        super.saveData()
        storeData("range", range)
        storeData("mode", mode)
    }
    
    inner class PumpGui : TileEntity.TileEntityGui() {
        
        private val sideConfigGui = SideConfigGui(
            this@Pump,
            fluidContainerNames = listOf(fluidTank to "container.nova.fluid_tank"),
            openPrevious = ::openWindow
        )
        
        private val rangeItems = ArrayList<Item>()
        
        override val gui = GuiType.NORMAL.builder()
            .setStructure(
                "1 - - - - - - - 2",
                "| s p # f # e M |",
                "| u n # f # e # |",
                "| v m # f # e # |",
                "3 - - - - - - - 4")
            .addIngredient('s', OpenSideConfigItem(sideConfigGui))
            .addIngredient('u', OpenUpgradesItem(upgradeHolder))
            .addIngredient('v', VisualizeRegionItem(uuid) { region })
            .addIngredient('p', AddNumberItem({ MIN_RANGE..maxRange }, { range }, { range = it }).also(rangeItems::add))
            .addIngredient('n', DisplayNumberItem { range }.also(rangeItems::add))
            .addIngredient('m', RemoveNumberItem({ MIN_RANGE..maxRange }, { range }, { range = it }).also(rangeItems::add))
            .addIngredient('M', PumpModeItem())
            .addIngredient('e', EnergyBar(3, energyHolder))
            .addIngredient('f', FluidBar(3, fluidHolder, fluidTank))
            .build()
        
        fun updateRangeItems() = rangeItems.forEach(Item::notifyWindows)
        
        private inner class PumpModeItem : BaseItem() {
            
            override fun getItemProvider() =
                (if (mode == PumpMode.PUMP) GuiMaterials.PUMP_MODE_BTN else GuiMaterials.PUMP_REPLACE_MODE_BTN).clientsideProvider
            
            override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
                mode = if (mode == PumpMode.PUMP) PumpMode.REPLACE else PumpMode.PUMP
                notifyWindows()
            }
        }
        
    }
    
}

private enum class PumpMode {
    PUMP, // Replace fluid with air
    REPLACE // Replace fluid with block
}
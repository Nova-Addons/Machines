package xyz.xenondevs.nova.machines.tileentity.world

import de.studiocode.invui.gui.GUI
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.guitype.GUIType
import de.studiocode.invui.item.Item
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.data.world.block.state.NovaTileEntityState
import xyz.xenondevs.nova.machines.registry.Blocks.CHUNK_LOADER
import xyz.xenondevs.nova.tileentity.ChunkLoadManager
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
import xyz.xenondevs.nova.util.getSurroundingChunks
import xyz.xenondevs.nova.world.pos

private val MAX_ENERGY = NovaConfig[CHUNK_LOADER].getLong("capacity")!!
private val ENERGY_PER_CHUNK = NovaConfig[CHUNK_LOADER].getLong("energy_per_chunk")!!
private val MAX_RANGE = NovaConfig[CHUNK_LOADER].getInt("max_range")!!

class ChunkLoader(blockState: NovaTileEntityState) : NetworkedTileEntity(blockState), Upgradable {
    
    override val gui = lazy { ChunkLoaderGUI() }
    
    override val upgradeHolder = UpgradeHolder(this, gui, ::updateEnergyPerTick, UpgradeType.ENERGY, UpgradeType.EFFICIENCY)
    override val energyHolder = ConsumerEnergyHolder(this, MAX_ENERGY, 0, 0, upgradeHolder) { createSideConfig(NetworkConnectionType.INSERT) }
    
    private var range = retrieveData("range") { 0 }
    private var chunks = chunk.getSurroundingChunks(range, true)
    private var active = false
    
    private var energyPerTick = 0
    
    init {
        updateEnergyPerTick()
    }
    
    private fun updateEnergyPerTick() {
        energyPerTick = (ENERGY_PER_CHUNK * chunks.size / upgradeHolder.getEfficiencyModifier()).toInt()
    }
    
    override fun saveData() {
        super.saveData()
        storeData("range", range)
    }
    
    override fun handleTick() {
        if (energyHolder.energy >= energyPerTick) {
            energyHolder.energy -= energyPerTick
            if (!active) {
                setChunksForceLoaded(true)
                active = true
            }
        } else if (active) {
            setChunksForceLoaded(false)
            active = false
        }
    }
    
    private fun setChunksForceLoaded(state: Boolean) {
        chunks.forEach {
            if (state) ChunkLoadManager.submitChunkLoadRequest(it.pos, uuid)
            else ChunkLoadManager.revokeChunkLoadRequest(it.pos, uuid)
        }
    }
    
    private fun setRange(range: Int) {
        this.range = range
        setChunksForceLoaded(false)
        chunks = chunk.getSurroundingChunks(range, true)
        active = false
        updateEnergyPerTick()
    }
    
    override fun handleRemoved(unload: Boolean) {
        super.handleRemoved(unload)
        if (!unload) setChunksForceLoaded(false)
    }
    
    inner class ChunkLoaderGUI : TileEntityGUI() {
        
        private val sideConfigGUI = SideConfigGUI(
            this@ChunkLoader,
            ::openWindow
        )
        
        private val rangeItems = ArrayList<Item>()
        
        override val gui: GUI = GUIBuilder(GUIType.NORMAL, 9, 3)
            .setStructure("" +
                "1 - - - - - - 2 e" +
                "| u # m n p # | e" +
                "3 - - - - - - 4 e"
            )
            .addIngredient('s', OpenSideConfigItem(sideConfigGUI))
            .addIngredient('p', AddNumberItem({ 0..MAX_RANGE }, { range }, ::setRange).also(rangeItems::add))
            .addIngredient('m', RemoveNumberItem({ 0..MAX_RANGE }, { range }, ::setRange).also(rangeItems::add))
            .addIngredient('n', DisplayNumberItem { range + 1 }.also(rangeItems::add))
            .addIngredient('u', OpenUpgradesItem(upgradeHolder))
            .addIngredient('e', EnergyBar(3, energyHolder))
            .build()
        
        private fun setRange(range: Int) {
            this@ChunkLoader.setRange(range)
            rangeItems.forEach(Item::notifyWindows)
        }
        
    }
    
}
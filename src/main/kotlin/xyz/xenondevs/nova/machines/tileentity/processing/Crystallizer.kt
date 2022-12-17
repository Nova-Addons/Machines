package xyz.xenondevs.nova.machines.tileentity.processing

import de.studiocode.invui.gui.GUI
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.guitype.GUIType
import de.studiocode.invui.item.builder.ItemBuilder
import de.studiocode.invui.virtualinventory.event.InventoryUpdatedEvent
import de.studiocode.invui.virtualinventory.event.ItemUpdateEvent
import net.md_5.bungee.api.ChatColor
import net.minecraft.core.particles.ParticleTypes
import org.bukkit.block.BlockFace
import xyz.xenondevs.nmsutils.particle.particle
import xyz.xenondevs.nmsutils.particle.vibration
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.data.config.configReloadable
import xyz.xenondevs.nova.data.recipe.RecipeManager
import xyz.xenondevs.nova.data.world.block.state.NovaTileEntityState
import xyz.xenondevs.nova.machines.registry.Blocks
import xyz.xenondevs.nova.machines.registry.RecipeTypes
import xyz.xenondevs.nova.material.CoreGUIMaterial
import xyz.xenondevs.nova.tileentity.NetworkedTileEntity
import xyz.xenondevs.nova.tileentity.TileEntityPacketTask
import xyz.xenondevs.nova.tileentity.network.NetworkConnectionType
import xyz.xenondevs.nova.tileentity.network.energy.holder.ConsumerEnergyHolder
import xyz.xenondevs.nova.tileentity.network.item.holder.NovaItemHolder
import xyz.xenondevs.nova.tileentity.upgrade.Upgradable
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeType
import xyz.xenondevs.nova.ui.EnergyBar
import xyz.xenondevs.nova.ui.OpenUpgradesItem
import xyz.xenondevs.nova.ui.VerticalBar
import xyz.xenondevs.nova.ui.config.side.OpenSideConfigItem
import xyz.xenondevs.nova.ui.config.side.SideConfigGUI
import xyz.xenondevs.nova.util.BlockSide
import xyz.xenondevs.nova.util.advance
import xyz.xenondevs.nova.util.data.localized
import xyz.xenondevs.nova.util.nmsCopy
import xyz.xenondevs.nova.world.fakeentity.impl.FakeItem

private val MAX_ENERGY = configReloadable { NovaConfig[Blocks.CRYSTALLIZER].getLong("capacity") }
private val ENERGY_PER_TICK = configReloadable { NovaConfig[Blocks.CRYSTALLIZER].getLong("energy_per_tick") }

class Crystallizer(
    blockState: NovaTileEntityState
) : NetworkedTileEntity(blockState), Upgradable {
    
    private val inventory = getInventory("inventory", 1, intArrayOf(1), false, ::handleInventoryUpdate, ::handleInventoryUpdated)
    
    override val gui = lazy(::CrystallizerGUI)
    override val upgradeHolder = getUpgradeHolder(UpgradeType.SPEED, UpgradeType.EFFICIENCY, UpgradeType.ENERGY)
    
    override val energyHolder = ConsumerEnergyHolder(this, MAX_ENERGY, ENERGY_PER_TICK, null, upgradeHolder) {
        createExclusiveSideConfig(NetworkConnectionType.INSERT, BlockSide.BOTTOM)
    }
    override val itemHolder = NovaItemHolder(this, inventory to NetworkConnectionType.BUFFER) {
        createExclusiveSideConfig(NetworkConnectionType.BUFFER, BlockSide.BOTTOM)
    }
    
    private var progressPerTick = 0.0
    private var progress by storedValue("progress") { 0.0 }
    private var recipe = inventory.getItemStack(0)?.let { RecipeManager.getConversionRecipeFor(RecipeTypes.CRYSTALLIZER, it) }
    
    private val particleTask: TileEntityPacketTask
    private var displayState: Boolean
    private val itemDisplay: FakeItem
    
    init {
        reload()
        
        // item display
        val itemStack = inventory.getItemStack(0).nmsCopy
        displayState = !itemStack.isEmpty
        itemDisplay = FakeItem(location.add(.5, .2, .5), displayState) { _, data ->
            data.item = itemStack
            data.hasNoGravity = true
        }
        
        // particle task
        val centerLocation = location.add(.5, .5, .5)
        val packets = listOf(BlockFace.NORTH, BlockFace.WEST, BlockFace.SOUTH, BlockFace.EAST).map {
            val startLocation = location.add(.5, .8, .5).advance(it, .4)
            particle(ParticleTypes.VIBRATION, startLocation) {
                vibration(centerLocation, 10)
            }
        }
        
        particleTask = createPacketTask(packets, 9)
    }
    
    override fun reload() {
        super.reload()
        progressPerTick = upgradeHolder.getValue(UpgradeType.SPEED)
    }
    
    override fun handleRemoved(unload: Boolean) {
        super.handleRemoved(unload)
        itemDisplay.remove()
    }
    
    private fun handleInventoryUpdate(event: ItemUpdateEvent) {
        progress = 0.0
        
        if (event.isRemove) {
            this.recipe = null
            return
        }
        
        val recipe = RecipeManager.getConversionRecipeFor(RecipeTypes.CRYSTALLIZER, event.newItemStack)
        if (recipe == null && event.updateReason != SELF_UPDATE_REASON) {
            event.isCancelled = true
        } else {
            this.recipe = recipe
        }
    }
    
    private fun handleInventoryUpdated(event: InventoryUpdatedEvent) {
        val itemStack = event.newItemStack.nmsCopy
        if (itemStack.isEmpty) {
            if (displayState) {
                itemDisplay.remove()
                displayState = false
            }
            return
        }
        
        itemDisplay.updateEntityData(displayState) {
            item = event.newItemStack.nmsCopy
        }
        
        if (!displayState) {
            itemDisplay.register()
            displayState = true
        }
    }
    
    override fun handleTick() {
        val recipe = recipe
        if (
            recipe != null
            && energyHolder.energy >= energyHolder.energyConsumption
        ) {
            energyHolder.energy -= energyHolder.energyConsumption
            progress += progressPerTick
            
            if (progress >= recipe.time) {
                inventory.setItemStack(SELF_UPDATE_REASON, 0, recipe.result)
            }
            
            if (gui.isInitialized()) {
                gui.value.progressBar.percentage = progress / recipe.time
            }
            
            if (!particleTask.isRunning()) {
                particleTask.start()
            }
        } else if (particleTask.isRunning()) particleTask.stop()
    }
    
    inner class CrystallizerGUI : TileEntityGUI() {
        
        private val sideConfigGUI = SideConfigGUI(
            this@Crystallizer,
            listOf(itemHolder.getNetworkedInventory(inventory) to "inventory.nova.default"),
            ::openWindow
        )
        
        val progressBar = object : VerticalBar(3) {
            override val barMaterial = CoreGUIMaterial.BAR_GREEN
            override fun modifyItemBuilder(itemBuilder: ItemBuilder) =
                itemBuilder.setDisplayName(localized(ChatColor.GRAY, "menu.machines.crystallizer.idle"))
        }
        
        override val gui: GUI = GUIBuilder(GUIType.NORMAL)
            .setStructure(
                "1 - - - - - - - 2",
                "| s # # # p # e |",
                "| u # i # p # e |",
                "| # # # # p # e |",
                "3 - - - - - - - 4")
            .addIngredient('s', OpenSideConfigItem(sideConfigGUI))
            .addIngredient('u', OpenUpgradesItem(upgradeHolder))
            .addIngredient('i', inventory)
            .addIngredient('p', progressBar)
            .addIngredient('e', EnergyBar(3, energyHolder))
            .build()
        
    }
    
}
package xyz.xenondevs.nova.machines.tileentity.processing

import de.studiocode.invui.gui.GUI
import de.studiocode.invui.gui.SlotElement.VISlotElement
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.guitype.GUIType
import de.studiocode.invui.virtualinventory.event.ItemUpdateEvent
import net.minecraft.core.particles.ParticleTypes
import org.bukkit.NamespacedKey
import xyz.xenondevs.nmsutils.particle.particle
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.data.config.configReloadable
import xyz.xenondevs.nova.data.recipe.RecipeManager
import xyz.xenondevs.nova.data.world.block.state.NovaTileEntityState
import xyz.xenondevs.nova.machines.gui.ProgressArrowItem
import xyz.xenondevs.nova.machines.gui.PulverizerProgressItem
import xyz.xenondevs.nova.machines.recipe.PulverizerRecipe
import xyz.xenondevs.nova.machines.registry.Blocks.PULVERIZER
import xyz.xenondevs.nova.machines.registry.RecipeTypes
import xyz.xenondevs.nova.tileentity.NetworkedTileEntity
import xyz.xenondevs.nova.tileentity.network.NetworkConnectionType
import xyz.xenondevs.nova.tileentity.network.energy.holder.ConsumerEnergyHolder
import xyz.xenondevs.nova.tileentity.network.item.holder.NovaItemHolder
import xyz.xenondevs.nova.tileentity.upgrade.Upgradable
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeType
import xyz.xenondevs.nova.ui.EnergyBar
import xyz.xenondevs.nova.ui.OpenUpgradesItem
import xyz.xenondevs.nova.ui.config.side.OpenSideConfigItem
import xyz.xenondevs.nova.ui.config.side.SideConfigGUI
import xyz.xenondevs.nova.util.BlockSide
import xyz.xenondevs.nova.util.advance
import kotlin.math.max

private val MAX_ENERGY = configReloadable { NovaConfig[PULVERIZER].getLong("capacity") }
private val ENERGY_PER_TICK = configReloadable { NovaConfig[PULVERIZER].getLong("energy_per_tick") }
private val PULVERIZE_SPEED by configReloadable { NovaConfig[PULVERIZER].getInt("speed") }

class Pulverizer(blockState: NovaTileEntityState) : NetworkedTileEntity(blockState), Upgradable {
    
    override val gui = lazy { PulverizerGUI() }
    
    private val inputInv = getInventory("input", 1, ::handleInputUpdate)
    private val outputInv = getInventory("output", 2, ::handleOutputUpdate)
    
    override val upgradeHolder = getUpgradeHolder(UpgradeType.SPEED, UpgradeType.EFFICIENCY, UpgradeType.ENERGY)
    override val energyHolder = ConsumerEnergyHolder(this, MAX_ENERGY, ENERGY_PER_TICK, null, upgradeHolder) { createSideConfig(NetworkConnectionType.INSERT, BlockSide.FRONT) }
    override val itemHolder = NovaItemHolder(
        this,
        inputInv to NetworkConnectionType.INSERT,
        outputInv to NetworkConnectionType.EXTRACT
    ) { createSideConfig(NetworkConnectionType.BUFFER, BlockSide.FRONT) }
    
    private var timeLeft = retrieveData("pulverizerTime") { 0 }
    private var pulverizeSpeed = 0
    
    private var currentRecipe: PulverizerRecipe? =
        retrieveDataOrNull<NamespacedKey>("currentRecipe")?.let { RecipeManager.getRecipe(RecipeTypes.PULVERIZER, it) }
    
    private val particleTask = createPacketTask(listOf(
        particle(ParticleTypes.SMOKE) {
            location(centerLocation.advance(getFace(BlockSide.FRONT), 0.6).apply { y += 0.8 })
            offset(0.05, 0.2, 0.05)
            speed(0f)
        }
    ), 6)
    
    init {
        reload()
        if (currentRecipe == null) timeLeft = 0
    }
    
    override fun reload() {
        super.reload()
        pulverizeSpeed = (PULVERIZE_SPEED * upgradeHolder.getValue(UpgradeType.SPEED)).toInt()
    }
    
    override fun handleTick() {
        if (energyHolder.energy >= energyHolder.energyConsumption) {
            if (timeLeft == 0) {
                takeItem()
                
                if (particleTask.isRunning()) particleTask.stop()
            } else {
                timeLeft = max(timeLeft - pulverizeSpeed, 0)
                energyHolder.energy -= energyHolder.energyConsumption
                
                if (!particleTask.isRunning()) particleTask.start()
                
                if (timeLeft == 0) {
                    outputInv.addItem(SELF_UPDATE_REASON, currentRecipe!!.result)
                    currentRecipe = null
                }
                
                if (gui.isInitialized()) gui.value.updateProgress()
            }
            
        } else if (particleTask.isRunning()) particleTask.stop()
    }
    
    private fun takeItem() {
        val inputItem = inputInv.getItemStack(0)
        if (inputItem != null) {
            val recipe = RecipeManager.getConversionRecipeFor(RecipeTypes.PULVERIZER, inputItem)!!
            val result = recipe.result
            if (outputInv.canHold(result)) {
                inputInv.addItemAmount(SELF_UPDATE_REASON, 0, -1)
                timeLeft = recipe.time
                currentRecipe = recipe
            }
        }
    }
    
    private fun handleInputUpdate(event: ItemUpdateEvent) {
        event.isCancelled = event.newItemStack != null && RecipeManager.getConversionRecipeFor(RecipeTypes.PULVERIZER, event.newItemStack) == null
    }
    
    private fun handleOutputUpdate(event: ItemUpdateEvent) {
        event.isCancelled = !event.isRemove && event.updateReason != SELF_UPDATE_REASON
    }
    
    override fun saveData() {
        super.saveData()
        storeData("pulverizerTime", timeLeft)
        storeData("currentRecipe", currentRecipe?.key)
    }
    
    inner class PulverizerGUI : TileEntityGUI() {
        
        private val mainProgress = ProgressArrowItem()
        private val pulverizerProgress = PulverizerProgressItem()
        
        private val sideConfigGUI = SideConfigGUI(
            this@Pulverizer,
            listOf(
                itemHolder.getNetworkedInventory(inputInv) to "inventory.nova.input",
                itemHolder.getNetworkedInventory(outputInv) to "inventory.nova.output"
            ),
            ::openWindow
        )
        
        override val gui: GUI = GUIBuilder(GUIType.NORMAL)
            .setStructure(
                "1 - - - - - - - 2",
                "| s u # # # # e |",
                "| i # , # o a e |",
                "| c # # # # # e |",
                "3 - - - - - - - 4")
            .addIngredient('i', VISlotElement(inputInv, 0))
            .addIngredient('o', VISlotElement(outputInv, 0))
            .addIngredient('a', VISlotElement(outputInv, 1))
            .addIngredient(',', mainProgress)
            .addIngredient('c', pulverizerProgress)
            .addIngredient('s', OpenSideConfigItem(sideConfigGUI))
            .addIngredient('u', OpenUpgradesItem(upgradeHolder))
            .addIngredient('e', EnergyBar(3, energyHolder))
            .build()
        
        init {
            updateProgress()
        }
        
        fun updateProgress() {
            val recipeTime = currentRecipe?.time ?: 0
            val percentage = if (timeLeft == 0) 0.0 else (recipeTime - timeLeft).toDouble() / recipeTime.toDouble()
            mainProgress.percentage = percentage
            pulverizerProgress.percentage = percentage
        }
        
    }
    
}
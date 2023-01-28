package xyz.xenondevs.nova.machines.tileentity.processing


import net.minecraft.world.SimpleContainer
import net.minecraft.world.item.crafting.RecipeType
import net.minecraft.world.item.crafting.SmeltingRecipe
import org.bukkit.NamespacedKey
import org.bukkit.World
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.commons.provider.mutable.map
import xyz.xenondevs.invui.gui.SlotElement
import xyz.xenondevs.invui.gui.builder.GuiBuilder
import xyz.xenondevs.invui.gui.builder.guitype.GuiType
import xyz.xenondevs.invui.virtualinventory.event.ItemUpdateEvent
import xyz.xenondevs.invui.virtualinventory.event.PlayerUpdateReason
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.data.config.configReloadable
import xyz.xenondevs.nova.data.world.block.state.NovaTileEntityState
import xyz.xenondevs.nova.machines.gui.ProgressArrowItem
import xyz.xenondevs.nova.machines.registry.Blocks.ELECTRIC_FURNACE
import xyz.xenondevs.nova.tileentity.NetworkedTileEntity
import xyz.xenondevs.nova.tileentity.network.NetworkConnectionType
import xyz.xenondevs.nova.tileentity.network.item.holder.NovaItemHolder
import xyz.xenondevs.nova.tileentity.upgrade.Upgradable
import xyz.xenondevs.nova.ui.EnergyBar
import xyz.xenondevs.nova.ui.OpenUpgradesItem
import xyz.xenondevs.nova.ui.config.side.OpenSideConfigItem
import xyz.xenondevs.nova.ui.config.side.SideConfigGui
import xyz.xenondevs.nova.util.BlockSide
import xyz.xenondevs.nova.util.bukkitCopy
import xyz.xenondevs.nova.util.intValue
import xyz.xenondevs.nova.util.minecraftServer
import xyz.xenondevs.nova.util.namespacedKey
import xyz.xenondevs.nova.util.nmsCopy
import xyz.xenondevs.nova.util.resourceLocation
import xyz.xenondevs.nova.util.serverLevel
import xyz.xenondevs.nova.util.spawnExpOrb
import xyz.xenondevs.simpleupgrades.ConsumerEnergyHolder
import xyz.xenondevs.simpleupgrades.registry.UpgradeTypes

private fun getRecipe(input: ItemStack, world: World): SmeltingRecipe? {
    return minecraftServer.recipeManager.getAllRecipesFor(RecipeType.SMELTING)
        .firstOrNull { it.matches(SimpleContainer(input.nmsCopy), world.serverLevel) }
}

private val MAX_ENERGY = configReloadable { NovaConfig[ELECTRIC_FURNACE].getLong("capacity") }
private val ENERGY_PER_TICK = configReloadable { NovaConfig[ELECTRIC_FURNACE].getLong("energy_per_tick") }
private val COOK_SPEED by configReloadable { NovaConfig[ELECTRIC_FURNACE].getInt("cook_speed") }

class ElectricFurnace(blockState: NovaTileEntityState) : NetworkedTileEntity(blockState), Upgradable {
    
    override val gui = lazy { ElectricFurnaceGui() }
    
    private val inputInventory = getInventory("input", 1, ::handleInputInventoryUpdate)
    private val outputInventory = getInventory("output", 1, ::handleOutputInventoryUpdate)
    
    override val upgradeHolder = getUpgradeHolder(UpgradeTypes.SPEED, UpgradeTypes.EFFICIENCY, UpgradeTypes.ENERGY)
    override val energyHolder = ConsumerEnergyHolder(this, MAX_ENERGY, ENERGY_PER_TICK, null, upgradeHolder) { createSideConfig(NetworkConnectionType.INSERT, BlockSide.FRONT) }
    override val itemHolder = NovaItemHolder(
        this,
        inputInventory to NetworkConnectionType.INSERT,
        outputInventory to NetworkConnectionType.EXTRACT
    ) { createSideConfig(NetworkConnectionType.BUFFER, BlockSide.FRONT) }
    
    private var currentRecipe: SmeltingRecipe? by storedValue<NamespacedKey>("currentRecipe").map(
        { minecraftServer.recipeManager.byKey(it.resourceLocation).orElse(null) as? SmeltingRecipe },
        { it.id.namespacedKey }
    )
    private var timeCooked: Int by storedValue("timeCooked") { 0 }
    private var experience: Float by storedValue("experience") { 0f }
    
    private var cookSpeed = 0
    
    private var active: Boolean = false
        set(active) {
            if (field != active) {
                field = active
                blockState.modelProvider.update(active.intValue)
            }
        }
    
    init {
        reload()
    }
    
    override fun reload() {
        super.reload()
        cookSpeed = (COOK_SPEED * upgradeHolder.getValue(UpgradeTypes.SPEED)).toInt()
    }
    
    private fun handleInputInventoryUpdate(event: ItemUpdateEvent) {
        if (event.newItemStack != null) {
            val itemStack = event.newItemStack
            if (getRecipe(itemStack, world) == null) event.isCancelled = true
        }
    }
    
    private fun handleOutputInventoryUpdate(event: ItemUpdateEvent) {
        val updateReason = event.updateReason
        if (updateReason == SELF_UPDATE_REASON) return
        
        if (event.isRemove) {
            if (updateReason is PlayerUpdateReason) {
                val player = updateReason.player
                if (event.newItemStack == null) { // took all items
                    experience -= pos.block.spawnExpOrb(experience.toInt(), player.location)
                } else {
                    val amount = event.removedAmount
                    val experiencePerItem = experience / event.previousItemStack.amount
                    val experience = amount * experiencePerItem
                    
                    this.experience -= pos.block.spawnExpOrb(experience.toInt(), player.location)
                }
            }
        } else event.isCancelled = true
    }
    
    override fun getExp(): Int = experience.toInt()
    
    override fun handleTick() {
        if (energyHolder.energy >= energyHolder.energyConsumption) {
            if (currentRecipe == null) {
                val item = inputInventory.getItemStack(0)
                if (item != null) {
                    val recipe = getRecipe(item, world)
                    if (recipe != null && outputInventory.canHold(recipe.resultItem.bukkitCopy)) {
                        currentRecipe = recipe
                        inputInventory.addItemAmount(null, 0, -1)
                        
                        active = true
                    } else active = false
                } else active = false
            }
            
            val currentRecipe = currentRecipe
            if (currentRecipe != null) {
                energyHolder.energy -= energyHolder.energyConsumption
                timeCooked += cookSpeed
                
                if (timeCooked >= currentRecipe.cookingTime) {
                    outputInventory.addItem(SELF_UPDATE_REASON, currentRecipe.resultItem.bukkitCopy)
                    experience += currentRecipe.experience
                    timeCooked = 0
                    this.currentRecipe = null
                }
                
                if (gui.isInitialized()) gui.value.updateProgress()
            }
        } else active = false
    }
    
    inner class ElectricFurnaceGui : TileEntityGui() {
        
        private val progressItem = ProgressArrowItem()
        
        private val sideConfigGui = SideConfigGui(
            this@ElectricFurnace,
            listOf(
                itemHolder.getNetworkedInventory(inputInventory) to "inventory.nova.input",
                itemHolder.getNetworkedInventory(outputInventory) to "inventory.nova.output"
            ),
            ::openWindow
        )
        
        override val gui = GuiBuilder(GuiType.NORMAL)
            .setStructure(
                "1 - - - - - - - 2",
                "| s u # # # # e |",
                "| i # > # o # e |",
                "| # # # # # # e |",
                "3 - - - - - - - 4")
            .addIngredient('i', SlotElement.VISlotElement(inputInventory, 0))
            .addIngredient('o', SlotElement.VISlotElement(outputInventory, 0))
            .addIngredient('>', progressItem)
            .addIngredient('s', OpenSideConfigItem(sideConfigGui))
            .addIngredient('u', OpenUpgradesItem(upgradeHolder))
            .addIngredient('e', EnergyBar(3, energyHolder))
            .build()
        
        init {
            updateProgress()
        }
        
        fun updateProgress() {
            val cookTime = currentRecipe?.cookingTime ?: 0
            progressItem.percentage = if (timeCooked == 0) 0.0 else timeCooked.toDouble() / cookTime.toDouble()
        }
        
    }
    
}

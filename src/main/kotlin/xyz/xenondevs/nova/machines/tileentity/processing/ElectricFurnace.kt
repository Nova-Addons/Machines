package xyz.xenondevs.nova.machines.tileentity.processing


import de.studiocode.invui.gui.GUI
import de.studiocode.invui.gui.SlotElement
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.guitype.GUIType
import de.studiocode.invui.virtualinventory.event.ItemUpdateEvent
import de.studiocode.invui.virtualinventory.event.PlayerUpdateReason
import net.minecraft.world.SimpleContainer
import net.minecraft.world.item.crafting.RecipeType
import net.minecraft.world.item.crafting.SmeltingRecipe
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.World
import org.bukkit.entity.EntityType
import org.bukkit.entity.ExperienceOrb
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.data.serialization.cbf.element.CompoundElement
import xyz.xenondevs.nova.machines.gui.ProgressArrowItem
import xyz.xenondevs.nova.machines.registry.Blocks.ELECTRIC_FURNACE
import xyz.xenondevs.nova.material.TileEntityNovaMaterial
import xyz.xenondevs.nova.tileentity.NetworkedTileEntity
import xyz.xenondevs.nova.tileentity.SELF_UPDATE_REASON
import xyz.xenondevs.nova.tileentity.network.NetworkConnectionType
import xyz.xenondevs.nova.tileentity.network.energy.EnergyConnectionType
import xyz.xenondevs.nova.tileentity.network.energy.holder.ConsumerEnergyHolder
import xyz.xenondevs.nova.tileentity.network.item.holder.NovaItemHolder
import xyz.xenondevs.nova.tileentity.upgrade.Upgradable
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeHolder
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeType
import xyz.xenondevs.nova.ui.EnergyBar
import xyz.xenondevs.nova.ui.OpenUpgradesItem
import xyz.xenondevs.nova.ui.config.side.OpenSideConfigItem
import xyz.xenondevs.nova.ui.config.side.SideConfigGUI
import xyz.xenondevs.nova.util.*
import xyz.xenondevs.nova.world.armorstand.FakeArmorStand
import java.util.*

private fun getRecipe(input: ItemStack, world: World): SmeltingRecipe? {
    return minecraftServer.recipeManager.getAllRecipesFor(RecipeType.SMELTING)
        .first { it.matches(SimpleContainer(input.nmsStack), world.serverLevel) }
}

private val MAX_ENERGY = NovaConfig[ELECTRIC_FURNACE].getLong("capacity")!!
private val ENERGY_PER_TICK = NovaConfig[ELECTRIC_FURNACE].getLong("energy_per_tick")!!
private val COOK_SPEED = NovaConfig[ELECTRIC_FURNACE].getInt("cook_speed")!!

class ElectricFurnace(
    uuid: UUID,
    data: CompoundElement,
    material: TileEntityNovaMaterial,
    ownerUUID: UUID,
    armorStand: FakeArmorStand,
) : NetworkedTileEntity(uuid, data, material, ownerUUID, armorStand), Upgradable {
    
    override val gui = lazy { ElectricFurnaceGUI() }
    
    private val inputInventory = getInventory("input", 1, ::handleInputInventoryUpdate)
    private val outputInventory = getInventory("output", 1, ::handleOutputInventoryUpdate)
    
    override val upgradeHolder = UpgradeHolder(this, gui, ::handleUpgradeUpdates, allowed = UpgradeType.ALL_ENERGY)
    override val energyHolder = ConsumerEnergyHolder(this, MAX_ENERGY, ENERGY_PER_TICK, 0, upgradeHolder) { createEnergySideConfig(EnergyConnectionType.CONSUME, BlockSide.FRONT) }
    override val itemHolder = NovaItemHolder(
        this,
        inputInventory to NetworkConnectionType.BUFFER,
        outputInventory to NetworkConnectionType.EXTRACT
    ) { createSideConfig(NetworkConnectionType.INSERT, BlockSide.FRONT) }
    
    private var currentRecipe: SmeltingRecipe? = retrieveOrNull<NamespacedKey>("currentRecipe")
        ?.let { minecraftServer.recipeManager.byKey(it.resourceLocation).orElse(null) as SmeltingRecipe? }
    private var timeCooked = retrieveData("timeCooked") { 0 }
    private var experience = retrieveData("exp") { 0f }
    
    private var cookSpeed = 0
    
    private var active: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                updateHeadStack()
            }
        }
    
    init {
        handleUpgradeUpdates()
    }
    
    override fun saveData() {
        super.saveData()
        storeData("currentRecipe", currentRecipe?.id?.namespacedKey)
        storeData("timeCooked", timeCooked)
        storeData("experience", experience)
    }
    
    override fun getHeadStack(): ItemStack {
        return material.block!!.createItemStack(active.intValue)
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
                    spawnExperienceOrb(player.location, experience)
                    experience = 0f
                } else {
                    val amount = event.removedAmount
                    val experiencePerItem = experience / event.previousItemStack.amount
                    val experience = amount * experiencePerItem
                    spawnExperienceOrb(player.location, experience)
                    this.experience -= experience
                }
            }
        } else event.isCancelled = true
    }
    
    private fun spawnExperienceOrb(location: Location, experience: Float) {
        if (experience == 0f) return
        
        val orb = location.world!!.spawnEntity(location, EntityType.EXPERIENCE_ORB) as ExperienceOrb
        orb.experience += experience.toInt()
    }
    
    override fun handleTick() {
        if (energyHolder.energy >= energyHolder.energyConsumption) {
            if (currentRecipe == null) {
                val item = inputInventory.getItemStack(0)
                if (item != null) {
                    val recipe = getRecipe(item, world)
                    if (recipe != null && outputInventory.canHold(recipe.resultItem.bukkitStack)) {
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
                    outputInventory.addItem(SELF_UPDATE_REASON, currentRecipe.resultItem.bukkitStack)
                    experience += currentRecipe.experience
                    timeCooked = 0
                    this.currentRecipe = null
                }
                
                if (gui.isInitialized()) gui.value.updateProgress()
            }
        } else active = false
    }
    
    private fun handleUpgradeUpdates() {
        cookSpeed = (COOK_SPEED * upgradeHolder.getSpeedModifier()).toInt()
    }
    
    override fun handleRemoved(unload: Boolean) {
        super.handleRemoved(unload)
        if (!unload)
            spawnExperienceOrb(armorStand.location, experience)
    }
    
    inner class ElectricFurnaceGUI : TileEntityGUI() {
        
        private val progressItem = ProgressArrowItem()
        
        private val sideConfigGUI = SideConfigGUI(
            this@ElectricFurnace,
            listOf(EnergyConnectionType.NONE, EnergyConnectionType.CONSUME),
            listOf(
                itemHolder.getNetworkedInventory(inputInventory) to "inventory.nova.input",
                itemHolder.getNetworkedInventory(outputInventory) to "inventory.nova.output"
            )
        ) { openWindow(it) }
        
        override val gui: GUI = GUIBuilder(GUIType.NORMAL, 9, 5)
            .setStructure("" +
                "1 - - - - - - - 2" +
                "| s u # # # # e |" +
                "| i # > # o # e |" +
                "| # # # # # # e |" +
                "3 - - - - - - - 4")
            .addIngredient('i', SlotElement.VISlotElement(inputInventory, 0))
            .addIngredient('o', SlotElement.VISlotElement(outputInventory, 0))
            .addIngredient('>', progressItem)
            .addIngredient('s', OpenSideConfigItem(sideConfigGUI))
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

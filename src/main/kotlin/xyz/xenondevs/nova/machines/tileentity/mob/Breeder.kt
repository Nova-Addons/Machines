package xyz.xenondevs.nova.machines.tileentity.mob

import net.md_5.bungee.api.ChatColor
import org.bukkit.entity.Animals
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.invui.gui.builder.GuiBuilder
import xyz.xenondevs.invui.gui.builder.guitype.GuiType
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.builder.ItemBuilder
import xyz.xenondevs.invui.virtualinventory.event.ItemUpdateEvent
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.data.config.configReloadable
import xyz.xenondevs.nova.data.world.block.state.NovaTileEntityState
import xyz.xenondevs.nova.machines.registry.Blocks.BREEDER
import xyz.xenondevs.nova.material.CoreGuiMaterial
import xyz.xenondevs.nova.tileentity.NetworkedTileEntity
import xyz.xenondevs.nova.tileentity.network.NetworkConnectionType
import xyz.xenondevs.nova.tileentity.network.item.holder.NovaItemHolder
import xyz.xenondevs.nova.tileentity.upgrade.Upgradable
import xyz.xenondevs.nova.ui.EnergyBar
import xyz.xenondevs.nova.ui.OpenUpgradesItem
import xyz.xenondevs.nova.ui.VerticalBar
import xyz.xenondevs.nova.ui.config.side.OpenSideConfigItem
import xyz.xenondevs.nova.ui.config.side.SideConfigGui
import xyz.xenondevs.nova.ui.item.AddNumberItem
import xyz.xenondevs.nova.ui.item.DisplayNumberItem
import xyz.xenondevs.nova.ui.item.RemoveNumberItem
import xyz.xenondevs.nova.ui.item.VisualizeRegionItem
import xyz.xenondevs.nova.util.BlockSide
import xyz.xenondevs.nova.util.data.localized
import xyz.xenondevs.nova.util.getSurroundingChunks
import xyz.xenondevs.nova.util.item.FoodUtils
import xyz.xenondevs.nova.util.item.canBredNow
import xyz.xenondevs.nova.util.item.genericMaxHealth
import xyz.xenondevs.nova.world.region.Region
import xyz.xenondevs.nova.world.region.VisualRegion
import xyz.xenondevs.simpleupgrades.ConsumerEnergyHolder
import xyz.xenondevs.simpleupgrades.registry.UpgradeTypes
import kotlin.math.min

private val MAX_ENERGY = configReloadable { NovaConfig[BREEDER].getLong("capacity") }
private val ENERGY_PER_TICK = configReloadable { NovaConfig[BREEDER].getLong("energy_per_tick") }
private val ENERGY_PER_BREED = configReloadable { NovaConfig[BREEDER].getLong("energy_per_breed") }
private val IDLE_TIME by configReloadable { NovaConfig[BREEDER].getInt("idle_time") }
private val BREED_LIMIT by configReloadable { NovaConfig[BREEDER].getInt("breed_limit") }
private val MIN_RANGE by configReloadable { NovaConfig[BREEDER].getInt("range.min") }
private val MAX_RANGE by configReloadable { NovaConfig[BREEDER].getInt("range.max") }
private val DEFAULT_RANGE by configReloadable { NovaConfig[BREEDER].getInt("range.default") }

class Breeder(blockState: NovaTileEntityState) : NetworkedTileEntity(blockState), Upgradable {
    
    private val inventory = getInventory("inventory", 9, ::handleInventoryUpdate)
    override val gui = lazy { MobCrusherGui() }
    override val upgradeHolder = getUpgradeHolder(UpgradeTypes.SPEED, UpgradeTypes.EFFICIENCY, UpgradeTypes.ENERGY, UpgradeTypes.RANGE)
    override val energyHolder = ConsumerEnergyHolder(this, MAX_ENERGY, ENERGY_PER_TICK, ENERGY_PER_BREED, upgradeHolder) { createSideConfig(NetworkConnectionType.INSERT) }
    override val itemHolder = NovaItemHolder(this, inventory to NetworkConnectionType.INSERT) { createSideConfig(NetworkConnectionType.INSERT, BlockSide.FRONT) }
    
    private lateinit var region: Region
    
    private var timePassed = 0
    private var maxIdleTime = 0
    private var maxRange = 0
    private var range = retrieveData("range") { DEFAULT_RANGE }
        set(value) {
            field = value
            updateRegion()
            if (gui.isInitialized()) gui.value.updateRangeItems()
        }
    
    init {
        reload()
        updateRegion()
    }
    
    override fun reload() {
        super.reload()
        
        maxIdleTime = (IDLE_TIME / upgradeHolder.getValue(UpgradeTypes.SPEED)).toInt()
        if (timePassed > maxIdleTime) timePassed = maxIdleTime
        
        maxRange = MAX_RANGE + upgradeHolder.getValue(UpgradeTypes.RANGE)
        if (maxRange < range) range = maxRange
    }
    
    private fun updateRegion() {
        region = getBlockFrontRegion(range, range, 4, -1)
        VisualRegion.updateRegion(uuid, region)
    }
    
    override fun saveData() {
        super.saveData()
        storeData("range", range)
    }
    
    override fun handleTick() {
        if (energyHolder.energy >= energyHolder.energyConsumption) {
            energyHolder.energy -= energyHolder.energyConsumption
            
            if (timePassed++ >= maxIdleTime) {
                timePassed = 0
                
                val breedableEntities =
                    location
                        .chunk
                        .getSurroundingChunks(1, includeCurrent = true, ignoreUnloaded = true)
                        .flatMap { it.entities.asList() }
                        .filterIsInstance<Animals>()
                        .filter { it.canBredNow && it.location in region }
                
                var breedsLeft = min((energyHolder.energy / energyHolder.specialEnergyConsumption).toInt(), BREED_LIMIT)
                for (animal in breedableEntities) {
                    val success = if (FoodUtils.requiresHealing(animal)) tryHeal(animal)
                    else tryBreed(animal)
                    
                    if (success) {
                        breedsLeft--
                        energyHolder.energy -= energyHolder.specialEnergyConsumption
                        if (breedsLeft == 0) break
                    }
                }
            }
        }
        
        if (gui.isInitialized()) gui.value.idleBar.percentage = timePassed / maxIdleTime.toDouble()
    }
    
    private fun tryHeal(animal: Animals): Boolean {
        for ((index, item) in inventory.items.withIndex()) {
            if (item == null) continue
            
            val healAmount = FoodUtils.getHealAmount(animal, item.type)
            if (healAmount > 0) {
                animal.health = min(animal.health + healAmount, animal.genericMaxHealth)
                inventory.addItemAmount(SELF_UPDATE_REASON, index, -1)
                
                val remains = FoodUtils.getItemRemains(item.type)
                if (remains != null)
                    inventory.setItemStack(SELF_UPDATE_REASON, index, ItemStack(remains))
                
                return true
            }
        }
        
        return false
    }
    
    private fun tryBreed(animal: Animals): Boolean {
        for ((index, item) in inventory.items.withIndex()) {
            if (item == null) continue
            
            if (FoodUtils.canUseBreedFood(animal, item.type)) {
                animal.loveModeTicks = 600
                inventory.addItemAmount(SELF_UPDATE_REASON, index, -1)
                
                val remains = FoodUtils.getItemRemains(item.type)
                if (remains != null)
                    inventory.setItemStack(SELF_UPDATE_REASON, index, ItemStack(remains))
                
                return true
            }
        }
        
        return false
    }
    
    private fun handleInventoryUpdate(event: ItemUpdateEvent) {
        if (event.updateReason != SELF_UPDATE_REASON && !event.isRemove && !FoodUtils.isFood(event.newItemStack.type))
            event.isCancelled = true
    }
    
    override fun handleRemoved(unload: Boolean) {
        super.handleRemoved(unload)
        VisualRegion.removeRegion(uuid)
    }
    
    inner class MobCrusherGui : TileEntityGui() {
        
        private val sideConfigGui = SideConfigGui(
            this@Breeder,
            listOf(itemHolder.getNetworkedInventory(inventory) to "inventory.nova.default"),
            ::openWindow
        )
        
        private val rangeItems = ArrayList<Item>()
        
        val idleBar = object : VerticalBar(3) {
            override val barMaterial = CoreGuiMaterial.BAR_GREEN
            override fun modifyItemBuilder(itemBuilder: ItemBuilder) =
                itemBuilder.setDisplayName(localized(ChatColor.GRAY, "menu.machines.breeder.idle", maxIdleTime - timePassed))
        }
        
        override val gui = GuiBuilder(GuiType.NORMAL)
            .setStructure(
                "1 - - - - - - - 2",
                "| s p i i i b e |",
                "| r n i i i b e |",
                "| u m i i i b e |",
                "3 - - - - - - - 4")
            .addIngredient('i', inventory)
            .addIngredient('s', OpenSideConfigItem(sideConfigGui))
            .addIngredient('r', VisualizeRegionItem(uuid) { region })
            .addIngredient('u', OpenUpgradesItem(upgradeHolder))
            .addIngredient('p', AddNumberItem({ MIN_RANGE..maxRange }, { range }, { range = it }).also(rangeItems::add))
            .addIngredient('m', RemoveNumberItem({ MIN_RANGE..maxRange }, { range }, { range = it }).also(rangeItems::add))
            .addIngredient('n', DisplayNumberItem { range }.also(rangeItems::add))
            .addIngredient('e', EnergyBar(3, energyHolder))
            .addIngredient('b', idleBar)
            .build()
        
        fun updateRangeItems() = rangeItems.forEach(Item::notifyWindows)
        
    }
    
}
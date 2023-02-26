package xyz.xenondevs.nova.machines.tileentity.agriculture

import net.md_5.bungee.api.chat.TranslatableComponent
import net.minecraft.world.entity.projectile.FishingHook
import net.minecraft.world.level.storage.loot.BuiltInLootTables
import net.minecraft.world.level.storage.loot.LootContext
import net.minecraft.world.level.storage.loot.LootTable
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets
import net.minecraft.world.level.storage.loot.parameters.LootContextParams
import net.minecraft.world.phys.Vec3
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.craftbukkit.v1_19_R2.CraftServer
import org.bukkit.craftbukkit.v1_19_R2.inventory.CraftItemStack
import org.bukkit.craftbukkit.v1_19_R2.util.RandomSourceWrapper
import org.bukkit.enchantments.Enchantment
import xyz.xenondevs.invui.gui.Gui
import xyz.xenondevs.invui.gui.SlotElement.VISlotElement
import xyz.xenondevs.invui.item.builder.ItemBuilder
import xyz.xenondevs.invui.virtualinventory.event.ItemUpdateEvent
import xyz.xenondevs.nova.data.config.GlobalValues
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.data.config.configReloadable
import xyz.xenondevs.nova.data.world.block.state.NovaTileEntityState
import xyz.xenondevs.nova.machines.registry.Blocks.AUTO_FISHER
import xyz.xenondevs.nova.machines.registry.GuiMaterials
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
import xyz.xenondevs.nova.util.BlockSide
import xyz.xenondevs.nova.util.EntityUtils
import xyz.xenondevs.nova.util.item.DamageableUtils
import xyz.xenondevs.nova.util.serverLevel
import xyz.xenondevs.simpleupgrades.ConsumerEnergyHolder
import xyz.xenondevs.simpleupgrades.registry.UpgradeTypes
import java.util.*

private val MAX_ENERGY = configReloadable { NovaConfig[AUTO_FISHER].getLong("capacity") }
private val ENERGY_PER_TICK = configReloadable { NovaConfig[AUTO_FISHER].getLong("energy_per_tick") }
private val IDLE_TIME by configReloadable { NovaConfig[AUTO_FISHER].getInt("idle_time") }

class AutoFisher(blockState: NovaTileEntityState) : NetworkedTileEntity(blockState), Upgradable {
    
    private val inventory = getInventory("inventory", 12, ::handleInventoryUpdate)
    private val fishingRodInventory = getInventory("fishingRod", 1, ::handleFishingRodInventoryUpdate)
    override val gui = lazy(::AutoFisherGui)
    override val upgradeHolder = getUpgradeHolder(UpgradeTypes.SPEED, UpgradeTypes.EFFICIENCY, UpgradeTypes.ENERGY)
    override val energyHolder = ConsumerEnergyHolder(this, MAX_ENERGY, ENERGY_PER_TICK, null, upgradeHolder) { createSideConfig(NetworkConnectionType.INSERT, BlockSide.BOTTOM) }
    override val itemHolder = NovaItemHolder(
        this,
        inventory to NetworkConnectionType.EXTRACT,
        fishingRodInventory to NetworkConnectionType.INSERT
    ) { createSideConfig(NetworkConnectionType.BUFFER, BlockSide.BOTTOM) }
    
    private var timePassed = 0
    private var maxIdleTime = 0
    
    private val waterBlock = location.clone().subtract(0.0, 1.0, 0.0).block
    private val random = RandomSourceWrapper(Random(uuid.mostSignificantBits xor System.currentTimeMillis()))
    private val level = world.serverLevel
    private val position = Vec3(centerLocation.x, location.y - 0.5, centerLocation.z)
    private val itemDropLocation = location.clone().add(0.0, 1.0, 0.0)
    private val fakePlayer = EntityUtils.createFakePlayer(location)
    
    init {
        reload()
    }
    
    override fun reload() {
        super.reload()
        maxIdleTime = (IDLE_TIME / upgradeHolder.getValue(UpgradeTypes.SPEED)).toInt()
        if (timePassed > maxIdleTime) timePassed = maxIdleTime
    }
    
    override fun handleTick() {
        if (energyHolder.energy >= energyHolder.energyConsumption && !fishingRodInventory.isEmpty && waterBlock.type == Material.WATER) {
            if (!GlobalValues.DROP_EXCESS_ON_GROUND && !inventory.hasEmptySlot()) return
            
            energyHolder.energy -= energyHolder.energyConsumption
            
            timePassed++
            if (timePassed >= maxIdleTime) {
                timePassed = 0
                fish()
            }
            
            if (gui.isInitialized()) gui.value.idleBar.percentage = timePassed.toDouble() / maxIdleTime.toDouble()
        }
    }
    
    private fun fish() {
        // Bukkit's LootTable API isn't applicable in this use case
        
        val rodItem = fishingRodInventory.getItemStack(0)
        val luck = rodItem.enchantments[Enchantment.LUCK] ?: 0
        
        // the fake fishing hook is required for the "in_open_water" check as the
        // fishing location affects the loot table
        val fakeFishingHook = FishingHook(fakePlayer, level, luck, 0)
        
        val contextBuilder = LootContext.Builder(level)
            .withParameter(LootContextParams.ORIGIN, position)
            .withParameter(LootContextParams.TOOL, CraftItemStack.asNMSCopy(rodItem))
            .withParameter(LootContextParams.THIS_ENTITY, fakeFishingHook)
            .withRandom(random)
            .withLuck(luck.toFloat())
        
        val server = (Bukkit.getServer() as CraftServer).server
        val lootTable: LootTable = server.lootTables.get(BuiltInLootTables.FISHING)
        
        val list = lootTable.getRandomItems(contextBuilder.create(LootContextParamSets.FISHING))
        
        list.stream()
            .map { CraftItemStack.asCraftMirror(it) }
            .forEach {
                val leftover = inventory.addItem(SELF_UPDATE_REASON, it)
                if (GlobalValues.DROP_EXCESS_ON_GROUND && leftover != 0) {
                    it.amount = leftover
                    world.dropItemNaturally(itemDropLocation, it)
                }
            }
        
        // damage the rod item
        useRod()
    }
    
    private fun useRod() {
        val itemStack = fishingRodInventory.getItemStack(0)!!
        fishingRodInventory.setItemStack(SELF_UPDATE_REASON, 0, DamageableUtils.damageItem(itemStack))
    }
    
    private fun handleInventoryUpdate(event: ItemUpdateEvent) {
        event.isCancelled = event.updateReason != SELF_UPDATE_REASON && event.isAdd
    }
    
    private fun handleFishingRodInventoryUpdate(event: ItemUpdateEvent) {
        event.isCancelled = event.isAdd && event.newItemStack.type != Material.FISHING_ROD
    }
    
    inner class AutoFisherGui : TileEntityGui() {
        
        private val sideConfigGui = SideConfigGui(
            this@AutoFisher,
            listOf(
                itemHolder.getNetworkedInventory(inventory) to "inventory.nova.default",
                itemHolder.getNetworkedInventory(fishingRodInventory) to "inventory.machines.fishing_rod"
            ),
            ::openWindow
        )
        
        val idleBar = object : VerticalBar(height = 3) {
            override val barMaterial = CoreGuiMaterial.BAR_GREEN
            override fun modifyItemBuilder(itemBuilder: ItemBuilder) =
                itemBuilder.setDisplayName(TranslatableComponent("menu.machines.auto_fisher.idle", maxIdleTime - timePassed))
        }
        
        override val gui = Gui.normal()
            .setStructure(
                "1 - - - - - - - 2",
                "| s u # # f p e |",
                "| i i i i # p e |",
                "| i i i i # p e |",
                "3 - - - - - - - 4")
            .addIngredient('i', inventory)
            .addIngredient('s', OpenSideConfigItem(sideConfigGui))
            .addIngredient('f', VISlotElement(fishingRodInventory, 0, GuiMaterials.FISHING_ROD_PLACEHOLDER.clientsideProvider))
            .addIngredient('u', OpenUpgradesItem(upgradeHolder))
            .addIngredient('e', EnergyBar(3, energyHolder))
            .addIngredient('p', idleBar)
            .build()
        
    }
    
}
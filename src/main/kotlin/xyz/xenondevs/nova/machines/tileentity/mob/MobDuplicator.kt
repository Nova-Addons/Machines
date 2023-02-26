package xyz.xenondevs.nova.machines.tileentity.mob

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import net.md_5.bungee.api.ChatColor
import net.minecraft.world.entity.Mob
import org.bukkit.Sound
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.commons.gson.isString
import xyz.xenondevs.invui.gui.Gui
import xyz.xenondevs.invui.gui.SlotElement.VISlotElement
import xyz.xenondevs.invui.item.ItemProvider
import xyz.xenondevs.invui.item.builder.ItemBuilder
import xyz.xenondevs.invui.item.builder.SkullBuilder
import xyz.xenondevs.invui.item.builder.SkullBuilder.HeadTexture
import xyz.xenondevs.invui.item.impl.BaseItem
import xyz.xenondevs.invui.virtualinventory.event.ItemUpdateEvent
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.data.config.configReloadable
import xyz.xenondevs.nova.data.world.block.state.NovaTileEntityState
import xyz.xenondevs.nova.machines.item.MobCatcherBehavior
import xyz.xenondevs.nova.machines.registry.Blocks.MOB_DUPLICATOR
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
import xyz.xenondevs.nova.util.center
import xyz.xenondevs.nova.util.data.NBTUtils
import xyz.xenondevs.nova.util.data.localized
import xyz.xenondevs.nova.util.isBetweenXZ
import xyz.xenondevs.nova.util.item.novaMaterial
import xyz.xenondevs.nova.util.nmsEntity
import xyz.xenondevs.nova.util.runAsyncTask
import xyz.xenondevs.simpleupgrades.ConsumerEnergyHolder
import xyz.xenondevs.simpleupgrades.registry.UpgradeTypes
import java.net.URL
import kotlin.random.Random
import kotlin.random.nextInt

private val MAX_ENERGY = configReloadable { NovaConfig[MOB_DUPLICATOR].getLong("capacity") }
private val ENERGY_PER_TICK = configReloadable { NovaConfig[MOB_DUPLICATOR].getLong("energy_per_tick") }
private val ENERGY_PER_TICK_NBT = configReloadable { NovaConfig[MOB_DUPLICATOR].getLong("energy_per_tick_nbt") }
private val IDLE_TIME by configReloadable { NovaConfig[MOB_DUPLICATOR].getInt("idle_time") }
private val IDLE_TIME_NBT by configReloadable { NovaConfig[MOB_DUPLICATOR].getInt("idle_time_nbt") }
private val ENTITY_LIMIT by configReloadable { NovaConfig[MOB_DUPLICATOR].getInt("entity_limit") }
private val NERF_MOBS by configReloadable { NovaConfig[MOB_DUPLICATOR].getBoolean("nerf_mobs") }

class MobDuplicator(blockState: NovaTileEntityState) : NetworkedTileEntity(blockState), Upgradable {
    
    private val inventory = getInventory("inventory", 1, ::handleInventoryUpdate)
    override val gui = lazy { MobDuplicatorGui() }
    override val upgradeHolder = getUpgradeHolder(UpgradeTypes.SPEED, UpgradeTypes.EFFICIENCY, UpgradeTypes.ENERGY)
    override val energyHolder = ConsumerEnergyHolder(this, MAX_ENERGY, ENERGY_PER_TICK, ENERGY_PER_TICK_NBT, upgradeHolder) { createSideConfig(NetworkConnectionType.INSERT, BlockSide.TOP) }
    override val itemHolder = NovaItemHolder(this, inventory to NetworkConnectionType.BUFFER)
    private val energyPerTick: Long
        get() = if (keepNbt) energyHolder.specialEnergyConsumption else energyHolder.energyConsumption
    private val totalIdleTime: Int
        get() = if (keepNbt) idleTimeNBT else idleTime
    
    private var idleTimeNBT = 0
    private var idleTime = 0
    
    private val spawnLocation = location.clone().center().add(0.0, 1.0, 0.0)
    private var timePassed = 0
    private var entityType: EntityType? = null
    private var entityData: ByteArray? = null
    private var keepNbt = retrieveData("keepNbt") { false }
    
    init {
        reload()
        updateEntityData(inventory.getItemStack(0))
    }
    
    override fun reload() {
        super.reload()
        idleTimeNBT = (IDLE_TIME_NBT / upgradeHolder.getValue(UpgradeTypes.SPEED)).toInt()
        idleTime = (IDLE_TIME / upgradeHolder.getValue(UpgradeTypes.SPEED)).toInt()
        if (timePassed > totalIdleTime) timePassed = totalIdleTime
    }
    
    override fun saveData() {
        super.saveData()
        storeData("keepNbt", keepNbt)
    }
    
    override fun handleTick() {
        if (entityData != null && entityType != null && energyHolder.energy >= energyPerTick) {
            energyHolder.energy -= energyPerTick
            
            if (timePassed++ == totalIdleTime) {
                timePassed = 0
                
                spawnEntity()
            }
            
            if (gui.isInitialized()) gui.value.updateIdleBar()
        }
    }
    
    private fun handleInventoryUpdate(event: ItemUpdateEvent) {
        if (event.newItemStack != null) {
            event.isCancelled = !updateEntityData(event.newItemStack)
        } else setEntityData(null, null)
    }
    
    private fun updateEntityData(itemStack: ItemStack?): Boolean {
        val catcher = itemStack?.novaMaterial?.novaItem?.getBehavior(MobCatcherBehavior::class)
        if (catcher != null) {
            setEntityData(catcher.getEntityType(itemStack), catcher.getEntityData(itemStack))
            return true
        }
        return false
    }
    
    private fun setEntityData(type: EntityType?, data: ByteArray?) {
        entityData = data
        entityType = type
        timePassed = 0
        if (gui.isInitialized()) gui.value.updateIdleBar()
    }
    
    private fun spawnEntity() {
        if (ENTITY_LIMIT != -1 && countSurroundingEntities() > ENTITY_LIMIT) return
        
        val entity = if (keepNbt) EntityUtils.deserializeAndSpawn(entityData!!, spawnLocation, NBTUtils::removeItemData).bukkitEntity
        else spawnLocation.world!!.spawnEntity(spawnLocation, entityType!!)
        
        val nmsEntity = entity.nmsEntity
        if (NERF_MOBS && nmsEntity is Mob)
            nmsEntity.aware = false
        
        if (patreonSkulls.isNotEmpty() && entity is LivingEntity && Random.nextInt(1..1000) == 1) {
            entity.equipment?.setHelmet(patreonSkulls.random().get(), true)
        }
    }
    
    private fun countSurroundingEntities(): Int {
        return world.livingEntities.asSequence().filter {
            it.location.isBetweenXZ(
                location.clone().subtract(16.0, 0.0, 16.0),
                location.clone().add(16.0, 0.0, 16.0)
            )
        }.count()
    }
    
    inner class MobDuplicatorGui : TileEntityGui() {
        
        private val sideConfigGui = SideConfigGui(
            this@MobDuplicator,
            listOf(itemHolder.getNetworkedInventory(inventory) to "inventory.nova.default"),
            ::openWindow
        )
        
        private val idleBar = object : VerticalBar(3) {
            override val barMaterial = CoreGuiMaterial.BAR_GREEN
            override fun modifyItemBuilder(itemBuilder: ItemBuilder) =
                itemBuilder.setDisplayName(localized(ChatColor.GRAY, "menu.machines.mob_duplicator.idle", totalIdleTime - timePassed))
        }
        
        override val gui = Gui.normal()
            .setStructure(
                "1 - - - - - - - 2",
                "| s # # # # p e |",
                "| n # # i # p e |",
                "| u # # # # p e |",
                "3 - - - - - - - 4")
            .addIngredient('s', OpenSideConfigItem(sideConfigGui))
            .addIngredient('i', VISlotElement(inventory, 0, GuiMaterials.MOB_CATCHER_PLACEHOLDER.clientsideProvider))
            .addIngredient('n', ToggleNBTModeItem())
            .addIngredient('u', OpenUpgradesItem(upgradeHolder))
            .addIngredient('e', EnergyBar(3, energyHolder))
            .addIngredient('p', idleBar)
            .build()
        
        fun updateIdleBar() {
            idleBar.percentage = timePassed.toDouble() / totalIdleTime.toDouble()
        }
        
        private inner class ToggleNBTModeItem : BaseItem() {
            
            override fun getItemProvider(): ItemProvider {
                return (if (keepNbt) GuiMaterials.NBT_BTN_ON else GuiMaterials.NBT_BTN_OFF).clientsideProvider
            }
            
            override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
                keepNbt = !keepNbt
                notifyWindows()
                
                timePassed = 0
                updateIdleBar()
                
                player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 1f)
            }
            
        }
        
    }
    
    private companion object PatronSkulls {
        
        private const val PATRON_SKULLS_URL = "https://xenondevs.xyz/nova/patron_skulls.json"
        val patreonSkulls = ArrayList<SkullBuilder>()
        
        init {
            runAsyncTask {
                val url = URL(PATRON_SKULLS_URL)
                val array = url.openConnection().getInputStream().bufferedReader().use(JsonParser::parseReader)
                if (array is JsonArray) {
                    array.asSequence()
                        .filter(JsonElement::isString)
                        .forEach {
                            patreonSkulls += SkullBuilder(HeadTexture(it.asString))
                        }
                }
            }
        }
        
    }
    
}
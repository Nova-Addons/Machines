package xyz.xenondevs.nova.machines.tileentity.world

import de.studiocode.invui.gui.GUI
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.guitype.GUIType
import de.studiocode.invui.item.builder.ItemBuilder
import de.studiocode.invui.virtualinventory.event.ItemUpdateEvent
import net.md_5.bungee.api.ChatColor
import net.minecraft.world.entity.EquipmentSlot
import org.bukkit.Bukkit
import org.bukkit.util.Vector
import xyz.xenondevs.nova.data.config.GlobalValues
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.data.config.configReloadable
import xyz.xenondevs.nova.data.resources.model.data.ArmorStandBlockModelData
import xyz.xenondevs.nova.data.world.block.state.NovaTileEntityState
import xyz.xenondevs.nova.machines.registry.Blocks.STAR_COLLECTOR
import xyz.xenondevs.nova.machines.registry.Items
import xyz.xenondevs.nova.material.CoreGUIMaterial
import xyz.xenondevs.nova.tileentity.NetworkedTileEntity
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
import xyz.xenondevs.nova.util.Vector
import xyz.xenondevs.nova.util.calculateYaw
import xyz.xenondevs.nova.util.center
import xyz.xenondevs.nova.util.data.localized
import xyz.xenondevs.nova.util.dropItem
import xyz.xenondevs.nova.util.isFull
import xyz.xenondevs.nova.util.particle
import xyz.xenondevs.nova.util.particleBuilder
import xyz.xenondevs.nova.world.fakeentity.impl.FakeArmorStand
import xyz.xenondevs.particle.ParticleEffect
import java.awt.Color

private val MAX_ENERGY = configReloadable { NovaConfig[STAR_COLLECTOR].getLong("capacity") }
private val IDLE_ENERGY_PER_TICK = configReloadable { NovaConfig[STAR_COLLECTOR].getLong("energy_per_tick_idle") }
private val COLLECTING_ENERGY_PER_TICK = configReloadable { NovaConfig[STAR_COLLECTOR].getLong("energy_per_tick_collecting") }
private val IDLE_TIME by configReloadable { NovaConfig[STAR_COLLECTOR].getInt("idle_time") }
private val COLLECTION_TIME by configReloadable { NovaConfig[STAR_COLLECTOR].getInt("collection_time") }

private const val STAR_PARTICLE_DISTANCE_PER_TICK = 0.75

class StarCollector(blockState: NovaTileEntityState) : NetworkedTileEntity(blockState), Upgradable {
    
    private val inventory = getInventory("inventory", 1, ::handleInventoryUpdate)
    override val gui: Lazy<StarCollectorGUI> = lazy(::StarCollectorGUI)
    override val upgradeHolder = getUpgradeHolder(UpgradeType.SPEED, UpgradeType.EFFICIENCY, UpgradeType.ENERGY)
    override val itemHolder = NovaItemHolder(this, inventory to NetworkConnectionType.EXTRACT) {
        createExclusiveSideConfig(NetworkConnectionType.EXTRACT, BlockSide.BOTTOM)
    }
    override val energyHolder = ConsumerEnergyHolder(this, MAX_ENERGY, IDLE_ENERGY_PER_TICK, COLLECTING_ENERGY_PER_TICK, upgradeHolder) {
        createExclusiveSideConfig(NetworkConnectionType.INSERT, BlockSide.BOTTOM)
    }
    
    private var maxIdleTime = 0
    private var maxCollectionTime = 0
    private var timeSpentIdle = 0
    private var timeSpentCollecting = -1
    private lateinit var particleVector: Vector
    
    private val rodLocation = location.clone().center().apply { y += 0.7 }
    private val rod = FakeArmorStand(location.clone().center().apply { y -= 1 }, true) { ast, data ->
        data.isMarker = true
        data.isInvisible = true
        ast.setEquipment(EquipmentSlot.HEAD, (material.block as ArmorStandBlockModelData)[1].get(), false)
    }
    
    private val particleTask = createParticleTask(listOf(
        particle(ParticleEffect.DUST_COLOR_TRANSITION) {
            location(location.clone().center().apply { y += 0.2 })
            dustFade(Color(132, 0, 245), Color(196, 128, 217), 1f)
            offset(0.25, 0.1, 0.25)
            amount(3)
        }
    ), 1)
    
    init {
        reload()
    }
    
    override fun reload() {
        super.reload()
        
        maxIdleTime = (IDLE_TIME / upgradeHolder.getValue(UpgradeType.SPEED)).toInt()
        maxCollectionTime = (COLLECTION_TIME / upgradeHolder.getValue(UpgradeType.SPEED)).toInt()
    }
    
    override fun handleTick() {
        if (world.time in 13_000..23_000 || timeSpentCollecting != -1) handleNightTick()
        else handleDayTick()
    }
    
    private fun handleNightTick() {
        if (timeSpentCollecting != -1) {
            if (!GlobalValues.DROP_EXCESS_ON_GROUND && inventory.isFull()) return
            if (energyHolder.energy >= energyHolder.specialEnergyConsumption) {
                energyHolder.energy -= energyHolder.specialEnergyConsumption
                handleCollectionTick()
            }
        } else if (energyHolder.energy >= energyHolder.energyConsumption) {
            energyHolder.energy -= energyHolder.energyConsumption
            handleIdleTick()
        }
    }
    
    private fun handleCollectionTick() {
        timeSpentCollecting++
        if (timeSpentCollecting >= maxCollectionTime) {
            timeSpentIdle = 0
            timeSpentCollecting = -1
            
            val item = Items.STAR_DUST.createItemStack()
            val leftOver = inventory.addItem(SELF_UPDATE_REASON, item)
            if (GlobalValues.DROP_EXCESS_ON_GROUND && leftOver != 0) location.dropItem(item)
            
            particleTask.stop()
            rod.setEquipment(EquipmentSlot.HEAD, (material.block as ArmorStandBlockModelData)[1].get(), true)
        } else {
            val percentageCollected = (maxCollectionTime - timeSpentCollecting) / maxCollectionTime.toDouble()
            val particleDistance = percentageCollected * (STAR_PARTICLE_DISTANCE_PER_TICK * maxCollectionTime)
            val particleLocation = rodLocation.clone().add(particleVector.clone().multiply(particleDistance))
            
            particleBuilder(ParticleEffect.REDSTONE) {
                location(particleLocation)
                color(Color(255, 255, 255))
            }.display(getViewers())
        }
        
        if (gui.isInitialized())
            gui.value.collectionBar.percentage = timeSpentCollecting / maxCollectionTime.toDouble()
    }
    
    private fun handleIdleTick() {
        timeSpentIdle++
        if (timeSpentIdle >= maxIdleTime) {
            timeSpentCollecting = 0
            
            particleTask.start()
            
            rod.setEquipment(EquipmentSlot.HEAD, (material.block as ArmorStandBlockModelData)[2].get(), true)
            
            rodLocation.yaw = rod.location.yaw
            particleVector = Vector(rod.location.yaw, -65F)
        } else rod.teleport { this.yaw += 2F }
        
        if (gui.isInitialized())
            gui.value.idleBar.percentage = timeSpentIdle / maxIdleTime.toDouble()
    }
    
    private fun handleDayTick() {
        val player = Bukkit.getOnlinePlayers()
            .asSequence()
            .filter { it.location.world == world }
            .minByOrNull { it.location.distanceSquared(rodLocation) }
        
        if (player != null) {
            val distance = rodLocation.distance(player.location)
            
            if (distance <= 5) {
                val vector = player.location.subtract(rodLocation).toVector()
                val yaw = vector.calculateYaw()
                
                rod.teleport { this.yaw = yaw }
            }
        }
    }
    
    private fun handleInventoryUpdate(event: ItemUpdateEvent) {
        event.isCancelled = event.updateReason != SELF_UPDATE_REASON && !event.isRemove
    }
    
    override fun handleRemoved(unload: Boolean) {
        super.handleRemoved(unload)
        rod.remove()
    }
    
    inner class StarCollectorGUI : TileEntityGUI() {
        
        private val sideConfigGUI = SideConfigGUI(
            this@StarCollector,
            listOf(itemHolder.getNetworkedInventory(inventory) to "inventory.nova.output"),
            ::openWindow
        )
        
        val collectionBar = object : VerticalBar(3) {
            override val barMaterial = CoreGUIMaterial.BAR_GREEN
            override fun modifyItemBuilder(itemBuilder: ItemBuilder): ItemBuilder {
                if (timeSpentCollecting != -1)
                    itemBuilder.setDisplayName(localized(ChatColor.GRAY, "menu.machines.star_collector.collection"))
                return itemBuilder
            }
        }
        
        val idleBar = object : VerticalBar(3) {
            override val barMaterial = CoreGUIMaterial.BAR_GREEN
            override fun modifyItemBuilder(itemBuilder: ItemBuilder) =
                itemBuilder.setDisplayName(localized(ChatColor.GRAY, "menu.machines.star_collector.idle"))
        }
        
        override val gui: GUI = GUIBuilder(GUIType.NORMAL)
            .setStructure(
                "1 - - - - - - - 2",
                "| s # # # c p e |",
                "| u # i # c p e |",
                "| # # # # c p e |",
                "3 - - - - - - - 4")
            .addIngredient('s', OpenSideConfigItem(sideConfigGUI))
            .addIngredient('u', OpenUpgradesItem(upgradeHolder))
            .addIngredient('i', inventory)
            .addIngredient('c', collectionBar)
            .addIngredient('p', idleBar)
            .addIngredient('e', EnergyBar(3, energyHolder))
            .build()
        
    }
    
}
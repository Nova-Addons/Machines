package xyz.xenondevs.nova.machines.tileentity.agriculture

import net.minecraft.core.particles.ParticleTypes
import net.minecraft.world.item.ItemDisplayContext
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.joml.Vector3f
import xyz.xenondevs.invui.gui.Gui
import xyz.xenondevs.invui.gui.SlotElement.VISlotElement
import xyz.xenondevs.invui.virtualinventory.event.ItemUpdateEvent
import xyz.xenondevs.nmsutils.particle.color
import xyz.xenondevs.nmsutils.particle.particle
import xyz.xenondevs.nova.data.config.GlobalValues
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.data.config.configReloadable
import xyz.xenondevs.nova.data.world.block.state.NovaTileEntityState
import xyz.xenondevs.nova.machines.registry.Blocks
import xyz.xenondevs.nova.machines.registry.Blocks.TREE_FACTORY
import xyz.xenondevs.nova.machines.registry.GuiMaterials
import xyz.xenondevs.nova.material.ItemNovaMaterial
import xyz.xenondevs.nova.tileentity.NetworkedTileEntity
import xyz.xenondevs.nova.tileentity.menu.TileEntityMenuClass
import xyz.xenondevs.nova.tileentity.network.NetworkConnectionType
import xyz.xenondevs.nova.tileentity.network.item.holder.NovaItemHolder
import xyz.xenondevs.nova.tileentity.upgrade.Upgradable
import xyz.xenondevs.nova.ui.EnergyBar
import xyz.xenondevs.nova.ui.OpenUpgradesItem
import xyz.xenondevs.nova.ui.config.side.OpenSideConfigItem
import xyz.xenondevs.nova.ui.config.side.SideConfigMenu
import xyz.xenondevs.nova.util.BlockSide
import xyz.xenondevs.nova.util.center
import xyz.xenondevs.nova.util.dropItem
import xyz.xenondevs.nova.util.nmsCopy
import xyz.xenondevs.nova.util.sendTo
import xyz.xenondevs.nova.world.fakeentity.impl.FakeItemDisplay
import xyz.xenondevs.simpleupgrades.ConsumerEnergyHolder
import xyz.xenondevs.simpleupgrades.registry.UpgradeTypes
import java.awt.Color

private class PlantConfiguration(val miniature: ItemNovaMaterial, val loot: ItemStack, val color: Color)

private val PLANTS = mapOf(
    Material.OAK_SAPLING to PlantConfiguration(Blocks.OAK_TREE_MINIATURE, ItemStack(Material.OAK_LOG), Color(43, 82, 39)),
    Material.SPRUCE_SAPLING to PlantConfiguration(Blocks.SPRUCE_TREE_MINIATURE, ItemStack(Material.SPRUCE_LOG), Color(43, 87, 60)),
    Material.BIRCH_SAPLING to PlantConfiguration(Blocks.BIRCH_TREE_MINIATURE, ItemStack(Material.BIRCH_LOG), Color(49, 63, 35)),
    Material.JUNGLE_SAPLING to PlantConfiguration(Blocks.JUNGLE_TREE_MINIATURE, ItemStack(Material.JUNGLE_LOG), Color(51, 127, 43)),
    Material.ACACIA_SAPLING to PlantConfiguration(Blocks.ACACIA_TREE_MINIATURE, ItemStack(Material.ACACIA_LOG), Color(113, 125, 75)),
    Material.DARK_OAK_SAPLING to PlantConfiguration(Blocks.DARK_OAK_TREE_MINIATURE, ItemStack(Material.DARK_OAK_LOG), Color(26, 65, 17)),
    Material.MANGROVE_PROPAGULE to PlantConfiguration(Blocks.MANGROVE_TREE_MINIATURE, ItemStack(Material.MANGROVE_LOG), Color(32, 47, 14)),
    Material.CRIMSON_FUNGUS to PlantConfiguration(Blocks.CRIMSON_TREE_MINIATURE, ItemStack(Material.CRIMSON_STEM), Color(121, 0, 0)),
    Material.WARPED_FUNGUS to PlantConfiguration(Blocks.WARPED_TREE_MINIATURE, ItemStack(Material.WARPED_STEM), Color(22, 124, 132)),
    Material.RED_MUSHROOM to PlantConfiguration(Blocks.GIANT_RED_MUSHROOM_MINIATURE, ItemStack(Material.RED_MUSHROOM, 3), Color(192, 39, 37)),
    Material.BROWN_MUSHROOM to PlantConfiguration(Blocks.GIANT_BROWN_MUSHROOM_MINIATURE, ItemStack(Material.BROWN_MUSHROOM, 3), Color(149, 112, 80))
)

private val MAX_ENERGY = configReloadable { NovaConfig[TREE_FACTORY].getLong("capacity") }
private val ENERGY_PER_TICK = configReloadable { NovaConfig[TREE_FACTORY].getLong("energy_per_tick") }
private val PROGRESS_PER_TICK by configReloadable { NovaConfig[TREE_FACTORY].getDouble("progress_per_tick") }
private val IDLE_TIME by configReloadable { NovaConfig[TREE_FACTORY].getInt("idle_time") }

class TreeFactory(blockState: NovaTileEntityState) : NetworkedTileEntity(blockState), Upgradable {
    
    private val inputInventory = getInventory("input", 1, intArrayOf(1), false, ::handleInputInventoryUpdate)
    private val outputInventory = getInventory("output", 9, ::handleOutputInventoryUpdate)
    
    override val upgradeHolder = getUpgradeHolder(UpgradeTypes.SPEED, UpgradeTypes.EFFICIENCY, UpgradeTypes.ENERGY)
    override val energyHolder = ConsumerEnergyHolder(this, MAX_ENERGY, ENERGY_PER_TICK, null, upgradeHolder) {
        createExclusiveSideConfig(NetworkConnectionType.INSERT, BlockSide.BOTTOM, BlockSide.BACK)
    }
    override val itemHolder = NovaItemHolder(
        this,
        outputInventory to NetworkConnectionType.EXTRACT,
        inputInventory to NetworkConnectionType.INSERT,
    ) { createExclusiveSideConfig(NetworkConnectionType.BUFFER, BlockSide.BOTTOM, BlockSide.BACK) }
    
    private var plantType = inputInventory.getItemStack(0)?.type
    private val plant: FakeItemDisplay
    
    private var progressPerTick = 0.0
    private var maxIdleTime = 0
    
    private var growthProgress = 0.0
    private var idleTimeLeft = 0
    
    init {
        val plantLocation = location.clone().add(.5, 1.0 / 16.0, .5)
        plant = FakeItemDisplay(plantLocation, true) { _, data -> data.itemDisplay = ItemDisplayContext.HEAD }
        reload()
    }
    
    override fun reload() {
        super.reload()
        progressPerTick = PROGRESS_PER_TICK * upgradeHolder.getValue(UpgradeTypes.SPEED)
        maxIdleTime = (IDLE_TIME / upgradeHolder.getValue(UpgradeTypes.SPEED)).toInt()
    }
    
    override fun handleTick() {
        if (energyHolder.energy >= energyHolder.energyConsumption && plantType != null) {
            val plantLoot = PLANTS[plantType]!!.loot
            if (!GlobalValues.DROP_EXCESS_ON_GROUND && !outputInventory.canHold(plantLoot)) return
            
            energyHolder.energy -= energyHolder.energyConsumption
            
            if (idleTimeLeft == 0) {
                if (plantType != null) {
                    growthProgress += progressPerTick
                    if (growthProgress >= 1.0)
                        idleTimeLeft = maxIdleTime
                    
                    updatePlantArmorStand()
                }
            } else {
                idleTimeLeft--
                
                particle(ParticleTypes.DUST) {
                    color(PLANTS[plantType]!!.color)
                    location(location.clone().center().apply { y += 0.5 })
                    offset(0.15, 0.15, 0.15)
                    speed(0.1f)
                    amount(5)
                }.sendTo(getViewers())
                
                if (idleTimeLeft == 0) {
                    growthProgress = 0.0
                    
                    val leftover = outputInventory.addItem(SELF_UPDATE_REASON, plantLoot)
                    if (GlobalValues.DROP_EXCESS_ON_GROUND && leftover > 0)
                        centerLocation.dropItem(plantLoot.clone().apply { amount = leftover })
                }
            }
        }
    }
    
    private fun updatePlantArmorStand() {
        val size = growthProgress.coerceIn(0.0..1.0).toFloat()
        plant.updateEntityData(true) {
            itemStack = plantType?.let { PLANTS[it]!!.miniature.clientsideProvider.get() }.nmsCopy
            scale = Vector3f(size, size, size)
            translation = Vector3f(0.0f, 0.5f * size, 0.0f)
        }
    }
    
    private fun handleInputInventoryUpdate(event: ItemUpdateEvent) {
        if (event.newItemStack != null && event.newItemStack.type !in PLANTS.keys) {
            event.isCancelled = true
        } else {
            plantType = event.newItemStack?.type
            growthProgress = 0.0
            updatePlantArmorStand()
        }
    }
    
    private fun handleOutputInventoryUpdate(event: ItemUpdateEvent) {
        event.isCancelled = event.updateReason != SELF_UPDATE_REASON && !event.isRemove
    }
    
    override fun handleRemoved(unload: Boolean) {
        super.handleRemoved(unload)
        plant.remove()
    }
    
    @TileEntityMenuClass
    private inner class TreeFactoryMenu : GlobalTileEntityMenu() {
        
        private val sideConfigGui = SideConfigMenu(
            this@TreeFactory,
            listOf(
                itemHolder.getNetworkedInventory(inputInventory) to "inventory.nova.input",
                itemHolder.getNetworkedInventory(outputInventory) to "inventory.nova.output"
            ),
            ::openWindow
        )
        
        override val gui = Gui.normal()
            .setStructure(
                "1 - - - - - - - 2",
                "| s u # # # # e |",
                "| # # # o o o e |",
                "| # i # o o o e |",
                "| # # # o o o e |",
                "3 - - - - - - - 4")
            .addIngredient('i', VISlotElement(inputInventory, 0, GuiMaterials.SAPLING_PLACEHOLDER.clientsideProvider))
            .addIngredient('o', outputInventory)
            .addIngredient('s', OpenSideConfigItem(sideConfigGui))
            .addIngredient('u', OpenUpgradesItem(upgradeHolder))
            .addIngredient('e', EnergyBar(4, energyHolder))
            .build()
        
    }
    
}
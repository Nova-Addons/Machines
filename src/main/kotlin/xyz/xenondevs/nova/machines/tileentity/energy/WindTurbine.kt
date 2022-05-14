package xyz.xenondevs.nova.machines.tileentity.energy

import de.studiocode.invui.gui.GUI
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.guitype.GUIType
import net.minecraft.core.Rotations
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.data.config.Reloadable
import xyz.xenondevs.nova.data.config.configReloadable
import xyz.xenondevs.nova.data.world.block.state.NovaTileEntityState
import xyz.xenondevs.nova.integration.protection.ProtectionManager
import xyz.xenondevs.nova.machines.registry.Blocks.WIND_TURBINE
import xyz.xenondevs.nova.tileentity.Model
import xyz.xenondevs.nova.tileentity.NetworkedTileEntity
import xyz.xenondevs.nova.tileentity.TileEntity
import xyz.xenondevs.nova.tileentity.network.NetworkConnectionType
import xyz.xenondevs.nova.tileentity.network.energy.holder.ProviderEnergyHolder
import xyz.xenondevs.nova.tileentity.upgrade.Upgradable
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeHolder
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeType
import xyz.xenondevs.nova.ui.EnergyBar
import xyz.xenondevs.nova.ui.OpenUpgradesItem
import xyz.xenondevs.nova.util.BlockSide
import xyz.xenondevs.nova.util.add
import xyz.xenondevs.nova.util.concurrent.CombinedBooleanFuture
import xyz.xenondevs.nova.util.item.isReplaceable
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.pos
import java.util.concurrent.CompletableFuture
import kotlin.math.abs

private val MAX_ENERGY by configReloadable { NovaConfig[WIND_TURBINE].getLong("capacity") }
private val ENERGY_PER_TICK by configReloadable { NovaConfig[WIND_TURBINE].getLong("energy_per_tick") }
private val PLAY_ANIMATION by configReloadable { NovaConfig[WIND_TURBINE].getBoolean("animation") }

class WindTurbine(blockState: NovaTileEntityState) : NetworkedTileEntity(blockState), Upgradable, Reloadable {
    
    override val gui = lazy { WindTurbineGUI() }
    override val upgradeHolder = UpgradeHolder(this, gui, ::updateEnergyPerTick, UpgradeType.EFFICIENCY, UpgradeType.ENERGY)
    override val energyHolder = ProviderEnergyHolder(this, MAX_ENERGY, ENERGY_PER_TICK, upgradeHolder) {
        createExclusiveSideConfig(NetworkConnectionType.EXTRACT, BlockSide.FRONT, BlockSide.BOTTOM)
    }
    
    private val turbineModel = createMultiModel()
    
    private val altitude = (location.y + abs(world.minHeight)) / (world.maxHeight - 1 + abs(world.minHeight))
    private val rotationPerTick = altitude.toFloat() * 15
    private var energyPerTick = 0
    
    init {
        NovaConfig.reloadables.add(this)
        updateEnergyPerTick()
        spawnModels()
    }
    
    override fun reload() {
        energyHolder.defaultMaxEnergy = MAX_ENERGY
        energyHolder.defaultEnergyGeneration = ENERGY_PER_TICK
        
        updateEnergyPerTick()
    }
    
    private fun updateEnergyPerTick() {
        energyPerTick = (altitude * energyHolder.energyGeneration).toInt()
    }
    
    private fun spawnModels() {
        val location = centerLocation.add(0.0, 2.0, 0.0)
        location.yaw += 180
        location.y += 1.0 / 32.0
        
        turbineModel.addModels(Model(
            WIND_TURBINE.block.createClientsideItemStack(4),
            location,
            Rotations(90f, 0f, 0f)
        ))
        
        for (blade in 0..2) {
            turbineModel.addModels(Model(
                material.block.createClientsideItemStack(5),
                location,
                Rotations(90f, 0f, blade * 120f)
            ))
        }
    }
    
    override fun handleTick() {
        energyHolder.energy += energyPerTick
    }
    
    override fun handleAsyncTick() {
        if (PLAY_ANIMATION) {
            turbineModel.useArmorStands {
                it.updateEntityData(true) {
                    headRotation = headRotation!!.add(0f, 0f, rotationPerTick)
                }
            }
        }
    }
    
    companion object {
        
        fun canPlace(player: Player, item: ItemStack, location: Location): CompletableFuture<Boolean> {
            return CombinedBooleanFuture(loadMultiBlock(location.pos).map {
                if (!it.block.type.isReplaceable())
                    return CompletableFuture.completedFuture(false)
                
                ProtectionManager.canPlace(player, item, it.location)
            })
        }
        
        fun loadMultiBlock(pos: BlockPos): List<BlockPos> =
            listOf(
                pos.copy(y = pos.y + 1),
                pos.copy(y = pos.y + 2),
                pos.copy(y = pos.y + 3)
            )
        
    }
    
    inner class WindTurbineGUI : TileEntity.TileEntityGUI() {
        
        override val gui: GUI = GUIBuilder(GUIType.NORMAL)
            .setStructure(
                "1 - - - - - - - 2",
                "| u # # e # # # |",
                "| # # # e # # # |",
                "| # # # e # # # |",
                "3 - - - - - - - 4")
            .addIngredient('u', OpenUpgradesItem(upgradeHolder))
            .addIngredient('e', EnergyBar(3, energyHolder))
            .build()
        
    }
    
}
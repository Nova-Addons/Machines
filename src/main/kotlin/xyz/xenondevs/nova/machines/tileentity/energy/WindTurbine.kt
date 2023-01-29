package xyz.xenondevs.nova.machines.tileentity.energy

import net.minecraft.core.Rotations
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.invui.gui.builder.GuiType
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.data.config.configReloadable
import xyz.xenondevs.nova.data.resources.model.data.ArmorStandBlockModelData
import xyz.xenondevs.nova.data.world.block.state.NovaTileEntityState
import xyz.xenondevs.nova.integration.protection.ProtectionManager
import xyz.xenondevs.nova.machines.registry.Blocks.WIND_TURBINE
import xyz.xenondevs.nova.tileentity.Model
import xyz.xenondevs.nova.tileentity.NetworkedTileEntity
import xyz.xenondevs.nova.tileentity.TileEntity
import xyz.xenondevs.nova.tileentity.network.NetworkConnectionType
import xyz.xenondevs.nova.tileentity.upgrade.Upgradable
import xyz.xenondevs.nova.ui.EnergyBar
import xyz.xenondevs.nova.ui.OpenUpgradesItem
import xyz.xenondevs.nova.util.BlockSide
import xyz.xenondevs.nova.util.add
import xyz.xenondevs.nova.util.concurrent.CombinedBooleanFuture
import xyz.xenondevs.nova.util.item.isReplaceable
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.pos
import xyz.xenondevs.simpleupgrades.ProviderEnergyHolder
import xyz.xenondevs.simpleupgrades.registry.UpgradeTypes
import java.util.concurrent.CompletableFuture
import kotlin.math.abs

private val MAX_ENERGY = configReloadable { NovaConfig[WIND_TURBINE].getLong("capacity") }
private val ENERGY_PER_TICK = configReloadable { NovaConfig[WIND_TURBINE].getLong("energy_per_tick") }
private val PLAY_ANIMATION by configReloadable { NovaConfig[WIND_TURBINE].getBoolean("animation") }

class WindTurbine(blockState: NovaTileEntityState) : NetworkedTileEntity(blockState), Upgradable {
    
    override val gui = lazy { WindTurbineGui() }
    override val upgradeHolder = getUpgradeHolder(UpgradeTypes.EFFICIENCY, UpgradeTypes.ENERGY)
    override val energyHolder = ProviderEnergyHolder(this, MAX_ENERGY, ENERGY_PER_TICK, upgradeHolder, UpgradeTypes.EFFICIENCY) {
        createExclusiveSideConfig(NetworkConnectionType.EXTRACT, BlockSide.FRONT, BlockSide.BOTTOM)
    }
    
    private val turbineModel = createMultiModel()
    
    private val altitude = (location.y + abs(world.minHeight)) / (world.maxHeight - 1 + abs(world.minHeight))
    private val rotationPerTick = altitude.toFloat() * 15
    private var energyPerTick = 0
    
    init {
        reload()
        spawnModels()
    }
    
    override fun reload() {
        super.reload()
        energyPerTick = (altitude * energyHolder.energyGeneration).toInt()
    }
    
    private fun spawnModels() {
        val location = centerLocation.add(0.0, 2.0, 0.0)
        location.yaw += 180
        location.y += 1.0 / 32.0
        
        turbineModel.addModels(Model(
            (material.block as ArmorStandBlockModelData)[4].get(),
            location,
            Rotations(90f, 0f, 0f)
        ))
        
        for (blade in 0..2) {
            turbineModel.addModels(Model(
                (material.block as ArmorStandBlockModelData)[5].get(),
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
                    headRotation = headRotation.add(0f, 0f, rotationPerTick)
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
    
    inner class WindTurbineGui : TileEntity.TileEntityGui() {
        
        override val gui = GuiType.NORMAL.builder()
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
package xyz.xenondevs.nova.machines.tileentity.energy

import de.studiocode.invui.gui.GUI
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.guitype.GUIType
import net.minecraft.core.particles.ParticleTypes
import xyz.xenondevs.nmsutils.particle.particle
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.data.config.configReloadable
import xyz.xenondevs.nova.data.world.block.state.NovaTileEntityState
import xyz.xenondevs.nova.machines.registry.Blocks.LAVA_GENERATOR
import xyz.xenondevs.nova.tileentity.NetworkedTileEntity
import xyz.xenondevs.nova.tileentity.network.NetworkConnectionType
import xyz.xenondevs.nova.tileentity.network.energy.holder.ProviderEnergyHolder
import xyz.xenondevs.nova.tileentity.network.fluid.FluidType
import xyz.xenondevs.nova.tileentity.network.fluid.holder.NovaFluidHolder
import xyz.xenondevs.nova.tileentity.upgrade.Upgradable
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeType
import xyz.xenondevs.nova.ui.EnergyBar
import xyz.xenondevs.nova.ui.FluidBar
import xyz.xenondevs.nova.ui.OpenUpgradesItem
import xyz.xenondevs.nova.ui.config.side.OpenSideConfigItem
import xyz.xenondevs.nova.ui.config.side.SideConfigGUI
import xyz.xenondevs.nova.util.BlockSide
import xyz.xenondevs.nova.util.advance
import xyz.xenondevs.nova.util.axis
import xyz.xenondevs.nova.util.intValue

private val ENERGY_CAPACITY = configReloadable { NovaConfig[LAVA_GENERATOR].getLong("energy_capacity") }
private val FLUID_CAPACITY = configReloadable { NovaConfig[LAVA_GENERATOR].getLong("fluid_capacity") }
private val ENERGY_PER_MB by configReloadable { NovaConfig[LAVA_GENERATOR].getDouble("energy_per_mb") }
private val BURN_RATE by configReloadable { NovaConfig[LAVA_GENERATOR].getDouble("burn_rate") }

class LavaGenerator(blockState: NovaTileEntityState) : NetworkedTileEntity(blockState), Upgradable {
    
    override val gui = lazy(::LavaGeneratorGUI)
    
    override val upgradeHolder = getUpgradeHolder(UpgradeType.SPEED, UpgradeType.EFFICIENCY, UpgradeType.ENERGY, UpgradeType.FLUID)
    private val fluidContainer = getFluidContainer("tank", hashSetOf(FluidType.LAVA), FLUID_CAPACITY, upgradeHolder = upgradeHolder)
    override val fluidHolder = NovaFluidHolder(this, fluidContainer to NetworkConnectionType.BUFFER) { createSideConfig(NetworkConnectionType.INSERT, BlockSide.FRONT) }
    override val energyHolder = ProviderEnergyHolder(this, ENERGY_CAPACITY, null, upgradeHolder) { createSideConfig(NetworkConnectionType.EXTRACT, BlockSide.FRONT) }
    
    private var on = false
    private var burnRate = 0.0
    private var burnProgress = 0.0
    private var energyPerTick = 0L
    
    private val smokeParticleTask = createPacketTask(listOf(
        particle(ParticleTypes.SMOKE) {
            location(centerLocation.advance(getFace(BlockSide.FRONT), 0.6).apply { y += 0.6 })
            offset(getFace(BlockSide.RIGHT).axis, 0.15f)
            offsetY(0.1f)
            speed(0f)
            amount(1)
        }
    ), 3)
    
    private val lavaParticleTask = createPacketTask(listOf(
        particle(ParticleTypes.LAVA) {
            location(centerLocation.advance(getFace(BlockSide.FRONT), 0.6).apply { y += 0.6 })
            offset(getFace(BlockSide.RIGHT).axis, 0.15f)
            offsetY(0.1f)
        }
    ), 200)
    
    init {
        reload()
    }
    
    override fun reload() {
        super.reload()
        burnRate = BURN_RATE * upgradeHolder.getValue(UpgradeType.SPEED) / upgradeHolder.getValue(UpgradeType.EFFICIENCY)
        energyPerTick = (ENERGY_PER_MB * BURN_RATE * upgradeHolder.getValue(UpgradeType.SPEED)).toLong()
    }
    
    private fun updateModelState() {
        blockState.modelProvider.update(on.intValue)
    }
    
    override fun handleTick() {
        if (energyHolder.energy == energyHolder.maxEnergy || fluidContainer.isEmpty()) {
            if (on) {
                on = false
                updateModelState()
                smokeParticleTask.stop()
                lavaParticleTask.stop()
            }
            
            return
        } else if (!on) {
            on = true
            updateModelState()
            smokeParticleTask.start()
            lavaParticleTask.start()
        }
        
        val lavaAmount = fluidContainer.amount
        if (lavaAmount >= burnRate) {
            energyHolder.energy += energyPerTick
            
            burnProgress += burnRate
            if (burnProgress > 1) {
                val burnt = burnProgress.toLong()
                
                burnProgress -= burnt
                fluidContainer.takeFluid(burnt)
            }
        } else {
            energyHolder.energy += (lavaAmount * ENERGY_PER_MB).toLong()
            fluidContainer.clear()
        }
    }
    
    inner class LavaGeneratorGUI : TileEntityGUI() {
        
        private val sideConfigGUI = SideConfigGUI(
            this@LavaGenerator,
            fluidContainerNames = listOf(fluidContainer to "container.nova.lava_tank"),
            openPrevious = ::openWindow
        )
        
        override val gui: GUI = GUIBuilder(GUIType.NORMAL)
            .setStructure(
                "1 - - - - - - - 2",
                "| s # # # # f e |",
                "| u # # # # f e |",
                "| # # # # # f e |",
                "3 - - - - - - - 4")
            .addIngredient('s', OpenSideConfigItem(sideConfigGUI))
            .addIngredient('u', OpenUpgradesItem(upgradeHolder))
            .addIngredient('e', EnergyBar(3, energyHolder))
            .addIngredient('f', FluidBar(3, fluidHolder, fluidContainer))
            .build()
        
    }
    
}
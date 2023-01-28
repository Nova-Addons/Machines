package xyz.xenondevs.nova.machines.tileentity.world

import xyz.xenondevs.invui.gui.builder.GuiBuilder
import xyz.xenondevs.invui.gui.builder.guitype.GuiType
import xyz.xenondevs.nova.data.world.block.state.NovaTileEntityState
import xyz.xenondevs.nova.tileentity.NetworkedTileEntity
import xyz.xenondevs.nova.tileentity.network.NetworkConnectionType
import xyz.xenondevs.nova.tileentity.network.fluid.FluidType
import xyz.xenondevs.nova.tileentity.network.fluid.container.FluidContainer
import xyz.xenondevs.nova.tileentity.network.fluid.holder.NovaFluidHolder
import xyz.xenondevs.nova.ui.FluidBar
import xyz.xenondevs.nova.ui.config.side.OpenSideConfigItem
import xyz.xenondevs.nova.ui.config.side.SideConfigGui
import java.util.*

class InfiniteWaterSource(blockState: NovaTileEntityState) : NetworkedTileEntity(blockState) {
    
    override val gui = lazy(::InfiniteWaterSourceGui)
    
    private val fluidContainer = InfiniteFluidContainer
    override val fluidHolder = NovaFluidHolder(this, fluidContainer to NetworkConnectionType.EXTRACT) { createSideConfig(NetworkConnectionType.EXTRACT) }
    
    inner class InfiniteWaterSourceGui : TileEntityGui() {
        
        private val sideConfigGui = SideConfigGui(
            this@InfiniteWaterSource,
            fluidContainerNames = listOf(fluidContainer to "block.minecraft.water"),
            openPrevious = ::openWindow
        )
        
        override val gui = GuiBuilder(GuiType.NORMAL)
            .setStructure(
                "1 - - - - - - - 2",
                "| s # # f # # # |",
                "| # # # f # # # |",
                "| # # # f # # # |",
                "3 - - - - - - - 4")
            .addIngredient('s', OpenSideConfigItem(sideConfigGui))
            .addIngredient('f', FluidBar(3, fluidHolder, fluidContainer))
            .build()
        
    }
    
}

object InfiniteFluidContainer : FluidContainer(UUID(0, 0), hashSetOf(FluidType.WATER), FluidType.WATER, Long.MAX_VALUE, Long.MAX_VALUE) {
    override fun addFluid(type: FluidType, amount: Long) = Unit
    override fun tryAddFluid(type: FluidType, amount: Long) = 0L
    override fun takeFluid(amount: Long) = Unit
    override fun tryTakeFluid(amount: Long) = amount
    override fun clear() = Unit
    override fun accepts(type: FluidType, amount: Long) = false
    override fun isFull() = true
    override fun hasFluid() = true
    override fun isEmpty() = false
}

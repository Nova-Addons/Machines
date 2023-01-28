package xyz.xenondevs.nova.machines.tileentity.world

import org.bukkit.Material
import xyz.xenondevs.invui.gui.builder.GuiBuilder
import xyz.xenondevs.invui.gui.builder.guitype.GuiType
import xyz.xenondevs.invui.virtualinventory.event.ItemUpdateEvent
import xyz.xenondevs.nova.api.event.tileentity.TileEntityBreakBlockEvent
import xyz.xenondevs.nova.data.config.GlobalValues
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.data.config.configReloadable
import xyz.xenondevs.nova.data.world.block.state.NovaTileEntityState
import xyz.xenondevs.nova.integration.protection.ProtectionManager
import xyz.xenondevs.nova.machines.registry.Blocks.BLOCK_BREAKER
import xyz.xenondevs.nova.tileentity.NetworkedTileEntity
import xyz.xenondevs.nova.tileentity.network.NetworkConnectionType
import xyz.xenondevs.nova.tileentity.network.item.holder.NovaItemHolder
import xyz.xenondevs.nova.tileentity.upgrade.Upgradable
import xyz.xenondevs.nova.ui.EnergyBar
import xyz.xenondevs.nova.ui.OpenUpgradesItem
import xyz.xenondevs.nova.ui.config.side.OpenSideConfigItem
import xyz.xenondevs.nova.ui.config.side.SideConfigGui
import xyz.xenondevs.nova.util.BlockSide
import xyz.xenondevs.nova.util.advance
import xyz.xenondevs.nova.util.callEvent
import xyz.xenondevs.nova.util.center
import xyz.xenondevs.nova.util.getAllDrops
import xyz.xenondevs.nova.util.hardness
import xyz.xenondevs.nova.util.item.ToolUtils
import xyz.xenondevs.nova.util.item.isTraversable
import xyz.xenondevs.nova.util.remove
import xyz.xenondevs.nova.util.setBreakStage
import xyz.xenondevs.nova.world.block.context.BlockBreakContext
import xyz.xenondevs.nova.world.pos
import xyz.xenondevs.simpleupgrades.ConsumerEnergyHolder
import xyz.xenondevs.simpleupgrades.registry.UpgradeTypes
import kotlin.math.min
import kotlin.math.roundToInt

private val MAX_ENERGY = configReloadable { NovaConfig[BLOCK_BREAKER].getLong("capacity") }
private val ENERGY_PER_TICK = configReloadable { NovaConfig[BLOCK_BREAKER].getLong("energy_per_tick") }
private val BREAK_SPEED_MULTIPLIER by configReloadable { NovaConfig[BLOCK_BREAKER].getDouble("break_speed_multiplier") }
private val BREAK_SPEED_CLAMP by configReloadable { NovaConfig[BLOCK_BREAKER].getDouble("break_speed_clamp") }

class BlockBreaker(blockState: NovaTileEntityState) : NetworkedTileEntity(blockState), Upgradable {
    
    private val inventory = getInventory("inventory", 9, ::handleInventoryUpdate)
    override val gui = lazy { BlockBreakerGui() }
    override val upgradeHolder = getUpgradeHolder(UpgradeTypes.SPEED, UpgradeTypes.EFFICIENCY, UpgradeTypes.ENERGY)
    override val energyHolder = ConsumerEnergyHolder(this, MAX_ENERGY, ENERGY_PER_TICK, null, upgradeHolder) { createSideConfig(NetworkConnectionType.INSERT, BlockSide.FRONT) }
    override val itemHolder = NovaItemHolder(
        this,
        inventory to NetworkConnectionType.EXTRACT
    ) { createSideConfig(NetworkConnectionType.EXTRACT, BlockSide.FRONT) }
    
    private val entityId = uuid.hashCode()
    private val block = location.clone().advance(getFace(BlockSide.FRONT)).block
    private var lastType: Material? = null
    private var breakProgress = retrieveData("breakProgress") { 0.0 }
    
    override fun saveData() {
        super.saveData()
        storeData("breakProgress", breakProgress)
    }
    
    private fun handleInventoryUpdate(event: ItemUpdateEvent) {
        if (event.isAdd && event.updateReason != SELF_UPDATE_REASON)
            event.isCancelled = true
    }
    
    override fun handleTick() {
        val type = block.type
        if (energyHolder.energy >= energyHolder.energyConsumption
            && !type.isTraversable()
            && block.hardness >= 0
            && ProtectionManager.canBreak(this, null, block.location).get()
        ) {
            // consume energy
            energyHolder.energy -= energyHolder.energyConsumption
            
            // reset progress when block changed
            if (lastType != null && type != lastType) breakProgress = 0.0
            
            // set last known type
            lastType = type
            
            // add progress
            val damage = ToolUtils.calculateDamage(
                block.hardness,
                correctCategory = true,
                correctForDrops = true,
                toolMultiplier = BREAK_SPEED_MULTIPLIER * upgradeHolder.getValue(UpgradeTypes.SPEED),
                efficiency = 0,
                onGround = true,
                underWater = false,
                hasteLevel = 0,
                fatigueLevel = 0
            ).coerceAtMost(BREAK_SPEED_CLAMP)
            breakProgress = min(1.0, breakProgress + damage)
            
            if (breakProgress >= 1.0) {
                val ctx = BlockBreakContext(block.pos, this, location)
                var drops = block.getAllDrops(ctx).toMutableList()
                drops = TileEntityBreakBlockEvent(this, block, drops).also(::callEvent).drops
                
                if (!GlobalValues.DROP_EXCESS_ON_GROUND && !inventory.canHold(drops))
                    return
                
                // reset break progress
                breakProgress = 0.0
                block.setBreakStage(entityId, -1)
                
                // break block, add items to inventory / drop them if full
                block.remove(ctx)
                drops.forEach { drop ->
                    val amountLeft = inventory.addItem(SELF_UPDATE_REASON, drop)
                    if (GlobalValues.DROP_EXCESS_ON_GROUND && amountLeft != 0) {
                        drop.amount = amountLeft
                        world.dropItemNaturally(block.location.center(), drop)
                    }
                }
            } else {
                // send break state
                block.setBreakStage(entityId, (breakProgress * 9).roundToInt())
            }
        }
    }
    
    override fun handleRemoved(unload: Boolean) {
        super.handleRemoved(unload)
        if (!unload)
            block.setBreakStage(entityId, -1)
    }
    
    inner class BlockBreakerGui : TileEntityGui() {
        
        private val sideConfigGui = SideConfigGui(
            this@BlockBreaker,
            listOf(Pair(itemHolder.getNetworkedInventory(inventory), "inventory.nova.default")),
            ::openWindow
        )
        
        override val gui = GuiBuilder(GuiType.NORMAL)
            .setStructure(
                "1 - - - - - - - 2",
                "| s # i i i # e |",
                "| u # i i i # e |",
                "| # # i i i # e |",
                "3 - - - - - - - 4")
            .addIngredient('i', inventory)
            .addIngredient('s', OpenSideConfigItem(sideConfigGui))
            .addIngredient('u', OpenUpgradesItem(upgradeHolder))
            .addIngredient('e', EnergyBar(3, energyHolder))
            .build()
        
    }
    
}
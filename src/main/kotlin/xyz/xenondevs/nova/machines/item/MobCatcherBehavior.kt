package xyz.xenondevs.nova.machines.item

import de.studiocode.invui.item.builder.ItemBuilder
import net.md_5.bungee.api.ChatColor
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.Mob
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.data.config.configReloadable
import xyz.xenondevs.nova.integration.protection.ProtectionManager
import xyz.xenondevs.nova.item.behavior.ItemBehavior
import xyz.xenondevs.nova.machines.registry.Items
import xyz.xenondevs.nova.util.EntityUtils
import xyz.xenondevs.nova.util.addPrioritized
import xyz.xenondevs.nova.util.data.addLoreLines
import xyz.xenondevs.nova.util.data.localized
import xyz.xenondevs.nova.util.getTargetLocation
import xyz.xenondevs.nova.util.item.retrieveData
import xyz.xenondevs.nova.util.item.storeData

private val DATA_KEY = NamespacedKey(NOVA, "entityData")
private val TYPE_KEY = NamespacedKey(NOVA, "entityType")
private val TIME_KEY = NamespacedKey(NOVA, "fillTime")

private val BLACKLISTED_ENTITY_TYPES by configReloadable {
    NovaConfig[Items.MOB_CATCHER]
        .getStringList("entity_blacklist")
        .mapTo(HashSet(), EntityType::valueOf)
}

object MobCatcherBehavior : ItemBehavior() {
    
    override fun handleEntityInteract(player: Player, itemStack: ItemStack, clicked: Entity, event: PlayerInteractAtEntityEvent) {
        if (clicked is Mob
            && clicked.type !in BLACKLISTED_ENTITY_TYPES
            && ProtectionManager.canInteractWithEntity(player, clicked, itemStack).get()
            && getEntityData(itemStack) == null
        ) {
            
            val fakeDamageEvent = EntityDamageByEntityEvent(player, clicked, EntityDamageEvent.DamageCause.ENTITY_ATTACK, Double.MAX_VALUE)
            Bukkit.getPluginManager().callEvent(fakeDamageEvent)
            
            if (!fakeDamageEvent.isCancelled && fakeDamageEvent.damage != 0.0) {
                val newCatcher = Items.MOB_CATCHER.createItemStack()
                absorbEntity(newCatcher, clicked)
                
                player.inventory.getItem(event.hand)!!.amount -= 1
                player.inventory.addPrioritized(event.hand, newCatcher)
                
                if (event.hand == EquipmentSlot.HAND) player.swingMainHand() else player.swingOffHand()
                
                event.isCancelled = true
            }
            
        }
    }
    
    override fun handleInteract(player: Player, itemStack: ItemStack, action: Action, event: PlayerInteractEvent) {
        if (action == Action.RIGHT_CLICK_BLOCK) {
            // Adds a small delay to prevent players from spamming the item
            if (System.currentTimeMillis() - (itemStack.retrieveData<Long>(TIME_KEY) ?: -1) < 50) return
            
            val data = getEntityData(itemStack)
            if (data != null) {
                val location = player.eyeLocation.getTargetLocation(0.25, 8.0)
                
                if (ProtectionManager.canUseItem(player, itemStack, location).get()) {
                    player.inventory.getItem(event.hand!!)!!.amount -= 1
                    player.inventory.addPrioritized(event.hand!!, Items.MOB_CATCHER.createItemStack())
                    
                    
                    EntityUtils.deserializeAndSpawn(data, location)
                    if (event.hand == EquipmentSlot.HAND) player.swingMainHand() else player.swingOffHand()
                    
                    event.isCancelled = true
                }
            }
        }
    }
    
    fun getEntityData(itemStack: ItemStack): ByteArray? = itemStack.retrieveData(DATA_KEY)
    
    fun getEntityType(itemStack: ItemStack): EntityType? = itemStack.retrieveData<String>(TYPE_KEY)?.let { EntityType.valueOf(it) }
    
    private fun setEntityData(itemStack: ItemStack, type: EntityType, data: ByteArray) {
        itemStack.storeData(DATA_KEY, data)
        itemStack.storeData(TYPE_KEY, type.name)
        itemStack.storeData(TIME_KEY, System.currentTimeMillis())
    }
    
    private fun absorbEntity(itemStack: ItemStack, entity: Entity) {
        val data = EntityUtils.serialize(entity, true)
        setEntityData(itemStack, entity.type, data)
        
        itemStack.itemMeta = ItemBuilder(itemStack).addLoreLines(localized(
            ChatColor.DARK_GRAY,
            "item.machines.mob_catcher.type",
            localized(ChatColor.YELLOW, entity)
        )).get().itemMeta
    }
    
}
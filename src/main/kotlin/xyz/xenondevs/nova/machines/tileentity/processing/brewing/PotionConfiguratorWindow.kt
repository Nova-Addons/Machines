package xyz.xenondevs.nova.machines.tileentity.processing.brewing

import de.studiocode.invui.gui.GUI
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.guitype.GUIType
import de.studiocode.invui.item.ItemProvider
import de.studiocode.invui.item.ItemWrapper
import de.studiocode.invui.item.builder.ItemBuilder
import de.studiocode.invui.item.builder.PotionBuilder
import de.studiocode.invui.item.impl.BaseItem
import de.studiocode.invui.item.impl.CycleItem
import de.studiocode.invui.window.impl.single.SimpleWindow
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.TranslatableComponent
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionData
import org.bukkit.potion.PotionEffectType
import org.bukkit.potion.PotionType
import xyz.xenondevs.nova.machines.registry.GUIMaterials
import xyz.xenondevs.nova.machines.registry.GUITextures
import xyz.xenondevs.nova.machines.tileentity.processing.brewing.ElectricBrewingStand.Companion.ALLOW_DURATION_AMPLIFIER_MIXING
import xyz.xenondevs.nova.machines.tileentity.processing.brewing.ElectricBrewingStand.Companion.AVAILABLE_POTION_EFFECTS
import xyz.xenondevs.nova.material.CoreGUIMaterial
import xyz.xenondevs.nova.ui.config.side.BackItem
import xyz.xenondevs.nova.ui.item.clickableItem
import xyz.xenondevs.nova.ui.menu.ColorPickerWindow
import xyz.xenondevs.nova.ui.menu.ColorPreviewItem
import xyz.xenondevs.nova.ui.menu.OpenColorPickerWindowItem
import xyz.xenondevs.nova.ui.overlay.character.gui.CoreGUITexture
import xyz.xenondevs.nova.util.data.addLoreLines
import xyz.xenondevs.nova.util.data.localized
import xyz.xenondevs.nova.util.playClickSound
import xyz.xenondevs.nova.util.playItemPickupSound
import java.awt.Color
import kotlin.math.min

class PotionConfiguratorWindow(
    effects: List<PotionEffectBuilder>,
    private var type: PotionBuilder.PotionType,
    color: Color,
    private val updatePotionData: (PotionBuilder.PotionType, List<PotionEffectBuilder>, Color) -> Unit,
    openPrevious: (Player) -> Unit
) {
    
    private val effects: MutableMap<PotionEffectBuilder, PotionTypeGUI> = effects.associateWithTo(LinkedHashMap(), ::PotionTypeGUI)
    
    private val potionTypeItem = CycleItem.withStateChangeHandler(
        { p, i -> p.playItemPickupSound(); type = PotionBuilder.PotionType.values()[i] },
        type.ordinal,
        PotionBuilder(PotionBuilder.PotionType.NORMAL).setDisplayName(TranslatableComponent("menu.machines.potion_configurator.potion_type.normal")),
        PotionBuilder(PotionBuilder.PotionType.SPLASH).setDisplayName(TranslatableComponent("menu.machines.potion_configurator.potion_type.splash")),
        PotionBuilder(PotionBuilder.PotionType.LINGERING).setDisplayName(TranslatableComponent("menu.machines.potion_configurator.potion_type.lingering"))
    )
    
    private val colorPickerWindow = ColorPickerWindow(
        PotionColorPreviewItem(
            PotionBuilder(PotionBuilder.PotionType.NORMAL)
                .setDisplayName(TranslatableComponent("menu.machines.color_picker.current_color"))
        ), color, ::openConfigurator)
    
    private val gui = GUIBuilder(GUIType.SCROLL_GUIS)
        .setStructure(
            "< c t . . . . . s",
            "x x x x x x x x u",
            "x x x x x x x x .",
            "x x x x x x x x .",
            "x x x x x x x x .",
            "x x x x x x x x d")
        .addIngredient('<', BackItem { p -> updatePotionData(type, this.effects.keys.filter { it.type != null }, colorPickerWindow.color); openPrevious(p) })
        .addIngredient('c', OpenColorPickerWindowItem(colorPickerWindow))
        .addIngredient('t', potionTypeItem)
        .build()
    
    init {
        updateEffectGUIs()
    }
    
    private fun removeEffect(effect: PotionEffectBuilder) {
        effects -= effect
        updateEffectGUIs()
    }
    
    private fun addEffect() {
        val builder = PotionEffectBuilder()
        val gui = PotionTypeGUI(builder)
        effects[builder] = gui
        updateEffectGUIs()
    }
    
    private fun updateEffectGUIs() {
        val guis = effects.values.mapTo(ArrayList()) { it.gui }
        guis += createAddEffectGUI()
        gui.setGuis(guis)
    }
    
    private fun createAddEffectGUI(): GUI {
        return GUIBuilder(GUIType.NORMAL)
            .setStructure("+ . . . . . . .")
            .addIngredient('+', clickableItem(
                GUIMaterials.TP_GREEN_PLUS.createItemBuilder()
                    .setDisplayName(TranslatableComponent("menu.machines.potion_configurator.add_effect"))
            ) { it.playClickSound(); addEffect() })
            .build()
    }
    
    fun openConfigurator(player: Player) {
        SimpleWindow(player, GUITextures.CONFIGURE_POTION.getTitle("menu.machines.electric_brewing_stand.configure_potion"), gui).show()
    }
    
    private inner class PotionTypeGUI(private val effect: PotionEffectBuilder) {
        
        private val durationModifierItem = DurationModifierItem()
        private val amplifierModifierItem = AmplifierModifierItem()
        private val potionPickerItem = OpenPotionPickerItem()
        
        val gui: GUI = GUIBuilder(GUIType.NORMAL)
            .setStructure("- . p . d . a .")
            .addIngredient('p', potionPickerItem)
            .addIngredient('d', durationModifierItem)
            .addIngredient('a', amplifierModifierItem)
            .addIngredient('-', clickableItem(
                GUIMaterials.TP_RED_MINUS.createItemBuilder()
                    .setDisplayName(TranslatableComponent("menu.machines.potion_configurator.remove_effect"))
            ) { it.playClickSound(); removeEffect(effect) })
            .build()
        
        private inner class OpenPotionPickerItem : BaseItem() {
            
            override fun getItemProvider(): ItemProvider {
                return if (effect.type != null) {
                    PotionBuilder(PotionBuilder.PotionType.NORMAL)
                        .setBasePotionData(PotionData(PotionType.WATER, false, false))
                        .setDisplayName(TranslatableComponent("menu.machines.potion_configurator.effect"))
                        .addEffect(effect.build())
                } else ItemBuilder(Material.GLASS_BOTTLE)
                    .setDisplayName(TranslatableComponent("menu.machines.potion_configurator.undefined_effect"))
            }
            
            override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
                PickPotionWindow(effect).openPicker(player)
            }
            
        }
        
        private inner class DurationModifierItem : BaseItem() {
            
            override fun getItemProvider(): ItemProvider {
                if (effect.type == null) return ItemWrapper(ItemStack(Material.AIR))
                
                val durationLevel = effect.durationLevel + 1
                val maxDurationLevel = effect.maxDurationLevel + 1
                
                return CoreGUIMaterial.NUMBER.item.createItemBuilder(min(999, durationLevel))
                    .setDisplayName(TranslatableComponent("menu.machines.potion_configurator.duration", durationLevel, maxDurationLevel))
                    .addLoreLines(
                        localized(ChatColor.GRAY, "menu.machines.potion_configurator.left_inc"),
                        localized(ChatColor.GRAY, "menu.machines.potion_configurator.right_dec"),
                    )
            }
            
            override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
                if (effect.type == null) return
                
                if (clickType.isLeftClick) {
                    if (effect.durationLevel < effect.maxDurationLevel) {
                        effect.durationLevel++
                        player.playItemPickupSound()
                        notifyWindows()
                        potionPickerItem.notifyWindows()
                        
                        if (!ALLOW_DURATION_AMPLIFIER_MIXING) {
                            effect.amplifierLevel = 0
                            amplifierModifierItem.notifyWindows()
                        }
                    }
                } else if (clickType.isRightClick) {
                    if (effect.durationLevel > 0) {
                        effect.durationLevel--
                        player.playItemPickupSound()
                        notifyWindows()
                        potionPickerItem.notifyWindows()
                    }
                }
            }
        }
        
        private inner class AmplifierModifierItem : BaseItem() {
            
            override fun getItemProvider(): ItemProvider {
                if (effect.type == null) return ItemWrapper(ItemStack(Material.AIR))
                
                val amplifierLevel = effect.amplifierLevel + 1
                val maxAmplifierLevel = effect.maxAmplifierLevel + 1
                
                return CoreGUIMaterial.NUMBER.item.createItemBuilder(min(999, amplifierLevel))
                    .setDisplayName(TranslatableComponent("menu.machines.potion_configurator.amplifier", amplifierLevel, maxAmplifierLevel))
                    .addLoreLines(
                        localized(ChatColor.GRAY, "menu.machines.potion_configurator.left_inc"),
                        localized(ChatColor.GRAY, "menu.machines.potion_configurator.right_dec"),
                    )
            }
            
            override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
                if (effect.type == null) return
                
                if (clickType.isLeftClick) {
                    if (effect.amplifierLevel < effect.maxAmplifierLevel) {
                        effect.amplifierLevel++
                        player.playItemPickupSound()
                        notifyWindows()
                        potionPickerItem.notifyWindows()
                        
                        if (!ALLOW_DURATION_AMPLIFIER_MIXING) {
                            effect.durationLevel = 0
                            durationModifierItem.notifyWindows()
                        }
                    }
                } else if (clickType.isRightClick) {
                    if (effect.amplifierLevel > 0) {
                        effect.amplifierLevel--
                        player.playItemPickupSound()
                        notifyWindows()
                        potionPickerItem.notifyWindows()
                    }
                }
            }
            
        }
        
    }
    
    private inner class PickPotionWindow(private val effect: PotionEffectBuilder) {
        
        private val potionItems = AVAILABLE_POTION_EFFECTS.keys
            .filter { availableEffect -> effects.keys.none { builder -> builder.type == availableEffect } }
            .map(::ChooseEffectTypeItem)
        
        private val gui = GUIBuilder(GUIType.SCROLL_ITEMS)
            .setStructure(
                "< - - - - - - - 2",
                "| x x x x x x x u",
                "| x x x x x x x |",
                "| x x x x x x x |",
                "| x x x x x x x d",
                "3 - - - - - - - 4")
            .addIngredient('<', BackItem { openConfigurator(it) })
            .setItems(potionItems)
            .build()
        
        fun openPicker(player: Player) {
            SimpleWindow(player, CoreGUITexture.EMPTY_GUI.getTitle("menu.machines.electric_brewing_stand.pick_effect"), gui).show()
        }
        
        private inner class ChooseEffectTypeItem(private val type: PotionEffectType) : BaseItem() {
            
            @Suppress("DEPRECATION")
            override fun getItemProvider(): ItemProvider {
                val type = PotionType.getByEffect(type)
                    ?: return ItemBuilder(Material.POTION).setDisplayName(type.name)
                
                return PotionBuilder(PotionBuilder.PotionType.NORMAL)
                    .setBasePotionData(PotionData(type, false, false))
            }
            
            override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
                effect.type = type
                openConfigurator(player)
                player.playItemPickupSound()
            }
            
        }
        
    }
    
}

class PotionColorPreviewItem(builder: PotionBuilder, color: Color = Color(0, 0, 0)) : ColorPreviewItem(color) {
    
    private val builder: PotionBuilder = builder.clone()
    
    override fun getItemProvider(): PotionBuilder =
        builder.setColor(color)
    
}
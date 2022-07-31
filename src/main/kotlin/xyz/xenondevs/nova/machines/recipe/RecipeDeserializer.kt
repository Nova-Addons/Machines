package xyz.xenondevs.nova.machines.recipe

import com.google.gson.JsonObject
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.RecipeChoice
import org.bukkit.potion.PotionEffectType
import xyz.xenondevs.nova.data.serialization.json.ConversionRecipeDeserializer
import xyz.xenondevs.nova.data.serialization.json.RecipeDeserializer
import xyz.xenondevs.nova.tileentity.network.fluid.FluidType
import xyz.xenondevs.nova.util.data.getDeserialized
import xyz.xenondevs.nova.util.data.getDouble
import xyz.xenondevs.nova.util.data.getInputStacks
import xyz.xenondevs.nova.util.data.getInt
import xyz.xenondevs.nova.util.data.getLong
import xyz.xenondevs.nova.util.data.getString
import xyz.xenondevs.nova.util.item.ItemUtils
import java.io.File

object PulverizerRecipeDeserializer : ConversionRecipeDeserializer<PulverizerRecipe>() {
    override fun createRecipe(json: JsonObject, key: NamespacedKey, input: RecipeChoice, result: ItemStack, time: Int) =
        PulverizerRecipe(key, input, result, time)
}

object PlatePressRecipeDeserializer : ConversionRecipeDeserializer<PlatePressRecipe>() {
    override fun createRecipe(json: JsonObject, key: NamespacedKey, input: RecipeChoice, result: ItemStack, time: Int) =
        PlatePressRecipe(key, input, result, time)
}

object GearPressRecipeDeserializer : ConversionRecipeDeserializer<GearPressRecipe>() {
    override fun createRecipe(json: JsonObject, key: NamespacedKey, input: RecipeChoice, result: ItemStack, time: Int) =
        GearPressRecipe(key, input, result, time)
}

object FluidInfuserRecipeDeserializer : RecipeDeserializer<FluidInfuserRecipe> {
    
    override fun deserialize(json: JsonObject, file: File): FluidInfuserRecipe {
        val mode = json.getDeserialized<FluidInfuserRecipe.InfuserMode>("mode")!!
        val fluidType = json.getDeserialized<FluidType>("fluid_type")!!
        val fluidAmount = json.getLong("fluid_amount")!!
        val input = parseRecipeChoice(json.get("input"))
        val time = json.getInt("time")!!
        val result = ItemUtils.getItemBuilder(json.getString("result")!!).get()
        
        return FluidInfuserRecipe(getRecipeKey(file), mode, fluidType, fluidAmount, input, result, time)
    }
    
}

object ElectricBrewingStandRecipeDeserializer : RecipeDeserializer<ElectricBrewingStandRecipe> {
    
    override fun deserialize(json: JsonObject, file: File): ElectricBrewingStandRecipe {
        val inputs = json.getAsJsonArray("inputs").map { ItemUtils.getRecipeChoice(listOf(it.asString)) }
        require(inputs.all { it.getInputStacks().size == 1 })
        
        val resultName = json.getString("result")
            ?: throw IllegalArgumentException("No result provided")
        val result = PotionEffectType.getByKey(NamespacedKey.fromString(resultName))
            ?: throw IllegalArgumentException("Invalid result")
        
        val defaultTime = json.getInt("default_time", 0)
        val redstoneMultiplier = json.getDouble("redstone_multiplier", 0.0)
        val glowstoneMultiplier = json.getDouble("glowstone_multiplier", 0.0)
        val maxDurationLevel = json.getInt("max_duration_level", 0)
        val maxAmplifierLevel = json.getInt("max_amplifier_level", 0)
        
        return ElectricBrewingStandRecipe(
            getRecipeKey(file),
            inputs, result,
            defaultTime,
            redstoneMultiplier, glowstoneMultiplier,
            maxDurationLevel, maxAmplifierLevel
        )
    }
    
}

package xyz.xenondevs.nova.machines.registry

import xyz.xenondevs.nova.data.recipe.RecipeTypeRegistry.register
import xyz.xenondevs.nova.machines.Machines
import xyz.xenondevs.nova.machines.recipe.CrystallizerRecipe
import xyz.xenondevs.nova.machines.recipe.CrystallizerRecipeDeserializer
import xyz.xenondevs.nova.machines.recipe.ElectricBrewingStandRecipe
import xyz.xenondevs.nova.machines.recipe.ElectricBrewingStandRecipeDeserializer
import xyz.xenondevs.nova.machines.recipe.FluidInfuserRecipe
import xyz.xenondevs.nova.machines.recipe.FluidInfuserRecipeDeserializer
import xyz.xenondevs.nova.machines.recipe.GearPressRecipe
import xyz.xenondevs.nova.machines.recipe.GearPressRecipeDeserializer
import xyz.xenondevs.nova.machines.recipe.PlatePressRecipe
import xyz.xenondevs.nova.machines.recipe.PlatePressRecipeDeserializer
import xyz.xenondevs.nova.machines.recipe.PulverizerRecipe
import xyz.xenondevs.nova.machines.recipe.PulverizerRecipeDeserializer
import xyz.xenondevs.nova.machines.recipe.group.CrystallizerRecipeGroup
import xyz.xenondevs.nova.machines.recipe.group.FluidInfuserRecipeGroup
import xyz.xenondevs.nova.machines.recipe.group.PressingRecipeGroup
import xyz.xenondevs.nova.machines.recipe.group.PulverizingRecipeGroup
import xyz.xenondevs.nova.machines.recipe.group.hardcoded.CobblestoneGeneratorRecipe
import xyz.xenondevs.nova.machines.recipe.group.hardcoded.CobblestoneGeneratorRecipeGroup
import xyz.xenondevs.nova.machines.recipe.group.hardcoded.FreezerRecipe
import xyz.xenondevs.nova.machines.recipe.group.hardcoded.FreezerRecipeGroup
import xyz.xenondevs.nova.machines.recipe.group.hardcoded.StarCollectorRecipe
import xyz.xenondevs.nova.machines.recipe.group.hardcoded.StarCollectorRecipeGroup

object RecipeTypes {
    
    val PULVERIZER = register(Machines, "pulverizer", PulverizerRecipe::class, PulverizingRecipeGroup, PulverizerRecipeDeserializer)
    val GEAR_PRESS = register(Machines, "press/gear", GearPressRecipe::class, PressingRecipeGroup, GearPressRecipeDeserializer)
    val PLATE_PRESS = register(Machines, "press/plate", PlatePressRecipe::class, PressingRecipeGroup, PlatePressRecipeDeserializer)
    val FLUID_INFUSER = register(Machines, "fluid_infuser", FluidInfuserRecipe::class, FluidInfuserRecipeGroup, FluidInfuserRecipeDeserializer)
    val ELECTRIC_BREWING_STAND = register(Machines, "electric_brewing_stand", ElectricBrewingStandRecipe::class, null, ElectricBrewingStandRecipeDeserializer)
    val CRYSTALLIZER = register(Machines, "crystallizer", CrystallizerRecipe::class, CrystallizerRecipeGroup, CrystallizerRecipeDeserializer)
    val STAR_COLLECTOR = register(Machines, null, StarCollectorRecipe::class, StarCollectorRecipeGroup, null)
    val COBBLESTONE_GENERATOR = register(Machines, null, CobblestoneGeneratorRecipe::class, CobblestoneGeneratorRecipeGroup, null)
    val FREEZER = register(Machines, null, FreezerRecipe::class, FreezerRecipeGroup, null)
    
    fun init() = Unit
    
}
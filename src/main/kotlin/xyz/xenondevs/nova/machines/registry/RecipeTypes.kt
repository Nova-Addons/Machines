package xyz.xenondevs.nova.machines.registry

import xyz.xenondevs.nova.addon.registry.RecipeTypeRegistry
import xyz.xenondevs.nova.initialize.Init
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
import xyz.xenondevs.nova.machines.recipe.group.ElectricBrewingStandRecipeGroup
import xyz.xenondevs.nova.machines.recipe.group.FluidInfuserRecipeGroup
import xyz.xenondevs.nova.machines.recipe.group.PressingRecipeGroup
import xyz.xenondevs.nova.machines.recipe.group.PulverizingRecipeGroup
import xyz.xenondevs.nova.machines.recipe.group.hardcoded.CobblestoneGeneratorRecipe
import xyz.xenondevs.nova.machines.recipe.group.hardcoded.CobblestoneGeneratorRecipeGroup
import xyz.xenondevs.nova.machines.recipe.group.hardcoded.FreezerRecipe
import xyz.xenondevs.nova.machines.recipe.group.hardcoded.FreezerRecipeGroup
import xyz.xenondevs.nova.machines.recipe.group.hardcoded.StarCollectorRecipe
import xyz.xenondevs.nova.machines.recipe.group.hardcoded.StarCollectorRecipeGroup

@Init
object RecipeTypes : RecipeTypeRegistry by Machines.registry {
    
    val PULVERIZER = registerRecipeType("pulverizer", PulverizerRecipe::class, PulverizingRecipeGroup, PulverizerRecipeDeserializer)
    val GEAR_PRESS = registerRecipeType("press/gear", GearPressRecipe::class, PressingRecipeGroup, GearPressRecipeDeserializer)
    val PLATE_PRESS = registerRecipeType("press/plate", PlatePressRecipe::class, PressingRecipeGroup, PlatePressRecipeDeserializer)
    val FLUID_INFUSER = registerRecipeType("fluid_infuser", FluidInfuserRecipe::class, FluidInfuserRecipeGroup, FluidInfuserRecipeDeserializer)
    val ELECTRIC_BREWING_STAND = registerRecipeType("electric_brewing_stand", ElectricBrewingStandRecipe::class, ElectricBrewingStandRecipeGroup, ElectricBrewingStandRecipeDeserializer)
    val CRYSTALLIZER = registerRecipeType("crystallizer", CrystallizerRecipe::class, CrystallizerRecipeGroup, CrystallizerRecipeDeserializer)
    val STAR_COLLECTOR = registerRecipeType("star_collector", StarCollectorRecipe::class, StarCollectorRecipeGroup, null)
    val COBBLESTONE_GENERATOR = registerRecipeType("cobblestone_generator", CobblestoneGeneratorRecipe::class, CobblestoneGeneratorRecipeGroup, null)
    val FREEZER = registerRecipeType("freezer", FreezerRecipe::class, FreezerRecipeGroup, null)
    
}
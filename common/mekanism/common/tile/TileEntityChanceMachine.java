package mekanism.common.tile;

import java.util.Map;

import mekanism.api.EnumColor;
import mekanism.api.ListUtils;
import mekanism.common.Mekanism;
import mekanism.common.SideData;
import mekanism.common.recipe.RecipeHandler;
import mekanism.common.tile.component.TileComponentEjector;
import mekanism.common.tile.component.TileComponentUpgrade;
import mekanism.common.util.ChargeUtils;
import mekanism.common.util.InventoryUtils;
import mekanism.common.util.MekanismUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import dan200.computer.api.IComputerAccess;
import dan200.computer.api.ILuaContext;

public class TileEntityChanceMachine extends TileEntityBasicMachine
{
	public TileEntityChanceMachine(String soundPath, String name, ResourceLocation location, double perTick, int ticksRequired, double maxEnergy)
	{
		super(soundPath, name, location, perTick, ticksRequired, maxEnergy);
		
		sideOutputs.add(new SideData(EnumColor.GREY, InventoryUtils.EMPTY));
		sideOutputs.add(new SideData(EnumColor.DARK_RED, new int[] {0}));
		sideOutputs.add(new SideData(EnumColor.DARK_GREEN, new int[] {1}));
		sideOutputs.add(new SideData(EnumColor.DARK_BLUE, new int[] {2}));
		sideOutputs.add(new SideData(EnumColor.ORANGE, new int[] {3}));
		sideOutputs.add(new SideData(EnumColor.YELLOW, new int[] {4}));
		
		sideConfig = new byte[] {2, 1, 0, 0, 4, 3};
		
		inventory = new ItemStack[5];
		
		upgradeComponent = new TileComponentUpgrade(this, 3);
		ejectorComponent = new TileComponentEjector(this, ListUtils.asList(sideOutputs.get(3), sideOutputs.get(5)));
	}
	
	@Override
	public void onUpdate()
	{
		super.onUpdate();
		
		if(!worldObj.isRemote)
		{
			ChargeUtils.discharge(1, this);
			
			if(canOperate() && MekanismUtils.canFunction(this) && getEnergy() >= MekanismUtils.getEnergyPerTick(getSpeedMultiplier(), getEnergyMultiplier(), ENERGY_PER_TICK))
			{
				setActive(true);
				
				if((operatingTicks+1) < MekanismUtils.getTicks(getSpeedMultiplier(), TICKS_REQUIRED))
				{
					operatingTicks++;
					electricityStored -= MekanismUtils.getEnergyPerTick(getSpeedMultiplier(), getEnergyMultiplier(), ENERGY_PER_TICK);
				}
				else if((operatingTicks+1) >= MekanismUtils.getTicks(getSpeedMultiplier(), TICKS_REQUIRED))
				{
					operate();
					
					operatingTicks = 0;
					electricityStored -= MekanismUtils.getEnergyPerTick(getSpeedMultiplier(), getEnergyMultiplier(), ENERGY_PER_TICK);
				}
			}
			else {
				if(prevEnergy >= getEnergy())
				{
					setActive(false);
				}
			}
			
			if(!canOperate())
			{
				operatingTicks = 0;
			}
			
			prevEnergy = getEnergy();
		}
	}
	
	@Override
	public boolean isItemValidForSlot(int slotID, ItemStack itemstack)
	{
		if(slotID == 2)
		{
			return false;
		}
		else if(slotID == 3)
		{
			return itemstack.itemID == Mekanism.SpeedUpgrade.itemID || itemstack.itemID == Mekanism.EnergyUpgrade.itemID;
		}
		else if(slotID == 0)
		{
			return RecipeHandler.isInRecipe(itemstack, getRecipes());
		}
		else if(slotID == 1)
		{
			return ChargeUtils.canBeDischarged(itemstack);
		}
		
		return false;
	}

	@Override
    public void operate()
    {
        ItemStack itemstack = RecipeHandler.getOutput(inventory[0], true, getRecipes());

        if(inventory[0].stackSize <= 0)
        {
            inventory[0] = null;
        }

        if(inventory[2] == null)
        {
            inventory[2] = itemstack;
        }
        else {
            inventory[2].stackSize += itemstack.stackSize;
        }

        onInventoryChanged();
        ejectorComponent.onOutput();
    }

	@Override
    public boolean canOperate()
    {
        if(inventory[0] == null)
        {
            return false;
        }

        ItemStack itemstack = RecipeHandler.getOutput(inventory[0], false, getRecipes());

        if(itemstack == null)
        {
            return false;
        }

        if(inventory[2] == null)
        {
            return true;
        }

        if(!inventory[2].isItemEqual(itemstack))
        {
            return false;
        }
        else {
            return inventory[2].stackSize + itemstack.stackSize <= inventory[2].getMaxStackSize();
        }
    }
	
	@Override
	public boolean canExtractItem(int slotID, ItemStack itemstack, int side)
	{
		if(slotID == 1)
		{
			return ChargeUtils.canBeOutputted(itemstack, false);
		}
		else if(slotID == 2)
		{
			return true;
		}
		
		return false;
	}

	@Override
	public Map getRecipes()
	{
		return null;
	}

	@Override
	public String[] getMethodNames()
	{
		return null;
	}

	@Override
	public Object[] callMethod(IComputerAccess computer, ILuaContext context, int method, Object[] arguments) throws Exception
	{
		return null;
	}
}

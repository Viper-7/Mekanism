package mekanism.common.tile.component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import mekanism.api.Coord4D;
import mekanism.api.EnumColor;
import mekanism.common.IEjector;
import mekanism.common.IInvConfiguration;
import mekanism.common.ILogisticalTransporter;
import mekanism.common.ITileComponent;
import mekanism.common.SideData;
import mekanism.common.tile.TileEntityContainerBlock;
import mekanism.common.transporter.TransporterManager;
import mekanism.common.util.InventoryUtils;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.TransporterUtils;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.ForgeDirection;

import com.google.common.io.ByteArrayDataInput;

public class TileComponentEjector implements ITileComponent, IEjector
{
	public TileEntityContainerBlock tileEntity;
	
	public boolean strictInput;
	
	public boolean ejecting;
	
	public EnumColor outputColor;
	
	public EnumColor[] inputColors = new EnumColor[] {null, null, null, null, null, null};
	
	public int tickDelay = 0;
	
	public List<SideData> dataList;
	
	public List<int[]> trackers = new ArrayList<int[]>();
	
	public TileComponentEjector(TileEntityContainerBlock tile, List<SideData> data)
	{
		tileEntity = tile;
		dataList = data;
		
		for(int i = 0; i < data.size(); i++)
		{
			trackers.add(i, new int[data.get(i).availableSlots.length]);
		}
		
		tile.components.add(this);
	}
	
	private Set<ForgeDirection> getTrackedOutputs(int index, int dataIndex, Set<ForgeDirection> dirs)
	{
		Set<ForgeDirection> sides = new HashSet<ForgeDirection>();
		
		for(int i = trackers.get(dataIndex)[index]+1; i <= trackers.get(dataIndex)[index]+6; i++)
		{
			for(ForgeDirection side : dirs)
			{
				if(ForgeDirection.getOrientation(i%6) == side)
				{
					sides.add(side);
				}
			}
		}
		
		return sides;
	}
	
	@Override
	public void tick() 
	{
		if(tickDelay == 0)
		{
			onOutput();
		}
		else {
			tickDelay--;
		}
	}
	
	@Override
	public void onOutput()
	{
		if(!ejecting || tileEntity.worldObj.isRemote)
		{
			return;
		}
		
		IInvConfiguration configurable = (IInvConfiguration)tileEntity;
		
		for(SideData sideData : dataList)
		{
			Set<ForgeDirection> outputSides = new HashSet<ForgeDirection>();
			
			for(int i = 0; i < configurable.getConfiguration().length; i++)
			{
				if(configurable.getConfiguration()[i] == configurable.getSideData().indexOf(sideData))
				{
					outputSides.add(ForgeDirection.getOrientation(MekanismUtils.getBaseOrientation(i, tileEntity.facing)));
				}
			}
			
			for(int index = 0; index < sideData.availableSlots.length; index++)
			{
				int slotID = sideData.availableSlots[index];
				
				if(tileEntity.inventory[slotID] == null)
				{
					continue;
				}
				
				ItemStack stack = tileEntity.inventory[slotID];
				Set<ForgeDirection> outputs = getTrackedOutputs(index, dataList.indexOf(sideData), outputSides);
				
				for(ForgeDirection side : outputs)
				{
					TileEntity tile = Coord4D.get(tileEntity).getFromSide(side).getTileEntity(tileEntity.worldObj);
					ItemStack prev = stack.copy();
					
					if(tile instanceof IInventory && !(tile instanceof ILogisticalTransporter))
					{
						stack = InventoryUtils.putStackInInventory((IInventory)tile, stack, side.ordinal(), false);
					}
					else if(tile instanceof ILogisticalTransporter)
					{
						ItemStack rejects = TransporterUtils.insert(tileEntity, (ILogisticalTransporter)tile, stack, outputColor, true, 0);
						
						if(TransporterManager.didEmit(stack, rejects))
						{
							stack = rejects;
						}
					}
					
					if(stack == null || prev.stackSize != stack.stackSize)
					{
						trackers.get(dataList.indexOf(sideData))[index] = side.ordinal();
					}
					
					if(stack == null)
					{
						break;
					}
				}
				
				tileEntity.inventory[slotID] = stack;
				tileEntity.onInventoryChanged();
			}
		}
		
		tickDelay = 20;
	}
	
	@Override
	public boolean isEjecting()
	{
		return ejecting;
	}
	
	@Override
	public void setEjecting(boolean eject)
	{
		ejecting = eject;
		MekanismUtils.saveChunk(tileEntity);
	}
	
	@Override
	public boolean hasStrictInput()
	{
		return strictInput;
	}
	
	@Override
	public void setStrictInput(boolean strict)
	{
		strictInput = strict;
		MekanismUtils.saveChunk(tileEntity);
	}
	
	@Override
	public void setOutputColor(EnumColor color)
	{
		outputColor = color;
		MekanismUtils.saveChunk(tileEntity);
	}
	
	@Override
	public EnumColor getOutputColor()
	{
		return outputColor;
	}
	
	@Override
	public void setInputColor(ForgeDirection side, EnumColor color)
	{
		inputColors[side.ordinal()] = color;
		MekanismUtils.saveChunk(tileEntity);
	}
	
	@Override
	public EnumColor getInputColor(ForgeDirection side)
	{
		return inputColors[side.ordinal()];
	}

	@Override
	public void read(NBTTagCompound nbtTags) 
	{
		ejecting = nbtTags.getBoolean("ejecting");
		strictInput = nbtTags.getBoolean("strictInput");
		
		if(nbtTags.hasKey("ejectColor"))
		{
			outputColor = TransporterUtils.colors.get(nbtTags.getInteger("ejectColor"));
		}
		
		for(SideData data : dataList)
		{
			trackers.add(dataList.indexOf(data), nbtTags.getIntArray("tracker" + dataList.indexOf(data)));
		}
		
		for(int i = 0; i < 6; i++)
		{
			if(nbtTags.hasKey("inputColors" + i))
			{
				int inC = nbtTags.getInteger("inputColors" + i);
				
				if(inC != -1)
				{
					inputColors[i] = TransporterUtils.colors.get(inC);
				}
				else {
					inputColors[i] = null;
				}
			}
		}
	}

	@Override
	public void read(ByteArrayDataInput dataStream) 
	{
		ejecting = dataStream.readBoolean();
		strictInput = dataStream.readBoolean();
		
		int c = dataStream.readInt();
		
		if(c != -1)
		{
			outputColor = TransporterUtils.colors.get(c);
		}
		else {
			outputColor = null;
		}
		
		for(int i = 0; i < 6; i++)
		{
			int inC = dataStream.readInt();
			
			if(inC != -1)
			{
				inputColors[i] = TransporterUtils.colors.get(inC);
			}
			else {
				inputColors[i] = null;
			}
		}
	}

	@Override
	public void write(NBTTagCompound nbtTags) 
	{
		nbtTags.setBoolean("ejecting", ejecting);
		nbtTags.setBoolean("strictInput", strictInput);
		
		if(outputColor != null)
		{
			nbtTags.setInteger("ejectColor", TransporterUtils.colors.indexOf(outputColor));
		}
		
		for(SideData data : dataList)
		{
			nbtTags.setIntArray("tracker" + dataList.indexOf(data), trackers.get(dataList.indexOf(data)));
		}
		
		for(int i = 0; i < 6; i++)
		{
			if(inputColors[i] == null)
			{
				nbtTags.setInteger("inputColors" + i, -1);
			}
			else {
				nbtTags.setInteger("inputColors" + i, TransporterUtils.colors.indexOf(inputColors[i]));
			}
		}
	}
	
	@Override
	public void write(ArrayList data) 
	{
		data.add(ejecting);
		data.add(strictInput);
		
		if(outputColor != null)
		{
			data.add(TransporterUtils.colors.indexOf(outputColor));
		}
		else {
			data.add(-1);
		}
		
		for(int i = 0; i < 6; i++)
		{
			if(inputColors[i] == null)
			{
				data.add(-1);
			}
			else {
				data.add(TransporterUtils.colors.indexOf(inputColors[i]));
			}
		}
	}
}

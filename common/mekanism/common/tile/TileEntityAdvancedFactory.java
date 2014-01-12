package mekanism.common.tile;

import mekanism.api.EnumColor;
import mekanism.api.ListUtils;
import mekanism.common.SideData;
import mekanism.common.Tier.FactoryTier;
import mekanism.common.block.BlockMachine.MachineType;
import mekanism.common.tile.component.TileComponentEjector;
import mekanism.common.util.InventoryUtils;

public class TileEntityAdvancedFactory extends TileEntityFactory
{
	public TileEntityAdvancedFactory()
	{
		super(FactoryTier.ADVANCED, MachineType.ADVANCED_FACTORY);
		
		sideOutputs.add(new SideData(EnumColor.GREY, InventoryUtils.EMPTY));
		sideOutputs.add(new SideData(EnumColor.ORANGE, new int[] {0}));
		sideOutputs.add(new SideData(EnumColor.DARK_GREEN, new int[] {1}));
		sideOutputs.add(new SideData(EnumColor.PURPLE, new int[] {4}));
		sideOutputs.add(new SideData(EnumColor.DARK_RED, new int[] {5, 6, 7, 8, 9}));
		sideOutputs.add(new SideData(EnumColor.DARK_BLUE, new int[] {10, 11, 12, 13, 14}));
		
		ejectorComponent = new TileComponentEjector(this, ListUtils.asList(sideOutputs.get(5)));
	}
}

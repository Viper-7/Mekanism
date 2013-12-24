package mekanism.api;

import mekanism.api.gas.GasStack;
import mekanism.api.gas.GasTank;

/**
 * An input of gasses for the Chemical Infuser.
 * @author aidancbrady
 *
 */
public class ChemicalInput
{
	/** The left gas of this chemical input */
	public GasStack leftGas;
	
	/** The right gas of this chemical input */
	public GasStack rightGas;
	
	/**
	 * Creates a chemical input with two defined gasses of the Chemical Infuser.
	 * @param left - left gas
	 * @param right - right gas
	 */
	public ChemicalInput(GasStack left, GasStack right)
	{
		leftGas = left;
		rightGas = right;
	}
	
	/**
	 * If this is a valid
	 * @return
	 */
	public boolean isValid()
	{
		return leftGas != null && rightGas != null;
	}
	
	/**
	 * Whether or not the defined input contains the same gasses and at least the required amount of the defined gasses as this input.
	 * @param input - input to check
	 * @return if the input meets this input's requirements
	 */
	public boolean meetsInput(ChemicalInput input)
	{
		return meets(input) || meets(input.swap());
	}
	
	/**
	 * Swaps the right gas and left gas of this input.
	 * @return a swapped ChemicalInput
	 */
	private ChemicalInput swap()
	{
		return new ChemicalInput(rightGas, leftGas);
	}
	
	/**
	 * Draws the needed amount of gas from each tank.
	 * @param leftTank - left tank to draw from
	 * @param rightTank - right tank to draw from
	 */
	public void draw(GasTank leftTank, GasTank rightTank)
	{
		if(meets(new ChemicalInput(leftTank.getGas(), rightTank.getGas())))
		{
			leftTank.draw(leftGas.amount, true);
			rightTank.draw(rightGas.amount, true);
		}
		else if(meets(new ChemicalInput(rightTank.getGas(), leftTank.getGas())))
		{
			leftTank.draw(rightGas.amount, true);
			rightTank.draw(leftGas.amount, true);
		}
	}
	
	/**
	 * Actual implementation of meetsInput(), performs the checks.
	 * @param input - input to check
	 * @return if the input meets this input's requirements
	 */
	private boolean meets(ChemicalInput input)
	{
		if(input == null || !input.isValid())
		{
			return false;
		}
		
		if(input.leftGas.getGas() != leftGas.getGas() || input.rightGas.getGas() != rightGas.getGas())
		{
			return false;
		}
		
		return input.leftGas.amount >= leftGas.amount && input.rightGas.amount >= rightGas.amount;
	}
}
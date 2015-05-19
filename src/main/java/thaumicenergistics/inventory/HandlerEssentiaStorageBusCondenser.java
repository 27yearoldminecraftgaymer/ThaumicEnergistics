package thaumicenergistics.inventory;

import net.minecraft.tileentity.TileEntity;
import thaumicenergistics.fluids.GaseousEssentia;
import thaumicenergistics.integration.tc.EssentiaConversionHelper;
import thaumicenergistics.parts.AEPartEssentiaStorageBus;
import appeng.api.config.Actionable;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IItemList;
import appeng.tile.misc.TileCondenser;
import com.google.common.collect.ImmutableList;

public class HandlerEssentiaStorageBusCondenser
	extends AbstractHandlerEssentiaStorageBus
{

	private TileCondenser condenser = null;

	public HandlerEssentiaStorageBusCondenser( final AEPartEssentiaStorageBus part )
	{
		super( part );
	}

	@Override
	public boolean canAccept( final IAEFluidStack fluidStack )
	{
		// Ensure there is a condenser
		if( this.condenser == null )
		{
			return false;
		}

		// Ensure the bus has security access
		if( !hasSecurityPermission() )
		{
			// The bus does not have security access.
			return false;
		}

		// Ensure the fluid is an essentia gas
		if( !this.isFluidEssentiaGas( fluidStack ) )
		{
			// Not essentia gas.
			return false;
		}

		// Ensure we are allowed to transfer this fluid
		if( !this.canTransferGas( (GaseousEssentia)fluidStack.getFluid() ) )
		{
			/*
			 * Either: Not on whitelist or is on blacklist
			 */
			return false;
		}

		// Can accept the fluid.
		return true;
	}

	@Override
	public IAEFluidStack extractItems( final IAEFluidStack request, final Actionable mode, final BaseActionSource source )
	{
		// Nothing comes out of the condenser.
		return null;
	}

	@Override
	public IItemList<IAEFluidStack> getAvailableItems( final IItemList<IAEFluidStack> out )
	{
		// Nothing is stored in the condenser.
		return out;
	}

	@Override
	public IAEFluidStack injectItems( final IAEFluidStack input, final Actionable mode, final BaseActionSource source )
	{
		// Ensure input and output
		if( ( this.condenser == null ) || ( input == null ) || ( !this.canAccept( input ) ) )
		{
			return input;
		}

		// Ignore if simulation
		if( mode == Actionable.SIMULATE )
		{
			// Condenser can accept all.
			return null;
		}

		// Create the fluidstack
		IAEFluidStack injectStack = input.copy();

		// Set the amount to the Essentia units, NOT the fluid units
		injectStack.setStackSize( 500 * EssentiaConversionHelper.instance.convertFluidAmountToEssentiaAmount( input.getStackSize() ) );

		// Inject the fluid
		this.condenser.fill( this.partStorageBus.getSide().getOpposite(), injectStack.getFluidStack(), ( mode == Actionable.MODULATE ) );

		// Since the fluid is being voided, update the network so it doesn't think its stored
		injectStack.setStackSize( -input.getStackSize() );
		this.postAlterationToHostGrid( ImmutableList.of( injectStack ) );

		// All fluid accepted.
		return null;
	}

	@Override
	public boolean onNeighborChange()
	{
		// Get the facing tile
		TileEntity te = this.getFaceingTile();

		// Is it a condenser?
		if( te instanceof TileCondenser )
		{
			this.condenser = (TileCondenser)te;
		}
		else
		{
			this.condenser = null;
		}

		// Nothing to update
		return false;
	}

	@Override
	public void tickingRequest( final IGridNode node, final int TicksSinceLastCall )
	{
		// Ignored.
	}

	/**
	 * Only valid for pass 2
	 * 
	 * @return
	 */
	@Override
	public boolean validForPass( final int pass )
	{
		if( this.condenser != null )
		{
			return pass == 2;
		}

		return false;
	}

}

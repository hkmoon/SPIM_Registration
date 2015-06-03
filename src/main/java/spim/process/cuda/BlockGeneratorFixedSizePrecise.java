package spim.process.cuda;

import java.util.ArrayList;

import mpicbg.spim.io.IOFunctions;
import net.imglib2.iterator.LocalizingZeroMinIntervalIterator;
import net.imglib2.util.Util;

public class BlockGeneratorFixedSizePrecise implements BlockGenerator< Block >
{
	final int[] blockSize;

	public BlockGeneratorFixedSizePrecise( final int[] blockSize )
	{
		this.blockSize = blockSize;
	}

	/**
	 * Divides an image into blocks
	 * 
	 * @param imgSize - the size of the image
	 * @param kernelSize - the size of the kernel (has to be odd!)
	 * @return
	 */
	public Block[] divideIntoBlocks( final int[] imgSize, final int[] kernelSize )
	{
		final int numDimensions = imgSize.length;
		
		// compute the effective size & local offset of each block
		// this is the same for all blocks
		final int[] effectiveSizeGeneral = new int[ numDimensions ];
		final int[] effectiveLocalOffset = new int[ numDimensions ];
		
		for ( int d = 0; d < numDimensions; ++d )
		{
			effectiveSizeGeneral[ d ] = blockSize[ d ] - kernelSize[ d ] + 1;
			
			if ( effectiveSizeGeneral[ d ] <= 0 )
			{
				IOFunctions.println( "Blocksize in dimension " + d + " (" + blockSize[ d ] + ") is smaller than the kernel (" + kernelSize[ d ] + ") which results in an negative effective size: " + effectiveSizeGeneral[ d ] + ". Quitting." );
				return null;
			}
			
			effectiveLocalOffset[ d ] = kernelSize[ d ] / 2;
		}
		
		// compute the amount of blocks needed
		final int[] numBlocks = new int[ numDimensions ];

		for ( int d = 0; d < numDimensions; ++d )
		{
			numBlocks[ d ] = imgSize[ d ] / effectiveSizeGeneral[ d ];
			
			// if the modulo is not 0 we need one more that is only partially useful
			if ( imgSize[ d ] % effectiveSizeGeneral[ d ] != 0 )
				++numBlocks[ d ];
		}
		
		System.out.println( "imgSize " + Util.printCoordinates( imgSize ) );
		System.out.println( "kernelSize " + Util.printCoordinates( kernelSize ) );
		System.out.println( "blockSize " + Util.printCoordinates( blockSize ) );
		System.out.println( "numBlocks " + Util.printCoordinates( numBlocks ) );
		IOFunctions.println( "effectiveSize of blocks" + Util.printCoordinates( effectiveSizeGeneral ) );
		System.out.println( "effectiveLocalOffset " + Util.printCoordinates( effectiveLocalOffset ) );
				
		// now we instantiate the individual blocks iterating over all dimensions
		// we use the well-known ArrayLocalizableCursor for that
		final LocalizingZeroMinIntervalIterator cursor = new LocalizingZeroMinIntervalIterator( numBlocks );
		final ArrayList< Block > blockList = new ArrayList< Block >();

		final int[] currentBlock = new int[ numDimensions ];

		while ( cursor.hasNext() )
		{
			cursor.fwd();
			cursor.localize( currentBlock );

			// compute the current offset
			final int[] offset = new int[ numDimensions ];
			final int[] effectiveOffset = new int[ numDimensions ];
			final int[] effectiveSize = effectiveSizeGeneral.clone();

			for ( int d = 0; d < numDimensions; ++d )
			{
				effectiveOffset[ d ] = currentBlock[ d ] * effectiveSize[ d ];
				offset[ d ] = effectiveOffset[ d ] - kernelSize[ d ]/2;

				if ( effectiveOffset[ d ] + effectiveSize[ d ] > imgSize[ d ] )
					effectiveSize[ d ] = imgSize[ d ] - effectiveOffset[ d ];
			}

			blockList.add( new Block( blockSize, offset, effectiveSize, effectiveOffset, effectiveLocalOffset, true ) );
			//System.out.println( "block " + Util.printCoordinates( currentBlock ) + " effectiveOffset: " + Util.printCoordinates( effectiveOffset ) + " effectiveSize: " + Util.printCoordinates( effectiveSize )  + " offset: " + Util.printCoordinates( offset ) + " inside: " + inside );
		}
		
		final Block[] blocks = new Block[ blockList.size() ];
		for ( int i = 0; i < blockList.size(); ++i )
			blocks[ i ] = blockList.get( i );
			
		return blocks;
	}

}

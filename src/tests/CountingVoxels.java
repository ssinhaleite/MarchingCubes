package marchingCubes;

import java.util.ArrayList;
import java.util.List;

import bdv.labels.labelset.Label;
import bdv.labels.labelset.LabelMultisetType;
import bdv.labels.labelset.Multiset;
import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.view.ExtendedRandomAccessibleInterval;
import net.imglib2.view.Views;

/**
 * MarchingCubes from
 * https://github.com/funkey/sg_gui/blob/master/MarchingCubes.h
 */
public class CountingVoxels
{
	public int numberOfVoxels;
	public int numberOfActiveVoxels;
	
	public CountingVoxels()
	{
		numberOfVoxels = 0;
		numberOfActiveVoxels = 0;
	}

	public void countingVoxels( RandomAccessibleInterval< LabelMultisetType > input, int[] cubeSize, int[] volDim, boolean isExact, int level )
	{
//		Timestamp begin = new Timestamp( System.currentTimeMillis() );

		ExtendedRandomAccessibleInterval< LabelMultisetType, RandomAccessibleInterval< LabelMultisetType > > extended =
				Views.extendValue( input, new LabelMultisetType() );
		Cursor< LabelMultisetType > c = Views.interval( extended, new FinalInterval( new long[] { input
				.min( 0 ) - 1, input.min( 1 ) - 1, input.min( 2 ) - 1 }, new long[] { input.max(
						0 ) + 1, input.max( 1 ) + 1, input.max( 2 ) + 1 } ) )
				.localizingCursor();

		// Timestamp end = new Timestamp( System.currentTimeMillis() );
//		System.out.println( "preparing RAI: " + ( end.getTime() - begin.getTime() ) );
		
		numberOfVoxels = 0;
		while ( c.hasNext() )
		{
			c.next();
			numberOfVoxels++;
		}
	}
	
	public void countingActiveVoxels( RandomAccessibleInterval< LabelMultisetType > input, int[] voxDim, int[] volDim, boolean isExact, int level )
	{
//		Timestamp begin = new Timestamp( System.currentTimeMillis() );

		ExtendedRandomAccessibleInterval< LabelMultisetType, RandomAccessibleInterval< LabelMultisetType > > extended =
				Views.extendValue( input, new LabelMultisetType() );
		Cursor< LabelMultisetType > c = Views.flatIterable( Views.interval( extended, new FinalInterval(
				new long[] { input
						.min( 0 ) - 1, input.min( 1 ) - 1, input.min( 2 ) - 1 }, new long[] { input.max(
								0 ) + 1, input.max( 1 ) + 1, input.max( 2 ) + 1 } ) ) )
				.localizingCursor();

		// Timestamp end = new Timestamp( System.currentTimeMillis() );
//		System.out.println( "preparing RAI: " + ( end.getTime() - begin.getTime() ) );
//		long[] volume = null;
		List<Long> volume = new ArrayList<Long>();

		
		numberOfActiveVoxels = 0;

		while ( c.hasNext() )
		{
			LabelMultisetType it = c.next();

			int cursorX = c.getIntPosition( 0 );
			int cursorY = c.getIntPosition( 1 );
			int cursorZ = c.getIntPosition( 2 );

			if ( cursorX == -1 || cursorY == -1 || cursorZ == -1 )
				continue;
			

			for ( final Multiset.Entry< Label > e : it.entrySet() )
			{
				volume.add( e.getElement().id());
			}
		}

		numberOfActiveVoxels = volume.size();
		
		int xWidth = ( volDim[0] + 1 );
		int xyWidth = xWidth * ( volDim[1] + 1 );
		double[] vertex_values = new double[ 8 ];
		
		for( int z = 0; z < volDim[2] ; z++)
			for( int y = 0; y < volDim[1] ; y++)
				for( int x = 0; x < volDim[0] ; x++)
				{
					vertex_values[0] = volume.get( z * xyWidth + y * xWidth + x );
					vertex_values[1] = volume.get( z * xyWidth + ( y + 1 ) * xWidth + x );
					vertex_values[2] = volume.get( z * xyWidth + ( y + 1 ) * xWidth + ( x + 1 ) );
					vertex_values[3] = volume.get( z * xyWidth + y * xWidth + ( x + 1 ) );
					vertex_values[4] = volume.get( ( z + 1 ) * xyWidth + y * xWidth + x );
					vertex_values[5] = volume.get( ( z + 1 ) * xyWidth + ( y + 1 ) * xWidth + x );
					vertex_values[6] = volume.get( ( z + 1 ) * xyWidth + ( y + 1 ) * xWidth + ( x + 1 ) );
					vertex_values[7] = volume.get( ( z + 1 ) * xyWidth + y * xWidth + ( x + 1 ) );
				}
	
	}
}

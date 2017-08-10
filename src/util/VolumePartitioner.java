package util;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bdv.labels.labelset.LabelMultisetType;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.view.Views;

/**
 * This class is responsible for create small subvolumes from a
 * RandomAccessibleInterval.
 * 
 * @author vleite
 *
 */
public class VolumePartitioner
{
	/** logger */
	private static final Logger LOGGER = LoggerFactory.getLogger( VolumePartitioner.class );

	/** number of voxels that must overlap between partitions */
	private int[] OVERLAP = { 1, 1, 1 };

	/** volume to be partitioned */
	private final RandomAccessibleInterval< LabelMultisetType > volumeLabels;

	/** Minimum size of each partition the partition */
	private final int[] partitionSize;

	/**
	 * dimension of the cube that will be used on the marching cubes algorithm
	 */
	private final int[] cubeSize;

	private static List< Chunk > chunks;

	/**
	 * Constructor - initialize parameters
	 */
	public VolumePartitioner( RandomAccessibleInterval< LabelMultisetType > volumeLabels, int[] partitionSize, int[] cubeSize )
	{
		this.volumeLabels = volumeLabels;
		this.partitionSize = partitionSize;
		this.cubeSize = cubeSize;
		this.OVERLAP = cubeSize;
		VolumePartitioner.chunks = new ArrayList< Chunk >();

		if ( LOGGER.isTraceEnabled() )
		{
			LOGGER.trace( "partition size: " + partitionSize[ 0 ] + " " + partitionSize[ 1 ] + " " + partitionSize[ 2 ] );
		}
	}

	/**
	 * Method to partitioning the data in small chunks.
	 * 
	 * @return list of chunks with its subvolume and offset created
	 */
	public List< Chunk > dataPartitioning()
	{
		for ( long bx = volumeLabels.min( 0 ); ( bx + partitionSize[ 0 ] ) <= volumeLabels.max( 0 ); bx += partitionSize[ 0 ] )
		{
			for ( long by = volumeLabels.min( 1 ); ( by + partitionSize[ 1 ] ) <= volumeLabels.max( 1 ); by += partitionSize[ 1 ] )
			{
				for ( long bz = volumeLabels.min( 2 ); ( bz + partitionSize[ 2 ] ) <= volumeLabels.max( 2 ); bz += partitionSize[ 2 ] )
				{
					long[] begin = new long[] { bx, by, bz };
					long[] end = new long[] { begin[ 0 ] + partitionSize[ 0 ],
							begin[ 1 ] + partitionSize[ 1 ],
							begin[ 2 ] + partitionSize[ 2 ] };

					if ( LOGGER.isTraceEnabled() )
					{
						LOGGER.trace( "begin: " + bx + " " + by + " " + bz );
						LOGGER.trace( "end: " + end[ 0 ] + " " + end[ 1 ] + " " + end[ 2 ] );
					}

					for ( int i = 0; i < begin.length; i++ )
					{
						if ( begin[ i ] - OVERLAP[ i ] >= 0 )
						{
							begin[ i ] -= OVERLAP[ i ];
						}

						if ( volumeLabels.max( i ) - end[ i ] < partitionSize[ i ] )
						{
							end[ i ] = volumeLabels.max( i );
						}
					}

					final Chunk chunk = new Chunk();
					chunk.setVolume( Views.interval( volumeLabels, begin, end ) );
					chunk.setOffset( new int[] { ( int ) ( begin[ 0 ] ), ( int ) ( begin[ 1 ] ), ( int ) ( begin[ 2 ] ) } );
					chunks.add( chunk );

					if ( LOGGER.isDebugEnabled() )
					{
						LOGGER.debug( "partition begins at: " + begin[ 0 ] + " " + begin[ 1 ] + " " + begin[ 2 ] );
						LOGGER.debug( "partition ends at: " + end[ 0 ] + " " + end[ 1 ] + " " + end[ 2 ] );
					}
				}
			}
		}
		return chunks;
	}

	/**
	 * Given a specific position, return the chunk were this position is. If the
	 * chunk already exists, just return the chunk. If not, create a chunk for
	 * the informed position.
	 * 
	 * @param position
	 *            x, y, z coordinates
	 * @return chunk were this position belongs to.
	 */
	public Chunk getChunk( int[] position )
	{
		for ( int i = 0; i < chunks.size(); i++ )
		{
			if ( chunks.get( i ).contains( position ) ) { return chunks.get( i ); }
		}

		long[] offset = getVolumeOffset( position );

		int xWidth = ( int ) volumeLabels.dimension( 0 );
		int xyWidth = ( int ) ( xWidth * volumeLabels.dimension( 1 ) );
		int index = ( int ) ( offset[ 0 ] + offset[ 1 ] * xWidth + offset[ 2 ] * xyWidth );

		long[] begin = new long[] { offset[ 0 ] * partitionSize[ 0 ],
				offset[ 1 ] * partitionSize[ 1 ],
				offset[ 2 ] * partitionSize[ 2 ] };
		long[] end = new long[] { begin[ 0 ] + partitionSize[ 0 ],
				begin[ 1 ] + partitionSize[ 1 ],
				begin[ 2 ] + partitionSize[ 2 ] };

		for ( int i = 0; i < begin.length; i++ )
		{
			if ( begin[ i ] - OVERLAP[ i ] >= 0 )
			{
				begin[ i ] -= OVERLAP[ i ];
			}

			if ( volumeLabels.max( i ) - end[ i ] < partitionSize[ i ] )
			{
				end[ i ] = volumeLabels.max( i );
			}
		}

		final Chunk chunk = new Chunk();
		chunk.setVolume( Views.interval( volumeLabels, begin, end ) );
		chunk.setOffset( new int[] { ( int ) ( begin[ 0 ] ), ( int ) ( begin[ 1 ] ), ( int ) ( ( begin[ 2 ] ) ) } );
		chunk.setIndex( index );
		chunks.add( chunk );

		if ( LOGGER.isDebugEnabled() )
		{
			LOGGER.debug( "partition begins at: " + begin[ 0 ] + " " + begin[ 1 ] + " " + begin[ 2 ] );
			LOGGER.debug( "partition ends at: " + end[ 0 ] + " " + end[ 1 ] + " " + end[ 2 ] );
		}

		return chunk;
	}

	public long[] getVolumeOffset( int[] position )
	{
		long[] offset = new long[ 3 ];
		offset[ 0 ] = ( position[ 0 ] / partitionSize[ 0 ] );
		offset[ 1 ] = ( position[ 1 ] / partitionSize[ 1 ] );
		offset[ 2 ] = ( position[ 2 ] / partitionSize[ 2 ] );

		for ( int i = 0; i < offset.length; i++ )
		{
			if ( position[ i ] % partitionSize[ i ] == 0 )
			{
				if ( offset[ i ] > 0 )
					offset[ i ]--;
			}

			if ( offset[ i ] >= ( volumeLabels.dimension( i ) / partitionSize[ i ] ) )
			{
				offset[ i ]--;
			}
		}

		LOGGER.info( "volume offset: " + offset[ 0 ] + " " + offset[ 1 ] + " " + offset[ 2 ] );
		return offset;
	}
}

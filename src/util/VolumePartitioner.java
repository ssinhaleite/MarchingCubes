package util;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bdv.labels.labelset.LabelMultisetType;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.view.Views;

/**
 * This class is responsible for create small subvolumes from a
 * RandomAccessibleInterval. Given the data, the size of each partition and the
 * size of the cube (from marching cubes algorithm), this class return a vector
 * with subvolumes and the offsets of each subvolume.
 * 
 * @author vleite
 *
 */
public class VolumePartitioner
{
	/** logger */
	private static final Logger LOGGER = LoggerFactory.getLogger( VolumePartitioner.class );

	/** number of voxels that must overlap between partitions */
	private static final long OVERLAP = 1;

	/** volume to be partitioned */
	private final RandomAccessibleInterval< LabelMultisetType > volumeLabels;

	/** Size of the partition on x axis */
	private final int partitionXSize;

	/** Size of the partition on y axis */
	private final int partitionYSize;

	/** Size of the partition on z axis */
	private final int partitionZSize;

	/**
	 * dimension of the cube that will be used on the marching cubes algorithm
	 */
	private final float[] voxDim;

	/**
	 * Constructor - initialize parameters
	 */
	public VolumePartitioner( RandomAccessibleInterval< LabelMultisetType > volumeLabels, int[] partitionSize, float[] voxDim )
	{
		this.volumeLabels = volumeLabels;
		this.partitionXSize = partitionSize[ 0 ];
		this.partitionYSize = partitionSize[ 1 ];
		this.partitionZSize = partitionSize[ 2 ];
		this.voxDim = voxDim;

		if ( LOGGER.isTraceEnabled() )
		{
			LOGGER.trace( "partition defined as: " + partitionXSize + " " + partitionYSize + " " + partitionZSize );
		}
	}

	/**
	 * Method to partitioning the data in small chunks.
	 * 
	 * @param subvolumes
	 *            list of each subvolume created
	 * @param offsets
	 *            the offset of each subvolume
	 */
	public void dataPartitioning( List< RandomAccessibleInterval< LabelMultisetType > > subvolumes, List< int[] > offsets )
	{
		for ( long bx = volumeLabels.min( 0 ); ( bx + partitionXSize ) <= volumeLabels.max( 0 ); bx += partitionXSize )
		{
			for ( long by = volumeLabels.min( 1 ); ( by + partitionYSize ) <= volumeLabels.max( 1 ); by += partitionYSize )
			{
				for ( long bz = volumeLabels.min( 2 ); ( bz + partitionZSize ) <= volumeLabels.max( 2 ); bz += partitionZSize )
				{
					long[] begin = new long[] { bx, by, bz };
					long[] end = new long[] { begin[ 0 ] + partitionXSize,
							begin[ 1 ] + partitionYSize,
							begin[ 2 ] + partitionZSize };

					if (LOGGER.isTraceEnabled())
					{
						LOGGER.trace( "begin: " + bx + " " + by + " " + bz );
						LOGGER.trace( "end: " + end[ 0 ] + " " + end[ 1 ] + " " + end[ 2 ] );
					}

					if ( begin[ 0 ] - OVERLAP >= 0 )
					{
						begin[ 0 ] -= OVERLAP;
					}

					if ( begin[ 1 ] - OVERLAP >= 0 )
					{
						begin[ 1 ] -= OVERLAP;
					}

					if ( begin[ 2 ] - OVERLAP >= 0 )
					{
						begin[ 2 ] -= OVERLAP;
					}

					if ( volumeLabels.max( 0 ) - end[ 0 ] < partitionXSize )
					{
						end[ 0 ] = volumeLabels.max( 0 );
					}

					if ( volumeLabels.max( 1 ) - end[ 1 ] < partitionYSize )
					{
						end[ 1 ] = volumeLabels.max( 1 );
					}

					if ( volumeLabels.max( 2 ) - end[ 2 ] < partitionZSize )
					{
						end[ 2 ] = volumeLabels.max( 2 );
					}

					final RandomAccessibleInterval< LabelMultisetType > partition = Views.interval( volumeLabels, begin, end );

					offsets.add( new int[] { ( int ) ( begin[ 0 ] / voxDim[ 0 ] ), ( int ) ( begin[ 1 ] / voxDim[ 1 ] ), ( int ) ( begin[ 2 ] / voxDim[ 2 ] ) } );
					if ( LOGGER.isDebugEnabled() )
					{
						LOGGER.debug( "partition begins at: " + begin[ 0 ] + " " + begin[ 1 ] + " " + begin[ 2 ] );
						LOGGER.debug( "partition ends at: " + end[ 0 ] + " " + end[ 1 ] + " " + end[ 2 ] );
					}

					subvolumes.add( partition );
				}
			}
		}
	}
}

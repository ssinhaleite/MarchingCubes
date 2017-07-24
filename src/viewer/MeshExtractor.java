package viewer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bdv.labels.labelset.LabelMultisetType;
import net.imglib2.RandomAccessibleInterval;
import util.Chunk;

/**
 * This class is responsible for generate region of interests (map of
 * boundaries). Will call the VolumePartitioner must have a callNext method (use
 * chunk index based on x, y and z)
 * 
 * @author vleite
 *
 */
public class MeshExtractor
{
	/** logger */
	static final Logger LOGGER = LoggerFactory.getLogger( MeshExtractor.class );

	private RandomAccessibleInterval< LabelMultisetType > volumeLabels;

	Map< Integer, Chunk > chunkMap = new HashMap< Integer, Chunk >();

	int[] initialPosition;

	int[] partitionSize;

	int numberOfPartitions = 0;

	private int[] cubeSize;;

	private int foregroundValue;

	private marchingCubes.MarchingCubes.ForegroundCriterion criterion;

	public MeshExtractor( RandomAccessibleInterval< LabelMultisetType > volumeLabels, final int[] cubeSize, final int foregroundValue, final marchingCubes.MarchingCubes.ForegroundCriterion criterion )
	{
		this.volumeLabels = volumeLabels;
		this.partitionSize = new int[] { 1, 1, 1 };
		this.cubeSize = cubeSize;
		this.foregroundValue = foregroundValue;
		this.criterion = criterion;

		for ( int i = 0; i < volumeLabels.numDimensions(); i++ )
		{
			this.initialPosition[ i ] = ( int ) volumeLabels.min( i );
		}

		generatePartitionSize();

		createChunks();
	}

	private void createChunks()
	{
		List< Chunk > chunks = new ArrayList< Chunk >();

		CompletionService< viewer.Mesh > executor = null;
		List< Future< viewer.Mesh > > resultMeshList = null;

		util.VolumePartitioner partitioner = new util.VolumePartitioner( volumeLabels, partitionSize, cubeSize );
		chunks = partitioner.dataPartitioning();

		LOGGER.info( "starting executor..." );
		executor = new ExecutorCompletionService< viewer.Mesh >(
				Executors.newWorkStealingPool() );

		resultMeshList = new ArrayList<>();

		if ( LOGGER.isDebugEnabled() )
		{
			LOGGER.debug( "creating callables for " + chunks.size() + " partitions..." );
		}

		for ( int i = 0; i < chunks.size(); i++ )
		{
			int[] subvolDim = new int[] { ( int ) chunks.get( i ).getVolume().dimension( 0 ), ( int ) chunks.get( i ).getVolume().dimension( 1 ),
					( int ) chunks.get( i ).getVolume().dimension( 2 ) };

			MarchingCubesCallable callable = new MarchingCubesCallable( chunks.get( i ).getVolume(), subvolDim, chunks.get( i ).getOffset(), cubeSize, criterion, foregroundValue,
					true );

			if ( LOGGER.isDebugEnabled() )
			{
				LOGGER.debug( "dimension: " + chunks.get( i ).getVolume().dimension( 0 ) + "x" + chunks.get( i ).getVolume().dimension( 1 )
						+ "x" + chunks.get( i ).getVolume().dimension( 2 ) );
				LOGGER.debug( "offset: " + chunks.get( i ).getOffset()[ 0 ] + " " + chunks.get( i ).getOffset()[ 1 ] + " " + chunks.get( i ).getOffset()[ 2 ] );
				LOGGER.debug( "callable: " + callable );
			}

			Future< viewer.Mesh > result = executor.submit( callable );
			resultMeshList.add( result );
		}

		Future< viewer.Mesh > completedFuture = null;
		LOGGER.info( "waiting results..." );

		while ( resultMeshList.size() > 0 )
		{
			// block until a task completes
			try
			{
				completedFuture = executor.take();
				if ( LOGGER.isTraceEnabled() )
				{
					LOGGER.trace( "task " + completedFuture + " is ready: " + completedFuture.isDone() );
				}
			}
			catch ( InterruptedException e )
			{
				// TODO Auto-generated catch block
				LOGGER.error( " task interrupted: " + e.getCause() );
			}

			resultMeshList.remove( completedFuture );
			viewer.Mesh m = new viewer.Mesh();

			// get the mesh, if the task was able to create it
			try
			{
				m = completedFuture.get();
				LOGGER.info( "getting mesh" );
			}
			catch ( InterruptedException | ExecutionException e )
			{
				LOGGER.error( "Mesh creation failed: " + e.getCause() );
				break;
			}

			// a mesh was created, so update the existing mesh
			if ( m.getNumberOfVertices() > 0 )
			{
				LOGGER.info( "updating mesh..." );
				if ( LOGGER.isDebugEnabled() )
				{
					LOGGER.debug( "size of mesh " + m.getNumberOfVertices() );
				}

				// TODO: need to call the viewer to update the mesh
//				updateMesh( m, neuron, false );
//				neuron.setVertices( FloatBuffer.wrap( verticesArray ) );
//				neuron.recalculateNormals();
//				neuron.setDirty( true );
			}
		}
	}

	public boolean hasNext()
	{
		if ( numberOfPartitions == 0 )
		{
			return false;
		}

		return true;
	}

	public viewer.Mesh next()
	{
		numberOfPartitions--;

		Chunk chunk = new Chunk();

		for ( int i = 0; i < volumeLabels.numDimensions(); i++ )
		{
			initialPosition[ i ] += partitionSize[ i ];
		}

		CompletionService< viewer.Mesh > executor = null;
		List< Future< viewer.Mesh > > resultMeshList = null;

		util.VolumePartitioner partitioner = new util.VolumePartitioner( volumeLabels, partitionSize, cubeSize );
		chunk = partitioner.getChunk( initialPosition );
		
		chunkMap.put( numberOfPartitions, chunk );

		LOGGER.info( "starting executor..." );
		executor = new ExecutorCompletionService< viewer.Mesh >(
				Executors.newWorkStealingPool() );

		return new viewer.Mesh();
	}

	private void generatePartitionSize()
	{
		int numberOfCellsX = ( int ) ( ( volumeLabels.max( 0 ) - volumeLabels.min( 0 ) ) + 1 ) / 32;
		int numberOfCellsY = ( int ) ( ( volumeLabels.max( 1 ) - volumeLabels.min( 1 ) ) + 1 ) / 32;
		int numberOfCellsZ = ( int ) ( ( volumeLabels.max( 2 ) - volumeLabels.min( 2 ) ) + 1 ) / 32;

		LOGGER.trace( "division: " + numberOfCellsX + " " + numberOfCellsY + " " + numberOfCellsZ );

		numberOfCellsX = numberOfCellsX >= 7 ? 7 * 32 : numberOfCellsX * 32;
		numberOfCellsY = numberOfCellsY >= 7 ? 7 * 32 : numberOfCellsY * 32;
		numberOfCellsZ = numberOfCellsZ >= 7 ? 7 * 32 : numberOfCellsZ * 32;

		LOGGER.trace( "partition size 1: " + numberOfCellsX + " " + numberOfCellsY + " " + numberOfCellsZ );

		numberOfCellsX = ( numberOfCellsX == 0 ) ? 1 : numberOfCellsX;
		numberOfCellsY = ( numberOfCellsY == 0 ) ? 1 : numberOfCellsY;
		numberOfCellsZ = ( numberOfCellsZ == 0 ) ? 1 : numberOfCellsZ;

		LOGGER.trace( "zero verification: " + numberOfCellsX + " " + numberOfCellsY + " " + numberOfCellsZ );

		partitionSize = new int[] { numberOfCellsX, numberOfCellsY, numberOfCellsZ };
		LOGGER.trace( "final partition size: " + numberOfCellsX + " " + numberOfCellsY + " " + numberOfCellsZ );

		numberOfPartitions = ( int ) ( ( ( volumeLabels.max( 0 ) - volumeLabels.min( 0 ) ) + 1 ) / numberOfCellsX ) *
				( int ) ( ( ( ( volumeLabels.max( 1 ) - volumeLabels.min( 1 ) ) + 1 ) / numberOfCellsY ) ) *
				( int ) ( ( ( ( volumeLabels.max( 2 ) - volumeLabels.min( 2 ) ) + 1 ) / numberOfCellsZ ) - 1 );
	}
}

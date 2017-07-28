package viewer;

import java.nio.FloatBuffer;
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
import graphics.scenery.Mesh;
import net.imglib2.RandomAccessibleInterval;
import util.Chunk;

/**
 * This class is responsible for generate region of interests (map of
 * boundaries).
 * 
 * Will call the VolumePartitioner must have a callNext method (use chunk index
 * based on x, y and z)
 * 
 * @author vleite
 *
 */
public class MeshExtractor
{
	/** logger */
	static final Logger LOGGER = LoggerFactory.getLogger( MeshExtractor.class );

	static final int MAX_CUBE_SIZE = 32;

	private RandomAccessibleInterval< LabelMultisetType > volumeLabels;

	int[] initialPosition;

	int[] partitionSize;

	private int[] cubeSize;

	private int foregroundValue;

	private marchingCubes.MarchingCubes.ForegroundCriterion criterion;

	private static Map< Future< viewer.Mesh >, Chunk > resultMeshMap = null;

	private static List< Future< Chunk > > resultChunkList = null;

	private static CompletionService< viewer.Mesh > executor = null;

	private static CompletionService< Chunk > chunkExecutor = null;

	private static util.VolumePartitioner partitioner;

	private static List< int[] > positionsExtracted;

	public MeshExtractor( RandomAccessibleInterval< LabelMultisetType > volumeLabels, final int[] cubeSize, final int foregroundValue, final marchingCubes.MarchingCubes.ForegroundCriterion criterion )
	{
		this.volumeLabels = volumeLabels;
		this.partitionSize = new int[] { 1, 1, 1 };
		this.cubeSize = cubeSize;
		this.foregroundValue = foregroundValue;
		this.criterion = criterion;

		executor = new ExecutorCompletionService< viewer.Mesh >(
				Executors.newWorkStealingPool() );

		chunkExecutor = new ExecutorCompletionService< Chunk >(
				Executors.newWorkStealingPool() );

		resultMeshMap = new HashMap< Future< viewer.Mesh >, Chunk >();

		generatePartitionSize();

		partitioner = new util.VolumePartitioner( this.volumeLabels, partitionSize, this.cubeSize );
		
		positionsExtracted = new ArrayList< int[] >();
	}

	public boolean hasNext()
	{
		System.out.println( "hasNext - there is/are " + resultMeshMap.size() + " threads to calculate the mesh" );
		return resultMeshMap.size() > 0;
	}

	public Mesh next()
	{
		Future< viewer.Mesh > completedFuture = null;
		// block until any task completes
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

		Chunk chunk = resultMeshMap.remove( completedFuture );
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
		}
		
		Mesh sceneryMesh = new Mesh();
		updateMesh( m, sceneryMesh );

		chunk.setMesh( sceneryMesh, cubeSize );

		// if one of the neighbors positions exist, creates a new callable for
		// them
		Thread thread1 = new Thread( new Runnable()
		{
			public void run()
			{
				// x + partitionSizeX, y, z
				int[] newPosition = new int[] { initialPosition[ 0 ] + partitionSize[ 0 ], initialPosition[ 1 ], initialPosition[ 2 ] };
				if ( initialPosition[ 0 ] + partitionSize[ 0 ] < volumeLabels.dimension( 0 ) )
					createChunks( newPosition );
			}
		} );
		thread1.start();

		Thread thread2 = new Thread( new Runnable()
		{
			public void run()
			{
				// x - partitionSizeX, y, z
				int[] newPosition = new int[] { initialPosition[ 0 ] - partitionSize[ 0 ], initialPosition[ 1 ], initialPosition[ 2 ] };
				if ( initialPosition[ 0 ] - partitionSize[ 0 ] > volumeLabels.min( 0 ) )
					createChunks( newPosition );
			}
		} );
		thread2.start();

		Thread thread3 = new Thread( new Runnable()
		{
			public void run()
			{
				// x, y + partitionSizeY, z
				int[] newPosition = new int[] { initialPosition[ 0 ], initialPosition[ 1 ] + partitionSize[ 1 ], initialPosition[ 2 ] };
				if ( initialPosition[ 1 ] + partitionSize[ 1 ] < volumeLabels.dimension( 1 ) )
					createChunks( newPosition );
			}
		} );
		thread3.start();

		Thread thread4 = new Thread( new Runnable()
		{
			public void run()
			{
				// x, y - partitionSizeY, z
				int[] newPosition = new int[] { initialPosition[ 0 ], initialPosition[ 1 ] - partitionSize[ 1 ], initialPosition[ 2 ] };
				if ( initialPosition[ 1 ] - partitionSize[ 1 ] > volumeLabels.min( 1 ) )
					createChunks( newPosition );
			}
		} );
		thread4.start();

		Thread thread5 = new Thread( new Runnable()
		{
			public void run()
			{
				// x, y, z + partitionSizeZ
				int[] newPosition = new int[] { initialPosition[ 0 ], initialPosition[ 1 ], initialPosition[ 2 ] + partitionSize[ 2 ] };
				if ( initialPosition[ 2 ] + partitionSize[ 2 ] < volumeLabels.dimension( 2 ) )
					createChunks( newPosition );
			}
		} );
		thread5.start();

		Thread thread6 = new Thread( new Runnable()
		{
			public void run()
			{
				// x, y, z - partitionSizeZ
				int[] newPosition = new int[] { initialPosition[ 0 ], initialPosition[ 1 ], initialPosition[ 2 ] - partitionSize[ 2 ] };
				if ( initialPosition[ 2 ] - partitionSize[ 2 ] > volumeLabels.min( 2 ) )
					createChunks( newPosition );
			}
		} );
		thread6.start();

		// a mesh was created, return it
		return sceneryMesh;
	}

	public void createChunks( int[] position )
	{
		initialPosition = position;

		System.out.println( "there is/are " + resultMeshMap.size() + " threads to calculate the mesh" );
		// creates the callable for the chunk in the given position
		System.out.println( "createChunks - " + position[ 0 ] + " " + position[ 1 ] + " " + position[ 2 ] );

		for ( int i = 0; i < positionsExtracted.size(); i++ )
		{
			int[] test = positionsExtracted.get( i );
			if ( test[ 0 ] == position[ 0 ] && test[ 1 ] == position[ 1 ] && test[ 2 ] == position[ 2 ] )
				return;
		}

		positionsExtracted.add( position );

		System.out.println( "adding in the set: " + position[ 0 ] + " " + position[ 1 ] + " " + position[ 2 ] );
		
		Chunk chunk = partitioner.getChunk( position );
		createCallable( chunk );
	}

	private void createCallable( Chunk chunk )
	{
		// if the chunk is already in the callable, do not add it again
		if ( resultMeshMap.containsValue( chunk ) ) { return; }

		int[] volumeDimension = new int[] { ( int ) chunk.getVolume().dimension( 0 ), ( int ) chunk.getVolume().dimension( 1 ),
				( int ) chunk.getVolume().dimension( 2 ) };

		MarchingCubesCallable callable = new MarchingCubesCallable( chunk.getVolume(), volumeDimension, chunk.getOffset(), cubeSize, criterion, foregroundValue,
				true );

		if ( LOGGER.isDebugEnabled() )
		{
			LOGGER.debug( "dimension: " + volumeDimension[ 0 ] + "x" + volumeDimension[ 1 ] + "x" + volumeDimension[ 2 ] );
			LOGGER.debug( "offset: " + chunk.getOffset()[ 0 ] + " " + chunk.getOffset()[ 1 ] + " " + chunk.getOffset()[ 2 ] );
		}

		Future< viewer.Mesh > result = executor.submit( callable );
		resultMeshMap.put( result, chunk );
	}

	private void generatePartitionSize()
	{
		for ( int i = 0; i < partitionSize.length; i++ )
		{
			partitionSize[ i ] = ( int ) ( ( volumeLabels.max( i ) - volumeLabels.min( i ) ) + 1 ) / MAX_CUBE_SIZE;
		}

		LOGGER.trace( "division: {}, {}, {}", partitionSize[ 0 ], partitionSize[ 1 ], partitionSize[ 2 ] );

		for ( int i = 0; i < partitionSize.length; i++ )
		{
			partitionSize[ i ] = partitionSize[ i ] >= 7 ? 7 * MAX_CUBE_SIZE : partitionSize[ i ] * MAX_CUBE_SIZE;
		}

		LOGGER.trace( "partition size: {}, {}, {}", partitionSize[ 0 ], partitionSize[ 1 ], partitionSize[ 2 ] );

		for ( int i = 0; i < partitionSize.length; i++ )
		{
			partitionSize[ i ] = ( partitionSize[ i ] == 0 ) ? 1 : partitionSize[ i ];
		}

		LOGGER.trace( "final partition size: {}, {}, {}", partitionSize[ 0 ], partitionSize[ 1 ], partitionSize[ 2 ] );
	}

	/**
	 * this method convert the viewer mesh into the scenery mesh
	 * 
	 * @param mesh
	 *            mesh information to be converted in a mesh for scenery
	 * @param sceneryMesh
	 *            scenery mesh that will receive the information
	 */
	public void updateMesh( viewer.Mesh mesh, Mesh sceneryMesh )
	{
		float[] verticesArray = new float[ mesh.getNumberOfVertices() * 3 ];

		float[][] vertices = mesh.getVertices();
		int v = 0;
		for ( int i = 0; i < mesh.getNumberOfVertices(); i++ )
		{
			verticesArray[ v++ ] = vertices[ i ][ 0 ];
			verticesArray[ v++ ] = vertices[ i ][ 1 ];
			verticesArray[ v++ ] = vertices[ i ][ 2 ];
		}

		final float maxX = volumeLabels.dimension( 0 ) - 1;
		final float maxY = volumeLabels.dimension( 1 ) - 1;
		final float maxZ = volumeLabels.dimension( 2 ) - 1;

		float maxAxisVal = Math.max( maxX, Math.max( maxY, maxZ ) );
		// omp parallel for
		for ( int i = 0; i < verticesArray.length; i++ )
		{
			verticesArray[ i ] /= maxAxisVal;
		}

		sceneryMesh.setVertices( FloatBuffer.wrap( verticesArray ) );
	}
}

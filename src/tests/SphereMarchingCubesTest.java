package tests;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import application.MarchingCubesApplication;
import bdv.img.h5.H5LabelMultisetSetupImageLoader;
import bdv.labels.labelset.LabelMultisetType;
import ch.systemsx.cisd.hdf5.HDF5Factory;
import ch.systemsx.cisd.hdf5.IHDF5Reader;
import cleargl.GLVector;
import graphics.scenery.Material;
import graphics.scenery.Mesh;
import marchingCubes.MarchingCubes;
import net.imglib2.RandomAccessibleInterval;
import util.HDF5Reader;
import util.MeshExtractor;

/**
 * This test creates an sphere and then generates the mesh using the Marching
 * Cubes implementation
 * 
 * @author vleite
 *
 */
public class SphereMarchingCubesTest
{
	/** logger */
	static Logger LOGGER;

	private static RandomAccessibleInterval< LabelMultisetType > volumeLabels = null;

	public static void main( String[] args )
	{
		System.setProperty( org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "info" );
		LOGGER = LoggerFactory.getLogger( SphereMarchingCubesTest.class );

		loadSphere();

		final MarchingCubesApplication sceneryApplication = new MarchingCubesApplication( "Marching cubes", 800, 600 );
		sceneryApplication.setVolumeResolution( new float[] { 0.5f, 0.75f, 0.5f } );
		new Thread( () -> {
			sceneryApplication.main();
		} ).start();

		new Thread( () -> {
			MeshExtractor meshExtractor = new MeshExtractor( volumeLabels, new int[] { 32, 32, 32 }, 1, MarchingCubes.ForegroundCriterion.EQUAL );

			for ( int voxelSize = 32; voxelSize > 0; voxelSize /= 2 )
			{
				LOGGER.info( "voxel size: " + voxelSize );

				Mesh completeNeuron = new Mesh();
				final Material material = new Material();
				material.setAmbient( new GLVector( 1f, 0.0f, 1f ) );
				material.setSpecular( new GLVector( 1f, 0.0f, 1f ) );

				if ( voxelSize == 32 )
				{
					material.setDiffuse( new GLVector( 1, 0, 0 ) );
				}
				if ( voxelSize == 16 )
				{
					material.setDiffuse( new GLVector( 0, 1, 0 ) );
				}
				if ( voxelSize == 8 )
				{
					material.setDiffuse( new GLVector( 0, 0, 1 ) );
				}
				if ( voxelSize == 4 )
				{
					material.setDiffuse( new GLVector( 1, 0, 1 ) );
				}
				if ( voxelSize == 2 )
				{
					material.setDiffuse( new GLVector( 0, 1, 1 ) );
				}
				if ( voxelSize == 1 )
				{
					material.setDiffuse( new GLVector( 1, 1, 0 ) );
				}

				completeNeuron.setMaterial( material );
				completeNeuron.setName( String.valueOf( 1 + " " + voxelSize ) );
				completeNeuron.setPosition( new GLVector( 0.0f, 0.0f, 0.0f ) );
				completeNeuron.setScale( new GLVector( 0.5f, 0.75f, 0.5f ) );
				sceneryApplication.addChild( completeNeuron );

				meshExtractor.setCubeSize( new int[] { voxelSize, voxelSize, 1 } );
				int[] position = new int[] { 0, 0, 0 };
				meshExtractor.createChunks( position );

				float[] completeNeuronVertices = new float[ 0 ];
				float[] verticesArray = new float[ 0 ];
				int completeMeshSize = 0;

				while ( meshExtractor.hasNext() )
				{
					Mesh neuron = new Mesh();
					neuron = meshExtractor.next();

					if ( completeNeuron.getVertices().hasArray() )
					{
						completeNeuronVertices = completeNeuron.getVertices().array();
						completeMeshSize = completeNeuronVertices.length;
					}

					float[] neuronVertices = neuron.getVertices().array();
					int meshSize = neuronVertices.length;
					verticesArray = Arrays.copyOf( completeNeuronVertices, completeMeshSize + meshSize );
					System.arraycopy( neuronVertices, 0, verticesArray, completeMeshSize, meshSize );

					System.out.println( "number of elements complete mesh: " + verticesArray.length );
					completeNeuron.setVertices( FloatBuffer.wrap( verticesArray ) );
					completeNeuron.recalculateNormals();
					completeNeuron.setDirty( true );
				}

				LOGGER.info( "all results generated!" );

				// Pause for 2 seconds
				try
				{
					Thread.sleep( 2000 );
				}
				catch ( InterruptedException e )
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if ( voxelSize != 1 )
					sceneryApplication.removeChild( completeNeuron );
			}
		} ).start();
	}

	private static void loadSphere()
	{
		final IHDF5Reader reader = HDF5Factory.openForReading( "resources/sphere500.hdf" );
		final String pathLabel = "/volumes/labels/sphere";
		/** loaded segments */
		ArrayList< H5LabelMultisetSetupImageLoader > labels = null;
		/* labels */
		if ( reader.exists( pathLabel ) )
		{
			try
			{
				labels = HDF5Reader.readLabels( reader, pathLabel );
			}
			catch ( IOException e )
			{
				LOGGER.error( "read labels failed: " + e.getCause() );
			}
		}
		else
		{
			LOGGER.error( "no label dataset '" + pathLabel + "' found" );
		}

		volumeLabels = labels.get( 0 ).getImage( 0 );
	}
}

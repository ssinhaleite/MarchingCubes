package viewer;

import java.nio.FloatBuffer;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bdv.labels.labelset.LabelMultisetType;
import cleargl.GLVector;
import graphics.scenery.Box;
import graphics.scenery.Camera;
import graphics.scenery.DetachedHeadCamera;
import graphics.scenery.Material;
import graphics.scenery.Mesh;
import graphics.scenery.PointLight;
import graphics.scenery.Scene;
import graphics.scenery.SceneryDefaultApplication;
import graphics.scenery.SceneryElement;
import graphics.scenery.backends.Renderer;
import net.imglib2.RandomAccessibleInterval;
import util.MeshExtractor;

/**
 * Class responsible for create the world/scene
 * 
 * @author vleite
 *
 */
public class MarchingCubesSceneryApplication extends SceneryDefaultApplication
{
	/** logger */
	static final Logger LOGGER = LoggerFactory.getLogger( MarchingCubesSceneryApplication.class );

	private static marchingCubes.MarchingCubes.ForegroundCriterion criterion = marchingCubes.MarchingCubes.ForegroundCriterion.EQUAL;

	private static int[] cubeSize = { 4, 4, 4 };

	private int foregroundValue;

	private RandomAccessibleInterval< LabelMultisetType > volumeLabels = null;

	private static float[] verticesArray = new float[ 0 ];

	public MarchingCubesSceneryApplication( String applicationName, int windowWidth, int windowHeight )
	{
		super( applicationName, windowWidth, windowHeight, false );
	}

	public void setForegroundValue( int foregroundValue )
	{
		this.foregroundValue = foregroundValue;
	}

	public void setVolumeLabels( RandomAccessibleInterval< LabelMultisetType > volumeLabels )
	{
		this.volumeLabels = volumeLabels;
	}

	@Override
	public void init()
	{
		LOGGER.info( "starting application..." );

		setRenderer( Renderer.Factory.createRenderer( getHub(), getApplicationName(), getScene(), getWindowWidth(),
				getWindowHeight() ) );
		getHub().add( SceneryElement.Renderer, getRenderer() );

		final Box hull = new Box( new GLVector( 50.0f, 50.0f, 50.0f ), true );
		hull.getMaterial().setDiffuse( new GLVector( 0.5f, 0.5f, 0.5f ) );
		hull.getMaterial().setDoubleSided( true );
		getScene().addChild( hull );

		final Camera cam = new DetachedHeadCamera();

		cam.perspectiveCamera( 50f, getWindowHeight(), getWindowWidth(), 0.001f, 1000.0f );
		cam.setActive( true );
		cam.setPosition( new GLVector( 2f, 2f, 10 ) );
		getScene().addChild( cam );

		PointLight[] lights = new PointLight[ 4 ];

		for ( int i = 0; i < lights.length; i++ )
		{
			lights[ i ] = new PointLight();
			lights[ i ].setEmissionColor( new GLVector( 1.0f, 1.0f, 1.0f ) );
			lights[ i ].setIntensity( 100.2f * 5 );
			lights[ i ].setLinear( 0.0f );
			lights[ i ].setQuadratic( 0.1f );
		}

		lights[ 0 ].setPosition( new GLVector( 1.0f, 0f, -1.0f / ( float ) Math.sqrt( 2.0 ) ) );
		lights[ 1 ].setPosition( new GLVector( -1.0f, 0f, -1.0f / ( float ) Math.sqrt( 2.0 ) ) );
		lights[ 2 ].setPosition( new GLVector( 0.0f, 1.0f, 1.0f / ( float ) Math.sqrt( 2.0 ) ) );
		lights[ 3 ].setPosition( new GLVector( 0.0f, -1.0f, 1.0f / ( float ) Math.sqrt( 2.0 ) ) );

		for ( int i = 0; i < lights.length; i++ )
		{
			getScene().addChild( lights[ i ] );
		}

		new Thread()
		{
			public void run()
			{
				marchingCube( foregroundValue, getScene() );
			}
		}.start();
	}

	private void marchingCube( int foregroundValue, Scene scene )
	{
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
			completeNeuron.setName( String.valueOf( foregroundValue + " " + voxelSize ) );
			completeNeuron.setPosition( new GLVector( 0.0f, 0.0f, 0.0f ) );
			completeNeuron.setScale( new GLVector( 4.0f, 4.0f, 40.0f ) );
			scene.addChild( completeNeuron );

			cubeSize[ 0 ] = voxelSize;
			cubeSize[ 1 ] = voxelSize;
			cubeSize[ 2 ] = 1;

			MeshExtractor meshExtractor = new MeshExtractor( volumeLabels, cubeSize, foregroundValue, criterion );
			int[] position = new int[] { 0, 0, 0 };
			meshExtractor.createChunks( position );

			float[] completeNeuronVertices = new float[ 0 ];
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
				scene.removeChild( completeNeuron );
		}
	}
}
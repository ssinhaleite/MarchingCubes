package viewer;

import java.nio.FloatBuffer;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cleargl.GLVector;
import graphics.scenery.Camera;
import graphics.scenery.Material;
import graphics.scenery.Mesh;
import graphics.scenery.Scene;

public class MeshView
{
	/** logger */
	private static final Logger LOGGER = LoggerFactory.getLogger( MeshView.class );

	private static int[] cubeSize;

	private Mesh completeNeuron = new Mesh();

	private int foregroundValue;

	private Scene scene;

	private static float[] verticesArray = new float[ 0 ];

	float[] completeNeuronVertices = new float[ 0 ];

	int completeMeshSize = 0;

	private Material material;

	public MeshView( int foregroundValue, Scene scene )
	{
		cubeSize = new int[] { 4, 4, 4 };
		this.foregroundValue = foregroundValue;
		this.scene = scene;

		material = new Material();
		material.setAmbient( new GLVector( 1f, 0.0f, 1f ) );
		material.setSpecular( new GLVector( 1f, 0.0f, 1f ) );
		material.setDiffuse( new GLVector( 1, 0, 0 ) );

		completeNeuron.setMaterial( material );
//		completeNeuron.setName( String.valueOf( foregroundValue + " " + voxelSize ) );
		completeNeuron.setPosition( new GLVector( 0.0f, 0.0f, 0.0f ) );
		completeNeuron.setScale( new GLVector( 4.0f, 4.0f, 40.0f ) );
//		completeNeuron.setGeometryType( GeometryType.POINTS );
	}

	public void render( Mesh mesh, int[] cubeSize )
	{

		if ( cubeSize[ 0 ] == 32 )
		{
			material.setDiffuse( new GLVector( 1, 0, 0 ) );
		}
		if ( cubeSize[ 0 ] == 16 )
		{
			material.setDiffuse( new GLVector( 0, 1, 0 ) );
		}
		if ( cubeSize[ 0 ] == 8 )
		{
			material.setDiffuse( new GLVector( 0, 0, 1 ) );
		}
		if ( cubeSize[ 0 ] == 4 )
		{
			material.setDiffuse( new GLVector( 1, 0, 1 ) );
		}
		if ( cubeSize[ 0 ] == 2 )
		{
			material.setDiffuse( new GLVector( 0, 1, 1 ) );
		}
		if ( cubeSize[ 0 ] == 1 )
		{
			material.setDiffuse( new GLVector( 1, 1, 0 ) );
		}

		if ( completeNeuron.getVertices().hasArray() )
		{
			completeNeuronVertices = completeNeuron.getVertices().array();
			completeMeshSize = completeNeuronVertices.length;
		}

		float[] neuronVertices = mesh.getVertices().array();
		int meshSize = neuronVertices.length;
		verticesArray = Arrays.copyOf( completeNeuronVertices, completeMeshSize + meshSize );
		System.arraycopy( neuronVertices, 0, verticesArray, completeMeshSize, meshSize );

		System.out.println( "number of elements complete mesh: " + verticesArray.length );
		completeNeuron.setVertices( FloatBuffer.wrap( verticesArray ) );
		completeNeuron.recalculateNormals();
		completeNeuron.setDirty( true );
	}

	public void addMesh()
	{
		verticesArray = new float[ 0 ];
		completeNeuronVertices = new float[ 0 ];
		completeMeshSize = 0;

		scene.addChild( completeNeuron );

	}

	public void removeMesh()
	{
		scene.removeChild( completeNeuron );
	}

	private void levelOfDetails( Mesh neuron, Scene scene, Camera cam )
	{
		final Thread neuronPositionThread = new Thread()
		{
			@Override
			public void run()
			{
				boolean dist3 = true;
				boolean dist2 = false;
				boolean dist1 = false;
				while ( true )
				{
					neuron.setNeedsUpdate( true );
//
//					float diff = cam.getPosition().minus( neuron.getPosition() ).magnitude();
//					logger.debug(" camera position: " + cam.getPosition().get( 0 ) + ":" + cam.getPosition().get( 1 ) + ":" + cam.getPosition().get( 2 ));
//					logger.debug(" mesh position: " + neuron.getPosition().get( 0 ) + ":" + neuron.getPosition().get( 1 ) + ":" + neuron.getPosition().get( 2 ));
//					logger.debug( "distance to camera: " + diff );
//					logger.debug( "dists - 4: " + dist3 + " 2: " + dist2 + " 1: " + dist1 );
//					if ( diff < 6 && diff >= 3 && dist3 )
//					{
//						cubeSize = new float[] { 1 , 1 , 1 };
//						logger.debug( "updating mesh dist4" );
//						logger.debug( "position before: " + neuron.getPosition() );
//						marchingCube( neuron, neuron.getMaterial(), scene, cam );
//						logger.debug( "position after: " + neuron.getPosition() );
//
//						dist3 = false;
//						dist2 = true;
//						dist1 = true;
//					}

//					else if ( diff < 3 && diff >= 2 && dist2 )
//					{
//						cubeSize = new int[] { 1, 1, 1 };
//						logger.debug( "updating mesh dist2" );
//						marchingCube( neuron, neuron.getMaterial(), scene, cam );
//						dist2 = false;
//						dist3 = true;
//						dist1 = true;
//					}
//					else if ( diff < 2 && diff >= 1 && dist1 )
//					{
//						cubeSize = new float[] { 1, 1, 1 };
//						logger.debug( "updating mesh dist1" );
//						marchingCube( neuron, neuron.getMaterial(), scene, cam );
//						dist1 = false;
//						dist2 = false;
//						dist3 = false;
//					}

					try
					{
						Thread.sleep( 20 );
					}
					catch ( InterruptedException e )
					{
						LOGGER.error( " thread sleep interrupted: " + e.getCause() );
					}
				}
			}
		};
		neuronPositionThread.start();
	}
}

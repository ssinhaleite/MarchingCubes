package application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cleargl.GLVector;
import graphics.scenery.Box;
import graphics.scenery.Camera;
import graphics.scenery.DetachedHeadCamera;
import graphics.scenery.Mesh;
import graphics.scenery.PointLight;
import graphics.scenery.SceneryDefaultApplication;
import graphics.scenery.SceneryElement;
import graphics.scenery.backends.Renderer;

/**
 * Class responsible for create the world/scene
 * 
 * @author vleite
 *
 */
public class MarchingCubesApplication extends SceneryDefaultApplication
{
	/** logger */
	static final Logger LOGGER = LoggerFactory.getLogger( MarchingCubesApplication.class );

	private float[] volumeResolution = null;

	public MarchingCubesApplication( String applicationName, int windowWidth, int windowHeight )
	{
		super( applicationName, windowWidth, windowHeight, false );
	}

	public void setVolumeResolution( float[] resolution )
	{
		this.volumeResolution = resolution;
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
		if ( volumeResolution == null )
			cam.setPosition( new GLVector( 0, 0, 10 ) );
		else
			cam.setPosition( new GLVector( volumeResolution[ 0 ] / 2, volumeResolution[ 1 ] / 2, 10 ) );
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
	}

	public void addChild( Mesh child )
	{
		getScene().addChild( child );
	}

	public void removeChild( Mesh child )
	{
		getScene().removeChild( child );
	}
}
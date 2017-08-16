package tests.javafx;

import cleargl.GLVector;
import graphics.scenery.Box;
import graphics.scenery.Camera;
import graphics.scenery.DetachedHeadCamera;
import graphics.scenery.Material;
import graphics.scenery.PointLight;
import graphics.scenery.SceneryBase;
import graphics.scenery.SceneryElement;
import graphics.scenery.backends.Renderer;
import graphics.scenery.utils.SceneryPanel;

public class SimpleSceneryApplication extends SceneryBase
{

	private SceneryPanel scPanel;

	public SimpleSceneryApplication( String applicationName, int windowWidth, int windowHeight, boolean wantREPL )
	{
		super( applicationName, windowWidth, windowHeight, wantREPL );

		scPanel = new SceneryPanel( 500, 500 );
	}

	public void init()
	{
		setRenderer(
				Renderer.createRenderer( getHub(), getApplicationName(), getScene(), getWindowWidth(), getWindowHeight(), scPanel ) );
		getHub().add( SceneryElement.Renderer, getRenderer() );

		final Box hull = new Box( new GLVector( 50.0f, 50.0f, 50.0f ), true );
		hull.getMaterial().setDiffuse( new GLVector( 0.5f, 0.5f, 0.5f ) );
		hull.getMaterial().setDoubleSided( true );
		getScene().addChild( hull );

		final Material material = new Material();
		material.setAmbient( new GLVector( 0.1f * 1, 1.0f, 1.0f ) );
		material.setDiffuse( new GLVector( 0.1f * 1, 0.0f, 1.0f ) );
		material.setSpecular( new GLVector( 0.1f * 1, 0f, 0f ) );

		final Camera cam = new DetachedHeadCamera();

		cam.perspectiveCamera( 50f, getWindowWidth(), getWindowHeight(), 0.1f, 1000.0f );
		cam.setActive( true );
		cam.setPosition( new GLVector( 2f, 2f, 10 ) );
		getScene().addChild( cam );

		final PointLight[] lights = new PointLight[ 4 ];

		for ( int i = 0; i < lights.length; i++ )
		{
			lights[ i ] = new PointLight();
			lights[ i ].setEmissionColor( new GLVector( 1.0f, 1.0f, 1.0f ) );
			lights[ i ].setIntensity( 100.2f * 5 );
			lights[ i ].setLinear( 0.0f );
			lights[ i ].setQuadratic( 0.1f );
			lights[ i ].showLightBox();
		}

		lights[ 0 ].setPosition( new GLVector( 1.0f, 0f, -1.0f / ( float ) Math.sqrt( 2.0 ) ) );
		lights[ 1 ].setPosition( new GLVector( -1.0f, 0f, -1.0f / ( float ) Math.sqrt( 2.0 ) ) );
		lights[ 2 ].setPosition( new GLVector( 0.0f, 1.0f, 1.0f / ( float ) Math.sqrt( 2.0 ) ) );
		lights[ 3 ].setPosition( new GLVector( 0.0f, -1.0f, 1.0f / ( float ) Math.sqrt( 2.0 ) ) );

		for ( int i = 0; i < lights.length; i++ )
			getScene().addChild( lights[ i ] );
	}

	public void setPanel( SceneryPanel scPanel )
	{
		this.scPanel = scPanel;
	}

	public SceneryPanel getPanel()
	{
		return scPanel;
	}
}

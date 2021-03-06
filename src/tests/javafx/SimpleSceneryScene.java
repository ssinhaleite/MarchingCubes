package tests.javafx;

import cleargl.GLVector;
import graphics.scenery.Box;
import graphics.scenery.Camera;
import graphics.scenery.DetachedHeadCamera;
import graphics.scenery.Hub;
import graphics.scenery.Material;
import graphics.scenery.PointLight;
import graphics.scenery.SceneryElement;
import graphics.scenery.Settings;
import graphics.scenery.backends.Renderer;
import graphics.scenery.controls.InputHandler;
import graphics.scenery.utils.SceneryPanel;
import graphics.scenery.utils.Statistics;

public class SimpleSceneryScene
{
	private final SceneryPanel scPanel;

	public SimpleSceneryScene( SceneryPanel panel )
	{
		if ( panel != null )
			this.scPanel = panel;
		else
			this.scPanel = new SceneryPanel( 500, 500 );
	}

	public void init()
	{
		final Hub hub = new Hub();

		final Settings settings = new Settings();
		hub.add( SceneryElement.Settings, settings );

		final Statistics statistics = new Statistics( hub );
		hub.add( SceneryElement.Statistics, statistics );

		final graphics.scenery.Scene scene = new graphics.scenery.Scene();
		final Renderer renderer = Renderer.createRenderer( hub, "Simple Scene", scene, 500, 500, scPanel );
		hub.add( SceneryElement.Renderer, renderer );

		InputHandler inputHandler = new InputHandler(scene, renderer, hub);
		inputHandler.useDefaultBindings( System.getProperty( "user.home" ) + "/.$applicationName.bindings" );

		final Box hull = new Box( new GLVector( 50.0f, 50.0f, 50.0f ), true );
		hull.getMaterial().setDiffuse( new GLVector( 0.5f, 0.5f, 0.5f ) );
		hull.getMaterial().setDoubleSided( true );
		scene.addChild( hull );

		final Material material = new Material();
		material.setAmbient( new GLVector( 0.1f * 1, 1.0f, 1.0f ) );
		material.setDiffuse( new GLVector( 0.1f * 1, 0.0f, 1.0f ) );
		material.setSpecular( new GLVector( 0.1f * 1, 0f, 0f ) );

		final Camera cam = new DetachedHeadCamera();

		cam.perspectiveCamera( 50f, renderer.getWindow().getHeight(), renderer.getWindow().getWidth(), 0.1f, 1000.0f );
		cam.setActive( true );
		cam.setPosition( new GLVector( 2f, 2f, 10 ) );
		scene.addChild( cam );

		// Add a delta for keyboard controls
		cam.setDeltaT( 5 );

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
			scene.addChild( lights[ i ] );
	}

	public SceneryPanel getPanel()
	{
		return scPanel;
	}
}

package marchingCubes;

import bdv.labels.labelset.Label;
import bdv.labels.labelset.LabelMultisetType;
import bdv.labels.labelset.Multiset;
import net.imagej.ops.Contingent;
import net.imagej.ops.Ops;
import net.imagej.ops.geom.geom3d.mesh.DefaultMesh;
import net.imagej.ops.geom.geom3d.mesh.DefaultVertexInterpolator;
import net.imagej.ops.geom.geom3d.mesh.Mesh;
import net.imagej.ops.geom.geom3d.mesh.TriangularFacet;
import net.imagej.ops.geom.geom3d.mesh.Vertex;
import net.imagej.ops.geom.geom3d.mesh.VertexInterpolator;
import net.imagej.ops.special.function.AbstractUnaryFunctionOp;
import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.BooleanType;
import net.imglib2.type.logic.BoolType;
import net.imglib2.view.ExtendedRandomAccessibleInterval;
import net.imglib2.view.Views;

/**
 * This is a marching cubes implementation. It is inspired by Paul Bourke's
 * (http://paulbourke.net/geometry/polygonise/) implementation. Especially the
 * lookup tables are from his implementation.
 * 
 * @author Tim-Oliver Buchholz (University of Konstanz)
 * @param <T>
 *            BooleanType
 */
public class MarchingCubes_ThreeDViewer extends
		AbstractUnaryFunctionOp< RandomAccessibleInterval< LabelMultisetType >, Mesh > implements
		Ops.Geometric.MarchingCubes, Contingent
{

//	@Parameter( type = ItemIO.INPUT, required = false )
	private double isolevel = 7;

//	@Parameter( type = ItemIO.INPUT, required = false )
	private VertexInterpolator interpolatorClass =
			new DefaultVertexInterpolator();

	@SuppressWarnings( { "unchecked" } )
	public DefaultMesh calculate( final RandomAccessibleInterval< LabelMultisetType > input )
	{
		DefaultMesh output = new DefaultMesh();
		ExtendedRandomAccessibleInterval< LabelMultisetType, RandomAccessibleInterval< LabelMultisetType > > extended =
				Views.extendValue( input, new LabelMultisetType() );
		Cursor< LabelMultisetType > c = Views.interval( extended, new FinalInterval( new long[] { input
				.min( 0 ) - 1, input.min( 1 ) - 1, input.min( 2 ) - 1 }, new long[] { input.max(
						0 ) + 1, input.max( 1 ) + 1, input.max( 2 ) + 1 } ) )
				.localizingCursor();

		while ( c.hasNext() )
		{
			c.next();

			int cursorX = c.getIntPosition( 0 );
			int cursorY = c.getIntPosition( 1 );
			int cursorZ = c.getIntPosition( 2 );
			Cursor< LabelMultisetType > cu = getCube( extended, cursorX, cursorY, cursorZ );
			int i = 0;
			double[] vertex_values = new double[ 8 ];
			while ( cu.hasNext() )
			{
//				System.out.println( "i: " + i );
//				System.out.println( "cu:" + cu );
//				System.out.println( "cu.hasNext():" + cu.hasNext() );
				LabelMultisetType it = cu.next();
//				System.out.println( "it:" + it );
//				boolean b = it.get();
//				System.out.println( "it.get():" + b );
//				vertex_values[ i++ ] = ( b ) ? 1 : 0;
				
				for ( final Multiset.Entry< Label > e : it.entrySet() )
				{
					vertex_values[ i ] = e.getElement().id();
//					System.out.println("value label: " + vertex_values[ i ]);
				}
				i++;
			}

			// @formatter:off
			//  6------7
			// /|     /|
			// 2-----3 |
			// |4----|-5
			// |/    |/
			// 0-----1
			// @formatter:on
			vertex_values = mapFlatIterableToLookUpCube( vertex_values );

			// @formatter:off
			//  4------5
			// /|     /|
			// 7-----6 |
			// |0----|-1
			// |/    |/
			// 3-----2
			// @formatter:on
			int cubeindex = getCubeIndex( vertex_values );

			if ( MarchingCubesTables.MC_EDGE_TABLE[ cubeindex ] != 0 )
			{
				int[] p0 = new int[] { 0 + cursorX, 0 + cursorY, 1 + cursorZ };
				int[] p1 = new int[] { 1 + cursorX, 0 + cursorY, 1 + cursorZ };
				int[] p2 = new int[] { 1 + cursorX, 0 + cursorY, 0 + cursorZ };
				int[] p3 = new int[] { 0 + cursorX, 0 + cursorY, 0 + cursorZ };
				int[] p4 = new int[] { 0 + cursorX, 1 + cursorY, 1 + cursorZ };
				int[] p5 = new int[] { 1 + cursorX, 1 + cursorY, 1 + cursorZ };
				int[] p6 = new int[] { 1 + cursorX, 1 + cursorY, 0 + cursorZ };
				int[] p7 = new int[] { 0 + cursorX, 1 + cursorY, 0 + cursorZ };

				double[][] vertlist = new double[ 12 ][];

				/* Find the vertices where the surface intersects the cube */
				if ( 0 != ( MarchingCubesTables.MC_EDGE_TABLE[ cubeindex ] & 1 ) )
				{
					vertlist[ 0 ] = interpolatePoint( p0, p1, vertex_values[ 0 ],
							vertex_values[ 1 ] );
				}
				if ( 0 != ( MarchingCubesTables.MC_EDGE_TABLE[ cubeindex ] & 2 ) )
				{
					vertlist[ 1 ] = interpolatePoint( p1, p2, vertex_values[ 1 ],
							vertex_values[ 2 ] );
				}
				if ( 0 != ( MarchingCubesTables.MC_EDGE_TABLE[ cubeindex ] & 4 ) )
				{
					vertlist[ 2 ] = interpolatePoint( p2, p3, vertex_values[ 2 ],
							vertex_values[ 3 ] );
				}
				if ( 0 != ( MarchingCubesTables.MC_EDGE_TABLE[ cubeindex ] & 8 ) )
				{
					vertlist[ 3 ] = interpolatePoint( p3, p0, vertex_values[ 3 ],
							vertex_values[ 0 ] );
				}
				if ( 0 != ( MarchingCubesTables.MC_EDGE_TABLE[ cubeindex ] & 16 ) )
				{
					vertlist[ 4 ] = interpolatePoint( p4, p5, vertex_values[ 4 ],
							vertex_values[ 5 ] );
				}
				if ( 0 != ( MarchingCubesTables.MC_EDGE_TABLE[ cubeindex ] & 32 ) )
				{
					vertlist[ 5 ] = interpolatePoint( p5, p6, vertex_values[ 5 ],
							vertex_values[ 6 ] );
				}
				if ( 0 != ( MarchingCubesTables.MC_EDGE_TABLE[ cubeindex ] & 64 ) )
				{
					vertlist[ 6 ] = interpolatePoint( p6, p7, vertex_values[ 6 ],
							vertex_values[ 7 ] );
				}
				if ( 0 != ( MarchingCubesTables.MC_EDGE_TABLE[ cubeindex ] & 128 ) )
				{
					vertlist[ 7 ] = interpolatePoint( p7, p4, vertex_values[ 7 ],
							vertex_values[ 4 ] );
				}
				if ( 0 != ( MarchingCubesTables.MC_EDGE_TABLE[ cubeindex ] & 256 ) )
				{
					vertlist[ 8 ] = interpolatePoint( p0, p4, vertex_values[ 0 ],
							vertex_values[ 4 ] );
				}
				if ( 0 != ( MarchingCubesTables.MC_EDGE_TABLE[ cubeindex ] & 512 ) )
				{
					vertlist[ 9 ] = interpolatePoint( p1, p5, vertex_values[ 1 ],
							vertex_values[ 5 ] );
				}
				if ( 0 != ( MarchingCubesTables.MC_EDGE_TABLE[ cubeindex ] & 1024 ) )
				{
					vertlist[ 10 ] = interpolatePoint( p2, p6, vertex_values[ 2 ],
							vertex_values[ 6 ] );
				}
				if ( 0 != ( MarchingCubesTables.MC_EDGE_TABLE[ cubeindex ] & 2048 ) )
				{
					vertlist[ 11 ] = interpolatePoint( p3, p7, vertex_values[ 3 ],
							vertex_values[ 7 ] );
				}

				/* Create the triangle */
				for ( i = 0; MarchingCubesTables.triTable[ cubeindex ][ i ] != -1; i += 3 )
				{

					TriangularFacet face = new TriangularFacet( new Vertex(
							vertlist[ MarchingCubesTables.triTable[ cubeindex ][ i + 2 ] ][ 0 ],
							vertlist[ MarchingCubesTables.triTable[ cubeindex ][ i + 2 ] ][ 1 ],
							vertlist[ MarchingCubesTables.triTable[ cubeindex ][ i + 2 ] ][ 2 ] ), new Vertex(
									vertlist[ MarchingCubesTables.triTable[ cubeindex ][ i + 1 ] ][ 0 ],
									vertlist[ MarchingCubesTables.triTable[ cubeindex ][ i + 1 ] ][ 1 ],
									vertlist[ MarchingCubesTables.triTable[ cubeindex ][ i + 1 ] ][ 2 ] ),
							new Vertex(
									vertlist[ MarchingCubesTables.triTable[ cubeindex ][ i ] ][ 0 ],
									vertlist[ MarchingCubesTables.triTable[ cubeindex ][ i ] ][ 1 ],
									vertlist[ MarchingCubesTables.triTable[ cubeindex ][ i ] ][ 2 ] ) );
					face.getArea();
					output.addFace( face );
				}
			}
		}
		
		System.out.println("The End");
		return output;
	}

	private double[] interpolatePoint( int[] p0, int[] p1, double v0, double v1 )
	{
		interpolatorClass.setPoint1( p0 );
		interpolatorClass.setPoint2( p1 );
		interpolatorClass.setValue1( v0 );
		interpolatorClass.setValue2( v1 );
		interpolatorClass.setIsoLevel( isolevel );
		((DefaultVertexInterpolator) interpolatorClass).run();

		return interpolatorClass.getOutput();
	}

	private int getCubeIndex( final double[] vertex_values )
	{
		int cubeindex = 0;
		for ( int i = 0; i < 8; i++ )
		{
			if ( vertex_values[ i ] < isolevel )
			{
				cubeindex |= ( int ) Math.pow( 2, i );
			}
		}
		return cubeindex;
	}

	private double[] mapFlatIterableToLookUpCube( final double[] vertex_values )
	{
		double[] vv = new double[ 8 ];
		vv[ 0 ] = vertex_values[ 4 ];
		vv[ 1 ] = vertex_values[ 5 ];
		vv[ 2 ] = vertex_values[ 1 ];
		vv[ 3 ] = vertex_values[ 0 ];
		vv[ 4 ] = vertex_values[ 6 ];
		vv[ 5 ] = vertex_values[ 7 ];
		vv[ 6 ] = vertex_values[ 3 ];
		vv[ 7 ] = vertex_values[ 2 ];

//		vv[ 0 ] = vertex_values[ 0 ];
//		vv[ 1 ] = vertex_values[ 1 ];
//		vv[ 2 ] = vertex_values[ 4 ];
//		vv[ 3 ] = vertex_values[ 5 ];
//		vv[ 4 ] = vertex_values[ 2 ];
//		vv[ 5 ] = vertex_values[ 3 ];
//		vv[ 6 ] = vertex_values[ 6 ];
//		vv[ 7 ] = vertex_values[ 7 ];

		return vv;
	}

	private Cursor< LabelMultisetType > getCube(
			final ExtendedRandomAccessibleInterval< LabelMultisetType, RandomAccessibleInterval< LabelMultisetType > > extended,
			final int cursorX, final int cursorY, final int cursorZ )
	{
		return Views.flatIterable( Views.interval( extended, new FinalInterval(
				new long[] { cursorX, cursorY, cursorZ }, new long[] { cursorX + 1,
						cursorY + 1, cursorZ + 1 } ) ) )
				.cursor();
	}

	@Override
	public boolean conforms() {
//		return in().numDimensions() == 3;
		return true;
	}

}

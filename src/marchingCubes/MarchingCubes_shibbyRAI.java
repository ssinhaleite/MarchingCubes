package marchingCubes;


import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import bdv.labels.labelset.Label;
import bdv.labels.labelset.LabelMultisetType;
import bdv.labels.labelset.Multiset;
import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.view.ExtendedRandomAccessibleInterval;
import net.imglib2.view.Views;

/**
 * Marching cubes based on
 * https://github.com/ShibbyTheCookie/MarchingCubes/tree/master/MarchingCubesJava
 * 
 * Using generic type
 */
public class MarchingCubes_shibbyRAI< T extends Comparable< T > >
{
	/** Log */
	private static final Logger LOGGER = Logger.getLogger( MarchingCubes_shibby.class.getName() );

	private ArrayList< float[] > vertices = new ArrayList<>();

	private static float[] lerp( float[] vec1, float[] vec2, double alpha )
	{
		return new float[] { ( float ) ( vec1[ 0 ] + ( vec2[ 0 ] - vec1[ 0 ] ) * alpha ), ( float ) ( vec1[ 1 ] + ( vec2[ 1 ] - vec1[ 1 ] ) * alpha ), ( float ) ( vec1[ 2 ] + ( vec2[ 2 ] - vec1[ 2 ] ) * alpha ) };
	}

	// Actual position along edge weighted according to function values.
	private float vertList[][] = new float[ 12 ][ 3 ];

	public ArrayList< float[] > marchingCubes( RandomAccessibleInterval< LabelMultisetType > values, int[] volDim, float[] voxDim, double isoLevel, int offset )
	{
//		System.out.println("marching cubes");
		// Calculate maximal possible axis value (used in vertices normalization)
		float maxX = voxDim[ 0 ] * ( volDim[ 0 ] - 1 );
		float maxY = voxDim[ 1 ] * ( volDim[ 1 ] - 1 );
		float maxZ = voxDim[ 2 ] * ( volDim[ 2 ] - 1 );
		float maxAxisVal = Math.max( maxX, Math.max( maxY, maxZ ) );

		ExtendedRandomAccessibleInterval< LabelMultisetType, RandomAccessibleInterval< LabelMultisetType > > extended =
				Views.extendValue( values, new LabelMultisetType() );
		Cursor< LabelMultisetType > c = Views.interval( extended, new FinalInterval( new long[] { values
				.min( 0 ) - 1, values.min( 1 ) - 1, values.min( 2 ) - 1 }, new long[] { values.max(
						0 ) + 1, values.max( 1 ) + 1, values.max( 2 ) + 1 } ) )
				.localizingCursor();
		
//		System.out.println("c: " + c);
//		System.out.println("c.hasNext(): " + c.hasNext());

		// Volume iteration
//		for ( int z = 0; z < volDim[ 2 ] - 1; z++ )
//		{
//			for ( int y = 0; y < volDim[ 1 ] - 1; y++ )
//			{
//				for ( int x = 0; x < volDim[ 0 ] - 1; x++ )
//				{
		while ( c.hasNext() ) {

			int cursorX = c.getIntPosition( 0 );
			int cursorY = c.getIntPosition( 1 );
			int cursorZ = c.getIntPosition( 2 );

			Cursor< LabelMultisetType > cu = Views.flatIterable( Views.interval( extended, new FinalInterval(
					new long[] { cursorX, cursorY, cursorZ }, new long[] { cursorX + 1,
							cursorY + 1, cursorZ + 1 } ) ) )
					.cursor();

			int i = 0;
			double[] vertex_values = new double[ 8 ];
			while ( cu.hasNext() )
			{
				LabelMultisetType it = cu.next();
				for ( final Multiset.Entry< Label > e : it.entrySet() )
				{
					vertex_values[ i ] = e.getElement().id();
				}
				i++;
			}

				// @formatter:off
					// Indices pointing to cube vertices
					//              pyz  ___________________  pxyz
					//                  /|                 /|
					//                 / |                / |
					//                /  |               /  |
					//          pz   /___|______________/pxz|
					//              |    |              |   |
					//              |    |              |   |
					//              | py |______________|___| pxy
					//              |   /               |   /
					//              |  /                |  /
					//              | /                 | /
					//              |/__________________|/
					//             p                     px
					// @formatter:on

//					int p = x + ( volDim[ 0 ] * y ) + ( volDim[ 0 ] * volDim[ 1 ] * ( z + offset ) ),
//							px = p + 1,
//							py = p + volDim[ 0 ],
//							pxy = py + 1,
//							pz = p + volDim[ 0 ] * volDim[ 1 ],
//							pxz = px + volDim[ 0 ] * volDim[ 1 ],
//							pyz = py + volDim[ 0 ] * volDim[ 1 ],
//							pxyz = pxy + volDim[ 0 ] * volDim[ 1 ];
//
//					// X Y Z
					float position[] = new float[] { cursorX * voxDim[ 0 ], cursorY * voxDim[ 1 ], ( cursorZ + offset ) * voxDim[ 2 ] };
//
//					// Voxel intensities
//					T value0 = values[ p ],
//							value1 = values[ px ],
//							value2 = values[ py ],
//							value3 = values[ pxy ],
//							value4 = values[ pz ],
//							value5 = values[ pxz ],
//							value6 = values[ pyz ],
//							value7 = values[ pxyz ];

			int p    = 0,
				px   = 1,
				py   = 4,
				pxy  = 5,
				pz   = 2,
				pxz  = 3,
				pyz  = 6,
				pxyz = 7;

			// Voxel intensities
			double value0 = vertex_values[ p ],
					value1 = vertex_values[ px ],
					value2 = vertex_values[ py ],
					value3 = vertex_values[ pxy ],
					value4 = vertex_values[ pz ],
					value5 = vertex_values[ pxz ],
					value6 = vertex_values[ pyz ],
					value7 = vertex_values[ pxyz ];

					// Voxel is active if its intensity is above isolevel
					int cubeindex = 0;
					if ( value0 < isoLevel )
						cubeindex |= 1;
					if ( value1 < isoLevel )
						cubeindex |= 2;
					if ( value2 < isoLevel )
						cubeindex |= 8;
					if ( value3 < isoLevel )
						cubeindex |= 4;
					if ( value4 < isoLevel )
						cubeindex |= 16;
					if ( value5 < isoLevel )
						cubeindex |= 32;
					if ( value6 < isoLevel )
						cubeindex |= 128;
					if ( value7 < isoLevel )
						cubeindex |= 64;

					// Fetch the triggered edges
					int bits = MarchingCubesTables.MC_EDGE_TABLE[ cubeindex ];

					// If no edge is triggered... skip
					if ( bits == 0 )
						continue;

					// Interpolate the positions based od voxel intensities
					double mu = 0.5f;

					// bottom of the cube
					if ( ( bits & 1 ) != 0 )
					{
//						T first = diff( isoLevel, value0 );
//						T second = diff( value1, value0 );
//						mu = ( float ) div( first, second );
						mu = (isoLevel - value0) / (value1 - value0);
						vertList[ 0 ] = lerp( position, new float[] { position[ 0 ] + voxDim[ 0 ], position[ 1 ], position[ 2 ] }, mu );
					}
					if ( ( bits & 2 ) != 0 )
					{
//						T first = diff( isoLevel, value1 );
//						T second = diff( value3, value1 );
//						mu = div( first, second );
						mu = (isoLevel - value1) / (value3 - value1);
						vertList[ 1 ] = lerp( new float[] { position[ 0 ] + voxDim[ 0 ], position[ 1 ], position[ 2 ] }, new float[] { position[ 0 ] + voxDim[ 0 ], position[ 1 ] + voxDim[ 1 ], position[ 2 ] }, mu );
					}
					if ( ( bits & 4 ) != 0 )
					{
//						T first = diff( isoLevel, value2 );
//						T second = diff( value3, value2 );
//						mu = div( first, second );
						mu = (isoLevel - value2) / (value3 - value2);
						vertList[ 2 ] = lerp( new float[] { position[ 0 ], position[ 1 ] + voxDim[ 1 ], position[ 2 ] }, new float[] { position[ 0 ] + voxDim[ 0 ], position[ 1 ] + voxDim[ 1 ], position[ 2 ] }, mu );
					}
					if ( ( bits & 8 ) != 0 )
					{
//						T first = diff( isoLevel, value0 );
//						T second = diff( value2, value0 );
//						mu = div( first, second );
						mu = (isoLevel - value0) / (value2 - value0);
						vertList[ 3 ] = lerp( position, new float[] { position[ 0 ], position[ 1 ] + voxDim[ 1 ], position[ 2 ] }, mu );
					}
					// top of the cube
					if ( ( bits & 16 ) != 0 )
					{
//						T first = diff( isoLevel, value4 );
//						T second = diff( value5, value4 );
//						mu = div( first, second );
						mu = (isoLevel - value4) / (value5 - value4);
						vertList[ 4 ] = lerp( new float[] { position[ 0 ], position[ 1 ], position[ 2 ] + voxDim[ 2 ] }, new float[] { position[ 0 ] + voxDim[ 0 ], position[ 1 ], position[ 2 ] + voxDim[ 2 ] }, mu );
					}
					if ( ( bits & 32 ) != 0 )
					{
//						T first = diff( isoLevel, value5 );
//						T second = diff( value7, value5 );
//						mu = div( first, second );
						mu = (isoLevel - value5) / (value7 - value5);
						vertList[ 5 ] = lerp( new float[] { position[ 0 ] + voxDim[ 0 ], position[ 1 ], position[ 2 ] + voxDim[ 2 ] }, new float[] { position[ 0 ] + voxDim[ 0 ], position[ 1 ] + voxDim[ 1 ], position[ 2 ] + voxDim[ 2 ] }, mu );
					}
					if ( ( bits & 64 ) != 0 )
					{
//						T first = diff( isoLevel, value6 );
//						T second = diff( value7, value6 );
//						mu = div( first, second );
						mu = (isoLevel - value6) / (value7 - value6);
						vertList[ 6 ] = lerp( new float[] { position[ 0 ], position[ 1 ] + voxDim[ 1 ], position[ 2 ] + voxDim[ 2 ] }, new float[] { position[ 0 ] + voxDim[ 0 ], position[ 1 ] + voxDim[ 1 ], position[ 2 ] + voxDim[ 2 ] }, mu );
					}
					if ( ( bits & 128 ) != 0 )
					{
//						T first = diff( isoLevel, value4 );
//						T second = diff( value6, value4 );
//						mu = div( first, second );
						mu = (isoLevel - value4) / (value6 - value4);
						vertList[ 7 ] = lerp( new float[] { position[ 0 ], position[ 1 ], position[ 2 ] + voxDim[ 2 ] }, new float[] { position[ 0 ], position[ 1 ] + voxDim[ 1 ], position[ 2 ] + voxDim[ 2 ] }, mu );
					}
					// vertical lines of the cube
					if ( ( bits & 256 ) != 0 )
					{
//						T first = diff( isoLevel, value0 );
//						T second = diff( value4, value0 );
//						mu = div( first, second );
						mu = (isoLevel - value0) / (value4 - value0);
						vertList[ 8 ] = lerp( position, new float[] { position[ 0 ], position[ 1 ], position[ 2 ] + voxDim[ 2 ] }, mu );
					}
					if ( ( bits & 512 ) != 0 )
					{
//						T first = diff( isoLevel, value1 );
//						T second = diff( value5, value1 );
//						mu = div( first, second );
						mu = (isoLevel - value1) / (value5 - value1);
						vertList[ 9 ] = lerp( new float[] { position[ 0 ] + voxDim[ 0 ], position[ 1 ], position[ 2 ] }, new float[] { position[ 0 ] + voxDim[ 0 ], position[ 1 ], position[ 2 ] + voxDim[ 2 ] }, mu );
					}
					if ( ( bits & 1024 ) != 0 )
					{
//						T first = diff( isoLevel, value3 );
//						T second = diff( value7, value3 );
//						mu = div( first, second );
						mu = (isoLevel - value3) / (value7 - value3);
						vertList[ 10 ] = lerp( new float[] { position[ 0 ] + voxDim[ 0 ], position[ 1 ] + voxDim[ 1 ], position[ 2 ] }, new float[] { position[ 0 ] + voxDim[ 0 ], position[ 1 ] + voxDim[ 1 ], position[ 2 ] + voxDim[ 2 ] }, mu );
					}
					if ( ( bits & 2048 ) != 0 )
					{
//						T first = diff( isoLevel, value2 );
//						T second = diff( value6, value2 );
//						mu = div( first, second );
						mu = (isoLevel - value2) / (value6 - value2);
						vertList[ 11 ] = lerp( new float[] { position[ 0 ], position[ 1 ] + voxDim[ 1 ], position[ 2 ] }, new float[] { position[ 0 ], position[ 1 ] + voxDim[ 1 ], position[ 2 ] + voxDim[ 2 ] }, mu );
					}

					// construct triangles -- get correct vertices from
					// triTable.
					i = 0;
					// "Re-purpose cubeindex into an offset into triTable."
					cubeindex <<= 4;

					while ( MarchingCubesTables.MC_TRI_TABLE[ cubeindex + i ] != -1 )
					{
						int index1 = MarchingCubesTables.MC_TRI_TABLE[ cubeindex + i ];
						int index2 = MarchingCubesTables.MC_TRI_TABLE[ cubeindex + i + 1 ];
						int index3 = MarchingCubesTables.MC_TRI_TABLE[ cubeindex + i + 2 ];

						// Add triangles vertices normalized with the maximal
						// possible value
						vertices.add( new float[] { vertList[ index3 ][ 0 ] / maxAxisVal - 0.5f, vertList[ index3 ][ 1 ] / maxAxisVal - 0.5f, vertList[ index3 ][ 2 ] / maxAxisVal - 0.5f } );
						vertices.add( new float[] { vertList[ index2 ][ 0 ] / maxAxisVal - 0.5f, vertList[ index2 ][ 1 ] / maxAxisVal - 0.5f, vertList[ index2 ][ 2 ] / maxAxisVal - 0.5f } );
						vertices.add( new float[] { vertList[ index1 ][ 0 ] / maxAxisVal - 0.5f, vertList[ index1 ][ 1 ] / maxAxisVal - 0.5f, vertList[ index1 ][ 2 ] / maxAxisVal - 0.5f } );
						LOGGER.log( Level.FINEST, "value on tritable: " + index1 );
						LOGGER.log( Level.FINEST, "value on tritable: " + index2 );
						LOGGER.log( Level.FINEST, "value on tritable: " + index3 );

						float aa = vertList[ index1 ][ 0 ] / maxAxisVal - 0.5f;
						float bb = vertList[ index1 ][ 1 ] / maxAxisVal - 0.5f;
						float cc = vertList[ index1 ][ 2 ] / maxAxisVal - 0.5f;
						LOGGER.log( Level.FINEST, "triangle 1: " + aa + " " + bb + " " + cc );
						float dd = vertList[ index2 ][ 0 ] / maxAxisVal - 0.5f;
						float ee = vertList[ index2 ][ 1 ] / maxAxisVal - 0.5f;
						float ff = vertList[ index2 ][ 2 ] / maxAxisVal - 0.5f;
						LOGGER.log( Level.FINEST, "triangle 2: " + dd + " " + ee + " " + ff );
						float gg = vertList[ index3 ][ 0 ] / maxAxisVal - 0.5f;
						float hh = vertList[ index3 ][ 1 ] / maxAxisVal - 0.5f;
						float ii = vertList[ index3 ][ 2 ] / maxAxisVal - 0.5f;
						LOGGER.log( Level.FINEST, "triangle 3: " + gg + " " + hh + " " + ii );

						i += 3;
					}
					LOGGER.log( Level.FINEST, "Number of vertices: " + vertices.size() );
//				}
//			}
//		}
		} //end while 
		LOGGER.log( Level.FINEST, "total number of vertices: "  + vertices.size() );
		return vertices;
	}

	@SuppressWarnings( "unchecked" )
	private T diff( T a, T b )
	{

		if ( a == null || b == null ) { return null; }

		if ( a instanceof Double )
		{
			return ( ( T ) new Double( ( ( Double ) a ).doubleValue() - ( ( Double ) b ).doubleValue() ) );
		}
		else if ( a instanceof Integer )
		{
			return ( ( T ) new Integer( ( ( Integer ) a ).intValue() - ( ( Integer ) b ).intValue() ) );
		}
		else if ( a instanceof Float )
		{
			return ( ( T ) new Float( ( ( Float ) a ).floatValue() - ( ( Float ) b ).floatValue() ) );
		}
		else if ( a instanceof Short )
		{
			return ( ( T ) new Short( ( short ) ( ( ( Short ) a ).shortValue() - ( ( Short ) b ).shortValue() ) ) );
		}
		else
		{
			throw new IllegalArgumentException( "Type " + a.getClass() + " is not supported by this method" );
		}
	}

	private float div( T a, T b )
	{

		if ( a == null || b == null ) { return 0; }

		if ( a instanceof Double )
		{
			return ( new Float( ( ( Double ) a ).doubleValue() / ( ( Double ) b ).doubleValue() ) );
		}
		else if ( a instanceof Integer )
		{
			return ( new Float( ( ( Integer ) a ).intValue() / ( ( Integer ) b ).intValue() ) );
		}
		else if ( a instanceof Float )
		{
			return ( new Float( ( ( Float ) a ).floatValue() / ( ( Float ) b ).floatValue() ) );
		}
		else if ( a instanceof Short )
		{
			return ( new Float( ( short ) ( ( ( Short ) a ).shortValue() / ( ( Short ) b ).shortValue() ) ) );
		}
		else
		{
			throw new IllegalArgumentException( "Type " + a.getClass() + " is not supported by this method" );
		}
	}
}

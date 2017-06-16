package marchingCubes;


import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Vector;
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
import viewer.Mesh;

/**
 * https://github.com/ilastik/marching_cubes/blob/master/src/marching_cubes.cpp
 * @author vleite
 * @param <T>
 */
public class MarchingCubes_ilastikRAI
{
	/** Log */
	private static final Logger LOGGER = Logger.getLogger( MarchingCubes_ilastikRAI.class.getName() );

	private int isolevel;
	
	int xWidth, xyWidth;
	
	/** List of Point3ds which form the isosurface. */
	HashMap< Long, IdPoint > vertexMapping = new HashMap< Long, IdPoint >();

	class IdPoint
	{
		int id;
		float x, y, z;
	}

	class Triangle
	{
		int pointId[] = new int[ 3 ];
	}

	private float maxVal;
	/**
	 * the marching cubes algorithm as described here:
	 * http://paulbourke.net/geometry/polygonise/ volume: contains the data
	 * (size = xDim * yDim * zDim) [xyz]Dim: the dimensions in each direction
	 * isoLevel: the minimum isoLevel, all values >= isoLevel will contribute to
	 * the mesh the mesh is returned, the caller takes ownership over the
	 * pointers
	 * @throws FileNotFoundException 
	 */
	public Mesh march( RandomAccessibleInterval< LabelMultisetType > input, int xDim, int yDim, int zDim, int isoLevel ) throws FileNotFoundException
	{
//		PrintStream fileStream = new PrintStream("filename.txt");
//		System.setOut( fileStream );

		float maxX = xDim * ( xDim - 1 );
		float maxY = yDim * ( yDim - 1 );
		float maxZ = zDim * ( zDim - 1 );
		maxVal = Math.max( maxX, Math.max( maxY, maxZ ) );
		isolevel = isoLevel;
		--xDim;
		--yDim;
		--zDim;

		xWidth = xDim;
		xyWidth = yDim;
		


		Vector< Triangle > triangles = new Vector< Triangle >();

		//System.out.println( "creating mesh for " + xDim + "x" + yDim + "x" + zDim
			//	+ " volume"/* with " + nCellsX + "x" + nCellsY + "x" + nCellsZ + " cells" */);

		//System.out.println( "volume size: " + xDim * yDim * zDim );

//		Timestamp begin = new Timestamp( System.currentTimeMillis() );
		ExtendedRandomAccessibleInterval< LabelMultisetType, RandomAccessibleInterval< LabelMultisetType > > extended =
				Views.extendValue( input, new LabelMultisetType() );
		Cursor< LabelMultisetType > c = Views.interval( extended, new FinalInterval( new long[] { input
				.min( 0 ) - 1, input.min( 1 ) - 1, input.min( 2 ) - 1 }, new long[] { input.max(
						0 ) + 1, input.max( 1 ) + 1, input.max( 2 ) + 1 } ) )
				.localizingCursor();
//		Timestamp end = new Timestamp( System.currentTimeMillis() );
//		//System.out.println( "time for generating cursor: " + ( end.getTime() - begin.getTime() ) );

		while ( c.hasNext() )
		{
//			begin = new Timestamp( System.currentTimeMillis() );
			c.next();

			int cursorX = c.getIntPosition( 0 );
			int cursorY = c.getIntPosition( 1 );
			int cursorZ = c.getIntPosition( 2 );
			
			if (cursorX == -1 || cursorY == -1 || cursorZ == -1)
				continue;

			Cursor< LabelMultisetType > cu = getCube( extended, cursorX, cursorY, cursorZ );
			int tableIndex = 0;

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
			// the values from the cube are given first in x, then y, then z
			// this way, the vertex_values are positioned in this way:
			//
			//  6------7
			// /|     /|
			// 2-----3 |
			// |4----|-5
			// |/    |/
			// 0-----1
			//
			// this algorithm (based on http://paulbourke.net/geometry/polygonise/)
			// consider the vertex of the cube in this order:
			//
			//  4------5
			// /|     /|
			// 7-----6 |
			// |0----|-1
			// |/    |/
			// 3-----2
			//
			// edge indexes:
			//
			//          4-----*4*----5
			//         /|           /|
			//        /*8*         / |
			//      *7* |        *5* |
			//      /   |        /  *9*
			//     7-----*6*----6    |
			//     |    0----*0*-----1
			//   *11*  /       *10* /
			//     |  /         | *1*
			//     |*3*         | /
			//     |/           |/
			//     3-----*2*----2
			//
			//
			// @formatter:on

			vertex_values = remapCube( vertex_values );

			for ( i = 0; i < 8; i++ )
			{
				if ( vertex_values[ i ] < isoLevel )
				{
					tableIndex |= ( int ) Math.pow( 2, i );
				}
			}

			LOGGER.log( Level.FINER, "tableIndex = " + tableIndex );

			if ( MarchingCubesTables.MC_EDGE_TABLE[ tableIndex ] != 0 )
			{
				//System.out.println("tableIndex: " + tableIndex  + " x: " + cursorX + " y: " + cursorY + " z: " + cursorZ);
				if ( ( MarchingCubesTables.MC_EDGE_TABLE[ tableIndex ] & 8 ) != 0 )
				{
//					//System.out.println("cursorX: " + cursorX + " cursorY " + cursorY + " cursorZ " + cursorZ);
					IdPoint pt = calculateIntersection( cursorX, cursorY, cursorZ, 3, vertex_values[3], vertex_values[0] );
					long id = edgeId( cursorX, cursorY, cursorZ, 3);
					//System.out.println("vertexMapping: id: " + id + " pt: " + pt.x + " " + pt.y + " " + pt.z );
					vertexMapping.put( id, pt );
				}
				if ( ( MarchingCubesTables.MC_EDGE_TABLE[ tableIndex ] & 1 ) != 0 )
				{
//					//System.out.println("cursorX: " + cursorX + " cursorY " + cursorY + " cursorZ " + cursorZ);
					IdPoint pt = calculateIntersection( cursorX, cursorY, cursorZ, 0, vertex_values[0], vertex_values[1] );
					long id = edgeId( cursorX, cursorY, cursorZ, 0 );
					//System.out.println("vertexMapping: id: " + id + " pt: " + pt.x + " " + pt.y + " " + pt.z );
					vertexMapping.put( id, pt );
				}
				if ( ( MarchingCubesTables.MC_EDGE_TABLE[ tableIndex ] & 256 ) != 0 )
				{
//					//System.out.println("cursorX: " + cursorX + " cursorY " + cursorY + " cursorZ " + cursorZ);
					IdPoint pt = calculateIntersection( cursorX, cursorY, cursorZ, 8, vertex_values[0], vertex_values[4] );
					long id = edgeId( cursorX, cursorY, cursorZ, 8 );
					//System.out.println("vertexMapping: id: " + id + " pt: " + pt.x + " " + pt.y + " " + pt.z );
					vertexMapping.put( id, pt );
				}

//				if ( cursorX == xDim - 1 )
//				{
					if ( ( MarchingCubesTables.MC_EDGE_TABLE[ tableIndex ] & 4 ) != 0 )
					{
						//					//System.out.println("cursorX: " + cursorX + " cursorY " + cursorY + " cursorZ " + cursorZ);
						IdPoint pt = calculateIntersection( cursorX, cursorY, cursorZ, 2, vertex_values[2], vertex_values[3] );
						long id = edgeId( cursorX, cursorY, cursorZ, 2 );
						//System.out.println("vertexMapping: id: " + id + " pt: " + pt.x + " " + pt.y + " " + pt.z );
						vertexMapping.put( id, pt );
					}
					if ( ( MarchingCubesTables.MC_EDGE_TABLE[ tableIndex ] & 2048 ) != 0 )
					{
						//					//System.out.println("cursorX: " + cursorX + " cursorY " + cursorY + " cursorZ " + cursorZ);
						IdPoint pt = calculateIntersection( cursorX, cursorY, cursorZ, 11, vertex_values[3], vertex_values[7] );
						long id = edgeId( cursorX, cursorY, cursorZ, 11 );
						//System.out.println("vertexMapping: id: " + id + " pt: " + pt.x + " " + pt.y + " " + pt.z );
						vertexMapping.put( id, pt );
					}
//				}
//				if ( cursorY == yDim - 1 )
//				{
					if ( ( MarchingCubesTables.MC_EDGE_TABLE[ tableIndex ] & 2 ) != 0 )
					{
						//					//System.out.println("cursorX: " + cursorX + " cursorY " + cursorY + " cursorZ " + cursorZ);
						IdPoint pt = calculateIntersection( cursorX, cursorY, cursorZ, 1, vertex_values[1], vertex_values[2] );
						long id = edgeId( cursorX, cursorY, cursorZ, 1 );
						//System.out.println("vertexMapping: id: " + id + " pt: " + pt.x + " " + pt.y + " " + pt.z );
						vertexMapping.put( id, pt );
					}
					if ( ( MarchingCubesTables.MC_EDGE_TABLE[ tableIndex ] & 512 ) != 0 )
					{
						//					//System.out.println("cursorX: " + cursorX + " cursorY " + cursorY + " cursorZ " + cursorZ);
						IdPoint pt = calculateIntersection( cursorX, cursorY, cursorZ, 9, vertex_values[1], vertex_values[5] );
						long id = edgeId( cursorX, cursorY, cursorZ, 9 );
						//System.out.println("vertexMapping: id: " + id + " pt: " + pt.x + " " + pt.y + " " + pt.z );
						vertexMapping.put( id, pt );
					}
//				}
//				if ( cursorZ == zDim - 1 )
//				{
					if ( ( MarchingCubesTables.MC_EDGE_TABLE[ tableIndex ] & 16 ) != 0 )
					{
						//					//System.out.println("cursorX: " + cursorX + " cursorY " + cursorY + " cursorZ " + cursorZ);
						IdPoint pt = calculateIntersection( cursorX, cursorY, cursorZ, 4, vertex_values[4], vertex_values[5] );
						long id = edgeId( cursorX, cursorY, cursorZ, 4 );
						//System.out.println("vertexMapping: id: " + id + " pt: " + pt.x + " " + pt.y + " " + pt.z );
						vertexMapping.put( id, pt );
					}
					if ( ( MarchingCubesTables.MC_EDGE_TABLE[ tableIndex ] & 128 ) != 0 )
					{
						//					//System.out.println("cursorX: " + cursorX + " cursorY " + cursorY + " cursorZ " + cursorZ);
						IdPoint pt = calculateIntersection( cursorX, cursorY, cursorZ, 7,vertex_values[7], vertex_values[4] );
						long id = edgeId( cursorX, cursorY, cursorZ, 7 );
						//System.out.println("vertexMapping: id: " + id + " pt: " + pt.x + " " + pt.y + " " + pt.z );
						vertexMapping.put( id, pt );
					}
//				}
//				if ( ( cursorX == xDim - 1 ) && ( cursorY == yDim - 1 ) )
					if ( ( MarchingCubesTables.MC_EDGE_TABLE[ tableIndex ] & 1024 ) != 0 )
					{
						//					//System.out.println("cursorX: " + cursorX + " cursorY " + cursorY + " cursorZ " + cursorZ);
						IdPoint pt = calculateIntersection( cursorX, cursorY, cursorZ, 10, vertex_values[2], vertex_values[6] );
						long id = edgeId( cursorX, cursorY, cursorZ, 10 );
						//System.out.println("vertexMapping: id: " + id + " pt: " + pt.x + " " + pt.y + " " + pt.z );
						vertexMapping.put( id, pt );
					}
//				if ( ( cursorX == xDim - 1 ) && ( cursorZ == zDim - 1 ) )
					if ( ( MarchingCubesTables.MC_EDGE_TABLE[ tableIndex ] & 64 ) != 0 )
					{
						//					//System.out.println("cursorX: " + cursorX + " cursorY " + cursorY + " cursorZ " + cursorZ);
						IdPoint pt = calculateIntersection( cursorX, cursorY, cursorZ, 6, vertex_values[6], vertex_values[7] );
						long id = edgeId( cursorX, cursorY, cursorZ, 6 );
						//System.out.println("vertexMapping: id: " + id + " pt: " + pt.x + " " + pt.y + " " + pt.z );
						vertexMapping.put( id, pt );
					}
//				if ( ( cursorY == yDim - 1 ) && ( cursorZ == zDim - 1 ) )
					if ( ( MarchingCubesTables.MC_EDGE_TABLE[ tableIndex ] & 32 ) != 0 )
					{
						//					//System.out.println("cursorX: " + cursorX + " cursorY " + cursorY + " cursorZ " + cursorZ);
						IdPoint pt = calculateIntersection( cursorX, cursorY, cursorZ, 5, vertex_values[5], vertex_values[6] );
						long id = edgeId( cursorX, cursorY, cursorZ, 5 );
						//System.out.println("vertexMapping: id: " + id + " pt: " + pt.x + " " + pt.y + " " + pt.z );
						vertexMapping.put( id, pt );
					}

				for ( i = 0; MarchingCubesTables.triTable[ tableIndex ][ i ] != MarchingCubesTables.Invalid; i += 3 )
				{
					Triangle triangle = new Triangle();
					int pointId0, pointId1, pointId2;
					pointId0 = edgeId( cursorX, cursorY, cursorZ, MarchingCubesTables.triTable[ tableIndex ][ i ] );
					pointId1 = edgeId( cursorX, cursorY, cursorZ, MarchingCubesTables.triTable[ tableIndex ][ i + 1 ] );
					pointId2 = edgeId( cursorX, cursorY, cursorZ, MarchingCubesTables.triTable[ tableIndex ][ i + 2 ] );
					triangle.pointId[ 0 ] = pointId0;
					triangle.pointId[ 1 ] = pointId1;
					triangle.pointId[ 2 ] = pointId2;
					triangles.add( triangle );
					//System.out.println("triangle ids: " + pointId0 + " " + pointId1 + " " + pointId2 );
				}
			}
			
//			end = new Timestamp( System.currentTimeMillis() );
//			//System.out.println( "time for generating triangles for each point: " + ( end.getTime() - begin.getTime() ) );
		}

		Mesh mesh = reindex( triangles );

		calculateNormals( mesh );

		return mesh;
	}

	private int vertexId( int x, int y, int z )
	{
		return 3 * ( z * ( xyWidth + 1 ) * ( xWidth + 1 ) + y * ( xWidth + 1 ) + x );
	}

	private int edgeId( int x, int y, int z, int edgeNumber )
	{
		switch ( edgeNumber )
		{
		case 0:
			return vertexId( x, y, z ) + 1;
		case 1:
			return vertexId( x, y + 1, z );
		case 2:
			return vertexId( x + 1, y, z ) + 1;
		case 3:
			return vertexId( x, y, z );
		case 4:
			return vertexId( x, y, z + 1 ) + 1;
		case 5:
			return vertexId( x, y + 1, z + 1 );
		case 6:
			return vertexId( x + 1, y, z + 1 ) + 1;
		case 7:
			return vertexId( x, y, z + 1 );
		case 8:
			return vertexId( x, y, z ) + 2;
		case 9:
			return vertexId( x, y + 1, z ) + 2;
		case 10:
			return vertexId( x + 1, y + 1, z ) + 2;
		case 11:
			return vertexId( x + 1, y, z ) + 2;
		default:
			return -1;
		}
	}

	private IdPoint calculateIntersection( int x, int y, int z, int edgeNumber, double vertex_values, double vertex_values2 )
	{
		float x1 = x;
		float x2 = x;
		float y1 = y;
		float y2 = y;
		float z1 = z;
		float z2 = z;

		switch ( edgeNumber )
		{
		case 0:
			y2 += 1;
			break;
		case 1:
			y1 += 1;
			x2 += 1;
			y2 += 1;
			break;
		case 2:
			x1 += 1;
			y1 += 1;
			x2 += 1;
			break;
		case 3:
			x1 += 1;
			break;
		case 4:
			z1 += 1;
			y2 += 1;
			z2 += 1;
			break;
		case 5:
			y1 += 1;
			z1 += 1;
			x2 += 1;
			y2 += 1;
			z2 += 1;
			break;
		case 6:
			x1 += 1;
			y1 += 1;
			z1 += 1;
			x2 += 1;
			z2 += 1;
			break;
		case 7:
			x1 += 1;
			z1 += 1;
			z2 += 1;
			break;
		case 8:
			z2 += 1;
			break;
		case 9:
			y1 += 1;
			y2 += 1;
			z2 += 1;
			break;
		case 10:
			x1 += 1;
			y1 += 1;
			x2 += 1;
			y2 += 1;
			z2 += 1;
			break;
		case 11:
			x1 += 1;
			x2 += 1;
			z2 += 1;
			break;
		}

		IdPoint intersection = new IdPoint();
		
		if (Math.abs(isolevel - vertex_values) < 0.00001) {
			intersection.x = x1;
			intersection.y = y1;
			intersection.z = z1;
		} else if (Math.abs(isolevel - vertex_values2) < 0.00001) {
			intersection.x = x2;
			intersection.y = y2;
			intersection.z = z2;
		} else if (Math.abs(vertex_values - vertex_values2) < 0.00001) {
				intersection.x = x1;
				intersection.y = y1;
				intersection.z = z1;
		} else {
			float mu = ( float ) ( (isolevel - vertex_values) / (vertex_values2 - vertex_values) );

			intersection.x = x1 + mu * (x2 - x1);
			intersection.y = y1 + mu * (y2 - y1);
			intersection.z = z1 + mu * (z1 - z1);
		}
//		intersection.x = ( x1 + x2 ) / 2;
//		intersection.y = ( y1 + y2 ) / 2;
//		intersection.z = ( z1 + z2 ) / 2;

		return intersection;
	}

	Mesh reindex( Vector< Triangle > triangles )
	{
		Iterator< Entry< Long, IdPoint > > vertexMappingIterator = vertexMapping.entrySet().iterator();
//		Iterator< Triangle > vecIterator = triangles.iterator();

		//System.out.println("number of ids on map: " + vertexMapping.size() );

		// rename vertices
		int nextId = 0;
		while ( vertexMappingIterator.hasNext() )
		{
			HashMap.Entry< Long, IdPoint > entry = vertexMappingIterator.next();
			//System.out.println("old id: " + entry.getValue().id + " new id: " + nextId );
			entry.getValue().id = nextId;
			nextId++;
		}

		//System.out.println("number of triangles: " + triangles.size() );

		// rename triangles
		for ( Triangle triangle : triangles )
		{
			//System.out.println("old triangle.pointId[0]: " + triangle.pointId[0]);
			//System.out.println("old triangle.pointId[1]: " + triangle.pointId[1]);
			//System.out.println("old triangle.pointId[2]: " + triangle.pointId[2]);

			triangle.pointId[ 0 ] = vertexMapping.get( triangle.pointId[ 0 ] ).id;
			triangle.pointId[ 1 ] = vertexMapping.get( triangle.pointId[ 1 ] ).id;
			triangle.pointId[ 2 ] = vertexMapping.get( triangle.pointId[ 2 ] ).id;
		}
//		while ( vecIterator.hasNext() )
//		{
//			Triangle next = vecIterator.next();
//			LOGGER.log( Level.FINEST, "getting triangle" );
//			for ( int i = 0; i < 3; i++ )
//			{
//				System.out.println( "triangle point old id: " + next.pointId[ i ] );
//				System.out.println( "id of id - new id: " + vertexMapping.get( next.pointId[ i ] ).id );
//
//				LOGGER.log( Level.FINEST, "triangle point old id: " + next.pointId[ i ] );
//				LOGGER.log( Level.FINEST, "id of id - new id: " + vertexMapping.get( next.pointId[ i ] ).id );
//				if (vertexMapping.get( next.pointId[ i ] ) == null )
//					continue;
//				long newId = vertexMapping.get( next.pointId[ i ] ).id;
//				next.pointId[ i ] = ( int ) newId;
//			}
//		}



		// Copy all the vertices and triangles into two arrays so that they
		// can be efficiently accessed.
		// Copy vertices.
		int vertexCount = vertexMapping.size();
		
		//System.out.println("created a mesh with " + vertexCount );
		float[][] vertices = new float[ vertexCount ][ 3 ];
		int index = 0;
		// point the iterator to the begin again
		vertexMappingIterator = vertexMapping.entrySet().iterator();
		while ( vertexMappingIterator.hasNext() )
		{
			HashMap.Entry< Long, IdPoint > entry = vertexMappingIterator.next();

			vertices[ index ][ 0 ] = ( float ) ( entry.getValue().x/* / maxVal - 0.5 */);
			vertices[ index ][ 1 ] = ( float ) ( entry.getValue().y/* / maxVal - 0.5 */);
			vertices[ index ][ 2 ] = ( float ) ( entry.getValue().z/* / maxVal - 0.5 */);
			++index;
		}

		int triangleCount = triangles.size();
		int[] triangleIndices = new int[ triangleCount * 3 ];
//		int i = 0;
//		while ( vecIterator.hasNext() )
//		//for ( int i = 0; i < triangleCount; i++ )
//		{
//			Triangle next = vecIterator.next();
//			triangleIndices[ i * 3 ] = next.pointId[ 0 ];
//			triangleIndices[ i * 3 + 1 ] = next.pointId[ 1 ];
//			triangleIndices[ i * 3 + 2 ] = next.pointId[ 2 ];
//			i++;
//		}
		index = 0;
		for ( Triangle triangle : triangles )
		{
			triangleIndices[ index * 3 ] = triangle.pointId[ 0 ];
			triangleIndices[ index * 3 + 1 ] = triangle.pointId[ 1 ];
			triangleIndices[ index * 3 + 2 ] = triangle.pointId[ 2 ];
			++index;
		}

//		vertexMapping.clear();
//		triangles.clear();

		Mesh mesh = new Mesh();
		mesh.setNumberOfVertices( vertexCount );
		mesh.setVertices( vertices );
		mesh.setNumberOfTriangles( triangleCount );
		mesh.setTriangles( triangleIndices );
		return mesh;
	}

	private void calculateNormals( Mesh mesh )
	{
		int vertexCount = mesh.getNumberOfVertices();
		float[][] vertices = mesh.getVertices();
		int triangleCount = mesh.getNumberOfTriangles();
		int[] triangles = mesh.getTriangles();

		float[][] normals = new float[ vertexCount ][ 3 ];

		for ( int i = 0; i < vertexCount; ++i )
		{
			normals[ i ][ 0 ] = 0;
			normals[ i ][ 1 ] = 0;
			normals[ i ][ 2 ] = 1;
		}

		// omp parallel for
		for ( int i = 0; i < triangleCount; ++i )
		{
			float[] vec1 = new float[ 3 ], vec2 = new float[ 3 ],
					normal = new float[ 3 ];
			int id0, id1, id2;
			id0 = ( int ) triangles[ i * 3 ];
			id1 = ( int ) triangles[ i * 3 + 1 ];
			id2 = ( int ) triangles[ i * 3 + 2 ];
			vec1[ 0 ] = vertices[ id1 ][ 0 ] - vertices[ id2 ][ 0 ];
			vec1[ 1 ] = vertices[ id1 ][ 1 ] - vertices[ id2 ][ 1 ];
			vec1[ 2 ] = vertices[ id1 ][ 2 ] - vertices[ id2 ][ 2 ];
			vec2[ 0 ] = vertices[ id0 ][ 0 ] - vertices[ id2 ][ 0 ];
			vec2[ 1 ] = vertices[ id0 ][ 1 ] - vertices[ id2 ][ 1 ];
			vec2[ 2 ] = vertices[ id0 ][ 2 ] - vertices[ id2 ][ 2 ];
			normal[ 0 ] = vec1[ 2 ] * vec2[ 1 ] - vec1[ 1 ] * vec2[ 2 ];
			normal[ 1 ] = vec1[ 0 ] * vec2[ 2 ] - vec1[ 2 ] * vec2[ 0 ];
			normal[ 2 ] = vec1[ 1 ] * vec2[ 0 ] - vec1[ 0 ] * vec2[ 1 ];
			normals[ id0 ][ 0 ] += normal[ 0 ];
			normals[ id0 ][ 1 ] += normal[ 1 ];
			normals[ id0 ][ 2 ] += normal[ 2 ];
			normals[ id1 ][ 0 ] += normal[ 0 ];
			normals[ id1 ][ 1 ] += normal[ 1 ];
			normals[ id1 ][ 2 ] += normal[ 2 ];
			normals[ id2 ][ 0 ] += normal[ 0 ];
			normals[ id2 ][ 1 ] += normal[ 1 ];
			normals[ id2 ][ 2 ] += normal[ 2 ];
		}

		// omp parallel for
		for ( int i = 0; i < vertexCount; ++i )
		{
			float length = ( float ) Math.sqrt( normals[ i ][ 0 ] * normals[ i ][ 0 ] + normals[ i ][ 1 ] * normals[ i ][ 1 ] + normals[ i ][ 2 ] * normals[ i ][ 2 ] );
			normals[ i ][ 0 ] /= length;
			normals[ i ][ 1 ] /= length;
			normals[ i ][ 2 ] /= length;
		}
		mesh.setNormals( normals );
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
	
	private double[] remapCube( final double[] vertex_values )
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

		return vv;
	}
}

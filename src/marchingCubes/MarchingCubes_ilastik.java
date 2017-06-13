package marchingCubes;


import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * https://github.com/ilastik/marching_cubes/blob/master/src/marching_cubes.cpp
 * @author vleite
 * @param <T>
 */
public class MarchingCubes_ilastik< T extends Comparable< T > >
{
	/** Log */
	private static final Logger LOGGER = Logger.getLogger( MarchingCubes_ilastik.class.getName() );

	/** No. of cells in x, y, and z directions. */
	int nCellsX, nCellsY, nCellsZ;

	/** List of Point3ds which form the isosurface. */
	HashMap< Long, IdPoint > vertexMapping = new HashMap< Long, IdPoint >();

	class IdPoint
	{
		long id;
		float x, y, z;
	}

	class Triangle
	{
		long pointId[] = new long[ 3 ];
	}

	/**
	 * the primary struct used to pass around the components of a mesh vertices:
	 * the vertex positions as an array of points normals: the normal direction
	 * of each vertex as an array of points vertexCount: the number of vertices
	 * and normals faces: the faces given by 3 vertex indices (length =
	 * faceCount * 3) faceCount: the number of faces
	 */
	class Mesh
	{
		int numberOfVertices;

		float[][] vertices;

		float[][] normals;

		int numberOfTriangles;

		long[] triangles;

		Mesh( int vertexCount, float[][] verticesArray, float[][] normalsArray, int trianglesCount, long[] trianglesArray )
		{
			numberOfVertices = vertexCount;
			vertices = verticesArray;
			normals = normalsArray;
			numberOfTriangles = trianglesCount;
			triangles = trianglesArray;
		}

		Mesh()
		{}
	}

	/**
	 * the marching cubes algorithm as described here:
	 * http://paulbourke.net/geometry/polygonise/ volume: contains the data
	 * (size = xDim * yDim * zDim) [xyz]Dim: the dimensions in each direction
	 * isoLevel: the minimum isoLevel, all values >= isoLevel will contribute to
	 * the mesh the mesh is returned, the caller takes ownership over the
	 * pointers
	 */
	public Mesh march( T[] volume, int xDim, int yDim, int zDim, T isoLevel )
	{
		--xDim;
		--yDim;
		--zDim;

		nCellsX = xDim;
		nCellsY = yDim;
		nCellsZ = zDim;

		int xWidth = ( xDim + 1 );
		int xyWidth = xWidth * ( yDim + 1 );
		Vector< Triangle > triangles = new Vector< Triangle >();

		LOGGER.log( Level.FINE, "creating mesh for " + xDim + "x" + yDim + "x" + zDim
				+ " volume with " + nCellsX + "x" + nCellsY + "x" + nCellsZ + " cells" );

		LOGGER.log( Level.FINE, "volume size: " + xDim * yDim * zDim );

		for ( int z = 0; z < zDim; z++ )
			for ( int y = 0; y < yDim; y++ )
				for ( int x = 0; x < xDim; x++ )
				{
					int tableIndex = 0;

					LOGGER.log( Level.FINER, "x y z: " + x + " " + y + " " + z );
					if ( volume[ z * xyWidth + y * xWidth + x ].compareTo( isoLevel ) < 0 )
						tableIndex |= 1;
					if ( volume[ z * xyWidth + ( y + 1 ) * xWidth + x ].compareTo( isoLevel ) < 0 )
						tableIndex |= 2;
					if ( volume[ z * xyWidth + ( y + 1 ) * xWidth + ( x + 1 ) ].compareTo( isoLevel ) < 0 )
						tableIndex |= 4;
					if ( volume[ z * xyWidth + y * xWidth + ( x + 1 ) ].compareTo( isoLevel ) < 0 )
						tableIndex |= 8;
					if ( volume[ ( z + 1 ) * xyWidth + y * xWidth + x ].compareTo( isoLevel ) < 0 )
						tableIndex |= 16;
					if ( volume[ ( z + 1 ) * xyWidth + ( y + 1 ) * xWidth + x ].compareTo( isoLevel ) < 0 )
						tableIndex |= 32;
					if ( volume[ ( z + 1 ) * xyWidth + ( y + 1 ) * xWidth + ( x + 1 ) ].compareTo( isoLevel ) < 0 )
						tableIndex |= 64;
					if ( volume[ ( z + 1 ) * xyWidth + y * xWidth + ( x + 1 ) ].compareTo( isoLevel ) < 0 )
						tableIndex |= 128;

					LOGGER.log( Level.FINER, "tableIndex = " + tableIndex );
					
					if ( MarchingCubesTables.MC_EDGE_TABLE[ tableIndex ] != 0 )
					{
						if ( ( MarchingCubesTables.MC_EDGE_TABLE[ tableIndex ] & 8 ) != 0 )
						{
							LOGGER.log( Level.FINEST, "x: " + x + " y " + y + " z " + z );
							IdPoint pt = calculateIntersection( x, y, z, 3 );
							long id = edgeId( x, y, z, 3 );
							LOGGER.log( Level.FINEST, " adding point with id: " + id );
							vertexMapping.put( id, pt );
						}
						if ( ( MarchingCubesTables.MC_EDGE_TABLE[ tableIndex ] & 1 ) != 0 )
						{
							LOGGER.log( Level.FINEST, "x: " + x + " y " + y + " z " + z );
							IdPoint pt = calculateIntersection( x, y, z, 0 );
							long id = edgeId( x, y, z, 0 );
							LOGGER.log( Level.FINEST, " adding point with id: " + id );
							vertexMapping.put( id, pt );
						}
						if ( ( MarchingCubesTables.MC_EDGE_TABLE[ tableIndex ] & 256 ) != 0 )
						{
							LOGGER.log( Level.FINEST, "x: " + x + " y " + y + " z " + z );
							IdPoint pt = calculateIntersection( x, y, z, 8 );
							long id = edgeId( x, y, z, 8 );
							LOGGER.log( Level.FINEST, " adding point with id: " + id );
							vertexMapping.put( id, pt );
						}

						if ( x == xDim - 1 )
						{
							if ( ( MarchingCubesTables.MC_EDGE_TABLE[ tableIndex ] & 4 ) != 0 )
							{
								LOGGER.log( Level.FINEST, "x: " + x + " y " + y + " z " + z );
								IdPoint pt = calculateIntersection( x, y, z, 2 );
								long id = edgeId( x, y, z, 2 );
								LOGGER.log( Level.FINEST, " adding point with id: " + id );
								vertexMapping.put( id, pt );
							}
							if ( ( MarchingCubesTables.MC_EDGE_TABLE[ tableIndex ] & 2048 ) != 0 )
							{
								LOGGER.log( Level.FINEST, "x: " + x + " y " + y + " z " + z );
								IdPoint pt = calculateIntersection( x, y, z, 11 );
								long id = edgeId( x, y, z, 11 );
								LOGGER.log( Level.FINEST, " adding point with id: " + id );
								vertexMapping.put( id, pt );
							}
						}
						if ( y == yDim - 1 )
						{
							if ( ( MarchingCubesTables.MC_EDGE_TABLE[ tableIndex ] & 2 ) != 0 )
							{
								LOGGER.log( Level.FINEST, "x: " + x + " y " + y + " z " + z );
								IdPoint pt = calculateIntersection( x, y, z, 1 );
								long id = edgeId( x, y, z, 1 );
								LOGGER.log( Level.FINEST, " adding point with id: " + id );
								vertexMapping.put( id, pt );
							}
							if ( ( MarchingCubesTables.MC_EDGE_TABLE[ tableIndex ] & 512 ) != 0 )
							{
								LOGGER.log( Level.FINEST, "x: " + x + " y " + y + " z " + z );
								IdPoint pt = calculateIntersection( x, y, z, 9 );
								long id = edgeId( x, y, z, 9 );
								LOGGER.log( Level.FINEST, " adding point with id: " + id );
								vertexMapping.put( id, pt );
							}
						}
						if ( z == zDim - 1 )
						{
							if ( ( MarchingCubesTables.MC_EDGE_TABLE[ tableIndex ] & 16 ) != 0 )
							{
								LOGGER.log( Level.FINEST, "x: " + x + " y " + y + " z " + z );
								IdPoint pt = calculateIntersection( x, y, z, 4 );
								long id = edgeId( x, y, z, 4 );
								LOGGER.log( Level.FINEST, " adding point with id: " + id );
								vertexMapping.put( id, pt );
							}
							if ( ( MarchingCubesTables.MC_EDGE_TABLE[ tableIndex ] & 128 ) != 0 )
							{
								LOGGER.log( Level.FINEST, "x: " + x + " y " + y + " z " + z );
								IdPoint pt = calculateIntersection( x, y, z, 7 );
								long id = edgeId( x, y, z, 7 );
								LOGGER.log( Level.FINEST, " adding point with id: " + id );
								vertexMapping.put( id, pt );
							}
						}
						if ( ( x == xDim - 1 ) && ( y == yDim - 1 ) )
							if ( ( MarchingCubesTables.MC_EDGE_TABLE[ tableIndex ] & 1024 ) != 0 )
							{
								LOGGER.log( Level.FINEST, "x: " + x + " y " + y + " z " + z );
								IdPoint pt = calculateIntersection( x, y, z, 10 );
								long id = edgeId( x, y, z, 10 );
								LOGGER.log( Level.FINEST, " adding point with id: " + id );
								vertexMapping.put( id, pt );
							}
						if ( ( x == xDim - 1 ) && ( z == zDim - 1 ) )
							if ( ( MarchingCubesTables.MC_EDGE_TABLE[ tableIndex ] & 64 ) != 0 )
							{
								LOGGER.log( Level.FINEST, "x: " + x + " y " + y + " z " + z );
								IdPoint pt = calculateIntersection( x, y, z, 6 );
								long id = edgeId( x, y, z, 6 );
								LOGGER.log( Level.FINEST, " adding point with id: " + id );
								vertexMapping.put( id, pt );
							}
						if ( ( y == yDim - 1 ) && ( z == zDim - 1 ) )
							if ( ( MarchingCubesTables.MC_EDGE_TABLE[ tableIndex ] & 32 ) != 0 )
							{
								LOGGER.log( Level.FINEST, "x: " + x + " y " + y + " z " + z );
								IdPoint pt = calculateIntersection( x, y, z, 5 );
								long id = edgeId( x, y, z, 5 );
								LOGGER.log( Level.FINEST, " adding point with id: " + id );
								vertexMapping.put( id, pt );
							}

						for ( int i = 0; MarchingCubesTables.triTable[ tableIndex ][ i ] != MarchingCubesTables.Invalid; i += 3 )
						{
							Triangle triangle = new Triangle();
							int pointId0, pointId1, pointId2;
							pointId0 = edgeId( x, y, z, MarchingCubesTables.triTable[ tableIndex ][ i ] );
							pointId1 = edgeId( x, y, z, MarchingCubesTables.triTable[ tableIndex ][ i + 1 ] );
							pointId2 = edgeId( x, y, z, MarchingCubesTables.triTable[ tableIndex ][ i + 2 ] );
							LOGGER.log( Level.FINEST, "value on tritable: " + MarchingCubesTables.triTable[ tableIndex ][ i ]);
							LOGGER.log( Level.FINEST, "value on tritable: " + MarchingCubesTables.triTable[ tableIndex ][ i + 1 ]  );
							LOGGER.log( Level.FINEST, "value on tritable: " + MarchingCubesTables.triTable[ tableIndex ][ i + 2 ]  );
							LOGGER.log( Level.FINEST, "triangles point id: " + pointId0 + " " + pointId1 + " " + pointId2 );
							triangle.pointId[ 0 ] = pointId0;
							triangle.pointId[ 1 ] = pointId1;
							triangle.pointId[ 2 ] = pointId2;
							triangles.add( triangle );
						}
					}
				}

		Mesh mesh = reindex( triangles );

		calculateNormals( mesh );

		return mesh;
	}

	private int vertexId( int x, int y, int z )
	{
		return 3 * ( z * ( nCellsY + 1 ) * ( nCellsX + 1 ) + y * ( nCellsX + 1 ) + x );
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

	private IdPoint calculateIntersection( int x, int y, int z, int edgeNumber )
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
		intersection.x = ( x1 + x2 ) / 2;
		intersection.y = ( y1 + y2 ) / 2;
		intersection.z = ( z1 + z2 ) / 2;

		return intersection;
	}

	Mesh reindex( Vector< Triangle > triangles )
	{
		Iterator< Entry< Long, IdPoint > > mapIterator = vertexMapping.entrySet().iterator();
		Iterator< Triangle > vecIterator = triangles.iterator();

		LOGGER.log( Level.FINEST, "number of ids on map: " + vertexMapping.size() );
		// rename vertices
		int nextId = 0;
		while ( mapIterator.hasNext() )
		{
			HashMap.Entry< Long, IdPoint > entry = mapIterator.next();
			LOGGER.log( Level.FINEST, "old id: " + entry.getValue().id + " new id: " + nextId );
			entry.getValue().id = nextId;
			nextId++;
		}

		LOGGER.log( Level.FINEST, "number of triangles: " + triangles.size() );

		// rename triangles
		while ( vecIterator.hasNext() )
		{
			Triangle next = vecIterator.next();
			LOGGER.log( Level.FINEST, "getting triangle" );
			for ( int i = 0; i < 3; i++ )
			{
				LOGGER.log( Level.FINEST, "triangle point old id: " + next.pointId[ i ] );
				LOGGER.log( Level.FINEST, "id of id - new id: " + vertexMapping.get( next.pointId[ i ] ).id );
				if (vertexMapping.get( next.pointId[ i ] ) == null )
					continue;
				long newId = vertexMapping.get( next.pointId[ i ] ).id;
				next.pointId[ i ] = ( int ) newId;
			}
		}

//		for ( Triangle triangle : triangles )
//		{
//			System.out.println("triangle.pointId[0]: " + triangle.pointId[0]);
//			System.out.println("triangle.pointId[1]: " + triangle.pointId[1]);
//			System.out.println("triangle.pointId[2]: " + triangle.pointId[2]);
//			
//			System.out.println("vertexMapping.get( triangle.pointId[ 0 ] ): " +vertexMapping.get( triangle.pointId[ 0 ] ));
//			System.out.println("vertexMapping.get( triangle.pointId[ 0 ].id ): " +(vertexMapping.get( triangle.pointId[ 0 ]).id ));
//			
//			triangle.pointId[ 0 ] = vertexMapping.get( triangle.pointId[ 0 ] ).id;
//			triangle.pointId[ 1 ] = vertexMapping.get( triangle.pointId[ 1 ] ).id;
//			triangle.pointId[ 2 ] = vertexMapping.get( triangle.pointId[ 2 ] ).id;
//		}

		// Copy all the vertices and triangles into two arrays so that they
		// can be efficiently accessed.
		// Copy vertices.
		int vertexCount = vertexMapping.size();
		
		LOGGER.log( Level.FINEST, "created a mesh with " + vertexCount );
		float[][] vertices = new float[ vertexCount ][ 3 ];
		int index = 0;
		// point the iterator to the begin again
		mapIterator = vertexMapping.entrySet().iterator();
		while ( mapIterator.hasNext() )
		{
			HashMap.Entry< Long, IdPoint > entry = mapIterator.next();

			vertices[ index ][ 0 ] = entry.getValue().x;
			vertices[ index ][ 1 ] = entry.getValue().y;
			vertices[ index ][ 2 ] = entry.getValue().z;
			++index;
		}

		int triangleCount = triangles.size();
		long[] triangleIndices = new long[ triangleCount * 3 ];
		int i = 0;
		while ( vecIterator.hasNext() )
		//for ( int i = 0; i < triangleCount; i++ )
		{
			Triangle next = vecIterator.next();
			triangleIndices[ i * 3 ] = next.pointId[ 0 ];
			triangleIndices[ i * 3 + 1 ] = next.pointId[ 1 ];
			triangleIndices[ i * 3 + 2 ] = next.pointId[ 2 ];
			i++;
		}
//		index = 0;
//		for ( Triangle triangle : triangles )
//		{
//			triangleIndices[ index * 3 ] = triangle.pointId[ 0 ];
//			triangleIndices[ index * 3 + 1 ] = triangle.pointId[ 1 ];
//			triangleIndices[ index * 3 + 2 ] = triangle.pointId[ 2 ];
//			++index;
//		}

		vertexMapping.clear();
		triangles.clear();

		Mesh mesh = new Mesh();
		mesh.numberOfVertices = vertexCount;
		mesh.vertices = vertices;
		mesh.numberOfTriangles = triangleCount;
		mesh.triangles = triangleIndices;
		return mesh;
	}

	private void calculateNormals( Mesh mesh )
	{
		int vertexCount = mesh.numberOfVertices;
		float[][] vertices = mesh.vertices;
		int triangleCount = mesh.numberOfTriangles;
		long[] triangles = mesh.triangles;

		float[][] normals = new float[ vertexCount ][ 3 ];

		for ( int i = 0; i < vertexCount; ++i )
		{
			normals[ i ][ 0 ] = 0;
			normals[ i ][ 1 ] = 0;
			normals[ i ][ 2 ] = 1;
		}

		for ( int i = 0; i < triangleCount; ++i )
		{
			float[] vec1 = new float[ 3 ], vec2 = new float[ 3 ],
					normal = new float[ 3 ];
			int id0, id1, id2;
			id0 = ( int ) triangles[ i * 3 ];
			id1 = ( int ) triangles[ i * 3 + 1 ];
			id2 = ( int ) triangles[ i * 3 + 2 ];
			vec1[ 0 ] = vertices[ id1 ][ 0 ] - vertices[ id0 ][ 0 ];
			vec1[ 1 ] = vertices[ id1 ][ 1 ] - vertices[ id0 ][ 1 ];
			vec1[ 2 ] = vertices[ id1 ][ 2 ] - vertices[ id0 ][ 2 ];
			vec2[ 0 ] = vertices[ id2 ][ 0 ] - vertices[ id0 ][ 0 ];
			vec2[ 1 ] = vertices[ id2 ][ 1 ] - vertices[ id0 ][ 1 ];
			vec2[ 2 ] = vertices[ id2 ][ 2 ] - vertices[ id0 ][ 2 ];
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

		for ( int i = 0; i < vertexCount; ++i )
		{
			float length = ( float ) Math.sqrt( normals[ i ][ 0 ] * normals[ i ][ 0 ] + normals[ i ][ 1 ] * normals[ i ][ 1 ] + normals[ i ][ 2 ] * normals[ i ][ 2 ] );
			normals[ i ][ 0 ] /= length;
			normals[ i ][ 1 ] /= length;
			normals[ i ][ 2 ] /= length;
		}
		mesh.normals = normals;
	}
}

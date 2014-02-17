/*
 * GAMA - V1.4 http://gama-platform.googlecode.com
 * 
 * (c) 2007-2011 UMI 209 UMMISCO IRD/UPMC & Partners (see below)
 * 
 * Developers :
 * 
 * - Alexis Drogoul, UMI 209 UMMISCO, IRD/UPMC (Kernel, Metamodel, GAML), 2007-2012
 * - Vo Duc An, UMI 209 UMMISCO, IRD/UPMC (SWT, multi-level architecture), 2008-2012
 * - Patrick Taillandier, UMR 6228 IDEES, CNRS/Univ. Rouen (Batch, GeoTools & JTS), 2009-2012
 * - Beno�t Gaudou, UMR 5505 IRIT, CNRS/Univ. Toulouse 1 (Documentation, Tests), 2010-2012
 * - Phan Huy Cuong, DREAM team, Univ. Can Tho (XText-based GAML), 2012
 * - Pierrick Koch, UMI 209 UMMISCO, IRD/UPMC (XText-based GAML), 2010-2011
 * - Romain Lavaud, UMI 209 UMMISCO, IRD/UPMC (RCP environment), 2010
 * - Francois Sempe, UMI 209 UMMISCO, IRD/UPMC (EMF model, Batch), 2007-2009
 * - Edouard Amouroux, UMI 209 UMMISCO, IRD/UPMC (C++ initial porting), 2007-2008
 * - Chu Thanh Quang, UMI 209 UMMISCO, IRD/UPMC (OpenMap integration), 2007-2008
 * - Samuel Thiriot, 2012-2013
 */
package msi.gama.util.graph;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import msi.gama.common.interfaces.IKeyword;
import msi.gama.common.util.StringUtils;
import msi.gama.metamodel.agent.IAgent;
import msi.gama.metamodel.shape.GamaShape;
import msi.gama.metamodel.shape.ILocation;
import msi.gama.metamodel.shape.IShape;
import msi.gama.metamodel.topology.graph.FloydWarshallShortestPathsGAMA;
import msi.gama.metamodel.topology.graph.GamaSpatialGraph.VertexRelationship;
import msi.gama.runtime.GAMA;
import msi.gama.runtime.IScope;
import msi.gama.runtime.exceptions.GamaRuntimeException;
import msi.gama.util.GamaList;
import msi.gama.util.GamaMap;
import msi.gama.util.GamaPair;
import msi.gama.util.IContainer;
import msi.gama.util.IList;
import msi.gama.util.graph.GraphEvent.GraphEventType;
import msi.gama.util.matrix.GamaIntMatrix;
import msi.gama.util.matrix.GamaMatrix;
import msi.gama.util.matrix.IMatrix;
import msi.gama.util.path.IPath;
import msi.gama.util.path.PathFactory;
import msi.gaml.operators.Cast;
import msi.gaml.operators.Spatial.Creation;
import msi.gaml.species.ISpecies;

import org.graphstream.graph.implementations.MultiGraph;
import org.jgrapht.DirectedGraph;
import org.jgrapht.EdgeFactory;
import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.Graphs;
import org.jgrapht.UndirectedGraph;
import org.jgrapht.WeightedGraph;
import org.jgrapht.alg.BellmanFordShortestPath;
import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.alg.HamiltonianCycle;
import org.jgrapht.alg.KShortestPaths;
import org.jgrapht.alg.KruskalMinimumSpanningTree;
import org.jgrapht.graph.AsUndirectedGraph;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.jgrapht.util.VertexPair;

public class GamaGraph<V, E> implements IGraph<V, E> {

	protected final Map<V, _Vertex<E>> vertexMap;
	
	protected final Map<E, _Edge<V>> edgeMap;
	protected boolean directed;
	protected boolean edgeBased;
	protected boolean agentEdge;
	protected final IScope scope;
	protected Map<VertexPair<V>,GamaList<GamaList<E>>> shortestPathComputed = null;
	protected VertexRelationship vertexRelation;
	// protected IScope scope;

	public static int FloydWarshall = 1;
	public static int BellmannFord = 2;
	public static int Djikstra = 3;
	public static int AStar = 4;
	
	protected boolean saveComputedShortestPaths = true;

	protected ISpecies edgeSpecies;
	protected int optimizerType = Djikstra;
	private FloydWarshallShortestPathsGAMA<V, E> optimizer;

	private final LinkedList<IGraphEventListener> listeners = new LinkedList<IGraphEventListener>();

	private final Set<IAgent> generatedEdges = new HashSet<IAgent>();
	protected int version;

	public GamaGraph(final IScope scope, final boolean directed) {
		this.directed = directed;
		vertexMap = new GamaMap<V, _Vertex<E>>();
		edgeMap = new GamaMap<E, _Edge<V>>();
		edgeBased = false;
		vertexRelation = null;
		version = 1;
		agentEdge = false;
		this.scope = scope;
		shortestPathComputed = new GamaMap<VertexPair<V>, GamaList<GamaList<E>>>();
	}

	public GamaGraph(final IContainer edgesOrVertices, final boolean byEdge, final boolean directed,
		final VertexRelationship rel, final ISpecies edgesSpecies, final IScope scope) {
		vertexMap = new GamaMap();
		edgeMap = new GamaMap();
		shortestPathComputed = new GamaMap<VertexPair<V>, GamaList<GamaList<E>>>();
		this.scope = scope;
		init(scope, edgesOrVertices, byEdge, directed, rel, edgesSpecies);
	}

	public GamaGraph(final IScope scope) {
		vertexMap = new GamaMap();
		edgeMap = new GamaMap();
		shortestPathComputed = new GamaMap<VertexPair<V>,GamaList<GamaList<E>>>();
		this.scope = scope;
	}

	protected void init(final IScope scope, final IContainer edgesOrVertices, final boolean byEdge,
		final boolean directed, final VertexRelationship rel, final ISpecies edgesSpecies) {
		this.directed = directed;
		edgeBased = byEdge;
		vertexRelation = rel;
		edgeSpecies = edgesSpecies;
		agentEdge =
			edgesSpecies != null || byEdge && edgesOrVertices != null && edgesOrVertices.first(scope) instanceof IAgent;
		if ( byEdge ) {
			buildByEdge(scope, edgesOrVertices);
		} else {
			buildByVertices(scope, edgesOrVertices);
		}
		version = 1;
	}

	@Override
	public String toString() {

		final StringBuffer sb = new StringBuffer();

		// display the list of verticies
		sb.append("graph { \nvertices (").append(vertexSet().size()).append("): ").append("[");
		for ( final Object v : vertexSet() ) {
			sb.append(v.toString()).append(",");
		}
		sb.append("]\n");
		sb.append("edges (").append(edgeSet().size()).append("): [\n");
		// display each edge
		for ( final Entry<E, _Edge<V>> entry : edgeMap.entrySet() ) {
			final E e = entry.getKey();
			final _Edge<V> v = entry.getValue();
			sb.append(e.toString()).append("\t(").append(v.toString()).append("),\n");
		}
		sb.append("]\n}");
		/*
		 * old aspect, kept if someone prefers this one.
		 * List<String> renderedVertices = new ArrayList<String>();
		 * List<String> renderedEdges = new ArrayList<String>();
		 * StringBuffer sb = new StringBuffer();
		 * for ( Object e : edgeSet() ) {
		 * sb.append(e.toString()).append("=(").append(getEdgeSource(e)).append(",")
		 * .append(getEdgeTarget(e)).append(")");
		 * renderedEdges.add(sb.toString());
		 * sb.setLength(0);
		 * }
		 * for ( Object v : vertexSet() ) {
		 * sb.append(v.toString()).append(": in").append(incomingEdgesOf(v)).append(" + out")
		 * .append(outgoingEdgesOf(v));
		 * renderedVertices.add(sb.toString());
		 * sb.setLength(0);
		 * }
		 */
		return sb.toString();
		// return "(" + renderedVertices + ", " + renderedEdges + ")";
	}

	protected void buildByVertices(final IScope scope, final IContainer<?, E> vertices) {
		for ( final E p : vertices.iterable(scope) ) {
			addVertex(p);
		}
	}

	protected void buildByEdge(final IScope scope, final IContainer vertices) {
		for ( final Object p : vertices.iterable(scope) ) {
			addEdge(p);
			if ( p instanceof IShape ) {
				getEdge(p).setWeight(((IShape) p).getPerimeter());
			}
		}
	}

	protected void buildByEdge(final IScope scope, final IContainer edges, final IContainer vertices) {}

	public _Edge<V> getEdge(final Object e) {
		return edgeMap.get(e);
	}

	public _Vertex<E> getVertex(final Object v) {
		return vertexMap.get(v);
	}

	@Override
	public Object addEdge(final Object e) {
		if ( e instanceof GamaPair ) {
			final GamaPair p = (GamaPair) e;
			return addEdge(p.first(), p.last());
		}
		return addEdge(null, null, e) ? e : null;

	}

	@Override
	public Object addEdge(final Object v1, final Object v2) {
		if ( v1 instanceof GamaPair ) {
			final GamaPair p = (GamaPair) v1;
			if ( addEdge(p.first(), p.last(), v2) ) { return v2; }
			return null;
		}
		final Object p = createNewEdgeObjectFromVertices(v1, v2);

		if ( addEdge(v1, v2, p) ) { return p; }
		return null;
	}

	protected Object createNewEdgeObjectFromVertices(final Object v1, final Object v2) {
		if ( edgeSpecies == null ) { return generateEdgeObject(v1, v2); }
		final Map<String, Object> map = new GamaMap();
		final IList initVal = new GamaList();
		map.put(IKeyword.SOURCE, v1);
		map.put(IKeyword.TARGET, v2);
		map.put(IKeyword.SHAPE, Creation.link(scope, new GamaPair(v1, v2)));
		initVal.add(map);
		return generateEdgeAgent(initVal);
	}

	protected Object generateEdgeObject(final Object v1, final Object v2) {
		return new GamaPair(v1, v2);
	}

	protected IAgent generateEdgeAgent(final List<Map> attributes) {
		final IAgent agent =
			scope.getAgentScope().getPopulationFor(edgeSpecies).createAgents(scope, 1, attributes, false).first(scope);
		if ( agent != null ) {
			generatedEdges.add(agent);
		}
		return agent;
	}

	@Override
	public boolean addEdge(final Object v1, final Object v2, final Object e) {
		if ( containsEdge(e) ) { return false; }
		addVertex(v1);
		addVertex(v2);
		_Edge<V> edge;
		try {
			edge = newEdge(e, v1, v2);
		} catch (final GamaRuntimeException e1) {
			e1.addContext("Impossible to create edge from " + StringUtils.toGaml(e) + " in graph " + this);
			throw e1;
		}
		if ( edge == null ) { return false; }
		edgeMap.put((E) e, edge);
		dispatchEvent(new GraphEvent(scope, this, this, e, null, GraphEventType.EDGE_ADDED));
		return true;

	}

	protected _Edge<V> newEdge(final Object e, final Object v1, final Object v2) throws GamaRuntimeException {
		return new _Edge(this, e, v1, v2);
	}

	protected _Vertex<E> newVertex(final Object v) throws GamaRuntimeException {
		return new _Vertex<E>(this);
	}

	@Override
	public boolean addVertex(final Object v) {
		if ( v == null || containsVertex(v) ) { return false; }
		_Vertex<E> vertex;
		try {
			vertex = newVertex(v);
		} catch (final GamaRuntimeException e) {
			e.addContext("Impossible to create vertex from " + StringUtils.toGaml(v) + " in graph " + this);
			throw e;
		}
		if ( vertex == null ) { return false; }
		vertexMap.put((V) v, vertex);
		dispatchEvent(new GraphEvent(scope, this, this, null, v, GraphEventType.VERTEX_ADDED));
		return true;

	}

	@Override
	public boolean containsEdge(final Object e) {
		return edgeMap.containsKey(e);
	}

	@Override
	public boolean containsEdge(final Object v1, final Object v2) {
		return getEdge(v1, v2) != null || !directed && getEdge(v2, v1) != null;
	}

	@Override
	public boolean containsVertex(final Object v) {
		return vertexMap.containsKey(v);
	}

	@Override
	public Set edgeSet() {
		return edgeMap.keySet();
	}

	@Override
	public Collection _internalEdgeSet() {
		return edgeMap.values();
	}

	@Override
	public Collection _internalNodesSet() {
		return edgeMap.values();
	}

	@Override
	public Map<E, _Edge<V>> _internalEdgeMap() {
		return edgeMap;
	}

	@Override
	public Map<V, _Vertex<E>> _internalVertexMap() {
		return vertexMap;
	}

	@Override
	public Set edgesOf(final Object vertex) {
		final _Vertex<E> v = getVertex(vertex);
		return v == null ? Collections.EMPTY_SET : v.getEdges();
	}

	@Override
	public Set getAllEdges(final Object v1, final Object v2) {
		final Set s = new HashSet();
		if ( !containsVertex(v1) || !containsVertex(v2) ) { return s; }
		s.addAll(getVertex(v1).edgesTo(v2));
		if ( !directed ) {
			s.addAll(getVertex(v2).edgesTo(v1));
		}
		return s;
	}

	@Override
	public Object getEdge(final Object v1, final Object v2) {
		if ( !containsVertex(v1) || !containsVertex(v2) ) { return null; }
		final Object o = getVertex(v1).edgeTo(v2);
		return o == null && !directed ? getVertex(v2).edgeTo(v1) : o;
	}

	@Override
	public EdgeFactory getEdgeFactory() {
		return null; // NOT USED
	}

	@Override
	public Object getEdgeSource(final Object e) {
		if ( !containsEdge(e) ) { return null; }
		return getEdge(e).getSource();
	}

	@Override
	public Object getEdgeTarget(final Object e) {
		if ( !containsEdge(e) ) { return null; }
		return getEdge(e).getTarget();
	}

	@Override
	public double getEdgeWeight(final Object e) {
		if ( !containsEdge(e) ) { return WeightedGraph.DEFAULT_EDGE_WEIGHT; }
		return getEdge(e).getWeight(e);
	}

	@Override
	public double getVertexWeight(final Object v) {
		if ( !containsVertex(v) ) { return WeightedGraph.DEFAULT_EDGE_WEIGHT; }
		return getVertex(v).getWeight(v);
	}

	@Override
	public Double getWeightOf(final Object v) {
		if ( containsVertex(v) ) { return getVertexWeight(v); }
		if ( containsEdge(v) ) { return getEdgeWeight(v); }
		return null;
	}

	@Override
	public Set incomingEdgesOf(final Object vertex) {
		final _Vertex<E> v = getVertex(vertex);
		return v == null ? Collections.EMPTY_SET : v.inEdges;
	}

	@Override
	public int inDegreeOf(final Object vertex) {
		return incomingEdgesOf(vertex).size();
	}

	@Override
	public int outDegreeOf(final Object vertex) {
		return outgoingEdgesOf(vertex).size();
	}

	@Override
	public int degreeOf(final Object v) {
		return inDegreeOf(v) + outDegreeOf(v);
	}

	@Override
	public Set outgoingEdgesOf(final Object vertex) {
		final _Vertex<E> v = getVertex(vertex);
		return v == null ? Collections.EMPTY_SET : v.outEdges;
	}

	@Override
	public boolean removeAllEdges(final Collection edges) {
		boolean result = false;
		for ( final Object e : edges ) {
			result = result || removeEdge(e);
		}
		return result;
	}

	@Override
	public Set removeAllEdges(final Object v1, final Object v2) {
		final Set result = new HashSet();
		Object edge = removeEdge(v1, v2);
		while (edge != null) {
			result.add(edge);
			edge = removeEdge(v1, v2);
		}
		if ( !directed ) {
			edge = removeEdge(v2, v1);
			while (edge != null) {
				result.add(edge);
				edge = removeEdge(v2, v1);
			}
		}
		return result;
	}

	@Override
	public boolean removeAllVertices(final Collection vertices) {
		boolean result = false;
		for ( final Object o : vertices.toArray() ) {
			result = result || removeVertex(o);
		}
		return result;
	}

	@Override
	public boolean removeEdge(final Object e) {
		if ( e == null ) { return false; }
		final _Edge<V> edge = getEdge(e);
		if ( edge == null && e instanceof GamaPair ) { return removeEdge(((GamaPair) e).first(), ((GamaPair) e).last()) != null; }

		if ( edge == null ) { return false; }
		edge.removeFromVerticesAs(e);
		edgeMap.remove(e);
		if ( generatedEdges.contains(e) ) {
			((IAgent) e).dispose();
		}
		dispatchEvent(new GraphEvent(scope, this, this, e, null, GraphEventType.EDGE_REMOVED));
		return true;
	}

	@Override
	public Object removeEdge(final Object v1, final Object v2) {
		final Object edge = getEdge(v1, v2);
		if ( removeEdge(edge) ) { return edge; }
		return null;

	}

	@Override
	public boolean removeVertex(final Object v) {
		if ( !containsVertex(v) ) { return false; }
		final Set edges = edgesOf(v);
		for ( final Object e : edges ) {
			removeEdge(e);
		}

		vertexMap.remove(v);
		dispatchEvent(new GraphEvent(scope, this, this, null, v, GraphEventType.VERTEX_REMOVED));
		return true;
	}

	@Override
	public void setEdgeWeight(final Object e, final double weight) {
		if ( !containsEdge(e) ) { return; }
		getEdge(e).setWeight(weight);
	}

	@Override
	public void setVertexWeight(final Object v, final double weight) {
		if ( !containsVertex(v) ) { return; }
		getVertex(v).setWeight(weight);
	}

	@Override
	public Set vertexSet() {
		return vertexMap.keySet();
	}

	@Override
	public void setOptimizerType(final String s) {
		if ( "AStar".equals(s) ) {
			optimizerType = 4;
		} else if ( "Djikstra".equals(s) ) {
			optimizerType = 3;
		} else if ( "Bellmann".equals(s) ) {
			optimizerType = 2;
		} else {
			optimizerType = 1;
		}
	}

	// protected IPath<V,E> pathFromEdges(final Object source, final Object target, final IList<E> edges) {
	protected IPath<V, E, IGraph<V, E>> pathFromEdges(final V source, final V target, final IList<E> edges) {
		// return new GamaPath(this, source, target, edges);
		return PathFactory.newInstance(this, source, target, edges);
	}

	@Override
	// public IPath<V,E> computeShortestPathBetween(final Object source, final Object target) {
	public IPath<V, E, IGraph<V, E>> computeShortestPathBetween(final V source, final V target) {
		return pathFromEdges(source, target, computeBestRouteBetween(source, target));
	}

	@Override
	public IList<E> computeBestRouteBetween(final V source, final V target) {

		switch (optimizerType) {
			case 1:
				if ( optimizer == null ) {
					optimizer = new FloydWarshallShortestPathsGAMA<V, E>(this);
				}
				GraphPath<V, E> path = optimizer.getShortestPath(source, target);
				if (path == null) return new GamaList<E>();
				return new GamaList<E>((Iterable) path.getEdgeList());
			case 2:
				VertexPair<V> nodes1 = new VertexPair<V>(source, target);
				GamaList<GamaList<E>> sp1 = shortestPathComputed.get(nodes1);
				GamaList<E> spl1 = null;
				if (sp1 == null || sp1.isEmpty()) {
					spl1 = new GamaList<E>();
					final BellmanFordShortestPath<V, E> p1 = new BellmanFordShortestPath<V, E>(getProxyGraph(), source);
					List<E> re = p1.getPathEdgeList(target);
					if (re == null) spl1 = new GamaList<E>();
					else spl1 = new GamaList<E>(re);
					if (saveComputedShortestPaths) {
						saveShortestPaths(spl1,source,target);
					}
				} else {
					spl1 = new GamaList<E>(sp1.get(0));
				}
				return spl1;
			case 3:
				//long t1 = java.lang.System.currentTimeMillis();
				VertexPair<V> nodes2 = new VertexPair<V>(source, target);
				//System.out.println("nodes2 : " + nodes2);
				GamaList<GamaList<E>> sp2 = shortestPathComputed.get(nodes2);
				GamaList<E> spl2 = null;
				
				if (sp2 == null || sp2.isEmpty()) {
					spl2 = new GamaList<E>();
					
					try {
						final DijkstraShortestPath<GamaShape, GamaShape> p2 =
								new DijkstraShortestPath(getProxyGraph(), source, target);
						List re = p2.getPathEdgeList();
						if (re == null) spl2 = new GamaList<E>();
						else spl2 = new GamaList<E>(re);
						
					} catch (IllegalArgumentException e) {
						spl2 = new GamaList<E>();
					}
					if (saveComputedShortestPaths) {
						saveShortestPaths(spl2,source,target);
					}
				} else {
					spl2 = new GamaList<E>(sp2.get(0));
				}
				//java.lang.System.out.println("DijkstraShortestPath : " + (java.lang.System.currentTimeMillis() - t1 ));
				return spl2;
			case 4: 
				//t1 = java.lang.System.currentTimeMillis();
				
				VertexPair<V> nodes3 = new VertexPair<V>(source, target);
				GamaList<GamaList<E>> sp3 = shortestPathComputed.get(nodes3);
				GamaList<E> spl3 = null;
				if (sp3 == null || sp3.isEmpty()) {
					spl3 = new GamaList<E>();
					msi.gama.metamodel.topology.graph.AStar astarAlgo = new msi.gama.metamodel.topology.graph.AStar(this, source, target);
					astarAlgo.compute();
					spl3 = new GamaList<E>(astarAlgo.getShortestPath());
					if (saveComputedShortestPaths) {
						saveShortestPaths(spl3,source,target);
					}
					
				} else {
					spl3 = new GamaList<E>(sp3.get(0));
				}
				
				//java.lang.System.out.println("ASTAR : " + (java.lang.System.currentTimeMillis() - t1 ));
				return spl3;
				
				
		}
		return new GamaList<E>();

	}
	
	private void saveShortestPaths(List<E> edges, V source, V target){
		V s = source;
		GamaList<GamaList<E>> spl = new GamaList<GamaList<E>>();
		spl.add(new GamaList<E>(edges));
		shortestPathComputed.put(new VertexPair<V>(source, target), spl);
		List<E> edges2 = new GamaList<E>(edges);
		for (E edge: edges) {
			edges2.remove(0);
			//System.out.println("s : " + s + " j : " + j + " i: " + i);
			V nwS = (V) this.getEdgeTarget(edge);
			if (! directed && nwS == s) {
				nwS = (V) this.getEdgeSource(edge);
			}
			VertexPair<V> pp = new VertexPair<V>(nwS, target);
			if (!shortestPathComputed.containsKey(pp)) {
				GamaList<GamaList<E>> spl2 = new GamaList<GamaList<E>>();
				spl2.add(new GamaList<E>(edges2));	
				shortestPathComputed.put(pp, spl2);
			}
			s = nwS;
			if (edges2.isEmpty()) break;
		}
	}
	
	@Override
	public IList<IPath<V, E, IGraph<V, E>>> computeKShortestPathsBetween(final V source, final V target, int k) {
		final IList<IList<E>>  pathLists = computeKBestRoutesBetween(source, target, k); 
		IList<IPath<V, E, IGraph<V, E>>> paths = new GamaList<IPath<V,E,IGraph<V,E>>>();
		
		for (IList<E> p : pathLists) {
			paths.add(pathFromEdges(source, target,  p));
		}
		return paths;
	}
	
	@Override
	public IList<IList<E>> computeKBestRoutesBetween(final V source, final V target,int k) {
		VertexPair<V> pp = new VertexPair<V>(source, target);
		IList<IList<E>> paths = new GamaList<IList<E>> ();
		GamaList<GamaList<E>> sps = shortestPathComputed.get(pp);
		if (sps != null && sps.size() >= k) {
			for (GamaList<E> sp : sps) {
				paths.add(new GamaList<E>(sp));
			}
		} else {
			final KShortestPaths<V, E> kp = new KShortestPaths<V, E>(getProxyGraph(), source, k);
			List<GraphPath<V, E>> pathsJGT = kp.getPaths(target);
			GamaList<GamaList<E>> el = new GamaList<GamaList<E>>();
			for (GraphPath<V, E> p : pathsJGT) {
				paths.add(new GamaList(p.getEdgeList()));
				if (saveComputedShortestPaths) {
					el.add(new GamaList<E>(p.getEdgeList()));
				}
			}
			if (saveComputedShortestPaths)
				shortestPathComputed.put(pp, el);
		}
		return paths;
	}

	protected Graph<V, E> getProxyGraph() {
		return directed ? this : new AsUndirectedGraph<V, E>(this);
	}

	@Override
	public IList<E> listValue(final IScope scope) {
		// TODO V�rifier ceci.
		final GamaList list = edgeBased ? new GamaList(edgeSet()) : new GamaList(vertexSet());
		return list;
	}

	@Override
	public String stringValue(final IScope scope) {
		return toString();
	}

	@Override
	public IMatrix matrixValue(final IScope scope) {
		// TODO Representation of the graph as a matrix ?
		return null;
	}

	@Override
	public IMatrix matrixValue(final IScope scope, final ILocation preferredSize) {
		// TODO Representation of the graph as a matrix ?
		return null;
	}

	@Override
	public String toGaml() {
		return mapValue(null).toGaml() + " as graph";
	}

	@Override
	public GamaMap mapValue(final IScope scope) {
		final GamaMap m = new GamaMap();
		for ( final Object edge : edgeSet() ) {
			m.add(scope, new GamaPair(getEdgeSource(edge), getEdgeTarget(edge)), edge, null, false, false);
		}
		return m;
	}

	// @Override
	// public Iterator<E> iterator() {
	// return listValue(null).iterator();
	// }

	@Override
	public void add(final IScope scope, final Object index, final Object value, final Object param, final boolean all,
		final boolean add) throws GamaRuntimeException {
		final double weight = param == null ? DEFAULT_EDGE_WEIGHT : Cast.asFloat(scope, param);
		if ( index == null ) {
			if ( all ) {
				if ( value instanceof GamaGraph ) {
					for ( final Object o : ((GamaGraph) value).edgeSet() ) {
						addEdge(o);
					}
				} else if ( value instanceof IContainer ) {
					for ( final Object o : ((IContainer) value).iterable(scope) ) {
						this.add(scope, null, o, param, false, true);
					}
				} else { // value != container
					// TODO Runtime exception
				}
			} else if ( value instanceof GamaPair ) {
				final Object v = addEdge(((GamaPair) value).getKey(), ((GamaPair) value).getValue());
				setEdgeWeight(v, weight);
			} else {
				addVertex(value);
				setVertexWeight(value, weight);
			}
		} else { // index != null
			if ( index instanceof GamaPair ) {
				addEdge(((GamaPair) index).getKey(), ((GamaPair) index).getValue(), value);
				setEdgeWeight(value, weight);
			}
		}
	}

	@Override
	public Object get(final IScope scope, final Object index) {
		if ( index instanceof GamaPair ) { return getEdge(((GamaPair) index).first(), ((GamaPair) index).last()); }
		if ( containsVertex(index) ) { return new GamaList(edgesOf(index)); }
		if ( containsEdge(index) ) { return new GamaPair(getEdgeSource(index), getEdgeTarget(index)); }
		return null;
	}

	@Override
	public Object getFromIndicesList(final IScope scope, final IList indices) throws GamaRuntimeException {
		if ( indices == null || indices.isEmpty(scope) ) { return null; }
		return get(scope, indices.first(scope));
		// Maybe we should consider the case where two indices that represent vertices are passed
		// (instead of a pair).
	}

	@Override
	public boolean contains(final IScope scope, final Object o) {
		return containsVertex(o) || containsEdge(o);
	}

	@Override
	public E first(final IScope scope) {
		final Iterator it = iterable(scope).iterator();
		if ( it.hasNext() ) { return (E) it.next(); }
		return null;
	}

	@Override
	public E last(final IScope scope) {
		// Solution d�bile. On devrait conserver le dernier entr�.
		return new GamaList<E>(vertexSet()).last(scope); // Attention a l'ordre
	}

	@Override
	public int length(final IScope scope) {
		return edgeBased ? edgeSet().size() : vertexSet().size(); // ??
	}

	@Override
	public boolean isEmpty(final IScope scope) {
		return edgeSet().isEmpty() && vertexSet().isEmpty();
	}

	@Override
	public void remove(final IScope scope, final Object index, final Object value, final boolean all) {
		if ( index == null ) {
			if ( all ) {
				if ( value instanceof IContainer ) {
					for ( final Object obj : ((IContainer) value).iterable(scope) ) {
						remove(scope, null, obj, true);
					}
				} else if ( value != null ) {
					remove(scope, null, value, false);
				} else {
					vertexSet().clear();
				}
			} else if ( !removeVertex(value) ) {
				removeEdge(value);
			}
		} else {
			// TODO if value != null ?
			// EX: remove edge1 at: v1::v2 in case of several edges between vertices
			removeEdge(index);
		}
	}

	@Override
	public GamaGraph reverse(final IScope scope) {
		final GamaGraph g = new GamaGraph(new GamaList(), false, directed, vertexRelation, edgeSpecies, scope);
		Graphs.addGraphReversed(g, this);
		return g;
	}

	@Override
	public IList getEdges() {
		return new GamaList(edgeSet());
	}

	@Override
	public IList getVertices() {
		return new GamaList(vertexSet());
	}

	@Override
	public IList getSpanningTree() {
		final KruskalMinimumSpanningTree tree = new KruskalMinimumSpanningTree(this);
		return new GamaList(tree.getEdgeSet());
	}

	@Override
	public IPath getCircuit() {
		final SimpleWeightedGraph g = new SimpleWeightedGraph(getEdgeFactory());
		Graphs.addAllEdges(g, this, edgeSet());
		final List vertices = HamiltonianCycle.getApproximateOptimalForCompleteGraph(g);
		final int size = vertices.size();
		final IList edges = new GamaList();
		for ( int i = 0; i < size - 1; i++ ) {
			edges.add(this.getEdge(vertices.get(i), vertices.get(i + 1)));
		}
		return pathFromEdges(null, null, edges);
	}

	@Override
	public Boolean getConnected() {
		ConnectivityInspector c;
		if ( directed ) {
			c = new ConnectivityInspector((DirectedGraph) this);
		} else {
			c = new ConnectivityInspector((UndirectedGraph) this);
		}
		return c.isGraphConnected();
	}

	@Override
	public boolean isDirected() {
		return directed;
	}

	@Override
	public void setDirected(final boolean b) {
		directed = b;
	}

	@Override
	public IGraph copy(final IScope scope) {
		final GamaGraph g = new GamaGraph(GamaList.EMPTY_LIST, true, directed, vertexRelation, edgeSpecies, scope);
		Graphs.addAllEdges(g, this, this.edgeSet());
		return g;
	}

	@Override
	public boolean checkBounds(final Object index, final boolean forAdding) {
		return true;
	}

	@Override
	public void setWeights(final Map w) {
		final Map<Object, Double> weights = w;
		for ( final Map.Entry<Object, Double> entry : weights.entrySet() ) {
			Object target = entry.getKey();
			if ( target instanceof GamaPair ) {
				target = getEdge(((GamaPair) target).first(), ((GamaPair) target).last());
				setEdgeWeight(target, entry.getValue());
			} else {
				if ( containsEdge(target) ) {
					setEdgeWeight(target, entry.getValue());
				} else {
					setVertexWeight(target, entry.getValue());
				}
			}
		}

	}

	/**
	 * @see msi.gama.interfaces.IGamaContainer#any()
	 */
	@Override
	public E any(final IScope scope) {
		if ( vertexMap.isEmpty() ) { return null; }
		final E[] array = (E[]) vertexMap.keySet().toArray();
		final int i = GAMA.getRandom().between(0, array.length - 1);
		return array[i];
	}

	@Override
	public void addListener(final IGraphEventListener listener) {
		synchronized (listeners) {
			if ( !listeners.contains(listener) ) {
				listeners.add(listener);
			}
		}

	}

	@Override
	public void removeListener(final IGraphEventListener listener) {
		synchronized (listeners) {
			listeners.remove(listener);
		}
	}

	@Override
	public void dispatchEvent(final GraphEvent event) {
		synchronized (listeners) {
			if ( listeners.isEmpty() ) { return; }
			for ( final IGraphEventListener l : listeners ) {
				l.receiveEvent(event);
			}
		}
	}

	@Override
	public int getVersion() {
		return version;
	}

	@Override
	public void setVersion(final int version) {
		this.version = version;
		shortestPathComputed.clear();
	}

	@Override
	public void incVersion() {
		version++;
		shortestPathComputed.clear();
	}

	@Override
	public Iterable<E> iterable(final IScope scope) {
		return listValue(scope);
	}

	@Override
	public double computeWeight(final IPath gamaPath) {
		double result = 0;
		final List l = gamaPath.getEdgeList();
		for ( final Object o : l ) {
			result += getEdgeWeight(o);
		}
		return result;
	}

	@Override
	public double computeTotalWeight() {
		double result = 0;
		for ( final Object o : edgeSet() ) {
			result += getEdgeWeight(o);
		}
		for ( final Object o : vertexSet() ) {
			result += getEdgeWeight(o);
		}
		return result;
	}

	public void reInitPathFinder() {
		optimizer = null;
	}

	public boolean isAgentEdge() {
		return agentEdge;
	}
	
	public boolean isSaveComputedShortestPaths() {
		return saveComputedShortestPaths;
	}

	public void setSaveComputedShortestPaths(boolean saveComputedShortestPaths) {
		this.saveComputedShortestPaths = saveComputedShortestPaths;
	}

	public FloydWarshallShortestPathsGAMA<V, E> getOptimizer() {
		return optimizer;
	}

	public void setOptimizer(FloydWarshallShortestPathsGAMA<V, E> optimizer) {
		this.optimizer = optimizer;
	}

	public void loadShortestPaths(GamaMatrix matrix) {
		GamaList<V> vertices = (GamaList<V>) getVertices();
		int nbvertices = matrix.numCols;
		shortestPathComputed = new GamaMap<VertexPair<V>, GamaList<GamaList<E>>>();
		GamaIntMatrix mat = null;
		if (matrix instanceof GamaIntMatrix) 
			mat = (GamaIntMatrix) matrix;
		else {
			mat = new GamaIntMatrix(matrix);
		}
		Map<Integer,E> edgesVertices = new GamaMap<Integer,E>();
		for (int i = 0; i < nbvertices; i++) {
			V v1 = vertices.get(i);
			for (int j = 0; j < nbvertices; j++) {
				V v2 = vertices.get(j);
				VertexPair<V> vv = new VertexPair<V>(v1, v2);
				GamaList<E> edges = new GamaList<E>();
				if (v1 == v2) {
					GamaList<GamaList<E>> spl  = new GamaList<GamaList<E>>();
					spl.add(edges);
					shortestPathComputed.put(vv, spl);
					continue;
				}
				V vs = v1;
				int previous = i;
				Integer next =  mat.get(scope, j, i);
				if(i == next) {
					GamaList<GamaList<E>> spl  = new GamaList<GamaList<E>>();
					spl.add(edges);
					shortestPathComputed.put(vv, spl);
					continue;
				}
				do  {
					V vn = (V) vertices.get(next);
					Integer id = (previous * nbvertices) + next;
					E edge = edgesVertices.get(id);
					if (edge == null) {
						Set<E> eds = this.getAllEdges(vs, vn);
						for (E ed: eds) {
							if (edge == null || getEdgeWeight(ed) < getEdgeWeight(edge)) {
								edge = ed;
							}
						}
						edgesVertices.put(id, edge);
					}
					if (edge == null) break;
					edges.add(edge);
					previous = next;
					next = mat.get(scope, j,next);
					
					vs = vn;
				}while (previous != j);
				GamaList<GamaList<E>> spl  = new GamaList<GamaList<E>>();
				spl.add(edges);
				shortestPathComputed.put(vv, spl);
			}	
		}
	}
	
	public GamaIntMatrix saveShortestPaths() {
		GamaMap<V, Integer> indexVertices = new GamaMap<V, Integer>();
		GamaList<V> vertices = (GamaList<V>) getVertices();
		
		for (int i= 0; i < vertexMap.size(); i++) {
			indexVertices.put(vertices.get(i), i);
		}
		GamaIntMatrix matrix = new GamaIntMatrix(vertices.size(), vertices.size());
		for (int i = 0; i < vertices.size(); i++) {
			for (int j = 0; j < vertices.size(); j++) {
				matrix.set(scope, j,i, i);
			}
		}
		if (optimizer != null) {
			for (int i = 0; i < vertices.size(); i++) {
				V v1 = (V) vertices.get(i);
				for (int j = 0; j < vertices.size(); j++) {
					V v2 = vertices.get(j);
					GraphPath<V, E> path = optimizer.getShortestPath(v1, v2);
					if (path == null || path.getEdgeList() == null || path.getEdgeList().isEmpty()) 
						continue;
					matrix.set(scope, j,i, nextVertice(path.getEdgeList().get(0), v1, indexVertices, directed));
				}
			}
		} else {
			for (int i = 0; i < vertexMap.size(); i++) {
				V v1 = vertices.get(i);
				for (int j = 0; j < vertexMap.size(); j++) {
					if (i == j) continue;
					if (matrix.get(scope, j,i) != i) continue;
					V v2 = vertices.get(j);
					List edges = computeBestRouteBetween(v1, v2);
					//System.out.println("edges : " + edges);
					if (edges == null)
						continue;
					V source = v1;
					int s = i;
					for (Object edge: edges) {
						//System.out.println("s : " + s + " j : " + j + " i: " + i);
						if (s != i  && (matrix.get(scope, j,s) != s)) break;
						
						V target = (V) this.getEdgeTarget(edge);
						if (! directed && target == source) {
							target = (V) this.getEdgeSource(edge);
						}
						Integer k = (Integer) indexVertices.get(scope,target);
					//	System.out.println("k : " +k);
						matrix.set(scope, j,s, k);
						s = k;
						source = target;
					}
					
				}
			}
		}
		return matrix;
		
	}
	
	
	private Integer nextVertice(E edge, V source, GamaMap<V, Integer> indexVertices, boolean isDirected) {
		if (isDirected)
			return (Integer) indexVertices.get(scope, this.getEdgeTarget(edge));

		V target = (V) this.getEdgeTarget(edge);
		if (target != source) {
			source = target;
			return (Integer) indexVertices.get(scope, target);
		}
		source = (V) this.getEdgeSource(edge);
		return (Integer) indexVertices.get(scope, source);
	}

	public Map<VertexPair<V>, GamaList<GamaList<E>>> getShortestPathComputed() {
		return shortestPathComputed;
	}
	
	public Map<V, _Vertex<E>> getVertexMap() {
		return vertexMap;
	}

	
	
}

package jcfgonc.mapper.structures;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

import graph.DirectedMultiGraph;
import graph.GraphReadWrite;
import structures.OrderedPair;

/**
 * Contains the mapping graph and starting reference pair of concepts.
 * 
 * @author jcfgonc@gmail.com
 *
 * @param <V>
 * @param <E>
 */
public class MappingStructure<V, E> {

	private DirectedMultiGraph<OrderedPair<V>, E> pairGraph;
	private OrderedPair<V> referencePair;

	public MappingStructure(OrderedPair<V> referencePair, DirectedMultiGraph<OrderedPair<V>, E> pairGraph) {
		super();
		this.referencePair = referencePair;
		this.pairGraph = pairGraph;
	}

	public MappingStructure(OrderedPair<V> referencePair) {
		this(referencePair, null);
	}

	public MappingStructure() {
		this(null, null);
	}

	public DirectedMultiGraph<OrderedPair<V>, E> getPairGraph() {
		return pairGraph;
	}

	public OrderedPair<V> getReferencePair() {
		return referencePair;
	}

	public void setPairGraph(DirectedMultiGraph<OrderedPair<V>, E> pairGraph) {
		this.pairGraph = pairGraph;
	}

	public void setReferencePair(OrderedPair<V> refPair) {
		this.referencePair = refPair;
	}

	public void writeTGF(String filename) throws IOException {
		GraphReadWrite.writeTGF(filename, pairGraph);
	}

	@Override
	public int hashCode() {
		return referencePair.hashCode() + 37 * pairGraph.hashCode();
	}

	public String toString() {
		StringBuilder s = new StringBuilder();
		// go trough the pairs
		Iterator<OrderedPair<V>> iterator = pairGraph.vertexSet().iterator();
		while (iterator.hasNext()) {
			OrderedPair<V> pair = iterator.next();
			s.append(pair.toString());
			// if there are more pairs, put them after a comma
			if (iterator.hasNext()) {
				s.append(',');
			}
		}
		return s.toString();
	}

	public void toString(BufferedWriter bw) throws IOException {
		Set<OrderedPair<V>> mapping = pairGraph.vertexSet();

		// graph may not have been expanded, so there is only the reference pair
		if (pairGraph.getNumberOfVertices() == 1) {
			bw.write(getReferencePair().toString());
		} else {

			// pair graph was expanded, contains all the concept pairs
			Iterator<OrderedPair<V>> iterator = mapping.iterator();
			while (iterator.hasNext()) {
				OrderedPair<V> pair = iterator.next();
				bw.write(pair.toString());
				// if there are more pairs, put them after a comma
				if (iterator.hasNext()) {
					bw.write(',');
				}
			}
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		@SuppressWarnings("unchecked")
		MappingStructure<V, E> other = (MappingStructure<V, E>) obj;
		return pairGraph.equals(other.pairGraph) && referencePair.equals(other.referencePair);
	}
}

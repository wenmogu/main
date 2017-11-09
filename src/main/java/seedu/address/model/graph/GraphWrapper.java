package seedu.address.model.graph;

import static java.util.Objects.requireNonNull;
import static seedu.address.commons.util.CollectionUtil.requireAllNonNull;

import java.util.Set;

import org.graphstream.graph.Edge;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.graph.implementations.SingleNode;
import org.graphstream.ui.graphicGraph.stylesheet.StyleConstants;
import org.graphstream.ui.layout.Layout;
import org.graphstream.ui.layout.Layouts;
import org.graphstream.ui.spriteManager.Sprite;
import org.graphstream.ui.spriteManager.SpriteManager;
import org.graphstream.ui.view.View;
import org.graphstream.ui.view.Viewer;

import javafx.collections.ObservableList;
import seedu.address.commons.exceptions.IllegalValueException;
import seedu.address.model.Model;
import seedu.address.model.person.ReadOnlyPerson;
import seedu.address.model.relationship.Relationship;
import seedu.address.model.relationship.RelationshipDirection;
import seedu.address.ui.GraphDisplay;

//@@author wenmogu
/**
 * This class is a wrapper class of SingleGraph class in GraphStream
 * It is used when creating a new SingleGraph when changes happen in the lastShownList
 */
public class GraphWrapper {

    public static final String MESSAGE_PERSON_DOES_NOT_EXIST = "The person does not exist in this address book.";
    private static final String graphId = "ImARandomGraphID";

    private SingleGraph graph;
    private Viewer viewer;
    private Layout layoutAlgorithm;
    private View view;
    private SpriteManager spriteManager;
    private Model model;
    private ObservableList<ReadOnlyPerson> filteredPersons;

    private final String nodeAttributeNodeLabel = "ui.label";
    private final String nodeAttributePerson = "Person";

    public GraphWrapper() {
        this.graph = new SingleGraph(graphId);
        initialiseRenderer();
        initialiseViewer();
        initialiseSpriteManager();
    }

    /**
     * Produce a graph based on model given
     */
    public SingleGraph buildGraph(Model model) {
        requireNonNull(model);
        this.clear();
        this.setData(model);
        this.initiateGraphNodes();
        this.initiateGraphEdges();

        graph.setAttribute("ui.stylesheet", GraphDisplay.getGraphDisplayStylesheet());

        return graph;
    }

    /**
     * Returns the view attached to the viewer for the graph.
     */
    public View getView() {
        return this.view;
    }

    /**
     * add an edge between two persons with direction specified
     */
    public Edge addEdge(ReadOnlyPerson firstPerson, ReadOnlyPerson secondPerson, RelationshipDirection direction) {
        requireAllNonNull(firstPerson, secondPerson, direction);
        if (direction.isDirected()) {
            return addDirectedEdge(firstPerson, secondPerson);
        } else {
            return addUndirectedEdge(firstPerson, secondPerson);
        }
    }

    /**
     * Initialise advanced renderer for integrated graph display.
     */
    private void initialiseRenderer() {
        System.setProperty("org.graphstream.ui.renderer", "org.graphstream.ui.j2dviewer.J2DGraphRenderer");
        graph.addAttribute("ui.quality");
    }

    /**
     * Initialise custom viewer for integrated graph display.
     */
    private void initialiseViewer() {
        this.viewer = new Viewer(graph, Viewer.ThreadingModel.GRAPH_IN_ANOTHER_THREAD);
        layoutAlgorithm = Layouts.newLayoutAlgorithm();
        viewer.enableAutoLayout(layoutAlgorithm);
        viewer.setCloseFramePolicy(Viewer.CloseFramePolicy.EXIT);
        this.view = viewer.addDefaultView(false);
    }

    /**
     * Initialise sprite manager for integrated graph display.
     */
    private void initialiseSpriteManager() {
        this.spriteManager = new SpriteManager(graph);
    }

    private void setData(Model model) {
        this.model = model;
        this.filteredPersons = model.getFilteredPersonList();
    }

    /**
     * Add all the persons in the last displayed list into graph
     * the ID of the node formed for a person is the person's index in the last displayed list
     *
     * Note that the graph only displays first name so that the layout is more aesthetically pleasing.
     * @return graph
     */
    private SingleGraph initiateGraphNodes() {
        try {
            for (ReadOnlyPerson person : filteredPersons) {
                String personIndexInFilteredPersons = getNodeIdFromPerson(person);
                SingleNode node = graph.addNode(personIndexInFilteredPersons);
                String shortenedPersonLabel = (Integer.parseInt(personIndexInFilteredPersons) + 1) + ". "
                        + person.getName().toString().split(" ")[0];
                styleGraphNode(node, shortenedPersonLabel);
            }
        } catch (IllegalValueException ive) {
            assert false : "it should not happen.";
        }

        return graph;
    }

    /**
     * Style each node in the integrated graph display
     */
    private void styleGraphNode(SingleNode node, String nodeLabel) {
        node.addAttribute(nodeAttributeNodeLabel, nodeLabel);
        node.addAttribute("layout.weight", 3);
        layoutAlgorithm.setStabilizationLimit(0.95);
    }

    /**
     * Read all the edges from model and store into graph
     * @return
     */
    private SingleGraph initiateGraphEdges() {
        for (ReadOnlyPerson person: filteredPersons) {
            Set<Relationship> relationshipSet = person.getRelationships();
            for (Relationship relationship: relationshipSet) {
                Edge edge = addEdge(relationship.getFromPerson(), relationship.getToPerson(),
                        relationship.getDirection());
                labelGraphEdge(relationship, edge);
            }
        }

        return graph;
    }

    /**
     * Label edges in the integrated graph display
     */
    private void labelGraphEdge(Relationship relationship, Edge edge) {
        StringBuilder edgeLabel = new StringBuilder();

        String shortRelationshipName = relationship.getName().toString();
        if (relationship.getName().toString().length() > 10) {
            shortRelationshipName = relationship.getName().toString().substring(0, 6) + "...";
        }
        String confidenceEstimate = relationship.getConfidenceEstimate().toString();

        if (shortRelationshipName.length() != 0 || !confidenceEstimate.equals("0.0")) {
            if (!confidenceEstimate.equals("0.0")) {
                edgeLabel.append("(" + confidenceEstimate + ")");
            }

            if (shortRelationshipName.length() != 0) {
                if (!confidenceEstimate.equals("0.0")) {
                    edgeLabel.append(" ");
                }
                edgeLabel.append(shortRelationshipName);
            }

            edge.addAttribute(nodeAttributeNodeLabel, edgeLabel.toString());
        }
        edge.addAttribute("layout.weight", 5);
    }

    private String getNodeIdFromPerson(ReadOnlyPerson person) throws IllegalValueException {
        requireNonNull(person);
        int indexOfThePerson = filteredPersons.indexOf(person);
        if (indexOfThePerson == -1) {
            throw new IllegalValueException(MESSAGE_PERSON_DOES_NOT_EXIST);
        } else {
            return Integer.toString(indexOfThePerson);
        }
    }

    /**
     * Standardize the format of edge ID
     */
    private String computeEdgeId(ReadOnlyPerson person1, ReadOnlyPerson person2) {
        return  Integer.toString(filteredPersons.indexOf(person1)) + "_"
                + Integer.toString(filteredPersons.indexOf(person2));
    }

    /**
     * Add a directed edge from one person to another
     * remove the previous undirected edge between the two persons (if exists) and add a directed edge
     * @return the directed edge from fromPerson to toPerson
     */
    private Edge addDirectedEdge(ReadOnlyPerson fromPerson, ReadOnlyPerson toPerson) {
        String designatedEdgeId = checkForRedundantEdgeAndRemove(fromPerson,
                toPerson, RelationshipDirection.UNDIRECTED);

        try {
            graph.addEdge(designatedEdgeId, getNodeIdFromPerson(fromPerson),
                    getNodeIdFromPerson(toPerson), true);
        } catch (IllegalValueException ive) {
            assert false : "it should not happen.";
        }

        return graph.getEdge(designatedEdgeId);
    }

    /**
     * Add an undirected edge between two persons
     * remove the previous directed edge between the two persons (if exists) and add an undirected edge
     * @return the undirected edge between firstPerson and secondPerson
     */
    private Edge addUndirectedEdge(ReadOnlyPerson firstPerson, ReadOnlyPerson secondPerson) {
        String designatedEdgeId1 = checkForRedundantEdgeAndRemove(firstPerson,
                secondPerson, RelationshipDirection.DIRECTED);
        String designatedEdgeId2 = checkForRedundantEdgeAndRemove(secondPerson,
                firstPerson, RelationshipDirection.DIRECTED);

        try {
            graph.addEdge(designatedEdgeId1, getNodeIdFromPerson(firstPerson),
                    getNodeIdFromPerson(secondPerson), false);
        } catch (IllegalValueException ive) {
            assert false : "it should not happen.";
        }

        return graph.getEdge(designatedEdgeId1);
    }

    /**
     * Remove the previous edge (if exists) with a different RelationshipDirection from
     * the edge to be added.
     * @param fromPerson
     * @param toPerson
     * @param intendedDirectionOfRedundantEdge
     * @return String
     */
    private String checkForRedundantEdgeAndRemove(ReadOnlyPerson fromPerson, ReadOnlyPerson toPerson,
                                                  RelationshipDirection intendedDirectionOfRedundantEdge) {
        requireAllNonNull(fromPerson, toPerson, intendedDirectionOfRedundantEdge);
        String redundantEdgeId1 = computeEdgeId(fromPerson, toPerson);
        String redundantEdgeId2 = computeEdgeId(toPerson, fromPerson);
        Edge redundantEdge1 = graph.getEdge(redundantEdgeId1);
        Edge redundantEdge2 = graph.getEdge(redundantEdgeId2);

        if (intendedDirectionOfRedundantEdge.isDirected()) {
            if (redundantEdge1 != null) {
                graph.removeEdge(redundantEdge1);
            }
            if (redundantEdge2 != null && !redundantEdge2.isDirected()) {
                graph.removeEdge(redundantEdge2);
            }
        } else {
            if (redundantEdge1 != null) {
                graph.removeEdge(redundantEdge1);
            }
            if (redundantEdge2 != null) {
                graph.removeEdge(redundantEdge2);
            }
        }
        return redundantEdgeId1;
    }

    private void clear() {
        graph.clear();
        this.model = null;
        this.filteredPersons = null;
    }
}

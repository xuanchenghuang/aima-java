package aima.gui.fx.applications.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import aima.core.search.csp.*;
import aima.core.search.csp.examples.MapCSP;
import aima.core.search.csp.examples.NotEqualConstraint;
import aima.core.search.csp.inference.AC3Strategy;
import aima.core.search.csp.inference.ForwardCheckingStrategy;
import aima.gui.fx.framework.IntegrableApplication;
import aima.gui.fx.framework.Parameter;
import aima.gui.fx.framework.SimulationPaneBuilder;
import aima.gui.fx.framework.SimulationPaneCtrl;
import aima.gui.fx.views.CspViewCtrl;
import javafx.application.Platform;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

/**
 * Application which demonstrates basic constraint algorithms based on map
 * coloring problems. It shows the constraint graph, lets the user select a
 * solution strategy, and allows then to follow the progress step by step.
 *
 * @author Ruediger Lunde
 */
public class MapColoringCspApp extends IntegrableApplication {

    public static void main(String[] args) {
        launch(args);
    }

    private final static String PARAM_MAP = "map";
    private final static String PARAM_STRATEGY = "strategy";

    private CspViewCtrl<Variable, String> stateViewCtrl;
    private SimulationPaneCtrl simPaneCtrl;

    private CSP<Variable, String> csp;
    private CspSolver<Variable, String> strategy;
    private int stepCounter;

    public MapColoringCspApp() {
    }

    @Override
    public String getTitle() {
        return "Map Coloring CSP App";
    }

    /**
     * Defines state view, parameters, and call-back functions and calls the
     * simulation pane builder to create layout and controller objects.
     */
    @Override
    public Pane createRootPane() {
        BorderPane root = new BorderPane();

        StackPane stateView = new StackPane();
        stateViewCtrl = new CspViewCtrl<>(stateView);

        List<Parameter> params = createParameters();

        SimulationPaneBuilder builder = new SimulationPaneBuilder();
        builder.defineParameters(params);
        builder.defineStateView(stateView);
        builder.defineInitMethod(this::initialize);
        builder.defineSimMethod(this::simulate);
        simPaneCtrl = builder.getResultFor(root);

        return root;
    }

    protected List<Parameter> createParameters() {
        Parameter p1 = new Parameter(PARAM_MAP, "Map of Australia",
                "Map of Australia NSW=BLUE (for LCV)",
                "Map of Australia WA=RED (for LCV)",
                "Tree-Structured Map");
        Parameter p2 = new Parameter(PARAM_STRATEGY, "Backtracking",
                "Backtracking + DEG",
                "Backtracking + Forward Checking",
                "Backtracking + Forward Checking + MRV",
                "Backtracking + Forward Checking + LCV",
                "Backtracking + AC3",
                "Backtracking + AC3 + MRV & DEG + LCV",
                "Min-Conflicts (50)",
                "Tree-CSP-Solver",
                "Tree-CSP-Solver with Random Root");
        return Arrays.asList(p1, p2);
    }

    /**
     * Displays the selected function on the state view.
     */
    @Override
    public void initialize() {
        csp = null;
        switch (simPaneCtrl.getParamValueIndex(PARAM_MAP)) {
            case 0:
                initMapOfAustraliaCsp();
                break;
            case 1:
                initMapOfAustraliaCsp();
                csp.setDomain(MapCSP.NSW, new Domain<>(MapCSP.BLUE));
                break;
            case 2:
                initMapOfAustraliaCsp();
                csp.setDomain(MapCSP.WA, new Domain<>(MapCSP.RED));
                break;
            case 3:
                initTreeCsp();
                break;
        }


        switch (simPaneCtrl.getParamValueIndex(PARAM_STRATEGY)) {
            case 0:
                strategy = new FlexibleBacktrackingSolver<>();
                break;
            case 1: // DEG
                strategy = new FlexibleBacktrackingSolver<Variable, String>().set(CspHeuristics.deg());
                break;
            case 2: // FC
                strategy = new FlexibleBacktrackingSolver<Variable, String>().set(new ForwardCheckingStrategy<>());
                break;
            case 3: // MRV + FC
                strategy = new FlexibleBacktrackingSolver<Variable, String>().set(CspHeuristics.mrvDeg())
                        .set(new ForwardCheckingStrategy<>());
                break;
            case 4: // LCV + FC
                strategy = new FlexibleBacktrackingSolver<Variable, String>().set(CspHeuristics.lcv())
                        .set(new ForwardCheckingStrategy<>());
                break;
            case 5: // AC3
                strategy = new FlexibleBacktrackingSolver<Variable, String>().set(new AC3Strategy<>());
                break;
            case 6: // MRV & DEG + LCV + AC3
                strategy = new FlexibleBacktrackingSolver<Variable, String>().setAll();
                break;
            case 7:
                strategy = new MinConflictsSolver<>(50);
                break;
            case 8:
                strategy = new TreeCspSolver<>();
                break;
            case 9:
                strategy = new TreeCspSolver<Variable, String>().useRandom(true);
                break;
        }

        strategy.addCspListener((csp1, var, assignment) -> {
            stepCounter++;
            updateStateView(csp1, var, assignment);
        });

        stateViewCtrl.initialize(csp);
    }

    @Override
    public void cleanup() {
        simPaneCtrl.cancelSimulation();
    }

    /**
     * Starts the experiment.
     */
    public void simulate() {
        try {
            stepCounter = 0;
            strategy.solve(csp);
        } catch (RuntimeException ex) { // If TreeCspSolver is applied to non-tree-structured CSP
            simPaneCtrl.setStatus(ex.getClass().getSimpleName() + ": " + ex.getMessage());
        }
    }

    /**
     * Caution: While the background thread should be slowed down, updates of
     * the GUI have to be done in the GUI thread!
     */
    private void updateStateView(CSP<Variable, String> csp, Variable var, Assignment<Variable, String> assignment) {
        Platform.runLater(() -> updateStateViewLater(csp, var, assignment));
        simPaneCtrl.waitAfterStep();
    }

    /**
     * Must be called by the GUI thread!
     */
    private void updateStateViewLater(CSP<Variable, String> csp, Variable var, Assignment<Variable, String> assignment) {
        stateViewCtrl.update(csp, assignment);
        String txt1 = "Step " + stepCounter + ": ";
        String txt2 = "Domain reduced";
        if (assignment != null) {
            if (var != null)
                txt2 = "Assignment changed: " + var + "=" + assignment.getValue(var);
            else
                txt2 = "Assignment changed: " + assignment.toString();
            if (assignment.isSolution(csp))
                txt2 += " (Solution)";
        } else {
            txt2 = "Domain reduced" + (var != null ? " at " + var : "");
        }
        simPaneCtrl.setStatus(txt1 + txt2);
    }


    private void initMapOfAustraliaCsp() {
        csp = new MapCSP();
        int c = 15;
        stateViewCtrl.clearMappings();
        stateViewCtrl.setPositionMapping(MapCSP.WA, c*5, c*10);
        stateViewCtrl.setPositionMapping(MapCSP.NT, c*15, c*3);
        stateViewCtrl.setPositionMapping(MapCSP.SA, c*20, c*15);
        stateViewCtrl.setPositionMapping(MapCSP.Q, c*30, c*5);
        stateViewCtrl.setPositionMapping(MapCSP.NSW, c*35, c*15);
        stateViewCtrl.setPositionMapping(MapCSP.V, c*30, c*23);
        stateViewCtrl.setPositionMapping(MapCSP.T, c*33, c*30);

        stateViewCtrl.setColorMapping(MapCSP.RED, Color.RED);
        stateViewCtrl.setColorMapping(MapCSP.GREEN, Color.GREEN);
        stateViewCtrl.setColorMapping(MapCSP.BLUE, Color.BLUE);
    }

    private void initTreeCsp() {
        List<Variable> vars = new ArrayList<>();
        for (int i = 0; i < 8; i++)
            vars.add(new Variable("V" + i));
        csp = new CSP<>(vars);

        csp.setDomain(vars.get(0), new Domain<>(MapCSP.RED, MapCSP.GREEN, MapCSP.BLUE));
        csp.setDomain(vars.get(1), new Domain<>(MapCSP.RED, MapCSP.GREEN, MapCSP.BLUE));
        csp.setDomain(vars.get(2), new Domain<>(MapCSP.RED));
        csp.setDomain(vars.get(3), new Domain<>(MapCSP.RED, MapCSP.GREEN, MapCSP.BLUE));
        csp.setDomain(vars.get(4), new Domain<>(MapCSP.GREEN));
        csp.setDomain(vars.get(5), new Domain<>(MapCSP.RED, MapCSP.GREEN, MapCSP.BLUE));
        csp.setDomain(vars.get(6), new Domain<>(MapCSP.RED));
        csp.setDomain(vars.get(7), new Domain<>(MapCSP.BLUE));

        csp.addConstraint(new NotEqualConstraint<>(vars.get(0), vars.get(1)));
        csp.addConstraint(new NotEqualConstraint<>(vars.get(1), vars.get(2)));
        csp.addConstraint(new NotEqualConstraint<>(vars.get(1), vars.get(3)));
        csp.addConstraint(new NotEqualConstraint<>(vars.get(1), vars.get(4)));
        csp.addConstraint(new NotEqualConstraint<>(vars.get(3), vars.get(5)));
        csp.addConstraint(new NotEqualConstraint<>(vars.get(5), vars.get(6)));
        csp.addConstraint(new NotEqualConstraint<>(vars.get(5), vars.get(7)));

        int c = 15;
        stateViewCtrl.clearMappings();
        stateViewCtrl.setPositionMapping(vars.get(0), c*5, c*10);
        stateViewCtrl.setPositionMapping(vars.get(1), c*15, c*3);
        stateViewCtrl.setPositionMapping(vars.get(2), c*20, c*15);
        stateViewCtrl.setPositionMapping(vars.get(3), c*35, c*15);
        stateViewCtrl.setPositionMapping(vars.get(4), c*30, c*5);
        stateViewCtrl.setPositionMapping(vars.get(5), c*30, c*23);
        stateViewCtrl.setPositionMapping(vars.get(6), c*20, c*30);
        stateViewCtrl.setPositionMapping(vars.get(7), c*33, c*30);

        stateViewCtrl.setColorMapping(MapCSP.RED, Color.RED);
        stateViewCtrl.setColorMapping(MapCSP.GREEN, Color.GREEN);
        stateViewCtrl.setColorMapping(MapCSP.BLUE, Color.BLUE);
    }
}

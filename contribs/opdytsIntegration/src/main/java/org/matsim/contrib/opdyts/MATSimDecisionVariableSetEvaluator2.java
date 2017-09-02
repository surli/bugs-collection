package org.matsim.contrib.opdyts;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import com.google.inject.Inject;
import floetteroed.opdyts.DecisionVariable;
import floetteroed.opdyts.trajectorysampling.TrajectorySampler;
import floetteroed.utilities.math.Vector;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.utils.io.IOUtils;

/**
 * Identifies the approximately best out of a set of decision variables.
 * 
 * @author Gunnar Flötteröd
 * 
 * @see DecisionVariable
 *
 */
public class MATSimDecisionVariableSetEvaluator2<U extends DecisionVariable>
		implements StartupListener, BeforeMobsimListener, ShutdownListener {

	// -------------------- MEMBERS --------------------

	private static final Logger LOGGER = Logger.getLogger(MATSimDecisionVariableSetEvaluator2.class);

	private final TrajectorySampler<U> trajectorySampler;

	private final MATSimStateFactory<U> stateFactory;

	private int memory = 1;

	private boolean averageMemory = false;

	@Inject
	private EventsManager eventsManager;

	@Inject
	private Population population;

	// a list because the order matters in the state space vector
	private final List<SimulationStateAnalyzerProvider> simulationStateAnalyzers = new ArrayList<>();

	private LinkedList<Vector> stateList = null;

	private MATSimState finalState = null;

	private boolean justStarted = true;

	// -------------------- CONSTRUCTION --------------------

	public MATSimDecisionVariableSetEvaluator2(final TrajectorySampler<U> trajectorySampler,
											   final MATSimStateFactory<U> stateFactory) {
		this.trajectorySampler = trajectorySampler;
		this.stateFactory = stateFactory;
	}

	public void addSimulationStateAnalyzer(final SimulationStateAnalyzerProvider analyzer) {
		if (this.simulationStateAnalyzers.contains(analyzer)) {
			throw new RuntimeException("Analyzer " + analyzer + " has already been added.");
		}
		this.simulationStateAnalyzers.add(analyzer);
	}

	// -------------------- SETTERS AND GETTERS --------------------

	public boolean foundSolution() {
		return this.trajectorySampler.foundSolution();
	}

	/**
	 * The vector representation of MATSim's instantaneous state omits some
	 * memory effects, meaning that it is not perfectly precise. Setting the
	 * memory parameter to more than one (its default value) will increase the
	 * precision at the cost of a larger computer memory usage. Too large values
	 * (perhaps larger than ten) may again impair the optimization performance.
	 */
	public void setMemory(final int memory) {
		this.memory = memory;
	}

	public int getMemory() {
		return this.memory;
	}

	/**
	 * <b>WARNING Use this option only if you know what you are doing.</b>
	 * <p>
	 * Setting this to true saves <em>computer</em> memory by averaging the
	 * <em>simulation process</em> memory instead of completely keeping track of
	 * it. This only works if the right (and problem-specific!) memory length is
	 * chosen.
	 * 
	 * @param averageMemory
	 */
	public void setAverageMemory(final boolean averageMemory) {
		this.averageMemory = averageMemory;
	}

	public boolean getAverageMemory() {
		return this.averageMemory;
	}

	public MATSimState getFinalState() {
		return finalState;
	}

	// -------------------- INTERNALS --------------------

	private MATSimState newState(final Population population) {
		final Vector newSummaryStateVector;
		if (this.averageMemory) {
			// average state vectors
			newSummaryStateVector = this.stateList.getFirst().copy();
			for (int i = 1; i < this.memory; i++) {
				// TODO Why iterate up to memory and not up to stateList.size()?
				newSummaryStateVector.add(this.stateList.get(i));
			}
			newSummaryStateVector.mult(1.0 / this.memory);
		} else {
			// concatenate state vectors
			newSummaryStateVector = Vector.concat(this.stateList);
		}
		return this.stateFactory.newState(population, newSummaryStateVector,
				this.trajectorySampler.getCurrentDecisionVariable());
	}

	// --------------- CONTROLLER LISTENER IMPLEMENTATIONS ---------------

	@Override
	public void notifyStartup(final StartupEvent event) {

		this.stateList = new LinkedList<Vector>();

		if (this.simulationStateAnalyzers.isEmpty()) {
			throw new RuntimeException("No simulation state analyzers have been added.");
		}

		for (SimulationStateAnalyzerProvider analyzer : this.simulationStateAnalyzers) {
			this.eventsManager.addHandler(analyzer.newEventHandler());
		}

		// this.trajectorySampler.initialize();

		// TODO NEW
		this.justStarted = true;
	}

	@Override
	public void notifyBeforeMobsim(final BeforeMobsimEvent event) {
		/*
		 * (0) The mobsim must have been run at least once to allow for the
		 * extraction of a vector-valued system state. The "just started" MATSim
		 * iteration is hence run through without Opdyts in the loop.
		 */
		if (this.justStarted) {
			this.justStarted = false;
		} else {

			/*
			 * (1) Extract the instantaneous state vector.
			 */
			Vector newInstantaneousStateVector = null;
			for (SimulationStateAnalyzerProvider analyzer : this.simulationStateAnalyzers) {
				if (newInstantaneousStateVector == null) {
					newInstantaneousStateVector = analyzer.newStateVectorRepresentation();

					//NEW: amit
					String outFile = event.getServices().getControlerIO().getIterationFilename(event.getIteration(),"stateVector_"+analyzer.getStringIdentifier()+".txt");
					StateVectorSizeWriter.writeData(newInstantaneousStateVector, outFile);
				} else {
					// NEW: amit
					Vector tempVector = analyzer.newStateVectorRepresentation();
					newInstantaneousStateVector = Vector.concat(newInstantaneousStateVector,
							tempVector);
					//NEW: amit
					String outFile = event.getServices().getControlerIO().getIterationFilename(event.getIteration(),"stateVector_"+analyzer.getStringIdentifier()+".txt");
					StateVectorSizeWriter.writeData(tempVector, outFile);
				}
			}

			/*
			 * (2) Add instantaneous state vector to the list of past state
			 * vectors and ensure that the size of this list is equal to what
			 * the memory parameter prescribes.
			 */
			this.stateList.addFirst(newInstantaneousStateVector);
			while (this.stateList.size() < this.memory) {
				this.stateList.addFirst(newInstantaneousStateVector);
			}
			while (this.stateList.size() > this.memory) {
				this.stateList.removeLast();
			}

			/*
			 * (3) Inform the TrajectorySampler that one iteration has been
			 * completed and provide the resulting state.
			 */

			this.trajectorySampler.afterIteration(this.newState(this.population));
		}

		for (SimulationStateAnalyzerProvider analyzer : this.simulationStateAnalyzers) {
			analyzer.beforeIteration();
		}

	}

	/*
	 * TODO Given that an iteration is assumed to end before the
	 * "mobsim execution" step, the final state is only approximately correctly
	 * computed because it leaves out the last iteration's "replanning" step.
	 * 
	 */
	@Override
	public void notifyShutdown(final ShutdownEvent event) {
		this.finalState = this.newState(this.population);
	}

	//NEW: Amit
	static class StateVectorSizeWriter {
		public static void writeData(final Vector vector, final String outFile) {
			List<Double> vectorElements = new ArrayList<>(vector.asList());
			Collections.sort(vectorElements, Collections.reverseOrder());

			try (BufferedWriter writer = IOUtils.getBufferedWriter(outFile) ){
				for(Double d : vectorElements) {
					writer.write(d+"\n");
				}
				writer.close();
			} catch (IOException e) {
				throw new RuntimeException("Data is not written/read. Reason : " + e);
			}
		}
	}
}

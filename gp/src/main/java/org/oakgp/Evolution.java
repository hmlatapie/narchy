/*
 * Copyright 2015 S. Webber
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.oakgp;

import org.oakgp.evolve.GenerationEvolver;
import org.oakgp.evolve.GenerationEvolverImpl;
import org.oakgp.evolve.GeneticOperator;
import org.oakgp.evolve.crossover.SubtreeCrossover;
import org.oakgp.evolve.mutate.ConstantToFunctionMutation;
import org.oakgp.evolve.mutate.PointMutation;
import org.oakgp.evolve.mutate.SubTreeMutation;
import org.oakgp.function.Function;
import org.oakgp.generate.TreeGenerator;
import org.oakgp.generate.TreeGeneratorImpl;
import org.oakgp.node.ConstantNode;
import org.oakgp.node.Node;
import org.oakgp.primitive.*;
import org.oakgp.rank.GenerationRanker;
import org.oakgp.rank.RankedCandidate;
import org.oakgp.rank.RankedCandidates;
import org.oakgp.rank.fitness.FitnessFunction;
import org.oakgp.rank.fitness.FitnessFunctionCache;
import org.oakgp.rank.fitness.FitnessFunctionGenerationRanker;
import org.oakgp.rank.tournament.RoundRobinTournament;
import org.oakgp.rank.tournament.TwoPlayerGame;
import org.oakgp.rank.tournament.TwoPlayerGameCache;
import org.oakgp.select.NodeSelectorFactory;
import org.oakgp.select.RankSelectionFactory;
import org.oakgp.terminate.CompositeTerminator;
import org.oakgp.terminate.MaxGenerationsTerminator;
import org.oakgp.terminate.MaxGenerationsWithoutImprovementTerminator;
import org.oakgp.terminate.TargetFitnessTerminator;
import org.oakgp.util.NodeSet;
import org.oakgp.util.StdRandom;

import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Logger;

import static java.util.Objects.requireNonNull;
import static org.oakgp.util.NodeSimplifier.simplify;

/**
 * Provides a convenient way to configure and start a genetic programming run.
 *
 * @see <a href="http://oakgp.org/getting-started-with-oakgp">Getting Started with OakGP</a>
 */
public final class Evolution {
    private static final org.oakgp.util.Random RANDOM = new StdRandom();
    private static final double RATIO_VARIABLES = .6;
    private static final int DEFAULT_CACHE_SIZE = 10000;

    private Type _returnType;
    private org.oakgp.util.Random _random = RANDOM;
    private PrimitiveSet _primitiveSet;
    private GenerationRanker _generationRanker;
    private GenerationEvolver _generationEvolver;
    private Collection<Node> _initialPopulation;

    /**
     * Performs a Genetic Programming run.
     *
     * @param generationRanker  ranks a generation by their fitness
     * @param generationEvolver creates a new generation based on the previous generation
     * @param terminator        a function that determines if the run should finish
     * @param initialPopulation the initial population that will be used as a basis for generating future generations
     * @return the final generation produced as part of this run - the best candidate of this generation can be retrieved using {@link RankedCandidates#best()}
     */
    public static RankedCandidates process(GenerationRanker generationRanker, GenerationEvolver generationEvolver, Predicate<RankedCandidates> terminator,
                                           Collection<Node> initialPopulation) {
        RankedCandidates rankedCandidates = generationRanker.rank(initialPopulation);
        while (!terminator.test(rankedCandidates)) {
            Collection<Node> newGeneration = generationEvolver.evolve(rankedCandidates);
            rankedCandidates = generationRanker.rank(newGeneration);
        }
        return rankedCandidates;
    }

    /**
     * Sets the required {@code Type} associated with the values produced as a result of evaluating the programs that are automatically generated by the run.
     */
    public RandomSetter returning(final Type returnType) {
        _returnType = requireNonNull(returnType);
        return new RandomSetter();
    }

    /**
     * Allows the function set to be configured.
     */
    public interface FunctionSetSetter {
        /**
         * Sets the functions that are available for use in the construction of programs generated by the run.
         */
        GenerationRankerSetter setFunctions(Function... functions);

        /**
         * Sets the functions that are available for use in the construction of programs generated by the run.
         */
        GenerationRankerSetter setFunctions(List<Function> functions);
    }

    /**
     * Provides a method for starting the genetic programming run or setting more termination criteria.
     */
    public interface TerminatorSetterOrProcessRunner extends TerminatorSetter, ProcessRunner {
    }

    /**
     * Allows termination criteria to be configured.
     */
    public interface TerminatorSetter extends MaxGenerationsTerminatorSetter {
        /**
         * Sets the criteria used by this run to determine when it should stop.
         */
        TerminatorSetterOrProcessRunner setTerminator(Predicate<RankedCandidates> terminator);

        /**
         * Set the target fitness that when found should cause the run to stop.
         */
        MaxGenerationsTerminatorSetterOrProcessRunner setTargetFitness(double targetFitness);
    }

    /**
     * Provides a method for starting the genetic programming run or setting more termination criteria.
     */
    public interface MaxGenerationsTerminatorSetterOrProcessRunner extends MaxGenerationsTerminatorSetter, ProcessRunner {
    }

    /**
     * Allows termination criteria to be configured.
     */
    public interface MaxGenerationsTerminatorSetter extends MaxGenerationsWithoutImprovementTerminatorSetter {
        /**
         * Sets the maximum number of generations the run should process before stopping.
         */
        MaxGenerationsWithoutImprovementTerminatorSetterOrProcessRunner setMaxGenerations(int maxGenerations);
    }

    /**
     * Provides a method for starting the genetic programming run or setting more termination criteria.
     */
    public interface MaxGenerationsWithoutImprovementTerminatorSetterOrProcessRunner extends MaxGenerationsWithoutImprovementTerminatorSetter, ProcessRunner {
    }

    /**
     * Allows termination criteria to be configured.
     */
    public interface MaxGenerationsWithoutImprovementTerminatorSetter {
        /**
         * Sets the number of consecutive generations without improvement the run should process before stopping.
         */
        ProcessRunner setMaxGenerationsWithoutImprovement(int maxGenerationsWithoutImprovement);
    }

    /**
     * Provides a method for starting the genetic programming run.
     */
    public interface ProcessRunner extends Supplier<RankedCandidates> {
        /**
         * Processes a genetic programming run using the values configured earlier for this {@code RunBuilder}.
         *
         * @return the final generation produced as part of this run - the best candidate of this generation can be retrieved using
         * {@link RankedCandidates#best()}
         */

    }

    /**
     * Provides the option to set a random number generator, or to skip that option and instead configure the primitive set.
     * <p>
     * If you do not explicitly specify a random number generator then the {@code RunBuilder} will default to using {@link StdRandom}. Unless you
     * have a specific requirement about how random numbers are generated then the default random number generator will be sufficient.
     */
    public final class RandomSetter extends PrimitiveSetSetter {
        private RandomSetter() {
        }

        /**
         * Sets the {@code Random} to use to generate random numbers required by the run.
         */
        public PrimitiveSetSetter setRandom(final org.oakgp.util.Random random) {
            _random = requireNonNull(random);
            return new PrimitiveSetSetter();
        }
        public PrimitiveSetSetter setRandom(final java.util.Random random) {
            return setRandom(new StdRandom(random));
        }
    }

    /**
     * Allows the primitive set to be configured.
     */
    public class PrimitiveSetSetter {
        private PrimitiveSetSetter() {
        }

        /**
         * Sets the functions and terminal nodes that are available for use in the construction of programs generated by the run.
         */
        public GenerationRankerSetter setPrimitiveSet(final PrimitiveSet primitiveSet) {
            _primitiveSet = requireNonNull(primitiveSet);
            return new GenerationRankerSetter();
        }

        /**
         * Sets the constants that are available for use in the construction of programs generated by the run.
         */
        public VariablesSetter setConstants(final ConstantNode... constants) {
            ConstantSet constantSet = new ConstantSet(constants);
            return new VariablesSetter(constantSet);
        }

        /**
         * Sets the constants that are available for use in the construction of programs generated by the run.
         */
        public VariablesSetter setConstants(final List<ConstantNode> constants) {
            return setConstants(constants.toArray(new ConstantNode[constants.size()]));
        }
    }

    /**
     * Allows the variable types to be configured.
     */
    public final class VariablesSetter {
        private final ConstantSet constantSet;

        private VariablesSetter(final ConstantSet constantSet) {
            this.constantSet = constantSet;
        }

        /**
         * Sets the {@code Type}s to associate with the variables available for use in the construction of programs generated by the run.
         */
        public VariablesRatioSetter setVariables(final Type... variableTypes) {
            VariableSet variableSet = VariableSet.createVariableSet(variableTypes);
            return new VariablesRatioSetter(constantSet, variableSet);
        }
    }

    /**
     * Allows the ratio of variables to constants to be configured.
     */
    public final class VariablesRatioSetter implements FunctionSetSetter {
        private final ConstantSet constantSet;
        private final VariableSet variableSet;

        private VariablesRatioSetter(ConstantSet constantSet, VariableSet variableSet) {
            this.constantSet = constantSet;
            this.variableSet = variableSet;
        }

        /**
         * Sets the ratio of terminal nodes that should be variable nodes, rather than constant nodes.
         *
         * @param ratioVariables a value in the range 0 to 1 (inclusive) which specifies the proportion of terminal nodes that should represent variables, rather than
         *                       constants
         * @throws IllegalArgumentException if {@code ratioVariables} is not in the range 0 to 1 inclusive
         */
        public FunctionSetSetter setRatioVariables(final double ratioVariables) {
            if (ratioVariables < 0 || ratioVariables > 1) {
                throw new IllegalArgumentException("Ratio of variables must be in range 0 to 1, not: " + ratioVariables);
            }
            return new FunctionSetSetterImpl(constantSet, variableSet, ratioVariables);
        }

        @Override
        public GenerationRankerSetter setFunctions(Function... functions) {
            return setRatioVariables(RATIO_VARIABLES).setFunctions(functions);
        }

        @Override
        public GenerationRankerSetter setFunctions(final List<Function> functions) {
            return setRatioVariables(RATIO_VARIABLES).setFunctions(functions);
        }
    }

    private final class FunctionSetSetterImpl implements FunctionSetSetter {
        private final ConstantSet constantSet;
        private final VariableSet variableSet;
        private final double ratioVariables;

        private FunctionSetSetterImpl(ConstantSet constantSet, VariableSet variableSet, double ratioVariables) {
            this.constantSet = constantSet;
            this.variableSet = variableSet;
            this.ratioVariables = ratioVariables;
        }

        @Override
        public GenerationRankerSetter setFunctions(final Function... functions) {
            //logFunctionSet(functions);

            FunctionSet functionSet = new FunctionSet(functions);
            _primitiveSet = new PrimitiveSetImpl(functionSet, constantSet, variableSet, _random, ratioVariables);
            return new GenerationRankerSetter();
        }

        @Override
        public GenerationRankerSetter setFunctions(final List<Function> functions) {
            return setFunctions(functions.toArray(new Function[functions.size()]));
        }

//        private void logFunctionSet(final Function[] functions) {
//            boolean first = true;
//            Arrays.sort(functions, Comparator.comparing(o -> o.getClass().getName()));
//            for (Function function : functions) {
//                if (first) {
//                    first = false;
//                } else {
//                    System.out.println("<br>");
//                }
//                System.out.println("|Class:|" + function.getClass().getName());
//                System.out.println("|Symbol:|" + function.name().replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"));
//                System.out.println("|Return Type:|" + function.sig().returnType());
//                String argumentTypes = function.sig().argTypes().toString();
//                argumentTypes = argumentTypes.substring(1, argumentTypes.length() - 1);
//                System.out.println("|Arguments:|" + argumentTypes);
//            }
//        }
    }

    /**
     * Allows the configuration of the mechanism for ranking candidates.
     */
    public final class GenerationRankerSetter {
        private GenerationRankerSetter() {
        }

        /**
         * Set the {@code GenerationRanker} used to rank and sort the candidates of a generation.
         */
        public InitialPopulationSetter ranked(final GenerationRanker generationRanker) {
            _generationRanker = requireNonNull(generationRanker);
            return new InitialPopulationSetter();
        }

        public InitialPopulationSetter setFitness(final FitnessFunction fitnessFunction) {
            return setFitness(fitnessFunction, false);
        }

        /**
         * Set the {@code FitnessFunction} used to determine the fitness of a candidate.
         */
        public InitialPopulationSetter setFitness(final FitnessFunction fitnessFunction, boolean parallel) {
            requireNonNull(fitnessFunction);
            FitnessFunction cf = ensureCached(fitnessFunction);
            return ranked(
                parallel ? new FitnessFunctionGenerationRanker.Parallel(cf) : new FitnessFunctionGenerationRanker.SingleThread(cf)
            );
        }

        private FitnessFunction ensureCached(final FitnessFunction fitnessFunction) {
            if (fitnessFunction instanceof FitnessFunctionCache) {
                return fitnessFunction;
            } else {
                return new FitnessFunctionCache(DEFAULT_CACHE_SIZE, fitnessFunction);
            }
        }

        /**
         * Set the {@code TwoPlayerGame} used to determine the relative fitness of two candidates.
         */
        public InitialPopulationSetter setTwoPlayerGame(final TwoPlayerGame twoPlayerGame) {
            requireNonNull(twoPlayerGame);
            return ranked(new RoundRobinTournament(ensureCached(twoPlayerGame)));
        }

        private TwoPlayerGame ensureCached(final TwoPlayerGame twoPlayerGame) {
            if (twoPlayerGame instanceof TwoPlayerGameCache) {
                return twoPlayerGame;
            } else {
                return new TwoPlayerGameCache(DEFAULT_CACHE_SIZE, twoPlayerGame);
            }
        }
    }

    /**
     * Allows the initial population to be specified.
     */
    public final class InitialPopulationSetter {
        private InitialPopulationSetter() {
        }

        /**
         * Set the contents of the initial population.
         */
        public GenerationEvolverSetter setInitialPopulation(final java.util.function.Function<Config, Collection<Node>> initialPopulation) {
            return setInitialPopulation(initialPopulation.apply(new Config()));
        }

        /**
         * Set the contents of the initial population.
         */
        private GenerationEvolverSetter setInitialPopulation(Collection<Node> initialPopulation) {
            _initialPopulation = requireNonNull(initialPopulation);
            return new GenerationEvolverSetter();
        }

        /**
         * Set the number of randomly generated trees to include in the initial population.
         */
        public TreeDepthSetter setInitialPopulationSize(final int generationSize) {
            return new TreeDepthSetter(generationSize);
        }
    }

    /**
     * Allows configuration of the maximum tree depth of trees randomly generated for the initial population.
     */
    public final class TreeDepthSetter {
        private final int generationSize;

        private TreeDepthSetter(final int generationSize) {
            this.generationSize = requiresPositive(generationSize);
        }

        /**
         * Set the maximum depth of the trees randomly generated for the initial population.
         */
        public GenerationEvolverSetter setTreeDepth(final int treeDepth) {
            requiresPositive(treeDepth);

            // NOTE could use a NodeSet rather than an ArrayList - but then the resulting population may be < generationSize (due to duplicates)
            // NOTE could generate using a 50:50 split of TreeGeneratorImpl.grow and TreeGeneratorImpl.full
            NodeSet initialPopulation = new NodeSet();
            TreeGenerator treeGenerator = TreeGeneratorImpl.grow(_primitiveSet, _random);
            while (initialPopulation.size() < generationSize) {
                Node n = treeGenerator.generate(_returnType, treeDepth);
                initialPopulation.add(n);
            }
            return new InitialPopulationSetter().setInitialPopulation(initialPopulation);
        }

        private int requiresPositive(final int i) {
            if (i > 0) {
                return i;
            } else {
                throw new IllegalArgumentException("Expected a positive integer but got: " + i);
            }
        }
    }

    /**
     * Provides the option to configure the how new generations evolve from existing ones, or to skip that option and instead configure the termination criteria.
     * <p>
     * If you do not explicitly specify how generations evolve then a default strategy will be used. The default strategy is sufficient for allowing people to
     * quickly get started with OakGP
     */
    public final class GenerationEvolverSetter extends FirstTerminatorSetter {
        private GenerationEvolverSetter() {
        }

        /**
         * Set how new generations will be created from existing ones.
         */
        public TerminatorSetter setGenerationEvolver(final java.util.function.Function<Config, GenerationEvolver> generationEvolver) {
            return setGenerationEvolver(generationEvolver.apply(new Config()));
        }

        /**
         * Set how new generations will be created from existing ones.
         */
        private TerminatorSetter setGenerationEvolver(GenerationEvolver generationEvolver) {
            _generationEvolver = requireNonNull(generationEvolver);
            return new FirstTerminatorSetter();
        }
    }

    private class FirstTerminatorSetter implements TerminatorSetter {
        private final List<Predicate<RankedCandidates>> terminators = new ArrayList<>();

        private FirstTerminatorSetter() {
        }

        @Override
        public TerminatorSetterOrProcessRunner setTerminator(final Predicate<RankedCandidates> terminator) {
            terminators.add(requireNonNull(terminator));
            return new SubsequentTerminatorSetter(terminators);
        }

        @Override
        public MaxGenerationsTerminatorSetterOrProcessRunner setTargetFitness(double targetFitness) {
            return new SubsequentTerminatorSetter(terminators).setTargetFitness(targetFitness);
        }

        @Override
        public MaxGenerationsWithoutImprovementTerminatorSetterOrProcessRunner setMaxGenerations(final int maxGenerations) {
            return new MaxGenerationsTerminatorSetterImpl(terminators).setMaxGenerations(maxGenerations);
        }

        @Override
        public ProcessRunner setMaxGenerationsWithoutImprovement(int maxGenerationsWithoutImprovement) {
            return new MaxGenerationsWithoutImprovementTerminatorSetterImpl(terminators).setMaxGenerationsWithoutImprovement(maxGenerationsWithoutImprovement);
        }
    }

    private final class SubsequentTerminatorSetter extends MaxGenerationsTerminatorSetterImpl implements TerminatorSetterOrProcessRunner {
        private SubsequentTerminatorSetter(List<Predicate<RankedCandidates>> terminators) {
            super(terminators);
        }

        @Override
        public TerminatorSetterOrProcessRunner setTerminator(final Predicate<RankedCandidates> terminator) {
            terminators.add(terminator);
            return this;
        }

        @Override
        public MaxGenerationsTerminatorSetterOrProcessRunner setTargetFitness(double targetFitness) {
            terminators.add(new TargetFitnessTerminator(c -> Math.abs(c.getFitness() - targetFitness) < .0000001));
            return new MaxGenerationsTerminatorSetterImpl(terminators);
        }
    }

    private class MaxGenerationsTerminatorSetterImpl extends MaxGenerationsWithoutImprovementTerminatorSetterImpl implements
            MaxGenerationsTerminatorSetterOrProcessRunner {
        private MaxGenerationsTerminatorSetterImpl(List<Predicate<RankedCandidates>> terminators) {
            super(terminators);
        }

        @Override
        public final MaxGenerationsWithoutImprovementTerminatorSetterOrProcessRunner setMaxGenerations(int maxGenerations) {
            terminators.add(new MaxGenerationsTerminator(maxGenerations));
            return new MaxGenerationsWithoutImprovementTerminatorSetterImpl(terminators);
        }
    }

    private class MaxGenerationsWithoutImprovementTerminatorSetterImpl implements MaxGenerationsWithoutImprovementTerminatorSetterOrProcessRunner {
        protected final List<Predicate<RankedCandidates>> terminators;

        private MaxGenerationsWithoutImprovementTerminatorSetterImpl(List<Predicate<RankedCandidates>> terminators) {
            this.terminators = terminators;
        }

        @Override
        public final ProcessRunner setMaxGenerationsWithoutImprovement(int maxGenerationsWithoutImprovement) {
            terminators.add(new MaxGenerationsWithoutImprovementTerminator(maxGenerationsWithoutImprovement));
            return new ProcessRunnerImpl(terminators);
        }

        @Override
        public final RankedCandidates get() {
            return new ProcessRunnerImpl(terminators).get();
        }
    }

    private final class ProcessRunnerImpl implements ProcessRunner {
        private Predicate<RankedCandidates> terminator;

        @SuppressWarnings("unchecked")
        private ProcessRunnerImpl(List<Predicate<RankedCandidates>> terminators) {
            if (terminators.isEmpty()) {
                throw new IllegalStateException("No termination criteria set");
            } else if (terminators.size() == 1) {
                terminator = terminators.get(0);
            } else {
                terminator = new CompositeTerminator(terminators.toArray(new Predicate[terminators.size()]));
            }
        }

        @Override
        public RankedCandidates get() {
            if (_generationEvolver == null) {
                _generationEvolver = createDefaultGenerationEvolver();
            }

            RankedCandidates rankedCandidates = process(_generationRanker, _generationEvolver, terminator, _initialPopulation);
            RankedCandidate best = rankedCandidates.best();
            Node simplifiedBestNode = simplify(best.getNode());
            Logger.getGlobal().info("Best candidate: Fitness: " + best.getFitness() + " Structure: " + simplifiedBestNode);
            return rankedCandidates;
        }

        private GenerationEvolver createDefaultGenerationEvolver() {
            int populationSize = _initialPopulation.size();
            NodeSelectorFactory nodeSelectorFactory = new RankSelectionFactory(_random);
            Map<GeneticOperator, Integer> operators = createDefaultGeneticOperators(populationSize);
            int operatorsSize = operators.values().stream().mapToInt(l -> l).sum();
            int elitismSize = populationSize - operatorsSize;
            Logger.getGlobal().info("total: " + populationSize + " elitism: " + elitismSize + ' ' + operators);
            return new GenerationEvolverImpl(elitismSize, nodeSelectorFactory, operators);
        }

        private Map<GeneticOperator, Integer> createDefaultGeneticOperators(int populationSize) {
            Map<GeneticOperator, Integer> operators = new HashMap<>();
            TreeGenerator treeGenerator = TreeGeneratorImpl.grow(_primitiveSet, _random);
            operators.put(t -> treeGenerator.generate(_returnType, 4), ratio(populationSize, .08));
            operators.put(new SubtreeCrossover(_random, 5), ratio(populationSize, .4));
            operators.put(new PointMutation(_random, _primitiveSet), ratio(populationSize, .4));
            operators.put(new SubTreeMutation(_random, treeGenerator), ratio(populationSize, .04));
            operators.put(new ConstantToFunctionMutation(_random, TreeGeneratorImpl.full(_primitiveSet)), ratio(populationSize, .04));
            return operators;
        }

        private int ratio(int whole, double ratio) {
            return (int) (whole * ratio);
        }
    }

    /**
     * Provides access to configuration values that have already been set on a {@code RunBuilder}.
     */
    public final class Config {
        public PrimitiveSet getPrimitiveSet() {
            return _primitiveSet;
        }

        public org.oakgp.util.Random getRandom() {
            return _random;
        }

        public Type getReturnType() {
            return _returnType;
        }
    }
}
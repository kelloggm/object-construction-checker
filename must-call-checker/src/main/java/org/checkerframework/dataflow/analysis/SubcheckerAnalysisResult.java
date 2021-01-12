package org.checkerframework.dataflow.analysis;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.analysis.AbstractValue;
import org.checkerframework.dataflow.analysis.AnalysisResult;
import org.checkerframework.dataflow.analysis.Store;
import org.checkerframework.dataflow.cfg.block.Block;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.javacutil.BugInCF;

import java.util.ArrayList;
import java.util.List;

/**
 * This class represents a view of an AnalysisResult from
 * a subchecker.
 */
public class SubcheckerAnalysisResult<V extends AbstractValue<V>, S extends Store<S>> extends AnalysisResult<V, S> {
    /**
     * Only constructor is a copy constructor for an existing analysis result.
     */
    public SubcheckerAnalysisResult(AnalysisResult<V, S> result) {
        super(result.nodeValues,
                result.stores,
                result.treeLookup,
                result.unaryAssignNodeLookup,
                result.finalLocalValues,
                result.analysisCaches);
    }

    /**
     * This method functions exactly like the super() method, but the expectation
     * is that the node is from a different but corresponding CFG - the superchecker's
     * CFG.
     */
    @Override
    protected @Nullable S runAnalysisFor(Node node, Analysis.BeforeOrAfter preOrPost) {
        /*Block block = node.getBlock();
        assert block != null : "@AssumeAssertion(nullness): invariant";
        List<TransferInput<V, S>> matchingTransferInputs = new ArrayList<>();
        for (Block knownBlock : stores.keySet()) {
            // equals on the toString is obviously evil, but blocks don't have an equals()
            // method (other than the one defined in Object). The goal here is to find all
            // the blocks that might possibly be the target block and then LUB them.
            if (knownBlock.toString().equals(block.toString())) {
                matchingTransferInputs.add(stores.get(knownBlock));
            }
        }
        TransferInput<V, S> transferInput = lubTransferInputs(matchingTransferInputs);*/

        // TODO: try this?
        // Set<Node> corresponding = treeLookup.get(node.getTree());

        Node corresponding = null;

        for (Node knownNode : nodeValues.keySet()) {
            if (node.equals(knownNode)) {
                if (corresponding == null) {
                    corresponding = knownNode;
                } else {
                    System.out.println(this.toStringDebug());
                    throw new BugInCF("Found multiple corresponding nodes. Looking for this node: " + node);
                }
            }
        }

        if (corresponding == null) {
            return null;
        }

        Block block = corresponding.getBlock();
        TransferInput<V, S> transferInput = stores.get(block);

        if (transferInput == null) {
            return null;
        }
        return runAnalysisFor(corresponding, preOrPost, transferInput, nodeValues, analysisCaches);
    }

    private @Nullable TransferInput<V,S> lubTransferInputs(List<TransferInput<V,S>> inputs) {
        if (inputs.isEmpty()) {
            return null;
        }
        TransferInput<V, S> acc = inputs.get(0);
        for (int i = 1; i < inputs.size(); i++) {
            TransferInput<V, S> next = inputs.get(i);
            acc = acc.leastUpperBound(next);
        }
        return acc;
    }
}



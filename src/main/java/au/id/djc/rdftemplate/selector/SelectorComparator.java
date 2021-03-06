/**
 * 
 */
package au.id.djc.rdftemplate.selector;

import java.util.Comparator;

import org.apache.commons.lang.builder.ToStringBuilder;

import com.hp.hpl.jena.rdf.model.RDFNode;

public class SelectorComparator<T extends Comparable<T>> implements Comparator<RDFNode> {
    
    private Selector<T> selector;
    private boolean reversed = false;
    
    public Selector<T> getSelector() {
        return selector;
    }
    
    public void setSelector(Selector<T> selector) {
        this.selector = selector;
    }
    
    public boolean isReversed() {
        return reversed;
    }
    
    public void setReversed(boolean reversed) {
        this.reversed = reversed;
    }
    
    @Override
    public String toString() {
        return new ToStringBuilder(this).append(selector).append("reversed", reversed).toString();
    }
    
    @Override
    public int compare(RDFNode left, RDFNode right) {
        T leftKey;
        try {
            leftKey = selector.singleResult(left);
        } catch (SelectorEvaluationException e) {
            throw new SelectorEvaluationException("Exception evaluating selector [" + selector + "] " +
            		"with context node [" + left + "] for comparison", e);
        }
        T rightKey;
        try {
            rightKey = selector.singleResult(right);
        } catch (SelectorEvaluationException e) {
            throw new SelectorEvaluationException("Exception evaluating selector [" + selector + "] " +
            		"with context node [" + right + "] for comparison", e);
        }
        int result = leftKey.compareTo(rightKey);
        return reversed ? -result : result;
    }
    
}
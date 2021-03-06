package au.id.djc.rdftemplate.selector;

import static org.hamcrest.CoreMatchers.equalTo;

import org.hamcrest.Matcher;

public class SelectorComparatorMatcher<T extends Comparable<T>> extends BeanPropertyMatcher<SelectorComparator<T>> {

    @SuppressWarnings("unchecked")
    public SelectorComparatorMatcher() {
        super((Class) SelectorComparator.class);
    }
    
    public static SelectorComparatorMatcher<?> selectorComparator(Matcher<? extends Selector<?>> selector) {
        SelectorComparatorMatcher<?> m = new SelectorComparatorMatcher();
        m.addRequiredProperty("selector", selector);
        return m;
    }
    
    public SelectorComparatorMatcher<T> reversed() {
        addRequiredProperty("reversed", equalTo(true));
        return this;
    }

}

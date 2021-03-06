package au.id.djc.rdftemplate;

import java.util.List;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.util.XMLEventConsumer;

import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Seq;
import com.hp.hpl.jena.vocabulary.RDF;
import org.apache.commons.lang.builder.ToStringBuilder;

import au.id.djc.rdftemplate.selector.Selector;

public class ForAction extends TemplateAction {
    
    public static final String ACTION_NAME = "for";
    public static final QName ACTION_QNAME = new QName(TemplateInterpolator.NS, ACTION_NAME);
    private static final Logger LOG = Logger.getLogger(ForAction.class.getName());
    
    private final List<XMLEvent> tree;
    private final Selector<RDFNode> selector;
    
    public ForAction(List<XMLEvent> tree, Selector<RDFNode> selector) {
        this.tree = tree;
        this.selector = selector;
    }
    
    public void evaluate(TemplateInterpolator interpolator, RDFNode node, XMLEventConsumer writer)
            throws XMLStreamException {
        List<RDFNode> result = selector.result(node);
        if (result.size() == 1 && result.get(0).canAs(Resource.class)) {
            if (result.get(0).as(Resource.class).hasProperty(RDF.type, RDF.Seq)) {
                LOG.fine("Apply rdf:Seq special case for " + result.get(0));
                result = result.get(0).as(Seq.class).iterator().toList();
                LOG.fine("Resulting sequence is " + result);
            }
        }
        for (RDFNode eachNode: result) {
            interpolator.interpolate(tree.iterator(), eachNode, writer);
        }
    }
    
    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("selector", selector)
                .toString();
    }

}

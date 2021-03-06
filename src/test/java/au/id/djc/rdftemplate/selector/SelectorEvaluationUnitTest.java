package au.id.djc.rdftemplate.selector;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.junit.matchers.JUnitMatchers.*;

import java.io.InputStream;
import java.util.List;

import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import au.id.djc.rdftemplate.TestNamespacePrefixMap;
import au.id.djc.rdftemplate.datatype.DateDataType;
import au.id.djc.rdftemplate.datatype.DateTimeDataType;

public class SelectorEvaluationUnitTest {
    
    private Model m;
    private Resource journal, issue, article, multiAuthorArticle, citedArticle, author, anotherAuthor, book, review, anotherReview, obituary, en, ru, forum;
    private AntlrSelectorFactory selectorFactory;
    
    @BeforeClass
    public static void ensureDatatypesRegistered() {
        DateDataType.registerStaticInstance();
        DateTimeDataType.registerStaticInstance();
    }
    
    @Before
    public void setUp() {
        m = ModelFactory.createDefaultModel();
        InputStream stream = this.getClass().getResourceAsStream("/au/id/djc/rdftemplate/test-data.xml");
        m.read(stream, "");
        journal = m.createResource("http://miskinhill.com.au/journals/test/");
        issue = m.createResource("http://miskinhill.com.au/journals/test/1:1/");
        article = m.createResource("http://miskinhill.com.au/journals/test/1:1/article");
        multiAuthorArticle = m.createResource("http://miskinhill.com.au/journals/test/1:1/multi-author-article");
        citedArticle = m.createResource("http://miskinhill.com.au/cited/journals/asdf/1:1/article");
        author = m.createResource("http://miskinhill.com.au/authors/test-author");
        anotherAuthor = m.createResource("http://miskinhill.com.au/authors/another-author");
        book = m.createResource("http://miskinhill.com.au/cited/books/test");
        review = m.createResource("http://miskinhill.com.au/journals/test/1:1/reviews/review");
        anotherReview = m.createResource("http://miskinhill.com.au/journals/test/2:1/reviews/another-review");
        obituary = m.createResource("http://miskinhill.com.au/journals/test/1:1/in-memoriam-john-doe");
        en = m.createResource("http://www.lingvoj.org/lang/en");
        ru = m.createResource("http://www.lingvoj.org/lang/ru");
        forum = m.createResource("http://miskinhill.com.au/");
        selectorFactory = new AntlrSelectorFactory();
        selectorFactory.setNamespacePrefixMap(TestNamespacePrefixMap.getInstance());
    }
    
    @Test
    public void shouldEvaluateTraversal() {
        RDFNode result = selectorFactory.get("dc:creator").withResultType(RDFNode.class).singleResult(article);
        assertThat(result, equalTo((RDFNode) author));
    }
    
    @Test
    public void shouldEvaluateMultipleTraversals() throws Exception {
        RDFNode result = selectorFactory.get("dc:creator/foaf:name")
                .withResultType(RDFNode.class).singleResult(article);
        assertThat(((Literal) result).getString(), equalTo("Test Author"));
    }
    
    @Test
    public void shouldEvaluateInverseTraversal() throws Exception {
        List<RDFNode> results = selectorFactory.get("!dc:isPartOf")
                .withResultType(RDFNode.class).result(issue);
        assertThat(results.size(), equalTo(4));
        assertThat(results, hasItems((RDFNode) article, (RDFNode) multiAuthorArticle, (RDFNode) review, (RDFNode) obituary));
    }
    
    @Test
    public void shouldEvaluateSortOrder() throws Exception {
        List<RDFNode> results = selectorFactory.get("dc:language(lingvoj:iso1#comparable-lv)")
                .withResultType(RDFNode.class).result(journal);
        assertThat(results.size(), equalTo(2));
        assertThat(results.get(0), equalTo((RDFNode) en));
        assertThat(results.get(1), equalTo((RDFNode) ru));
    }
    
    @Test
    public void shouldEvaluateReverseSortOrder() throws Exception {
        List<RDFNode> results = selectorFactory.get("dc:language(~lingvoj:iso1#comparable-lv)")
                .withResultType(RDFNode.class).result(journal);
        assertThat(results.size(), equalTo(2));
        assertThat(results.get(0), equalTo((RDFNode) ru));
        assertThat(results.get(1), equalTo((RDFNode) en));
    }
    
    @Test
    public void shouldEvaluateComplexSortOrder() throws Exception {
        List<RDFNode> results = selectorFactory.get("!mhs:reviews(dc:isPartOf/mhs:publicationDate#comparable-lv)")
                .withResultType(RDFNode.class).result(book);
        assertThat(results.size(), equalTo(2));
        assertThat(results.get(0), equalTo((RDFNode) review));
        assertThat(results.get(1), equalTo((RDFNode) anotherReview));
    }
    
    @Test
    public void shouldEvaluateUriAdaptation() throws Exception {
        String result = selectorFactory.get("mhs:coverThumbnail#uri")
                .withResultType(String.class).singleResult(issue);
        assertThat(result, equalTo("http://miskinhill.com.au/journals/test/1:1/cover.thumb.jpg"));
    }
    
    @Test
    public void shouldEvaluateBareUriAdaptation() throws Exception {
        String result = selectorFactory.get("#uri").withResultType(String.class).singleResult(journal);
        assertThat(result, equalTo("http://miskinhill.com.au/journals/test/"));
    }
    
    @Test
    public void shouldEvaluateUriSliceAdaptation() throws Exception {
        String result = selectorFactory.get("dc:identifier[uri-prefix='urn:issn:']#uri-slice(9)")
                .withResultType(String.class).singleResult(journal);
        assertThat(result, equalTo("12345678"));
    }
    
    @Test
    public void shouldEvaluateUriAnchorAdaptation() throws Exception {
        String result = selectorFactory.get("mhs:inSection#uri-anchor")
                .withResultType(String.class).singleResult(anotherReview);
        assertThat(result, equalTo("stuff"));
    }
    
    @Test
    public void shouldEvaluateSubscript() throws Exception {
        String result = selectorFactory.get(
                "!mhs:isIssueOf(~mhs:publicationDate#comparable-lv)[0]/mhs:coverThumbnail#uri")
                .withResultType(String.class).singleResult(journal);
        assertThat(result, equalTo("http://miskinhill.com.au/journals/test/2:1/cover.thumb.jpg"));
        result = selectorFactory.get(
                "!mhs:isIssueOf(mhs:publicationDate#comparable-lv)[0]/mhs:coverThumbnail#uri")
                .withResultType(String.class).singleResult(journal);
        assertThat(result, equalTo("http://miskinhill.com.au/journals/test/1:1/cover.thumb.jpg"));
    }
    
    @Test
    public void shouldEvaluateLVAdaptation() throws Exception {
        List<Object> results = selectorFactory.get("dc:language/lingvoj:iso1#lv")
                .withResultType(Object.class).result(journal);
        assertThat(results.size(), equalTo(2));
        assertThat(results, hasItems((Object) "en", (Object) "ru"));
    }
    
    @Test
    public void shouldEvaluateTypePredicate() throws Exception {
        List<RDFNode> results = selectorFactory.get("!dc:creator[type=mhs:Review]")
                .withResultType(RDFNode.class).result(author);
        assertThat(results.size(), equalTo(1));
        assertThat(results, hasItems((RDFNode) review));
    }
    
    @Test
    public void shouldEvaluateAndCombinationOfPredicates() throws Exception {
        List<RDFNode> results = selectorFactory.get("!dc:creator[type=mhs:Article and uri-prefix='http://miskinhill.com.au/journals/']")
                .withResultType(RDFNode.class).result(author);
        assertThat(results.size(), equalTo(2));
        assertThat(results, hasItems((RDFNode) article, (RDFNode) multiAuthorArticle));
    }
    
    @Test
    public void shouldEvaluateUnion() throws Exception {
        List<RDFNode> results = selectorFactory.get("!dc:creator | !mhs:translator")
                .withResultType(RDFNode.class).result(anotherAuthor);
        assertThat(results.size(), equalTo(4));
        assertThat(results, hasItems((RDFNode) article, (RDFNode) multiAuthorArticle, (RDFNode) citedArticle, (RDFNode) anotherReview));
    }
    
    @Test
    public void shouldEvaluateMultipleSortSelectors() throws Exception {
        List<RDFNode> results = selectorFactory.get("!dc:creator[uri-prefix='http://miskinhill.com.au/journals/']" +
        		"(~dc:isPartOf/mhs:publicationDate#comparable-lv,mhs:startPage#comparable-lv)")
                .withResultType(RDFNode.class).result(author);
        assertThat(results.size(), equalTo(4));
        assertThat(results.get(0), equalTo((RDFNode) obituary));
        assertThat(results.get(1), equalTo((RDFNode) article));
        assertThat(results.get(2), equalTo((RDFNode) multiAuthorArticle));
        assertThat(results.get(3), equalTo((RDFNode) review));
    }
    
    @Test
    public void shouldEvaluateFormattedDTAdaptation() throws Exception {
        String result = selectorFactory.get("!sioc:has_container/dc:created#formatted-dt('d MMMM yyyy')")
                .withResultType(String.class).singleResult(forum);
        assertThat(result, equalTo("15 June 2009"));
    }
    
    @Test
    public void shouldEvaluateFormattedDTAdaptationWithDoubleQuotes() throws Exception {
        String result = selectorFactory.get("!sioc:has_container/dc:created#formatted-dt('yyyy-MM-dd\"T\"HH:mm:ssZZ')")
                .withResultType(String.class).singleResult(forum);
        assertThat(result, equalTo("2009-06-15T18:21:32+10:00"));
    }
    
    @Test
    public void shouldEvaluateStringLVAdaptation() throws Exception {
        List<String> results = selectorFactory.get("dc:language/lingvoj:iso1#string-lv")
                .withResultType(String.class).result(journal);
        assertThat(results.size(), equalTo(2));
        assertThat(results, hasItems("en", "ru"));
    }
    
    @Test
    public void stringLVAdaptationShouldStripTagsFromXMLLiteral() throws Exception {
        String result = selectorFactory.get("!sioc:has_container/sioc:content/awol:body#string-lv")
                .withResultType(String.class).singleResult(forum);
        assertEquals("To coincide with the publication of our second issue, " +
        		"the 2008 volume of Australian Slavonic and East European Studies, " +
        		"we are making available two new data feeds: an Atom feed of all " +
        		"journal issues published on this site, and the complete RDF dataset " +
        		"underlying the site. We hope this helps our users and aggregators " +
        		"to discover new content as it is published.",
        		result.trim().replaceAll("\\s+", " "));
    }
    
    @Test
    public void should_evaluate_lang_adaptation_for_plain_literals() throws Exception {
        String result = selectorFactory.get("dc:title#lang")
                .withResultType(String.class).singleResult(journal);
        assertThat(result, equalTo("en"));
    }
    
    @Test
    public void should_evaluate_lang_adaptation_for_xml_literals() throws Exception {
        String result = selectorFactory.get("!sioc:has_container/sioc:content/awol:body#lang")
                .withResultType(String.class).singleResult(forum);
        assertThat(result, equalTo("en"));
    }
    
}

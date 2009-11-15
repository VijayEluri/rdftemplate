grammar Selector;

@parser::header {
package au.com.miskinhill.rdftemplate.selector;
import java.util.Map;
}

@parser::members {
    
    @Override
    public void reportError(RecognitionException e) {
        throw new InvalidSelectorSyntaxException(e);
    }
    
    private AdaptationResolver adaptationResolver;
    private PredicateResolver predicateResolver;
    private Map<String, String> namespacePrefixMap;
    
    public void setAdaptationResolver(AdaptationResolver adaptationResolver) {
        this.adaptationResolver = adaptationResolver;
    }
    
    public void setPredicateResolver(PredicateResolver predicateResolver) {
        this.predicateResolver = predicateResolver;
    }
    
    public void setNamespacePrefixMap(Map<String, String> map) {
        this.namespacePrefixMap = map;
    }
    
    private String ns(String prefix) {
        String ns = namespacePrefixMap.get(prefix);
        if (ns == null)
            throw new InvalidSelectorSyntaxException("Unbound namespace prefix " + prefix);
        return ns;
    }
    
}

@lexer::header {
package au.com.miskinhill.rdftemplate.selector;
}

@lexer::members {
    @Override
    public void reportError(RecognitionException e) {
        throw new InvalidSelectorSyntaxException(e);
    }
}

start : unionSelector ;

unionSelector returns [Selector<?> result]
@init {
    List<Selector<?>> selectors = new ArrayList<Selector<?>>();
}
    : s=selector { selectors.add(s); }
      ( '|'
        s=selector { selectors.add(s); }
      )*
      {
        if (selectors.size() > 1)
            result = new UnionSelector(selectors);
        else
            result = selectors.get(0);
      }
    ;

selector returns [Selector<?> result]
@init {
    Class<? extends Adaptation<?>> adaptationClass;
    Adaptation<?> adaptation = null;
}
    : ' '*
      ( ts=traversingSelector { result = ts; }
      | { result = new NoopSelector(); }
      )
      ( '#'
        adaptationName=XMLTOKEN
            {
                adaptationClass = adaptationResolver.getByName($adaptationName.text);
                if (adaptationClass == null)
                    throw new InvalidSelectorSyntaxException("No adaptation named " + $adaptationName.text);
            }
        ( '('
          ( startIndex=INTEGER {
                                try {
                                    adaptation = adaptationClass.getConstructor(Integer.class)
                                            .newInstance(Integer.parseInt($startIndex.text));
                                } catch (Exception e) {
                                    throw new InvalidSelectorSyntaxException(e);
                                }
                             }
          | sq=SINGLE_QUOTED {
                                try {
                                    adaptation = adaptationClass.getConstructor(String.class)
                                            .newInstance($sq.text);
                                } catch (Exception e) {
                                    throw new InvalidSelectorSyntaxException(e);
                                }
                             }
          )
          ')'
        | {
               try {
                   adaptation = adaptationClass.newInstance();
               } catch (Exception e) {
                   throw new InvalidSelectorSyntaxException(e);
               }
          }
        )
        { $result = new SelectorWithAdaptation(result, adaptation); }
      |
      )
      ' '*
    ;

traversingSelector returns [TraversingSelector result]
@init {
    result = new TraversingSelector();
}
    : t=traversal { $result.addTraversal(t); }
      ( '/'
        t=traversal { $result.addTraversal(t); }
      ) *
    ;
    
traversal returns [Traversal result]
@init {
    result = new Traversal();
}
    : ( '!' { $result.setInverse(true); }
      | // optional
      )
      nsprefix=XMLTOKEN { $result.setPropertyNamespace(ns($nsprefix.text)); }
      ':'
      localname=XMLTOKEN { $result.setPropertyLocalName($localname.text); }
      ( '['
        p=booleanPredicate { $result.setPredicate(p); }
        ']'
      | // optional
      )
      ( '('
        so=sortOrder { $result.addSortOrderComparator(so); }
        ( ','
          so=sortOrder { $result.addSortOrderComparator(so); }
        )*
        ')'
      | // optional
      )
      ( '['
        subscript=INTEGER { $result.setSubscript(Integer.parseInt($subscript.text)); }
        ']'
      | // optional
      )
    ;

sortOrder returns [SelectorComparator<? extends Comparable<?>> result]
@init {
    result = new SelectorComparator();
}
    : ( '~' { $result.setReversed(true); }
      | // optional
      )
      s=selector { $result.setSelector((Selector) s.withResultType(Comparable.class)); }
    ;

booleanPredicate returns [Predicate result]
    : ( p=predicate { result = p; }
      | left=predicate
        ' '+
        'and'
        ' '+
        right=booleanPredicate
        { result = new BooleanAndPredicate(left, right); }
      )
    ;
    
predicate returns [Predicate result]
@init {
    Class<? extends Predicate> predicateClass;
}
    : predicateName=XMLTOKEN
            {
                predicateClass = predicateResolver.getByName($predicateName.text);
                if (predicateClass == null)
                    throw new InvalidSelectorSyntaxException("No predicate named " + $predicateName.text);
            }
      '='
      ( sq=SINGLE_QUOTED {
                try {
                    result = predicateClass.getConstructor(String.class).newInstance($sq.text);
                } catch (Exception e) {
                    throw new InvalidSelectorSyntaxException(e);
                }
             }
      | nsprefix=XMLTOKEN
        ':'
        localname=XMLTOKEN
        {
            try {
                result = predicateClass.getConstructor(String.class, String.class).newInstance(ns($nsprefix.text), $localname.text);
            } catch (Exception e) {
                throw new InvalidSelectorSyntaxException(e);
            }
        }
      )
    ;

XMLTOKEN : ('a'..'z'|'A'..'Z') ('a'..'z'|'A'..'Z'|'0'..'9'|'_'|'-')* ;
INTEGER : ('0'..'9')+ ;
SINGLE_QUOTED : '\'' ( options {greedy=false;} : . )* '\''
    {
        // strip quotes
        String txt = getText();
        setText(txt.substring(1, txt.length() -1));
    };
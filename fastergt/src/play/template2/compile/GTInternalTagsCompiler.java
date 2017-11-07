package play.template2.compile;

import play.template2.exceptions.GTCompilationExceptionWithSourceInfo;

import java.lang.reflect.Method;

public class GTInternalTagsCompiler {


    public boolean generateCodeForGTFragments( String tagName, String contentMethodName, GTPreCompiler.SourceContext sc, int startLine) {

        // Check if we have a method named 'tag_tagName'

        Method tagMethod;
        try {
            tagMethod = getClass().getMethod("tag_"+tagName, String.class, String.class, GTPreCompiler.SourceContext.class, Integer.TYPE);
        } catch( Exception e) {
            // did not find a method to handle this tag
            return false;
        }

        try {
            tagMethod.invoke(this, tagName, contentMethodName, sc, startLine);
        } catch (Exception e) {
            throw new GTCompilationExceptionWithSourceInfo("Error generating code for tag '"+tagName+"'", sc.templateLocation, startLine+1, e);
        }

        return true;
    }

    public void tag_list(String tagName, String contentMethodName, GTPreCompiler.SourceContext sc, int startLine) {


        // one can else if list is empty - must clear the else flag
        sc.jprintln(" clearElseFlag();", startLine);


        sc.jprintln(" String as = (String)tagArgs.get(\"as\");");
        sc.jprintln(" String itemName = (as==null?\"_\":as);");
        sc.jprintln(" as = (as == null ? \"\" : as);");

        sc.jprintln(" Object _items = tagArgs.get(\"items\");");
        sc.jprintln(" if (_items == null ) _items = tagArgs.get(\"arg\");");
        sc.jprintln(" if (_items == null ) return ;");

        sc.jprintln(" int i=0;");
        sc.jprintln(" Iterator it = convertToIterator(_items);");
        sc.jprintln(" while( it.hasNext()) {");
        // prepare for next iteration
        sc.jprintln("   Object item = it.next();");
        sc.jprintln("   i++;");
        sc.jprintln("   binding.setProperty(itemName, item);");
        sc.jprintln("   binding.setProperty(as+\"_index\", i);");
        sc.jprintln("   binding.setProperty(as+\"_isLast\", !it.hasNext());");
        sc.jprintln("   binding.setProperty(as+\"_isFirst\", i==1);");
        sc.jprintln("   binding.setProperty(as+\"_parity\", (i%2==0?\"even\":\"odd\"));");

        // call list tag content
        sc.jprintln("   " + contentMethodName + "();");

        sc.jprintln(" }");

        // if we did not iterate over anything, we must set the else-flag so that the next else-block is executed
        sc.jprintln(" if(i==0) { setElseFlag(); }");

    }


    public void tag_if(String tagName, String contentMethodName, GTPreCompiler.SourceContext sc, int startLine) {
        // extract the argument named "arg"
        sc.jprintln(" Object e = tagArgs.get(\"arg\");", startLine);
        // clear the runNextElse
        sc.jprintln(" clearElseFlag();");
        // do the if
        sc.jprintln(" if(evaluateCondition(e)) {" + contentMethodName + "();} else { setElseFlag(); }");
    }

    public void tag_ifnot(String tagName, String contentMethodName, GTPreCompiler.SourceContext sc, int startLine) {
        // extract the argument named "arg"
        sc.jprintln(" Object e = tagArgs.get(\"arg\");", startLine);

        // clear the runNextElse
        sc.jprintln(" clearElseFlag();");
        // do the if
        sc.jprintln(" if(!evaluateCondition(e)) {" + contentMethodName + "();} else { setElseFlag(); }");
    }

    public void tag_else(String tagName, String contentMethodName, GTPreCompiler.SourceContext sc, int startLine) {
        // run the else if runNextElse is true

        // do the if
        sc.jprintln(" if( elseFlagIsSet()) {" + contentMethodName + "();}", startLine);

        // clear runNextElse
        sc.jprintln(" clearElseFlag();");
    }

    public void tag_elseif(String tagName, String contentMethodName, GTPreCompiler.SourceContext sc, int startLine) {
        // run the elseif if runNextElse is true AND expression is true

        // do the if
        sc.jprintln(" if( elseFlagIsSet()) {", startLine);

        // Just include the regluar if-tag here..
        tag_if(tagName, contentMethodName, sc, startLine);

        sc.jprintln(" }");
    }

    public void tag_extends(String tagName, String contentMethodName, GTPreCompiler.SourceContext sc, int startLine) {
        // the template we extends is the single argument named 'args'

        String templateNameVar = "_tn_"+ (sc.nextMethodIndex++);
        sc.jprintln(" String "+templateNameVar + " = (String)tagArgs.get(\"arg\");", startLine );

        sc.jprintln(" play.template2.GTTemplateLocationReal templateLocation = this.resolveTemplateLocation( "+templateNameVar+" );");

        // must check runtime that the template exists
        sc.jprintln(" if(templateLocation == null ) " +
                "{throw new play.template2.exceptions.GTTemplateNotFoundWithSourceInfo("+templateNameVar+", this.templateLocation, "+(startLine+1)+");}");

        sc.jprintln(" this.extendsTemplateLocation = templateLocation;");

        // that's it..
    }

    // used when dumping the output from the template that extended this one
    public void tag_doLayout(String tagName, String contentMethodName, GTPreCompiler.SourceContext sc, int startLine) {
        // someone is extending us - and we are supposed to dump the output now..
        sc.jprintln(" if( this.extendingTemplate == null) throw new play.template2.exceptions.GTRuntimeException(\"No template is currently extending this template\");", startLine);
        // inject all the output from the extending template into our output stream
        sc.jprintln(" this.insertOutput(this.extendingTemplate);");

        // done..
    }


}

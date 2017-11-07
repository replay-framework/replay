package play.template2;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * Tag Context (retrieve who call you)
 */
public abstract class GTTagContext {

    // Need to do it like this to be able to let the framework (1x module) decided that some other TagContext-impl should be in charge
    public static GTTagContext singleton = new GTTagContextDefaultImpl();

    public interface GTTagContextInfo {
        String getTagName();
        Map<String, Object> getData();
    }

    public abstract void init();
    
    public abstract void enterTag(String name);
    
    public abstract void exitTag();
    
    public abstract GTTagContextInfo current();
    
    public abstract GTTagContextInfo parent();
    
    public abstract boolean hasParentTag(String name);
    
    public abstract GTTagContextInfo parent(String name);

}

class GTTagContextDefaultImpl extends GTTagContext {
    static ThreadLocal<Stack<GTTagContextInfo>> currentStack = new ThreadLocal<>();

    public static class GTTagContextInfoImpl implements GTTagContextInfo {
        private final String tagName;
        private final Map<String, Object> data = new HashMap<>();

        protected GTTagContextInfoImpl(String tagName) {
            this.tagName = tagName;
        }

        @Override public String getTagName() {
            return tagName;
        }

        @Override public Map<String, Object> getData() {
            return data;
        }

        @Override
        public String toString() {
            return tagName + data;
        }

    }
    
    @Override public void init() {
        currentStack.set(new Stack<>());
        enterTag("ROOT");
    }

    @Override public void enterTag(String name) {
        currentStack.get().add(new GTTagContextInfoImpl(name));
    }

    @Override public void exitTag() {
        currentStack.get().pop();
    }

    @Override public GTTagContextInfo current() {
        return currentStack.get().peek();
    }

    @Override public GTTagContextInfo parent() {
        if(currentStack.get().size() < 2) {
            return null;
        }
        return currentStack.get().get(currentStack.get().size()-2);
    }

    @Override public boolean hasParentTag(String name) {
        for(int i=currentStack.get().size()-1; i>=0; i--) {
            if(name.equals(currentStack.get().get(i).getTagName())) {
                return true;
            }
        }
        return false;
    }

    @Override public GTTagContextInfo parent(String name) {
        for (int i=currentStack.get().size()-1; i>=0; i--) {
            if (name.equals(currentStack.get().get(i).getTagName())) {
                return currentStack.get().get(i);
            }
        }
        return null;
    }

}

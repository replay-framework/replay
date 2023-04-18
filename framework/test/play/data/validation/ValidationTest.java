package play.data.validation;

import org.junit.Before;
import org.junit.Test;
import play.Play;
import play.i18n.Messages;
import play.i18n.MessagesBuilder;
import play.mvc.Http;

import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

public class ValidationTest {
    private final ValidationPlugin validationPlugin = new ValidationPlugin();
    private final Http.Request request = new Http.Request();
    private final Http.Response response = new Http.Response();

    @Before
    public void setUp() {
        new MessagesBuilder().build();
        Validation.current.set(new Validation());
        Play.configuration = new Properties();
        Play.secretKey = "secret-secret-secret-secret";
    }

    @Test
    public void verifyError() {
        String field = "f1";

        assertThat(Validation.error(field)).isNull();
        assertThat(Validation.errors(field)).isEmpty();

        String errorMsg = "My errorMessage";

        Validation.addError(field, errorMsg);

        assertThat( Validation.error(field).getMessageKey()).isEqualTo(errorMsg);
        assertThat(Validation.errors(field)).containsOnly(Validation.error(field));

        // ticket [#109] - add an error with null-key
        Validation.addError(null, errorMsg);
        // make sure this null key does not break stuff
        assertThat( Validation.error(field).getMessageKey()).isEqualTo(errorMsg);
        assertThat(Validation.errors(field)).containsOnly(Validation.error(field));

    }
    
    @Test
    public void addErrorTest(){
        String field = "f1";
        String field2 = "f1.element";

        String errorMsg = "My errorMessage";
        
        Validation.addError(field, errorMsg);  
        Validation.addError(field, errorMsg); 
        
        Validation.addError(field2, errorMsg);
        
        assertThat( Validation.error(field).getMessageKey()).isEqualTo(errorMsg);
        
        // Test avoid insert duplicate message key
        assertEquals(2, Validation.errors().size());
        
        assertEquals(1, Validation.errors(field).size());
        assertEquals(1, Validation.errors(field2).size());
        
        Validation.clear();
        
        // Test clear empty the list
        assertEquals(0, Validation.errors().size());
        assertEquals(0, Validation.errors(field).size());
        assertEquals(0, Validation.errors(field2).size());
        
        String errorMsgWithParam = "My errorMessage: %2$s";
        
        Validation.addError(field, errorMsgWithParam, "param1");  
        Validation.addError(field, errorMsgWithParam, "param2"); 
                
        assertThat( Validation.error(field).getMessageKey()).isEqualTo(errorMsgWithParam);
        
        // Test avoid insert duplicate message key
        assertEquals(1, Validation.errors().size());
        
        assertEquals(1, Validation.errors(field).size());
        
        assertEquals("My errorMessage: param1", Validation.error(field).message());
    }
    
    @Test
    public void removeErrorTest(){
        String field = "f1";
        String field2 = "f1.element";

        String errorMsg = "My errorMessage";
        String errorMsg2 = "My errorMessage2";
        Validation.addError(field, errorMsg); 
        Validation.addError(field, errorMsg2); 
        
        Validation.addError(field2, errorMsg);
        Validation.addError(field2, errorMsg2);

        // Check the first error
        assertThat( Validation.error(field).getMessageKey()).isEqualTo(errorMsg);
        assertEquals(4, Validation.current().errors.size());   
        
        // Remove Errors on field2
        Validation.removeErrors(field2);
        
        assertEquals(2, Validation.errors().size());  
        assertEquals(2, Validation.errors(field).size()); 
        assertEquals(0, Validation.errors(field2).size()); 
        
        // Restore error on field2
        Validation.addError(field2, errorMsg);
        Validation.addError(field2, errorMsg2);
        
        assertEquals(4, Validation.current().errors.size()); 
        
        // Remove Errors on field
        Validation.removeErrors(field);
        
        assertEquals(2, Validation.errors().size());  
        assertEquals(0, Validation.errors(field).size()); 
        assertEquals(2, Validation.errors(field2).size());         
    }
    
    @Test
    public void removeErrorMessageTest(){
        String field = "f1";
        String field2 = "f1.element";

        String errorMsg = "My errorMessage";
        String errorMsg2 = "My errorMessage2";
        Validation.addError(field, errorMsg); 
        Validation.addError(field, errorMsg2); 
        
        Validation.addError(field2, errorMsg);
        Validation.addError(field2, errorMsg2);

        // Check the first error
        assertThat( Validation.error(field).getMessageKey()).isEqualTo(errorMsg);
        assertEquals(4, Validation.current().errors.size());   
        
        // Remove Errors on field2
        Validation.removeErrors(field2, errorMsg);
        
        assertEquals(3, Validation.errors().size());  
        assertEquals(2, Validation.errors(field).size()); 
        assertEquals(1, Validation.errors(field2).size()); 
        
        assertThat( Validation.error(field2).getMessageKey()).isEqualTo(errorMsg2);
        
        // Restore error on field2
        Validation.addError(field2, errorMsg);
        Validation.addError(field2, errorMsg2);
        
        assertEquals(4, Validation.current().errors.size()); 
        
        // Remove Errors on field
        Validation.removeErrors(field, errorMsg);
        
        assertEquals(3, Validation.errors().size());  
        assertEquals(1, Validation.errors(field).size()); 
        assertEquals(2, Validation.errors(field2).size()); 
        
        assertThat( Validation.error(field).getMessageKey()).isEqualTo(errorMsg2);
    }
    
    @Test
    public void insertErrorTest(){
        String field = "f1";

        String errorMsg = "My errorMessage";
        String errorMsg2 = "My errorMessage2";
        Validation.addError(field, errorMsg); 
        Validation.insertError(0, field, errorMsg2); 
        

        // Check the first error
        assertThat( Validation.error(field).getMessageKey()).isEqualTo(errorMsg2);
        assertEquals(2, Validation.current().errors.size());    
    }

    @Test
    public void restoreEmptyVariable() {
        Messages.defaults.setProperty("validation.error.missingName", "%s is invalid, given: '%s'");
        Play.secretKey = "secret-secret-secret-secret";
        Validation.addError("user.name", "validation.error.missingName", "");
        Validation.keep();
        validationPlugin.save(request, response);

        request.cookies = response.cookies;

        Validation restored = validationPlugin.restore(request);
        assertEquals("user.name is invalid, given: ''", restored.errors.get(0).message());
    }

    @Test
    public void restoreCookieWithNullValue() {
        request.cookies = Map.of("PLAY_ERRORS", new Http.Cookie("PLAY_ERRORS", null));

        Validation restored = validationPlugin.restore(request);
        assertThat(restored.errors).hasSize(0);
    }

    @Test
    public void restoreCookieWithBlankValue() {
        request.cookies = Map.of("PLAY_ERRORS", new Http.Cookie("PLAY_ERRORS", " "));

        Validation restored = validationPlugin.restore(request);
        assertThat(restored.errors).hasSize(0);
    }
}

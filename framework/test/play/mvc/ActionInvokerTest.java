package play.mvc;

import org.junit.Before;
import org.junit.Test;
import play.PlayBuilder;
import play.data.binding.CachedBoundActionMethodArgs;
import play.exceptions.PlayException;
import play.exceptions.UnexpectedException;
import play.mvc.Scope.Session;
import play.mvc.results.Forbidden;
import play.mvc.results.Result;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.Assert.*;
import static play.mvc.ActionInvokerTest.TestInterceptor.aftersCounter;
import static play.mvc.ActionInvokerTest.TestInterceptor.beforesCounter;

public class ActionInvokerTest {
    private Object[] noArgs = new Object[0];
    Http.Request request = new Http.Request();
    Session session = new Session();

    @Before
    public void setUp() throws Exception {
        new PlayBuilder().build();
        Http.Request.removeCurrent();
        CachedBoundActionMethodArgs.init();
        beforesCounter = 0;
        aftersCounter = 0;
    }

    @org.junit.After
    public void tearDown() {
        CachedBoundActionMethodArgs.clear();
    }

    @Test
    public void invokeStaticJavaMethod() throws Exception {
        request.controllerClass = TestController.class;
        assertEquals("static", ActionInvoker.invokeControllerMethod(request, session, TestController.class.getMethod("staticJavaMethod"), noArgs));
    }

    @Test
    public void invokeNonStaticJavaMethod() throws Exception {
        request.controllerClass = TestController.class;
        request.controllerInstance = new TestController();

        assertEquals("non-static-parent", ActionInvoker.invokeControllerMethod(request, session, TestController.class.getMethod("nonStaticJavaMethod"), noArgs));
    }

    @Test
    public void invokeNonStaticJavaMethodInChildController() throws Exception {
        request.controllerClass = TestChildController.class;
        request.controllerInstance = new TestChildController();

        assertEquals("non-static-child", ActionInvoker.invokeControllerMethod(request, session, TestChildController.class.getMethod("nonStaticJavaMethod"), noArgs));
    }

    @Test
    public void invokeNonStaticJavaMethodWithNonStaticWith() throws Exception {
        request.controllerClass = TestControllerWithWith.class;
        request.controllerInstance = new TestControllerWithWith();
        executeMethod("handleBefores", request, session);
        assertEquals("non-static", ActionInvoker.invokeControllerMethod(request, session, TestControllerWithWith.class.getMethod("nonStaticJavaMethod")));
        executeMethod("handleAfters", request, session);
        assertEquals(1, beforesCounter);
        assertEquals(1, aftersCounter);
    }

    private void executeMethod(String methodName, Http.Request request, Session session) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Method method = ActionInvoker.class.getDeclaredMethod(methodName, Http.Request.class, Session.class);
        method.setAccessible(true);
        method.invoke(null, request, session);
    }

    @Test
    public void controllerInstanceIsPreservedForAllControllerMethodInvocations() throws Exception {
        request.controllerClass = FullCycleTestController.class;
        request.controllerInstance = new FullCycleTestController();

        Controller controllerInstance = (Controller) ActionInvoker.invokeControllerMethod(request, session, FullCycleTestController.class.getMethod("before"), noArgs);
        assertSame(controllerInstance, request.controllerInstance);

        controllerInstance = (Controller) ActionInvoker.invokeControllerMethod(request, session, FullCycleTestController.class.getMethod("action"), noArgs);
        assertSame(controllerInstance, request.controllerInstance);

        controllerInstance = (Controller) ActionInvoker.invokeControllerMethod(request, session, FullCycleTestController.class.getMethod("after"), noArgs);
        assertSame(controllerInstance, request.controllerInstance);
    }

    @Test
    public void invocationUnwrapsPlayException() throws Exception {
        final UnexpectedException exception = new UnexpectedException("unexpected");

        class AController extends Controller {
            public void action() {
                throw exception;
            }
        }

        try {
            ActionInvoker.invoke(AController.class.getMethod("action"), new AController());
            fail();
        }
        catch (PlayException e) {
            assertEquals(exception, e);
        }
    }

    @Test
    public void invocationUnwrapsResult() throws Exception {
        final Result result = new Forbidden("unexpected");

        class AController extends Controller {
            public void action() {
                throw result;
            }
        }

        try {
            ActionInvoker.invoke(AController.class.getMethod("action"), new AController());
            fail();
        }
        catch (Result e) {
            assertEquals(result, e);
        }
    }

    @Test
    public void testFindActionMethod() throws Exception {
        assertNull(ActionInvoker.findActionMethod("notExistingMethod", ActionClass.class));

        ensureNotActionMethod("privateMethod");
        ensureNotActionMethod("beforeMethod");
        ensureNotActionMethod("afterMethod");
        ensureNotActionMethod("utilMethod");
        ensureNotActionMethod("catchMethod");
        ensureNotActionMethod("finallyMethod");

        Method m = ActionInvoker.findActionMethod("actionMethod", ActionClass.class);
        assertNotNull(m);
        assertEquals("actionMethod", m.invoke( new ActionClass()));

        //test that it works with subclassing
        m = ActionInvoker.findActionMethod("actionMethod", ActionClassChild.class);
        assertNotNull(m);
        assertEquals("actionMethod", m.invoke( new ActionClassChild()));
    }

    private void ensureNotActionMethod(String name) throws NoSuchMethodException {
        assertNull(ActionInvoker.findActionMethod(ActionClass.class.getDeclaredMethod(name).getName(), ActionClass.class));
    }

    public static class TestController extends Controller {
        public static String staticJavaMethod() {
            return "static";
        }

        public String nonStaticJavaMethod() {
            return "non-static-" + getPrefix();
        }

        protected String getPrefix() {
            return "parent";
        }
    }

    public static class TestChildController extends TestController {
        @Override
        protected String getPrefix() {
            return "child";
        }
    }

    public static class FullCycleTestController extends Controller {
        @play.mvc.Before  public Controller before() {
            return this;
        }

        public Controller action() {
            return this;
        }

        @play.mvc.After public Controller after() {
            return this;
        }
    }

    private static class ActionClass {

        private static String privateMethod() {
            return "private";
        }


        public static String actionMethod() {
            return "actionMethod";
        }

        @play.mvc.Before
        public static String beforeMethod() {
            return "before";
        }

        @After
        public static String afterMethod() {
            return "after";
        }

        @Util
        public static void utilMethod() {
        }

        @Catch
        public static void catchMethod() {
        }

        @Finally
        public static String finallyMethod() {
            return "finally";
        }

    }

    private static class ActionClassChild extends ActionClass {
    }

    @With(TestInterceptor.class)
    public static class TestControllerWithWith extends Controller {
        public String nonStaticJavaMethod() {
            return "non-static";
        }
    }
    
    public static class TestInterceptor extends Controller {
        static int beforesCounter, aftersCounter;
        
        @play.mvc.Before
        public void beforeMethod() {beforesCounter++;}

        @After
        public void afterMethod() {aftersCounter++;}
    }
}

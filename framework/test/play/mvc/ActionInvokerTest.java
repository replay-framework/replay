package play.mvc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import play.PlayBuilder;
import play.data.binding.CachedBoundActionMethodArgs;
import play.exceptions.PlayException;
import play.exceptions.UnexpectedException;
import play.mvc.Scope.Session;
import play.mvc.results.Forbidden;
import play.mvc.results.Result;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static play.mvc.ActionInvokerTest.TestInterceptor.aftersCounter;
import static play.mvc.ActionInvokerTest.TestInterceptor.beforesCounter;

public class ActionInvokerTest {
    private final Object[] noArgs = new Object[0];
    private final Http.Request request = new Http.Request();
    private final Session session = new Session();
    private final ActionInvoker invoker = new ActionInvoker(mock(SessionStore.class));

    @BeforeEach
    public void setUp() {
        new PlayBuilder().build();
        Http.Request.removeCurrent();
        CachedBoundActionMethodArgs.init();
        beforesCounter = 0;
        aftersCounter = 0;
    }

    @org.junit.jupiter.api.AfterEach
    public void tearDown() {
        CachedBoundActionMethodArgs.clear();
    }

    @Test
    public void invokeStaticJavaMethod() throws Exception {
        request.controllerClass = TestController.class;
      assertThat(ActionInvoker.invokeControllerMethod(request, session, TestController.class.getMethod("staticJavaMethod"), noArgs)).isEqualTo("static");
    }

    @Test
    public void invokeNonStaticJavaMethod() throws Exception {
        request.controllerClass = TestController.class;
        request.controllerInstance = new TestController();

      assertThat(ActionInvoker.invokeControllerMethod(request, session, TestController.class.getMethod("nonStaticJavaMethod"), noArgs)).isEqualTo("non-static-parent");
    }

    @Test
    public void invokeNonStaticJavaMethodInChildController() throws Exception {
        request.controllerClass = TestChildController.class;
        request.controllerInstance = new TestChildController();

      assertThat(ActionInvoker.invokeControllerMethod(request, session, TestChildController.class.getMethod("nonStaticJavaMethod"), noArgs)).isEqualTo("non-static-child");
    }

    @Test
    public void invokeNonStaticJavaMethodWithNonStaticWith() throws Exception {
        request.controllerClass = TestControllerWithWith.class;
        request.controllerInstance = new TestControllerWithWith();
        executeMethod("handleBefores", request, session);
      assertThat(ActionInvoker.invokeControllerMethod(request, session, TestControllerWithWith.class.getMethod("nonStaticJavaMethod"))).isEqualTo("non-static");
        executeMethod("handleAfters", request, session);
      assertThat(beforesCounter).isEqualTo(1);
      assertThat(aftersCounter).isEqualTo(1);
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
        assertThat(request.controllerInstance).isSameAs(controllerInstance);

        controllerInstance = (Controller) ActionInvoker.invokeControllerMethod(request, session, FullCycleTestController.class.getMethod("action"), noArgs);
        assertThat(request.controllerInstance).isSameAs(controllerInstance);

        controllerInstance = (Controller) ActionInvoker.invokeControllerMethod(request, session, FullCycleTestController.class.getMethod("after"), noArgs);
        assertThat(request.controllerInstance).isSameAs(controllerInstance);
    }

    @Test
    public void invocationUnwrapsPlayException() {
        final UnexpectedException exception = new UnexpectedException("unexpected");

        class AController extends Controller {
            public void action() {
                throw exception;
            }
        }

        assertThatThrownBy(() -> ActionInvoker.invoke(AController.class.getMethod("action"), new AController()))
          .isInstanceOf(PlayException.class)
          .hasMessage("unexpected");
    }

    @Test
    public void invocationUnwrapsResult() {
        final Result result = new Forbidden("unexpected");

        class AController extends Controller {
            public void action() {
                throw result;
            }
        }

        assertThatThrownBy(() -> ActionInvoker.invoke(AController.class.getMethod("action"), new AController()))
          .isInstanceOf(Result.class)
          .isEqualTo(result);
    }

    @Test
    public void findActionMethod() throws Exception {
      assertThat(ActionInvoker.findActionMethod("notExistingMethod", ActionClass.class)).isNull();

        ensureNotActionMethod("privateMethod");
        ensureNotActionMethod("beforeMethod");
        ensureNotActionMethod("afterMethod");
        ensureNotActionMethod("utilMethod");
        ensureNotActionMethod("catchMethod");
        ensureNotActionMethod("finallyMethod");

        Method m = ActionInvoker.findActionMethod("actionMethod", ActionClass.class);
        assertThat(m).isNotNull();
      assertThat(m.invoke(new ActionClass())).isEqualTo("actionMethod");

        //test that it works with subclassing
        m = ActionInvoker.findActionMethod("actionMethod", ActionClassChild.class);
        assertThat(m).isNotNull();
      assertThat(m.invoke(new ActionClassChild())).isEqualTo("actionMethod");
    }

    @Test
    public void everyActionNeedsSessionByDefault() throws NoSuchMethodException {
        request.invokedMethod = ActionClass.class.getMethod("actionMethod");

        assertThat(invoker.actionNeedsSession(request)).isTrue();
    }

    @Test
    public void actionCanOptOutUsingSession() throws NoSuchMethodException {
        request.invokedMethod = ActionClass.class.getMethod("prefetchUserData");

        assertThat(invoker.actionNeedsSession(request)).isFalse();
    }

    private void ensureNotActionMethod(String name) throws NoSuchMethodException {
      assertThat(ActionInvoker.findActionMethod(ActionClass.class.getDeclaredMethod(name).getName(), ActionClass.class)).isNull();
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
        @Before  public Controller before() {
            return this;
        }

        public Controller action() {
            return this;
        }

        @After public Controller after() {
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

        @NoSession
        public String prefetchUserData() {
            return "this is a background request which should not affect session";
        }

        @Before
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
        
        @Before
        public void beforeMethod() {beforesCounter++;}

        @After
        public void afterMethod() {aftersCounter++;}
    }
}

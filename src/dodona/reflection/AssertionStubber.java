package dodona.reflection;

import java.lang.Class;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;

public class AssertionStubber {

    public <T> T stub(Class<T> expectedInterface, Class<?> solution, Object... constructionParameters) {
        return expectedInterface.cast(Proxy.newProxyInstance(
            expectedInterface.getClassLoader(),
            new Class<?>[] { expectedInterface },
            new AssertingInvocationHandler(solution, constructionParameters)
        ));
    }

    class AssertingInvocationHandler implements InvocationHandler {

        private Class<?> solutionClass;
        private Object solutionInstance;

        public AssertingInvocationHandler(Class<?> solution, Object... constructionParameters) {
            // Getting the relevant constructor.
            Constructor<?> constructor = null;
            Class<?>[] constructionParameterTypes = new Class<?>[constructionParameters.length];
            for(int i = 0; i < constructionParameters.length; i++) {
                constructionParameterTypes[i] = constructionParameters[i].getClass();
            }
            try {
                constructor = solution.getConstructor(constructionParameterTypes);
            } catch(NoSuchMethodException e) { missingConstructor(solution, constructionParameterTypes); }

            solutionClass = solution;
            try {
                solutionInstance = constructor.newInstance(constructionParameters);
            } catch(InstantiationException e) { testclassIsAbstract();
            } catch(IllegalAccessException e) { illegalConstructorAccess();
            } catch(InvocationTargetException e) { throw new RuntimeException(e.getCause());
            }
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Method solutionMethod = null;
            try {
                solutionMethod = solutionClass.getMethod(method.getName(), method.getParameterTypes());
            } catch(NoSuchMethodException e) { missingMethod(method); }

            return solutionMethod.invoke(solutionInstance, args);
        }

    }

    /* =========================================================================
     * Assertions
     */
    private String missingMethod(String returnName, Class<?>... parameterTypes) {
        return returnName + "(" + Stream.of(parameterTypes).map(Class::getSimpleName).collect(Collectors.joining(",")) + ")";
    }

    private void missingConstructor(Class<?> solution, Class<?>... constructionParameterTypes) {
        Assert.fail("Missing constructor: " + missingMethod(solution.getSimpleName(), constructionParameterTypes));
    }

    private void missingMethod(Method method) {
        Assert.fail("Missing method: " + missingMethod(method.getReturnType().getSimpleName() + " " + method.getName(), method.getParameterTypes()));
    }

    private void testclassIsAbstract() {
        Assert.fail("Tested class is abstract");
    }

    private void illegalConstructorAccess() {
        Assert.fail("Illegal Constructor access");
    }

}

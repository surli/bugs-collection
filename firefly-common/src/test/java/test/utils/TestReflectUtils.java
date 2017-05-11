package test.utils;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.junit.Assert;
import org.junit.Test;

import com.firefly.utils.ReflectUtils;
import com.firefly.utils.ReflectUtils.ArrayProxy;
import com.firefly.utils.ReflectUtils.FieldProxy;
import com.firefly.utils.ReflectUtils.MethodProxy;
import com.firefly.utils.classproxy.ArrayProxyFactoryUsingJavaCompiler;
import com.firefly.utils.classproxy.FieldProxyFactoryUsingJavaCompiler;
import com.firefly.utils.classproxy.MethodProxyFactoryUsingJavaCompiler;

public class TestReflectUtils {

    @Test
    public void testGetterAndSetterMethod() {
        Assert.assertThat(ReflectUtils.getGetterMethod(Foo.class, "name").getName(), is("getName"));
        Assert.assertThat(ReflectUtils.getGetterMethod(Foo.class, "failure").getName(), is("isFailure"));

        Assert.assertThat(ReflectUtils.getSetterMethod(Foo.class, "name").getName(), is("setName"));
        Assert.assertThat(ReflectUtils.getSetterMethod(Foo.class, "failure").getName(), is("setFailure"));

        Assert.assertThat(ReflectUtils.getSetterMethod(Foo.class, "iPad").getName(), is("setiPad"));
        Assert.assertThat(ReflectUtils.getSetterMethod(Foo.class, "iPhone").getName(), is("setiPhone"));

        Assert.assertThat(ReflectUtils.getGetterMethod(Foo.class, "iPad").getName(), is("isiPad"));
        Assert.assertThat(ReflectUtils.getGetterMethod(Foo.class, "iPhone").getName(), is("getiPhone"));
    }

    @Test
    public void testProxyMethod() throws Throwable {
        Foo foo = new Foo();
        MethodProxy proxy = ReflectUtils.getMethodProxy(Foo.class.getMethod("setProperty", String.class, boolean.class));
        Assert.assertThat(proxy.invoke(foo, "proxy foo", true), nullValue());
        Assert.assertThat(foo.getName(), is("proxy foo"));
        Assert.assertThat(foo.isFailure(), is(true));

        proxy = ReflectUtils.getMethodProxy(ReflectUtils.getGetterMethod(Foo.class, "name"));
        Assert.assertThat(proxy.invoke(foo), is("proxy foo"));

        proxy = ReflectUtils.getMethodProxy(ReflectUtils.getGetterMethod(Foo.class, "failure"));
        Assert.assertThat(proxy.invoke(foo), is(true));

        proxy = ReflectUtils.getMethodProxy(ReflectUtils.getSetterMethod(Foo.class, "price"));
        Assert.assertThat(proxy.invoke(foo, 35.5), nullValue());
        Assert.assertThat(foo.getPrice(), is(35.5));

        MethodProxyFactoryUsingJavaCompiler proxyUsingJavaCompiler = MethodProxyFactoryUsingJavaCompiler.INSTANCE;
        Foo foo2 = new Foo();
        MethodProxy proxy2 = proxyUsingJavaCompiler.getMethodProxy(Foo.class.getMethod("setProperty", String.class, boolean.class));
        Assert.assertThat(proxy2.invoke(foo2, "proxy foo", true), nullValue());
        Assert.assertThat(foo2.getName(), is("proxy foo"));
        Assert.assertThat(foo2.isFailure(), is(true));

        proxy2 = proxyUsingJavaCompiler.getMethodProxy(ReflectUtils.getGetterMethod(Foo.class, "name"));
        Assert.assertThat(proxy2.invoke(foo2), is("proxy foo"));


        proxy2 = proxyUsingJavaCompiler.getMethodProxy(ReflectUtils.getGetterMethod(Foo.class, "failure"));
        Assert.assertThat(proxy2.invoke(foo2), is(true));

        proxy2 = proxyUsingJavaCompiler.getMethodProxy(ReflectUtils.getSetterMethod(Foo.class, "price"));
        Assert.assertThat(proxy2.invoke(foo2, 35.5), nullValue());
        Assert.assertThat(foo2.getPrice(), is(35.5));
    }

    @Test
    public void testProxyMethodCache() throws Throwable {
        MethodProxyFactoryUsingJavaCompiler proxyUsingJavaCompiler = MethodProxyFactoryUsingJavaCompiler.INSTANCE;
        Foo foo2 = new Foo();
        MethodProxy proxy2 = proxyUsingJavaCompiler.getMethodProxy(Foo.class.getMethod("setProperty", String.class, boolean.class));
        Assert.assertThat(proxy2.invoke(foo2, "proxy foo", true), nullValue());
        Assert.assertThat(foo2.getName(), is("proxy foo"));
        Assert.assertThat(foo2.isFailure(), is(true));

        proxy2 = proxyUsingJavaCompiler.getMethodProxy(Foo.class.getMethod("setProperty", String.class, boolean.class));
        Assert.assertThat(proxy2.invoke(foo2, "proxy foo", true), nullValue());
        Assert.assertThat(foo2.getName(), is("proxy foo"));
        Assert.assertThat(foo2.isFailure(), is(true));
    }

    @Test
    public void testProxyField() throws Throwable {
        Foo foo = new Foo();
        Field num2 = Foo.class.getField("num2");
        Field info = Foo.class.getField("info");

        FieldProxy proxyNum2 = ReflectUtils.getFieldProxy(num2);
        proxyNum2.set(foo, 30);
        Assert.assertThat(proxyNum2.get(foo), is(30));

        FieldProxy proxyInfo = ReflectUtils.getFieldProxy(info);
        proxyInfo.set(foo, "test info 0");
        Assert.assertThat(proxyInfo.get(foo), is("test info 0"));

        ReflectUtils.setProperty(foo, "name", "hello");
        Assert.assertThat(ReflectUtils.getProperty(foo, "name"), is("hello"));


        Foo foo2 = new Foo();

        proxyNum2 = FieldProxyFactoryUsingJavaCompiler.INSTANCE.getFieldProxy(num2);
        proxyNum2.set(foo2, 303);
        Assert.assertThat(proxyNum2.get(foo2), is(303));

        proxyInfo = FieldProxyFactoryUsingJavaCompiler.INSTANCE.getFieldProxy(info);
        proxyInfo.set(foo2, "test info 03");
        Assert.assertThat(proxyInfo.get(foo2), is("test info 03"));
    }

    @Test
    public void testGetAndSet() throws Throwable {
        Foo foo = new Foo();
        ReflectUtils.set(foo, "price", 4.44);
        ReflectUtils.set(foo, "failure", true);
        ReflectUtils.set(foo, "name", "foo hello");

        Assert.assertThat(ReflectUtils.get(foo, "price"), is(4.44));
        Assert.assertThat(ReflectUtils.get(foo, "failure"), is(true));
        Assert.assertThat(ReflectUtils.get(foo, "name"), is("foo hello"));
    }

    @Test
    public void testArray() throws Throwable {
        ArrayProxyFactoryUsingJavaCompiler arrayProxyFactory = ArrayProxyFactoryUsingJavaCompiler.INSTANCE;

        int[] intArr = new int[5];
        Integer[] intArr2 = new Integer[10];

        ArrayProxy intArrProxy = arrayProxyFactory.getArrayProxy(intArr.getClass());
        ArrayProxy intArr2Proxy = arrayProxyFactory.getArrayProxy(intArr2.getClass());

        Assert.assertThat(ReflectUtils.arraySize(intArr), is(5));
        Assert.assertThat(ReflectUtils.arraySize(intArr2), is(10));

        Assert.assertThat(intArrProxy.size(intArr), is(5));
        Assert.assertThat(intArr2Proxy.size(intArr2), is(10));

        ReflectUtils.arraySet(intArr, 0, 33);
        Assert.assertThat(ReflectUtils.arrayGet(intArr, 0), is(33));

        ReflectUtils.arraySet(intArr2, intArr2.length - 1, 55);
        Assert.assertThat(ReflectUtils.arrayGet(intArr2, 9), is(55));

        intArrProxy.set(intArr, 1, 23);
        Assert.assertThat(intArrProxy.get(intArr, 1), is(23));

        intArr2Proxy.set(intArr2, intArr2.length - 1, 65);
        Assert.assertThat(intArr2Proxy.get(intArr2, 9), is(65));
    }

    @Test
    public void testCopy() throws Throwable {
        Foo foo = new Foo();
        foo.setName("hello foo");
        foo.setPrice(3.3);
        foo.setNumber(40);

        Foo2 foo2 = new Foo2();
        foo2.setName("hello foo2");

        ReflectUtils.copy(foo2, foo);
        Assert.assertThat(foo.getName(), is("hello foo2"));
        Assert.assertThat(foo.getNumber(), is(40));
    }

    public static void main(String[] args) throws Throwable {
        Foo foo = new Foo();
        Field num2 = Foo.class.getField("num2");
        Field info = Foo.class.getField("info");

        FieldProxy proxyNum2 = FieldProxyFactoryUsingJavaCompiler.INSTANCE.getFieldProxy(num2);
        proxyNum2.set(foo, 30);
        System.out.println(proxyNum2.get(foo));

        FieldProxy proxyInfo = FieldProxyFactoryUsingJavaCompiler.INSTANCE.getFieldProxy(info);
        proxyInfo.set(foo, "test info 0");
        System.out.println(proxyInfo.get(foo));
    }

    public static void main7(String[] args) throws Throwable {
        Foo foo = new Foo();
        MethodProxyFactoryUsingJavaCompiler proxy = MethodProxyFactoryUsingJavaCompiler.INSTANCE;
        MethodProxy setPriceProxy = proxy.getMethodProxy(ReflectUtils.getSetterMethod(Foo.class, "price"));
        setPriceProxy.invoke(foo, 3.3);

        MethodProxy getPriceProxy = proxy.getMethodProxy(ReflectUtils.getGetterMethod(Foo.class, "price"));
        System.out.println(getPriceProxy.invoke(foo));

        MethodProxy getPriceProxy2 = ReflectUtils.getMethodProxy(ReflectUtils.getGetterMethod(Foo.class, "price"));
        System.out.println(getPriceProxy2.invoke(foo));

        int times = 1000 * 1000 * 1000;
        long start = System.currentTimeMillis();
        for (int i = 0; i < times; i++) {
            getPriceProxy.invoke(foo);
        }
        long end = System.currentTimeMillis();
        System.out.println("java compiler -> " + (end - start));

        start = System.currentTimeMillis();
        for (int i = 0; i < times; i++) {
            getPriceProxy.invoke(foo);
        }
        end = System.currentTimeMillis();
        System.out.println("java compiler -> " + (end - start));

        start = System.currentTimeMillis();
        for (int i = 0; i < times; i++) {
            getPriceProxy2.invoke(foo);
        }
        end = System.currentTimeMillis();
        System.out.println("javassist -> " + (end - start));

        start = System.currentTimeMillis();
        for (int i = 0; i < times; i++) {
            getPriceProxy2.invoke(foo);
        }
        end = System.currentTimeMillis();
        System.out.println("javassist -> " + (end - start));
    }

    public static void main5(String[] args) throws Throwable {
        Integer[] array = new Integer[]{77, 88, 99, 0, 11};
        int[] array2 = new int[]{15, 44, 55, 66};
        ArrayProxy arrayProxy = ArrayProxyFactoryUsingJavaCompiler.INSTANCE.getArrayProxy(array.getClass());
        ArrayProxy arrayProxy2 = ArrayProxyFactoryUsingJavaCompiler.INSTANCE.getArrayProxy(array2.getClass());
        System.out.println(arrayProxy.get(array, 2));
        System.out.println(arrayProxy2.get(array2, 3));

        ArrayProxy arrayProxy3 = ReflectUtils.getArrayProxy(array.getClass());
        ArrayProxy arrayProxy4 = ReflectUtils.getArrayProxy(array2.getClass());
        System.out.println(arrayProxy3.get(array, 2));
        System.out.println(arrayProxy4.get(array2, 3));

        int times = 1000 * 1000 * 1000;
        long start = System.currentTimeMillis();
        for (int i = 0; i < times; i++) {
            arrayProxy2.get(array2, 3);
        }
        long end = System.currentTimeMillis();
        System.out.println("java compiler -> " + (end - start));

        start = System.currentTimeMillis();
        for (int i = 0; i < times; i++) {
            arrayProxy4.get(array2, 3);
        }
        end = System.currentTimeMillis();
        System.out.println("javassist -> " + (end - start));

    }

    public static void main1(String[] args) throws Throwable {
        set(new int[]{15, 44, 55, 66}, 0);

        set(new Integer[]{77, 88, 99, 0, 11}, 0);
    }

    public static void set(Object obj, int index) throws Throwable {
//		System.out.println(((int[])obj)[index]);
//		int[] arr = (int[])obj;
//		System.out.println(arr[index]);
//		System.out.println(obj.getClass().getCanonicalName());
//		System.out.println(obj.getClass().getComponentType().getCanonicalName());
//		System.out.println(ReflectUtils.createArraySizeCode(obj.getClass()));

        ReflectUtils.getArrayProxy(obj.getClass()).set(obj, index, 30);
        System.out.println(ReflectUtils.getArrayProxy(obj.getClass()).size(obj));
        System.out.println(ReflectUtils.getArrayProxy(obj.getClass()).get(obj, index));
    }

    public static void main2(String[] args) throws Throwable {
        Foo foo = new Foo();
        MethodProxy proxy = ReflectUtils.getMethodProxy(ReflectUtils.getGetterMethod(Foo.class, "failure"));
        System.out.println(proxy.invoke(foo));

        Field field = Foo.class.getField("num2");
        System.out.println(field.getType());
        System.out.println(field.getName());
        Field info = Foo.class.getField("info");
        FieldProxy proxyInfo = ReflectUtils.getFieldProxy(info);
        proxyInfo.set(foo, "test info 0");
        System.out.println(proxyInfo.get(foo));
//		System.out.println(ReflectUtils.createFieldGetterMethodCode(field));
//		System.out.println(ReflectUtils.createFieldGetterMethodCode(Foo.class.getField("info")));
//		System.out.println(ReflectUtils.createFieldSetterMethodCode(field));
//		System.out.println(ReflectUtils.createFieldSetterMethodCode(Foo.class.getField("info")));
    }

    public static void main4(String[] args) throws Throwable {
        Bar bar = new Bar();
        ReflectUtils.setProperty(bar, "name", "hello");
        System.out.println(ReflectUtils.getProperty(bar, "name"));
        System.out.println(bar.name);

        int times = 1000 * 1000 * 100;
        String a = null;
        long start = System.currentTimeMillis();
        for (int i = 0; i < times; i++) {
            a = (String) ReflectUtils.getFields(bar.getClass()).get("name").get(bar); //bar.getClass().getField("name").get(bar);
        }
        long end = System.currentTimeMillis();
        System.out.println("protogenic invocation: " + (end - start) + "|" + a);

        start = System.currentTimeMillis();
        for (int i = 0; i < times; i++) {
            a = (String) ReflectUtils.getProperty(bar, "name");
        }
        end = System.currentTimeMillis();
        System.out.println("proxy invocation: " + (end - start) + "|" + a);
    }

    public static void main6(String[] args) throws Throwable {
        int times = 1000 * 1000 * 1000;

        Foo foo = new Foo();
        Method method = Foo.class.getMethod("setProperty", String.class, boolean.class);
        method.setAccessible(true);
        MethodProxy proxy = ReflectUtils.getMethodProxy(method);

        long start = System.currentTimeMillis();
        for (int i = 0; i < times; i++) {
            proxy.invoke(foo, "method b", true);
        }
        long end = System.currentTimeMillis();
        System.out.println(foo.getName() + " invoke: " + (end - start) + "ms");

        start = System.currentTimeMillis();
        for (int i = 0; i < times; i++) {
            proxy.invoke(foo, "method b", true);
        }
        end = System.currentTimeMillis();
        System.out.println(foo.getName() + " invoke: " + (end - start) + "ms");

        start = System.currentTimeMillis();
        for (int i = 0; i < times; i++) {
            method.invoke(foo, "method a", true);
        }
        end = System.currentTimeMillis();
        System.out.println(foo.getName() + " invoke: " + (end - start) + "ms");

        start = System.currentTimeMillis();
        for (int i = 0; i < times; i++) {
            method.invoke(foo, "method a", true);
        }
        end = System.currentTimeMillis();
        System.out.println(foo.getName() + " invoke: " + (end - start) + "ms");

        start = System.currentTimeMillis();
        for (int i = 0; i < times; i++) { // 直接方法调用
            foo.setProperty("method c", true);
        }
        end = System.currentTimeMillis();
        System.out.println(foo.getName() + " invoke: " + (end - start) + "ms");

        start = System.currentTimeMillis();
        for (int i = 0; i < times; i++) { // 直接方法调用
            foo.setProperty("method c", true);
        }
        end = System.currentTimeMillis();
        System.out.println(foo.getName() + " invoke: " + (end - start) + "ms");
    }

    public static class Bar {
        public String name;
    }

    public static class Foo2 {
        private String name;
        private Integer number;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getNumber() {
            return number;
        }

        public void setNumber(Integer number) {
            this.number = number;
        }

    }

    public static class Foo {
        private boolean failure;
        public String name;
        private int number;
        private double price;

        public int num2;
        public String info;

        private String iPhone;
        private boolean iPad;

        public String getiPhone() {
            return iPhone;
        }

        public void setiPhone(String iPhone) {
            this.iPhone = iPhone;
        }

        public boolean isiPad() {
            return iPad;
        }

        public void setiPad(boolean iPad) {
            this.iPad = iPad;
        }

        public int getNumber() {
            return number;
        }

        public void setNumber(int number) {
            this.number = number;
        }

        public double getPrice() {
            return price;
        }

        public void setPrice(double price) {
            this.price = price;
        }

        public boolean isFailure() {
            return failure;
        }

        public void setFailure(boolean failure) {
            this.failure = failure;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setProperty(String name, boolean failure) {
            this.name = name;
            this.failure = failure;
        }

    }

}

package net.gibr.util.hstack;

import static net.gibr.util.hstack.HStack.fold;
import static net.gibr.util.hstack.HStack.create;
import static net.gibr.util.hstack.HStack.swap;
import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.StringJoiner;

import org.junit.Test;

import net.gibr.util.hstack.HStack.Bottom;
import net.gibr.util.hstack.HStack.Result;

public class HStackTest {
    @Test
    public void testPushPeek() {
        String value = new String("a");
        Result<String, Bottom> stack = create().push(value);
        assertEquals("[a]", stack.toString());
        String actual = stack.peek();
        assertSame(value, actual);
    }

    @Test
    public void testFunction() {
        String actual = create().push("a").apply(String::toUpperCase).peek();
        assertEquals("A", actual);
    }

    @Test
    public void testBiFunction() {
        Result<String, Result<String, Bottom>> stack = create().push("a").push("b");
        assertEquals("[b, a]", stack.toString());
        String actual = fold(stack, String::concat).peek();
        assertEquals("ba", actual);
    }

    @Test
    public void testDuplicate() {
        Result<String, Result<String, Bottom>> stack = create().push("a").dup();
        assertEquals("[a, a]", stack.toString());
        String actual = fold(stack, String::concat).peek();
        assertEquals("aa", actual);
    }

    @Test
    public void testApplyRest() {
        Result<String, Result<String, Bottom>> stack = create().push("a").dup();
        assertEquals("[a, a]", stack.toString());
        Result<String, Result<String, Bottom>> applyRest = stack.applyRest((rest) -> rest.apply(String::toUpperCase));
        String actual = fold(applyRest, String::concat).peek();
        assertEquals("aA", actual);
    }

    @Test
    public void testSwap() {
        Result<Integer, Result<String, Bottom>> stack = create().push("a").push(1);
        assertEquals("[1, a]", stack.toString());
        Result<String, Result<Integer, Bottom>> actual = swap(stack);
        assertEquals("[a, 1]", actual.toString());
    }

    @Test
    public void testPop() {
        assertEquals("a", create().push("a").push("b").pop().peek());
    }

    @Test
    public void testEqualsHashcodeWithBottom() {
        assertTrue(create().equals(create()));
        assertFalse(create().equals(null));
    }

    @Test
    public void testEqualsHashWithNull() {
        Result<Void, Bottom> null1 = create().push(null);
        Result<String, Bottom> null2 = create().push(null);
        // equal even though they are different types because they are both
        // null.
        assertTrue(null1.equals(null2));
        assertEquals(null1.hashCode(), null2.hashCode());

        assertFalse(null1.equals(create()));
        assertFalse(null1.equals(null));
    }

    @Test
    public void testEqualsHashWithValue() {
        Result<String, Bottom> str1 = create().push("a");
        Result<String, Bottom> str2 = create().push("a");
        Result<String, Bottom> str3 = create().push("b");

        assertTrue(str1.equals(str2));
        assertEquals(str1.hashCode(), str2.hashCode());
        assertFalse(str1.equals(str3));
        // could fail but probably won't
        assertNotEquals(str1.hashCode(), str3.hashCode());
    }

    @Test
    public void testSame() {
        assertSame(create(), create());
        Result<String, Bottom> stack = create().push("a");
        assertEquals(stack, stack);
    }

    @Test
    public void testToStringWithNull() {
        assertEquals("[null]", create().push(null).toString());
    }

    @Test
    public void testSerialization() throws IOException, ClassNotFoundException {
        Result<String, Bottom> stackOut = create().push("a");
        ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
        ObjectOutputStream objOutput = new ObjectOutputStream(byteOutput);
        objOutput.writeObject(stackOut);
        objOutput.close();

        ByteArrayInputStream byteInput = new ByteArrayInputStream(byteOutput.toByteArray());
        ObjectInputStream objInput = new ObjectInputStream(byteInput);
        Object stackIn = objInput.readObject();

        assertEquals(stackOut, stackIn);
    }

    @Test
    public void testFoldLeft() {
        String a = "a";
        String b = "b";
        assertSame(a, create().foldL(a, null, null));
        assertSame(a, create().foldR(a, null, null));

        Result<String, Result<String, Bottom>> stack = create().push(b).push(a);

        assertEquals("a,b", stack.foldL(new StringJoiner(","), String::valueOf, StringJoiner::add).toString());
        assertEquals("b,a", stack.foldR(new StringJoiner(","), String::valueOf, StringJoiner::add).toString());

        assertEquals("ab", stack.foldL(String::valueOf, (x, y) -> x + y));
    }
}

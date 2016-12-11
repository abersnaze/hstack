package net.gibr.util.hstack;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * To start a new stack use:
 * <p>
 * {@code HStack.create()}
 * <p>
 * This abstract class is the base class for the heterogeneous stack and holder of all the static methods. The class {@link Result} is the API that will be used the most.
 * <p>
 * The methods that apply to more than one item of the stack have to exist only has static on this class to allow the enforcing the type constraints:
 * <ul>
 * <li>{@link HStack#fold(Result, BiFunction)}
 * <li>{@link HStack#swap(Result)}
 * </ul>
 * All of the other statics are here just for consistency and are no different then the instance methods on {@link Result} or {@link Bottom}.
 * <p>
 * 
 * @param <X>
 *            the recursive type of the whole stack
 */
public abstract class HStack<X extends HStack<X>> implements Serializable {
    private static final long serialVersionUID = 8164173014873566210L;

    /**
     * A static marker of the bottom of all stacks.
     */
    public static final class Bottom extends HStack<Bottom> {
        private static final long serialVersionUID = -2672954791136534732L;

        private Bottom() {
        }

        /**
         * Push a new value onto the stack.
         * 
         * @param value
         *            to be added to top of the stack
         */
        public <T> Result<T, Bottom> push(T value) {
            return HStack.push(this, value);
        }

        private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
            ois.defaultReadObject();
        }

        private Object readResolve() {
            return BOTTOM;
        }

        @Override
        public <V, R> R foldL(R seed, Function<? super Object, V> map, BiFunction<R, V, R> fold) {
            return seed;
        }

        @Override
        public <V, R> R foldR(R seed, Function<? super Object, V> map, BiFunction<R, V, R> fold) {
            return seed;
        }
    }

    /**
     * Captures the value for one element of the stack while recursively storing rest of the elements in the stack U.
     *
     * @param <T>
     *            type of the value.
     * @param <U>
     *            type for the rest of the stack.
     */
    public static final class Result<T, U extends HStack<U>> extends HStack<Result<T, U>> {
        private static final long serialVersionUID = -2289775929496261940L;

        private final U rest;
        private final T value;

        private Result(T value, U rest) {
            this.value = value;
            this.rest = rest;
        }

        /**
         * Apply a mapping function to the top value of the stack.
         * 
         * @param f
         *            a function that maps a of type T to a of type R.
         * @return a stack with a of type R on top.
         */
        public <R> Result<R, U> apply(Function<T, R> f) {
            return HStack.apply(this, f);
        }

        /**
         * Applies a function mapping the rest of the stack.
         * 
         * @param f
         *            the function that maps a stack of type U to a new stack of type R.
         * @return a new stack where the first value is left unchanged but the rest of the stack it mapped.
         */
        public <R extends HStack<R>> Result<T, R> applyRest(Function<U, R> f) {
            return HStack.applyRest(this, f);
        }

        /**
         * Duplicates the <b>reference</b> to the top value of the stack. If a new copy of the value is needed then {@link HStack#apply(Result, Function)} or {@link Result#apply(Function)} must be
         * called afterwards to create a copy of the value.
         * 
         * @return a stack where the top two values are the same object.
         */
        public Result<T, Result<T, U>> dup() {
            return HStack.dup(this);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (!(obj instanceof Result))
                return false;
            Result<?, ?> that = (Result<?, ?>) obj;
            if (Objects.equals(this.value, that.value))
                return this.rest.equals(that.rest);
            return false;
        }

        @Override
        public int hashCode() {
            return rest.hashCode() * 31 + (value == null ? 0 : value.hashCode());
        }

        /**
         * Access to the top value in the stack without affecting the structure of the stack.
         * 
         * @return the top value of the stack.
         */
        public T peek() {
            return HStack.peek(this);
        }

        /**
         * Discards the top value of the stack.
         * 
         * @return the rest of the stack.
         */
        public U pop() {
            return HStack.pop(this);
        }

        /**
         * Push a new value onto the stack.
         * 
         * @param value
         *            to be added to top of the stack
         */
        public <S> Result<S, Result<T, U>> push(S value) {
            return HStack.push(this, value);
        }

        @Override
        public <V, R> R foldL(R seed, Function<? super Object, V> map, BiFunction<R, V, R> fold) {
            return rest.foldL(fold.apply(seed, map.apply(value)), map, fold);
        }

        public <V> V foldL(Function<? super Object, V> map, BiFunction<V, V, V> fold) {
            return rest.foldL(map.apply(value), map, fold);
        }

        @Override
        public <V, R> R foldR(R seed, Function<? super Object, V> map, BiFunction<R, V, R> fold) {
            return fold.apply(rest.foldR(seed, map, fold), map.apply(value));
        }
    }

    /** singleton for the bottom stack marker */
    private static final Bottom BOTTOM = new Bottom();

    /**
     * Apply a mapping function to the top value of the stack.
     * 
     * @param stack
     *            a stack with of type T on top.
     * @param f
     *            a function that maps a of type T to a of type R.
     * @return a stack with a of type R on top.
     */
    public static <R, T, U extends HStack<U>> Result<R, U> apply(Result<T, U> stack, Function<T, R> f) {
        return new Result<R, U>(f.apply(stack.value), stack.rest);
    }

    /**
     * Applies a function mapping the rest of the stack.
     * 
     * @param stack
     *            the stack where the first element is carried over.
     * @param f
     *            the function that maps a stack of type U to a new stack of type R.
     * @return a new stack where the first value is left unchanged but the rest of the stack it mapped.
     */
    public static <T, U extends HStack<U>, R extends HStack<R>> Result<T, R> applyRest(Result<T, U> stack, Function<U, R> f) {
        return new Result<T, R>(stack.value, f.apply(stack.rest));
    }

    /**
     * Start an empty stack. The only meaningful operation that can be applied is {@link HStack#push(HStack, Object)} or {@link Bottom#push(Object)}.
     * 
     * @return an empty stack that be used to build on.
     */
    public static Bottom create() {
        return BOTTOM;
    }

    /**
     * Duplicates the <b>reference</b> to the top value of the stack. If a new copy of the value is needed then {@link HStack#apply(Result, Function)} or {@link Result#apply(Function)} must be called
     * afterwards to create a copy of the value.
     * 
     * @param stack
     * @return a stack where the top two values are the same object.
     */
    public static <T, U extends HStack<U>> Result<T, Result<T, U>> dup(Result<T, U> stack) {
        return new Result<>(stack.value, new Result<>(stack.value, stack.rest));
    }

    /**
     * Applies a function to fold the top value into the second value of the stack. Common operations are:
     * <ul>
     * <li>Concatenation of {@link String}s
     * <p>
     * {@code apply(String::concat, stack)}
     * <li>Addition of numbers
     * <p>
     * {@code apply(Math::addExact, stack)}
     * </ul>
     * 
     * @param stack
     *            the stack to apply the function to.
     * @param f
     *            a function that folds the top two values into a new value.
     * 
     * @return a new stack with the top two values replaced with the result of the function.
     */
    public static <R, T, U, V extends HStack<V>> Result<R, V> fold(Result<T, Result<U, V>> stack, BiFunction<T, U, R> f) {
        return new Result<R, V>(f.apply(stack.value, stack.rest.value), stack.rest.rest);
    }

    public abstract <V, R> R foldL(R seed, Function<? super Object, V> map, BiFunction<R, V, R> fold);

    public abstract <V, R> R foldR(R seed, Function<? super Object, V> map, BiFunction<R, V, R> fold);

    /**
     * Access to the top value in the stack without affecting the structure of the stack.
     * 
     * @param stack
     *            a stack with at least one value
     * @return the top value of the stack.
     */
    public static <T> T peek(Result<T, ?> stack) {
        return stack.value;
    }

    /**
     * Discards the top value of the stack.
     * 
     * @param stack
     *            a stack of at least one value.
     * @return the rest of the stack.
     * 
     */
    public static <U extends HStack<U>> U pop(Result<?, U> stack) {
        return stack.rest;
    }

    /**
     * Push a new value onto the stack.
     * 
     * @param value
     *            to be added to top of the stack
     * 
     * @return a new stack with
     */
    public static <T, U extends HStack<U>> Result<T, U> push(U stack, T value) {
        return new Result<T, U>(value, stack);
    }

    /**
     * Swaps the top two values of the stack. To swap deeper values to the top of the stack combine with {@link HStack#applyRest(Result, Function)} or {@link Result#applyRest(Function)}. For example
     * to swap the:
     * <ul>
     * <li>third value to the top.
     * <p>
     * {@code swap(stack.applyOnRest(HStack::swap))}
     * <li>fourth value to the top.
     * <p>
     * {@code swap(stack.applyOnRest(rest -> rest.applyOnRest(HStack::swap)).applyOnRest(HStack::swap)))}
     * </ul>
     * 
     * @param stack
     *            the stack to swap values on
     * @return a stack with the top two values swapped.
     */
    public static <T, U, V extends HStack<V>> Result<U, Result<T, V>> swap(Result<T, Result<U, V>> stack) {
        return new Result<>(stack.rest.value, new Result<>(stack.value, stack.rest.rest));
    }

    HStack() {
    }

    /**
     * Calls {@link Object#toString()} on each component of the stack and joins them together with commas surrounded by square brackets.
     */
    @Override
    public String toString() {
        return foldL(new StringJoiner(", ", "[", "]"), String::valueOf, StringJoiner::add).toString();
    }
}

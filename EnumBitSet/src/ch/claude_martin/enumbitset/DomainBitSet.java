package ch.claude_martin.enumbitset;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import ch.claude_martin.enumbitset.annotations.CheckReturnValue;
import ch.claude_martin.enumbitset.annotations.DefaultAnnotationForParameters;
import ch.claude_martin.enumbitset.annotations.NonNull;
import ch.claude_martin.enumbitset.annotations.Nonnegative;
import ch.claude_martin.enumbitset.annotations.SuppressFBWarnings;

/** A bit set with a defined domain (universe). The domain is an ordered set of all elements that are
 * allowed in this bit set. Null is not allowed as an element of any DomainBitSet.
 * 
 * <p>
 * The methods that return a {@code DomainBitSet<T>} are expected to create a new object and not
 * change the state of this object. The implementation could be mutable or immutable. Mutable
 * implementations should not implement {@code Set<T>}, because the specifications of
 * {@link #equals(Object)} are different.
 * 
 * <p>
 * Methods such as {@link #union(Iterable)}, {@link #toSet()}, and {@link #complement()} return a
 * new and independent set. This allows a functional style of programming.
 * 
 * However, this set could be mutable. This allows the classic imperative style of programming.
 * 
 * <p>
 * Note that the implementation is not necessarily a bit set. It could be any data structure that
 * allows to add and remove elements of the domain. Therefore, all elements of the domain should
 * implement {@link #hashCode()} and {@link #equals(Object)} for correct behavior and better
 * performance. The {@link #iterator() iterator} can return the elements in any order.
 * 
 * @param <T>
 *          A type that all elements in the domain share.
 * 
 * @author <a href="http://claude-martin.ch/enumbitset/">Copyright &copy; 2014 Claude Martin</a> */
@DefaultAnnotationForParameters({ NonNull.class })
public interface DomainBitSet<T> extends Iterable<T>, Cloneable, Serializable {

  /** Creates a set with the given domain, that contains all elements.
   * 
   * @param <T>
   *          The type of the elements.
   * @param elements
   *          The elements of the domain and the set.
   * @return A new DomainBitSet containing all given elements. */
  public static <T> DomainBitSet<T> allOf(final List<T> elements) {
    if (elements.size() > 64)
      return GeneralDomainBitSet.allOf(elements);
    else
      return SmallDomainBitSet.allOf(elements);
  }

  /** Creates a set with the given domain, that contains all elements.
   * 
   * @param <T>
   *          The type of the elements.
   * @param elements
   *          The elements of the domain and the set.
   * @return A new DomainBitSet containing all given elements. */
  @SafeVarargs
  public static <T> DomainBitSet<T> allOf(final T... elements) {
    if (elements.length > 64)
      return GeneralDomainBitSet.allOf(elements);
    else
      return SmallDomainBitSet.allOf(elements);
  }

  /** Creates a general bit set with a domain that consists of all elements of all given enum types.
   * Note that all bit masks become invalid when any of the types are altered. The set is empty
   * after creation.
   * 
   * @param enumTypes
   *          All enum types that define the domain. The ordering is relevant.
   * @return A new DomainBitSet that can contain enums from different enum types. */
  @SafeVarargs
  @SuppressFBWarnings({ "unchecked", "rawtypes" })
  public static DomainBitSet<Enum<?>> createMultiEnumBitSet(
      final Class<? extends Enum<?>>... enumTypes) {
    final List<Enum<?>> dom = new LinkedList<>();
    for (final Class type : enumTypes)
      for (final Object e : EnumSet.allOf(type))
        dom.add((Enum) e);
    if (dom.size() > 64)
      return GeneralDomainBitSet.noneOf(dom);
    else
      return SmallDomainBitSet.noneOf(dom);
  }

  /** Creates a set with the given domain, that contains none of the elements.
   * 
   * @param <T>
   *          The type of the elements.
   * @param elements
   *          The elements of the domain.
   * @return A new DomainBitSet containing none of the given elements. */
  public static <T> DomainBitSet<T> noneOf(final List<T> elements) {
    if (elements.size() > 64)
      return GeneralDomainBitSet.noneOf(elements);
    else
      return SmallDomainBitSet.noneOf(elements);
  }

  /** Creates a set with the given domain, that contains none of the elements.
   * 
   * @param <T>
   *          The type of the elements.
   * @param elements
   *          The elements of the domain.
   * @return A new DomainBitSet containing none of the given elements. */
  @SafeVarargs
  public static <T> DomainBitSet<T> noneOf(final T... elements) {
    if (elements.length > 64)
      return GeneralDomainBitSet.noneOf(elements);
    else
      return SmallDomainBitSet.noneOf(elements);
  }

  /** Default implementation of a {@link java.lang.Cloneable cloning-method} using
   * {@link #union(BigInteger)}.
   * 
   * @return <code>this.union(BigInteger.ZERO)</code> */
  @NonNull
  @CheckReturnValue
  public default DomainBitSet<T> clone() {
    return this.union(BigInteger.ZERO);
  }

  /** Creates a new set with the same domain, initially containing all the elements of the domain
   * that are not contained in this set.
   * 
   * @return The complement of this set. */
  @NonNull
  @CheckReturnValue
  public default DomainBitSet<T> complement() {
    return allOf(getDomain()).minus(this);

  }

  /** Returns <tt>true</tt> if this set contains the specified element.
   *
   * @see Collection#contains(Object)
   * @param o
   *          element whose presence in this collection is to be tested
   * @return <tt>true</tt> if this set contains the specified element
   * @throws ClassCastException
   *           if the type of the specified element is incompatible with this collection (<a
   *           href="#optional-restrictions">optional</a>)
   * @throws NullPointerException
   *           if the specified element is null. */
  public default boolean contains(final Object o) {
    requireNonNull(o);
    for (final T e : this)
      if (o.equals(e))
        return true;
    return false;
  }

  /** Returns <tt>true</tt> if this set contains all of the elements in the specified collection.
   *
   * @see Collection#containsAll(Collection)
   * @param c
   *          collection to be checked for containment in this collection
   * @return <tt>true</tt> if this collection contains all of the elements in the specified
   *         collection
   * @throws ClassCastException
   *           if the types of one or more elements in the specified collection are incompatible
   *           with this collection
   * @throws NullPointerException
   *           if the specified collection contains one or more null elements, or if the specified
   *           collection is null.
   * @see #contains(Object) */
  public default boolean containsAll(final Collection<?> c) {
    for (final Object e : requireNonNull(c))
      if (!this.contains(e))
        return false;
    return true;
  }

  /** Returns the Cartesian Product.
   * 
   * <p>
   * The returned set has a size of <code>this.size() * set.size()</code>.
   * 
   * @see #cross(DomainBitSet, BiConsumer)
   * @param <Y>
   *          The type of the elements in the given set.
   * @param set
   *          Another set.
   * @return the Cartesian Product.
   * @see #semijoin(DomainBitSet, BiPredicate)
   * @see BitSetUtilities#cross(DomainBitSet, DomainBitSet)
   * @see BitSetUtilities#cross(DomainBitSet, DomainBitSet, Class) */
  @NonNull
  @CheckReturnValue
  public default <Y> Set<Pair<?, T, Y>> cross(final DomainBitSet<Y> set) {
    requireNonNull(set, "set");
    final HashSet<Pair<?, T, Y>> result = new HashSet<>(this.size() * set.size());
    // this.cross(set, Pair.curry(result::add)::apply);
    this.cross(set, (x, y) -> result.add(Pair.of(x, y)));
    return result;
  }

  /** Creates the Cartesian Product and applies a given function to all coordinates.
   * <p>
   * Cartesian product of A and B, denoted <code>A × B</code>, is the set whose members are all
   * possible ordered pairs <code>(a,b)</code> where a is a member of A and b is a member of B. <br>
   * The Cartesian product of <code>{1, 2}</code> and <code>{red, white}</code> is {(1, red), (1,
   * white), (2, red), (2, white)}.
   * <p>
   * The consumer will be invoked exactly <code>(this.size() &times; set.size())</code> times.
   * <p>
   * A BiFunction can be used by passing <code>::apply</code> as consumer.<br>
   * Example: <code>Pair.curry(mySet::add)::apply</code>.
   * 
   * @param <Y>
   *          The type of the elements in the given set.
   * @param set
   *          Another set.
   * @param consumer
   *          A function to consume two elements.
   * @see #cross(DomainBitSet)
   * @see BitSetUtilities#cross(DomainBitSet, DomainBitSet, Class)
   * @see #semijoin(DomainBitSet, BiPredicate) */
  public default <Y> void cross(final DomainBitSet<Y> set, final BiConsumer<T, Y> consumer) {
    requireNonNull(consumer);
    requireNonNull(set);
    if (set.isEmpty())
      return; // Nothing to do...
    this.forEach(x -> set.forEach(y -> consumer.accept(x, y)));
  }

  /** Searches an object in the domain of this set.
   * 
   * @param object
   *          The object to be searched.
   * @throws NullPointerException
   *           if the object is <tt>null</tt>.
   * @return <tt>true</tt>, iff the domain contains the given object. */
  public default boolean domainContains(final T object) {
    return this.getDomain().contains(requireNonNull(object));
  }

  /** Compares the specified object with this domain bit set for equality. Returns <tt>true</tt>, iff
   * the given object is also a {@link DomainBitSet}, the two sets have the same domain, and every
   * member of the given set is contained in this set.
   * 
   * <p>
   * Comparison of elements only: <br>
   * <code>this.ofEqualElements(other)</code><br>
   * Which is equivalent to:<br>
   * <code>this.toSet().equals(other.toSet())</code>
   * <p>
   * Comparison of domain only: <br>
   * <code>this.ofEqualDomain(other)</code>
   * 
   * @see #ofEqualElements(DomainBitSet)
   * @return True, if this also a {@link DomainBitSet}, with the same domain and elements. */
  @Override
  public boolean equals(final Object other);

  /** Returns the value of the bit with the specified index. The value is {@code true} if the bit
   * with the index {@code bitIndex} is currently set in this {@code BitSet}; otherwise, the result
   * is {@code false}.
   * <p>
   * Note that not all DomainBitSets are implemented as a bit set. In that case this method emulates
   * the behavior of an actual bit set.
   * 
   * @param bitIndex
   *          the bit index
   * @return the value of the bit with the specified index
   * @see #contains(Object)
   * @see BitSet#get(int)
   * @see BigInteger#testBit(int)
   * @throws IndexOutOfBoundsException
   *           if the specified index is negative or out of bounds. */
  public boolean getBit(@Nonnegative final int bitIndex) throws IndexOutOfBoundsException;

  /** Returns a distinct list, containing all elements of the domain. There is no guarantee that the
   * set is the same for multiple invocations.
   * <p>
   * All elements are ordered as they are defined in the domain.
   * <p>
   * Note that the returned set is immutable.
   * 
   * @return The {@link Domain} of this set. */
  @NonNull
  public Domain<T> getDomain();

  /** Returns an Optional that might contain the element at the specified position.
   * <p>
   * The inverse has to be done in the domain: <br>
   * <code>mySet.{@linkplain #getDomain()}.{@linkplain Domain#indexOf(Object) indexOf(element)};</code>
   * 
   * @param index
   *          index of an element in the domain.
   * @see #zipWithPosition()
   * @see #getBit(int)
   * @see Domain#indexOf(Object)
   * @return Optional that might contain the element at the specified position.
   * @throws IndexOutOfBoundsException
   *           if the index is out of range */
  @NonNull
  public default Optional<T> getElement(final int index) {
    final T o = this.getDomain().get(index);
    if (this.contains(o))
      return Optional.of(o);
    else
      return Optional.empty();
  }

  /** Hash code of domain and elements. The hash code of the domain must be xored with the sum of the
   * hash codes of all elements:<br>
   * {@code this.getDomain().hashCode() ^ this.stream().mapToInt(Object::hashCode).sum()} */
  @Override
  public int hashCode();

  /** The intersection of this and a given mask.
   * 
   * @param mask
   *          The mask of the other set.
   * 
   * @return Intersection of this and the given mask. */
  @NonNull
  @CheckReturnValue
  public DomainBitSet<T> intersect(@Nonnegative final BigInteger mask);

  /** The intersection of this and a given bit set.
   * 
   * @param set
   *          The bit set representation of the other set.
   * 
   * @return Intersection of this and the given bit set. */
  @NonNull
  @CheckReturnValue
  public DomainBitSet<T> intersect(final BitSet set);

  /** Intersection of this and the given set.
   * 
   * @param set
   *          An {@link Iterable} collection of elements from the domain.
   * @throws IllegalArgumentException
   *           If any of the elements are not in the domain.
   * @return Intersection of this and the given collection. */
  @NonNull
  @CheckReturnValue
  public DomainBitSet<T> intersect(final Iterable<T> set) throws IllegalArgumentException;

  /** Intersection of this and the given set.
   * 
   * @param mask
   *          The bit mask of another set.
   * @return A new DomainBitSet that represents the intersection.
   * @throws MoreThan64ElementsException
   *           If the domain contains more than 64 elements, then long can't be used.
   * @throws IllegalArgumentException
   *           If the domain contains less than 64 elements then some long values are illegal. */
  @NonNull
  @CheckReturnValue
  public DomainBitSet<T> intersect(final long mask) throws MoreThan64ElementsException;

  /** The intersection of this set and a set represented by an array (varargs). The name is different
   * so that it is unambiguous.
   * 
   * @see #intersect(Iterable)
   * @param set
   *          A set as an array. Duplicates are ignored. Must not be nor contain <code>null</code>.
   * @return The intersection of this and the given set. */
  @NonNull
  @CheckReturnValue
  public default DomainBitSet<T> intersectVarArgs(
      @NonNull @SuppressFBWarnings("unchecked") final T... set) {
    return this.intersect(Arrays.asList(requireNonNull(set)));
  }

  /** Returns <tt>true</tt> if this set contains no elements.
   * 
   * @return <tt>true</tt> if this set contains no elements
   * @see Collection#isEmpty() */
  public default boolean isEmpty() {
    return this.size() == 0;
  }

  /** Returns an iterator over elements of type T.
   * 
   * <p>
   * The order is not defined as this could be backed by a set. Iteration in the same order as the
   * domain can be done like this: <br>
   * <code>domainBitSet.getDomain().stream().filter(domainBitSet::contains).forEach(...)</code> */
  @Override
  @NonNull
  public Iterator<T> iterator();

  /** Returns a new set with elements of a given domain, containing all mapped elements. Mapping is
   * done by index in the domain. Therefore the new domains must not be smaller than the domain of
   * this set.
   * 
   * <p>
   * If the given domain is the same as the domain of this set then the returned value is equal to
   * <code>this.clone()</code>.
   * 
   * @param domain
   *          The new domain
   * @param <S>
   *          Type of given domain. It has to be the same size or larger than the domain of this
   *          set.
   * @throws IllegalArgumentException
   *           if the given domain contains less elements.
   * @see #zipWithPosition()
   * @see #map(Domain, Function)
   * @return new set, using the given domain. */
  @SuppressFBWarnings("unchecked")
  @NonNull
  @CheckReturnValue
  public default <S> DomainBitSet<S> map(final Domain<S> domain) {
    requireNonNull(domain, "domain");
    if (domain == this.getDomain())
      return (DomainBitSet<S>) this.clone();
    if (domain.size() < this.getDomain().size())
      throw new IllegalArgumentException("The given domain is too small.");
    return this.map(domain, (t) -> domain.get(this.getDomain().indexOf(t)));
  }

  /** Returns a new set with elements of a given domain, containing all mapped elements.
   * <p>
   * This is a convenience method. The same can be done with: <code>this.stream().map(mapper)</code>
   * 
   * @param domain
   *          The new domain
   * @param mapper
   *          function to map from T to S.
   * @param <S>
   *          Type of given domain.
   * 
   * @see Stream#map(Function)
   * @throws IllegalArgumentException
   *           if the mapper returns illegal elements.
   * @see #zipWithPosition()
   * @see #map(Domain)
   * @return new set, using the given domain. */
  @NonNull
  @CheckReturnValue
  public default <S> DomainBitSet<S> map(final Domain<S> domain, final Function<T, S> mapper) {
    requireNonNull(domain, "domain");
    requireNonNull(mapper, "mapper");
    return this.stream().map(mapper).collect(BitSetUtilities.toDomainBitSet(domain.factory()));
  }

  /** The relative complement of this set and a set represented by a {@link BigInteger}.
   * 
   * @param mask
   *          The other set as a bit mask.
   * @throws IllegalArgumentException
   *           If the parameter is negative.
   * @return The relative complement of this and the given set. */
  @NonNull
  @CheckReturnValue
  public DomainBitSet<T> minus(@Nonnegative final BigInteger mask);

  /** The relative complement of this set and a set represented by a {@link BitSet}.
   * 
   * @param set
   *          The other set.
   * @return The relative complement of this and the given set. */
  @NonNull
  @CheckReturnValue
  public DomainBitSet<T> minus(final BitSet set);

  /** The relative complement of this set and another set.
   * 
   * @param set
   *          The other set.
   * @return The relative complement of this and the given set. */
  @NonNull
  @CheckReturnValue
  public DomainBitSet<T> minus(final Iterable<T> set);

  /** The relative complement of this set and a set represented by a bit mask.
   * 
   * @param mask
   *          The mask representing the other set.
   * @throws MoreThan64ElementsException
   *           If the domain contains more than 64 elements, then long can't be used.
   * @throws IllegalArgumentException
   *           If the domain contains less than 64 elements then some long values are illegal.
   * @return The relative complement of this and the given set. */
  @NonNull
  @CheckReturnValue
  public DomainBitSet<T> minus(final long mask) throws MoreThan64ElementsException;

  /** The relative complement of this set and a set represented by an array (varargs). The name is
   * different so that it is unambiguous.
   * 
   * @see #minus(Iterable)
   * @param set
   *          A set as an array. Duplicates are ignored. Must not be nor contain <code>null</code>.
   * @return The relative complement of this and the given set. */
  @NonNull
  @CheckReturnValue
  public default DomainBitSet<T> minusVarArgs(
      @NonNull @SuppressFBWarnings("unchecked") final T... set) {
    return this.minus(Arrays.asList(requireNonNull(set)));
  }

  /** Compares the domains.
   * <p>
   * This is equal to, but could be a bit faster than
   * <code>this.getDomain().equals(set.getDomain())</code>.
   * 
   * @param set
   *          The other set.
   * @return <code>true</code> if both are of equal domains. */
  public default boolean ofEqualDomain(final DomainBitSet<T> set) {
    return this.getDomain().equals(set.getDomain());
  }

  /** Compares the elements, ignoring the domains.
   * <p>
   * This is equal to, but could be a bit faster than <code>this.toSet().equals(set.toSet())</code>.
   * 
   * @param set
   *          The other set.
   * @see Set#equals(Object)
   * @return <code>true</code> if both contain the same elements. */
  public default boolean ofEqualElements(final DomainBitSet<T> set) {
    return this.toSet().equals(set.toSet());
  }

  /** Returns a possibly parallel {@code Stream} with this set as its source. It is allowable for
   * this method to return a sequential stream.
   *
   * @see Collection#parallelStream()
   * @return a possibly parallel {@code Stream} over the elements in this set */
  @NonNull
  public default Stream<T> parallelStream() {
    return StreamSupport.stream(spliterator(), true);
  }

  /** The powerset, which is the set of all subsets.
   * <p>
   * Note: Complexity is <code>O(2<sup>n</sup>)</code>. For sets with more than 64 elements this
   * would be insanely large. Therefore this is limited to sets with up to 64 elements. However, the
   * size of the domain does not matter.
   * <p>
   * This is not thread safe and has to be processed sequentially.
   * 
   * @throws MoreThan64ElementsException
   *           if this set contains more than 64 elements. This would result in more than 18E18
   *           subsets.
   * @return The powerset of this set.
   * 
   * @see #powerset(Consumer, boolean) */
  // This always returns sets of the exact same type as this set.
  @NonNull
  public default Iterable<? extends DomainBitSet<T>> powerset() throws MoreThan64ElementsException {
    final DomainBitSet<T> empty = DomainBitSet.this.getDomain().factory()
        .apply(Collections.emptySet());
    if (this.isEmpty())
      return new HashSet<>(asList(empty));

    if (DomainBitSet.this.size() > 64)
      throw new MoreThan64ElementsException();
    @SuppressFBWarnings("unchecked")
    final T[] array = (T[]) this.toSet().toArray(new Object[this.size()]);

    return new Iterable<DomainBitSet<T>>() {
      @Override
      public Iterator<DomainBitSet<T>> iterator() {
        return new Iterator<DomainBitSet<T>>() {
          /** Size of the returned Iterator. Max value: 2<sup>64</sup> ~ 1.8E19 */
          BigInteger         size    = BigInteger.ONE.shiftLeft(DomainBitSet.this.size());
          // long size = 1L << DomainBitSet.this.size();
          /** Current state. The domain can hold more than 64 elements, therefore we need a
           * BigInteger. */
          BigInteger         i       = BigInteger.ZERO;
          // long i = 0L;
          /** List for items of next result. Used instead of a Set. */
          final ArrayList<T> tmp     = new ArrayList<>();
          /** Size of the domain. */
          final int          domSize = DomainBitSet.this.getDomain().size();

          @Override
          public boolean hasNext() {
            return this.i.compareTo(this.size) < 0;
            // return this.i < this.size;
          }

          @Override
          public DomainBitSet<T> next() {
            if (!hasNext())
              throw new NoSuchElementException();

            // All bits set to 1 in i indicate that array[x] is in the result.
            this.tmp.clear();

            for (int x = 0; x < this.domSize; x++)
              if (this.i.testBit(x))
                // if (((this.i & (1L << x)) != 0))
                this.tmp.add(array[x]);
            final DomainBitSet<T> result = empty.union(this.tmp);
            this.i = this.i.add(BigInteger.ONE);
            // this.i++;
            return result;
          }
        };
      }
    };
  }

  /** Pass all subsets to a given consumer.
   * <p>
   * The consumer must be thread-safe. This will process all possible subsets concurrently, using an
   * {@link ExecutorService}. For better performance this uses SmallDomainBitSet only.
   * 
   * @see #powerset()
   * 
   * @param consumer
   *          to process each subset
   * @param blocking
   *          if true this will block until all are processed
   * 
   * @throws MoreThan64ElementsException
   *           if this set contains more than 64 elements. This would result in more than 18E18
   *           subsets. */
  public default void powerset(final Consumer<DomainBitSet<T>> consumer, final boolean blocking)
      throws MoreThan64ElementsException {
    requireNonNull(consumer, "consumer");
    if (DomainBitSet.this.size() > 64)
      throw new MoreThan64ElementsException();
    final ExecutorService pool = Executors.newWorkStealingPool();
    final Domain<T> domain = getDomain();
    final Pair<?, Integer, T>[] pairs = this.zipWithPosition().toArray(Pair[]::new);
    final int domSize = DomainBitSet.this.getDomain().size();
    final BigInteger size = BigInteger.ONE.shiftLeft(DomainBitSet.this.size());
    BigInteger i = BigInteger.ZERO;
    while (i.compareTo(size) < 0) {
      final BigInteger _i = i;
      pool.execute(() -> {
        long mask = 0L;
        for (int x = 0; x < domSize; x++)
          if (_i.testBit(x))
            mask |= 1L << pairs[x]._1();
        consumer.accept(SmallDomainBitSet.of(domain, mask));
      });
      i = i.add(BigInteger.ONE);
    }
    pool.shutdown();
    if (blocking)
      try {
        pool.awaitTermination(1, TimeUnit.DAYS);
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
      }
  }

  /** Returns a new set with all elements in this set, that have a matching element in the other set.
   * <p>
   * This is basically the same as {@link #cross(DomainBitSet)}, but filtered by a predicate. All
   * <code>this.size() &times; set.size()</code> combinations are tested! The term "semijoin" is
   * used in relational algebra, where the predicate compares the primary attributes of two tuples
   * (natural join).
   * 
   * @param set
   *          The other set
   * @param predicate
   *          Predicate to match the tuples
   * @return <code>this ⋉ set  = { t | t ∈ <i>this</i>, s ∈ <i>set</i> : <i>predicate</i>(t, s) }</code>
   *         .
   * @see #cross(DomainBitSet)
   * @see #cross(DomainBitSet, BiConsumer)
   * @see #map(Domain)
   * @see #map(Domain, Function) */
  @NonNull
  @CheckReturnValue
  public default <S> DomainBitSet<T> semijoin(final DomainBitSet<S> set,
      final BiPredicate<T, S> predicate) {
    final List<T> result = new ArrayList<>();
    this.cross(set, (a, b) -> {
      if (predicate.test(a, b))
        result.add(a);
    });
    return this.intersect(result);
  }

  /** The number of elements in this set.
   * 
   * @see Collection#size()
   * @return The number of elements in this set. */
  public int size();

  /** Creates a {@link Spliterator} over the elements in this collection.
   * 
   * @see Collection#spliterator()
   * @return a {@code Spliterator} over the elements in this collection */
  @Override
  @NonNull
  default Spliterator<T> spliterator() {
    return Spliterators.spliterator(iterator(), size(), Spliterator.SIZED | Spliterator.DISTINCT
        | Spliterator.NONNULL);
  }

  /** Returns a sequential {@code Stream} with this collection as its source.
   *
   * @see Collection#stream()
   * @return a sequential {@code Stream} over the elements in this collection */
  @NonNull
  default Stream<T> stream() {
    return StreamSupport.stream(spliterator(), false);
  }

  /** Elemens in an array. The elements are ordered as they appear in the domain. */
  @NonNull
  public default Object[] toArray() {
    return getDomain().stream().filter(this::contains).toArray();
  }

  /** A representation of the elements in this set as a {@link BigInteger}.
   * 
   * @return The bit mask as a {@link BigInteger}. */
  @NonNull
  @CheckReturnValue
  @Nonnegative
  public BigInteger toBigInteger();

  /** Binary string representation of this set.
   * <p>
   * The length of the returned string is the same as the amount of enum elements in the enum type.
   * 
   * @return A representation of this bit set as a String of 0s and 1s. */
  @NonNull
  public default String toBinaryString() {
    return this.toBinaryString(this.getDomain().size());
  }

  /** Binary string representation of this set.
   * <p>
   * The length of the returned string is as least as long as <i>width</i>.
   * 
   * @param width
   *          The minimal width of the returned String.
   * @return A representation of this bit set as a String of 0s and 1s. The length is at least
   *         <i>width</i>. */
  @NonNull
  public default String toBinaryString(final int width) {
    final String binary = this.toBigInteger().toString(2);
    final StringBuilder sb = new StringBuilder(width < 8 ? 8 : width);
    while (sb.length() < width - binary.length())
      sb.append('0');
    sb.append(binary);
    return sb.toString();
  }

  /** A representation of the elements in this set as a {@link BitSet}.
   * 
   * @return The set as a {@link BitSet}. */
  @NonNull
  @CheckReturnValue
  public BitSet toBitSet();

  /** A representation of the elements in this set as a {@link Long long}.
   * 
   * @throws MoreThan64ElementsException
   *           If the domain contains more than 64 elements.
   * @return The set as a {@link Long long}. */
  @CheckReturnValue
  public long toLong() throws MoreThan64ElementsException;

  /** A regular set, with no defined domain. Note that the returned set can be compared to other
   * regular sets, which this always returns <code>false</code> for other sets without a domain.
   * 
   * @return A set, without the domain. */
  @NonNull
  @CheckReturnValue
  public Set<T> toSet();

  /** The union of this set and a set represented by a {@link BitSet}.
   * <p>
   * Note: A fast version for BigInteger.ZERO should exist for each implementation!
   * 
   * @param mask
   *          A BitSet representing another set.
   * @return The union of this set and the given set. */
  @NonNull
  @CheckReturnValue
  public DomainBitSet<T> union(@Nonnegative final BigInteger mask);

  /** The union of this set and a set represented by a {@link BitSet}.
   * 
   * @param set
   *          A BitSet representing another set.
   *
   * @return The union of this set and the given set. */
  @NonNull
  @CheckReturnValue
  public DomainBitSet<T> union(final BitSet set);

  /** The union of this set and a set represented by an {@link Iterable iterable} collection.
   * 
   * @param set
   *          An Iterable representing another set.
   *
   * @return The union of this set and the given set. */
  @NonNull
  @CheckReturnValue
  public DomainBitSet<T> union(final Iterable<T> set);

  /** The union of this set and a set represented by a bit mask.
   * 
   * @param mask
   *          A bit mask representing another set.
   *
   * @throws MoreThan64ElementsException
   *           If the domain contains more than 64 elements, then long can't be used.
   * @throws IllegalArgumentException
   *           If the domain contains less than 64 elements then some long values are illegal.
   * @return The union of this set and the given set. */
  @NonNull
  @CheckReturnValue
  public DomainBitSet<T> union(final long mask) throws MoreThan64ElementsException;

  /** The union of this set and a set represented by an array (varargs). The name is different so
   * that it is unambiguous.
   * 
   * @see #union(Iterable)
   * @param set
   *          A set as an array. Duplicates are ignored. Must not be nor contain <code>null</code>.
   * @return The union of this and the given set. */
  @NonNull
  @CheckReturnValue
  public default DomainBitSet<T> unionVarArgs(
      @NonNull @SuppressFBWarnings("unchecked") final T... set) {
    return this.union(Arrays.asList(requireNonNull(set)));
  }

  /** Returns a sequential stream with pairs of all elements of this set and their position in the
   * domain.
   * <p>
   * This can be collected to a {@link Map} using {@link BitSetUtilities#toTreeMap() toTreeMap}.
   * 
   * @see BitSetUtilities#toTreeMap()
   * @see #getElement(int)
   * @return A stream of elements and their position. */
  @NonNull
  @CheckReturnValue
  public default Stream<Pair<Object, Integer, T>> zipWithPosition() {
    final Domain<T> domain = this.getDomain();
    return domain.stream().filter(this::contains).map(e -> Pair.of(domain.indexOf(e), e));
  }
}

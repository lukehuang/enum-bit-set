package ch.claude_martin.enumbitset;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.BiFunction;

import org.junit.Test;

import ch.claude_martin.enumbitset.EnumBitSetTest.Alphabet;
import ch.claude_martin.enumbitset.EnumBitSetTest.Planet;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressWarnings("static-method")
@SuppressFBWarnings(value = { "DM_NUMBER_CTOR", "DM_STRING_CTOR" }, justification = "It's part of the test.")
public class PairTest {

  @Test
  public final void testClone() {
    final Pair<?, String, Integer> p = Pair.of("foo", 42);
    assertTrue(p == p.clone());
  }

  @Test
  public final void testCurry() {
    { // Basic test of curry and uncurry.
      final Pair<Serializable, String, Integer> p = Pair.of("foo", 42);
      final Integer snd1 = p.applyTo(Pair.curry(x -> x.second));
      final Integer snd2 = Pair.uncurry((String a, Integer b) -> b).apply(p);
      final Integer snd3 = p.applyTo(Pair.curry(Pair::_2));
      assertEquals(snd1, snd2);
      assertEquals(snd1, snd3);
    }
    {
      final Pair<Serializable, String, Integer> p = Pair.of("bla", 1);
      final Set<Pair<Serializable, String, Integer>> set = new HashSet<>();
      final BiFunction<String, Integer, Boolean> add = Pair.curry(set::add);
      p.applyTo(add);
      assertEquals(new HashSet<>(asList(p)), set);

      final BiFunction<String, Integer, Boolean> contains = Pair.curry(set::contains);
      // Three ways to see if 'set' contains 'p':
      assertTrue(contains.apply(p.first, p.second));
      assertTrue(p.applyTo(contains));
      assertTrue(Pair.uncurry(contains).apply(p));
    }
    { // With more complex types:
      Domain<? extends Number> domain = DefaultDomain.of(asList(1,2,3));
      List<? super Integer> list = new ArrayList<>(asList(1,2,3));
      Pair<Object, Domain<? extends Number>, List<? super Integer>> p = Pair.of(domain, list);
      
      final Set<Pair<Object, Domain<? extends Number>, List<? super Integer>>> set = new HashSet<>();
      final BiFunction<Domain<? extends Number>, List<? super Integer>, Boolean> add = Pair.curry(set::add);
      p.applyTo(add);
      assertEquals(new HashSet<>(asList(p)), set);
    }
  }

  @Test
  public final void testEqualsObject() {
    final Pair<?, String, Integer> p1 = Pair.of("foo", 42);
    final Pair<?, String, Integer> p2 = Pair.of(new String("foo"), new Integer(42));
    assertEquals(p1, p1);
    assertEquals(p1, p2);
    assertEquals(p2, p1);
    assertEquals(p2, p2);
    assertFalse(p1.equals(null));
    assertFalse(p1.equals("X"));
  }

  @Test
  public final void testHashCode() {
    final Pair<?, String, Integer> p1 = Pair.of("foo", 42);
    final Pair<?, String, Integer> p2 = Pair.of(new String("foo"), new Integer(42));
    assertEquals(p1.hashCode(), p1.hashCode());
    assertEquals(p1.hashCode(), p2.hashCode());
    assertEquals(p2.hashCode(), p2.hashCode());
  }

  @Test
  public void testIterator() throws Exception {
    final Pair<Number, Double, Integer> p = Pair.of(Math.PI, 42);
    final List<?> list = Arrays.asList(p.toArray());
    for (final Number num : p)
      assertTrue(list.contains(num));

    try {
      final Iterator<Number> itr = p.iterator();
      for (int i = 0; i < 19; i++)
        itr.next();
      fail("expected: NoSuchElementException ");
    } catch (final NoSuchElementException e) {
    }

    final Object[] array = p.stream().distinct().toArray();
    assertArrayEquals(p.toArray(), array);
  }

  @Test
  public void testOf() throws Exception {
    {
      final Pair<Number, Integer, Double> p1 = Pair.of(1, 0.5);
      final Pair<Number, Integer, Double> p2 = Pair.of(Number.class, 1, 0.5);
      assertEquals(p1, p2);
      final Pair<?, ?, ?> p3 = Pair.of(Pair.of(1, 2), Pair.of(3, 4));
      final Pair<?, ?, ?> p4 = Pair.of(Pair.class, Pair.of(Integer.class, 1, 2),
          Pair.of(Integer.class, 3, 4));
      assertEquals(p3, p4);
    }
  }

  @Test
  public final void testPair() {
    Pair.of("foo", "bar");

    final Pair<Enum<?>, Alphabet, Planet> p2 = Pair.of(EnumBitSetTest.Alphabet.A,
        EnumBitSetTest.Planet.EARTH);
    for (final Enum<?> e : p2)
      assertNotNull(e);

    try {
      Pair.of("foo", null);
      fail("null");
    } catch (final NullPointerException e) {

    }
    try {
      Pair.of(null, "foo");
      fail("null");
    } catch (final NullPointerException e) {

    }
  }

  @Test
  public final void testSwap() {
    final Pair<?, String, Integer> p = Pair.of("foo", 42);
    assertEquals(p, p.swap().swap());
    assertEquals(p.swap(), p.swap());
    final Pair<?, Integer, String> swap = p.swap();
    assertEquals(p.first, swap.second);
    assertEquals(p.second, swap.first);
  }

  @Test
  public final void testToArray() {
    final Pair<?, String, Integer> p = Pair.of("foo", 42);
    final Object[] array = p.toArray();
    assertEquals(2, array.length);
    assertEquals(p.first, array[0]);
    assertEquals(p.second, array[1]);
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Test
  public final void testToString() {
    final Pair<?, String, Integer> p = Pair.of("foo", 42);
    final String string = p.toString();
    assertTrue(string.startsWith(Pair.class.getSimpleName()));
    assertTrue(string.contains(p.first));
    assertTrue(string.contains(p.second.toString()));

    // Recursion:
    final Pair<Object, ArrayList, ArrayList<Object>> p2 = Pair.of(new ArrayList(),
        new ArrayList<>());
    p2.first.add(p2);
    p2.second.add(p2);
    try {
      p2.toString();
    } catch (final Throwable e) {
      fail("Pair.toString failed: " + e);
    }
  }

  @Test
  public final void testComparing() throws Exception {
    List<Pair<Number, Integer, Double>> list = new ArrayList<>();
    Pair<Number, Integer, Double> a = Pair.of(42, Math.PI);
    list.add(a);
    Pair<Number, Integer, Double> b = Pair.of(-7, Math.E);
    list.add(b);
    Pair<Number, Integer, Double> c = Pair.of(0, Math.sqrt(2));
    list.add(c);
    
    list.sort(Pair.comparingByFirst());
    assertArrayEquals(new Object[] { b, c, a }, list.toArray());
    list.sort(Pair.comparingBySecond());
    assertArrayEquals(new Object[] { c, b, a }, list.toArray());
    list.sort(Pair.comparingByFirst((x, y) -> Integer.compare(y, x)));
    assertArrayEquals(new Object[] { a, c, b }, list.toArray());
    list.sort(Pair.comparingBySecond((x, y) -> Double.compare(y, x)));
    assertArrayEquals(new Object[] { a, b, c }, list.toArray());
  }
}
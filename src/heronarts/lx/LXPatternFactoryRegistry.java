package heronarts.lx;

import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * A registry that maps LXPattern implementation classes to {@link LXPattern.Factory} instances which are used to
 * instantiate the patterns when deserializing from LX project json.
 *
 * Factories are only required if your pattern constructor does not follow the <pre>public MyPattern(LX lx)</pre> form.
 */
public class LXPatternFactoryRegistry {

  private final Map<Class<? extends LXPattern>, LXPattern.Factory<? extends LXPattern>> factoriesByClass =
      new HashMap<>();

  private final Deque<Class<? extends LXPattern>> patternClasses = new LinkedList<>();

  protected LXPatternFactoryRegistry() {

  }

  public <T extends LXPattern> void register(Class<T> patternClass, LXPattern.Factory<T> patternFactory) {
    if (!this.factoriesByClass.containsKey(patternClass)) {
      this.patternClasses.addFirst(patternClass);
    }
    this.factoriesByClass.put(patternClass, patternFactory);
  }

  /**
   * Gets a factory that can create a <pre>patternClazz</pre> instance, if one is recognized.
   *
   * If no direct mappings are known, returns the first factory that can create an assignable subclass of the pattern.
   */
  @SuppressWarnings("unchecked")
  public <T extends LXPattern> LXPattern.Factory<T> getPatternFactory(Class<T> patternClazz) {
    // First try by direct mapping
    LXPattern.Factory factory = this.factoriesByClass.get(patternClazz);

    // If that doesn't work, fall back to instance checks to cover subclasses
    if (factory == null) {
      for (Class<? extends LXPattern> clazz : this.patternClasses) {
        if (patternClazz.isAssignableFrom(clazz)) {
          factory = factoriesByClass.get(clazz);
          break;
        }
      }
    }

    return factory;
  }
}

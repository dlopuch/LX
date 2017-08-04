package heronarts.lx;

import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * A registry that maps some LXComponent implementation classes to {@link Factory} instances which are used to
 * instantiate the components when deserializing from LX project json.
 *
 * Factories are only required if your component constructor does not follow the <pre>public MyComponent(LX lx)</pre>
 * form.
 *
 * @param <C> The LXComponent context of this registry
 */
public class LXComponentFactoryRegistry<C extends LXComponent> {

  /**
   * Factories recognized by this registry
   * @param <C> The LXComponent context of this registry
   */
  public interface Factory<C extends LXComponent> {
    C build(LX lx);
  }

  private final Map<Class<? extends C>, Factory<? extends C>> factoriesByClass =
      new HashMap<>();

  private final Deque<Class<? extends C>> componentClasses = new LinkedList<>();

  protected LXComponentFactoryRegistry() {

  }

  public <T extends C> void register(Class<T> componentClass, Factory<T> componentFactory) {
    if (!this.factoriesByClass.containsKey(componentClass)) {
      this.componentClasses.addFirst(componentClass);
    }
    this.factoriesByClass.put(componentClass, componentFactory);
  }

  /**
   * Gets a factory that can create a <pre>componentClazz</pre> instance, if one is recognized.
   *
   * If no direct mappings are known, returns the first factory that can create an assignable subclass of the component.
   */
  @SuppressWarnings("unchecked")
  public <T extends C> Factory<T> getFactory(Class<T> componentClazz) {
    // First try by direct mapping
    Factory factory = this.factoriesByClass.get(componentClazz);

    // If that doesn't work, fall back to instance checks to cover subclasses
    if (factory == null) {
      for (Class<? extends C> clazz : this.componentClasses) {
        if (componentClazz.isAssignableFrom(clazz)) {
          factory = factoriesByClass.get(clazz);
          break;
        }
      }
    }

    return factory;
  }
}

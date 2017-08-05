package heronarts.lx;

import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * A registry that maps some LXComponent implementations' Classes to Factory instances which are used to
 * instantiate the components when deserializing from LX project json.
 *
 * Factories are only required if your component constructor does not use a simple parameter constructor,
 * usually of the form <pre>public MyComponent(LX lx)</pre>
 *
 * @param <C> The LXComponent context of this registry
 * @param <F> The type of Factory used by this context of registry
 */
public class LXComponentFactoryRegistry<C extends LXComponent, F extends LXComponentFactoryRegistry.BaseFactory<C>> {

  /**
   * Usages of this registry class should specify the type of factory used by the usage context, since different
   * contexts may send different parameters to the factory.
   *
   * Context factories should specify a single build() function
   *
   * @param <C> The LXComponent context of this registry
   */
  public interface BaseFactory<C extends LXComponent> {
    // extensions should specify a single build() method that returns the type of LXComponent being created
  }

  private final Map<Class<? extends C>, F> factoriesByClass =
      new HashMap<>();

  private final Deque<Class<? extends C>> componentClasses = new LinkedList<>();

  protected LXComponentFactoryRegistry() {

  }

  public <T extends C> void register(Class<T> componentClass, F componentFactory) {
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
  public <T extends C> F getFactory(Class<T> componentClazz) {
    // First try by direct mapping
    F factory = this.factoriesByClass.get(componentClazz);

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

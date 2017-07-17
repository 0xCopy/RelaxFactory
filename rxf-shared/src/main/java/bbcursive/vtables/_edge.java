package bbcursive.vtables;

import java.util.Objects;

/**
 * context class. midpoint between 2 casts. this class is a pair, but we pretend its more. this should be refactored to
 * a pair class.
 * <p>
 * Function interface performs reification from the addressType against the core type,context, delta, coersion points,
 * etc.
 * <p>
 * <p>
 * left refers to "refreence" side, right refers to "pointer" side.
 * <p>
 * User: jim Date: Sep 18, 2008 Time: 6:05:14 AM
 */
public abstract class _edge<coreType, addressType> {
  /**
   * this is a core of memory accessed somehow by an addressType to get java Objects from this context of core that is
   * probably bytes or chars
   */
  private coreType core;

  protected abstract addressType at();

  protected abstract addressType goTo(addressType addressType);

  /**
   * left type node with induction of core only. address will be null until set
   * 
   * @param e
   * @return
   */
  public coreType core(_edge<coreType, addressType>... e) {
    boolean empty = 0 == e.length;

    boolean isMe = !empty && this == e[0];

    return !empty && !isMe ? core(bind(e[0].core(), e[0].location())) : core;

  }

  /**
   * an address
   * <p>
   * for _ptr, Integer is an address of a ByteBuffer state, linear memory here.
   * <p>
   * for {@code Map<K,V>}, K is an address to get a V from {@code _edge<V,K>}
   * <p>
   * for {@code _edge<_edge<A,B>,_ptr>}
   * 
   * @param notnullorself null for self. non-empty set for induction
   * @return typically what is returned is what is passed in most recently to any of the Pair.second mutators (this.at,
   *         this.goto, this.location).
   */
  protected final addressType at(addressType... notnullorself) {
    addressType addressType1 = notnullorself[0];
    return 0 != notnullorself.length && !Objects.equals(this, addressType1) ? goTo(addressType1)
        : r$();
  }

  /**
   * internal factory or getter for pair.second. for _ptr this is inferred from bytebuffer instance.
   * 
   * @return
   */
  protected abstract addressType r$();

  /**
   * right type node with induction
   * 
   * @param e
   * @return
   */
  public final addressType location(_edge<coreType, addressType>... e) {
    _edge<coreType, addressType> subj = this;
    boolean empty = 0 == e.length;
    boolean alien = !empty && subj != e[0];
    return empty || !alien ? at() : bind(e[0].core(), at(e[0].location())).location();
  }

  /**
   * binds two types
   * 
   * @param coreType
   * @param address
   * @return fused arc
   */
  public _edge<coreType, addressType> bind(coreType coreType, addressType address) {
    core = (coreType);
    at(address);
    return this;
  }

}
/**
 * public interface €<Ω, µ> extends _proto<Ω> { Ω Ω(€<Ω, µ> €); µ µ(€<Ω, µ> €); €<Ω, µ> €(Ω Ω, µ µ);}
 */

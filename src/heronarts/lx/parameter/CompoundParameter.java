/**
 * Copyright 2013- Mark C. Slee, Heron Arts LLC
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 * @author Mark C. Slee <mark@heronarts.com>
 */

package heronarts.lx.parameter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import heronarts.lx.LXUtils;

public class CompoundParameter extends BoundedParameter {

  private final List<LXCompoundModulation> mutableModulations = new ArrayList<LXCompoundModulation>();

  public final List<LXCompoundModulation> modulations = Collections.unmodifiableList(this.mutableModulations);

  /**
   * Labeled parameter with value of 0 and range of 0-1
   *
   * @param label Label for parameter
   */
  public CompoundParameter(String label) {
    super(label, 0);
  }

  /**
   * A bounded parameter with label and value, initial value of 0 and a range of 0-1
   *
   * @param label Label
   * @param value value
   */
  public CompoundParameter(String label, double value) {
    super(label, value, 1);
  }

  /**
   * A bounded parameter with an initial value, and range from 0 to max
   *
   * @param label Label
   * @param value value
   * @param max Maximum value
   */
  public CompoundParameter(String label, double value, double max) {
    super(label, value, 0, max);
  }

  /**
   * A bounded parameter with initial value and range from v0 to v1. Note that it is not necessary for
   * v0 to be less than v1, if it is desired for the knob's value to progress negatively.
   *
   * @param label Label
   * @param value Initial value
   * @param v0 Start of range
   * @param v1 End of range
   */
  public CompoundParameter(String label, double value, double v0, double v1) {
    super(label, value, v0, v1, null);
  }

  /**
   * Creates a CompoundParameter which limits the value of an underlying MutableParameter to a given
   * range. Changes to the CompoundParameter are forwarded to the MutableParameter, and vice versa.
   * If the MutableParameter is set to a value outside the specified bounds, this BoundedParmaeter
   * will ignore the update and the values will be inconsistent. The typical use of this mode is
   * to create a parameter suitable for limited-range UI control of a parameter, typically a
   * MutableParameter.
   *
   * @param underlying The underlying parameter
   * @param v0 Beginning of range
   * @param v1 End of range
   */
  public CompoundParameter(LXListenableParameter underlying, double v0, double v1) {
    super(underlying.getLabel(), underlying.getValue(), v0, v1, underlying);
  }

  @Override
  public CompoundParameter setDescription(String description) {
    return (CompoundParameter) super.setDescription(description);
  }

  /**
   * Adds a modulation to this parameter
   *
   * @param modulation
   * @return
   */
  public CompoundParameter addModulation(LXCompoundModulation modulation) {
    if (this.mutableModulations.contains(modulation)) {
      throw new IllegalStateException("Cannot add same modulation twice");
    }
    this.mutableModulations.add(modulation);
    bang();
    return this;
  }

  /**
   * Removes a modulation from this parameter
   *
   * @param modulation
   * @return
   */
  public CompoundParameter removeModulation(LXCompoundModulation modulation) {
    this.mutableModulations.remove(modulation);
    bang();
    return this;
  }

  public double getBaseValue() {
    return super.getValue();
  }

  public double getBaseNormalized() {
    return super.getNormalized(getBaseValue());
  }

  @Override
  public double getNormalized() {
    double normalized = super.getNormalized(getBaseValue());
    for (LXCompoundModulation modulation : this.mutableModulations) {
      if (modulation.getPolarity() == Polarity.UNIPOLAR) {
        normalized += modulation.source.getNormalized() * modulation.range.getValue();
      } else {
        normalized += 2.*(modulation.source.getNormalized()-.5) * modulation.range.getValue();
      }
    }
    return LXUtils.constrain(normalized, 0, 1);
  }

  @Override
  public double getValue() {
    if (this.mutableModulations.size() == 0) {
      return super.getValue();
    }
    return normalizedToValue(getNormalized());
  }

}

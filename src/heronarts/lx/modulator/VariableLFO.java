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

package heronarts.lx.modulator;

import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.DiscreteParameter;
import heronarts.lx.parameter.FixedParameter;
import heronarts.lx.parameter.LXParameter;

/**
 * A sawtooth LFO oscillates from one extreme value to another. When the later
 * value is hit, the oscillator rests to its initial value.
 */
public class VariableLFO extends LXRangeModulator implements LXWaveshape {

  /**
   * Parameter of {@link LXWaveshape} objects that select the wave shape used by this LFO.
   * Default options are the waveshapes predefined in {@link LXWaveshape}, but you can pass your own.
   */
  public final DiscreteParameter waveshape;

  /** Period of the waveform, in ms */
  public final CompoundParameter period;

  public final CompoundParameter skew = (CompoundParameter)
    new CompoundParameter("Skew", 0, -1, 1)
    .setDescription("Sets a skew coefficient for the waveshape")
    .setPolarity(LXParameter.Polarity.BIPOLAR);

  public final CompoundParameter shape = (CompoundParameter)
    new CompoundParameter("Shape", 0, -1, 1)
    .setDescription("Applies shaping to the waveshape")
    .setPolarity(LXParameter.Polarity.BIPOLAR);

  public final CompoundParameter exp = (CompoundParameter)
    new CompoundParameter("Exp", 0, -1, 1)
    .setDescription("Applies exponential scaling to the waveshape")
    .setPolarity(LXParameter.Polarity.BIPOLAR);

  public final CompoundParameter phase =
    new CompoundParameter("Phase", 0)
    .setDescription("Shifts the phase of the waveform");

  public VariableLFO() {
    this("LFO", null, null);
  }

  public VariableLFO(String label) {
    this(label, null, null);
  }

  public VariableLFO(String label, LXWaveshape[] waveshapes) {
    this(label, waveshapes, null);
  }

  /**
   * Constructs a VariableLFO with a custom list of waveshapes
   * @param label LFO label
   * @param waveshapes Optional list of custom {@link LXWaveshape}.  If null, will use predefined ones
   *                   in {@link LXWaveshape}
   * @param period Optional. Parameter that supplies custom waveform period, in ms.  Default goes 100-10000ms.
   */
  public VariableLFO(String label, LXWaveshape[] waveshapes, CompoundParameter period) {
    super(label, new FixedParameter(0), new FixedParameter(1), new FixedParameter(1000));

    if (waveshapes == null) {
      waveshapes = new LXWaveshape[] {
          LXWaveshape.SIN,
          LXWaveshape.TRI,
          LXWaveshape.SQUARE,
          LXWaveshape.UP,
          LXWaveshape.DOWN
      };
    }

    this.waveshape = new DiscreteParameter("Wave", waveshapes);
    this.waveshape.setDescription("Selects the wave shape used by this LFO");

    if (period == null) {
      period = (CompoundParameter) new CompoundParameter("Period", 1000, 100, 10000)
        .setDescription("Sets the period of the LFO in secs")
        .setExponent(2)
        .setUnits(LXParameter.Units.MILLISECONDS);
    }
    this.period = period;


    setPeriod(period);
    addParameter("wave", waveshape);
    addParameter("period", period);
    addParameter("skew", skew);
    addParameter("shape", shape);
    addParameter("phase", phase);
    addParameter("exp", exp);
  }

  public LXWaveshape getWaveshape() {
    return (LXWaveshape) this.waveshape.getObject();
  }

  @Override
  protected final double computeNormalizedValue(double deltaMs, double basis) {
    return compute(basis);
  }

  @Override
  protected final double computeNormalizedBasis(double basis, double normalizedValue) {
    return invert(normalizedValue, basis);
  }

  @Override
  public double compute(double basis) {
    return compute(basis, this.phase.getValue(), this.skew.getValue(), this.shape.getValue(), this.exp.getValue());
  }

  public double computeBase(double basis) {
    return compute(basis, this.phase.getBaseValue(), this.skew.getBaseValue(), this.shape.getBaseValue(), this.exp.getBaseValue());
  }

  private double compute(double basis, double phase, double skew, double shape, double exp) {
    basis = basis + phase;
    if (basis > 1.) {
      basis = basis % 1.;
    }

    double skewPower = (skew >= 0) ? (1 + 3*skew) : (1 / (1-3*skew));
    if (skewPower != 1) {
      basis = Math.pow(basis, skewPower);
    }
    double value = getWaveshape().compute(basis);
    double shapePower = (shape <= 0) ? (1 - 3*shape) : (1 / (1+3*shape));
    if (shapePower != 1) {
      if (value >= 0.5) {
        value = 0.5 + 0.5 * Math.pow(2*(value-0.5), shapePower);
      } else {
        value = 0.5 - 0.5 * Math.pow(2*(0.5 - value), shapePower);
      }
    }
    double expPower = (exp >= 0) ? (1 + 3*exp) : (1 / (1 - 3*exp));
    if (expPower != 1) {
      value = Math.pow(value, expPower);
    }
    return value;
  }

  @Override
  public double invert(double value, double basisHint) {
    // TODO(mcslee): implement shape anad bias inversion properly
    return getWaveshape().invert(value, basisHint);
  }
}
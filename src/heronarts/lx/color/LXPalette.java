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

package heronarts.lx.color;

import heronarts.lx.LX;
import heronarts.lx.LXModelComponent;
import heronarts.lx.model.LXModel;
import heronarts.lx.model.LXPoint;
import heronarts.lx.modulator.DampedParameter;
import heronarts.lx.modulator.LXModulator;
import heronarts.lx.modulator.SawLFO;
import heronarts.lx.modulator.SinLFO;
import heronarts.lx.osc.LXOscComponent;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.EnumParameter;
import heronarts.lx.parameter.FunctionalParameter;
import heronarts.lx.parameter.LXParameter;

/**
 * A palette is an object that is used to compute color values and set modes
 * of color computation. Though its use is not required, it is very useful for
 * creating coherent color schemes across patterns.
 */
public class LXPalette extends LXModelComponent implements LXOscComponent {

  public enum Mode {
    FIXED,
    OSCILLATE,
    CYCLE
  };

  public final BooleanParameter cue =
    new BooleanParameter("Cue-Palette", false)
    .setDescription("Enables cue preview of the palette");

  public final EnumParameter<Mode> hueMode =
    new EnumParameter<Mode>("Mode", Mode.FIXED)
    .setDescription("Sets the operation mode of the palette");

  public final ColorParameter color =
    new ColorParameter("Color", 0xffff0000)
    .setDescription("The base color selection for the palette");

  /**
   * Hack... the Process preprocessor doesn't let you address object.color, duplicate it to clr
   */
  public final ColorParameter clr = color;

  public final CompoundParameter range = new CompoundParameter("Range", 0, 360)
    .setDescription("Sets range in degrees (0-360) of how much spread the palette applies");

  public final CompoundParameter period = (CompoundParameter)
    new CompoundParameter("Period", 120000, 1000, 1800000)
    .setDescription("Sets how long the palette takes to complete one full oscillation")
    .setUnits(LXParameter.Units.MILLISECONDS);

  public final CompoundParameter spreadX = (CompoundParameter)
    new CompoundParameter("X-add", 0, -360, 360)
    .setDescription("Sets the amount of hue spread on the X axis")
    .setPolarity(LXParameter.Polarity.BIPOLAR);

  public final CompoundParameter spreadY = (CompoundParameter)
    new CompoundParameter("Y-add", 0, -360, 360)
    .setDescription("Sets the amount of hue spread on the Y axis")
    .setPolarity(LXParameter.Polarity.BIPOLAR);

  public final CompoundParameter spreadZ = (CompoundParameter)
    new CompoundParameter("Z-add", 0, -360, 360)
    .setDescription("Sets the amount of hue spread on the Z axis")
    .setPolarity(LXParameter.Polarity.BIPOLAR);

  public final CompoundParameter offsetX = (CompoundParameter)
    new CompoundParameter("X-off", 0, -1, 1)
    .setDescription("Sets the offset of the hue spread point on the X axis")
    .setPolarity(LXParameter.Polarity.BIPOLAR);

  public final CompoundParameter offsetY = (CompoundParameter)
    new CompoundParameter("Y-off", 0, -1, 1)
    .setDescription("Sets the offset of the hue spread point on the Y axis")
    .setPolarity(LXParameter.Polarity.BIPOLAR);

  public final CompoundParameter offsetZ = (CompoundParameter)
    new CompoundParameter("Z-off", 0, -1, 1)
    .setDescription("Sets the offset of the hue spread point on the Z axis")
    .setPolarity(LXParameter.Polarity.BIPOLAR);

  public final CompoundParameter spreadR = (CompoundParameter)
    new CompoundParameter("R-add", 0, -360, 360)
    .setDescription("Sets the amount of hue spread in the radius from center")
    .setPolarity(LXParameter.Polarity.BIPOLAR);

  public final BooleanParameter mirror =
    new BooleanParameter("Mirror", true)
    .setDescription("If engaged, the hue spread is mirrored from center");

  private final DampedParameter hueFixed = new DampedParameter(this.color.hue, 1800).setModulus(360.);

  private final SawLFO hueCycle = new SawLFO(0, 360, period);

  private final FunctionalParameter hue2 = new FunctionalParameter() {
    @Override
    public double getValue() {
      return color.hue.getValue() + range.getValue();
    }
  };

  private final SinLFO hueOscillate = new SinLFO(color.hue, hue2, period);

  private LXModulator hue = hueFixed;

  private double xMult;
  private double yMult;
  private double zMult;
  private double rMult;

  public LXPalette(LX lx) {
    super(lx);
    computeMults(lx.model);
    lx.addListener(new LX.Listener() {
      @Override
      public void modelChanged(LX lx, LXModel model) {
        computeMults(model);
      }
    });

    this.hueMode.setOptions(new String[] { "Fixed", "Oscillate", "Cycle" });
    addParameter("cue", this.cue);
    addParameter("hueMode", this.hueMode);
    addParameter("color", this.color);
    addParameter("period", this.period);
    addParameter("range", this.range);
    addParameter("spreadX", this.spreadX);
    addParameter("spreadY", this.spreadY);
    addParameter("spreadZ", this.spreadZ);
    addParameter("spreadR", this.spreadR);
    addParameter("offsetX", this.offsetX);
    addParameter("offsetY", this.offsetY);
    addParameter("offsetZ", this.offsetZ);
    addParameter("mirror", this.mirror);
    addModulator(this.hueFixed).start();
    addModulator(this.hueCycle);
    addModulator(this.hueOscillate);
  }

  public String getOscAddress() {
    return "/lx/palette";
  }

  @Override
  public String getLabel() {
    return "Palette";
  }

  private void computeMults(LXModel model) {
    this.xMult = (model.xRange == 0) ? 1 : (1 / model.xRange);
    this.yMult = (model.yRange == 0) ? 1 : (1 / model.yRange);
    this.zMult = (model.zRange == 0) ? 1 : (1 / model.zRange);
    this.rMult = (model.rRange == 0) ? 1 : (1 / model.rRange);
  }

  @Override
  public void onParameterChanged(LXParameter parameter) {
    if (parameter == this.hueMode) {
      double hueValue = this.hue.getValue();
      this.color.hue.setValue(hueValue);
      switch ((Mode) this.hueMode.getObject()) {
        case FIXED:
          this.hue = this.hueFixed;
          this.hueFixed.setValue(hueValue).start();
          this.hueCycle.stop();
          this.hueOscillate.stop();
          break;
        case CYCLE:
          this.hue = this.hueCycle;
          this.hueFixed.stop();
          this.hueOscillate.stop();
          this.hueCycle.setValue(hueValue).start();
          break;
        case OSCILLATE:
          this.hue = this.hueOscillate;
          this.hueFixed.stop();
          this.hueCycle.stop();
          this.hueOscillate.setValue(hueValue).start();
          break;
      }
    }
  }

  public double getHue() {
    return this.hue.getValue();
  }

  public final float getHuef() {
    return (float) getHue();
  }

  public double getSaturation() {
    return this.color.saturation.getValue();
  }

  public final float getSaturationf() {
    return (float) getSaturation();
  }

  public double getHue(LXPoint point) {
    double dx = point.x - this.model.cx - this.offsetX.getValue() * model.xRange;
    double dy = point.y - this.model.cy - this.offsetY.getValue() * model.yRange;
    double dz = point.z - this.model.cz - this.offsetZ.getValue() * model.zRange;
    if (this.mirror.isOn()) {
      dx = Math.abs(dx);
      dy = Math.abs(dy);
      dz = Math.abs(dz);
    }
    return (
      this.hue.getValue() +
      this.spreadX.getValue() * this.xMult * dx +
      this.spreadY.getValue() * this.yMult * dy +
      this.spreadZ.getValue() * this.zMult * dz +
      this.spreadR.getValue() * this.rMult * (point.r - model.rMin)
     );
  }

  public final float getHuef(LXPoint point) {
    return (float) getHue(point);
  }

  public double getSaturation(LXPoint point) {
    return getSaturation();
  }

  public final float getSaturationf(LXPoint point) {
    return (float) getSaturation(point);
  }

  public int getColor() {
    return LXColor.hsb(getHue(), getSaturation(), 100);
  }

  public int getColor(double brightness) {
    if (brightness > 0) {
      return LXColor.hsb(getHue(), getSaturation(), brightness);
    }
    return LXColor.BLACK;
  }

  public int getColor(double saturation, double brightness) {
    if (brightness > 0) {
      return LXColor.hsb(getHue(), saturation, brightness);
    }
    return LXColor.BLACK;
  }

  public int getColor(LXPoint point) {
    return getColor(point, getSaturation(), 100);
  }

  public int getColor(LXPoint point, double brightness) {
    if (brightness > 0) {
      return getColor(point, getSaturation(point), brightness);
    }
    return LXColor.BLACK;
  }

  public int getColor(LXPoint point, double saturation, double brightness) {
    if (brightness > 0) {
      return LXColor.hsb(getHue(point), saturation, brightness);
    }
    return LXColor.BLACK;
  }

}

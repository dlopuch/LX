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

package heronarts.lx;

/**
 * Helper class of useful utilities, many just mirror Processing built-ins but
 * reduce the awkwardness of calling through applet in the library code.
 */
public class LXUtils {

  /**
   * Only used statically, need not be instantiated.
   */
  private LXUtils() {
  }

  public static double constrain(double value, double min, double max) {
    return value < min ? min : (value > max ? max : value);
  }

  public static float constrainf(float value, float min, float max) {
    return value < min ? min : (value > max ? max : value);
  }

  public static int constrain(int value, int min, int max) {
    return value < min ? min : (value > max ? max : value);
  }

  public static double random(double min, double max) {
    return min + Math.random() * (max - min);
  }

  public static double distance(double x1, double y1, double x2, double y2) {
    return Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
  }

  public static double lerp(double v1, double v2, double amt) {
    return v1 + (v2 - v1) * amt;
  }

  public static float lerpf(float v1, float v2, float amt) {
    return v1 + (v2 - v1) * amt;
  }

  public static double tri(double t) {
    t = t - Math.floor(t);
    if (t < 0.25) {
      return t * 4;
    } else if (t < 0.75) {
      return 1 - 4 * (t - 0.25);
    } else {
      return -1 + 4 * (t - 0.75);
    }
  }

  public static float trif(float t) {
    return (float) LXUtils.tri(t);
  }

  public static double avg(double v1, double v2) {
    return (v1 + v2) / 2.;
  }

  public static float avgf(float v1, float v2) {
    return (v1 + v2) / 2.f;
  }

  /**
   * Computes the distance between v1 and v2 with a wrap-around at the modulus.
   * Both v1 and v2 must be in the range [0, modulus]. For example, if v1=1, v2=11,
   * and modulus = 12, then the distance is 2, not 10.
   *
   * @param v1 First value
   * @param v2 Second value
   * @param modulus Modulus to wrap around
   * @return
   */
  public static double wrapdist(double v1, double v2, double modulus) {
    if (v1 < v2) {
      return Math.min(v2 - v1, v1 + modulus - v2);
    } else {
      return Math.min(v1 - v2, v2 + modulus - v1);
    }
  }

  public static float wrapdistf(float v1, float v2, float modulus) {
    if (v1 < v2) {
      return Math.min(v2 - v1, v1 + modulus - v2);
    } else {
      return Math.min(v1 - v2, v2 + modulus - v1);
    }
  }

  public static class LookupTable {

    public interface Function {
      static Function SIN = new Function() {
        public float compute(int i, int tableSize) {
          return (float) Math.sin(i * LX.TWO_PI / tableSize);
        }
      };

      public float compute(int i, int tableSize);
    }

    public static class Sin extends LookupTable {
      public Sin(int tableSize) {
        super(tableSize, Function.SIN);
      }

      public float sin(float radians) {
        return this.values[(int) Math.round(radians / LX.TWO_PI * this.tableSize)];
      }
    }

    protected final int tableSize;
    protected float[] values;

    public LookupTable(int tableSize, Function function) {
      this.tableSize = tableSize;
      this.values = new float[tableSize + 1];
      for (int i = 0; i <= tableSize; ++i) {
        this.values[i] = function.compute(i, tableSize);
      }
    }

    /**
     * Looks up the value in the table
     * @param basis
     * @return
     */
    public float get(float basis) {
      return this.values[Math.round(basis * this.tableSize)];
    }

    public float get(double basis) {
      return this.values[(int) Math.round(basis * this.tableSize)];
    }
  }
}

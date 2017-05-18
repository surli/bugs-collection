/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.integration;

import java.util.function.Function;

import com.opengamma.strata.collect.ArgChecker;

/**
 * Gauss-Legendre quadrature approximates the value of integrals of the form
 * $$
 * \begin{align*}
 * \int_{-1}^{1} f(x) dx
 * \end{align*}
 * $$
 * The weights and abscissas are generated by {@link GaussLegendreWeightAndAbscissaFunction}.
 * <p>
 * The function to integrate is scaled in such a way as to allow any values for the limits of the integrals.
 */
public class GaussLegendreQuadratureIntegrator1D extends GaussianQuadratureIntegrator1D {

  private static final Double[] LIMITS = new Double[] {-1., 1.};
  private static final GaussLegendreWeightAndAbscissaFunction GENERATOR = new GaussLegendreWeightAndAbscissaFunction();

  /**
   * @param n The number of sample points to be used in the integration, not negative or zero
   */
  public GaussLegendreQuadratureIntegrator1D(int n) {
    super(n, GENERATOR);
  }

  @Override
  public Double[] getLimits() {
    return LIMITS;
  }

  /**
   * {@inheritDoc}
   * To evaluate an integral over $[a, b]$, a change of interval must be performed:
   * $$
   * \begin{align*}
   * \int_a^b f(x)dx 
   * &= \frac{b - a}{2}\int_{-1}^1 f(\frac{b - a}{2} x + \frac{a + b}{2})dx\\
   * &\approx \frac{b - a}{2}\sum_{i=1}^n w_i f(\frac{b - a}{2} x + \frac{a + b}{2})
   * \end{align*}
   * $$
   */
  @Override
  public Function<Double, Double> getIntegralFunction(Function<Double, Double> function, Double lower, Double upper) {
    ArgChecker.notNull(function, "function");
    ArgChecker.notNull(lower, "lower");
    ArgChecker.notNull(upper, "upper");
    double m = (upper - lower) / 2;
    double c = (upper + lower) / 2;
    return new Function<Double, Double>() {
      @Override
      public Double apply(Double x) {
        return m * function.apply(m * x + c);
      }
    };
  }

}

/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.swaption;

import java.io.Serializable;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutablePreBuild;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;
import org.joda.beans.impl.direct.DirectPrivateBeanBuilder;

import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.market.param.ParameterMetadata;

/**
 * Surface node metadata for a surface node for swaptions with a specific time to expiry and strike.
 * <p>
 * This typically represents a node of swaption volatility surface parameterized by expiry and strike.
 */
@BeanDefinition(builderScope = "private")
public final class SwaptionSurfaceExpiryStrikeParameterMetadata
    implements ParameterMetadata, ImmutableBean, Serializable {

  /**
  * The year fraction of the surface node.
  * <p>
  * This is the time to expiry that the node on the surface is defined as.
  * There is not necessarily a direct relationship with a date from an underlying instrument.
  */
  @PropertyDefinition
  private final double yearFraction;
  /**
  * The strike of the surface node.
  */
  @PropertyDefinition
  private final double strike;
  /**
   * The label that describes the node.
   */
  @PropertyDefinition(validate = "notEmpty", overrideGet = true)
  private final String label;

  //-------------------------------------------------------------------------
  /**
   * Creates node metadata using swap convention, year fraction and strike.
   * 
   * @param yearFraction  the year fraction
   * @param strike  the strike
   * @return node metadata 
   */
  public static SwaptionSurfaceExpiryStrikeParameterMetadata of(
      double yearFraction,
      double strike) {

    String label = Pair.of(yearFraction, strike).toString();
    return new SwaptionSurfaceExpiryStrikeParameterMetadata(yearFraction, strike, label);
  }

  /**
   * Creates node using swap convention, year fraction, strike and label.
   * 
   * @param yearFraction  the year fraction
   * @param strike  the simple moneyness
   * @param label  the label to use
   * @return the metadata
   */
  public static SwaptionSurfaceExpiryStrikeParameterMetadata of(
      double yearFraction,
      double strike,
      String label) {

    return new SwaptionSurfaceExpiryStrikeParameterMetadata(yearFraction, strike, label);
  }

  @ImmutablePreBuild
  private static void preBuild(Builder builder) {
    if (builder.label == null) {
      builder.label = Pair.of(builder.yearFraction, builder.strike).toString();
    }
  }

  @Override
  public Pair<Double, Double> getIdentifier() {
    return Pair.of(yearFraction, strike);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code SwaptionSurfaceExpiryStrikeParameterMetadata}.
   * @return the meta-bean, not null
   */
  public static SwaptionSurfaceExpiryStrikeParameterMetadata.Meta meta() {
    return SwaptionSurfaceExpiryStrikeParameterMetadata.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(SwaptionSurfaceExpiryStrikeParameterMetadata.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private SwaptionSurfaceExpiryStrikeParameterMetadata(
      double yearFraction,
      double strike,
      String label) {
    JodaBeanUtils.notEmpty(label, "label");
    this.yearFraction = yearFraction;
    this.strike = strike;
    this.label = label;
  }

  @Override
  public SwaptionSurfaceExpiryStrikeParameterMetadata.Meta metaBean() {
    return SwaptionSurfaceExpiryStrikeParameterMetadata.Meta.INSTANCE;
  }

  @Override
  public <R> Property<R> property(String propertyName) {
    return metaBean().<R>metaProperty(propertyName).createProperty(this);
  }

  @Override
  public Set<String> propertyNames() {
    return metaBean().metaPropertyMap().keySet();
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the year fraction of the surface node.
   * <p>
   * This is the time to expiry that the node on the surface is defined as.
   * There is not necessarily a direct relationship with a date from an underlying instrument.
   * @return the value of the property
   */
  public double getYearFraction() {
    return yearFraction;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the strike of the surface node.
   * @return the value of the property
   */
  public double getStrike() {
    return strike;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the label that describes the node.
   * @return the value of the property, not empty
   */
  @Override
  public String getLabel() {
    return label;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      SwaptionSurfaceExpiryStrikeParameterMetadata other = (SwaptionSurfaceExpiryStrikeParameterMetadata) obj;
      return JodaBeanUtils.equal(yearFraction, other.yearFraction) &&
          JodaBeanUtils.equal(strike, other.strike) &&
          JodaBeanUtils.equal(label, other.label);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(yearFraction);
    hash = hash * 31 + JodaBeanUtils.hashCode(strike);
    hash = hash * 31 + JodaBeanUtils.hashCode(label);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(128);
    buf.append("SwaptionSurfaceExpiryStrikeParameterMetadata{");
    buf.append("yearFraction").append('=').append(yearFraction).append(',').append(' ');
    buf.append("strike").append('=').append(strike).append(',').append(' ');
    buf.append("label").append('=').append(JodaBeanUtils.toString(label));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code SwaptionSurfaceExpiryStrikeParameterMetadata}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code yearFraction} property.
     */
    private final MetaProperty<Double> yearFraction = DirectMetaProperty.ofImmutable(
        this, "yearFraction", SwaptionSurfaceExpiryStrikeParameterMetadata.class, Double.TYPE);
    /**
     * The meta-property for the {@code strike} property.
     */
    private final MetaProperty<Double> strike = DirectMetaProperty.ofImmutable(
        this, "strike", SwaptionSurfaceExpiryStrikeParameterMetadata.class, Double.TYPE);
    /**
     * The meta-property for the {@code label} property.
     */
    private final MetaProperty<String> label = DirectMetaProperty.ofImmutable(
        this, "label", SwaptionSurfaceExpiryStrikeParameterMetadata.class, String.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "yearFraction",
        "strike",
        "label");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case -1731780257:  // yearFraction
          return yearFraction;
        case -891985998:  // strike
          return strike;
        case 102727412:  // label
          return label;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends SwaptionSurfaceExpiryStrikeParameterMetadata> builder() {
      return new SwaptionSurfaceExpiryStrikeParameterMetadata.Builder();
    }

    @Override
    public Class<? extends SwaptionSurfaceExpiryStrikeParameterMetadata> beanType() {
      return SwaptionSurfaceExpiryStrikeParameterMetadata.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code yearFraction} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> yearFraction() {
      return yearFraction;
    }

    /**
     * The meta-property for the {@code strike} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> strike() {
      return strike;
    }

    /**
     * The meta-property for the {@code label} property.
     * @return the meta-property, not null
     */
    public MetaProperty<String> label() {
      return label;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -1731780257:  // yearFraction
          return ((SwaptionSurfaceExpiryStrikeParameterMetadata) bean).getYearFraction();
        case -891985998:  // strike
          return ((SwaptionSurfaceExpiryStrikeParameterMetadata) bean).getStrike();
        case 102727412:  // label
          return ((SwaptionSurfaceExpiryStrikeParameterMetadata) bean).getLabel();
      }
      return super.propertyGet(bean, propertyName, quiet);
    }

    @Override
    protected void propertySet(Bean bean, String propertyName, Object newValue, boolean quiet) {
      metaProperty(propertyName);
      if (quiet) {
        return;
      }
      throw new UnsupportedOperationException("Property cannot be written: " + propertyName);
    }

  }

  //-----------------------------------------------------------------------
  /**
   * The bean-builder for {@code SwaptionSurfaceExpiryStrikeParameterMetadata}.
   */
  private static final class Builder extends DirectPrivateBeanBuilder<SwaptionSurfaceExpiryStrikeParameterMetadata> {

    private double yearFraction;
    private double strike;
    private String label;

    /**
     * Restricted constructor.
     */
    private Builder() {
      super(meta());
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -1731780257:  // yearFraction
          return yearFraction;
        case -891985998:  // strike
          return strike;
        case 102727412:  // label
          return label;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case -1731780257:  // yearFraction
          this.yearFraction = (Double) newValue;
          break;
        case -891985998:  // strike
          this.strike = (Double) newValue;
          break;
        case 102727412:  // label
          this.label = (String) newValue;
          break;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
      return this;
    }

    @Override
    public SwaptionSurfaceExpiryStrikeParameterMetadata build() {
      preBuild(this);
      return new SwaptionSurfaceExpiryStrikeParameterMetadata(
          yearFraction,
          strike,
          label);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(128);
      buf.append("SwaptionSurfaceExpiryStrikeParameterMetadata.Builder{");
      buf.append("yearFraction").append('=').append(JodaBeanUtils.toString(yearFraction)).append(',').append(' ');
      buf.append("strike").append('=').append(JodaBeanUtils.toString(strike)).append(',').append(' ');
      buf.append("label").append('=').append(JodaBeanUtils.toString(label));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}

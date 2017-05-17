package com.opengamma.strata.measure.credit;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.calc.Measure;
import com.opengamma.strata.calc.runner.CalculationFunction;
import com.opengamma.strata.calc.runner.CalculationParameters;
import com.opengamma.strata.calc.runner.FunctionRequirements;
import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.measure.Measures;
import com.opengamma.strata.product.credit.Cds;
import com.opengamma.strata.product.credit.CdsTrade;
import com.opengamma.strata.product.credit.ResolvedCdsTrade;

public class CdsTradeCalculationFunction
    implements CalculationFunction<CdsTrade> {

  /**
   * The calculations by measure.
   */
  private static final ImmutableMap<Measure, SingleMeasureCalculation> CALCULATORS =
      ImmutableMap.<Measure, SingleMeasureCalculation>builder()
          .put(Measures.PRESENT_VALUE, CdsMeasureCalculations.DEFAULT::presentValue)
          .put(Measures.PV01_CALIBRATED_SUM, CdsMeasureCalculations.DEFAULT::pv01CalibratedSum)
          .put(Measures.PV01_CALIBRATED_BUCKETED, CdsMeasureCalculations.DEFAULT::pv01CalibratedBucketed)
          .put(Measures.PV01_MARKET_QUOTE_SUM, CdsMeasureCalculations.DEFAULT::pv01MarketQuoteSum)
          .put(Measures.PV01_MARKET_QUOTE_BUCKETED, CdsMeasureCalculations.DEFAULT::pv01MarketQuoteBucketed)
          .put(Measures.UNIT_PRICE, CdsMeasureCalculations.DEFAULT::unitPrice)
          .put(CreditMeasures.IR01_CALIBRATED__PARALLEL, CdsMeasureCalculations.DEFAULT::ir01MarketQuoteParallel)
          .put(CreditMeasures.IR01_CALIBRATED__BUCKETED, CdsMeasureCalculations.DEFAULT::ir01MarketQuoteBucketed)
          .put(CreditMeasures.IR01_MARKET_QUOTE_PARALLEL, CdsMeasureCalculations.DEFAULT::ir01MarketQuoteParallel)
          .put(CreditMeasures.IR01_MARKET_QUOTE_BUCKETED, CdsMeasureCalculations.DEFAULT::ir01MarketQuoteBucketed)
          .put(CreditMeasures.CS01_PARALLEL, CdsMeasureCalculations.DEFAULT::cs01Parallel)
          .put(CreditMeasures.CS01_BUCKETED, CdsMeasureCalculations.DEFAULT::cs01Bucketed)
          .put(CreditMeasures.RECOVERY01, CdsMeasureCalculations.DEFAULT::recovery01)
          .put(CreditMeasures.JUMP_TO_DEFAULT, CdsMeasureCalculations.DEFAULT::jumpToDefault)
          .put(CreditMeasures.EXPECTED_LOSS, CdsMeasureCalculations.DEFAULT::expectedLoss)
          .put(Measures.RESOLVED_TARGET, (rt, smd, rd) -> rt)
          .build();

  private static final ImmutableSet<Measure> MEASURES = CALCULATORS.keySet();

  /**
   * Creates an instance.
   */
  public CdsTradeCalculationFunction() {
  }

  //-------------------------------------------------------------------------
  @Override
  public Class<CdsTrade> targetType() {
    return CdsTrade.class;
  }

  @Override
  public Set<Measure> supportedMeasures() {
    return MEASURES;
  }

  @Override
  public Optional<String> identifier(CdsTrade target) {
    return target.getInfo().getId().map(id -> id.toString());
  }

  @Override
  public Currency naturalCurrency(CdsTrade trade, ReferenceData refData) {
    return trade.getProduct().getCurrency();
  }

  //-------------------------------------------------------------------------
  @Override
  public FunctionRequirements requirements(
      CdsTrade trade,
      Set<Measure> measures,
      CalculationParameters parameters,
      ReferenceData refData) {

    // extract data from product
    Cds product = trade.getProduct();
    StandardId legalEntityId = product.getLegalEntityId();
    Currency currency = product.getCurrency();
    // use lookup to build requirements
    CreditRatesMarketDataLookup lookup = parameters.getParameter(CreditRatesMarketDataLookup.class);
    return lookup.requirements(legalEntityId, currency);
  }

  //-------------------------------------------------------------------------
  @Override
  public Map<Measure, Result<?>> calculate(
      CdsTrade trade,
      Set<Measure> measures,
      CalculationParameters parameters,
      ScenarioMarketData scenarioMarketData,
      ReferenceData refData) {

    // resolve the trade once for all measures and all scenarios
    ResolvedCdsTrade resolved = trade.resolve(refData);

    // use lookup to query market data
    CreditRatesMarketDataLookup ledLookup = parameters.getParameter(CreditRatesMarketDataLookup.class);
    CreditRatesScenarioMarketData marketData = ledLookup.marketDataView(scenarioMarketData);

    // loop around measures, calculating all scenarios for one measure
    Map<Measure, Result<?>> results = new HashMap<>();
    for (Measure measure : measures) {
      results.put(measure, calculate(measure, resolved, marketData, refData));
    }
    return results;
  }

  // calculate one measure
  private Result<?> calculate(
      Measure measure,
      ResolvedCdsTrade trade,
      CreditRatesScenarioMarketData marketData,
      ReferenceData refData) {

    SingleMeasureCalculation calculator = CALCULATORS.get(measure);
    if (calculator == null) {
      return Result.failure(FailureReason.UNSUPPORTED, "Unsupported measure for CdsTrade: {}", measure);
    }
    return Result.of(() -> calculator.calculate(trade, marketData, refData));
  }

  //-------------------------------------------------------------------------
  @FunctionalInterface
  interface SingleMeasureCalculation {
    public abstract Object calculate(
        ResolvedCdsTrade trade,
        CreditRatesScenarioMarketData marketData,
        ReferenceData refData);
  }

}

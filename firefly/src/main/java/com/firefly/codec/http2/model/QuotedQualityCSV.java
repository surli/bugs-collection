package com.firefly.codec.http2.model;

import static java.lang.Integer.MIN_VALUE;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

/**
 * Implements a quoted comma separated list of quality values in accordance with
 * RFC7230 and RFC7231. Values are returned sorted in quality order, with OWS
 * and the quality parameters removed.
 * 
 * @see "https://tools.ietf.org/html/rfc7230#section-3.2.6"
 * @see "https://tools.ietf.org/html/rfc7230#section-7"
 * @see "https://tools.ietf.org/html/rfc7231#section-5.3.1"
 */
public class QuotedQualityCSV extends QuotedCSV implements Iterable<String> {
	private final static Double ZERO = new Double(0.0);
	private final static Double ONE = new Double(1.0);

	private final List<Double> _quality = new ArrayList<>();
	private boolean _sorted = false;
	private final Function<String, Integer> secondaryOrderingFunction;

	/**
	 * Sorts values with equal quality according to the length of the value
	 * String.
	 */
	public QuotedQualityCSV() {
		this((s) -> s.length());
	}

	/**
	 * Sorts values with equal quality according to given order.
	 */
	public QuotedQualityCSV(String[] serverPreferredValueOrder) {
		this((s) -> {
			for (int i = 0; i < serverPreferredValueOrder.length; ++i)
				if (serverPreferredValueOrder[i].equals(s))
					return serverPreferredValueOrder.length - i;

			if ("*".equals(s))
				return serverPreferredValueOrder.length;

			return MIN_VALUE;
		});
	}

	/**
	 * Orders values with equal quality with the given function.
	 */
	public QuotedQualityCSV(Function<String, Integer> secondaryOrderingFunction) {
		this.secondaryOrderingFunction = secondaryOrderingFunction;
	}

	public void addValue(String value) {
		super.addValue(value);
		while (_quality.size() < _values.size())
			_quality.add(ONE);
	}

	@Override
	protected void parsedValue(StringBuffer buffer) {
		super.parsedValue(buffer);
	}

	@Override
	protected void parsedParam(StringBuffer buffer, int valueLength, int paramName, int paramValue) {
		if (buffer.charAt(paramName) == 'q' && paramValue > paramName && buffer.charAt(paramName + 1) == '=') {
			Double q;
			try {
				q = (_keepQuotes && buffer.charAt(paramValue) == '"')
						? new Double(buffer.substring(paramValue + 1, buffer.length() - 1))
						: new Double(buffer.substring(paramValue));
			} catch (Exception e) {
				q = ZERO;
			}
			buffer.setLength(paramName - 1);

			while (_quality.size() < _values.size())
				_quality.add(ONE);
			_quality.add(q);
		}
	}

	public List<String> getValues() {
		if (!_sorted)
			sort();
		return _values;
	}

	@Override
	public Iterator<String> iterator() {
		if (!_sorted)
			sort();
		return _values.iterator();
	}

	protected void sort() {
		_sorted = true;

		Double last = ZERO;
		int lastOrderIndex = Integer.MIN_VALUE;

		for (int i = _values.size(); i-- > 0;) {
			String v = _values.get(i);
			Double q = _quality.get(i);

			int compare = last.compareTo(q);
			if (compare > 0 || (compare == 0 && secondaryOrderingFunction.apply(v) < lastOrderIndex)) {
				_values.set(i, _values.get(i + 1));
				_values.set(i + 1, v);
				_quality.set(i, _quality.get(i + 1));
				_quality.set(i + 1, q);
				last = ZERO;
				lastOrderIndex = 0;
				i = _values.size();
				continue;
			}

			last = q;
			lastOrderIndex = secondaryOrderingFunction.apply(v);
		}

		int last_element = _quality.size();
		while (last_element > 0 && _quality.get(--last_element).equals(ZERO)) {
			_quality.remove(last_element);
			_values.remove(last_element);
		}
	}
}

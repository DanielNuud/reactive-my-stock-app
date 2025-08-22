import { useState } from "react";
import useLiveConvertedPrice from "../hooks/useLiveConvertedPrice";

const RealTimePrice = ({ latestPrice, ticker }) => {
  const [showCurrencyOptions, setShowCurrencyOptions] = useState(false);
  const [selectedCurrency, setSelectedCurrency] = useState(null);

  const { price: convertedPrice, timestamp } = useLiveConvertedPrice(
    ticker,
    selectedCurrency
  );

  const displayPrice = selectedCurrency ? convertedPrice : latestPrice;
  const currencyLabel = selectedCurrency || "USD";

  return (
    <div className="panel real-time-card mb-3">
      <div className="panel-title">Real-Time Price</div>

      <div className="panel-value">
        {displayPrice != null ? (
          <span>
            {displayPrice.toFixed(2)} {currencyLabel}
          </span>
        ) : (
          <span className="muted">Loading...</span>
        )}
      </div>

      <div className="panel-actions">
        {!showCurrencyOptions ? (
          <button
            className="btn btn-primary btn-sm"
            onClick={() => setShowCurrencyOptions(true)}
          >
            Convert
          </button>
        ) : (
          <select
            className="select"
            value={selectedCurrency || ""}
            onChange={(e) => setSelectedCurrency(e.target.value)}
          >
            <option value="">Select currency</option>
            <option value="USD">USD</option>
            <option value="EUR">EUR</option>
            <option value="JPY">JPY</option>
            <option value="GBP">GBP</option>
          </select>
        )}
      </div>

      {selectedCurrency && timestamp && (
        <div className="panel-meta">
          Updated at {new Date(timestamp).toLocaleTimeString()}
        </div>
      )}
    </div>
  );
};

export default RealTimePrice;
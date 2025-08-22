import React, { useState } from "react";

const currencies = [
    { code: "EUR", name: "Euro" },
    { code: "USD", name: "US Dollar" },
    { code: "JPY", name: "Japanese Yen" },
    { code: "BGN", name: "Bulgarian Lev" },
    { code: "CZK", name: "Czech Republic Koruna" },
    { code: "DKK", name: "Danish Krone" },
    { code: "GBP", name: "British Pound Sterling" },
    { code: "HUF", name: "Hungarian Forint" },
    { code: "PLN", name: "Polish Zloty" },
    { code: "RON", name: "Romanian Leu" },
    { code: "SEK", name: "Swedish Krona" },
    { code: "CHF", name: "Swiss Franc" },
    { code: "ISK", name: "Icelandic Króna" },
    { code: "NOK", name: "Norwegian Krone" },
    { code: "HRK", name: "Croatian Kuna" },
    { code: "RUB", name: "Russian Ruble" },
    { code: "TRY", name: "Turkish Lira" },
    { code: "AUD", name: "Australian Dollar" },
    { code: "BRL", name: "Brazilian Real" },
    { code: "CAD", name: "Canadian Dollar" },
    { code: "CNY", name: "Chinese Yuan" },
    { code: "HKD", name: "Hong Kong Dollar" },
    { code: "IDR", name: "Indonesian Rupiah" },
    { code: "ILS", name: "Israeli New Sheqel" },
    { code: "INR", name: "Indian Rupee" },
    { code: "KRW", name: "South Korean Won" },
    { code: "MXN", name: "Mexican Peso" },
    { code: "MYR", name: "Malaysian Ringgit" },
    { code: "NZD", name: "New Zealand Dollar" },
    { code: "PHP", name: "Philippine Peso" },
    { code: "SGD", name: "Singapore Dollar" },
    { code: "THB", name: "Thai Baht" },
    { code: "ZAR", name: "South African Rand" }
];

const CurrencyConverter = () => {
    const [fromCurrency, setFromCurrency] = useState("");
    const [toCurrency, setToCurrency] = useState("");
    const [amount, setAmount] = useState();
    const [result, setResult] = useState(null);

    const handleConvert = async () => {
        if (fromCurrency === toCurrency) return;

        try {
            const response = await fetch(`/api/currency/convert?from=${fromCurrency}&to=${toCurrency}&amount=${amount}`);
            const data = await response.json();
            setResult(data || "Error");
        } catch (err) {
            console.error(err);
            setResult("Error");
        }
    };

    return (
        <div className="converter-container">
            <select
                value={fromCurrency}
                onChange={(e) => setFromCurrency(e.target.value)}
                className="search-input currency-select"
            >
                <option value="" disabled hidden>Enter currency from...</option>
                {currencies.map((c) => (
                    <option key={c.code} value={c.code}>
                        {c.code} – {c.name}
                    </option>
                ))}
            </select>

            <select
                value={toCurrency}
                onChange={(e) => setToCurrency(e.target.value)}
                className="search-input currency-select"
            >
                <option value="" disabled hidden>Enter currency to...</option>
                {currencies
                    .filter((c) => c.code !== fromCurrency) 
                    .map((c) => (
                        <option key={c.code} value={c.code}>
                            {c.code} – {c.name}
                        </option>
                    ))}
            </select>

            <input
                type="number"
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
                className="search-input amount-input"
                placeholder="Enter amount"
                min="0"
            />

            <button onClick={handleConvert} className="search-button">
                Convert
            </button>

            {result && (
                <div className="converter-result">
                    {amount} {fromCurrency} = {result} {toCurrency}
                </div>
            )}
        </div>
    );
};

export default CurrencyConverter;

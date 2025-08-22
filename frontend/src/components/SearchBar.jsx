import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";

const SearchBar = () => {
    const [ticker, setTicker] = useState("");
    const [suggestions, setSuggestions] = useState([]);
    const navigate = useNavigate();

    useEffect(() => {
        const fetchSuggestions = async () => {
            if (ticker.trim() === "") {
                setSuggestions([]);
                return;
            }
            try {
                const response = await fetch(`${import.meta.env.VITE_API_URL}/api/tickers/search?query=${ticker}`);
                if (response.ok) {
                    const data = await response.json();
                    setSuggestions(data);
                } else {
                    setSuggestions([]);
                }
            } catch (error) {
                console.error(error);
                setSuggestions([]);
            }
        };

        const timeoutId = setTimeout(fetchSuggestions, 300);
        return () => clearTimeout(timeoutId);
    }, [ticker]);

    const handleSubmit = (e) => {
        e.preventDefault();
        if (ticker.trim() !== "") {
            navigate(`/company/${ticker.trim().toUpperCase()}`);
            setSuggestions([]);
        }
    };

    const handleSuggestionClick = (selectedTicker) => {
        navigate(`/company/${selectedTicker}`);
        setSuggestions([]);
    };

    return (
        <div className="search-container">
            <form onSubmit={handleSubmit} className="search-form">
                <input
                    type="text"
                    placeholder="Enter stock ticker..."
                    value={ticker}
                    onChange={(e) => setTicker(e.target.value)}
                    className="search-input"
                />
                <button type="submit" className="search-button">
                    Search
                </button>
            </form>
            {suggestions.length > 0 && (
                <ul className="autocomplete-panel">
                    {suggestions.slice(0, 10).map((s) => (
                        <li
                            key={s.ticker}
                            className="autocomplete-item"
                            onClick={() => handleSuggestionClick(s.ticker)}
                        >
                            <img
                                src={s.iconUrl || "https://via.placeholder.com/24"}
                                alt={s.ticker}
                                className="autocomplete-icon"
                            />
                            <div>
                                <strong>${s.ticker}</strong> – {s.companyName}
                            </div>
                        </li>
                    ))}
                </ul>
            )}
        </div>
    );
};

export default SearchBar;

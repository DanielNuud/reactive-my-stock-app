import { useState } from "react";
import SearchBar from "../components/SearchBar.jsx";
import CurrencyConverter from "../components/CurrencyConverter.jsx";
import TopBar from "../components/TopBar";

const SearchPage = () => {
    const [selected, setSelected] = useState("search");

    return (
        <>
        <TopBar title="My Stock App" />
        <div className="search-page d-flex flex-column align-items-center justify-content-start min-vh-100 px-3 pt-5">

            <h1 className="display-3 fw-bold mb-3 text-white">My Stock App</h1>

            <div className="mb-5 text-center" style={{ maxWidth: "720px" }}>
                <p className="fs-5 text-light mb-3">
                    This prototype was created as part of a diploma thesis comparing traditional and reactive microservice architectures.
                </p>
                <p className="fs-5 text-light">
                    It demonstrates key financial features like real-time stock price tracking and live currency conversion, using a Spring Boot backend and a microservice-based approach.
                </p>
            </div>

            <h2 className="h4 fw-bold text-white mb-3">Choose Mode</h2>
            <div className="btn-group mb-4" role="group">
                <input
                    type="radio"
                    className="btn-check"
                    name="options"
                    id="option1"
                    autoComplete="off"
                    checked={selected === "search"}
                    onChange={() => setSelected("search")}
                />
                <label className="btn btn-outline-primary" htmlFor="option1">
                    Search
                </label>

                <input
                    type="radio"
                    className="btn-check"
                    name="options"
                    id="option2"
                    autoComplete="off"
                    checked={selected === "convert"}
                    onChange={() => setSelected("convert")}
                />
                <label className="btn btn-outline-primary" htmlFor="option2">
                    Currency Converter
                </label>
            </div>

            {selected === "search" && (
                <>
                    <p className="text-light mb-3">
                        Enter a ticker to track live prices, view charts, and analyze company details.
                    </p>
                    <div className="search-bar-wrapper">
                        <SearchBar />
                    </div>
                </>
            )}

            {selected === "convert" && (
                <>
                    <p className="text-light mb-3">
                        Convert currencies using real-time exchange rates powered by our backend service.
                    </p>
                    <div className="currency-converter-wrapper">
                        <CurrencyConverter />
                    </div>
                </>
            )}
        </div>
        </>
    );
};

export default SearchPage;

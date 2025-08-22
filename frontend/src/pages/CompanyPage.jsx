import React, { useState, useEffect } from "react";
import { useParams } from "react-router-dom";
import { Line } from "react-chartjs-2";
import {
    Chart as ChartJS,
    CategoryScale,
    LinearScale,
    PointElement,
    LineElement,
    Title,
    Tooltip,
    Legend,
} from "chart.js";
import useLiveStockChartData from "../hooks/useLiveStockChartData";
import RealTimePrice from "../components/RealTimePrice";
import TopBar from "../components/TopBar";

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, Title, Tooltip, Legend);


const periods = [
    { label: "Today (Live)", value: "today" },
    { label: "1 Week", value: "one_week" },
    { label: "1 Month", value: "one_month" },
    { label: "1 Year", value: "one_year" },
    { label: "5 Years", value: "five_years" },
];

const CompanyPage = () => {
    const { ticker } = useParams();
    const [company, setCompany] = useState(null);
    const [news, setNews] = useState([]);
    const [loading, setLoading] = useState(true);
    const [period, setPeriod] = useState("one_week");
    const [companyError, setCompanyError] = useState(false);
    const [newsError, setNewsError] = useState(false);
    const [historicalError, setHistoricalError] = useState(false);
    const [historicalData, setHistoricalData] = useState([]);
    const [latestData, setLatestData] = useState(null);
    useLiveStockChartData(ticker, setLatestData);

    useEffect(() => {
        if (period !== "today" || !latestData) return;

        setHistoricalData(prev => {
            const label = new Date(latestData.timestamp).toLocaleTimeString();
            const value = latestData.price;

            const last = prev[prev.length - 1];
            if (last && last.label === label && last.value === value) return prev;

            const next = [...prev, { label, value }];
            return next.length > 300 ? next.slice(next.length - 300) : next;
        });
    }, [latestData, period]);

    useEffect(() => {
        const fetchCompany = async () => {
            try {
                const response = await fetch(`${import.meta.env.VITE_API_URL}/api/companies/${ticker}`);
                if (!response.ok) throw new Error("Company service unavailable");
                const data = await response.json();
                setCompany(data);
            } catch (error) {
                console.error("Error fetching company data:", error);
                setCompanyError(true);
            }
        };

        const fetchNews = async () => {
            try {
                const response = await fetch(`${import.meta.env.VITE_API_URL}/api/news/${ticker}`);
                if (!response.ok) throw new Error("News service unavailable");
                const data = await response.json();
                setNews(data);
            } catch (error) {
                console.error("Error fetching news:", error);
                setNewsError(true);
            }
        };

        const loadAll = async () => {
            await Promise.all([fetchCompany(), fetchNews()]);
            setLoading(false);
        };

        loadAll();
    }, [ticker]);


    useEffect(() => {
        const fetchHistorical = async () => {
            try {
                const response = await fetch(
                    `${import.meta.env.VITE_API_URL}/api/historical/${ticker}?period=${period}`
                );
                if (!response.ok) throw new Error("Historical service unavailable");
                const raw = await response.json();

                const points = raw
                    .filter(r => r && (r.closePrice ?? r.close) != null)
                    .map(r => ({
                        label: r.date
                            ? r.date
                            : new Date(r.timestamp).toISOString(),
                        value: (r.closePrice ?? r.close) * 1
                    }));

                setHistoricalData(points);
            } catch (error) {
                console.error("Error fetching historical data:", error);
                setHistoricalError(true);
            }
        };

        fetchHistorical();
    }, [ticker, period]);

    const chartData = {
        labels: historicalData.map(p => p.label),
        datasets: [
            {
                label: `${ticker} Price`,
                data: historicalData.map(p => p.value),
                fill: false,
                borderColor: "rgb(75, 192, 192)",
                tension: 0.1,
            },
        ],
    };

    const handlePeriodChange = async (selectedPeriod) => {
        setPeriod(selectedPeriod);
    };


    if (loading) {
        return <div className="text-center text-light mt-5">Loading...</div>;
    }

    if (companyError || !company) {
        return <div className="text-center text-danger mt-5">No available information about company</div>;
    }

    return (
        <>
        <TopBar title="My Stock App" />
        
        <div className="container-fluid mt-3">
            <div className="row">
                {/* LEFT SIDE */}
                <div className="col-md-8 mb-5">

                    <div className="company-header">
                        <img
                            src={`${import.meta.env.VITE_API_URL || ""}/api/assets/logo/${company.ticker}`}
                            alt="Company Logo"
                        />
                        <h3>{company.name}</h3>
                        <span className="badge bg-light text-dark">{company.ticker}</span>
                    </div>

                    <div className="historical-chart-card p-3 mb-4 bg-dark rounded text-light">
                        <div className="d-flex mb-2 justify-content-center">
                            {periods.map(p => (
                                <button
                                    key={p.value}
                                    onClick={() => handlePeriodChange(p.value)}
                                    className={`btn btn-sm mx-1 ${p.value === period ? 'btn-primary' : 'btn-outline-primary'
                                        }`}
                                >
                                    {p.label}
                                </button>
                            ))}
                        </div>

                        {historicalError ? (
                            <p className="text-center text-danger">Chart information isnt available</p>
                        ) : historicalData.length > 0 ? (
                            <Line
                                data={chartData}
                                options={{
                                    responsive: true,
                                    plugins: {
                                        legend: { display: false },
                                        tooltip: { mode: 'index', intersect: false },
                                    },
                                    scales: {
                                        x: { display: true },
                                        y: { display: true },
                                    },
                                }}
                            />
                        ) : (
                            <p className="text-center">Loading chart...</p>
                        )}
                    </div>



                    <div className="company-info-card container-fluid">
                        <div className="row g-2">
                            {/* Full width description */}
                            <div className="col-md-12">
                                <div className="info-item flex-column align-items-center text-center">
                                    <i className="bi bi-info-circle mb-2" style={{ fontSize: "2rem" }}></i>
                                    <span>{company.description}</span>
                                </div>
                            </div>

                            {/* Row 1 */}
                            <div className="col-md-6">
                                <div className="info-item justify-content-center">
                                    <i className="bi bi-globe"></i>
                                    <a href={company.homepageUrl} target="_blank" rel="noopener noreferrer">
                                        {company.homepageUrl}
                                    </a>
                                </div>
                            </div>
                            <div className="col-md-6">
                                <div className="info-item justify-content-center">
                                    <i className="bi bi-bar-chart"></i>
                                    <span>{company.primaryExchange}</span>
                                </div>
                            </div>

                            {/* Row 2 */}
                            <div className="col-md-6">
                                <div className="info-item justify-content-center">
                                    <i className="bi bi-currency-dollar"></i>
                                    <span>{company.marketCap.toLocaleString()} USD</span>
                                </div>
                            </div>
                            <div className="col-md-6">
                                <div className="info-item justify-content-center">
                                    <i className="bi bi-geo-alt"></i>
                                    <span>{company.city}, {company.address1}</span>
                                </div>
                            </div>
                        </div>
                    </div>

                </div>

                {/* RIGHT SIDE NEWS */}
                <div className="col-md-4">

                    <RealTimePrice latestPrice={latestData?.price} ticker={company.ticker} />

                    <div className="news-section">
                        <h4 className="text-center mb-3">Latest News</h4>

                        {newsError ? (
                            <p className="text-danger text-center">News unavailable, please try again later</p>
                        ) : news.length > 0 ? (
                            news.map(article => (
                                <div key={article.id} className="news-card d-flex flex-column flex-md-row align-items-center mb-2">
                                    {article.imageUrl && (
                                        <img
                                            src={article.imageUrl}
                                            alt="News Image"
                                            className="news-image me-md-3 mb-2 mb-md-0"
                                        />
                                    )}
                                    <div className="flex-grow-1">
                                        <h6 className="mb-1">{article.title}</h6>
                                        <p className="mb-1 small">
                                            {article.publisherName} • {new Date(article.publishedUtc).toLocaleDateString()}
                                            <br />
                                            <span className="text-secondary">Author: {article.author}</span>
                                        </p>
                                        <p className="mb-2 small">{article.description}</p>
                                        <a
                                            href={article.articleUrl}
                                            target="_blank"
                                            rel="noopener noreferrer"
                                            className="btn btn-sm btn-outline-primary"
                                        >
                                            Read Article
                                        </a>
                                    </div>
                                </div>
                            ))
                        ) : (
                            <p>No news available.</p>
                        )}
                    </div>
                </div>

            </div>
        </div>
        </>
    );
};

export default CompanyPage;

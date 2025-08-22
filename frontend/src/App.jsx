import React from "react";
import { Routes, Route } from "react-router-dom";
import SearchPage from "./pages/SearchPage.jsx";
import CompanyPage from "./pages/CompanyPage.jsx";
import { NotificationsProvider } from "./context/NotificationContext.jsx";

const App = () => {
    return (
        <NotificationsProvider>
            <Routes>
                <Route path="/" element={<SearchPage />} />
                <Route path="/company/:ticker" element={<CompanyPage />} />
            </Routes>
        </NotificationsProvider>
    );
};

export default App;

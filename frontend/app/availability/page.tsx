"use client";

import { useState } from "react";
import styles from "./page.module.css";

interface CarAvailability {
    carId: string;
    model: string;
    isAvailable: boolean;
    conflicts: number;
    estimatedPrice: number | null;
}

const API_BASE = "http://localhost:8080";

export default function AvailabilityPage() {
    const [fromDate, setFromDate] = useState("");
    const [toDate, setToDate] = useState("");
    const [results, setResults] = useState<CarAvailability[] | null>(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const handleCheck = async () => {
        if (!fromDate || !toDate) {
            setError("Please select both From and To dates.");
            return;
        }

        if (fromDate >= toDate) {
            setError("'From' date must be before 'To' date.");
            return;
        }

        setLoading(true);
        setError(null);
        setResults(null);

        try {
            const res = await fetch(
                `${API_BASE}/api/availability?from=${fromDate}&to=${toDate}`
            );

            if (!res.ok) {
                const body = await res.json().catch(() => null);
                throw new Error(
                    body?.error || `Server error: ${res.status} ${res.statusText}`
                );
            }

            const data: CarAvailability[] = await res.json();
            setResults(data);
        } catch (err: unknown) {
            if (err instanceof TypeError && err.message === "Failed to fetch") {
                setError(
                    "Cannot connect to the backend. Make sure the server is running on port 8080."
                );
            } else if (err instanceof Error) {
                setError(err.message);
            } else {
                setError("An unexpected error occurred.");
            }
        } finally {
            setLoading(false);
        }
    };

    const formatPrice = (price: number | null): string => {
        if (price === null || price === undefined) return "N/A";
        return `€${price.toFixed(2)}`;
    };

    return (
        <div className={styles.container}>
            {/* Header */}
            <header className={styles.header}>
                <h1 className={styles.title}>Car Availability</h1>
                <p className={styles.subtitle}>
                    Check vehicle availability and pricing for your rental period
                </p>
            </header>

            {/* Search Form */}
            <div className={styles.searchCard}>
                <div className={styles.formRow}>
                    <div className={styles.field}>
                        <label htmlFor="from-date" className={styles.label}>
                            From
                        </label>
                        <input
                            id="from-date"
                            type="date"
                            className={styles.input}
                            value={fromDate}
                            onChange={(e) => setFromDate(e.target.value)}
                        />
                    </div>
                    <div className={styles.field}>
                        <label htmlFor="to-date" className={styles.label}>
                            To
                        </label>
                        <input
                            id="to-date"
                            type="date"
                            className={styles.input}
                            value={toDate}
                            onChange={(e) => setToDate(e.target.value)}
                        />
                    </div>
                    <button
                        id="check-availability"
                        className={styles.button}
                        onClick={handleCheck}
                        disabled={loading}
                    >
                        {loading ? "Checking…" : "Check Availability"}
                    </button>
                </div>
            </div>

            {/* Loading State */}
            {loading && (
                <div className={styles.loadingWrapper}>
                    <div className={styles.spinner} />
                    <span className={styles.loadingText}>
                        Checking availability…
                    </span>
                </div>
            )}

            {/* Error State */}
            {error && (
                <div className={styles.errorBox}>
                    <span className={styles.errorIcon}>⚠</span>
                    <span>{error}</span>
                </div>
            )}

            {/* Results Table */}
            {results && results.length > 0 && (
                <div className={styles.tableCard}>
                    <div className={styles.tableHeader}>
                        <span className={styles.tableTitle}>Results</span>
                        <span className={styles.carCount}>
                            {results.length} vehicle{results.length !== 1 ? "s" : ""}
                        </span>
                    </div>
                    <div className={styles.tableWrapper}>
                        <table className={styles.table}>
                            <thead>
                                <tr>
                                    <th>Model</th>
                                    <th>Status</th>
                                    <th>Estimated Price</th>
                                    <th>Conflicts</th>
                                </tr>
                            </thead>
                            <tbody>
                                {results.map((car) => (
                                    <tr key={car.carId}>
                                        <td className={styles.model}>{car.model}</td>
                                        <td>
                                            {car.isAvailable ? (
                                                <span
                                                    className={`${styles.badge} ${styles.badgeAvailable}`}
                                                >
                                                    ● Available
                                                </span>
                                            ) : (
                                                <span
                                                    className={`${styles.badge} ${styles.badgeUnavailable}`}
                                                >
                                                    ● Not Available
                                                </span>
                                            )}
                                        </td>
                                        <td>
                                            {car.estimatedPrice !== null ? (
                                                <span className={styles.price}>
                                                    {formatPrice(car.estimatedPrice)}
                                                </span>
                                            ) : (
                                                <span className={styles.priceNa}>N/A</span>
                                            )}
                                        </td>
                                        <td>
                                            {car.conflicts > 0 ? (
                                                <span
                                                    className={`${styles.badge} ${styles.badgeConflicts}`}
                                                >
                                                    ⚠ {car.conflicts} conflict
                                                    {car.conflicts !== 1 ? "s" : ""}
                                                </span>
                                            ) : (
                                                <span className={styles.noConflicts}>None</span>
                                            )}
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                </div>
            )}
        </div>
    );
}

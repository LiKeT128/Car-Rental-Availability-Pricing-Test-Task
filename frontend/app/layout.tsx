import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "Car Rental — Availability & Pricing",
  description:
    "Check car rental availability and estimated pricing for your travel dates.",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  );
}

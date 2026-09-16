import type { Metadata } from "next";
import { ClinicProvider } from "@/features/dental/clinic-provider";
import "./globals.css";

export const metadata: Metadata = {
  title: "Dental Clinic Management",
  description: "Generic dental clinic management system",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="en" className="h-full antialiased">
      <body className="min-h-full flex flex-col"><ClinicProvider>{children}</ClinicProvider></body>
    </html>
  );
}

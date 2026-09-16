"use client";

import { useEffect, type ReactNode } from "react";
import type { AppointmentStatus, CaseStatus } from "../types";

export function Icon({ name, className = "h-5 w-5" }: { name: string; className?: string }) {
  const paths: Record<string, ReactNode> = {
    grid: <><rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/><rect x="14" y="14" width="7" height="7" rx="1"/></>,
    calendar: <><rect x="3" y="5" width="18" height="16" rx="2"/><path d="M16 3v4M8 3v4M3 10h18"/></>,
    users: <><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M22 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75"/></>,
    user: <><circle cx="12" cy="8" r="4"/><path d="M4 21a8 8 0 0 1 16 0"/></>,
    doctor: <><circle cx="12" cy="7" r="4"/><path d="M5 21v-2a7 7 0 0 1 14 0v2M9 14l3 4 3-4M18 8h4M20 6v4"/></>,
    card: <><rect x="2" y="5" width="20" height="14" rx="2"/><path d="M2 10h20M6 15h4"/></>,
    settings: <><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.7 1.7 0 0 0 .34 1.88l.06.06-2.83 2.83-.06-.06a1.7 1.7 0 0 0-1.88-.34 1.7 1.7 0 0 0-1.03 1.56V21h-4v-.08A1.7 1.7 0 0 0 9 19.37a1.7 1.7 0 0 0-1.88.34l-.06.06-2.83-2.83.06-.06A1.7 1.7 0 0 0 4.63 15 1.7 1.7 0 0 0 3.08 14H3v-4h.08A1.7 1.7 0 0 0 4.63 9a1.7 1.7 0 0 0-.34-1.88l-.06-.06 2.83-2.83.06.06A1.7 1.7 0 0 0 9 4.63h.01A1.7 1.7 0 0 0 10 3.08V3h4v.08A1.7 1.7 0 0 0 15 4.63a1.7 1.7 0 0 0 1.88-.34l.06-.06 2.83 2.83-.06.06A1.7 1.7 0 0 0 19.37 9v.01A1.7 1.7 0 0 0 20.92 10H21v4h-.08A1.7 1.7 0 0 0 19.4 15Z"/></>,
    briefcase: <><rect x="3" y="7" width="18" height="13" rx="2"/><path d="M8 7V4h8v3M3 12h18M10 12v2h4v-2"/></>,
    plus: <path d="M12 5v14M5 12h14"/>, search: <><circle cx="11" cy="11" r="7"/><path d="m20 20-4-4"/></>,
    chevron: <path d="m9 18 6-6-6-6"/>, menu: <path d="M4 6h16M4 12h16M4 18h16"/>, close: <path d="m6 6 12 12M18 6 6 18"/>,
    clock: <><circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/></>, check: <path d="m5 12 4 4L19 6"/>, alert: <><circle cx="12" cy="12" r="9"/><path d="M12 8v5M12 17h.01"/></>,
    logout: <><path d="M10 17l5-5-5-5M15 12H3M15 4h4a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2h-4"/></>,
  };
  return <svg aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" className={className}>{paths[name] ?? paths.grid}</svg>;
}

export function Button({ children, variant = "primary", className = "", ...props }: React.ButtonHTMLAttributes<HTMLButtonElement> & { variant?: "primary" | "secondary" | "danger" | "ghost" }) {
  const variants = { primary: "bg-[#176b73] text-white hover:bg-[#125b62] shadow-sm", secondary: "border border-slate-200 bg-white text-slate-700 hover:bg-slate-50", danger: "bg-red-50 text-red-700 hover:bg-red-100", ghost: "text-slate-600 hover:bg-slate-100" };
  return <button {...props} className={`inline-flex min-h-10 items-center justify-center gap-2 rounded-xl px-4 py-2 text-sm font-semibold transition disabled:cursor-not-allowed disabled:opacity-45 ${variants[variant]} ${className}`}>{children}</button>;
}

export function Card({ children, className = "" }: { children: ReactNode; className?: string }) { return <section className={`rounded-2xl border border-slate-200/80 bg-white shadow-[0_8px_30px_rgba(15,23,42,0.04)] ${className}`}>{children}</section>; }
export function Field({ label, error, children }: { label: string; error?: string; children: ReactNode }) { return <label className="block text-sm font-semibold text-slate-700"><span className="mb-1.5 block">{label}</span>{children}{error && <span className="mt-1 block text-xs font-medium text-red-600">{error}</span>}</label>; }
export const inputClass = "min-h-11 w-full rounded-xl border border-slate-200 bg-white px-3.5 text-sm text-slate-900 outline-none transition placeholder:text-slate-400 focus:border-[#4b9097] focus:ring-4 focus:ring-[#176b73]/10";

export function StatusBadge({ status }: { status: AppointmentStatus | CaseStatus }) {
  const style: Record<string, string> = { Scheduled: "bg-blue-50 text-blue-700 ring-blue-600/10", "Checked In": "bg-amber-50 text-amber-700 ring-amber-600/10", "In Progress": "bg-violet-50 text-violet-700 ring-violet-600/10", Completed: "bg-emerald-50 text-emerald-700 ring-emerald-600/10", Cancelled: "bg-red-50 text-red-700 ring-red-600/10", Open: "bg-sky-50 text-sky-700 ring-sky-600/10" };
  return <span className={`inline-flex whitespace-nowrap rounded-full px-2.5 py-1 text-xs font-bold ring-1 ring-inset ${style[status]}`}>{status}</span>;
}

export function Modal({ title, description, children, onClose, wide = false }: { title: string; description?: string; children: ReactNode; onClose(): void; wide?: boolean }) {
  useEffect(() => { const fn = (event: KeyboardEvent) => event.key === "Escape" && onClose(); document.addEventListener("keydown", fn); return () => document.removeEventListener("keydown", fn); }, [onClose]);
  return <div className="fixed inset-0 z-50 flex items-end justify-center bg-slate-950/40 p-0 backdrop-blur-[2px] sm:items-center sm:p-5" onMouseDown={(event) => event.target === event.currentTarget && onClose()}><div role="dialog" aria-modal="true" className={`max-h-[92vh] w-full overflow-y-auto rounded-t-3xl bg-white p-5 shadow-2xl sm:rounded-2xl sm:p-6 ${wide ? "max-w-3xl" : "max-w-xl"}`}><header className="mb-5 flex items-start justify-between gap-4"><div><h2 className="text-xl font-bold tracking-tight text-slate-950">{title}</h2>{description && <p className="mt-1 text-sm text-slate-500">{description}</p>}</div><button aria-label="Close dialog" onClick={onClose} className="rounded-lg p-2 text-slate-500 hover:bg-slate-100"><Icon name="close" /></button></header>{children}</div></div>;
}

export function EmptyState({ title, description, action }: { title: string; description: string; action?: ReactNode }) { return <div className="flex min-h-52 flex-col items-center justify-center px-5 text-center"><span className="mb-4 rounded-2xl bg-slate-100 p-3 text-slate-400"><Icon name="search" className="h-6 w-6" /></span><h3 className="font-bold text-slate-800">{title}</h3><p className="mt-1 max-w-sm text-sm text-slate-500">{description}</p>{action && <div className="mt-4">{action}</div>}</div>; }
export function LoadingScreen() { return <div className="flex min-h-[60vh] items-center justify-center"><div className="text-center"><span className="mx-auto block h-9 w-9 animate-spin rounded-full border-4 border-slate-200 border-t-[#176b73]"/><p className="mt-4 text-sm font-medium text-slate-500">Preparing your clinic workspace…</p></div></div>; }
export function ErrorScreen({ message }: { message: string }) { return <div className="mx-auto mt-20 max-w-md rounded-2xl border border-red-200 bg-red-50 p-6 text-center"><Icon name="alert" className="mx-auto h-7 w-7 text-red-600"/><h2 className="mt-3 font-bold text-red-900">Something went wrong</h2><p className="mt-1 text-sm text-red-700">{message}</p></div>; }

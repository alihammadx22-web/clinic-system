"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useState, type ReactNode } from "react";
import { useClinic } from "../clinic-provider";
import type { PortalRole } from "../types";
import { Icon } from "./ui";

const links: Record<PortalRole, { href: string; label: string; icon: string }[]> = {
  reception: [
    { href: "/reception", label: "Dashboard", icon: "grid" },
    { href: "/reception/appointments", label: "Appointments", icon: "calendar" },
    { href: "/reception/patients", label: "Patients", icon: "users" },
    { href: "/reception/doctors", label: "Doctors & schedules", icon: "doctor" },
  ],
  doctor: [
    { href: "/doctor", label: "Today", icon: "grid" },
    { href: "/doctor/patients", label: "Patients", icon: "users" },
    { href: "/doctor/cases", label: "Dental cases", icon: "briefcase" },
  ]
};

const labels: Record<PortalRole, string> = { reception: "Reception portal", doctor: "Doctor portal" };

export function PortalShell({ role, children }: { role: PortalRole; children: ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const { data, notice, clearNotice, signOut } = useClinic();
  const [mobileOpen, setMobileOpen] = useState(false);
  const currentName = role === "doctor" ? data?.doctors[0]?.name : "Reception team";

  function switchRole(next: PortalRole) { router.push(`/${next}`); setMobileOpen(false); }

  return <div className="min-h-screen bg-[#f5f8f8] text-slate-900">
    {notice && <button onClick={clearNotice} className={`fixed right-4 top-4 z-[70] flex max-w-sm items-center gap-3 rounded-xl px-4 py-3 text-left text-sm font-semibold text-white shadow-xl ${notice.kind === "success" ? "bg-[#176b73]" : "bg-red-600"}`}><span className="rounded-full bg-white/20 p-1"><Icon name={notice.kind === "success" ? "check" : "alert"} className="h-4 w-4" /></span>{notice.message}</button>}
    <aside className={`fixed inset-y-0 left-0 z-40 flex w-[276px] flex-col bg-[#0c3440] text-white transition-transform duration-200 lg:translate-x-0 ${mobileOpen ? "translate-x-0" : "-translate-x-full"}`}>
      <div className="flex h-[82px] items-center justify-between border-b border-white/10 px-6">
        <Link href={`/${role}`} className="flex items-center gap-3" onClick={() => setMobileOpen(false)}><span className="grid h-10 w-10 place-items-center rounded-xl bg-white text-[#176b73] shadow-sm"><DentalMark /></span><span><b className="block text-[17px] tracking-tight">Dental Clinic</b><span className="text-[11px] font-semibold uppercase tracking-[.16em] text-white/55">{labels[role]}</span></span></Link>
        <button onClick={() => setMobileOpen(false)} className="p-2 lg:hidden" aria-label="Close menu"><Icon name="close" /></button>
      </div>
      <nav className="flex-1 space-y-1 overflow-y-auto p-4 pt-6">
        <p className="mb-3 px-3 text-[10px] font-bold uppercase tracking-[.18em] text-white/35">Workspace</p>
        {links[role].map((item) => { const active = pathname === item.href || (item.href !== `/${role}` && pathname.startsWith(`${item.href}/`)); return <Link key={item.href} href={item.href} onClick={() => setMobileOpen(false)} className={`flex items-center gap-3 rounded-xl px-3 py-3 text-sm font-semibold transition ${active ? "bg-white text-[#0c3440] shadow-md" : "text-white/70 hover:bg-white/8 hover:text-white"}`}><Icon name={item.icon} className="h-[19px] w-[19px]" />{item.label}</Link>; })}
      </nav>
      <div className="border-t border-white/10 p-4"><Link href="/login" onClick={signOut} className="flex items-center gap-3 rounded-xl px-3 py-3 text-sm font-semibold text-white/65 hover:bg-white/8 hover:text-white"><Icon name="logout" className="h-[19px] w-[19px]"/>Sign out</Link></div>
    </aside>
    {mobileOpen && <button className="fixed inset-0 z-30 bg-slate-950/35 lg:hidden" aria-label="Close navigation" onClick={() => setMobileOpen(false)} />}
    <div className="lg:pl-[276px]">
      <header className="sticky top-0 z-20 flex h-[82px] items-center justify-between border-b border-slate-200/80 bg-white/95 px-4 backdrop-blur sm:px-7 lg:px-9">
        <div className="flex items-center gap-3"><button onClick={() => setMobileOpen(true)} className="rounded-xl border border-slate-200 p-2.5 text-slate-600 lg:hidden" aria-label="Open navigation"><Icon name="menu"/></button><div className="hidden sm:block"><p className="text-xs font-semibold text-slate-400">Welcome back</p><p className="text-sm font-bold text-slate-800">{currentName}</p></div></div>
        <div className="flex items-center gap-3"><div className="hidden items-center gap-2 rounded-xl border border-slate-200 bg-slate-50 p-1 md:flex">{(["reception", "doctor"] as PortalRole[]).map((item) => <button key={item} onClick={() => switchRole(item)} className={`rounded-lg px-3 py-1.5 text-xs font-bold capitalize transition ${role === item ? "bg-white text-[#176b73] shadow-sm" : "text-slate-500 hover:text-slate-800"}`}>{item}</button>)}</div><select aria-label="Switch demo role" value={role} onChange={(e) => switchRole(e.target.value as PortalRole)} className="rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm font-semibold text-slate-700 md:hidden"><option value="reception">Reception</option><option value="doctor">Doctor</option></select><span className="grid h-10 w-10 place-items-center rounded-full bg-[#dceced] text-sm font-extrabold text-[#176b73]">{currentName?.split(" ").filter((item) => !item.includes("Dr.")).slice(0,2).map((item) => item[0]).join("")}</span></div>
      </header>
      <main className="mx-auto max-w-[1500px] p-4 sm:p-7 lg:p-9">{children}</main>
    </div>
  </div>;
}

function DentalMark() { return <svg viewBox="0 0 32 32" fill="none" className="h-7 w-7" aria-hidden="true"><path d="M9.4 4.5c2.2-.4 4.4 1.6 6.6 1.6s4.4-2 6.6-1.6c3.6.7 4.8 4.4 4.2 8-.5 3-2 5.2-3 8.7-.8 2.8-1.7 6.3-3.8 6.3-2.6 0-1.6-7-4-7s-1.4 7-4 7c-2.1 0-3-3.5-3.8-6.3-1-3.5-2.5-5.7-3-8.7-.6-3.6.6-7.3 4.2-8Z" stroke="currentColor" strokeWidth="2"/><path d="M11 9.5c1.8 1 3.2 1.4 5 1.4s3.2-.4 5-1.4" stroke="currentColor" strokeWidth="1.5"/></svg>; }

export function PageHeader({ eyebrow, title, description, action }: { eyebrow?: string; title: string; description: string; action?: ReactNode }) { return <header className="mb-7 flex flex-col justify-between gap-4 sm:flex-row sm:items-end"><div>{eyebrow && <p className="mb-2 text-xs font-extrabold uppercase tracking-[.14em] text-[#176b73]">{eyebrow}</p>}<h1 className="text-3xl font-bold tracking-[-.035em] text-slate-950 sm:text-[36px]">{title}</h1><p className="mt-2 max-w-2xl text-sm leading-6 text-slate-500 sm:text-[15px]">{description}</p></div>{action}</header>; }

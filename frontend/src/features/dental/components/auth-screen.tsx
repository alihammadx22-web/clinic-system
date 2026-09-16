"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState, type FormEvent } from "react";
import { apiAuth } from "../api-service";
import { useClinic } from "../clinic-provider";
import type { PortalRole } from "../types";
import { Button, Field, Icon, inputClass } from "./ui";

type Mode = "login" | "register" | "forgot" | "verification";

export function AuthScreen({ mode }: { mode: Mode }) {
  const router = useRouter();
  const { signIn } = useClinic();
  const [role, setRole] = useState<PortalRole>("reception");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [name, setName] = useState("");
  const [phone, setPhone] = useState("");
  const [code, setCode] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [done, setDone] = useState(false);
  const titles = {
    login: ["Welcome back", "Sign in to manage your clinic workspace."],
    register: ["Create your account", "Set up a patient portal account."],
    forgot: ["Reset your password", "We will send reset instructions to your email."],
    verification: ["Verify your email", "Enter the six-digit code sent to your inbox."],
  }[mode];

  async function submit(e: FormEvent) {
    e.preventDefault();
    setError("");
    if (mode !== "verification" && !/^\S+@\S+\.\S+$/.test(email)) {
      setError("Enter a valid email address.");
      return;
    }
    if ((mode === "login" || mode === "register") && password.length < 6) {
      setError("Password must be at least 6 characters.");
      return;
    }
    if (mode === "register" && name.trim().length < 2) {
      setError("Enter your full name.");
      return;
    }
    if (mode === "register" && !/^\+?[\d\s()-]{7,}$/.test(phone)) {
      setError("Enter a valid phone number.");
      return;
    }
    if (mode === "verification" && !/^\d{6}$/.test(code)) {
      setError("Enter the six-digit verification code.");
      return;
    }

    setLoading(true);
    try {
      if (mode === "login") {
        const { token, user } = await apiAuth.login(email, password);
        signIn(token, user);
        router.push(`/${user.role}`);
      } else if (mode === "register") {
        await apiAuth.register(name, email, phone, password);
        router.push("/verification");
      } else {
        setDone(true);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "The request could not be completed.");
    } finally {
      setLoading(false);
    }
  }

  return <main className="grid min-h-screen bg-white lg:grid-cols-[.9fr_1.1fr]"><section className="relative hidden overflow-hidden bg-[#0c3440] p-12 text-white lg:flex lg:flex-col lg:justify-between xl:p-16"><div className="absolute -right-28 -top-24 h-80 w-80 rounded-full border border-white/10"/><div className="absolute -bottom-36 -left-24 h-96 w-96 rounded-full bg-teal-500/10 blur-3xl"/><div className="relative flex items-center gap-3"><span className="grid h-12 w-12 place-items-center rounded-xl bg-white text-[#176b73]"><Tooth/></span><div><b className="block text-xl">Dental Clinic</b><span className="text-xs font-semibold uppercase tracking-[.17em] text-white/50">Management system</span></div></div><div className="relative max-w-lg"><p className="text-xs font-bold uppercase tracking-[.2em] text-teal-200/70">Simple, connected care</p><h1 className="mt-5 text-5xl font-bold leading-[1.08] tracking-[-.05em]">A calmer way to run every clinic day.</h1><p className="mt-6 text-lg leading-8 text-white/65">One clear workspace for reception teams, doctors, and patients-from booking through completed care.</p></div><p className="relative text-sm text-white/40">Generic, configurable clinic experience</p></section><section className="flex items-center justify-center px-5 py-10 sm:px-8"><div className="w-full max-w-md"><div className="mb-9 flex items-center gap-3 lg:hidden"><span className="grid h-11 w-11 place-items-center rounded-xl bg-[#176b73] text-white"><Tooth/></span><b>Dental Clinic</b></div><p className="text-sm font-bold text-[#176b73]">Secure access</p><h2 className="mt-2 text-4xl font-bold tracking-[-.045em]">{titles[0]}</h2><p className="mt-3 text-sm leading-6 text-slate-500">{titles[1]}</p>{done?<div className="mt-8 rounded-2xl border border-emerald-200 bg-emerald-50 p-6 text-center"><Icon name="check" className="mx-auto h-7 w-7 text-emerald-700"/><h3 className="mt-3 font-bold text-emerald-900">Instructions sent</h3><p className="mt-1 text-sm text-emerald-700">Check your email for the next step.</p><Link href="/login" className="mt-5 inline-block text-sm font-bold text-emerald-800">Return to login</Link></div>:<form onSubmit={submit} className="mt-8 space-y-4">{mode==="login"&&<Field label="Portal"><div className="grid grid-cols-3 gap-2">{(["reception","doctor","patient"] as PortalRole[]).map(v=><button type="button" key={v} onClick={()=>setRole(v)} className={`rounded-xl border px-2 py-3 text-xs font-bold capitalize ${role===v?"border-[#176b73] bg-teal-50 text-[#176b73]":"border-slate-200 text-slate-500"}`}>{v}</button>)}</div></Field>}{mode==="register"&&<Field label="Full name"><input className={inputClass} value={name} onChange={e=>setName(e.target.value)} placeholder="Your full name"/></Field>}{mode==="register"&&<Field label="Phone"><input className={inputClass} value={phone} onChange={e=>setPhone(e.target.value)} placeholder="+1 (555) 000-0000"/></Field>}{mode!=="verification"&&<Field label="Email address"><input type="email" className={inputClass} value={email} onChange={e=>setEmail(e.target.value)} placeholder="you@example.com"/></Field>}{(mode==="login"||mode==="register")&&<Field label="Password"><input type="password" className={inputClass} value={password} onChange={e=>setPassword(e.target.value)} placeholder="At least 6 characters"/></Field>}{mode==="verification"&&<Field label="Verification code"><input inputMode="numeric" maxLength={6} className={`${inputClass} text-center text-xl tracking-[.4em]`} value={code} onChange={e=>setCode(e.target.value.replace(/\D/g,""))} placeholder="000000"/></Field>}{error&&<p className="rounded-xl bg-red-50 p-3 text-sm font-semibold text-red-700">{error}</p>}<Button type="submit" className="w-full" disabled={loading}>{loading?"Please wait...":mode==="login"?"Sign in":mode==="register"?"Create account":mode==="forgot"?"Send reset link":"Verify email"}</Button></form>}<div className="mt-6 flex justify-center gap-4 text-sm">{mode==="login"?<><Link href="/forgot-password" className="font-semibold text-slate-500">Forgot password?</Link><Link href="/register" className="font-bold text-[#176b73]">Register</Link></>:<Link href="/login" className="font-bold text-[#176b73]">Back to login</Link>}</div></div></section></main>;
}

function Tooth(){return <svg viewBox="0 0 32 32" fill="none" className="h-8 w-8"><path d="M9.4 4.5c2.2-.4 4.4 1.6 6.6 1.6s4.4-2 6.6-1.6c3.6.7 4.8 4.4 4.2 8-.5 3-2 5.2-3 8.7-.8 2.8-1.7 6.3-3.8 6.3-2.6 0-1.6-7-4-7s-1.4 7-4 7c-2.1 0-3-3.5-3.8-6.3-1-3.5-2.5-5.7-3-8.7-.6-3.6.6-7.3 4.2-8Z" stroke="currentColor" strokeWidth="2"/></svg>}

"use client";

import { useRouter } from "next/navigation";
import { useState, type FormEvent } from "react";
import { apiAuth } from "../api-service";
import { useClinic } from "../clinic-provider";
import { Button, Field, inputClass } from "./ui";

type Mode = "login";

export function AuthScreen({ mode }: { mode: Mode }) {
  void mode;
  const router = useRouter();
  const { signIn } = useClinic();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  async function submit(e: FormEvent) {
    e.preventDefault();
    setError("");
    if (!/^\S+@\S+\.\S+$/.test(email)) {
      setError("Enter a valid email address.");
      return;
    }
    if (password.length < 6) {
      setError("Password must be at least 6 characters.");
      return;
    }
    setLoading(true);
    try {
      const { token, user } = await apiAuth.login(email, password);
      signIn(token, user);
      router.push(`/${user.role}`);
    } catch (err) {
      setError(err instanceof Error ? err.message : "The request could not be completed.");
    } finally {
      setLoading(false);
    }
  }

  return <main className="grid min-h-screen bg-white lg:grid-cols-[.9fr_1.1fr]">
    <section className="relative hidden overflow-hidden bg-[#0c3440] p-12 text-white lg:flex lg:flex-col lg:justify-between xl:p-16">
      <div className="absolute -right-28 -top-24 h-80 w-80 rounded-full border border-white/10" />
      <div className="absolute -bottom-36 -left-24 h-96 w-96 rounded-full bg-teal-500/10 blur-3xl" />
      <div className="relative flex items-center gap-3"><span className="grid h-12 w-12 place-items-center rounded-xl bg-white text-[#176b73]"><Tooth /></span><div><b className="block text-xl">Dental Clinic</b><span className="text-xs font-semibold uppercase tracking-[.17em] text-white/50">Internal management system</span></div></div>
      <div className="relative max-w-lg"><p className="text-xs font-bold uppercase tracking-[.2em] text-teal-200/70">Staff workspace</p><h1 className="mt-5 text-5xl font-bold leading-[1.08] tracking-[-.05em]">Run the clinic day from one clear place.</h1><p className="mt-6 text-lg leading-8 text-white/65">Secure access for reception and doctors to manage patients, appointments, cases, and schedules.</p></div>
      <p className="relative text-sm text-white/40">Reception and doctor access only</p>
    </section>
    <section className="flex items-center justify-center px-5 py-10 sm:px-8"><div className="w-full max-w-md">
      <div className="mb-9 flex items-center gap-3 lg:hidden"><span className="grid h-11 w-11 place-items-center rounded-xl bg-[#176b73] text-white"><Tooth /></span><b>Dental Clinic</b></div>
      <p className="text-sm font-bold text-[#176b73]">Secure staff access</p>
      <h2 className="mt-2 text-4xl font-bold tracking-[-.045em]">Welcome back</h2>
      <p className="mt-3 text-sm leading-6 text-slate-500">Sign in with a reception or doctor account.</p>
      <form onSubmit={submit} className="mt-8 space-y-4">
        <Field label="Email address"><input type="email" className={inputClass} value={email} onChange={e => setEmail(e.target.value)} placeholder="you@example.com" /></Field>
        <Field label="Password"><input type="password" className={inputClass} value={password} onChange={e => setPassword(e.target.value)} placeholder="At least 6 characters" /></Field>
        {error && <p className="rounded-xl bg-red-50 p-3 text-sm font-semibold text-red-700">{error}</p>}
        <Button type="submit" className="w-full" disabled={loading}>{loading ? "Please wait..." : "Sign in"}</Button>
      </form>
    </div></section>
  </main>;
}

function Tooth(){return <svg viewBox="0 0 32 32" fill="none" className="h-8 w-8"><path d="M9.4 4.5c2.2-.4 4.4 1.6 6.6 1.6s4.4-2 6.6-1.6c3.6.7 4.8 4.4 4.2 8-.5 3-2 5.2-3 8.7-.8 2.8-1.7 6.3-3.8 6.3-2.6 0-1.6-7-4-7s-1.4 7-4 7c-2.1 0-3-3.5-3.8-6.3-1-3.5-2.5-5.7-3-8.7-.6-3.6.6-7.3 4.2-8Z" stroke="currentColor" strokeWidth="2" /></svg>}

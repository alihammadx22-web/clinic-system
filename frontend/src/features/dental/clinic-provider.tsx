"use client";

import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from "react";
import { authSession, clinicApi, type SessionUser } from "./api-service";
import type { Appointment, AppointmentInput, AppointmentStatus, CaseInput, ClinicData, DentalCase, Doctor, ExceptionInput, Patient, PatientInput } from "./types";

type Notice = { kind: "success" | "error"; message: string } | null;
type ContextValue = {
  data: ClinicData | null;
  user: SessionUser | null;
  loading: boolean;
  error: string | null;
  notice: Notice;
  clearNotice(): void;
  authLoaded: boolean;
  signIn(token: string, user: SessionUser): void;
  addPatient(input: PatientInput): Promise<Patient | null>;
  editPatient(patient: Patient): Promise<void>;
  deletePatient(id: string): Promise<void>;
  addAppointment(input: AppointmentInput): Promise<Appointment | null>;
  editAppointment(appointment: Appointment): Promise<void>;
  setAppointmentStatus(id: string, status: AppointmentStatus): Promise<void>;
  addCase(input: CaseInput): Promise<DentalCase | null>;
  editCase(item: DentalCase): Promise<void>;
  addException(input: ExceptionInput): Promise<void>;
  addDoctor(doctor: Doctor): Promise<void>;
  editDoctor(doctor: Doctor): Promise<void>;
  deleteDoctor(id: string): Promise<void>;
  reset(): void;
  signOut(): void;
};

const ClinicContext = createContext<ContextValue | null>(null);

export function ClinicProvider({ children }: { children: ReactNode }) {
  const [data, setData] = useState<ClinicData | null>(null);
  const [user, setUser] = useState<SessionUser | null>(null);
  const [authLoaded, setAuthLoaded] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<Notice>(null);

  const load = useCallback(async () => {
    const token = authSession.getToken();
    const sessionUser = authSession.getUser();
    setUser(sessionUser);
    setAuthLoaded(true);
    if (!token || !sessionUser) {
      setData(null);
      setLoading(false);
      return null;
    }
    setLoading(true);
    setError(null);
    try {
      const next = await clinicApi.load();
      setData(next);
      return next;
    } catch (err) {
      setError(err instanceof Error ? err.message : "The clinic data could not be loaded. Please try again.");
      return null;
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    let active = true;
    const timer = window.setTimeout(() => {
      if (active) void load();
    }, 0);
    return () => { active = false; window.clearTimeout(timer); };
  }, [load]);
  useEffect(() => { if (!notice) return; const timer = window.setTimeout(() => setNotice(null), 3200); return () => window.clearTimeout(timer); }, [notice]);

  const commit = useCallback(async <T,>(action: () => Promise<{ data: ClinicData; item?: T } | ClinicData>, message: string) => {
    try {
      const result = await action();
      const next = "data" in result ? result.data : result;
      setData(next);
      setNotice({ kind: "success", message });
      return "item" in result ? (result.item ?? null) : null;
    } catch (err) {
      setNotice({ kind: "error", message: err instanceof Error ? err.message : "Changes could not be saved." });
      return null;
    }
  }, []);

  const value = useMemo<ContextValue>(() => ({
    data, user, loading, error, notice, authLoaded, clearNotice: () => setNotice(null),
    signIn(token, nextUser) {
      authSession.saveSession(token, nextUser);
      setUser(nextUser);
      setAuthLoaded(true);
      setData(null);
      setError(null);
      void load();
    },
    addPatient(input) { return commit<Patient>(() => clinicApi.addPatient(input).then((next) => ({ data: next, item: next.patients[0] })), "Patient added successfully"); },
    async editPatient(patient) { await commit(() => clinicApi.editPatient(patient), "Patient details updated"); },
    async deletePatient(id) { await commit(() => clinicApi.deletePatient(id), "Patient deleted"); },
    addAppointment(input) { return commit<Appointment>(() => clinicApi.addAppointment(input).then((next) => ({ data: next.data, item: next.appointment })), "Appointment booked successfully"); },
    async editAppointment(appointment) { await commit(() => clinicApi.editAppointment(appointment), "Appointment rescheduled"); },
    async setAppointmentStatus(id, status) { await commit(() => clinicApi.setAppointmentStatus(id, status), status === "Cancelled" ? "Appointment cancelled" : `Appointment marked ${status.toLowerCase()}`); },
    addCase(input) { return commit<DentalCase>(() => clinicApi.addCase(input).then((next) => ({ data: next, item: next.cases[0] })), "Dental case created"); },
    async editCase(item) { await commit(() => clinicApi.editCase(item), item.status === "Completed" ? "Dental case completed" : "Dental case updated"); },
    async addException(input) { await commit(() => clinicApi.addException(input), "Schedule exception added"); },
    async addDoctor(doctor) { await commit(() => clinicApi.addDoctor(doctor), "Doctor added successfully"); },
    async editDoctor(doctor) { await commit(() => clinicApi.editDoctor(doctor), "Doctor updated successfully"); },
    async deleteDoctor(id) { await commit(() => clinicApi.deleteDoctor(id), "Doctor deleted"); },
    reset() { void load(); setNotice({ kind: "success", message: "Clinic data refreshed" }); },
    signOut() { authSession.clear(); setUser(null); setData(null); setAuthLoaded(true); },
  }), [data, user, loading, error, notice, authLoaded, commit, load]);

  return <ClinicContext.Provider value={value}>{children}</ClinicContext.Provider>;
}

export function useClinic() {
  const value = useContext(ClinicContext);
  if (!value) throw new Error("useClinic must be used inside ClinicProvider");
  return value;
}

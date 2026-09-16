"use client";

import { useRouter } from "next/navigation";
import { useEffect } from "react";
import { useClinic } from "../clinic-provider";
import type { PortalRole } from "../types";
import { DoctorPortal } from "./doctor-portal";
import { PatientPortal } from "./patient-portal";
import { PortalShell } from "./portal-shell";
import { ReceptionPortal } from "./reception-portal";
import { ErrorScreen, LoadingScreen } from "./ui";

export function DentalPortal({ role, section = "dashboard" }: { role: PortalRole; section?: string }) {
  const router = useRouter();
  const { authLoaded, loading, error, user } = useClinic();
  useEffect(() => {
    if (!authLoaded) return;
    if (!user) router.replace("/login");
    else if (user.role !== role) router.replace(`/${user.role}`);
  }, [authLoaded, role, router, user]);
  if (!authLoaded || !user || user.role !== role) return <LoadingScreen />;
  return <PortalShell role={role}>{loading ? <LoadingScreen /> : error ? <ErrorScreen message={error} /> : role === "reception" ? <ReceptionPortal section={section} /> : role === "doctor" ? <DoctorPortal section={section} /> : <PatientPortal section={section} />}</PortalShell>;
}

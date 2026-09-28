import type {
  Appointment,
  AppointmentInput,
  AppointmentStatus,
  CaseInput,
  CaseStatus,
  CaseType,
  ClinicData,
  DentalCase,
  Doctor,
  ExceptionInput,
  Patient,
  PatientInput,
  PortalRole,
} from "./types";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080/api";
const TOKEN_KEY = "dental-clinic-token";
const USER_KEY = "dental-clinic-user";

type ApiRole = "RECEPTION" | "DOCTOR" | "PATIENT";
type ApiUser = {
  id: string;
  fullName: string;
  email: string | null;
  phone: string;
  role: ApiRole;
  age: number | null;
  gender: string | null;
  notes: string | null;
  address: string | null;
  active: boolean;
  createdAt: string | null;
  updatedAt: string | null;
};
type ApiLoginResponse = { token: string; user: ApiUser };
type ApiSchedule = { id: string; doctorId: string; dayOfWeek: number; startTime: string; endTime: string };
type ApiAppointment = { id: string; patientId: string; doctorId: string; appointmentDate: string; startTime: string; status: string; notes: string | null };
type ApiCase = { id: string; patientId: string; doctorId: string; caseType: string; notes: string | null; status: string; createdAt: string | null; completedAt: string | null };

export type SessionUser = { id: string; name: string; email: string; role: PortalRole };

export const authSession = {
  getToken() {
    if (typeof window === "undefined") return null;
    return window.localStorage.getItem(TOKEN_KEY);
  },
  getUser(): SessionUser | null {
    if (typeof window === "undefined") return null;
    const raw = window.localStorage.getItem(USER_KEY);
    if (!raw) return null;
    try {
      return normalizeSessionUser(JSON.parse(raw));
    } catch {
      return null;
    }
  },
  save(token: string, user: ApiUser) {
    this.saveSession(token, toSessionUser(user));
  },
  saveSession(token: string, user: SessionUser) {
    window.localStorage.setItem(TOKEN_KEY, token);
    window.localStorage.setItem(USER_KEY, JSON.stringify(user));
  },
  clear() {
    window.localStorage.removeItem(TOKEN_KEY);
    window.localStorage.removeItem(USER_KEY);
  },
};

export const apiAuth = {
  async login(email: string, password: string) {
    const response = await request<ApiLoginResponse>("/auth/login", {
      method: "POST",
      body: { email, password },
      auth: false,
    });
    const user = toSessionUser(response.user);
    authSession.saveSession(response.token, user);
    return { token: response.token, user };
  },
};

export const clinicApi = {
  async load(): Promise<ClinicData> {
    const [users, schedules, appointments, cases] = await Promise.all([
      request<ApiUser[]>("/users"),
      request<ApiSchedule[]>("/doctor-schedules"),
      request<ApiAppointment[]>("/appointments"),
      request<ApiCase[]>("/dental-cases"),
    ]);
    return toClinicData(users, schedules, appointments, cases);
  },

  async addPatient(input: PatientInput) {
    await request<ApiUser>("/users", {
      method: "POST",
      body: toUserRequest(input, "PATIENT"),
    });
    return this.load();
  },

  async editPatient(patient: Patient) {
    await request<ApiUser>(`/users/${patient.id}`, {
      method: "PUT",
      body: toUserRequest(patient, "PATIENT"),
    });
    return this.load();
  },

  async deletePatient(id: string) {
    await request<void>(`/users/${id}`, { method: "DELETE" });
    return this.load();
  },

  async addAppointment(input: AppointmentInput) {
    const appointment = await request<ApiAppointment>("/appointments", {
      method: "POST",
      body: {
        patientId: input.patientId,
        doctorId: input.doctorId,
        appointmentDate: input.date,
        startTime: input.time,
        notes: input.notes,
      },
    });
    return { data: await this.load(), appointment: toAppointment(appointment) };
  },

  async editAppointment(appointment: Appointment) {
    await request<ApiAppointment>(`/appointments/${appointment.id}/reschedule`, {
      method: "PUT",
      body: {
        doctorId: appointment.doctorId,
        appointmentDate: appointment.date,
        startTime: appointment.time,
        notes: appointment.notes,
      },
    });
    return this.load();
  },

  async setAppointmentStatus(id: string, status: AppointmentStatus) {
    await request<ApiAppointment>(`/appointments/${id}/status`, {
      method: "PUT",
      body: { status: toApiAppointmentStatus(status) },
    });
    return this.load();
  },

  async addCase(input: CaseInput) {
    const currentUser = authSession.getUser();
    await request<ApiCase>("/dental-cases", {
      method: "POST",
      body: {
        patientId: input.patientId,
        doctorId: input.doctorId || currentUser?.id,
        caseType: input.type,
        notes: input.notes,
        status: toApiCaseStatus(input.status),
      },
    });
    return this.load();
  },

  async editCase(item: DentalCase) {
    await request<ApiCase>(`/dental-cases/${item.id}`, {
      method: "PUT",
      body: {
        patientId: item.patientId,
        doctorId: item.doctorId,
        caseType: item.type,
        notes: item.notes,
        status: toApiCaseStatus(item.status),
      },
    });
    return this.load();
  },


  async addDoctor(doctor: Doctor) {
    const created = await request<ApiUser>("/users", {
      method: "POST",
      body: toDoctorUserRequest(doctor, true),
    });
    await syncDoctorSchedules(created.id, doctor);
    return this.load();
  },

  async editDoctor(doctor: Doctor) {
    await request<ApiUser>(`/users/${doctor.id}`, {
      method: "PUT",
      body: toDoctorUserRequest(doctor, false),
    });
    await syncDoctorSchedules(doctor.id, doctor);
    return this.load();
  },

  async deleteDoctor(id: string) {
    await request<void>(`/users/${id}`, { method: "DELETE" });
    return this.load();
  },

  async addException(input: ExceptionInput) {
    void input;
    return this.load();
  },

};

async function request<T>(path: string, options: { method?: string; body?: unknown; auth?: boolean } = {}): Promise<T> {
  const headers = new Headers();
  headers.set("Accept", "application/json");
  if (options.body !== undefined) headers.set("Content-Type", "application/json");

  if (options.auth !== false) {
    const token = authSession.getToken();
    if (token) headers.set("Authorization", `Bearer ${token}`);
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    method: options.method ?? "GET",
    headers,
    body: options.body === undefined ? undefined : JSON.stringify(options.body),
  });

  if (response.status === 204) return undefined as T;
  if (!response.ok) throw new Error(await errorMessage(response));
  return response.json() as Promise<T>;
}

async function errorMessage(response: Response) {
  try {
    const body = await response.json() as { detail?: string; message?: string; error?: string };
    return body.detail ?? body.message ?? body.error ?? "Request failed.";
  } catch {
    return "Request failed.";
  }
}

function toClinicData(users: ApiUser[], schedules: ApiSchedule[], appointments: ApiAppointment[], cases: ApiCase[]): ClinicData {
  const currentUser = authSession.getUser();
  const patients = currentUserFirst(users.filter((user) => user.role === "PATIENT"), currentUser?.id).map(toPatient);
  const doctors = currentUserFirst(users.filter((user) => user.role === "DOCTOR"), currentUser?.id)
    .map((user, index) => toDoctor(user, index, schedules));

  return {
    exceptions: [],
    patients,
    doctors,
    appointments: appointments.map(toAppointment),
    cases: cases.map(toCase),
  };
}

function currentUserFirst(users: ApiUser[], currentUserId?: string) {
  if (!currentUserId) return users;
  return [...users].sort((left, right) => {
    if (left.id === currentUserId) return -1;
    if (right.id === currentUserId) return 1;
    return left.fullName.localeCompare(right.fullName);
  });
}

function toPatient(user: ApiUser): Patient {
  return {
    id: user.id,
    name: user.fullName,
    phone: user.phone,
    age: user.age ?? 0,
    address: user.address ?? "",
    medicalNotes: user.notes ?? "",
    active: user.active,
    createdAt: dateOnly(user.createdAt),
  };
}

function toDoctor(user: ApiUser, index: number, schedules: ApiSchedule[]): Doctor {
  const weekly = schedules.filter((item) => item.doctorId === user.id).sort((a, b) => a.dayOfWeek - b.dayOfWeek);
  return {
    id: user.id,
    name: user.fullName.startsWith("Dr.") ? user.fullName : `Dr. ${user.fullName}`,
    specialty: user.notes || "General Dentistry",
    phone: user.phone,
    email: user.email ?? "",
    room: `Room ${index + 1}`,
    fee: 140,
    workingDays: weekly.map((item) => item.dayOfWeek),
    startTime: stripSeconds(weekly[0]?.startTime ?? "09:00"),
    endTime: stripSeconds(weekly[0]?.endTime ?? "17:00"),
    active: user.active,
  };
}

function toAppointment(item: ApiAppointment): Appointment {
  return {
    id: item.id,
    patientId: item.patientId,
    doctorId: item.doctorId,
    date: item.appointmentDate,
    time: stripSeconds(item.startTime),
    duration: 30,
    notes: item.notes ?? "",
    status: toUiAppointmentStatus(item.status),
    createdAt: item.appointmentDate,
  };
}

function toCase(item: ApiCase): DentalCase {
  return {
    id: item.id,
    patientId: item.patientId,
    doctorId: item.doctorId,
    type: toUiCaseType(item.caseType),
    notes: item.notes ?? "",
    status: toUiCaseStatus(item.status),
    createdDate: dateOnly(item.createdAt),
    completedDate: item.completedAt ? dateOnly(item.completedAt) : undefined,
  };
}


function toUserRequest(input: PatientInput | Patient, role: ApiRole) {
  return {
    fullName: input.name,
    email: null,
    phone: input.phone,
    password: null,
    role,
    age: input.age,
    gender: null,
    notes: input.medicalNotes,
    address: input.address || null,
    active: input.active,
  };
}

function toDoctorUserRequest(input: Doctor, creating: boolean) {
  const name = input.name.replace(/^Dr\.?\s*/i, "").trim();
  return {
    fullName: name || input.name,
    email: input.email,
    phone: input.phone,
    password: creating ? input.password : input.password || null,
    role: "DOCTOR",
    age: null,
    gender: null,
    notes: input.specialty || "General Dentistry",
    address: null,
    active: input.active,
  };
}

async function syncDoctorSchedules(doctorId: string, doctor: Doctor) {
  const existing = await request<ApiSchedule[]>(`/doctor-schedules/doctor/${doctorId}`);
  const byDay = new Map(existing.map((item) => [item.dayOfWeek, item]));

  await Promise.all(
    doctor.workingDays.map((dayOfWeek) => {
      const body = { doctorId, dayOfWeek, startTime: doctor.startTime, endTime: doctor.endTime };
      const current = byDay.get(dayOfWeek);
      return current
        ? request<ApiSchedule>(`/doctor-schedules/${current.id}`, { method: "PUT", body })
        : request<ApiSchedule>("/doctor-schedules", { method: "POST", body });
    })
  );

  await Promise.all(
    existing
      .filter((item) => !doctor.workingDays.includes(item.dayOfWeek))
      .map((item) => request<void>(`/doctor-schedules/${item.id}`, { method: "DELETE" }))
  );
}


function toApiAppointmentStatus(status: AppointmentStatus) {
  return status.trim().toUpperCase().replaceAll(" ", "_");
}

function toUiAppointmentStatus(status: string): AppointmentStatus {
  const value = status.trim().toUpperCase();
  if (value === "CHECKED_IN") return "Checked In";
  if (value === "IN_PROGRESS") return "In Progress";
  if (value === "COMPLETED") return "Completed";
  if (value === "CANCELLED") return "Cancelled";
  return "Scheduled";
}

function toApiCaseStatus(status: CaseStatus) {
  return status.trim().toUpperCase().replaceAll(" ", "_");
}

function toUiCaseStatus(status: string): CaseStatus {
  const value = status.trim().toUpperCase();
  if (value === "IN_PROGRESS") return "In Progress";
  if (value === "COMPLETED") return "Completed";
  return "Open";
}

function toUiCaseType(caseType: string): CaseType {
  const normalized = caseType.replaceAll("_", " ").toLowerCase();
  const known: CaseType[] = ["Filling", "Extraction", "Root Canal", "Crown", "Orthodontics", "Cleaning", "Whitening", "Other"];
  return known.find((item) => item.toLowerCase() === normalized) ?? "Other";
}

function toSessionUser(user: ApiUser): SessionUser {
  const role = normalizeRole(user.role);
  if (!role) throw new Error("Only reception and doctor users can sign in.");
  return { id: user.id, name: user.fullName, email: user.email ?? "", role };
}

function normalizeSessionUser(value: unknown): SessionUser | null {
  if (!value || typeof value !== "object") return null;
  const item = value as Partial<SessionUser>;
  if (!item.id || !item.name || !item.role) return null;
  const role = normalizeRole(String(item.role));
  if (!role) return null;
  return {
    id: String(item.id),
    name: String(item.name),
    email: item.email ? String(item.email) : "",
    role,
  };
}

function normalizeRole(role: string): PortalRole | null {
  const normalized = role.trim().toLowerCase();
  if (normalized === "doctor" || normalized === "reception") return normalized;
  return null;
}

function stripSeconds(value: string) {
  return value.slice(0, 5);
}

function dateOnly(value: string | null) {
  return value?.slice(0, 10) ?? "";
}
